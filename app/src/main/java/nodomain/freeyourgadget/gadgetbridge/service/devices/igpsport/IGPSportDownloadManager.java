/*  Copyright (C) 2025 Vitaliy Tomin, Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils.garminTimestampToJavaMillis;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.CyclingData;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.FileDownload;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitAsyncProcessor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitImporter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification;


public class IGPSportDownloadManager {

        private static final String PREF_LAST_SYNC_GARMIN_TIMESTAMP = "lastSyncGarminTimestamp";

        Logger LOG = LoggerFactory.getLogger(IGPSportDownloadManager.class);
        private IGPSportDeviceSupport support = null;
        private List<FileInfo> avaliableActivityFiles = new ArrayList<>();
        private ByteArrayOutputStream recievingDataBuffer;
        private FileInfo downloadingFile;
        private Boolean downloadInProgress = false;
        private Boolean firstChunk = true;
        private FitImporter fitImporter;
        private GBProgressNotification transferNotification;
        private List<File> filesToProcess = new ArrayList<>();
        private int pbSize = 0;
        private int downloadFileSize = 0;
        private int expectedDownloadSize = 0;
        private int lastProgressPercent = -1;
        private int maxDownloadedGarminTimestamp = 0;



        public IGPSportDownloadManager(IGPSportDeviceSupport support) {
            this.support = support;

            recievingDataBuffer = new ByteArrayOutputStream();

        }

        public void setContext(Context context) {
            this.transferNotification = new GBProgressNotification(context, GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
        }

        public void setFilesAvaliable(byte[] pbData) throws InvalidProtocolBufferException {

            List<CyclingData.cycling_data_file_flag_message> message =  CyclingData.cycling_data_msg.parseFrom(pbData).getCyclingDataFileFlagMsgList();
            final int lastSyncTimestamp = getLastSyncGarminTimestamp();
            avaliableActivityFiles.clear();
            filesToProcess.clear();
            maxDownloadedGarminTimestamp = lastSyncTimestamp;
            for (final CyclingData.cycling_data_file_flag_message  fileMsg : message) {
                final FileInfo fileInfo = new FileInfo(fileMsg);
                if (fileInfo.getGarminTimeStamp() > lastSyncTimestamp) {
                    avaliableActivityFiles.add(fileInfo);
                }
            }

            LOG.info("Found {} iGPSPORT activity files, {} new since {}", message.size(), avaliableActivityFiles.size(), lastSyncTimestamp);
            syncNextFile();

        }

        public void syncNextFile() {
            if (avaliableActivityFiles.isEmpty()) {
                LOG.info("No files to sync");

                transferNotification.start(R.string.busy_task_processing_files, 0, filesToProcess.size());

                final FitAsyncProcessor fitAsyncProcessor = new FitAsyncProcessor(getContext(), support.getDevice());
                fitAsyncProcessor.process(filesToProcess, new FitAsyncProcessor.Callback() {
                    @Override
                    public void onProgress(final int i) {
                        transferNotification.setTotalProgress(i);
                    }

                    @Override
                    public void onFinish() {
                        setLastSyncGarminTimestamp(maxDownloadedGarminTimestamp);
                        support.getDevice().unsetBusyTask();
                        GB.signalActivityDataFinish(support.getDevice());
                        transferNotification.finish();
                        support.getDevice().sendDeviceUpdateIntent(getContext());
                    }
                });


                return;
            } else {
                if (!support.getDevice().isBusy() ) {
                    support.getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
                    support.getDevice().sendDeviceUpdateIntent(getContext());
                }
                LOG.info(avaliableActivityFiles.size() + " files to sync");
            }
            downloadingFile = avaliableActivityFiles.remove(0);
            //if (downloadingFile.getStandardTimeStamp() == 1092299406) { // FIXME replace with newer that las sync. hardcoded for debug
                TransactionBuilder builder = support.createTransactionBuilder("ongettrainingfile");
                CyclingData.cycling_data_msg.Builder cycleDataMsg = CyclingData.cycling_data_msg.newBuilder();
                cycleDataMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA);
                cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_GET);
                cycleDataMsg.addCyclingDataFileFlagMsg( CyclingData.cycling_data_file_flag_message.newBuilder().setTimestamp(downloadingFile.getGarminTimeStamp()) );
                byte[] cycleDataMsgBytes = IGPSportDeviceSupport.craftData(cycleDataMsg.getServiceType().getNumber(), 0xff, cycleDataMsg.getCyclingDataOperateType().getNumber(), cycleDataMsg.build().toByteArray(), true);

                builder.write(support.writeCharacteristicThird, cycleDataMsgBytes);
                builder.queue();
//            } else {
//                syncNextFile();
//            }

        }

        public void startDownload() {
            recievingDataBuffer.reset();
            downloadInProgress = true;
            firstChunk = true;
            pbSize = 0;
            downloadFileSize = 0;
            expectedDownloadSize = 0;
            lastProgressPercent = -1;

        }

        public void addData(byte[] data) {
            try {
                recievingDataBuffer.write(data);
            } catch (IOException e) {
                LOG.error("Failed to add data to buffer" + e);
            }

            if (firstChunk && recievingDataBuffer.size() >= 24) {
                firstChunk = false;
                pbSize = ByteBuffer.wrap(recievingDataBuffer.toByteArray(), 20, 4).getInt();
            }

            if (expectedDownloadSize == 0 && pbSize > 0 && recievingDataBuffer.size() >= 20 + 4 + pbSize) {
                try {
                    final byte[] pbData = new byte[pbSize];
                    System.arraycopy(recievingDataBuffer.toByteArray(), 20 + 4, pbData, 0, pbSize);
                    final FileDownload.file_download pbInfo = FileDownload.file_download.parseFrom(pbData);
                    downloadFileSize = pbInfo.getFileSize();
                    expectedDownloadSize = 20 + 4 + pbSize + downloadFileSize;

                    LOG.info("Downloading iGPSPORT FIT file {}, {} bytes", downloadingFile.getFileName(), downloadFileSize);
                    if (transferNotification != null) {
                        transferNotification.start(R.string.busy_task_fetch_activity_data, 0, expectedDownloadSize);
                    }
                } catch (final IOException e) {
                    LOG.warn("Failed to parse iGPSPORT file download header, falling back to list size", e);
                    downloadFileSize = downloadingFile.getFileSize();
                    expectedDownloadSize = 20 + 4 + pbSize + downloadFileSize;
                }
            }

            if (expectedDownloadSize == 0) {
                LOG.debug("Received {} bytes of iGPSPORT FIT file, waiting for file header", recievingDataBuffer.size());
                return;
            }

            final int progressPercent = Math.min(100, (int) ((recievingDataBuffer.size() * 100L) / expectedDownloadSize));
            if (progressPercent != lastProgressPercent) {
                lastProgressPercent = progressPercent;
                LOG.debug("iGPSPORT FIT download progress: {} / {} bytes ({}%)", recievingDataBuffer.size(), expectedDownloadSize, progressPercent);
                if (transferNotification != null) {
                    transferNotification.setTotalProgress(Math.min(recievingDataBuffer.size(), expectedDownloadSize));
                }
            }

            if (recievingDataBuffer.size() >= expectedDownloadSize) { // fileSize + header + pbSize + pbInfo
                //LOG.info(GB.hexdump(recievingDataBuffer.toByteArray()));
                downloadInProgress = false;

                File dir;
                File outputFile = null;

                try {
                    dir = support.getWritableExportDirectory();
                    outputFile = new File(dir, downloadingFile.getFileName());

                    byte[] pbData = new byte[pbSize];
                    System.arraycopy(recievingDataBuffer.toByteArray(), 20+4, pbData, 0, pbSize);
                    FileDownload.file_download pbInfo = FileDownload.file_download.parseFrom(pbData);
                    FileUtils.copyStreamToFile(new ByteArrayInputStream(recievingDataBuffer.toByteArray(), 20+4+pbSize, pbInfo.getFileSize()), outputFile);
                    outputFile.setLastModified(garminTimestampToJavaMillis(downloadingFile.getGarminTimeStamp()));
                    filesToProcess.add(outputFile);
                    maxDownloadedGarminTimestamp = Math.max(maxDownloadedGarminTimestamp, downloadingFile.getGarminTimeStamp());
                } catch (IOException e) {
                    LOG.error("Failed to save fit file to: {}", outputFile != null ? outputFile.getAbsolutePath() : "<unknown>", e);
                }

                syncNextFile();

            }
        }

        public Boolean needMoreData() {
            if (!downloadInProgress) {
                return false;
            }
            return expectedDownloadSize == 0 || recievingDataBuffer.size() < expectedDownloadSize;
        }

        public boolean isDownloading() {
            return downloadInProgress;
        }

        private int getLastSyncGarminTimestamp() {
            return getDevicePrefs().getInt(PREF_LAST_SYNC_GARMIN_TIMESTAMP, 0);
        }

        private void setLastSyncGarminTimestamp(final int timestamp) {
            if (timestamp <= getLastSyncGarminTimestamp()) {
                return;
            }

            getDevicePrefs()
                    .edit()
                    .putInt(PREF_LAST_SYNC_GARMIN_TIMESTAMP, timestamp)
                    .apply();
        }

        private SharedPreferences getDevicePrefs() {
            return GBApplication.getDeviceSpecificSharedPrefs(support.getDevice().getAddress());
        }

        public static class FileInfo {
            private int garmin_timestamp = 0;
            private int file_size = 0;
            private String user_id = "";
            private String device_id = "";
            public FileInfo(CyclingData.cycling_data_file_flag_message message) {
                garmin_timestamp = message.getTimestamp();
                file_size = message.getFileSize();
                user_id = message.getUserId();
                device_id = message.getDeviceId();
            }

            public int getGarminTimeStamp() {
                return garmin_timestamp;
            }

            public String getFileName() {
                long timestampMillis = garminTimestampToJavaMillis(garmin_timestamp);
                Instant instant = Instant.ofEpochMilli(timestampMillis);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
                ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
                return zonedDateTime.format(formatter)+".fit";
            }

            public int getFileSize() {
                return file_size;
            }
        }



}
