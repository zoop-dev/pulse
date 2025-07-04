/*  Copyright (C) 2025 Me7c7

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

import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.ota.HuaweiOTAFileList;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTADataChunkRequestAck;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTADataParamsRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTADeviceRequestReply;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTAFileChunk;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTAGetMode;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTANotifyNewVersion;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTAProgress;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTASetStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTAStartQuery;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendOTAUploadResultAck;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class HuaweiOTAManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiOTAManager.class);
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);

    public static class UploadInfo {
        public int waitTimeout;
        public int restartTimeout;
        public int maxUnitSize;
        public long interval = 0;
        public boolean ack = false;
        public boolean offset = false;
    }

    static class FirmwareCheckHandler extends Handler {
        private final WeakReference<HuaweiOTAManager> otaManager;

        FirmwareCheckHandler(HuaweiOTAManager otaManager) {
            super();
            this.otaManager = new WeakReference<>(otaManager);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            HuaweiOTAManager manager = otaManager.get();
            if (manager == null)
                return;
            switch (msg.what) {
                case 1:
                    Integer val = (Integer) msg.obj;
                    manager.onFwCheckProgress(val);
                    break;
                case 2:
                    File fwFile = (File) msg.obj;
                    manager.onFwCheckSuccess(fwFile);
                    break;
                case 3:
                    manager.onFwCheckFail();
                    break;
                default:
                    LOG.warn("Unknown what in message: {}", msg.what);
                    break;
            }
        }
    }


    private final HuaweiSupportProvider support;


    private final FirmwareCheckHandler firmwareCheckHandler = new FirmwareCheckHandler(this);

    private int downloadProgress = 0;


    private int state = 0;

    private HuaweiOTAFileList.OTAFileInfo fwInfo;
    private Uri uri;

    private File fwFile = null;
    private FileInputStream fwInputStream = null;

    private UploadInfo currentUploadInfo = null;

    public HuaweiOTAManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    private void cleanup() {
        if (fwInputStream != null) {
            try {
                fwInputStream.close();
            } catch (IOException e) {
                LOG.error("Error close input stream", e);
            }
            fwInputStream = null;
        }
        if (this.fwFile != null) {
            this.fwFile.delete();
            this.fwFile = null;
        }
        this.fwInfo = null;
        this.uri = null;
        currentUploadInfo = null;
        this.state = 0;
        this.downloadProgress = 0;
        unsetDeviceBusy();
    }

    public void startFwUpdate(HuaweiOTAFileList.OTAFileInfo fwInfo, Uri uri) {
        this.fwInfo = fwInfo;
        this.uri = uri;
        this.state = 1;
        if (fwInfo == null || uri == null) {
            cleanup();
            return;
        }
        if (!support.getHuaweiCoordinator().supportsOTAUpdate()) {
            LOG.info("OTA update is not supported");
            cleanup();
            return;
        }
        setDeviceBusy();

        if (support.getHuaweiCoordinator().supportsOTANotify()) {
            try {
                SendOTANotifyNewVersion notifyNewVersion = new SendOTANotifyNewVersion(support, this.fwInfo.versionName, this.fwInfo.size, (byte) 1, (byte) 2);
                notifyNewVersion.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTANotifyNewVersion", e);
            }
        } else {
            try {
                SendOTAStartQuery sendOTAStartQuery = new SendOTAStartQuery(support, this.fwInfo.versionName, (short) 256, (byte) 2, false);
                sendOTAStartQuery.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAStartQuery", e);
            }
        }
    }


    private void startFWCheck(HuaweiOTAFileList.OTAFileInfo fwInfo, Uri uri) {
        downloadProgress = 0;
        if (support.getHuaweiCoordinator().supportsOTADeviceRequest()) {
            try {
                SendOTAProgress progressReq = new SendOTAProgress(support, (byte) 0, (byte) 0, (byte) 0);
                progressReq.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAProgress", e);
            }
        }
        new Thread(new Runnable() {

            private void sendMessage(int what, Object obj) {
                Message msg = Message.obtain();
                msg.obj = obj;
                msg.what = what;
                firmwareCheckHandler.sendMessage(msg);
            }

            @Override
            public void run() {
                File fwFile = null;
                int lastProgress = 0;
                try {
                    sendMessage(1, lastProgress);

                    final UriHelper uriHelper = UriHelper.get(uri, support.getContext());
                    File outputDir = support.getContext().getCacheDir();
                    fwFile = new File(outputDir, fwInfo.dpath);

                    FileOutputStream outputStream = new FileOutputStream(fwFile);
                    ZipInputStream stream = new ZipInputStream(uriHelper.openInputStream());
                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (entry.getName().equals(fwInfo.dpath)) {
                            int n;
                            byte[] buf = new byte[1024];
                            int cnt = 0;
                            while ((n = stream.read(buf, 0, buf.length)) != -1) {
                                if (n > 0) {
                                    outputStream.write(buf, 0, n);
                                    cnt += n;
                                    int progress = (int) ((((double) cnt / fwInfo.size) / 2.0d) * 100.0d);
                                    if (progress > lastProgress) {
                                        lastProgress = progress;
                                        sendMessage(1, lastProgress);
                                    }
                                }
                            }
                            break;
                        }
                    }
                    stream.close();

                    LOG.info("Firmware size: {} -- {}", fwFile.length(), fwInfo.size);

                    if (fwFile.length() != fwInfo.size) {
                        throw new Exception("Size is not correct");
                    }

                    sendMessage(1, 49);

                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    MessageDigest sha256 = MessageDigest.getInstance("SHA256");

                    FileInputStream inStream = new FileInputStream(fwFile);
                    int n;
                    int cnt = 0;
                    byte[] buf = new byte[1024];
                    while ((n = inStream.read(buf)) != -1) {
                        if (n > 0) {
                            md5.update(buf, 0, n);
                            sha256.update(buf, 0, n);
                            cnt += n;
                            int progress = 49 + (int) ((((double) cnt / fwInfo.size) / 2.0d) * 100.0d);
                            if (progress > lastProgress) {
                                lastProgress = progress;
                                sendMessage(1, lastProgress);
                            }
                        }
                    }
                    inStream.close();

                    byte[] md5hash = md5.digest();
                    byte[] sha256hash = sha256.digest();
                    LOG.info("md5: {} -- {}", GB.hexdump(md5hash), fwInfo.md5);
                    LOG.info("sha256: {} -- {}", GB.hexdump(sha256hash), fwInfo.sha256);
                    if (!GB.hexdump(md5hash).equals(fwInfo.md5) || !GB.hexdump(sha256hash).equals(fwInfo.sha256)) {
                        throw new Exception("Hash mismatch");
                    }

                    sendMessage(1, 100);
                    sendMessage(2, fwFile);

                } catch (Exception e) {
                    LOG.error("Check firmware: Error occurred", e);
                    if (fwFile != null) {
                        fwFile.delete();
                    }
                    sendMessage(3, null);
                }
            }
        }, "HuaweiOTAManager_" + THREAD_COUNTER.getAndIncrement()).start();
    }

    private short calculateFwFileId() throws Exception {
        FileInputStream inStream = new FileInputStream(this.fwFile);
        byte[] buf = new byte[8];
        int n = inStream.read(buf);
        inStream.close();
        if (n < 0) {
            throw new Exception("invalid data");
        }

        ByteBuffer buffer = ByteBuffer.wrap(buf, 4, 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int data = buffer.getInt();

        int signature = 256;
        if (support.getHuaweiCoordinator().supportsOTASignature()) {
            signature = support.getHuaweiCoordinator().getOtaSignatureLength();
        }
        LOG.info("data: {}, signature: {}", data, signature);
        return (short) (data + signature);
    }

    private byte[] getFileChunk(long offset, long len) {
        if (this.fwFile == null || !this.fwFile.exists()) {
            LOG.error("no file");
            return null;
        }
        if (this.fwInputStream == null) {
            try {
                fwInputStream = new FileInputStream(this.fwFile);
            } catch (IOException e) {
                LOG.error("error open file", e);
                return null;
            }
        }
        try {
            ByteBuffer buf = ByteBuffer.allocate((int) len);
            int n = fwInputStream.getChannel().read(buf, offset);
            if (n >= 0) {
                return Arrays.copyOfRange(buf.array(), 0, n);
            }
            LOG.error("File read error");
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Read file exception", e);
        }
        return null;
    }

    private void startFWUpload() {
        short fileId;
        try {
            fileId = calculateFwFileId();
        } catch (Exception e) {
            LOG.error("Error getting ID");
            cleanup();
            return;
        }

        setUploadProgress(0, true);

        try {
            SendOTAStartQuery sendOTAStartQuery = new SendOTAStartQuery(support, fwInfo.versionName, fileId, (byte) 0, true);
            sendOTAStartQuery.doPerform();
        } catch (IOException e) {
            LOG.error("Error send SendOTAStartQuery", e);
        }

    }

    public void onFwCheckFail() {
        LOG.info("onFwCheckFail");
        if (support.getHuaweiCoordinator().supportsOTADeviceRequest()) {
            try {
                SendOTAProgress progressReq = new SendOTAProgress(support, (byte) downloadProgress, (byte) 2, (byte) 0);
                progressReq.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAProgress", e);
            }
        }
        setCheckingFailed();
        cleanup();
    }

    public void onFwCheckSuccess(File fwFile) {
        LOG.info("onFwCheckSuccess: {}", fwFile.getAbsoluteFile());

        setCheckingProgress(100, false);

        this.fwFile = fwFile;

        this.state = 3;

        if (support.getHuaweiCoordinator().supportsOTADeviceRequest()) {
            try {
                SendOTAGetMode sendOTAGetMode = new SendOTAGetMode(support);
                sendOTAGetMode.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAGetMode", e);
            }
        } else {
            // start upload
            startFWUpload();
        }
    }

    public void onFwCheckProgress(int val) {
        LOG.info("onFwCheckProgress: {}", val);
        downloadProgress = val;
        if (support.getHuaweiCoordinator().supportsOTADeviceRequest()) {
            try {
                SendOTAProgress progressReq = new SendOTAProgress(support, (byte) downloadProgress, (byte) 1, (byte) 0);
                progressReq.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAProgress", e);
            }
        }
        if (downloadProgress % 10 == 0) {
            setCheckingProgress(downloadProgress, true);
        }
    }

    public void handleStartQueryResponse(int respCode, byte batteryThreshold) {
        //109025
        if (respCode != 100000) {
            LOG.error("ERROR");
            cleanup();
            return;
        }
        if (this.state == 1) {
            this.state = 2;
            if (support.getHuaweiCoordinator().supportsOTANotify()) {
                try {
                    SendOTANotifyNewVersion notifyNewVersion = new SendOTANotifyNewVersion(support, fwInfo.versionName, fwInfo.size, (byte) 1, (byte) 0);
                    notifyNewVersion.doPerform();
                } catch (IOException e) {
                    LOG.error("Error send SendOTANotifyNewVersion", e);
                }
            } else {
                startFWCheck(this.fwInfo, this.uri);

            }
        } else if (this.state == 3) {
            try {
                SendOTADataParamsRequest sendOTADataParamsRequest = new SendOTADataParamsRequest(support);
                sendOTADataParamsRequest.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTADataParamsRequest", e);
            }
        }
    }

    private List<Integer> decodeBitmap(byte[] bitmap) {
        if (bitmap == null || bitmap.length == 0) {
            return null;
        }
        ArrayList<Integer> ret = new ArrayList<>();
        for (byte c : bitmap) {
            for(int i = 0; i< 8; i++) {
                ret.add((c >> i) & 1);
            }
        }
        return ret;
    }

    void handleDataChunkRequest(OTA.DataChunkRequest.Response response) {
        if (response.errorCode != null) {
            LOG.error("error");
            return;
        }
        if (currentUploadInfo == null) {
            LOG.error("No upload info");
            cleanup();
            return;
        }
        if (currentUploadInfo.ack) {
            try {
                // NOTE: send exactly same response back
                SendOTADataChunkRequestAck sendOTADataChunkRequestAck = new SendOTADataChunkRequestAck(support, response);
                sendOTADataChunkRequestAck.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTADataChunkRequestAck", e);
            }
        }

        LOG.info("ChunkRequest offset: {} length: {}", response.fileOffset, response.chunkLength);

        if (response.wifiSend > 0) {
            LOG.error("Device reports that firmware send by WIFI. But it is not possible as not supported.");
            cleanup();
            return;
        }

        byte[] data = getFileChunk(response.fileOffset, response.chunkLength);
        if (data == null) {
            LOG.error("No data to upload");
            cleanup();
            return;
        }

        List<Integer> bitmap = decodeBitmap(response.bitmap);

        LOG.info("data len: {} Bitmap: {}", data.length, bitmap);

        try {
            SendOTAFileChunk sendOTAFileChunk = new SendOTAFileChunk(support, data, (int)response.fileOffset, currentUploadInfo.maxUnitSize, currentUploadInfo.offset, bitmap);
            sendOTAFileChunk.doPerform();
        } catch (IOException e) {
            LOG.error("Error send SendOTAFileChunk", e);
        }
    }

    public void handleSizeReport(long size, long current) {
        LOG.info("handleSizeReport: {}, {}", size, current);
    }

    public void handleUploadResult(int resultCode) {
        LOG.info("handleUploadResult: {}", resultCode);
        // 0 - fail, 1 - success
        if (resultCode == 0 || resultCode == 1) {
            try {
                SendOTAUploadResultAck sendOTAUploadResultAck = new SendOTAUploadResultAck(support);
                sendOTAUploadResultAck.doPerform();
            } catch (IOException e) {
                LOG.error("Error send SendOTAUploadResultAck", e);
            }
        }

        cleanup();
        if(resultCode == 1) {
            setUploadComplete();
        } else if(resultCode == 0) {
            setUploadFailed();
        }
    }

    public void handleDeviceError(int errorCode) {
        LOG.info("handleDeviceError: {}", errorCode);
        if (errorCode != 100000) {
            cleanup();
            setUploadFailed();
        }
    }

    public void handleNotifyNewVersionResponse(int respCode) {
        if (respCode != 100000) {
            LOG.error("handleNotifyNewVersionResponse ERROR");
            cleanup();
            return;
        }
        if (this.state == 1) {
            if (support.getHuaweiCoordinator().supportsOTAUpdate()) {
                try {
                    SendOTAStartQuery sendOTAStartQuery = new SendOTAStartQuery(support, fwInfo.versionName, (short) 256, (byte) 2, false);
                    sendOTAStartQuery.doPerform();
                } catch (IOException e) {
                    LOG.error("Error send SendOTAStartQuery", e);
                }
            } else {
                LOG.info("OTA not supported");
            }
        } else if (this.state == 2) {
            startFWCheck(this.fwInfo, this.uri);
        }

    }

    public void handleGetModeResponse(int mode) {
        LOG.info("handleGetModeResponse: {}", mode);
        startFWUpload();
    }

    public void handleDataParamsResponse(UploadInfo info) {
        LOG.info("handleDataParamsResponse: {}", info);
        this.currentUploadInfo = info;
        LOG.info("UploadInfo : waitTimeout: {}, restartTimeout: {}, unitSize: {}, interval: {}, ack: {}, offset: {}", info.waitTimeout, info.restartTimeout, info.maxUnitSize, info.interval, info.ack, info.offset);

        try {
            SendOTASetStatus sendOTASetStatus = new SendOTASetStatus(support);
            sendOTASetStatus.doPerform();
        } catch (IOException e) {
            LOG.error("Error send SendOTASetStatus", e);
        }
    }


    public void handleDeviceRequest(byte unkn1) {
        try {
            int code = 100000;
            if (this.state == 2) {
                code = 109021; // downloading
            } else if (this.state == 3) {
                code = 109022; // uploading
            }
            SendOTADeviceRequestReply sendOTADeviceRequestReply = new SendOTADeviceRequestReply(support, code);
            sendOTADeviceRequestReply.doPerform();
        } catch (IOException e) {
            LOG.error("Could not send response", e);
        }
    }

    public void setDeviceBusy() {
        final GBDevice device = support.getDevice();
        if (device != null && device.isConnected()) {
            device.setBusyTask(R.string.updating_firmware, support.getContext());
            device.sendDeviceUpdateIntent(support.getContext());
        }
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

    public void setCheckingProgress(int progressPercent, boolean ongoing) {
        support.onUploadProgress(R.string.update_firmware_operation_check_in_progress, progressPercent, ongoing);
    }

    public void setCheckingFailed() {
        support.onUploadProgress(R.string.update_firmware_operation_check_failed, 100, false);
    }

    public void setUploadProgress(int progressPercent, boolean ongoing) {
        support.onUploadProgress(R.string.updatefirmwareoperation_update_in_progress, progressPercent, ongoing);
    }

    public void setUploadComplete() {
        support.onUploadProgress(R.string.updatefirmwareoperation_update_complete_rebooting, 100, false);
    }

    public void setUploadFailed() {
        support.onUploadProgress(R.string.updatefirmwareoperation_write_failed, 100, false);
    }
}
