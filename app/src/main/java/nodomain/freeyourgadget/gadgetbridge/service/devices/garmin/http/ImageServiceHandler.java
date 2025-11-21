package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;

public class ImageServiceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceHandler.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    public GarminHttpResponse handleRequest(final GarminHttpRequest request) {
        if (request.getRawRequest().getMethod() != GdiHttpService.HttpService.Method.GET) {
            LOG.warn("Known image service requests should be GET");
            return null;
        }

        if (!request.getPath().equals("/image-service/v2/device/images/details")) {
            LOG.warn("Unknown image service path {}", request.getPath());
            return null;
        }

        final String ownerAliasId = request.getQuery().get("ownerAliasId");

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

    public record ErrorResponse(String requestId, List<Error> errors) {
    }

    public record Error(String message, String type) {
    }
}
