package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;

public class HuaweiDataSyncNotificationPictures implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncNotificationPictures.class);

    private final HuaweiSupportProvider support;

    public static final String SRC_PKG_NAME = "hw.unitedevice.notify.picture";
    public static final String PKG_NAME = "hw.watch.notify.picture";

    private int picWidth = 0;
    private int picNum = 0;

    private final LimitedQueue<String, String> pictures = new LimitedQueue<>(16);

    private String getFileName() {
        //NOTE: persist current index between reconnections and restarts. Watch behaviour.
        int currNum = GBApplication
                .getDeviceSpecificSharedPrefs(support.getDevice().getAddress())
                .getInt("HUAWEI_NOTIFICATION_CURRENT_NOTIFICATION_INDEX", 0);
        int next = currNum + 1;
        currNum = (next < Math.max(picNum, 30))?next:0;
        SharedPreferences deviceSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(support.getDevice().getAddress());
        SharedPreferences.Editor deviceSharedPrefsEdit = deviceSharedPrefs.edit();
        deviceSharedPrefsEdit.putInt("HUAWEI_NOTIFICATION_CURRENT_NOTIFICATION_INDEX", currNum);
        deviceSharedPrefsEdit.apply();
        LOG.info("getFileName picNum: {} currNum: {} ", picNum, currNum);
        return "pic" + currNum + ".bin";
    }

    public String getNameForPath(String path) {
        String filename = getFileName();
        pictures.add(filename, path);
        return filename;
    }

    public HuaweiDataSyncNotificationPictures(HuaweiSupportProvider support) {
        LOG.info("HuaweiDataNotificationPictures");
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }


    private void sendFeaturesFile(String filename) {
        LOG.info("HuaweiDataNotificationPictures Send data");

        final String picturePath = pictures.lookup(filename);
        final byte[] data = prepareImageToSend(picturePath);

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 0x07);
        fileInfo.setFileName(filename);
        fileInfo.setBytes(data);

        fileInfo.setSrcPackage(SRC_PKG_NAME);
        fileInfo.setDstPackage(PKG_NAME);
        fileInfo.setSrcFingerprint("UniteDeviceManagement");
        fileInfo.setDstFingerprint("SystemApp");

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadComplete() {
                if (support.getDevice().isBusy()) {
                    support.getDevice().unsetBusyTask();
                    support.getDevice().sendDeviceUpdateIntent(support.getContext());
                }
            }

            @Override
            public void onError(int code) {
                if (support.getDevice().isBusy()) {
                    support.getDevice().unsetBusyTask();
                    support.getDevice().sendDeviceUpdateIntent(support.getContext());
                }
            }
        });

        HuaweiUploadManager huaweiUploadManager = support.getUploadManager();
        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(support, huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send file upload info", e);
        }
    }

    public Bitmap scaleBitmapToSize(Bitmap bitmap) {
        int maxSize = picWidth;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (maxSize <= 0 || (width <= maxSize && height <= maxSize)) {
            return bitmap;
        }
        if (width > height) {
            float scaleFactor = (float) maxSize / width;
            return Bitmap.createScaledBitmap(bitmap, maxSize, (int) (height * scaleFactor), true);
        } else {
            float scaleFactor = (float) maxSize / height;
            return Bitmap.createScaledBitmap(bitmap, (int) (width * scaleFactor), maxSize, true);
        }
    }

    private int alphaToColor(int i) {
        int alpha = Color.alpha(i);
        if (alpha == 255) {
            return i;
        }
        return ((Color.blue(i) * alpha) / 255) | (((Color.red(i) * alpha) / 255) << 16) | (((Color.green(i) * alpha) / 255) << 8) | 0xFF000000;
    }

    private void put24BitLE(ByteBuffer data, int i) {
        data.put((byte) (i & 0xFF));
        data.put((byte) ((i >> 8) & 0xFF));
        data.put((byte) ((i >> 16) & 0xFF));
    }

    private void putColorData(ByteBuffer data, int[] imageData, int count, int idx) {
        if (count < 4) {
            for (int i = 0; i < count; i++) {
                put24BitLE(data, alphaToColor(imageData[idx]));
            }
        } else {
            put24BitLE(data, 0x456789);
            put24BitLE(data, alphaToColor(imageData[idx]));
            put24BitLE(data, count);
        }
    }

    public byte[] convertImageData(Bitmap bmp) {
        if (bmp == null) {
            LOG.error("HuaweiDataNotificationPictures convertImageData bmp is null");
            return null;
        }

        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int imageSize = Math.multiplyExact(width, height);

        ByteBuffer data = ByteBuffer.allocate(imageSize * 3 + 8);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.putShort((short) 0x2345);
        data.putShort((short) 0x0888);
        data.putShort((short) width);
        data.putShort((short) height);

        int[] imageData = new int[imageSize];
        bmp.getPixels(imageData, 0, width, 0, 0, width, height);

        int i = 1;
        for (int j = 0; j < imageData.length; j++) {
            if (j == imageData.length - 1 || alphaToColor(imageData[j]) != alphaToColor(imageData[j + 1])) {
                putColorData(data, imageData, i, j);
                i = 1;
            } else {
                i++;
            }
        }
        return Arrays.copyOfRange(data.array(), 0, data.position());
    }

    public byte[] prepareImageToSend(String picturePath) {
        try {
            final Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            final Bitmap bmp = scaleBitmapToSize(bitmap);
            return convertImageData(bmp);
        } catch (Exception e) {
            LOG.info("prepareImageToSend Exception: ", e);
        }
        return null;
    }


    private void handlePicRequest(String filename) {
        sendFeaturesFile(filename);
    }

    private void handlePicInfo(int picWidth, int picNum) {
        this.picWidth = picWidth;
        this.picNum = picNum;
    }

    private void handlePicResult(String filename, String result) {
        LOG.info("HuaweiDataNotificationPictures handlePicResult: {} result: {}", filename, result);
        if (TextUtils.isEmpty(filename) || !"success".equals(result)) {
            LOG.info("HuaweiDataNotificationPictures handlePicResult is not success");
            return;
        }
        pictures.remove(filename);
    }

    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
    }

    @Override
    public void onEventCommand(HuaweiDataSyncCommon.EventCommandData data) {
        LOG.info("HuaweiDataNotificationPictures config code: {} EventId: {}, EventLevel: {}, Data: {}", data.getCode(), data.getEventId(), data.getEventLevel(), GB.hexdump(data.getData()));
        if (data.getEventId() != 800100023) {
            return;
        }

        try {
            JSONObject json = new JSONObject(new String(data.getData()));
            if (!json.has("type")) {
                LOG.error("no type");
                return;
            }
            switch (json.getString("type")) {
                case "picInfo" -> handlePicInfo(json.getInt("picWidth"), json.getInt("picNum"));
                case "picRequest" -> handlePicRequest(json.getString("fileName"));
                case "picResult" -> handlePicResult(json.getString("fileName"), json.getString("result"));
            }
        } catch (JSONException e) {
            LOG.error("HuaweiDataNotificationPictures Exception: ", e);
        }
    }

    @Override
    public void onDataCommand(HuaweiDataSyncCommon.DataCommandData data) {
    }

    @Override
    public void onDictDataCommand(HuaweiDataSyncCommon.DictDataCommandData data) {

    }
}
