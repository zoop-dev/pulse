package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.sport_x20;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreSportX20DeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreSportX20Protocol> {
    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3968");

    public SoundcoreSportX20DeviceSupport() {
        addSupportedService(UUID_DEVICE_CTRL);
    }

    @Override
    protected SoundcoreSportX20Protocol createDeviceProtocol() {
        return new SoundcoreSportX20Protocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.write(mDeviceProtocol.encodeDeviceInfoRequest());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }
}
