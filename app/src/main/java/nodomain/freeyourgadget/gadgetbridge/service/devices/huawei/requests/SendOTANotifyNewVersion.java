package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTANotifyNewVersion extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendOTANotifyNewVersion.class);

    private final String newVersion;
    private final long fileSize;
    private final byte unkn1;
    private final byte unkn2;

    public SendOTANotifyNewVersion(HuaweiSupportProvider support, String newVersion, long fileSize, byte unkn1, byte unkn2) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.NotifyNewVersion.id;
        this.newVersion = newVersion;
        this.fileSize = fileSize;
        this.unkn1 = unkn1;
        this.unkn2 = unkn2;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.NotifyNewVersion.Request(paramsProvider, this.newVersion, this.fileSize, this.unkn1, this.unkn2).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle SendOTANotifyNewVersion");
        if (receivedPacket instanceof OTA.NotifyNewVersion.Response) {
            supportProvider.getHuaweiOTAManager().handleNotifyNewVersionResponse(((OTA.NotifyNewVersion.Response) receivedPacket).respCode);
        } else {
            LOG.error("SendOTANotifyNewVersion response invalid type");
        }
    }
}
