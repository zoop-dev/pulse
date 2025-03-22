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

import android.os.Handler;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsVoiceMemosService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsVoiceMemosService.class);

    private static final short ENDPOINT = 0x0033;

    private static final byte CMD_LIST_REQUEST = 0x05;
    private static final byte CMD_LIST_RESPONSE = 0x06;
    private static final byte CMD_DOWNLOAD_START_REQUEST = 0x07;
    private static final byte CMD_DOWNLOAD_START_ACK = 0x08;
    private static final byte CMD_DOWNLOAD_FINISH_REQUEST = 0x0a;
    private static final byte CMD_DOWNLOAD_FINISH_ACK = 0x09;

    private final Queue<String> downloadQueue = new LinkedList<>();
    private boolean downloading = false;
    private final Handler handler = new Handler();

    public ZeppOsVoiceMemosService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void dispose() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        switch (buf.get()) {
            case CMD_LIST_RESPONSE:
                final short count = buf.getShort();
                LOG.info("Got list with {} voice memos", count);

                for (int i = 0; i < count; i++) {
                    final String filename = StringUtils.untilNullTerminator(buf);
                    final int size = buf.getInt();
                    final int duration = buf.getInt();
                    final long timestamp = buf.getLong();

                    LOG.debug(
                            "Voice memo: filename={}, size={}b, duration={}ms, timestamp={}",
                            filename, size, duration, DateTimeUtils.formatIso8601(new Date(timestamp))
                    );

                    downloadStart(Objects.requireNonNull(filename));
                }

                return;
            case CMD_DOWNLOAD_START_ACK:
                LOG.info("Download start ACK, status = {}", payload[1]);
                return;
            case CMD_DOWNLOAD_FINISH_ACK:
                LOG.info("Download finish ACK, status = {}", payload[1]);
                return;
        }

        LOG.warn("Unexpected voice memos byte {}", String.format("0x%02x", payload[0]));
    }

    public void requestList() {
        write("get voice memos list", CMD_LIST_REQUEST);
    }

    public void downloadStart(final String filename) {
        LOG.debug("Queuing voice memo download for {}", filename);

        downloadQueue.add(filename);
        if (!downloading) {
            downloading = true;
            downloadNext();
        }
    }

    public void downloadFinish() {
        downloadNext();
    }

    private void downloadNext() {
        handler.removeCallbacksAndMessages(null);

        final String filename = downloadQueue.poll();

        if (filename != null) {
            // Timeout after a while so we do not get stuck
            handler.postDelayed(() -> {
                LOG.warn("Timed out waiting for voice memo download, triggering next");
                downloadNext();
            }, 5000L);

            LOG.debug("Will download voice memo {}", filename);
            write(
                    "voice memo download " + filename,
                    ArrayUtils.addAll(new byte[]{CMD_DOWNLOAD_START_REQUEST}, filename.getBytes())
            );
        } else {
            LOG.debug("Voice memo downloads finished");
            downloading = false;
            write("voice memo download finish", CMD_DOWNLOAD_FINISH_REQUEST);
        }
    }
}
