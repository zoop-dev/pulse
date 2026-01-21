package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors;

import android.os.ParcelFileDescriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetFirewall;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetRequestType;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.ProtobufResponseEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.HttpHandler;
import nodomain.freeyourgadget.gadgetbridge.util.InternetHelperSingleton;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpRequest;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpResponse;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpCallback;
import nodomain.freeyourgadget.internethelper.aidl.http.IHttpService;

public class FirewallInterceptor implements HttpInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(FirewallInterceptor.class);

    private final InternetFirewall firewall;
    private final GarminSupport deviceSupport;

    public FirewallInterceptor(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
        this.firewall = new InternetFirewall(InternetRequestType.WATCH_APP, deviceSupport.getDevice());
    }

    @Override
    public boolean supports(@NotNull final GarminHttpRequest request) {
        return true;
    }

    @Override
    @Nullable
    public GarminHttpResponse handle(@NotNull final GarminHttpRequest request) {
        if (!GBApplication.hasInternetAccess()) {
            LOG.warn("Gb has no internet access");
            return null;
        }

        // Firewall checks the device_internet_access
        if (!firewall.isAllowed(request.getUri())) {
            LOG.warn("Firewall blocked the request");
            return null;
        }

        if (request.getDomain().endsWith("garmin.com") || request.getDomain().endsWith("dciwx.com")) {
            // For now, we explicitly block all requests to Garmin domains, even if the user whitelists them.
            // Due to fake OAuth, most of these will include invalid authentication credentials, and needs
            // further investigation
            LOG.warn("Blocking request to Garmin url: {}", request.getDomain());
            return null;
        }

        final IHttpService httpService = InternetHelperSingleton.INSTANCE.getHttpService();
        if (httpService == null) {
            LOG.error("HttpService is null");
            return null;
        }

        LOG.debug("Forwarding http request {} to InternetHelper", request.getMessageRequestId());

        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final ParcelFileDescriptor pipeRead = pipe[0];
            final ParcelFileDescriptor pipeWrite = pipe[1];

            final HttpRequest httpRequest = new HttpRequest(
                    request.getUri().toString(),
                    HttpRequest.Method.valueOf(request.getMethod()),
                    request.getHeaders(),
                    pipeRead,
                    false
            );

            httpService.send(httpRequest, new IHttpCallback.Stub() {
                @Override
                public void onResponse(final HttpResponse response) {
                    LOG.debug("Got response to {} from InternetHelper", request.getMessageRequestId());
                    handleHttpResponse(request, response);
                }

                @Override
                public void onException(final String message) {
                    LOG.error("Http request for {} failed: {}", request.getMessageRequestId(), message);
                    handleHttpException(request);
                }
            });

            try (ParcelFileDescriptor.AutoCloseOutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipeWrite)) {
                out.write(request.getBody());
            }
        } catch (final Exception e) {
            LOG.error("Failed to send request to InternetHelper", e);
            return null;
        }

        final GarminHttpResponse garminHttpResponse = new GarminHttpResponse();
        garminHttpResponse.setComplete(false);

        return garminHttpResponse;
    }

    private void handleHttpResponse(@NotNull final GarminHttpRequest request,
                                    final HttpResponse response) {
        final GarminHttpResponse garminHttpResponse = new GarminHttpResponse();
        garminHttpResponse.setStatus(response.getStatus());
        garminHttpResponse.getHeaders().putAll(response.getHeaders());
        try (ParcelFileDescriptor.AutoCloseInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(response.getBody())) {
            garminHttpResponse.setBody(readAllBytes(in));
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from response", e);
            handleHttpException(request);
            return;
        }

        final GdiHttpService.HttpService successResponse = HttpHandler.createSuccessResponse(request, garminHttpResponse);
        if (successResponse != null) {
            sendHttpServiceRequest(request, successResponse);
        } else {
            final GdiHttpService.HttpService errorResponse = HttpHandler.createErrorResponse(request);
            sendHttpServiceRequest(request, errorResponse);
        }
    }

    private void handleHttpException(@NotNull final GarminHttpRequest request) {
        final GdiHttpService.HttpService errorResponse = HttpHandler.createErrorResponse(request);
        sendHttpServiceRequest(request, errorResponse);
    }

    private void sendHttpServiceRequest(@NotNull final GarminHttpRequest request,
                                        @NotNull final GdiHttpService.HttpService httpService) {
        final GdiSmartProto.Smart smart = GdiSmartProto.Smart.newBuilder().setHttpService(httpService).build();
        deviceSupport.evaluateGBDeviceEvent(new ProtobufResponseEvent(
                smart, request.getMessageRequestId()
        ));
    }

    public static byte[] readAllBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            int read = in.read(buffer);
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
