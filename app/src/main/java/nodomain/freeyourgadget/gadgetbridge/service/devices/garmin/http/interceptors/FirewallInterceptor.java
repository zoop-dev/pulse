package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors;

import android.os.RemoteException;
import android.webkit.WebResourceResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetFirewall;
import nodomain.freeyourgadget.gadgetbridge.internet.InternetRequestType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse;
import nodomain.freeyourgadget.gadgetbridge.util.InternetHelperSingleton;
import nodomain.freeyourgadget.internethelper.aidl.http.HttpRequest;

@SuppressWarnings("ClassCanBeRecord")
public class FirewallInterceptor implements HttpInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(FirewallInterceptor.class);

    private final InternetFirewall firewall;

    public FirewallInterceptor(final GarminSupport deviceSupport) {
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

        // TODO refactor this to be asynchronous
        final WebResourceResponse response;
        try {
            response = InternetHelperSingleton.INSTANCE.send(
                    request.getUri(),
                    HttpRequest.Method.valueOf(request.getMethod()),
                    request.getHeaders(),
                    new String(request.getBody(), StandardCharsets.UTF_8), // FIXME we should be able to send the raw bytes
                    "application/json",
                    false
            );
        } catch (final RemoteException e) {
            LOG.error("Remote exception", e);
            return null;
        } catch (final InterruptedException e) {
            LOG.error("Request was interrupted", e);
            return null;
        }

        if (response == null) {
            LOG.error("Response is null");
            return null;
        }

        final GarminHttpResponse garminHttpResponse = new GarminHttpResponse();
        garminHttpResponse.setStatus(response.getStatusCode());
        garminHttpResponse.getHeaders().putAll(response.getResponseHeaders());

        try {
            garminHttpResponse.setBody(readAllBytes(response.getData()));
        } catch (final IOException e) {
            LOG.error("Failed to read bytes from response", e);
            return null;
        }

        return garminHttpResponse;
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
