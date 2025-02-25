package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetSpO2LowAlert extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetSpO2LowAlert.class);

    public SetSpO2LowAlert(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.LowSpoAlert.id;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        int lowSpOAlert = Integer.parseInt(GBApplication
                .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
                .getString(HuaweiConstants.PREF_HUAWEI_SPO_LOW_ALERT, "0"));

        LOG.info("Attempting to sel high spo alert: {}", lowSpOAlert);
        try {
            return new FitnessData.LowSpoAlert.Request(paramsProvider, lowSpOAlert > 0, (byte) lowSpOAlert).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}