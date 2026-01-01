package nodomain.freeyourgadget.gadgetbridge.service.devices.onemore_sonoflow;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class OneMoreSonoFlowSupport extends AbstractHeadphoneSerialDeviceSupportV2<OneMoreSonoFlowProtocol> {
    @Override
    protected OneMoreSonoFlowProtocol createDeviceProtocol() {
        return new OneMoreSonoFlowProtocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        // get some device information
        // TODO: we might not receive some responses, it might be worth requesting them again if that's a significant issue
        //  https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/4637#issuecomment-3035556
        builder.write(OneMorePacket.createGetDeviceInfoPacket());
        builder.write(OneMorePacket.createGetNoiseControlModePacket());
        builder.write(OneMorePacket.createGetLdacModePacket());
        builder.write(OneMorePacket.createGetDualDeviceModePacket());

        builder.setDeviceState(GBDevice.State.INITIALIZED);

        return builder;
    }
}
