/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload.FileUploadParams;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class HuaweiUploadManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiUploadManager.class);

    public interface UploadData {
        int getDataSize();
        byte[] getSHA256();
        byte[] getDataChunk(int pos, int size);
    }

    public static class UploadDataBuffer implements UploadData {
        private final int dataSize;
        private final byte[] data;
        private byte[] dataSHA256 = null;

        public UploadDataBuffer(byte[] data) {
            this.dataSize = data.length;
            this.data = data;

            try {
                MessageDigest m = MessageDigest.getInstance("SHA256");
                m.update(data, 0, data.length);
                this.dataSHA256 =  m.digest();
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Digest algorithm not found.", e);
            }
        }

        @Override
        public int getDataSize() {
            return dataSize;
        }

        @Override
        public byte[] getSHA256() {
            return dataSHA256;
        }

        @Override
        public byte[] getDataChunk(int pos, int size) {
            byte[] ret = new byte[size];
            System.arraycopy(data, pos, ret, 0, size);
            return ret;
        }
    }

    public static class UploadDataFile implements UploadData {
        private final UriHelper uriHelper;
        private byte[] dataSHA256 = null;

        public UploadDataFile(UriHelper uriHelper) {
            this.uriHelper = uriHelper;

            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA256");
                InputStream inStream = uriHelper.openInputStream();
                int n;
                byte[] buf = new byte[4096];
                while ((n = inStream.read(buf)) != -1) {
                    if (n > 0) {
                        sha256.update(buf, 0, n);
                    }
                }
                inStream.close();
                dataSHA256 = sha256.digest();
            } catch (Exception e) {
                LOG.error("Error calculate checksum", e);
            }
        }

        @Override
        public int getDataSize() {
            return (int) uriHelper.getFileSize();
        }

        @Override
        public byte[] getSHA256() {
            return dataSHA256;
        }

        @Override
        public byte[] getDataChunk(int pos, int size) {
            try {
                InputStream inStream = uriHelper.openInputStream();
                byte[] buf = new byte[size];
                int n = (int) inStream.skip(pos);
                if (n == pos) {
                    int k = inStream.read(buf);
                    if(k == size){
                        return buf;
                    }
                }
                LOG.error("File read error");
                inStream.close();
            } catch (Exception e) {
                LOG.error("Read file exception", e);
            }
            return null;
        }
    }

    public interface FileUploadCallback {
        void onUploadStart();
        void onUploadProgress(int progress);
        void onUploadComplete();
        void onError(int code);
    }

    public static class FileUploadInfo {

        UploadData uploadData;
        private byte fileType = 1; // 1 - watchface, 2 - music, 3 - png for background , 7 - app
        private byte fileId = 0; // get on incoming (2803)

        private String fileName = ""; //FIXME generate random name

        private String srcPackage = null;
        private String dstPackage = null;
        private String srcFingerprint = null;
        private String dstFingerprint = null;

        private int currentUploadPosition = 0;
        private int uploadChunkSize = 0;

        private FileUploadCallback fileUploadCallback = null;

        //ack values set from 28 4 response
        private FileUploadParams fileUploadParams = null;


        public FileUploadCallback getFileUploadCallback() {
            return fileUploadCallback;
        }

        public void setFileUploadCallback(FileUploadCallback fileUploadCallback) {
            this.fileUploadCallback = fileUploadCallback;
        }

        public void setFileUploadParams(FileUploadParams params) {
            this.fileUploadParams = params;
        }

        public int getUnitSize() {
            return fileUploadParams.unit_size;
        }

        public boolean getEncrypt() {
            return fileUploadParams.no_encrypt == 0;
        }

        public void setUploadData(UploadData uploadData) {
            this.uploadData = uploadData;

            currentUploadPosition = 0;
            uploadChunkSize = 0;
            LOG.info("File ready for upload, SHA256: {} fileName: {} filetype: {}", GB.hexdump(uploadData.getSHA256()), fileName, fileType);
        }

        public int getFileSize() {
            return uploadData.getDataSize();
        }

        public String getFileName() {
            return this.fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public byte getFileType() {
            return this.fileType;
        }

        public void setFileType(byte fileType) {
            this.fileType = fileType;
        }

        public byte getFileId() {
            return fileId;
        }

        public void setFileId(byte fileId) {
            this.fileId = fileId;
        }

        public String getSrcPackage() { return srcPackage; }

        public void setSrcPackage(String srcPackage) { this.srcPackage = srcPackage; }

        public String getDstPackage() { return dstPackage; }

        public void setDstPackage(String dstPackage) { this.dstPackage = dstPackage; }

        public String getSrcFingerprint() { return srcFingerprint; }

        public void setSrcFingerprint(String srcFingerprint) { this.srcFingerprint = srcFingerprint; }

        public String getDstFingerprint() { return dstFingerprint;}

        public void setDstFingerprint(String dstFingerprint) { this.dstFingerprint = dstFingerprint;}

        public byte[] getFileSHA256() {
            return uploadData.getSHA256();
        }

        public void setUploadChunkSize(int chunkSize) {
            uploadChunkSize = chunkSize;
        }

        public void setCurrentUploadPosition (int pos) {
            currentUploadPosition = pos;
        }

        public int getCurrentUploadPosition() {
            return currentUploadPosition;
        }

        public byte[] getCurrentChunk() {
            return uploadData.getDataChunk(currentUploadPosition, uploadChunkSize);
        }
    }

    private final HuaweiSupportProvider support;

    FileUploadInfo fileUploadInfo = null;

    public HuaweiUploadManager(HuaweiSupportProvider support) {
        this.support=support;
    }

    public FileUploadInfo getFileUploadInfo() {
        return fileUploadInfo;
    }

    public void setFileUploadInfo(FileUploadInfo fileUploadInfo) {
        this.fileUploadInfo = fileUploadInfo;
    }

    public void setDeviceBusy() {
        final GBDevice device = support.getDevice();
        if(fileUploadInfo != null && fileUploadInfo.fileType == FileUpload.Filetype.watchface) {
            device.setBusyTask(R.string.uploading_watchface, support.getContext());
        } else {
            device.setBusyTask(R.string.updating_firmware, support.getContext());
        }
        device.sendDeviceUpdateIntent(support.getContext());
    }

    public void unsetDeviceBusy() {
        final GBDevice device = support.getDevice();
        if (device != null && device.isConnected()) {
            if (device.isBusy()) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(support.getContext());
            }
            device.sendDeviceUpdateIntent(support.getContext());
        }
    }


}
