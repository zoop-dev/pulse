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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.CompressionUtils;

public class ZeppOsFileTransferV2 extends ZeppOsFileTransferImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferV2.class);

    private final Map<Byte, FileTransferRequest> mSessionRequests = new HashMap<>();

    public ZeppOsFileTransferV2(final ZeppOsFileTransferService fileTransferService,
                                final ZeppOsSupport support) {
        super(fileTransferService, support);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        byte session;
        byte status;

        switch (payload[0]) {
            case CMD_TRANSFER_RESPONSE:
                session = payload[1];
                status = payload[2];
                final int existingProgress = BLETypeConversions.toUint32(payload, 3);
                LOG.info("Band acknowledged file transfer request: session={}, status={}, existingProgress={}", session, status, existingProgress);
                if (status != 0) {
                    LOG.error("Unexpected status from band for session {}, aborting", session);
                    onUploadFinish(session, false);
                    return;
                }
                if (existingProgress != 0) {
                    LOG.info("Updating existing progress for session {} to {}", session, existingProgress);
                    final FileTransferRequest request = mSessionRequests.get(session);
                    if (request == null) {
                        LOG.error("No request found for session {}", session);
                        return;
                    }
                    request.setProgress(existingProgress);
                }
                sendNextChunk(session);
                return;
            case CMD_DATA_SEND:
                handleFileTransferData(payload);
                return;
            case CMD_DATA_ACK:
                session = payload[1];
                status = payload[2];
                LOG.info("Band acknowledged file transfer data: session={}, status={}", session, status);
                if (status != 0) {
                    LOG.error("Unexpected status from band, aborting session {}", session);
                    onUploadFinish(session, false);
                    return;
                }
                sendNextChunk(session);
                return;
            default:
                super.handlePayload(payload);
        }
    }

    @Override
    public void uploadFile(final FileTransferRequest request) {
        if (request.isCompressed()) {
            throw new IllegalArgumentException("V1/V2 does not support compressed transfers");
        }

        byte session = (byte) mSessionRequests.size();
        while (mSessionRequests.containsKey(session)) {
            session++;
        }

        final int payloadSize = 2 + request.getUrl().length() + 1 + request.getFilename().length() + 1 + 4 + 4;

        final ByteBuffer buf = ByteBuffer.allocate(payloadSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_TRANSFER_REQUEST);
        buf.put(session);
        buf.put(request.getUrl().getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put(request.getFilename().getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.putInt(request.getRawLength());
        buf.putInt(request.getCrc32());

        mFileTransferService.write("send file upload request", buf.array());

        mSessionRequests.put(session, request);
    }

    @Override
    public void handleFileDownloadRequest(final byte session, final FileTransferRequest request) {
        final ByteBuffer buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_TRANSFER_RESPONSE);
        buf.put(session);
        buf.put((byte) 0x00);
        buf.putInt(0);

        mFileTransferService.write("send file transfer response", buf.array());

        mSessionRequests.put(session, request);
    }

    @Override
    public void onCharacteristicChanged(final UUID characteristicUUID, final byte[] value) {
        LOG.error("Unknown characteristic changed: {}", characteristicUUID);
    }

    private void handleFileTransferData(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // Discard first byte

        final byte flags = buf.get();
        final boolean firstChunk = (flags & FLAG_FIRST_CHUNK) != 0;
        final boolean lastChunk = (flags & FLAG_LAST_CHUNK) != 0;
        final byte session = buf.get();
        final byte index = buf.get();

        if (firstChunk) {
            buf.getInt(); // ?
        }

        final short size = buf.getShort();

        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No download request found for V1 session {}", session);
            return;
        }

        if (index != request.getIndex()) {
            LOG.warn("Unexpected index {}, expected {}", index, request.getIndex());
            return;
        }

        if (firstChunk && request.getProgress() != 0) {
            LOG.warn("Got first packet, but progress is {}", request.getProgress());
            return;
        }

        buf.get(request.getBytes(), request.getProgress(), size);
        request.setIndex(index + 1);
        request.setProgress(request.getProgress() + size);

        LOG.debug("Got data for session={}, progress={}/{}", session, request.getProgress(), request.getSize());

        mFileTransferService.write("ack file data", new byte[]{CMD_DATA_ACK, session, 0x00});

        if (lastChunk) {
            mSessionRequests.remove(session);

            final byte[] data;
            if (request.isCompressed()) {
                data = CompressionUtils.INSTANCE.inflate(request.getBytes());
                if (data == null) {
                    LOG.error("Failed to decompress bytes for session={}", session);
                    return;
                }
            } else {
                data = request.getBytes();
            }

            final int checksum = CheckSums.getCRC32(data);
            if (checksum != request.getCrc32()) {
                LOG.warn("Checksum mismatch: expected {}, got {}", request.getCrc32(), checksum);
                return;
            }

            request.getCallback().onFileDownloadFinish(request.getUrl(), request.getFilename(), data);
        }
    }

    private void onUploadFinish(final byte session, final boolean success) {
        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {} to finish upload", session);
            return;
        }

        mSessionRequests.remove(session);

        request.getCallback().onFileUploadFinish(success);
    }

    private void sendNextChunk(final byte session) {
        final FileTransferRequest request = mSessionRequests.get(session);
        if (request == null) {
            LOG.error("No request found for session {} to send next chunk", session);
            return;
        }

        if (request.getProgress() >= request.getSize()) {
            LOG.info("Finished sending {}", request.getUrl());
            onUploadFinish(session, true);
            return;
        }

        LOG.debug("Sending file data for session={}, progress={}, index={}", session, request.getProgress(), request.getIndex());

        writeChunk(session, request);
    }

    private void writeChunk(final byte session, final FileTransferRequest request) {
        final ByteBuffer buf = ByteBuffer.allocate(10 + request.getChunkSize());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DATA_SEND);

        byte flags = 0;
        if (request.getProgress() == 0) {
            flags |= FLAG_FIRST_CHUNK;
        }
        if (request.getProgress() + request.getChunkSize() >= request.getSize()) {
            flags |= FLAG_LAST_CHUNK;
        }

        buf.put(flags);
        buf.put(session);
        buf.put((byte) request.getIndex());
        if ((flags & FLAG_FIRST_CHUNK) > 0) {
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
            buf.put((byte) 0x00); // ?
        }

        final byte[] payload = ArrayUtils.subarray(
                request.getBytes(),
                request.getProgress(),
                request.getProgress() + request.getChunkSize()
        );

        buf.putShort((short) payload.length);
        buf.put(payload);

        request.setProgress(request.getProgress() + payload.length);
        request.setIndex(request.getIndex() + 1);
        request.getCallback().onFileUploadProgress(request.getProgress());

        mFileTransferService.write("send file data", buf.array());
    }
}
