/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.core.content.pm.PackageInfoCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

public class HuaweiP2PAppIcon extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PAppIcon.class);

    private final Queue<String> queue = new LinkedList<>();
    private String currentPackage;

    public static final String MODULE = "hw.unitedevice.notificationpushapp";

    public HuaweiP2PAppIcon(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PAppIcon");
    }

    @Override
    public String getModule() {
        return HuaweiP2PAppIcon.MODULE;
    }

    @Override
    public String getPackage() {
        return "in.huawei.NotificationAppIcon";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    @Override
    public void registered() {
        LOG.info("HuaweiP2PAppIcon registered");
    }

    @Override
    public void unregister() {}

    public static HuaweiP2PAppIcon getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PAppIcon) manager.getRegisteredService(HuaweiP2PAppIcon.MODULE);
    }

    public void sendMsgToDevice(int type, JSONObject body) throws UnsupportedEncodingException, JSONException {
        JSONObject msg = new JSONObject();
        msg.put("msgType", type);
        msg.put("msgBody", body);

        LOG.info("HuaweiP2PAppIcon sendMsgToDevice: {}", msg);

        sendCommand(msg.toString().getBytes(StandardCharsets.UTF_8), new HuaweiP2PCallback() {
            @Override
            public void onResponse(int code, byte[] data) {
                LOG.info("HuaweiP2PAppIcon sendCommand onResponse: {}", code);
            }
        });
    }


    public void sendResponse(String appPkgName, String appInfoHash, int retCode) {
        try {
            JSONObject body = new JSONObject();
            body.put("appPkgName", appPkgName);
            body.put("retCode", retCode);
            body.put("appInfoHash", appInfoHash);
            sendMsgToDevice(6002, body);
        } catch (UnsupportedEncodingException | JSONException e) {
            LOG.error("HuaweiP2PAppIcon sendResponse error", e);
        }
    }

    private int getType(int retCode) {
        switch (retCode) {
            case 10001:
                return 1;
            case 10002:
                return 2;
            case 10003:
                return 3;
            default:
                break;
        }
        //Not sure
        LOG.warn("HuaweiP2PAppIcon getType default retCode: {}", retCode);
        return 2;
    }

    private String getSendFileName(String appPkgName, String appInfoHash, int retCode) {
        return (appInfoHash + "_" + appPkgName.replace(".", "") + "_" + getType(retCode)) + ".bin";
    }

    // BitmapUtil.convertDrawableToBitmap BitmapUtil.toBitmap do the same but without opacity and width|height check.
    // Maybe it is possible to change methods in the BitmapUtil.
    public Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            height = 0;
            width = 0;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
            return bitmap;
        } catch (IllegalArgumentException | OutOfMemoryError e) {
            LOG.error("HuaweiP2PAppIcon drawableToBitmap error", e);
            return null;
        }
    }

    public byte[] convertImageData(Bitmap bmp) {
        if (bmp == null) {
            LOG.error("HuaweiP2PAppIcon convertImageData bmp is null");
            return null;
        }

        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int imageSize = Math.multiplyExact(width, height);

        ByteBuffer data = ByteBuffer.allocate(imageSize * 3 + 8);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.putShort((short) 0x2345);
        data.putShort((short) 0x8888);
        data.putShort((short) width);
        data.putShort((short) height);

        int[] imageData = new int[imageSize];
        bmp.getPixels(imageData, 0, width, 0, 0, width, height);

        int i = 1;
        for (int j = 0; j < imageData.length; j++) {
            if (j == imageData.length - 1 || imageData[j] != imageData[j + 1]) {
                if (i < 4) {
                    for (int k = 0; k < i; k++) {
                        data.putInt(imageData[j]);
                    }
                } else {
                    data.putInt(0x23456789); //591751049
                    data.putInt(imageData[j]);
                    data.putInt(i);
                }
                i = 1;
            } else {
                i++;
            }
        }
        return Arrays.copyOfRange(data.array(), 0, data.position());
    }

    public int convertRetCode(int retCode) {
        switch (retCode) {
            case 10001:
                return 10006;
            case 10002:
                return 10007;
            case 10003:
                return 10008;
            default:
                break;
        }
        // Not sure
        LOG.warn("HuaweiP2PAppIcon convertRetCode default retCode: {}", retCode);
        return 10007;
    }

    private void uploadImageToDevice(int retCode, String appPkgName, String appInfoHash, byte[] data, String filename) {

        if (data == null) {
            LOG.error("HuaweiP2PAppIcon uploadImageToDevice no data: {}", appPkgName);
            return;
        }
        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 7);
        fileInfo.setFileName(filename);
        fileInfo.setBytes(data);
        fileInfo.setSrcPackage(this.getModule());
        fileInfo.setDstPackage(this.getPackage());
        fileInfo.setSrcFingerprint(this.getLocalFingerprint());
        fileInfo.setDstFingerprint(this.getFingerprint());

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
                manager.getSupportProvider().getDevice().setBusyTask(R.string.updating_firmware, manager.getSupportProvider().getContext());
                manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadComplete() {
                LOG.info("HuaweiP2PAppIcon upload complete");
                if (manager.getSupportProvider().getDevice().isBusy()) {
                    manager.getSupportProvider().getDevice().unsetBusyTask();
                    manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
                }
                sendResponse(appPkgName, appInfoHash, convertRetCode(retCode));
            }

            @Override
            public void onError(int code) {
                LOG.info("HuaweiP2PAppIcon upload error");
                if (manager.getSupportProvider().getDevice().isBusy()) {
                    manager.getSupportProvider().getDevice().unsetBusyTask();
                    manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
                }
            }
        });

        HuaweiUploadManager huaweiUploadManager = this.manager.getSupportProvider().getUploadManager();

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(this.manager.getSupportProvider(), huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("HuaweiP2PAppIcon Failed to send file upload info", e);
        }
    }

    public void deviceRequestIcon(JSONObject jSONObject) throws JSONException {
        int retCode = jSONObject.getInt("retCode");
        String appPkgName = jSONObject.getString("appPkgName");
        LOG.debug("HuaweiP2PAppIcon deviceRequestIcon retCode: {}", retCode);
        if (retCode == 10004 || retCode == 10005) {
            resetCurrentPackage();
            processNext();
            return;
        }
        String appInfoHash = jSONObject.getString("appInfoHash");
        if (retCode == 10001 || retCode == 10002 || retCode == 10003) {
            int width = jSONObject.getInt("iconWidth");
            int height = jSONObject.getInt("iconHeight");
            LOG.info("HuaweiP2PAppIcon deviceRequestIcon Icon appPkgName: {}  iconWidth: {} iconHeight: {}", appPkgName, width, height);
            if (TextUtils.isEmpty(appPkgName) || width <= 0 || height <= 0) {
                sendResponse(appPkgName, appInfoHash, 10010);
                return;
            }
            final Drawable drawable = NotificationUtils.getAppIcon(this.manager.getSupportProvider().getContext(), appPkgName);
            if (drawable == null) {
                sendResponse(appPkgName, appInfoHash, 10009);
                LOG.info("HuaweiP2PAppIcon deviceRequestIcon drawable is null. {}", appPkgName);
                return;
            }
            final Bitmap bitmap = drawableToBitmap(drawable);
            if (bitmap == null) {
                sendResponse(appPkgName, appInfoHash, 10009);
                LOG.info("HuaweiP2PAppIcon deviceRequestIcon bitmap is null. {}", appPkgName);
                return;
            }
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            final String filename = getSendFileName(appPkgName, appInfoHash, retCode);

            final byte[] data = convertImageData(scaledBitmap);

            uploadImageToDevice(retCode, appPkgName, appInfoHash, data, filename);
            return;
        }
        sendResponse(appPkgName, appInfoHash, 10010);
    }


    public void deviceStop(JSONObject jSONObject) throws JSONException {
        int retCode = jSONObject.getInt("retCode");
        LOG.debug("HuaweiP2PAppIcon deviceStop appPkgName: {}  retCode: {}", jSONObject.getString("appPkgName"), retCode);
        if (retCode == 10011) {
            resetCurrentPackage();
            processNext();
        }
    }

    @Override
    public void handleData(byte[] data) {
        String str = new String(data, StandardCharsets.UTF_8);
        if (TextUtils.isEmpty(str)) {
            LOG.error("HuaweiP2PAppIcon data is empty");
            return;
        }
        try {
            JSONObject jsonData = new JSONObject(str);
            int msgType = jsonData.getInt("msgType");
            LOG.debug("HuaweiP2PAppIcon msgType:{} json: {}", msgType, jsonData);
            JSONObject msgBody = jsonData.getJSONObject("msgBody");
            if (msgType == 6001) {
                deviceRequestIcon(msgBody);
            } else if (msgType == 6002) {
                deviceStop(msgBody);
            } else {
                LOG.info("HuaweiP2PAppIcon msgType is unknown or not implemented");
            }
        } catch (JSONException e) {
            LOG.error("HuaweiP2PAppIcon Error parse response", e);
        }
    }

    public PackageInfo getPackageInfo(final String packageName) {
        try {
            return GBApplication.getContext().getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.error("HuaweiP2PAppIcon getPackageInfo NameNotFoundException", e);
        }
        return null;
    }

    public void addPackageName(List<String> packageName) {
        synchronized (this) {
            this.queue.addAll(packageName);
        }
        processNext();
    }

    private void processNext() {
        LOG.info("HuaweiP2PAppIcon processNext");
        synchronized (this) {
            if (this.queue.isEmpty() || !TextUtils.isEmpty(currentPackage)) {
                return;
            }
            currentPackage = this.queue.poll();
        }
        processUpload(currentPackage);
    }

    private void resetCurrentPackage() {
        synchronized (this) {
            currentPackage = null;
        }
    }

    private String getAppHashInfo(final String packageName, final String versionCode) {
        int versionHash = (packageName + versionCode).hashCode();
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(versionHash);
        return GB.hexdump(buf.array()).toLowerCase();
    }

    private void processUpload(String packageName) {

        PackageInfo packageInfo = getPackageInfo(packageName);
        String name = NotificationUtils.getApplicationLabel(this.manager.getSupportProvider().getContext(), packageName);
        if (packageInfo == null || TextUtils.isEmpty(name)) {
            LOG.error("HuaweiP2PAppIcon error get packageInfo is null. {}", packageName);
            resetCurrentPackage();
            processNext();
            return;
        }

        String versionCode = String.valueOf(PackageInfoCompat.getLongVersionCode(packageInfo));

        try {
            String appInfoHash = getAppHashInfo(packageName, versionCode);
            JSONObject body = new JSONObject();
            body.put("appInfoHash", appInfoHash);
            body.put("appPkgName", packageName);
            body.put("appName", name);
            body.put("retCode", 10000);
            sendMsgToDevice(6001, body);
        } catch (UnsupportedEncodingException | JSONException e) {
            LOG.error("HuaweiP2PAppIcon processUpload Exception", e);
        }
    }

}
