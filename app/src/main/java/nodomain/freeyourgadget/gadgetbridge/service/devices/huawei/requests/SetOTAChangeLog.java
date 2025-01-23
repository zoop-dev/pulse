package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetOTAChangeLog extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetOTAChangeLog.class);

    public SetOTAChangeLog(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.SetChangeLog.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsOTAChangelog() &&
                supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion() != null;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // TODO: currently send empty. Send real changelog. Research required.
            return new OTA.SetChangeLog.Request(paramsProvider, supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion(), (byte) 0).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle SetOTAChangeLog");
    }
}
