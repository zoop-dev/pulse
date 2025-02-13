package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendReverseCapabilitiesRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendReverseCapabilitiesRequest.class);

    public SendReverseCapabilitiesRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.ReverseCapabilities.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsReverseCapabilities();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        DeviceConfig.ReverseCapabilities.Request reverseCapabilitiesRequest = new DeviceConfig.ReverseCapabilities.Request(paramsProvider);
        try {
            return reverseCapabilitiesRequest.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle ReverseCapabilities");
    }
}
