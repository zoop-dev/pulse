package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiUtil;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.AccountRelated;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendCountryCodeRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendCountryCodeRequest.class);

    public SendCountryCodeRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = AccountRelated.id;
        this.commandId = AccountRelated.SendCountryCodeToDevice.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getDeviceState().supportsSendCountryCode() && supportProvider.getDeviceState().getSendCountryCodeEnabled(supportProvider.getDevice());
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            final String countryCode = supportProvider.getDeviceState().getCountryCode(supportProvider.getDevice());
            Byte siteId = null;
            if(supportProvider.getDeviceState().supportsSendSiteId()) {
                siteId = (byte) HuaweiUtil.getSiteIdByCountryCode(countryCode);
            }

            return new AccountRelated.SendCountryCodeToDevice.Request(
                    paramsProvider,
                    countryCode,
                    siteId)
                    .serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.debug("handle Send Country code to Device");
    }
}
