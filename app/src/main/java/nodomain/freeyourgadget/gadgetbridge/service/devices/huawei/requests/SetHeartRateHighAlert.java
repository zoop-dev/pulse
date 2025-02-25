package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetHeartRateHighAlert extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetHeartRateHighAlert.class);

    public SetHeartRateHighAlert(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.HighHeartRateAlert.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        int highHeartRateAlert = Integer.parseInt(GBApplication
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getString(HuaweiConstants.PREF_HUAWEI_HEART_RATE_HIGH_ALERT, "0"));

        LOG.info("Attempting to sel high heart rate alert: {}", highHeartRateAlert);
        try {
            return new FitnessData.HighHeartRateAlert.Request(paramsProvider, highHeartRateAlert > 0, (byte) highHeartRateAlert).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}