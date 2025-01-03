package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendPermissionCheckResponse extends Request {
    short permission;
    short status;


    public SendPermissionCheckResponse(HuaweiSupportProvider support, short permission, short status) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.PermissionCheck.id;
        this.permission = permission;
        this.status = status;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new DeviceConfig.PermissionCheck.PermissionCheckResponse(this.paramsProvider,  this.permission, this.status).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}
