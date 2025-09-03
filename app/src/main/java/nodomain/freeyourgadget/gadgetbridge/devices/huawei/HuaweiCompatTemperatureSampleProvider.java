package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictData;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValues;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValuesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class HuaweiCompatTemperatureSampleProvider implements TimeSampleProvider<HuaweiTemperatureSample> {
    protected static final Logger LOG = LoggerFactory.getLogger(HuaweiCompatTemperatureSampleProvider.class);

    private final HuaweiTemperatureSampleProvider sp;

    private final GBDevice device;
    private final DaoSession session;

    public HuaweiCompatTemperatureSampleProvider(GBDevice device, DaoSession session) {
        this.device = device;
        this.session = session;
        sp = new HuaweiTemperatureSampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<HuaweiTemperatureSample> getAllSamples(long timestampFrom, long timestampTo) {

        TemperatureSample newFirst = sp.getFirstSample();
        if (newFirst != null && newFirst.getTimestamp() < timestampFrom) {
            return sp.getAllSamples(timestampFrom, timestampTo);
        }

        List<HuaweiTemperatureSample> ret = sp.getAllSamples(timestampFrom, timestampTo);

        long oldTo = timestampTo;
        if (!ret.isEmpty()) {
            oldTo = ret.get(0).getTimestamp();
        }

        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return ret;

        QueryBuilder<HuaweiDictData> qb = this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS))
                .where(HuaweiDictDataDao.Properties.StartTimestamp.between(timestampFrom, oldTo));
        final List<HuaweiDictData> dictData = qb.build().list();

        if (dictData.isEmpty())
            return ret;

        List<Long> ids = dictData.stream().map(HuaweiDictData::getDictId).collect(Collectors.toList());

        QueryBuilder<HuaweiDictDataValues> qbv = this.session.getHuaweiDictDataValuesDao().queryBuilder();

        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.in(ids));

        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return ret;

        int idx = 0;
        for (HuaweiDictDataValues vl : valuesData) {
            double skinTemperature = HuaweiUtil.convBytes2Double(vl.getValue());
            if (skinTemperature >= 20 && skinTemperature <= 42) {
                HuaweiTemperatureSample sample = new HuaweiTemperatureSample();
                sample.setTimestamp(vl.getHuaweiDictData().getStartTimestamp());
                sample.setTemperature((float) skinTemperature);
                sample.setTemperatureType(0);
                ret.add(idx++, sample);
            }
        }

        return ret;
    }

    @Override
    public void addSample(HuaweiTemperatureSample timeSample) {
        throw new UnsupportedOperationException("read-only sample provider");

    }

    @Override
    public void addSamples(List<HuaweiTemperatureSample> timeSamples) {
        throw new UnsupportedOperationException("read-only sample provider");

    }

    @Override
    public HuaweiTemperatureSample createSample() {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Nullable
    @Override
    public HuaweiTemperatureSample getLatestSample() {

        HuaweiTemperatureSample newSample = sp.getLatestSample();
        if (newSample != null)
            return newSample;

        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb = this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderDesc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return null;


        QueryBuilder<HuaweiDictDataValues> qbv = this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return null;

        HuaweiTemperatureSample sample = new HuaweiTemperatureSample();
        sample.setTimestamp(valuesData.get(0).getHuaweiDictData().getStartTimestamp());
        sample.setTemperature((float) HuaweiUtil.convBytes2Double(valuesData.get(0).getValue()));
        sample.setTemperatureType(0);
        return sample;
    }

    @Nullable
    @Override
    public HuaweiTemperatureSample getLatestSample(final long until) {

        HuaweiTemperatureSample newSample = sp.getLatestSample(until);
        if (newSample != null)
            return newSample;

        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb = this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.StartTimestamp.le(until))
                .where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderDesc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return null;


        QueryBuilder<HuaweiDictDataValues> qbv = this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return null;

        HuaweiTemperatureSample sample = new HuaweiTemperatureSample();
        sample.setTimestamp(valuesData.get(0).getHuaweiDictData().getStartTimestamp());
        sample.setTemperature((float) HuaweiUtil.convBytes2Double(valuesData.get(0).getValue()));
        sample.setTemperatureType(0);
        return sample;
    }

    @Nullable
    @Override
    public HuaweiTemperatureSample getFirstSample() {
        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb = this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderAsc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return sp.getFirstSample();

        QueryBuilder<HuaweiDictDataValues> qbv = this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return sp.getFirstSample();

        HuaweiTemperatureSample sample = new HuaweiTemperatureSample();
        sample.setTimestamp(valuesData.get(0).getHuaweiDictData().getStartTimestamp());
        sample.setTemperature((float) HuaweiUtil.convBytes2Double(valuesData.get(0).getValue()));
        sample.setTemperatureType(0);
        return sample;
    }

    public static boolean migrateOldData() {
        long count;
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            QueryBuilder<HuaweiDictDataValues> qbv1 = daoSession.getHuaweiDictDataValuesDao().queryBuilder();
            count = qbv1.count();
        } catch (Exception e) {
            LOG.error("Error calculate data to migrate", e);
            return false;
        }

        int limit = 10000;
        int offset = 0;
        while (true) {
            try (DBHandler db = GBApplication.acquireDB()) {
                DaoSession daoSession = db.getDaoSession();

                QueryBuilder<HuaweiDictDataValues> qbv = daoSession.getHuaweiDictDataValuesDao().queryBuilder();

                qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).limit(limit).offset(offset);

                final List<HuaweiDictDataValues> valuesData = qbv.build().list();

                if (valuesData.isEmpty())
                    break;

                List<HuaweiTemperatureSample> res = new ArrayList<>();
                for (HuaweiDictDataValues vl : valuesData) {
                    double skinTemperature = HuaweiUtil.convBytes2Double(vl.getValue());
                    if (skinTemperature >= 20 && skinTemperature <= 42) {
                        res.add(new HuaweiTemperatureSample(vl.getHuaweiDictData().getStartTimestamp(), vl.getHuaweiDictData().getDeviceId(), vl.getHuaweiDictData().getUserId(), Math.max(vl.getHuaweiDictData().getModifyTimestamp(), vl.getHuaweiDictData().getEndTimestamp()), (float) skinTemperature, 0));
                    }
                }
                daoSession.getHuaweiTemperatureSampleDao().insertInTx(res);
                offset += limit;
                LOG.info("Migrating: {}/{}", offset, count);
            } catch (Exception e) {
                LOG.error("Error migrate data", e);
                return false;
            }
        }

        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();

            final DeleteQuery<HuaweiDictDataValues> tableValuesDeleteQuery = daoSession.getHuaweiDictDataValuesDao().queryBuilder()
                    .where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE))
                    .buildDelete();
            tableValuesDeleteQuery.executeDeleteWithoutDetachingEntities();

            final DeleteQuery<HuaweiDictData> tableDataDeleteQuery = daoSession.getHuaweiDictDataDao().queryBuilder()
                    .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS))
                    .buildDelete();
            tableDataDeleteQuery.executeDeleteWithoutDetachingEntities();

        } catch (Exception e) {
            LOG.error("Error delete data", e);
            return false;
        }
        return true;
    }
}
