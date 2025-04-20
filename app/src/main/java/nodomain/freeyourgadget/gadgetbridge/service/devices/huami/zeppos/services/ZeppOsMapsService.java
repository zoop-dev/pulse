/*  Copyright (C) 2025 José Rebelo

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

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsMapsFile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ZeppOsMapsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMapsService.class);

    private static final short ENDPOINT = 0x0046;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_DOWNLOAD_START_REQUEST = 0x05;
    private static final byte CMD_DOWNLOAD_START_RESPONSE = 0x06;

    private final ZeppOsHttpService httpService;

    public ZeppOsMapsService(final ZeppOsSupport support, final ZeppOsHttpService httpService) {
        super(support, true);
        this.httpService = httpService;
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        switch (buf.get()) {
            case CMD_CAPABILITIES_RESPONSE:
                final int version = payload[1] & 0xff; // 1 supported
                final int unk1 = payload[2] & 0xff; // 1?
                LOG.info("Maps version={}, unk1={}", version, unk1);
                return;
            case CMD_DOWNLOAD_START_RESPONSE:
                LOG.info("Download start response, status = {}", payload[1]); // 1 = success
                return;
        }

        LOG.warn("Unexpected maps byte {}", String.format("0x%02x", payload[0]));
    }

    public void upload(final ZeppOsMapsFile mapsFile) {
        final List<String> urls = new ArrayList<>(1);
        final int i = 0;
        final long uncompressedSize = mapsFile.getUncompressedSize();
        final String fakeUrl = String.format(
                Locale.ROOT,
                "https://gadgetbridge.freeyourgadget.nodomain/map/%d.zip?type=1&zipsize=%d",
                i,
                mapsFile.getFileSize()
        );
        urls.add(fakeUrl);
        httpService.registerForDownload(fakeUrl, mapsFile.getUri(), new ZeppOsHttpService.Callback() {
            @Override
            public void onFileDownloadFinish(final boolean success) {
                final String notificationMessage = success ?
                        getContext().getString(R.string.map_upload_complete) :
                        getContext().getString(R.string.map_upload_failed);

                GB.updateInstallNotification(notificationMessage, false, 100, getContext());
                final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

                broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_INFO_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, ""));
                broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, notificationMessage));
                broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_FINISHED));
            }

            @Override
            public void onFileDownloadProgress(final int progress) {
                final int progressPercent = (int) ((((float) (progress)) / mapsFile.getFileSize()) * 100);
                updateProgress(progressPercent);
            }
        });

        LOG.debug("Url for map file {}: {}", mapsFile.getUri(), fakeUrl);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(CMD_DOWNLOAD_START_REQUEST);
            baos.write(BLETypeConversions.fromUint32((int) uncompressedSize));
            baos.write(BLETypeConversions.fromUint16(urls.size()));
            for (final String url : urls) {
                baos.write(url.getBytes(StandardCharsets.UTF_8));
                baos.write(0);
            }
            baos.write(1); // ?
        } catch (final IOException e) {
            LOG.error("Failed to build command", e);
            return;
        }

        write("upload maps", baos.toByteArray());
    }

    private void updateProgress(final int progressPercent) {
        GB.updateInstallNotification(
                getContext().getString(R.string.map_upload_in_progress),
                true,
                progressPercent,
                getContext()
        );

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_PROGRESS, progressPercent));
    }
}
