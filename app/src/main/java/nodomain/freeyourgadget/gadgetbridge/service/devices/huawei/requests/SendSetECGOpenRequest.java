package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.ECG;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendSetECGOpenRequest extends Request {

    public SendSetECGOpenRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = ECG.id;
        this.commandId = ECG.SetECGOpen.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getDeviceState().supportsECGOpen();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {

        try {
            return new ECG.SetECGOpen.Request(paramsProvider, (byte) 0).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}