/*  Copyright (C) 2024-2025 Daniele Gobbetti, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.CreateFileMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.DownloadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FileTransferDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SystemEventMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.UploadRequestMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.CreateFileStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.DownloadRequestStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FileTransferDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.UploadRequestStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FileTransferHandler implements MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandler.class);
    private final GarminSupport deviceSupport;
    private final Download download;
    private final Upload upload;

    private static final Set<FileType.FILETYPE> FILE_TYPES_TO_PROCESS = new HashSet<FileType.FILETYPE>() {{
        add(FileType.FILETYPE.DIRECTORY);
        add(FileType.FILETYPE.ACTIVITY);
        add(FileType.FILETYPE.MONITOR);
        add(FileType.FILETYPE.METRICS);
        add(FileType.FILETYPE.CHANGELOG);
        add(FileType.FILETYPE.HRV_STATUS);
        add(FileType.FILETYPE.SLEEP);
        add(FileType.FILETYPE.SKIN_TEMP);
    }};

    public FileTransferHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
        this.download = new Download();
        this.upload = new Upload();
    }

    public boolean isDownloading() {
        return download.getCurrentlyDownloading() != null;
    }

    public boolean isUploading() {
        return upload.getCurrentlyUploading() != null;
    }

    @Override
    public GFDIMessage handle(GFDIMessage message) {
        if (message instanceof DownloadRequestStatusMessage)
            download.processDownloadRequestStatusMessage((DownloadRequestStatusMessage) message);
        else if (message instanceof FileTransferDataMessage)
            download.processDownloadChunkedMessage((FileTransferDataMessage) message);
        else if (message instanceof CreateFileStatusMessage)
            return upload.setCreateFileStatusMessage((CreateFileStatusMessage) message);
        else if (message instanceof UploadRequestStatusMessage)
            return upload.setUploadRequestStatusMessage((UploadRequestStatusMessage) message);
        else if (message instanceof FileTransferDataStatusMessage)
            return upload.processUploadProgress((FileTransferDataStatusMessage) message);

        return null;
    }

    public DownloadRequestMessage downloadDirectoryEntry(DirectoryEntry directoryEntry) {
        download.setCurrentlyDownloading(new FileFragment(directoryEntry));
        return new DownloadRequestMessage(directoryEntry.getFileIndex(), 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
    }

    public DownloadRequestMessage initiateDownload() {
        download.setCurrentlyDownloading(new FileFragment(new DirectoryEntry(0, FileType.FILETYPE.DIRECTORY, 0, 0, 0, 0, null)));
        return new DownloadRequestMessage(0, 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
    }

    public DownloadRequestMessage initiateDebugDownload() {
        DirectoryEntry deviceXml = new DirectoryEntry(0xFFFD, FileType.FILETYPE.DEVICE_XML, 0xFFFD, 0, 0, 0, new Date());
        download.setCurrentlyDownloading(new FileFragment(deviceXml));
        return new DownloadRequestMessage(deviceXml.getFileIndex(), 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
    }

//    public DownloadRequestMessage downloadSettings() {
//        download.setCurrentlyDownloading(new FileFragment(new DirectoryEntry(0, FileType.FILETYPE.SETTINGS, 0, 0, 0, 0, null)));
//        return new DownloadRequestMessage(0, 0, DownloadRequestMessage.REQUEST_TYPE.NEW, 0, 0);
//    }
//
public CreateFileMessage initiateUpload(byte[] fileAsByteArray, FileType.FILETYPE filetype) {
    upload.setCurrentlyUploading(new FileFragment(new DirectoryEntry(0, filetype, 0, 0, 0, fileAsByteArray.length, null), fileAsByteArray));
    return new CreateFileMessage(fileAsByteArray.length, filetype);
}


    public class Download {
        private FileFragment currentlyDownloading;

        public FileFragment getCurrentlyDownloading() {
            return currentlyDownloading;
        }

        public void setCurrentlyDownloading(FileFragment currentlyDownloading) {
            this.currentlyDownloading = currentlyDownloading;
        }

        private void processDownloadChunkedMessage(FileTransferDataMessage fileTransferDataMessage) {
            if (!isDownloading())
                throw new IllegalStateException("Received file transfer of unknown file");

            currentlyDownloading.append(fileTransferDataMessage);
            if (!currentlyDownloading.dataHolder.hasRemaining())
                processCompleteDownload();
            else
                deviceSupport.onFileDownloadProgress(currentlyDownloading.dataHolder.position());
        }

        private void processCompleteDownload() {
            currentlyDownloading.dataHolder.flip();

            if (FileType.FILETYPE.DIRECTORY.equals(currentlyDownloading.directoryEntry.filetype)) { //is a directory
                parseDirectoryEntries();
            } else {
                saveFileToExternalStorage();
            }

            currentlyDownloading = null;
        }

        public void processDownloadRequestStatusMessage(DownloadRequestStatusMessage downloadRequestStatusMessage) {
            if (null == currentlyDownloading)
                throw new IllegalStateException("Received file transfer of unknown file");
            if (downloadRequestStatusMessage.canProceed())
                currentlyDownloading.setSize(downloadRequestStatusMessage);
            else {
                // Signal to the support class that the download failed so it can also continue to the next one
                FileDownloadedDeviceEvent fileDownloadedDeviceEvent = new FileDownloadedDeviceEvent();
                fileDownloadedDeviceEvent.directoryEntry = currentlyDownloading.directoryEntry;
                fileDownloadedDeviceEvent.success = false;
                deviceSupport.evaluateGBDeviceEvent(fileDownloadedDeviceEvent);
                currentlyDownloading = null;
            }
        }

        private void saveFileToExternalStorage() {
            File deviceDir;
            File outputFile;
            try {
                deviceDir = deviceSupport.getWritableExportDirectory();
                outputFile = new File(deviceDir, currentlyDownloading.directoryEntry.getOutputPath());
                final File parentFile = outputFile.getParentFile();
                parentFile.mkdirs();
                FileUtils.copyStreamToFile(new ByteArrayInputStream(currentlyDownloading.dataHolder.array()), outputFile);
                outputFile.setLastModified(currentlyDownloading.directoryEntry.fileDate.getTime());
            } catch (final IOException e) {
                LOG.error("Failed to save file", e);
                return; // do not signal file as saved
            }

            FileDownloadedDeviceEvent fileDownloadedDeviceEvent = new FileDownloadedDeviceEvent();
            fileDownloadedDeviceEvent.directoryEntry = currentlyDownloading.directoryEntry;
            fileDownloadedDeviceEvent.localPath = outputFile.getAbsolutePath();
            deviceSupport.evaluateGBDeviceEvent(fileDownloadedDeviceEvent);
        }

        private void parseDirectoryEntries() {
            LOG.debug("Parsing directory entries for {}", currentlyDownloading.directoryEntry);
            if (deviceSupport.newSyncProtocol()) {
                // Signal to the support class that we got the directory - but ignore the entries
                // Well request them using the new sync protocol
                deviceSupport.addFileToDownloadList(currentlyDownloading.directoryEntry);
                return;
            }

            if ((currentlyDownloading.getDataSize() % 16) != 0)
                throw new IllegalArgumentException("Invalid directory data length");
            final GarminByteBufferReader reader = new GarminByteBufferReader(currentlyDownloading.dataHolder.array());
            reader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            final boolean fetchUnknownFiles = deviceSupport.getDevicePrefs().getFetchUnknownFiles();
            while (reader.remaining() > 0) {
                final int fileIndex = reader.readShort();//2
                final int fileDataType = reader.readByte();//3
                final int fileSubType = reader.readByte();//4
                final FileType.FILETYPE filetype = FileType.FILETYPE.fromDataTypeSubType(fileDataType, fileSubType);
                final int fileNumber = reader.readShort();//6
                final int specificFlags = reader.readByte();//7
                final int fileFlags = reader.readByte();//8
                final int fileSize = reader.readInt();//12
                final Date fileDate = new Date(GarminTimeUtils.garminTimestampToJavaMillis(reader.readInt()));//16
                final DirectoryEntry directoryEntry = new DirectoryEntry(fileIndex, filetype, fileNumber, specificFlags, fileFlags, fileSize, fileDate);
                if (directoryEntry.filetype == null) {
                    // discard unsupported files
                    LOG.warn("Unsupported directory entry of type {}/{}: {}", fileDataType, fileSubType, directoryEntry);
                    continue;
                }
                if (!FILE_TYPES_TO_PROCESS.contains(directoryEntry.filetype) && !fetchUnknownFiles) {
                    LOG.debug("Skipping directory entry: {}", directoryEntry);
                    continue;
                }
                if (fileIndex == 0 && fileDataType == 0 && fileSubType == 0 && fileNumber == 0 && specificFlags == 0 && fileFlags == 0 && fileSize == 0) {
                    LOG.warn("Ignoring {} to avoid infinite loop", directoryEntry);
                    continue;
                }
                LOG.debug("Queueing {} for download", directoryEntry);
                deviceSupport.addFileToDownloadList(directoryEntry);
            }
            currentlyDownloading = null;
        }
    }

    private void updateUploadProgress(final int percentage) {
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(GBApplication.getContext());

        if (percentage < 0) {
            // Failure
            GB.updateInstallNotification(GBApplication.getContext().getString(R.string.installation_failed_), false, 100, GBApplication.getContext());
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_INFO_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, ""));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, GBApplication.getContext().getString(R.string.installation_failed_)));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_FINISHED));
        } else if (percentage >= 100) {
            // Success
            GB.updateInstallNotification(GBApplication.getContext().getString(R.string.installation_successful), false, 100, GBApplication.getContext());

            deviceSupport.getDevice().sendDeviceUpdateIntent(deviceSupport.getContext());

            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_INFO_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, ""));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, GBApplication.getContext().getString(R.string.installation_successful)));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_FINISHED));
        } else {
            // In Progress
            GB.updateInstallNotification(GBApplication.getContext().getString(R.string.uploading), true, percentage, GBApplication.getContext());

            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_INFO_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, ""));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, GBApplication.getContext().getString(R.string.uploading)));
            broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_PROGRESS, percentage));
        }
    }

    public class Upload {
        private FileFragment currentlyUploading;

        private UploadRequestMessage setCreateFileStatusMessage(CreateFileStatusMessage createFileStatusMessage) {
            if (createFileStatusMessage.canProceed()) {
                LOG.info("SENDING UPLOAD FILE");
                if (currentlyUploading.directoryEntry.filetype != FileType.FILETYPE.SETTINGS) {
                    updateUploadProgress(0);
                }
                return new UploadRequestMessage(createFileStatusMessage.getFileIndex(), currentlyUploading.getDataSize());
            } else {
                LOG.warn("Cannot proceed with upload");
                this.currentlyUploading = null;
            }
            return null;
        }

        private FileTransferDataMessage setUploadRequestStatusMessage(UploadRequestStatusMessage uploadRequestStatusMessage) {
            if (null == currentlyUploading)
                throw new IllegalStateException("Received upload request status transfer of unknown file");
            if (uploadRequestStatusMessage.canProceed()) {
                if (uploadRequestStatusMessage.getDataOffset() != currentlyUploading.dataHolder.position())
                    throw new IllegalStateException("Received upload request with unaligned offset");
                return currentlyUploading.take();
            } else {
                LOG.warn("Cannot proceed with upload");
                if (currentlyUploading.directoryEntry.filetype != FileType.FILETYPE.SETTINGS) {
                    updateUploadProgress(-1);
                }
                this.currentlyUploading = null;
            }
            return null;
        }

        private GFDIMessage processUploadProgress(FileTransferDataStatusMessage fileTransferDataStatusMessage) {
            final boolean showNotification = currentlyUploading.directoryEntry.filetype != FileType.FILETYPE.SETTINGS;

            if (currentlyUploading.getDataSize() <= fileTransferDataStatusMessage.getDataOffset()) {
                this.currentlyUploading = null;
                LOG.info("SENDING SYNC COMPLETE!!!");
                if (showNotification) {
                    updateUploadProgress(100);
                }

                return new SystemEventMessage(SystemEventMessage.GarminSystemEventType.SYNC_COMPLETE, 0);
            } else {
                if (fileTransferDataStatusMessage.canProceed()) {
                    LOG.info("SENDING NEXT CHUNK!!!");
                    if (showNotification) {
                        updateUploadProgress((100 * currentlyUploading.dataHolder.position()) / currentlyUploading.dataHolder.limit());
                    }
                    if (fileTransferDataStatusMessage.getDataOffset() != currentlyUploading.dataHolder.position())
                        throw new IllegalStateException("Received file transfer status with unaligned offset");
                    return currentlyUploading.take();
                } else {
                    LOG.warn("Cannot proceed with upload");
                    updateUploadProgress(-1);
                    this.currentlyUploading = null;
                }

            }
            return null;
        }

        public FileFragment getCurrentlyUploading() {
            return this.currentlyUploading;
        }

        public void setCurrentlyUploading(FileFragment currentlyUploading) {
            this.currentlyUploading = currentlyUploading;
        }

    }

    public static class FileFragment {
        private final DirectoryEntry directoryEntry;
        private final int maxBlockSize = 500; //TODO: why 500?
        private int dataSize;
        private ByteBuffer dataHolder;
        private int runningCrc;

        FileFragment(DirectoryEntry directoryEntry) {
            this.directoryEntry = directoryEntry;
            this.setRunningCrc(0);
        }

        FileFragment(DirectoryEntry directoryEntry, byte[] contents) {
            this.directoryEntry = directoryEntry;
            this.setDataSize(contents.length);
            this.dataHolder = ByteBuffer.wrap(contents);
            this.dataHolder.flip(); //we'll be only reading from here on
            this.dataHolder.compact();
            this.setRunningCrc(0);
        }

        private int getMaxBlockSize() {
            return Math.min(maxBlockSize, GFDIMessage.getMaxPacketSize()); //TODO: can we use GFDIMessage.getMaxPacketSize() directly?
        }

        private void setSize(DownloadRequestStatusMessage downloadRequestStatusMessage) {
            if (0 != getDataSize())
                throw new IllegalStateException("Data size already set");

            this.setDataSize(downloadRequestStatusMessage.getMaxFileSize());
            this.dataHolder = ByteBuffer.allocate(getDataSize());
        }

        private void append(FileTransferDataMessage fileTransferDataMessage) {
            if (fileTransferDataMessage.getDataOffset() != dataHolder.position())
                throw new IllegalStateException("Received message that was already received");

            final int dataCrc = ChecksumCalculator.computeCrc(getRunningCrc(), fileTransferDataMessage.getMessage(), 0, fileTransferDataMessage.getMessage().length);
            if (fileTransferDataMessage.getCrc() != dataCrc)
                throw new IllegalStateException("Received message with invalid CRC");
            setRunningCrc(dataCrc);

            this.dataHolder.put(fileTransferDataMessage.getMessage());
        }

        private FileTransferDataMessage take() {
            final int currentOffset = this.dataHolder.position();
            final byte[] chunk = new byte[Math.min(this.dataHolder.remaining(), getMaxBlockSize() - 13)]; //actual payload in FileTransferDataMessage
            this.dataHolder.get(chunk);
            setRunningCrc(ChecksumCalculator.computeCrc(getRunningCrc(), chunk, 0, chunk.length));
            return new FileTransferDataMessage(chunk, currentOffset, getRunningCrc());
        }

        private int getDataSize() {
            return dataSize;
        }

        private void setDataSize(int dataSize) {
            this.dataSize = dataSize;
        }

        private int getRunningCrc() {
            return runningCrc;
        }

        private void setRunningCrc(int runningCrc) {
            this.runningCrc = runningCrc;
        }
    }

    public static class DirectoryEntry {
        private static final SimpleDateFormat SDF_FULL = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
        private static final SimpleDateFormat SDF_YEAR = new SimpleDateFormat("yyyy", Locale.ROOT);

        private final int fileIndex;
        private final FileType.FILETYPE filetype;
        private final int fileNumber;
        private final int specificFlags;
        private final int fileFlags;
        private final int fileSize;
        private final Date fileDate;

        public DirectoryEntry(int fileIndex, FileType.FILETYPE filetype, int fileNumber, int specificFlags, int fileFlags, int fileSize, Date fileDate) {
            this.fileIndex = fileIndex;
            this.filetype = filetype;
            this.fileNumber = fileNumber;
            this.specificFlags = specificFlags;
            this.fileFlags = fileFlags;
            this.fileSize = fileSize;
            this.fileDate = fileDate;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public FileType.FILETYPE getFiletype() {
            return filetype;
        }

        public Date getFileDate() {
            return fileDate;
        }

        public int getFileSize() {
            return fileSize;
        }

        /**
         * Builds the output path.
         * Format: [FILE_TYPE]/[YEAR]/[FILE_TYPE]_[yyyy-MM-dd_HH-mm-ss]_[INDEX].[fit/bin]
         */
        public String getOutputPath() {
            // [FILE_TYPE]/
            final StringBuilder sb = new StringBuilder(getFiletype().name());
            sb.append(File.separator);

            // If we have a valid date, place the file inside a folder for each year
            // [YEAR]/
            if (fileDate.getTime() != GarminTimeUtils.GARMIN_TIME_EPOCH * 1000L) {
                sb.append(SDF_YEAR.format(fileDate));
                sb.append(File.separator);
            }

            // Finally, the filename
            sb.append(getFileName());
            return sb.toString();
        }

        /**
         * [FILE_TYPE]_[yyyy-MM-dd_HH-mm-ss]_[INDEX].[fit/bin]
         */
        public String getFileName() {
            final StringBuilder sb = new StringBuilder(getFiletype().name());
            if (fileDate.getTime() != GarminTimeUtils.GARMIN_TIME_EPOCH * 1000L) {
                sb.append("_").append(SDF_FULL.format(fileDate));
            }
            sb.append("_").append(getFileIndex()).append(getFiletype().isFitFile() ? ".fit" : ".bin");
            return sb.toString();
        }

        @NonNull
        @Override
        public String toString() {
            return "DirectoryEntry{" +
                    "fileIndex=" + fileIndex +
                    ", fileType=" + filetype +
                    ", fileNumber=" + fileNumber +
                    ", specificFlags=" + specificFlags +
                    ", fileFlags=" + fileFlags +
                    ", fileSize=" + fileSize +
                    ", fileDate=" + fileDate +
                    '}';
        }
    }
}
