package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

public class ImageServiceInterceptor implements HttpInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceInterceptor.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    private final GarminSupport deviceSupport;

    public ImageServiceInterceptor(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    @Override
    public boolean supports(@NotNull final GarminHttpRequest request) {
        return "api.gcs.garmin.com".equals(request.getDomain()) &&
                request.getPath().startsWith("/image-service/");
    }

    @Override
    @Nullable
    public GarminHttpResponse handle(@NotNull final GarminHttpRequest request) {
        if (request.getRawRequest().getMethod() != GdiHttpService.HttpService.Method.GET) {
            LOG.warn("Known image service requests should be GET");
            return null;
        }

        if (!request.getPath().equals("/image-service/v2/device/images/details")) {
            LOG.warn("Unknown image service path {}", request.getPath());
            return null;
        }

        final String ownerAliasId = request.getQuery().get("ownerAliasId");

        final Drawable icon = NotificationUtils.getAppIcon(
                deviceSupport.getContext(),
                ownerAliasId
        );

        if (icon == null) {
            LOG.warn("Failed to get icon for {}", ownerAliasId);

            return createNotFoundResponse(ownerAliasId);
        }

        final String imageSizeStr = request.getQuery().get("imageSize");
        if (imageSizeStr == null) {
            LOG.warn("No image size query param");
            return createNotFoundResponse(ownerAliasId);
        }

        final int imageSize = Integer.parseInt(imageSizeStr);
        final byte[] encodedPng = encodeToPng(icon, imageSize);
        final byte[] header = createImageHeader(encodedPng.length, imageSize);

        final ByteBuffer buf = ByteBuffer.allocate(header.length + encodedPng.length);
        buf.put(header);
        buf.put(encodedPng);

        final GarminHttpResponse response = new GarminHttpResponse();

        response.setStatus(200);
        response.getHeaders().put("Content-Type", "image/png");
        response.getHeaders().put("imagetype", "ICON");
        response.getHeaders().put("original-image-size", String.valueOf(encodedPng.length));
        response.getHeaders().put("ownerid", ownerAliasId);
        response.getHeaders().put("ownertype", "APP");
        response.setBody(buf.array());

        return response;
    }

    @NonNull
    private static GarminHttpResponse createNotFoundResponse(final String ownerAliasId) {
        final String requestId = UUID.randomUUID().toString();

        final ErrorResponse errorResponse = new ErrorResponse(requestId, Collections.singletonList(new Error(
                "Owner alias (" + ownerAliasId + ") not found",
                "NOT_FOUND"
        )));

        final GarminHttpResponse response = new GarminHttpResponse();

        response.setStatus(404);
        response.setBody(GSON.toJson(errorResponse).getBytes(StandardCharsets.UTF_8));
        response.getHeaders().put("Content-Type", "application/json");
        response.getHeaders().put("x-request-id", requestId);

        return response;
    }

    private static byte[] encodeToPng(final Drawable icon, final int imageSize) {
        final Bitmap bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        icon.setBounds(0, 0, imageSize, imageSize);
        icon.draw(canvas);

        final ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBaos);

        return pngBaos.toByteArray();
    }

    private static byte[] createImageHeader(final int pngLength, final int imageSize) {
        final ByteBuffer buf = ByteBuffer.allocate(43).order(ByteOrder.BIG_ENDIAN);

        // 0c 11 ee 5e
        buf.put((byte) 0x0c);
        buf.put((byte) 0x11);
        buf.put((byte) 0xee);
        buf.put((byte) 0x5e);

        buf.putInt(pngLength + 0x23);
        buf.putShort((short) imageSize);
        buf.putShort((short) imageSize);

        // ff ff 00 40 00 00 10 00 00 1c 00 00 1c 10
        buf.put((byte) 0xff);
        buf.put((byte) 0xff);
        buf.put((byte) 0x00);
        buf.put((byte) 0x40);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x10);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x1c);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x1c);
        buf.put((byte) 0x10);

        buf.putShort((short) imageSize);
        buf.putShort((short) imageSize);

        // 00 02 04 00 00 00 00 04 00
        buf.put((byte) 0x00);
        buf.put((byte) 0x02);
        buf.put((byte) 0x04);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x00);
        buf.put((byte) 0x04);
        buf.put((byte) 0x00);

        buf.put(BLETypeConversions.fromUint32(pngLength));

        return buf.array();
    }

    public record ErrorResponse(String requestId, List<Error> errors) {
    }

    public record Error(String message, String type) {
    }
}
