package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import android.net.Uri;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiHttpService;
import nodomain.freeyourgadget.gadgetbridge.util.HttpUtils;

public class GarminHttpRequest {
    private final GdiHttpService.HttpService.RawRequest rawRequest;
    private final GdiHttpService.HttpService.WebRequest webRequest;

    private final int messageRequestId;

    private final String method;
    private final Uri uri;
    private final Map<String, String> headers;

    public GarminHttpRequest(final GdiHttpService.HttpService.RawRequest rawRequest,
                             final int messageRequestId) {
        this.messageRequestId = messageRequestId;
        this.rawRequest = rawRequest;
        this.webRequest = null;
        this.method = rawRequest.getMethod().name();
        this.uri = Uri.parse(rawRequest.getUrl());
        this.headers = headersToMap(rawRequest.getHeaderList());
    }

    public GarminHttpRequest(final GdiHttpService.HttpService.WebRequest webRequest,
                             final int messageRequestId) throws GarminJsonException {
        this.messageRequestId = messageRequestId;
        this.rawRequest = null;
        this.webRequest = webRequest;
        this.method = webRequest.getMethod().name();
        this.uri = Uri.parse(webRequest.getUrl());
        final JsonElement garminHeaders = GarminJson.decode(webRequest.getHeaders().toByteArray());
        this.headers = new LinkedHashMap<>();
        if (garminHeaders instanceof JsonObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                headers.put(key, jsonObject.get(key).getAsString());
            }
        }
    }

    public int getMessageRequestId() {
        return messageRequestId;
    }

    public GdiHttpService.HttpService.RawRequest getRawRequest() {
        return rawRequest;
    }

    public GdiHttpService.HttpService.WebRequest getWebRequest() {
        return webRequest;
    }

    public Uri getUri() {
        return uri;
    }

    public String getDomain() {
        return uri.getHost();
    }

    public String getUrl() {
        return rawRequest != null ? rawRequest.getUrl() : webRequest.getUrl();
    }

    public String getPath() {
        return uri.getPath();
    }

    public byte[] getBody() {
        return rawRequest != null ? rawRequest.getRawBody().toByteArray() : webRequest.getBody().toByteArray();
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getQuery() {
        return HttpUtils.urlQueryParameters(uri.getQuery());
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    private static Map<String, String> headersToMap(final List<GdiHttpService.HttpService.Header> headers) {
        final Map<String, String> ret = new HashMap<>();
        for (final GdiHttpService.HttpService.Header header : headers) {
            ret.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
        }
        return ret;
    }
}
