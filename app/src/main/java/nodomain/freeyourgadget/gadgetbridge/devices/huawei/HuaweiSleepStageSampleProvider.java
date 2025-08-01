package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiSleepStageSampleProvider extends AbstractTimeSampleProvider<HuaweiSleepStageSample> {
    public HuaweiSleepStageSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiSleepStageSample, ?> getSampleDao() {
        return getSession().getHuaweiSleepStageSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiSleepStageSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiSleepStageSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiSleepStageSample createSample() {
        return new HuaweiSleepStageSample();
    }
}
