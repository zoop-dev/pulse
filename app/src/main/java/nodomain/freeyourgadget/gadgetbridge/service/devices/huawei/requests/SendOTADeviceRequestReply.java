package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTADeviceRequestReply extends Request {
    private final int code;

    public SendOTADeviceRequestReply(HuaweiSupportProvider support, int code) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.DeviceRequest.id;
        this.code = code;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.DeviceRequest.Request(paramsProvider, code).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
