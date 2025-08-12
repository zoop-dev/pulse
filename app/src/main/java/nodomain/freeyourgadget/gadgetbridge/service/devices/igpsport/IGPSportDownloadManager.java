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

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.CyclingData;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.FileDownload;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport.fit.FitImporter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class IGPSportDownloadManager {

        Logger LOG = LoggerFactory.getLogger(nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport.IGPSportDownloadManager.class);
        private IGPSportDeviceSupport support = null;
        private List<FileInfo> avaliableActivityFiles = new ArrayList<>();
        private ByteArrayOutputStream recievingDataBuffer;
        private FileInfo downloadingFile;
        private Boolean downloadInProgress = false;
        private Boolean firstChunk = true;
        private FitImporter fitImporter;
        int pbSize=0;


        public IGPSportDownloadManager(IGPSportDeviceSupport support) {
            this.support = support;
            recievingDataBuffer = new ByteArrayOutputStream();

        }

        public void setFilesAvaliable(byte[] pbData) throws InvalidProtocolBufferException {

            List<CyclingData.cycling_data_file_flag_message> message =  CyclingData.cycling_data_msg.parseFrom(pbData).getCyclingDataFileFlagMsgList();
            for (final CyclingData.cycling_data_file_flag_message  fileMsg : message) {
                avaliableActivityFiles.add(new FileInfo(fileMsg));
            }

            LOG.info("Found " + message.size() + " files");
            syncNextFile();

        }

        public void syncNextFile() {
            if (avaliableActivityFiles.isEmpty()) {
                LOG.info("No files to sync");
                if (support.getDevice().isBusy() ) {
                    support.getDevice().unsetBusyTask();
                    GB.signalActivityDataFinish(support.getDevice());
                    support.getDevice().sendDeviceUpdateIntent(getContext());
                }
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
            fitImporter = new FitImporter(support.getContext(), support.getDevice());
        }

        public void addData(byte[] data) {
            try {
                recievingDataBuffer.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (firstChunk) {
                firstChunk = false;
                pbSize = ByteBuffer.wrap(recievingDataBuffer.toByteArray(), 20, 4).getInt();
            }

            LOG.debug("current data stored size: " + recievingDataBuffer.size() + " need " + (downloadingFile.getFileSize() + 20 + 4 + pbSize) );

            if (recievingDataBuffer.size() >= (downloadingFile.getFileSize() + 20 + 4 + pbSize) ) { // fileSize + header + pbSize + pbInfo
                //LOG.info(GB.hexdump(recievingDataBuffer.toByteArray()));
                downloadInProgress = false;

                File dir;
                File outputFile;

                try {
                    dir = support.getWritableExportDirectory();
                    outputFile = new File(dir, downloadingFile.getFileName());

                    pbSize = ByteBuffer.wrap(recievingDataBuffer.toByteArray(), 20, 4).getInt();
                    byte[] pbData = new byte[pbSize];
                    System.arraycopy(recievingDataBuffer.toByteArray(), 20+4, pbData, 0, pbSize);
                    FileDownload.file_download pbInfo = FileDownload.file_download.parseFrom(pbData);
                    FileUtils.copyStreamToFile(new ByteArrayInputStream(recievingDataBuffer.toByteArray(), 20+4+pbSize, pbInfo.getFileSize()), outputFile);
                    outputFile.setLastModified(garminTimestampToJavaMillis(downloadingFile.getGarminTimeStamp()));
                    fitImporter.importFile(outputFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (FitParseException e) {
                    throw new RuntimeException(e);
                }

                syncNextFile();

            }
        }

        public Boolean needMoreData() {
            if (!downloadInProgress)
                return false;
            if (recievingDataBuffer.size() < (downloadingFile.getFileSize() + 20 + 4 + pbSize)) {
                return true;
            } else {
                return false;
            }
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
