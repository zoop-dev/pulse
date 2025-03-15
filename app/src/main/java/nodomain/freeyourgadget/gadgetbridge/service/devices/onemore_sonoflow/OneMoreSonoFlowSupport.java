package nodomain.freeyourgadget.gadgetbridge.service.devices.onemore_sonoflow;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class OneMoreSonoFlowSupport extends AbstractHeadphoneDeviceSupport {
    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new OneMoreSonoFlowProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new OneMoreSonoFlowIOThread(
            getDevice(),
            getContext(),
            (OneMoreSonoFlowProtocol) getDeviceProtocol(),
            OneMoreSonoFlowSupport.this,
            getBluetoothAdapter()
        );
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
