/*  Copyright (C) 2023-2024 José Rebelo, Yoran Vulker

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.DATA_TYPE_PLAIN;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.OPCODE_READ;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiChannelHandler.Channel;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiSppSupport extends XiaomiConnectionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppSupport.class);

    AbstractBTBRDeviceSupport commsSupport = new AbstractBTBRDeviceSupport(LOG, 1024) {
        @Override
        public boolean useAutoConnect() {
            return mXiaomiSupport.useAutoConnect();
        }

        @Override
        public void onSocketRead(byte[] data) {
            XiaomiSppSupport.this.onSocketRead(data);
        }

        @Override
        protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
            XiaomiSppSupport.this.reset();

            // FIXME unsetDynamicState unsets the fw version, which causes problems..
            if (getDevice().getFirmwareVersion() == null) {
                getDevice().setFirmwareVersion(mXiaomiSupport.getCachedFirmwareVersion() != null ?
                        mXiaomiSupport.getCachedFirmwareVersion() :
                        "N/A");
            }

            builder.setDeviceState(GBDevice.State.INITIALIZING);
            builder.setDeviceState(GBDevice.State.AUTHENTICATING);
            builder.write(XiaomiSppPacketV1.newBuilder()
                    .channel(Channel.Version)
                    .needsResponse(true)
                    .opCode(OPCODE_READ)
                    .dataType(DATA_TYPE_PLAIN)
                    .frameSerial(0)
                    .build()
                    .encode(null, null));
            builder.run(() -> mVersionResponseTimeoutHandler.postDelayed(new VersionTimeoutRunnable(), 5000L));

            return builder;
        }

        @Override
        protected UUID getSupportedService() {
            return XiaomiUuids.UUID_SERVICE_SERIAL_PORT_PROFILE;
        }

        @Override
        public void dispose() {
            mXiaomiSupport.onDisconnect();
            super.dispose();
        }
    };

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final XiaomiSupport mXiaomiSupport;
    private final Map<Channel, XiaomiChannelHandler> mChannelHandlers = new HashMap<>();
    private final Handler mVersionResponseTimeoutHandler = new Handler(Looper.getMainLooper());
    private AbstractXiaomiSppProtocol mProtocol = new XiaomiSppProtocolV1(this);

    public XiaomiSppSupport(final XiaomiSupport xiaomiSupport) {
        this.mXiaomiSupport = xiaomiSupport;

        mChannelHandlers.put(Channel.Version, this::handleVersionPacket);
        mChannelHandlers.put(Channel.ProtobufCommand, this.mXiaomiSupport::handleCommandBytes);
        mChannelHandlers.put(Channel.Activity, this.mXiaomiSupport.getHealthService().getActivityFetcher()::addChunk);
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter adapter, Context context) {
        this.commsSupport.setContext(device, adapter, context);
    }

    @Override
    public boolean connect() {
        return commsSupport.connect();
    }

    @Override
    public void dispose() {
        commsSupport.dispose();
        mVersionResponseTimeoutHandler.removeCallbacksAndMessages(null);
    }

    protected XiaomiAuthService getAuthService() {
        return mXiaomiSupport.getAuthService();
    }

    @Override
    public void onUploadProgress(final int textRsrc, final int progressPercent, final boolean ongoing) {
        try {
            final TransactionBuilder builder = commsSupport.createTransactionBuilder("send data upload progress");
            builder.setProgress(
                    textRsrc,
                    ongoing,
                    progressPercent
            );
            builder.queue();
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    @Override
    public void runOnQueue(String taskName, Runnable runnable) {
        if (commsSupport == null) {
            LOG.error("commsSupport is null, unable to queue task");
            return;
        }

        final TransactionBuilder b = commsSupport.createTransactionBuilder("run task " + taskName + " on queue");
        b.run(runnable);
        b.queue();
    }

    private void skipBuffer(int newStart) {
        final byte[] bufferState = buffer.toByteArray();
        buffer.reset();

        if (newStart < 0) {
            newStart = bufferState.length;
        }

        if (newStart >= bufferState.length) {
            return;
        }

        buffer.write(bufferState, newStart, bufferState.length - newStart);
    }

    private void processBuffer() {
        boolean shouldProcess = true;
        while (shouldProcess) {
            final byte[] bufferState = buffer.toByteArray();
            final AbstractXiaomiSppProtocol.ParseResult parseResult = mProtocol.processPacket(bufferState);
            LOG.debug("processBuffer(): protocol.processPacket() returned status {}", parseResult.status);
            int skipBytes;

            switch (parseResult.status) {
                case Incomplete:
                    skipBytes = 0;
                    shouldProcess = false;
                    break;
                case Complete:
                    skipBytes = parseResult.packetSize;
                    break;
                case Invalid:
                    skipBytes = mProtocol.findNextPacketOffset(bufferState);
                    if (skipBytes < 0) {
                        skipBytes = bufferState.length;
                    }
                    break;
                default:
                    throw new IllegalStateException(String.format("Unhandled parse state %s", parseResult.status));
            }

            if (skipBytes > 0) {
                LOG.debug("processBuffer(): skipping {} bytes for state {}", skipBytes, parseResult.status);
                skipBuffer(skipBytes);
            }
        }
    }

    public void onSocketRead(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException ex) {
            LOG.error("Exception while writing buffer: ", ex);
        }

        processBuffer();
    }

    protected void onPacketReceived(final Channel channel, final byte[] payload) {
        final XiaomiChannelHandler handler = mChannelHandlers.get(channel);
        if (handler != null) {
            handler.handle(payload);
        } else {
            LOG.warn("Unhandled SppPacket on channel {}", channel);
        }
    }

    @Override
    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        try {
            final TransactionBuilder builder = this.commsSupport.createTransactionBuilder("send " + taskName);
            sendCommand(builder, command);
            builder.queue();
        } catch (final Exception ex) {
            LOG.error("Caught unexpected exception while sending command, device may not have been informed!", ex);
        }
    }

    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        LOG.debug("sendCommand(): encoded command for task '{}': {}", builder.getTaskName(), GB.hexdump(command.toByteArray()));
        if (command.getType() == XiaomiAuthService.COMMAND_TYPE) {
            builder.write(mProtocol.encodePacket(Channel.Authentication, command.toByteArray()));
        } else {
            builder.write(mProtocol.encodePacket(Channel.ProtobufCommand, command.toByteArray()));
        }
        // do not queue here, that's the job of the caller
    }

    @Override
    public void sendDataChunk(final String taskName, final byte[] chunk, @Nullable final XiaomiSendCallback callback) {
        LOG.debug("sendDataChunk(): encoded data chunk for task '{}': {}", taskName, GB.hexdump(chunk));
        this.commsSupport.createTransactionBuilder("send " + taskName)
            .write(mProtocol.encodePacket(Channel.Data, chunk))
            .queue();

        if (callback != null) {
            // callback puts a SetProgressAction onto the queue
            callback.onSend();
        }
    }

    private void handleVersionPacket(final byte[] payloadBytes) {
        // remove timeout actions from handler
        mVersionResponseTimeoutHandler.removeCallbacksAndMessages(null);

        if (payloadBytes != null && payloadBytes.length > 0) {
            LOG.debug("Received SPP protocol version: {}", GB.hexdump(payloadBytes));

            // show in details
            final GBDeviceEventUpdateDeviceInfo event = new GBDeviceEventUpdateDeviceInfo("SPP_PROTOCOL: ", GB.hexdump(payloadBytes));
            mXiaomiSupport.evaluateGBDeviceEvent(event);

            // TODO handle different protocol versions
            if (payloadBytes[0] >= 2) {
                LOG.info("handleVersionPacket(): detected protocol version higher than 2, switching protocol");
                mProtocol = new XiaomiSppProtocolV2(this);
            }
        }

        if (mProtocol.initializeSession()) {
            mXiaomiSupport.getAuthService().startEncryptedHandshake();
        }
    }

    public void reset() {
        buffer.reset();
        mVersionResponseTimeoutHandler.removeCallbacksAndMessages(null);
        // FIXME this is a bit ugly, reset the protocol back to V1 so we're able to parse the version packet
        mProtocol = new XiaomiSppProtocolV1(this);
    }

    class VersionTimeoutRunnable implements Runnable {
        @Override
        public void run() {
            LOG.warn("SPP protocol version request timed out");
            XiaomiSppSupport.this.handleVersionPacket(new byte[0]);
        }
    }
}
