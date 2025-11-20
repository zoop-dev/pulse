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
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEmotionsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEmotionsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiEmotionsSampleProvider extends AbstractTimeSampleProvider<HuaweiEmotionsSample> {
    public HuaweiEmotionsSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiEmotionsSample, ?> getSampleDao() {
        return getSession().getHuaweiEmotionsSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiEmotionsSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiEmotionsSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiEmotionsSample createSample() {
        return new HuaweiEmotionsSample();
    }


    public long getLastFetchTimestamp() {
        QueryBuilder<HuaweiEmotionsSample> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return 0;
        final Property deviceProperty = HuaweiEmotionsSampleDao.Properties.DeviceId;
        final Property timestampProperty = HuaweiEmotionsSampleDao.Properties.LastTimestamp;

        qb.where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty)
                .limit(1);

        List<HuaweiEmotionsSample> samples = qb.build().list();
        if (samples.isEmpty())
            return 0;

        HuaweiEmotionsSample sample = samples.get(0);
        return sample.getLastTimestamp();
    }

}
