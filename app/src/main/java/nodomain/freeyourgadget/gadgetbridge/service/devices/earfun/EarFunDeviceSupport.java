package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class EarFunDeviceSupport extends AbstractHeadphoneDeviceSupport {
    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        super.onTestNewFunction();
    }

    @Override
    public synchronized EarFunIOThread getDeviceIOThread() {
        return (EarFunIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new EarFunProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new EarFunIOThread(getDevice(), getContext(), (EarFunProtocol) getDeviceProtocol(),
                EarFunDeviceSupport.this, getBluetoothAdapter());
    }
}
