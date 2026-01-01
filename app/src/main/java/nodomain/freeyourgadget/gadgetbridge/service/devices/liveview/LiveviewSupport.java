/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Daniele
    Gobbetti, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupportV2;

public class LiveviewSupport extends AbstractSerialDeviceSupportV2<LiveviewProtocol> {
    private static final Logger LOG = LoggerFactory.getLogger(LiveviewSupport.class);

    private final ByteBuffer packetBuffer = ByteBuffer.allocate(8000).order(LiveviewConstants.BYTE_ORDER);

    public LiveviewSupport() {
        super(8000);
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }

    @Override
    protected LiveviewProtocol createDeviceProtocol() {
        return new LiveviewProtocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        packetBuffer.clear();
        builder.write(mDeviceProtocol.encodeSetTime());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public void onSocketRead(final byte[] data) {
        packetBuffer.put(data);
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            final int start = packetBuffer.position();
            packetBuffer.mark();

            if (packetBuffer.remaining() < 2) {
                // not enough bytes for min packet
                packetBuffer.reset();
                break;
            }

            packetBuffer.get(); // id

            final int headerLength = packetBuffer.get() & 0xff;
            if (packetBuffer.remaining() < headerLength) {
                // not enough bytes
                packetBuffer.reset();
                break;
            }

            final byte[] header = new byte[headerLength];
            packetBuffer.get(header);

            final int payloadSize = getLastInt(header);
            if (packetBuffer.remaining() < payloadSize) {
                // not enough bytes
                packetBuffer.reset();
                break;
            }

            final int end = packetBuffer.position() + payloadSize;

            final byte[] packet = new byte[end - start];
            packetBuffer.position(start);
            packetBuffer.get(packet);

            // Handle it upstream, to the protocol
            try {
                super.onSocketRead(packet);
            } catch (final Exception e) {
                LOG.error("Failed to handle command", e);
            }
        }

        packetBuffer.compact();
    }

    private int getLastInt(final byte[] array) {
        final ByteBuffer buffer = ByteBuffer.wrap(array, array.length - 4, 4);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        return buffer.getInt();
    }
}
