package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiSleepStatsSampleProvider extends AbstractTimeSampleProvider<HuaweiSleepStatsSample>  {
    public HuaweiSleepStatsSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiSleepStatsSample, ?> getSampleDao() {
        return getSession().getHuaweiSleepStatsSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiSleepStatsSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiSleepStatsSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiSleepStatsSample createSample() {
        return new HuaweiSleepStatsSample();
    }

    public long getLastSleepFetchTimestamp() {
        QueryBuilder<HuaweiSleepStatsSample> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return 0;
        final Property deviceProperty = HuaweiSleepStatsSampleDao.Properties.DeviceId;
        final Property timestampProperty = HuaweiSleepStatsSampleDao.Properties.Timestamp;

        qb.where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty)
                .limit(1);

        List<HuaweiSleepStatsSample> samples = qb.build().list();
        if (samples.isEmpty())
            return 0;

        HuaweiSleepStatsSample sample = samples.get(0);
        return sample.getWakeupTime();
    }

    public List<HuaweiSleepStatsSample> getSleepSamples(final long timestampFrom, final long timestampTo) {
        final QueryBuilder<HuaweiSleepStatsSample> qb = getSampleDao().queryBuilder();
        final Property fallAsleepProperty = getTimestampSampleProperty();
        final Property wakeupProperty = HuaweiSleepStatsSampleDao.Properties.WakeupTime;
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()),
                qb.or(
                        qb.and(fallAsleepProperty.ge(timestampFrom), fallAsleepProperty.le(timestampTo)),
                        qb.and(wakeupProperty.ge(timestampFrom), wakeupProperty.le(timestampTo))
                )
        ).orderAsc(fallAsleepProperty);

        final List<HuaweiSleepStatsSample> samples = qb.build().list();
        detachFromSession();
        return samples;
    }
}
