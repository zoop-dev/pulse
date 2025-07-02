package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import com.google.protobuf.InvalidProtocolBufferException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.CyclingData;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class IGPSportDownloadManager {

        Logger LOG = LoggerFactory.getLogger(nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport.IGPSportDownloadManager.class);
        private IGPSportDeviceSupport support = null;
        private List<FileInfo> avaliableActivityFiles = new ArrayList<>();
        private ByteArrayOutputStream recievingDataBuffer;
        private FileInfo downloadingFile;
        private Boolean downloadInProgress = false;


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
                return;
            } else {
                LOG.info(avaliableActivityFiles.size() + " files to sync");
            }
            downloadingFile = avaliableActivityFiles.remove(0);
            //if (downloadingFile.getTimeStamp() == 1092299406) { // FIXME replace with newer that las sync. hardcoded for debug
                TransactionBuilder builder = support.createTransactionBuilder("ongettrainingfile");
                CyclingData.cycling_data_msg.Builder cycleDataMsg = CyclingData.cycling_data_msg.newBuilder();
                cycleDataMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_CYCLING_DATA);
                cycleDataMsg.setCyclingDataOperateType(CyclingData.CYCLING_DATA_OPERATE_TYPE.enum_CYCLING_DATA_OPERATE_TYPE_FILE_GET);
                cycleDataMsg.addCyclingDataFileFlagMsg( CyclingData.cycling_data_file_flag_message.newBuilder().setTimestamp(downloadingFile.getTimeStamp()) );
                byte[] cycleDataMsgBytes = IGPSportDeviceSupport.craftData(cycleDataMsg.getServiceType().getNumber(), 0xff, cycleDataMsg.getCyclingDataOperateType().getNumber(), cycleDataMsg.build().toByteArray(), true);

                builder.write(support.writeCharacteristicThird, cycleDataMsgBytes);
                builder.queue(support.getQueue());
//            } else {
//                syncNextFile();
//            }

        }

        public void startDownload() {
            recievingDataBuffer.reset();
            downloadInProgress = true;
        }

        public void addData(byte[] data) {
            try {
                recievingDataBuffer.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            LOG.debug("current data stored size: " + recievingDataBuffer.size() + " need " + downloadingFile.getFileSize());

            if (recievingDataBuffer.size() > downloadingFile.getFileSize()) {
                //LOG.info(GB.hexdump(recievingDataBuffer.toByteArray()));
                downloadInProgress = false;

                File dir;
                File outputFile;

                try {
                    dir = support.getWritableExportDirectory();
                    outputFile = new File(dir, downloadingFile.getFileName());
                    FileUtils.copyStreamToFile(new ByteArrayInputStream(recievingDataBuffer.toByteArray(), 28, recievingDataBuffer.size()-28), outputFile);
                    outputFile.setLastModified(downloadingFile.getTimeStamp()* 1000L);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                syncNextFile();

            }
        }

        public Boolean needMoreData() {
            if (!downloadInProgress)
                return false;
            if (recievingDataBuffer.size() < downloadingFile.getFileSize()) {
                return true;
            } else {
                return false;
            }
        }

        public static class FileInfo {
            private int timestamp = 0;
            private int file_size = 0;
            private String user_id = "";
            private String device_id = "";
            public FileInfo(CyclingData.cycling_data_file_flag_message message) {
                timestamp = message.getTimestamp();
                file_size = message.getFileSize();
                user_id = message.getUserId();
                device_id = message.getDeviceId();
            }

            public int getTimeStamp() {
                return timestamp;
            }

            public String getFileName() {
                long timestampMillis = timestamp * 1000L;
                Instant instant = Instant.ofEpochMilli(timestampMillis);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
                return zonedDateTime.format(formatter)+".fit";
            }

            public int getFileSize() {
                return file_size;
            }
        }



}
