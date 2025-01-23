package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTADataParamsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendOTADataParamsRequest.class);

    public SendOTADataParamsRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.DataParams.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.DataParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle SendOTADataParamsRequest");
        if (receivedPacket instanceof OTA.DataParams.Response) {
            supportProvider.getHuaweiOTAManager().handleDataParamsResponse(((OTA.DataParams.Response) receivedPacket).info);
        } else {
            LOG.error("SendOTADataParamsRequest response invalid type");
        }
    }
}
