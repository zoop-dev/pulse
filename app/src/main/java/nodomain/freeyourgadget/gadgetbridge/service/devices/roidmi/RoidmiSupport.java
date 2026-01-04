/*  Copyright (C) 2018-2024 Arjan Schrijver, Daniel Dakhno, José Rebelo,
    Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi;

import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class RoidmiSupport extends AbstractHeadphoneSerialDeviceSupportV2<RoidmiProtocol> {
    private static final Logger LOG = LoggerFactory.getLogger(RoidmiSupport.class);

    private final Handler handler = new Handler();

    private final ByteBuffer packetBuffer = ByteBuffer.allocate(2048);

    private int infoRequestTries = 0;
    private final Runnable infosRunnable = () -> {
        infoRequestTries += 1;

        boolean infoMissing = false;

        final TransactionBuilder builder = createTransactionBuilder("request missing infos");

        if (getDevice().getExtraInfo("led_color") == null) {
            infoMissing = true;
            builder.write(mDeviceProtocol.encodeGetLedColor());
        }

        if (getDevice().getExtraInfo("fm_frequency") == null) {
            infoMissing = true;

            builder.write(mDeviceProtocol.encodeGetFmFrequency());
        }

        if (mDeviceProtocol.supportsBatteryVoltage()) {
            if (getDevice().getBatteryVoltage() == -1) {
                infoMissing = true;

                builder.write(mDeviceProtocol.encodeGetVoltage());
            }
        }

        if (infoMissing) {
            if (infoRequestTries < 6) {
                builder.queue();
                requestDeviceInfos(500 + infoRequestTries * 120);
            } else {
                LOG.error("Failed to get Roidmi infos after 6 tries");
            }
        } else {
            builder.setDeviceState(GBDevice.State.INITIALIZED);
            builder.queue();
        }
    };

    private void requestDeviceInfos(int delayMillis) {
        handler.postDelayed(infosRunnable, delayMillis);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        packetBuffer.clear();
        getDevice().setFirmwareVersion("N/A");
        builder.write(mDeviceProtocol.encodeGetLedColor());
        builder.write(mDeviceProtocol.encodeGetFmFrequency());
        if (mDeviceProtocol.supportsBatteryVoltage()) {
            builder.write(mDeviceProtocol.encodeGetVoltage());
        }
        requestDeviceInfos(1500);
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            handler.removeCallbacksAndMessages(null);
            super.dispose();
        }
    }

    @Override
    protected RoidmiProtocol createDeviceProtocol() {
        final DeviceType deviceType = getDevice().getType();

        switch (deviceType) {
            case ROIDMI:
                return new Roidmi1Protocol(getDevice());
            case ROIDMI3:
                return new Roidmi3Protocol(getDevice());
            default:
                LOG.error("Unsupported device type {} with key = {}", deviceType, deviceType.name());
        }

        return null;
    }

    @Override
    public void onSocketRead(final byte[] data) {
        packetBuffer.put(data);
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            final int start = packetBuffer.position();
            packetBuffer.mark();

            if (packetBuffer.remaining() < mDeviceProtocol.minPacketLength()) {
                // not enough bytes for min packet
                packetBuffer.reset();
                break;
            }

            final byte[] expectedHeader = mDeviceProtocol.packetHeader();
            if (expectedHeader.length > 0) {
                final byte[] header = new byte[expectedHeader.length];
                packetBuffer.get(header);
                if (!ArrayUtils.equals(header, expectedHeader, 0)) {
                    LOG.warn("Unexpected header {}", GB.hexdump(header));
                    // Skip 1 byte
                    packetBuffer.reset();
                    packetBuffer.position(packetBuffer.position() + 1);
                    continue;
                }
            }

            final int payloadLength = packetBuffer.get() & 0xff;

            final byte[] expectedTrailer = mDeviceProtocol.packetTrailer();
            if (packetBuffer.remaining() < payloadLength + 1 + expectedTrailer.length) {
                // not enough bytes
                packetBuffer.reset();
                break;
            }

            // header + payload length + payload + checksum + trailer
            final byte[] packet = new byte[expectedHeader.length + 1 + payloadLength + 1 + expectedTrailer.length];
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
}
