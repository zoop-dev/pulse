package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetHeartRateLowAlert extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetHeartRateLowAlert.class);

    public SetHeartRateLowAlert(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.LowHeartRateAlert.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        int lowHeartRateAlert = Integer.parseInt(GBApplication
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getString(HuaweiConstants.PREF_HUAWEI_HEART_RATE_LOW_ALERT, "0"));

        LOG.info("Attempting to set low heart rate alert: {}", lowHeartRateAlert);
        try {
            return new FitnessData.LowHeartRateAlert.Request(paramsProvider, lowHeartRateAlert > 0, (byte) lowHeartRateAlert).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
