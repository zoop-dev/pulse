package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendSMSReplyAck extends Request {
    private final byte resultCode;

    public SendSMSReplyAck(HuaweiSupportProvider support,
                               byte resultCode) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationReply.id;
        this.resultCode = resultCode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Notifications.NotificationReply.ReplyAck(this.paramsProvider, this.resultCode).serialize();
        } catch(HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e.getMessage());
        }
    }
}
