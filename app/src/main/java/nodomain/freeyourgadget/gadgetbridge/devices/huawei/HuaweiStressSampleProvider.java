package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiStressSampleProvider extends AbstractTimeSampleProvider<HuaweiStressSample> {
    public HuaweiStressSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiStressSample, ?> getSampleDao() {
        return getSession().getHuaweiStressSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiStressSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiStressSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiStressSample createSample() {
        return new HuaweiStressSample();
    }
}
