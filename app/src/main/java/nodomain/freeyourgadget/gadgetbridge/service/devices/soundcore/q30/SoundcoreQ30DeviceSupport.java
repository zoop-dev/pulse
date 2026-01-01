package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.q30;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreQ30DeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreQ30Protocol> {
    public SoundcoreQ30DeviceSupport() {
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    @Override
    protected SoundcoreQ30Protocol createDeviceProtocol() {
        return new SoundcoreQ30Protocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.write(mDeviceProtocol.encodeDeviceInfoRequest());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }
}
