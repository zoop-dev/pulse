package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Breath;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendSleepBreathRequest extends Request {

    public SendSleepBreathRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Breath.id;
        this.commandId = Breath.SleepBreath.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsSleepBreath();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        boolean sleepBreathSwitch = GBApplication
                .getDeviceSpecificSharedPrefs(this.getDevice().getAddress())
                .getBoolean(HuaweiConstants.PREF_HUAWEI_SLEEP_BREATH, false);
        byte type = (byte) (sleepBreathSwitch?2:0);
        try {
            return new Breath.SleepBreath.Request(paramsProvider, type).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}