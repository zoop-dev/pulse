package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetOTAChangeLog extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetOTAChangeLog.class);

    public GetOTAChangeLog(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.GetChangeLog.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsOTAChangelog() &&
                supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion() != null;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // TODO: get proper language.
            return new OTA.GetChangeLog.Request(paramsProvider, supportProvider.getHuaweiCoordinator().getOtaSoftwareVersion(), "en").serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle GetOTAChangeLog");
        if (!(receivedPacket instanceof OTA.GetChangeLog.Response))
            throw new ResponseTypeMismatchException(receivedPacket, OTA.GetChangeLog.Response.class);

        OTA.GetChangeLog.Response resp = (OTA.GetChangeLog.Response) receivedPacket;

        SetOTAChangeLog setOTAChangeLog = new SetOTAChangeLog(supportProvider);
        setOTAChangeLog.nextRequest(this.nextRequest);
        nextRequest(setOTAChangeLog);
    }
}
