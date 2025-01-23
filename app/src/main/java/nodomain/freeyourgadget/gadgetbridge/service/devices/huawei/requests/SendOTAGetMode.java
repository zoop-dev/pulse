package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTAGetMode  extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendOTAGetMode.class);

    public SendOTAGetMode(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.GetMode.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.GetMode.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle SendOTAGetMode");
        if (receivedPacket instanceof OTA.GetMode.Response) {
            supportProvider.getHuaweiOTAManager().handleGetModeResponse(((OTA.GetMode.Response) receivedPacket).mode);
        } else {
            LOG.error("SendOTAGetMode response invalid type");
        }
    }
}
