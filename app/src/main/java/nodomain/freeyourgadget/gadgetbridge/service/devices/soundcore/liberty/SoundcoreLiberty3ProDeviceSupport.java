package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreLiberty3ProDeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreLibertyProtocol> {
    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3952");

    public SoundcoreLiberty3ProDeviceSupport() {
        addSupportedService(UUID_DEVICE_CTRL);
    }

    @Override
    protected SoundcoreLibertyProtocol createDeviceProtocol() {
        return new SoundcoreLibertyProtocol(getDevice());
    }

    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.write(mDeviceProtocol.encodeDeviceInfoRequest());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }
}
