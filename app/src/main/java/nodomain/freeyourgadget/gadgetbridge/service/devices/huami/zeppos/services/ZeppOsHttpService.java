/*  Copyright (C) 2023-2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.net.Uri;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsWeatherHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.HttpUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsHttpService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsHttpService.class);

    private static final short ENDPOINT = 0x0001;

    public static final byte CMD_SIMPLE_REQUEST = 0x01;
    public static final byte CMD_SIMPLE_RESPONSE = 0x02;
    public static final byte CMD_RAW_DOWNLOAD_REQUEST = 0x03;
    public static final byte CMD_RAW_DOWNLOAD_START = 0x04;
    public static final byte CMD_RAW_DOWNLOAD_FINISH = 0x05;

    public static final byte RESPONSE_SUCCESS = 0x01;
    public static final byte RESPONSE_NO_INTERNET = 0x02;

    private ZeppOsWeatherHandler weatherHandler;

    /**
     * A map from url to local file URI that will be served to the watch.
     */
    private final Map<String, Uri> urlToLocalFile = new HashMap<>();
    private final Map<String, Callback> urlDownloadCallbacks = new HashMap<>();

    private final ZeppOsFileTransferService fileTransferService;

    public ZeppOsHttpService(final ZeppOsSupport support, final ZeppOsFileTransferService fileTransferService) {
        super(support, true);
        this.fileTransferService = fileTransferService;
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard command byte

        switch (payload[0]) {
            case CMD_SIMPLE_REQUEST: {
                final int requestId = buf.get() & 0xff;
                final String method = StringUtils.untilNullTerminator(buf);
                if (method == null) {
                    LOG.error("Failed to decode method from payload");
                    replyHttpNoInternet(requestId);
                    return;
                }
                final String url = StringUtils.untilNullTerminator(buf);
                if (url == null) {
                    LOG.error("Failed to decode url from payload");
                    return;
                }
                // headers after, but we ignore them

                LOG.info("Got simple HTTP {} request: {}", method, url);

                handleUrlRequest(requestId, method, url);
                return;
            }

            case CMD_RAW_DOWNLOAD_REQUEST: {
                final int requestId = buf.get() & 0xff;
                final String url = StringUtils.untilNullTerminator(buf);
                if (url == null) {
                    LOG.error("Failed to decode raw download url from payload");
                    return;
                }
                // headers after, but we ignore them

                LOG.info("Got raw download HTTP request: {}", url);

                handleRawDownloadRequest(requestId, url);
                return;
            }

            default:
                LOG.warn("Unexpected HTTP payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        this.weatherHandler = new ZeppOsWeatherHandler(getSupport().getDevice());
        urlToLocalFile.clear();
        urlDownloadCallbacks.clear();
    }

    public void registerForDownload(final String url, final Uri uri, @Nullable final Callback callback) {
        this.urlToLocalFile.put(url, uri);
        this.urlDownloadCallbacks.put(url, callback);
    }

    private void handleUrlRequest(final int requestId, final String method, final String urlString) {
        if (!"GET".equals(method)) {
            LOG.error("Unable to handle HTTP method {}", method);
            // TODO: There's probably a "BAD REQUEST" response or similar
            replyHttpNoInternet(requestId);
            return;
        }

        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            LOG.error("Failed to parse simple request url", e);
            replyHttpNoInternet(requestId);
            return;
        }

        final String path = url.getPath();
        final Map<String, String> query = HttpUtils.urlQueryParameters(url);

        if (path.startsWith("/weather/")) {
            if (weatherHandler != null) {
                final ZeppOsWeatherHandler.Response response = weatherHandler.handleHttpRequest(path, query);
                replySimpleHttpSuccess(requestId, response.getHttpStatusCode(), response.toJson());
                return;
            }

            LOG.error("Weather handler is null");
        }

        LOG.error("Unhandled simple request URL {}", url);
        replyHttpNoInternet(requestId);
    }

    private void handleRawDownloadRequest(final int requestId, final String urlString) {
        final Uri uri = urlToLocalFile.remove(urlString);
        if (uri == null) {
            LOG.error("Unhandled raw download URL {}", urlString);
            replyHttpNoInternet(requestId);
            return;
        }

        final Callback downloadCallback = urlDownloadCallbacks.remove(urlString);

        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            LOG.error("Failed to parse raw download url", e);
            replyHttpNoInternet(requestId);
            return;
        }

        final String filename = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);

        final byte[] rawBytes;
        try (InputStream is = getContext().getContentResolver().openInputStream(uri)) {
            // FIXME we probably should stream this
            // with the current implementation, anything over ~2MB will overflow the chunk index
            rawBytes = FileUtils.readAll(Objects.requireNonNull(is), 100 * 1024 * 1024); // 10MB
        } catch (final IOException e) {
            LOG.error("Failed to read local file for {}", urlString, e);
            replyHttpNoInternet(requestId);
            return;
        }

        LOG.debug("Starting raw download request {} with {} bytes", requestId, rawBytes.length);

        fileTransferService.sendFile(
                "httpproxy://download?sessionid=" + requestId,
                filename,
                rawBytes,
                false,
                new ZeppOsFileTransferService.UploadCallback() {
                    @Override
                    public void onFileUploadFinish(final boolean success) {
                        LOG.info("Finished sending '{}' to http request id '{}', success={}", filename, requestId, success);
                        onRawDownloadFinish(requestId, success);
                        if (downloadCallback != null) {
                            downloadCallback.onFileDownloadFinish(success);
                        }
                    }

                    @Override
                    public void onFileUploadProgress(final int progress) {
                        LOG.trace("HTTP send progress: {}", progress);
                        if (downloadCallback != null) {
                            downloadCallback.onFileDownloadProgress(progress);
                        }
                    }
                }
        );

        final ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_RAW_DOWNLOAD_START);
        buf.put((byte) requestId);
        buf.putInt(rawBytes.length);
        buf.putInt(0); // ?

        write("http raw download start", buf.array());
    }

    private void onRawDownloadFinish(final int requestId, final boolean success) {
        LOG.debug("Download {} finished, success = {}", requestId, success);
        final ByteBuffer buf = ByteBuffer.allocate(5);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_RAW_DOWNLOAD_FINISH);
        buf.put((byte) requestId);
        buf.put((byte) 1); // success?
        buf.putShort((short) 200);

        write("http raw download finish", buf.array());
    }

    private void replyHttpNoInternet(final int requestId) {
        LOG.info("Replying with no internet to http request {}", requestId);

        final byte[] cmd = new byte[]{CMD_SIMPLE_RESPONSE, (byte) requestId, RESPONSE_NO_INTERNET, 0x00, 0x00, 0x00, 0x00};

        write("http reply no internet", cmd);
    }

    private void replySimpleHttpSuccess(final int requestId, final int status, final String content) {
        LOG.debug("Replying with http {} request {} with {}", status, requestId, content);

        final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buf = ByteBuffer.allocate(8 + contentBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_SIMPLE_RESPONSE);
        buf.put((byte) requestId);
        buf.put(RESPONSE_SUCCESS);
        buf.put((byte) status);
        buf.putInt(contentBytes.length);
        buf.put(contentBytes);

        write("http reply success", buf.array());
    }

    public interface Callback {
        void onFileDownloadFinish(boolean success);

        void onFileDownloadProgress(int progress);
    }
}
