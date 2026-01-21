package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.AgpsInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.ContactsInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.FirewallInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.HttpInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.ImageServiceInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.OauthInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.WeatherInterceptor;

@SuppressWarnings("ClassCanBeRecord")
public class HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private final List<HttpInterceptor> interceptors;

    public HttpHandler(final GarminSupport deviceSupport) {
        this.interceptors = Arrays.asList(
                new WeatherInterceptor(),
                new AgpsInterceptor(deviceSupport),
                new ImageServiceInterceptor(deviceSupport),
                new ContactsInterceptor(deviceSupport),
                new OauthInterceptor(deviceSupport),
                // Should always be the last one
                new FirewallInterceptor(deviceSupport)
        );
    }

    public GdiHttpService.HttpService handle(final GdiHttpService.HttpService httpService, final int messageRequestId) {
        if (httpService.hasRawRequest()) {
            final GdiHttpService.HttpService.RawResponse rawResponse = handleRawRequest(httpService.getRawRequest(), messageRequestId);
            if (rawResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setRawResponse(rawResponse)
                        .build();
            }
            return null;
        } else if (httpService.hasWebRequest()) {
            final GdiHttpService.HttpService.WebResponse webResponse = handleWebRequest(httpService.getWebRequest(), messageRequestId);
            if (webResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setWebResponse(webResponse)
                        .build();
            }
            return null;
        }

        LOG.warn("Unsupported http service request {}", httpService);

        return null;
    }

    public GdiHttpService.HttpService.RawResponse handleRawRequest(final GdiHttpService.HttpService.RawRequest rawRequest,
                                                                   final int messageRequestId) {
        LOG.debug("Got rawRequest {}: {} - {}", messageRequestId, rawRequest.getMethod(), rawRequest.getUrl());

        final GarminHttpRequest request = new GarminHttpRequest(rawRequest, messageRequestId);

        final GarminHttpResponse response = handleRequest(request);

        if (response == null) {
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                    .build();
        }

        if (!response.isComplete()) {
            // Async response
            return null;
        }

        return createRawResponse(request, response);
    }

    @Nullable
    private GarminHttpResponse handleRequest(final GarminHttpRequest request) {
        final Optional<HttpInterceptor> interceptorOpt = interceptors.stream()
                .filter(it -> it.supports(request))
                .findFirst();

        if (interceptorOpt.isEmpty()) {
            LOG.warn("No interceptor for {}", request.getPath());
            return null;
        }

        final HttpInterceptor interceptor = interceptorOpt.get();
        LOG.debug("Handling request to {}", interceptor.getClass().getSimpleName());
        final GarminHttpResponse response = interceptor.handle(request);
        if (response != null) {
            LOG.debug(
                    "Response from interceptor: {}, {} bytes, complete={}",
                    response.getStatus(),
                    response.getBody().length,
                    response.isComplete()
            );
        } else {
            LOG.debug("No response from interceptor");
        }
        return response;
    }

    public static GdiHttpService.HttpService.RawResponse createRawResponse(
            final GarminHttpRequest request,
            final GarminHttpResponse response
    ) {
        final List<GdiHttpService.HttpService.Header> responseHeaders = new ArrayList<>();
        for (final Map.Entry<String, String> h : response.getHeaders().entrySet()) {
            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey(h.getKey())
                            .setValue(h.getValue())
                            .build()
            );
        }

        if (request.getRawRequest().hasUseDataXfer() && request.getRawRequest().getUseDataXfer()) {
            LOG.debug("Data will be returned using data_xfer");
            final int id = DataTransferHandler.registerData(response.getBody());
            if (response.getOnDataSuccessfullySentListener() != null) {
                DataTransferHandler.addOnDataSuccessfullySentListener(id, response.getOnDataSuccessfullySentListener());
            }
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.OK)
                    .setHttpStatus(response.getStatus())
                    .addAllHeader(responseHeaders)
                    .setXferData(
                            GdiHttpService.HttpService.DataTransferItem.newBuilder()
                                    .setId(id)
                                    .setSize(response.getBody().length)
                                    .build()
                    )
                    .build();
        }

        final byte[] responseBody;
        if ("gzip".equals(request.getHeaders().get("accept-encoding"))) {
            LOG.debug("Compressing response");
            responseHeaders.add(
                    GdiHttpService.HttpService.Header.newBuilder()
                            .setKey("Content-Encoding")
                            .setValue("gzip")
                            .build()
            );

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(response.getBody());
                gzos.finish();
                gzos.flush();
                responseBody = baos.toByteArray();
            } catch (final Exception e) {
                LOG.error("Failed to compress response", e);
                return null;
            }
        } else {
            responseBody = response.getBody();
        }

        return GdiHttpService.HttpService.RawResponse.newBuilder()
                .setStatus(GdiHttpService.HttpService.Status.OK)
                .setHttpStatus(response.getStatus())
                .setBody(ByteString.copyFrom(responseBody))
                .addAllHeader(responseHeaders)
                .build();
    }

    public GdiHttpService.HttpService.WebResponse handleWebRequest(final GdiHttpService.HttpService.WebRequest webRequest,
                                                                   final int messageRequestId) {
        LOG.debug("Got webRequest {}: {} {}", messageRequestId, webRequest.getMethod(), webRequest.getUrl());

        try {
            final GarminHttpRequest request = new GarminHttpRequest(webRequest, messageRequestId);

            final GarminHttpResponse response = handleRequest(request);

            if (response == null) {
                return GdiHttpService.HttpService.WebResponse.newBuilder()
                        .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                        .setHttpStatus(0)
                        .build();
            }

            if (!response.isComplete()) {
                // Async response
                return null;
            }

            return createWebResponse(request, response);
        } catch (final Exception e) {
            LOG.error("Failed to create web response", e);
            return GdiHttpService.HttpService.WebResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                    .setHttpStatus(0)
                    .build();
        }
    }

    public static GdiHttpService.HttpService createSuccessResponse(final GarminHttpRequest request,
                                                                   final GarminHttpResponse response) {
        if (request.getRawRequest() != null) {
            final GdiHttpService.HttpService.RawResponse rawResponse = createRawResponse(request, response);
            if (rawResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setRawResponse(rawResponse)
                        .build();
            }
            return null;
        } else if (request.getWebRequest() != null) {
            final GdiHttpService.HttpService.WebResponse webResponse = createWebResponse(request, response);
            if (webResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setWebResponse(webResponse)
                        .build();
            }
            return null;
        } else {
            throw new IllegalArgumentException("Should never happen");
        }
    }

    public static GdiHttpService.HttpService createErrorResponse(final GarminHttpRequest request) {
        if (request.getRawRequest() != null) {
            return GdiHttpService.HttpService.newBuilder()
                    .setRawResponse(GdiHttpService.HttpService.RawResponse.newBuilder()
                            .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                            .build())
                    .build();
        } else if (request.getWebRequest() != null) {
            return GdiHttpService.HttpService.newBuilder()
                    .setWebResponse(GdiHttpService.HttpService.WebResponse.newBuilder()
                            .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                            .setHttpStatus(0)
                            .build())
                    .build();
        } else {
            throw new IllegalArgumentException("Should never happen");
        }
    }

    public static GdiHttpService.HttpService.WebResponse createWebResponse(
            final GarminHttpRequest request,
            final GarminHttpResponse response
    ) {
        final GdiHttpService.HttpService.WebRequest webRequest = request.getWebRequest();
        try {
            final byte[] headers;
            if (webRequest.getHttpHeadersInResponse()) {
                byte[] encodedHeaders = null;
                try {
                    final JsonObject jsonObject = new JsonObject();
                    for (Map.Entry<String, String> e : response.getHeaders().entrySet()) {
                        jsonObject.addProperty(e.getKey(), e.getValue());
                    }
                    encodedHeaders = GarminJson.encode(jsonObject);
                } catch (final GarminJsonException e) {
                    LOG.error("Failed to encode headers", e);
                }
                headers = encodedHeaders;
            } else {
                headers = null;
            }

            if (response.getBody().length > webRequest.getMaxResponseLength()) {
                // TODO figure out the compression algorithm
                return GdiHttpService.HttpService.WebResponse.newBuilder()
                        .setStatus(GdiHttpService.HttpService.Status.FILE_TOO_LARGE)
                        .setHttpStatus(0)
                        .build();
            }

            final JsonElement jsonElement;
            if ("application/json".equals(response.getHeaders().get("content-type"))) {
                try (final InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(response.getBody()))) {
                    jsonElement = JsonParser.parseReader(reader);
                } catch (IOException e) {
                    LOG.error("Failed to decode body as json", e);
                    return GdiHttpService.HttpService.WebResponse.newBuilder()
                            .setStatus(GdiHttpService.HttpService.Status.DATA_TRANSFER_ITEM_FAILURE)
                            .setHttpStatus(0)
                            .build();
                }
            } else if (webRequest.getResponseType() == GdiHttpService.HttpService.ResponseType.JSON) {
                // We got non-json
                return GdiHttpService.HttpService.WebResponse.newBuilder()
                        .setStatus(GdiHttpService.HttpService.Status.DATA_TRANSFER_ITEM_FAILURE)
                        .setHttpStatus(response.getStatus())
                        .build();
            } else {
                jsonElement = new JsonPrimitive(new String(response.getBody(), StandardCharsets.UTF_8));
            }
            final byte[] body = GarminJson.encode(jsonElement);

            final GdiHttpService.HttpService.WebResponse.Builder builder = GdiHttpService.HttpService.WebResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.OK)
                    .setHttpStatus(response.getStatus())
                    .setBody(ByteString.copyFrom(body))
                    .setSize(0); // 0 for non-compressed

            if (headers != null) {
                builder.setHeaders(ByteString.copyFrom(headers));
            }

            return builder.build();
        } catch (final GarminJsonException e) {
            LOG.error("Garmin json error (?)", e);
            return null;
        }
    }
}
