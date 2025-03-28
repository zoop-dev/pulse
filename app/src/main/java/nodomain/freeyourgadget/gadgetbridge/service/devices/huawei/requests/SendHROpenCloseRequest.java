package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.HrRriTest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendHROpenCloseRequest  extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendHROpenCloseRequest.class);

    private final byte type;

    public SendHROpenCloseRequest(HuaweiSupportProvider support, byte type) {
        super(support);
        this.serviceId = HrRriTest.id;
        this.commandId = HrRriTest.OpenOrClose.id;
        this.type = type;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new HrRriTest.OpenOrClose.Request(paramsProvider, this.type).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}
