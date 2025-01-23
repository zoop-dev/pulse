package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTAStartQuery extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendOTAStartQuery.class);

    private final String firmwareVersion;
    private final short fileId;
    private final byte operation;
    private final boolean add;

    public SendOTAStartQuery(HuaweiSupportProvider support, String firmwareVersion, short fileId, byte operation, boolean add) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.StartQuery.id;
        this.firmwareVersion = firmwareVersion;
        this.fileId = fileId;
        this.operation = operation;
        this.add = add;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.StartQuery.Request(paramsProvider, this.firmwareVersion, this.fileId, this.operation, this.add).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle SendOTAStartQuery");
        if (receivedPacket instanceof OTA.StartQuery.Response) {
            supportProvider.getHuaweiOTAManager().handleStartQueryResponse(((OTA.StartQuery.Response) receivedPacket).respCode, ((OTA.StartQuery.Response) receivedPacket).batteryThreshold);
        } else {
            LOG.error("SendOTAStartQuery response invalid type");
        }
    }
}
