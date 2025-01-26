package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminPrefs;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class OauthHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OauthHandler.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    private final GarminSupport deviceSupport;

    public OauthHandler(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public GarminHttpResponse handleRequest(final GarminHttpRequest request) {
        if (request.getRawRequest().getMethod() != GdiHttpService.HttpService.Method.POST) {
            LOG.warn("Known OAuth requests should be POST");
            return null;
        }

        final GarminPrefs devicePrefs = deviceSupport.getDevicePrefs();
        if (!devicePrefs.fakeOauthEnabled()) {
            LOG.warn("Got OAuth HTTP request, but fake OAuth is disabled");
            return null;
        }

        final List<String> scopes = Arrays.asList(
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
                "OMT_SUBSCRIPTION_READ"
        );

        if (request.getPath().equals("/oauthTokenExchangeService/connectToIT")) {
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
        } else if (request.getPath().equals("/api/oauth/token")) {
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
