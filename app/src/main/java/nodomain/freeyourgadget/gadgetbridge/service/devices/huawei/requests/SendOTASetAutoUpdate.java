package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTASetAutoUpdate extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendOTASetAutoUpdate.class);

    public SendOTASetAutoUpdate(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.SetAutoUpdate.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsOTAAutoUpdate();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // NOTE: always set autoupdate to false for now
            return new OTA.SetAutoUpdate.Request(paramsProvider, false).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle SendOTASetAutoUpdate");
    }
}
