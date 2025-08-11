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

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.calcMaxWriteChunk;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.CompressionUtils;

public class ZeppOsFileTransferV3 extends ZeppOsFileTransferImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFileTransferV3.class);

    private static final byte CMD_DATA_V3_SEND = 0x12;
    private static final byte CMD_DATA_V3_ACK = 0x13;
    private static final byte CMD_CANCEL_TRANSFER_REQ = 0x20; // 20:01
    private static final byte CMD_CANCEL_TRANSFER_ACK = 0x21; // 21:01:00

    private static final long TRANSFER_TIMEOUT_THRESHOLD_MILLIS = 5_000L;

    private byte nextSession = 0;

    private FileTransferRequest currentSendRequest;
    private byte currentSendSession = -1;
    private long lastSendActivityMillis = -1;

    private FileTransferRequest currentReceiveRequest;
    private byte currentReceiveSession = -1;
    private long lastReceiveActivityMillis = -1;

    private int currentReceiveChunkSize = -1;
    private boolean currentReceiveChunkIsLast = false;
    private final ByteArrayOutputStream receivePacketBuffer = new ByteArrayOutputStream();

    public ZeppOsFileTransferV3(final ZeppOsFileTransferService fileTransferService,
                                final ZeppOsSupport support) {
        super(fileTransferService, support);
    }

    /** @noinspection SwitchStatementWithTooFewBranches*/
    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_TRANSFER_RESPONSE:
                final byte session = payload[1];
                final byte status = payload[2];
                final int existingProgress = BLETypeConversions.toUint32(payload, 3);
                LOG.info("Band acknowledged file transfer request: session={}, status={}, existingProgress={}", session, status, existingProgress);
                if (currentSendRequest == null) {
                    LOG.error("No ongoing send request found");
                    return;
                }
                if (status != 0) {
                    LOG.error("Unexpected status from band for session {}, aborting", session);
                    onUploadFinish(false);
                    resetSend();
                    return;
                }
                if (currentSendSession != session) {
                    LOG.error("Unexpected send session from band {}, expected {}, aborting", session, currentSendSession);
                    onUploadFinish(false);
                    resetSend();
                    return;
                }
                if (existingProgress != 0) {
                    LOG.info("Updating existing progress for session {} to {}", session, existingProgress);
                    currentSendRequest.setProgress(existingProgress);
                }
                sendNextQueuedData();
                return;
            default:
                super.handlePayload(payload);
        }
    }

    @Override
    public void uploadFile(final FileTransferRequest request) {
        if (currentSendRequest != null) {
            if (System.currentTimeMillis() - lastSendActivityMillis < TRANSFER_TIMEOUT_THRESHOLD_MILLIS) {
                LOG.warn("Already uploading {}", currentSendRequest.getFilename());
                request.getCallback().onFileUploadFinish(false);
                return;
            }

            LOG.warn("Timing out existing upload request for {}", currentSendRequest.getFilename());
            currentSendRequest.getCallback().onFileUploadFinish(false);
            resetSend();
        }

        final byte session = nextSession++;

        int payloadSize = 2 +
                request.getUrl().getBytes(StandardCharsets.UTF_8).length + 1 +
                request.getFilename().getBytes(StandardCharsets.UTF_8).length + 1 +
                4 + 4 + 2;
        if (request.isCompressed()) {
            payloadSize += 4;
        }

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
        buf.put((byte) (request.isCompressed() ? 1 : 0));
        if (request.isCompressed()) {
            buf.putInt(mCompressedChunkSize);
        }
        buf.put((byte) 0);

        mFileTransferService.write("send v3 file upload request", buf.array());

        currentSendSession = session;
        currentSendRequest = request;
        lastSendActivityMillis = System.currentTimeMillis();
    }

    @Override
    public void handleFileDownloadRequest(final byte session, final FileTransferRequest request) {
        if (currentReceiveRequest != null) {
            if (System.currentTimeMillis() - lastReceiveActivityMillis < TRANSFER_TIMEOUT_THRESHOLD_MILLIS) {
                LOG.warn("Already downloading {}", currentReceiveRequest.getFilename());
                // TODO how to send nack?
                return;
            }

            LOG.warn("Timing out existing download request for {}", currentReceiveRequest.getFilename());
            currentReceiveRequest.getCallback().onFileUploadFinish(false);

            resetReceive();
        }

        final ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_TRANSFER_RESPONSE);
        buf.put(session);
        buf.put((byte) 0x00);
        buf.putInt(0);
        buf.put((byte) 0x01);

        mFileTransferService.write("send file transfer response", buf.array());

        currentReceiveChunkIsLast = false;
        currentReceiveChunkSize = -1;
        receivePacketBuffer.reset();
        currentReceiveSession = session;
        currentReceiveRequest = request;
        lastReceiveActivityMillis = System.currentTimeMillis();
    }

    @Override
    public void onCharacteristicChanged(final UUID characteristicUUID, final byte[] value) {
        if (HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_RECEIVE.equals(characteristicUUID)) {
            handleFileReceiveData(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_SEND.equals(characteristicUUID)) {
            if (value[0] != CMD_DATA_V3_ACK) {
                LOG.error("Got non-ack on file send characteristic");
                return;
            }

            final byte status = value[1];
            final int chunkIndex = value[2] & 0xff;
            final byte unk1 = value[3]; // 1/2?

            LOG.info(
                    "Band acknowledged file transfer data: session={}, status={}, chunkIndex={}, unk1={}",
                    currentSendSession,
                    status,
                    chunkIndex,
                    unk1
            );

            if (currentSendRequest == null) {
                LOG.error("Got ack for file send, but we are not uploading");
                return;
            }

            if (status != 0) {
                LOG.error("Unexpected status from band, aborting session {}", currentSendSession);
                onUploadFinish(false);
                return;
            }

            if (currentSendRequest.getIndex() - 1 != chunkIndex) {
                LOG.error("Got ack for unexpected chunk index {}, expected {}", chunkIndex, currentSendRequest.getIndex() - 1);
                onUploadFinish(false);
                return;
            }

            sendNextQueuedData();
        } else {
            LOG.warn("Unknown characteristic changed: {}", characteristicUUID);
        }
    }

    private void writeChunk(final FileTransferRequest request) {
        final byte[] chunk = ArrayUtils.subarray(
                request.getBytes(),
                request.getProgress(),
                request.getProgress() + request.getChunkSize()
        );

        byte flags = 0;
        if (request.getProgress() == 0) {
            flags |= FLAG_FIRST_CHUNK;
        }
        if (request.getProgress() + request.getChunkSize() >= request.getSize()) {
            flags |= FLAG_LAST_CHUNK;
        }

        final int partSize = calcMaxWriteChunk(mSupport.getMTU());

        final ByteBuffer buf = ByteBuffer.allocate(chunk.length + 5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DATA_V3_SEND);
        buf.put(flags);
        buf.put((byte) request.getIndex());
        buf.putShort((short) chunk.length);
        buf.put(chunk);

        final byte[] payload = buf.array();

        final ZeppOsTransactionBuilder builder = mSupport.createZeppOsTransactionBuilder("send chunk v3");
        for (int i = 0; i < payload.length; i += partSize) {
            final byte[] part = ArrayUtils.subarray(payload, i, i + partSize);
            builder.write(HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_SEND, part);
        }
        builder.queue();

        request.setProgress(request.getProgress() + chunk.length);
        request.setIndex(request.getIndex() + 1);
        request.getCallback().onFileUploadProgress(request.getProgress());
    }

    private void handleFileReceiveData(final byte[] payload) {
        if (currentReceiveRequest == null) {
            LOG.error("No receive request found for V3 session {}", currentReceiveSession);
            return;
        }

        lastReceiveActivityMillis = System.currentTimeMillis();

        if (currentReceiveChunkSize > 0) {
            // We are currently receiving a chunk
            try {
                receivePacketBuffer.write(payload);
            } catch (final IOException e) {
                LOG.error("Failed to write packet to chunk buffer", e);
                resetReceive();
                return;
            }

            LOG.debug(
                    "Received {} ({}/{}) bytes for chunk at index {}",
                    payload.length,
                    receivePacketBuffer.size(),
                    currentReceiveChunkSize,
                    currentReceiveRequest.getIndex()
            );
        } else {
            // Start of a chunk
            receivePacketBuffer.reset();

            final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
            buf.get(); // Discard first byte
            final byte flags = buf.get();
            final boolean firstChunk = (flags & FLAG_FIRST_CHUNK) != 0;
            currentReceiveChunkIsLast = (flags & FLAG_LAST_CHUNK) != 0;
            final byte index = buf.get();
            currentReceiveChunkSize = buf.getShort();

            if (index != currentReceiveRequest.getIndex()) {
                LOG.warn("Unexpected V3 index {}, expected {}", index, currentReceiveRequest.getIndex());
                return;
            }

            if (firstChunk && currentReceiveRequest.getProgress() != 0) {
                LOG.warn("Got first V3 packet, but progress is {}", currentReceiveRequest.getProgress());
                return;
            }

            LOG.debug(
                    "Got start of chunk - payload={}b, firstChunk={}, lastChunk={}, index={}, chunkSize={}",
                    payload.length, firstChunk, currentReceiveChunkIsLast, index, currentReceiveChunkSize
            );

            receivePacketBuffer.write(payload, buf.position(), buf.limit() - buf.position());
        }

        LOG.trace(
                "Chunk buffer: {} of {}, currentReceiveChunkIsLast={}",
                receivePacketBuffer.size(),
                currentReceiveChunkSize,
                currentReceiveChunkIsLast
        );

        if (receivePacketBuffer.size() >= currentReceiveChunkSize) {
            // Finished a chunk

            System.arraycopy(
                    receivePacketBuffer.toByteArray(),
                    0,
                    currentReceiveRequest.getBytes(),
                    currentReceiveRequest.getProgress(),
                    currentReceiveChunkSize
            );

            currentReceiveRequest.setIndex(currentReceiveRequest.getIndex() + 1);
            currentReceiveRequest.setProgress(currentReceiveRequest.getProgress() + currentReceiveChunkSize);

            LOG.debug("Got V3 data for session={}, progress={}/{}", currentReceiveSession, currentReceiveRequest.getProgress(), currentReceiveRequest.getSize());

            final ZeppOsTransactionBuilder builder = mSupport.createZeppOsTransactionBuilder("send ack v3 file data");
            builder.write(
                    HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_RECEIVE,
                    new byte[]{
                            CMD_DATA_V3_ACK,
                            0x00,
                            (byte) (currentReceiveRequest.getIndex() - 1),
                            (byte) 0x01,
                            (byte) 0x00,
                            (byte) 0x00,
                            (byte) 0x00
                    }
            );
            builder.queue();

            if (currentReceiveChunkIsLast) {
                final byte[] data;
                if (currentReceiveRequest.isCompressed()) {
                    data = CompressionUtils.INSTANCE.inflate(currentReceiveRequest.getBytes());
                    if (data == null) {
                        LOG.error("Failed to decompress V3 bytes for {}", currentReceiveRequest.getFilename());
                        resetReceive();
                        return;
                    }
                } else {
                    data = currentReceiveRequest.getBytes();
                }

                final int checksum = CheckSums.getCRC32(data);
                if (checksum != currentReceiveRequest.getCrc32()) {
                    LOG.warn("V3 Checksum mismatch: expected {}, got {}", currentReceiveRequest.getCrc32(), checksum);
                    resetReceive();
                    return;
                }

                final FileTransferRequest requestBackup = currentReceiveRequest;

                resetReceive();

                requestBackup.getCallback().onFileDownloadFinish(
                        requestBackup.getUrl(),
                        requestBackup.getFilename(),
                        data
                );
            }

            currentReceiveChunkSize = -1;
            receivePacketBuffer.reset();
        }
    }

    private void sendNextQueuedData() {
        if (currentSendRequest == null) {
            LOG.error("No ongoing V3 send request found");
            return;
        }

        if (currentSendRequest.getProgress() >= currentSendRequest.getSize()) {
            LOG.info("Finished sending {}", currentSendRequest.getUrl());
            onUploadFinish(true);
            return;
        }

        LOG.debug(
                "Sending file data for session={}, progress={}, index={}",
                currentSendSession,
                currentSendRequest.getProgress(),
                currentSendRequest.getIndex()
        );

        lastSendActivityMillis = System.currentTimeMillis();

        writeChunk(currentSendRequest);
    }

    private void onUploadFinish(final boolean success) {
        if (currentSendRequest == null) {
            LOG.error("No request found for session {} to finish upload", currentSendSession);
            return;
        }

        currentSendRequest.getCallback().onFileUploadFinish(success);

        resetSend();
    }

    private void resetSend() {
        currentSendSession = -1;
        currentSendRequest = null;
        lastSendActivityMillis = -1;
    }

    private void resetReceive() {
        currentReceiveSession = -1;
        currentReceiveRequest = null;
        lastReceiveActivityMillis = -1;
        currentReceiveChunkIsLast = false;
        currentReceiveChunkSize = -1;
        receivePacketBuffer.reset();
    }
}
