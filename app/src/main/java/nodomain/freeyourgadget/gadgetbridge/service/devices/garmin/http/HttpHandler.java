package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.HttpInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.ImageServiceInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.OauthInterceptor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.interceptors.WeatherInterceptor;

public class HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private final List<HttpInterceptor> interceptors;

    public HttpHandler(GarminSupport deviceSupport) {
        interceptors = Arrays.asList(
                new WeatherInterceptor(),
                new AgpsInterceptor(deviceSupport),
                new ImageServiceInterceptor(deviceSupport),
                new ContactsInterceptor(deviceSupport),
                new OauthInterceptor(deviceSupport)
        );
    }

    public GdiHttpService.HttpService handle(final GdiHttpService.HttpService httpService) {
        if (httpService.hasRawRequest()) {
            final GdiHttpService.HttpService.RawResponse rawResponse = handleRawRequest(httpService.getRawRequest());
            if (rawResponse != null) {
                return GdiHttpService.HttpService.newBuilder()
                        .setRawResponse(rawResponse)
                        .build();
            }
            return null;
        } else if (httpService.hasWebRequest()) {
            final GdiHttpService.HttpService.WebResponse webResponse = handleWebRequest(httpService.getWebRequest());
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

    public GdiHttpService.HttpService.RawResponse handleRawRequest(final GdiHttpService.HttpService.RawRequest rawRequest) {
        LOG.debug("Got rawRequest: {} - {}", rawRequest.getMethod(), rawRequest.getUrl());

        final GarminHttpRequest request = new GarminHttpRequest(rawRequest);

        final GarminHttpResponse response = handleRequest(request);

        if (response == null) {
            return GdiHttpService.HttpService.RawResponse.newBuilder()
                    .setStatus(GdiHttpService.HttpService.Status.UNKNOWN_STATUS)
                    .build();
        }

        LOG.debug("Http response status={}", response.getStatus());

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
        return interceptor.handle(request);
    }

    private static GdiHttpService.HttpService.RawResponse createRawResponse(
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

    public GdiHttpService.HttpService.WebResponse handleWebRequest(final GdiHttpService.HttpService.WebRequest webRequest) {
        LOG.debug("Got webRequest: {} - {}", webRequest.getMethod(), webRequest.getUrl());

        return null;
    }
}
