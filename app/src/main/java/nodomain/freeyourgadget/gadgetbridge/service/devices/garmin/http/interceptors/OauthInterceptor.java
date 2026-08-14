package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminAuthExpiredActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminPrefs;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminHttpResponse;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class OauthInterceptor implements HttpInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(OauthInterceptor.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    private final GarminSupport deviceSupport;

    public OauthInterceptor(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    @Override
    public boolean supports(@NonNull final GarminHttpRequest request) {
        return request.getPath().startsWith("/api/oauth/") ||
                request.getPath().startsWith("/oauth/") ||
                request.getPath().startsWith("/oauthTokenExchangeService/");
    }

    @Override
    @Nullable
    public GarminHttpResponse handle(@NonNull final GarminHttpRequest request) {
        if (request.getRawRequest().getMethod() != GdiHttpService.HttpService.Method.POST) {
            LOG.warn("Known OAuth requests should be POST");
            return null;
        }

        final GarminPrefs devicePrefs = deviceSupport.getDevicePrefs();
        if (!devicePrefs.fakeOauthEnabled()) {
            LOG.warn("Got OAuth HTTP request, but fake OAuth is disabled");
            if (request.getPath().equals("/oauthTokenExchangeService/connectToIT") ||
                    request.getPath().equals("/oauth/refresh_token/token") ||
                    request.getPath().equals("/api/oauth/token")) {
                showAuthExpiredNotification();
            }
            return null;
        }

        final Set<String> scopes = new LinkedHashSet<>(Arrays.asList(
                // Swim 2
                "GCS_EPHEMERIS_SONY_READ",
                // Venu 3
                "GCS_CIQ_APPSTORE_MOBILE_READ",
                "GCS_EMERGENCY_ASSISTANCE_CREATE",
                "GCS_GEOLOCATION_ELEVATION_READ",
                "GCS_IMAGE_READ",
                "GCS_LIVETRACK_FIT_CREATE",
                "GCS_LIVETRACK_FIT_READ",
                "GCS_LIVETRACK_FIT_UPDATE",
                "OMT_GOLF_SUBSCRIPTION_READ",
                "OMT_SUBSCRIPTION_READ",
                // Enduro 3
                "CSE_CDS_ACCOUNT_READ",
                "GCS_CIQ_APPSTORE_MOBILE_READ",
                "GCS_DEVICE_INSTRUCTION_OUTDOOR_READ",
                "GCS_EMERGENCY_ASSISTANCE_CREATE",
                "GCS_EPHEMERIS_SONY_READ",
                "GCS_GEOLOCATION_ELEVATION_READ",
                "GCS_IMAGE_STORAGE_READ",
                "GCS_LIVETRACK_FIT_CREATE",
                "GCS_LIVETRACK_FIT_READ",
                "GCS_LIVETRACK_FIT_UPDATE",
                "GCS_LIVE_EVENT_SHARING_CREATE",
                "GCS_LTE_SIGNAL_UPDATE",
                "GCS_MESSAGING_FITNESS_CREATE",
                "GCS_MESSAGING_FITNESS_READ",
                "GCS_MESSAGING_FITNESS_UPDATE",
                "GCS_STOCKS_READ",
                "GCS_TIDE_READ",
                "GCS_WEATHER_RACEDAY_READ",
                "MARINE_SERVER_ACCESS",
                "MARINE_SERVER_CHARTS_SUBSCRIPTION_READ",
                "MARINE_SERVER_CHARTS_SUBSCRIPTION_WRITE",
                "OMT_BIRDSEYE_READ",
                "OMT_OUTDOOR_MAP_SUBSCRIPTION_CREATE",
                "OMT_OUTDOOR_MAP_SUBSCRIPTION_DELETE",
                "OMT_OUTDOOR_MAP_SUBSCRIPTION_READ",
                "OMT_SUBSCRIPTION_READ",
                "YAR_BILLING_SUBSCRIBER_READ",
                "YAR_INREACH_HERMES_READ",
                "YAR_INREACH_IRIS_CREATE",
                "YAR_INREACH_IRIS_READ",
                "YAR_INREACH_IRIS_UPDATE",
                "YAR_INREACH_VOICE_EVENT_CREATE"
        ));

        if (request.getPath().equals("/oauthTokenExchangeService/connectToIT") || request.getPath().equals("/oauth/connect_exchange/token")) {
            final AuthorizationResponse authorizationResponse = new AuthorizationResponse();
            authorizationResponse.accessToken = UUID.randomUUID().toString();
            authorizationResponse.tokenType = "Bearer";
            authorizationResponse.refreshToken = UUID.randomUUID().toString();
            authorizationResponse.expiresIn = 7776000;
            authorizationResponse.scope = String.join(" ", scopes);
            authorizationResponse.refreshTokenExpiresIn = "31536000";
            authorizationResponse.customerId = UUID.randomUUID().toString();

            final GarminHttpResponse response = new GarminHttpResponse();
            response.setStatus(200);
            response.setBody(GSON.toJson(authorizationResponse).getBytes(StandardCharsets.UTF_8));
            response.getHeaders().put("Content-Type", "application/json");
            return response;
        } else if (request.getPath().equals("/api/oauth/token") || request.getPath().equals("/oauth/refresh_token/token")) {
            // Attempt to keep the same refresh token
            final String refreshToken;
            if (request.getRawRequest().hasRawBody()) {
                // grant_type=refresh_token&refresh_token=xxxxxxx&client_id=yyyyyyyy
                final String body = request.getRawRequest().getRawBody().toStringUtf8();
                final String[] args = body.split("&");
                final Map<String, String> queryParameters = Arrays.stream(args)
                        .map(a -> a.split("="))
                        .filter(a -> a.length == 2)
                        .collect(Collectors.toMap(a -> a[0], a -> a[1]));
                if (queryParameters.containsKey("refresh_token")) {
                    refreshToken = queryParameters.get("refresh_token");
                } else {
                    LOG.warn("Failed to find refresh_token in parameters");
                    refreshToken = UUID.randomUUID().toString();
                }
            } else {
                LOG.warn("Oauth refresh request has no body");
                refreshToken = UUID.randomUUID().toString();
            }

            final RefreshResponse refreshResponse = new RefreshResponse();
            refreshResponse.access_token = UUID.randomUUID().toString();
            refreshResponse.token_type = "Bearer";
            refreshResponse.expires_in = 7776000;
            refreshResponse.scope = String.join(" ", scopes);
            refreshResponse.refresh_token = refreshToken;
            refreshResponse.refresh_token_expires_in = "31536000";
            refreshResponse.customerId = UUID.randomUUID().toString();

            final GarminHttpResponse response = new GarminHttpResponse();
            response.setStatus(200);
            response.setBody(GSON.toJson(refreshResponse).getBytes(StandardCharsets.UTF_8));
            response.getHeaders().put("Content-Type", "application/json");
            return response;
        } else {
            LOG.warn("Unknown OAuth path {}", request.getPath());
        }

        return null;
    }

    private void showAuthExpiredNotification() {
        final GarminPrefs devicePrefs = deviceSupport.getDevicePrefs();
        if (!devicePrefs.authExpiredNotificationEnabled()) {
            LOG.debug("Will not notify, auth expired notification is disabled for this device");
            return;
        }

        final long currentTime = System.currentTimeMillis();
        final long lastNotificationTime = devicePrefs.getLong("last_auth_expired_notification", 0);
        if (currentTime - lastNotificationTime < 604800000 /* 1 week */) {
            LOG.debug("Will not notify, last notification was at {}", lastNotificationTime);
            return;
        }
        devicePrefs.getPreferences().edit()
                .putLong("last_auth_expired_notification", currentTime)
                .apply();

        final GBDevice device = deviceSupport.getDevice();
        final Intent activityIntent = new Intent(deviceSupport.getContext(), GarminAuthExpiredActivity.class);
        activityIntent.putExtra(GBDevice.EXTRA_DEVICE, device);

        final Notification notification = new NotificationCompat.Builder(deviceSupport.getContext(), GB.NOTIFICATION_CHANNEL_ID_DEVICE_WARNINGS)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(deviceSupport.getContext().getString(R.string.authentication_expired))
                .setContentText(deviceSupport.getContext().getString(R.string.click_here_for_more_information))
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(deviceSupport.getContext().getString(R.string.garmin_oauth_expired_description, device.getName()))
                )
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                        deviceSupport.getContext(),
                        0,
                        activityIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
                ))
                .build();

        GB.notify((int) (currentTime / 1000L), notification, deviceSupport.getContext());
    }

    public static class AuthorizationResponse {
        public String accessToken;
        public String tokenType;
        public String refreshToken;
        public int expiresIn;
        public String scope;
        public String refreshTokenExpiresIn;
        public String customerId;
    }

    public static class RefreshResponse {
        public String access_token;
        public String token_type;
        public int expires_in;
        public String scope;
        public String refresh_token;
        public String refresh_token_expires_in;
        public String customerId;
    }
}
