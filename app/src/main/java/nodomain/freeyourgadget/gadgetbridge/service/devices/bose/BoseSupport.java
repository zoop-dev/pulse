/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.bose;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;

public class BoseSupport extends AbstractHeadphoneBTBRDeviceSupport {
    public static final Logger LOG = LoggerFactory.getLogger(BoseSupport.class);

    public BoseSupport() {
        super(LOG, 1024);
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final byte[] connectPayload = new byte[]{0x00, 0x01, 0x01, 0x00};
        final byte[] ncPayload = encodeNoiseCancelling();
        final byte[] batteryPayload = new byte[]{0x02, 0x02, 0x01, 0x00};
        final byte[] packet = new byte[connectPayload.length + ncPayload.length + batteryPayload.length];
        System.arraycopy(connectPayload, 0, packet, 0, connectPayload.length);
        System.arraycopy(ncPayload, 0, packet, connectPayload.length, ncPayload.length);
        System.arraycopy(batteryPayload, 0, packet, ncPayload.length + connectPayload.length, batteryPayload.length);

        getDevice().setFirmwareVersion("0");

        builder.setDeviceState(GBDevice.State.INITIALIZED);
        builder.write(packet);

        return builder;
    }

    @Override
    public void onSocketRead(final byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.remaining() > 0) {
            int first = buffer.get();
            int second = buffer.get();
            int third = buffer.get();
            int length = buffer.get();
            byte[] data = new byte[length];
            buffer.get(data);
            if (first == 0x02) {
                if (second == 0x02) {
                    if (third == 0x03) {
                        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                        batteryInfo.level = data[0];
                        batteryInfo.state = BatteryState.BATTERY_NORMAL;
                        evaluateGBDeviceEvent(batteryInfo);
                    }
                }
            }
        }
    }

    @Override
    public void onSendConfiguration(@NonNull final String config) {
        if (DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL.equals(config)) {
            final TransactionBuilder builder = createTransactionBuilder("set noise cancelling");
            builder.write(encodeNoiseCancelling());
            builder.queue();
        }
    }

    @NonNull
    private byte[] encodeNoiseCancelling() {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        if (getDevice().getType() == DeviceType.BOSE_NC700) {
            int level = prefs.getInt(DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL, 10);
            byte[] packet = new byte[]{0x01, 0x05, 0x02, 0x02, (byte) (10 - level), 0x01};
            // device resets to its default level when enabled flips 0 -> 1, so re-send it
            ByteBuffer repeated = ByteBuffer.allocate(packet.length * 3);
            repeated.put(packet).put(packet).put(packet);
            return repeated.array();
        }

        if (getDevice().getType() == DeviceType.BOSE_QC35) {
            int level = prefs.getInt(DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL, 0);
            if (level == 2) {
                level = 1;
            } else if (level == 1) {
                level = 3;
            }
            return new byte[]{0x01, 0x06, 0x02, 0x01, (byte) level};
        }

        throw new IllegalArgumentException("Unknown device type " + getDevice().getType().name());
    }
}
