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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qc35;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class QC35BaseSupport extends AbstractHeadphoneSerialDeviceSupportV2<QC35Protocol> {
    public QC35BaseSupport() {
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    @Override
    protected QC35Protocol createDeviceProtocol() {
        return new QC35Protocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {

        byte[] connectPayload = new byte[]{0x00, 0x01, 0x01, 0x00};
        byte[] ncPayload = mDeviceProtocol.encodeSendConfiguration(DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL);
        byte[] batteryPayload = new byte[]{0x02, 0x02, 0x01, 0x00};
        byte[] packet = new byte[connectPayload.length + ncPayload.length + batteryPayload.length];
        System.arraycopy(connectPayload, 0, packet, 0, connectPayload.length);
        System.arraycopy(ncPayload, 0, packet, connectPayload.length, ncPayload.length);
        System.arraycopy(batteryPayload, 0, packet, ncPayload.length + connectPayload.length, batteryPayload.length);

        getDevice().setFirmwareVersion("0");

        builder.setDeviceState(GBDevice.State.INITIALIZED);
        builder.write(packet);

        return builder;
    }
}
