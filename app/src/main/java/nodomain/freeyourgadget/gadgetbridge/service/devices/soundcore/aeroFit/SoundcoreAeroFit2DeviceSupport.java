package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.aeroFit;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreAeroFit2DeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreAeroFitProtocol> {
    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3874");

    public SoundcoreAeroFit2DeviceSupport() {
        addSupportedService(UUID_DEVICE_CTRL);
    }

    @Override
    protected SoundcoreAeroFitProtocol createDeviceProtocol() {
        return new SoundcoreAeroFitProtocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.write(mDeviceProtocol.encodeDeviceInfoRequest());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }
}
