package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class EarFunDeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<EarFunProtocol> {
    public static final UUID GAIA_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID GAIA_UUID = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");

    public EarFunDeviceSupport() {
        addSupportedService(GAIA_SPP_UUID);
    }

    @Override
    protected EarFunProtocol createDeviceProtocol() {
        return new EarFunProtocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        builder.write(mDeviceProtocol.encodeFirmwareVersionReq());
        builder.write(mDeviceProtocol.encodeSettingsReq());
        return builder;
    }
}
