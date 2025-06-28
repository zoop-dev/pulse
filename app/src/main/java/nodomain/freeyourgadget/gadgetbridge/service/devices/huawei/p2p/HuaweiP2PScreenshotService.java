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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class HuaweiP2PScreenshotService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PScreenshotService.class);

    public static final String MODULE = "hw.unitedevice.screenshot";

    public static class DeviceInfo {
        @SerializedName("bezierCurvePoint")
        public BezierCurvePoint bezierCurvePoint;
        @SerializedName("cutout")
        public int cutout;
        @SerializedName("compressionAlgorithm")
        public int compressionAlgorithm;
        @SerializedName("photoHeight")
        public int photoHeight;
        @SerializedName("radius")
        public float[] radius;
        @SerializedName("photoWidth")
        public int photoWidth;
        @SerializedName("roundedCutType")
        public int roundedCutType;
        @SerializedName("startPointX")
        public int startPointX;
        @SerializedName("startPointY")
        public int startPointY;
        @SerializedName("watchType")
        public int watchType;

        public static class BezierCurvePoint {
            @SerializedName("inPoint")
            public float[][] inPoint;
            @SerializedName("outPoint")
            public float[][] outPoint;
        }
    }

    // Crop related constants.
    private final int PIXELS_TO_CROP = 1;
    private final int CROP_BORDER_WIDTH = 2;
    private final int CROP_BORDER_COLOR = Color.BLACK;
    private final int CROP_BACKGROUND_COLOR = Color.WHITE;

    private DeviceInfo deviceInfo = null;

    public HuaweiP2PScreenshotService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PScreenshotService");
    }

    @Override
    public String getModule() {
        return HuaweiP2PScreenshotService.MODULE;
    }

    @Override
    public String getPackage() {
        return "com.huawei.watch.screenshot";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    private String startServiceData() {
        JSONObject data = new JSONObject();
        try {
            data.put("operateType", 1);
            data.put("statusCode", 1);
            data.put("hasPermission", 1);
        } catch (JSONException e) {
            LOG.error("startServiceData: Failed to prepare JSOM Object", e);
        }
        return data.toString();
    }

    private String createResponse(int i) {
        JSONObject data = new JSONObject();
        try {
            data.put("operateType", 3);
            data.put("statusCode", i);
        } catch (JSONException e) {
            LOG.error("createResponse: Failed to prepare JSOM Object", e);
        }
        return data.toString();
    }

    private String createDownloadResponse(String filename, int i) {
        JSONObject data = new JSONObject();
        try {
            data.put("operateType", 2);
            data.put("fileName", filename);
            data.put("statusCode", i);
        } catch (JSONException e) {
            LOG.error("createResponse: Failed to prepare JSOM Object", e);
        }
        return data.toString();
    }

    public void sendNegotiateConfig() {
        String packet = startServiceData();
        LOG.info("HuaweiP2PScreenshotService sendNegotiateConfig");
        sendCommand(packet.getBytes(StandardCharsets.UTF_8), null);
    }

    @Override
    public void registered() {
    }

    @Override
    public void unregister() {

    }

    @Override
    public void handleData(byte[] data) {
        if (data == null) {
            LOG.error("HuaweiP2PScreenshotService data is null");
            return;
        }
        String config = new String(data, StandardCharsets.UTF_8);
        if (TextUtils.isEmpty(config)) {
            LOG.error("HuaweiP2PScreenshotService config is empty");
            return;
        }
        LOG.info("HuaweiP2PScreenshotService handleData: {}", config);
        DeviceInfo deviceInfo = null;
        try {
            deviceInfo = new Gson().fromJson(config, DeviceInfo.class);
        } catch (JsonSyntaxException e) {
            LOG.error("HuaweiP2PScreenshotService error parse config", e);
        }

        int status = 2;
        if (deviceInfo == null) {
            LOG.error("HuaweiP2PScreenshotService deviceInfo is null");
        } else {
            if (deviceInfo.compressionAlgorithm == 1) {
                status = 1;
            }
        }

        if (status == 1) {
            this.deviceInfo = deviceInfo;
        }
        sendCommand(createResponse(status).getBytes(StandardCharsets.UTF_8), null);
    }

    private int getWightWithoutCropPixels(int width) {
        return width - PIXELS_TO_CROP * 2; // crop from both sides.
    }

    private int getHeightWithoutCropPixels(int height) {
        return height - PIXELS_TO_CROP * 2; // crop from both sides.
    }

    private Bitmap prepareCroppedImageCircle(Bitmap image) {
        int cropWidth = getWightWithoutCropPixels(this.deviceInfo.photoWidth);
        int cropHeight = getHeightWithoutCropPixels(this.deviceInfo.photoHeight);
        int width = getWightWithoutCropPixels(this.deviceInfo.photoWidth) + CROP_BORDER_WIDTH * 2; // we have border on both sides
        int height = getHeightWithoutCropPixels(this.deviceInfo.photoHeight) + CROP_BORDER_WIDTH * 2; // we have border on both sides
        Bitmap createBitmap = Bitmap.createBitmap(width, height, image.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        canvas.drawColor(CROP_BACKGROUND_COLOR);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(CROP_BORDER_WIDTH);
        paint.setColor(CROP_BORDER_COLOR);
        canvas.drawCircle(((float) width) / 2.0f, ((float) height) / 2.0f, ((float) Math.min(width - CROP_BORDER_WIDTH, height - CROP_BORDER_WIDTH)) / 2.0f, paint);
        Bitmap croppedImage = cropImageCircle(image, cropWidth, cropHeight);
        canvas.drawBitmap(croppedImage, ((float) (width - cropWidth)) / 2.0f, ((float) (height - cropHeight)) / 2.0f, new Paint(Paint.ANTI_ALIAS_FLAG));
        return createBitmap;
    }

    private Bitmap cropImageCircle(Bitmap srcImage, int width, int height) {
        Bitmap resultImage = Bitmap.createBitmap(width, height, srcImage.getConfig());
        Canvas canvas = new Canvas(resultImage);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(((float) width) / 2.0f, ((float) height) / 2.0f, ((float) Math.min(width, height)) / 2.0f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(srcImage, (float) (this.deviceInfo.startPointX - PIXELS_TO_CROP), (float) (this.deviceInfo.startPointY - PIXELS_TO_CROP), paint);
        return resultImage;
    }

    private Bitmap prepareCroppedImageRounded(Bitmap image) {
        //TODO: implement crop for device with rounded corners
        return image;
    }

    private Bitmap prepareCroppedImage(Bitmap bitmap) {
        if (bitmap == null)
            return null;
        if (this.deviceInfo.watchType == 1) {
            return prepareCroppedImageCircle(bitmap);
        } else if (this.deviceInfo.watchType == 2) {
            return prepareCroppedImageRounded(bitmap);
        }
        LOG.info("HuaweiP2PScreenshotService unknown watchType: {}", this.deviceInfo.watchType);
        // I don't know what to do here. So save image as is.
        return bitmap;
    }

    private boolean saveBitmapOld(String filename, Bitmap bitmap) {
        LOG.info("HuaweiP2PScreenshotService saveBitmapOld: {}", filename);
        File targetFile;
        try {
            targetFile = new File(FileUtils.getExternalFilesDir() + File.separator + filename);
        } catch (IOException e) {
            LOG.error("Could not open Screenshot file to write to", e);
            return false;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                fileOutputStream.close();
                return false;
            }

            fileOutputStream.close();
        } catch (IOException e) {
            LOG.error("Error save bitmap", e);
            return false;
        }
        return true;
    }

    private boolean saveMediaStore(String filename, Bitmap bitmap) {
        LOG.info("HuaweiP2PScreenshotService saveMediaStore: {}", filename);
        String relativePath = Environment.DIRECTORY_PICTURES + File.separator + "Screenshots";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.ImageColumns.RELATIVE_PATH, relativePath);
        contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.Images.ImageColumns.IS_PENDING, 1);
        ContentResolver contentResolver = GBApplication.getContext().getContentResolver();
        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (uri == null) {
            contentValues.clear();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            return false;
        }
        try {
            OutputStream stream = contentResolver.openOutputStream(uri);
            if (stream != null && bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                contentResolver.update(uri, contentValues, null, null);
                stream.close();
                return true;
            }
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e) {
            LOG.error("Error save screenshot", e);
        }

        contentValues.clear();
        contentValues.put(MediaStore.Images.ImageColumns.IS_PENDING, 0);
        contentResolver.update(uri, contentValues, null, null);
        return false;
    }

    private boolean saveBitmap(String filename, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return saveBitmapOld(filename, bitmap);
        return saveMediaStore(filename, bitmap);
    }


    @Override
    public void handleFile(String filename, byte[] data) {
        LOG.info("HuaweiP2PScreenshotService handleFile: {}", filename);
        if (data == null) {
            LOG.error("HuaweiP2PScreenshotService  empty file data");
            sendCommand(createDownloadResponse(filename, 2).getBytes(StandardCharsets.UTF_8), null);
            return;
        }
        if (this.deviceInfo == null) {
            LOG.error("HuaweiP2PScreenshotService no device config");
            sendCommand(createDownloadResponse(filename, 2).getBytes(StandardCharsets.UTF_8), null);
            return;
        }

        Bitmap image = null;
        if (this.deviceInfo.compressionAlgorithm == 1) {
            image = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        if (this.deviceInfo.cutout == 1) {
            image = this.prepareCroppedImage(image);
        }
        if (image == null) {
            LOG.error("HuaweiP2PScreenshotService no image");
            sendCommand(createDownloadResponse(filename, 2).getBytes(StandardCharsets.UTF_8), null);
            return;
        }

        if (saveBitmap(filename, image)) {
            sendCommand(createDownloadResponse(filename, 1).getBytes(StandardCharsets.UTF_8), null);
        } else {
            LOG.error("HuaweiP2PScreenshotService error to save image");
            sendCommand(createDownloadResponse(filename, 2).getBytes(StandardCharsets.UTF_8), null);
        }
    }

    public static HuaweiP2PScreenshotService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PScreenshotService) manager.getRegisteredService(HuaweiP2PScreenshotService.MODULE);
    }
}
