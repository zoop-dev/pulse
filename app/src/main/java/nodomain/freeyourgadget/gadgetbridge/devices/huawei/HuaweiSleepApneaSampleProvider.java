package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepApneaSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepApneaSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiSleepApneaSampleProvider extends AbstractTimeSampleProvider<HuaweiSleepApneaSample> {
    public HuaweiSleepApneaSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiSleepApneaSample, ?> getSampleDao() {
        return getSession().getHuaweiSleepApneaSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiSleepApneaSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiSleepApneaSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiSleepApneaSample createSample() {
        return new HuaweiSleepApneaSample();
    }


    public long getLastFetchTimestamp() {
        QueryBuilder<HuaweiSleepApneaSample> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return 0;
        final Property deviceProperty = HuaweiSleepApneaSampleDao.Properties.DeviceId;
        final Property timestampProperty = HuaweiSleepApneaSampleDao.Properties.LastTimestamp;

        qb.where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty)
                .limit(1);

        List<HuaweiSleepApneaSample> samples = qb.build().list();
        if (samples.isEmpty())
            return 0;

        HuaweiSleepApneaSample sample = samples.get(0);
        return sample.getLastTimestamp();
    }
}
