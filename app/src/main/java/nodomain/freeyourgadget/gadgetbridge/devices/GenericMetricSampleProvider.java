/*  Copyright (C) 2026 Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericMetricSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericMetricSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.MetricSample;

public class GenericMetricSampleProvider extends AbstractTimeSampleProvider<GenericMetricSample> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericMetricSampleProvider.class);

    public GenericMetricSampleProvider(@NonNull final GBDevice device, @NonNull final DaoSession session) {
        super(device, session);
    }

    @NonNull
    public static List<? extends MetricSample> getMetricSamples(final @NonNull DBHandler db, final @NonNull GBDevice device, @NonNull MetricSample.Metric metric, long tsFromMillis, long tsToMillis) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final GenericMetricSampleProvider sampleProvider = coordinator.getMetricsSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement MetricSample", device);
            return new ArrayList<>();
        }
        final List<? extends MetricSample> samples = sampleProvider.getAllSamples(metric, tsFromMillis, tsToMillis);
        return samples;
    }

    @Nullable
    public static MetricSample getLatestMetricSampleBefore(@NonNull final DBHandler db, @NonNull final GBDevice device, @NonNull MetricSample.Metric metric, long tsToMillis) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final GenericMetricSampleProvider sampleProvider = coordinator.getMetricsSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement MetricSample", device);
            return null;
        }
        final MetricSample sample = sampleProvider.getLatestSampleBefore(metric, tsToMillis);
        return sample;
    }

    @Nullable
    public static MetricSample getLatestMetricSample(@NonNull final DBHandler db, @NonNull final GBDevice device, @NonNull MetricSample.Metric metric, @NotNull Calendar day) {
        long tsStart = day.getTimeInMillis();
        long tsEnd = tsStart + 24 * 60 * 60 * 1000L;
        final MetricSample sample = getLatestMetricSample(db, device, metric, tsStart, tsEnd);
        return sample;
    }

    @Nullable
    public static MetricSample getLatestMetricSample(@NonNull final DBHandler db, @NonNull final GBDevice device, @NonNull MetricSample.Metric metric, long tsFromMillis, long tsToMillis) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final GenericMetricSampleProvider sampleProvider = coordinator.getMetricsSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement MetricSample", device);
            return null;
        }
        final MetricSample sample = sampleProvider.getLatestSample(metric, tsFromMillis, tsToMillis);
        return sample;
    }

    @Nullable
    public static MetricSample getLatestMetricSample(@NonNull final DBHandler db, @NonNull final GBDevice device, @NonNull MetricSample.Metric metric) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final GenericMetricSampleProvider sampleProvider = coordinator.getMetricsSampleProvider(device, db.getDaoSession());
        if (sampleProvider == null) {
            LOG.warn("Device {} does not implement MetricSample", device);
            return null;
        }
        final MetricSample sample = sampleProvider.getLatestSample(metric);
        return sample;
    }

    public static boolean supportsMetrics(@NonNull GBDevice device, @NonNull MetricSample.Metric metric) {
        // check the database to see what if this metric has actually been recorded.
        HashSet<MetricSample.Metric> recorded = new HashSet<>();
        try (DBHandler handler = GBApplication.acquireDbReadOnly()) {
            SQLiteDatabase db = handler.getDatabase();
            String sql = String.format(
                    "SELECT %1$s.%2$s FROM %1$s INNER JOIN %4$s ON %1$s.%3$s = %4$s.%5$s WHERE %4$s.%6$s = ? AND %1$s.%2$s = ? LIMIT 1",
                    GenericMetricSampleDao.TABLENAME,
                    GenericMetricSampleDao.Properties.MetricType.columnName,
                    GenericMetricSampleDao.Properties.DeviceId.columnName,
                    DeviceDao.TABLENAME,
                    DeviceDao.Properties.Id.columnName,
                    DeviceDao.Properties.Identifier.columnName
            );

            try (final Cursor c = db.rawQuery(sql, new String[]{device.getAddress(), Integer.toString(metric.dbId)})) {
                while (c.moveToNext()) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.warn("failed to identify recorded metrics ", e);
        }
        return false;
    }

    public static Set<MetricSample.Metric> getSupportedMetrics(@NonNull GBDevice device) {
        // check the database to see what metrics have actually been recorded
        // instead of guessing firmware version and usage dependent capabilities
        HashSet<MetricSample.Metric> recorded = new HashSet<>();
        try (DBHandler handler = GBApplication.acquireDbReadOnly()) {
            SQLiteDatabase db = handler.getDatabase();
            String sql = String.format(
                    "SELECT DISTINCT %1$s.%2$s FROM %1$s WHERE %1$s.%3$s = (SELECT %4$s.%5$s FROM %4$s WHERE %4$s.%6$s = ? LIMIT 1) ORDER BY %1$s.%2$s",
                    GenericMetricSampleDao.TABLENAME,
                    GenericMetricSampleDao.Properties.MetricType.columnName,
                    GenericMetricSampleDao.Properties.DeviceId.columnName,
                    DeviceDao.TABLENAME,
                    DeviceDao.Properties.Id.columnName,
                    DeviceDao.Properties.Identifier.columnName
            );

            try (final Cursor c = db.rawQuery(sql, new String[]{device.getAddress()})) {
                while (c.moveToNext()) {
                    final int dbId = c.getInt(0);
                    MetricSample.Metric metric = MetricSample.Metric.fromDbId(dbId);
                    if (metric == null) {
                        LOG.warn("unknown MetricSample.Metric id: {}", dbId);
                    } else {
                        recorded.add(metric);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("failed to identify recorded metrics ", e);
        }
        return recorded;
    }

    public static Set<MetricSample.Metric> supportsMetrics(@NonNull GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        Set<MetricSample.Metric> supported = coordinator.supportsMetrics(device);
        return supported;
    }


    @NonNull
    @Override
    public AbstractDao<GenericMetricSample, ?> getSampleDao() {
        return getSession().getGenericMetricSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GenericMetricSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GenericMetricSampleDao.Properties.DeviceId;
    }

    @NonNull
    @Override
    public GenericMetricSample createSample() {
        return new GenericMetricSample();
    }

    @NonNull
    public List<GenericMetricSample> getAllSamples(@NonNull final MetricSample.Metric metric, final long timestampFrom, final long timestampTo) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }

        final AbstractDao<GenericMetricSample, ?> dao = getSampleDao();
        final QueryBuilder<GenericMetricSample> qb = dao.queryBuilder();
        final List<GenericMetricSample> samples = qb
                .where(
                        GenericMetricSampleDao.Properties.DeviceId.eq(dbDevice.getId()),
                        GenericMetricSampleDao.Properties.Timestamp.between(timestampFrom, timestampTo),
                        GenericMetricSampleDao.Properties.MetricType.eq(metric.dbId)
                )
                .orderAsc(GenericMetricSampleDao.Properties.Timestamp)
                .list();
        return samples;
    }

    @Nullable
    public MetricSample getLatestSampleBefore(@NonNull final MetricSample.Metric metric, final long until) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final AbstractDao<GenericMetricSample, ?> dao = getSampleDao();
        final QueryBuilder<GenericMetricSample> qb = dao.queryBuilder();
        final List<GenericMetricSample> samples = qb
                .where(
                        GenericMetricSampleDao.Properties.DeviceId.eq(dbDevice.getId()),
                        GenericMetricSampleDao.Properties.Timestamp.le(until),
                        GenericMetricSampleDao.Properties.MetricType.eq(metric.dbId)
                )
                .orderDesc(GenericMetricSampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        if (samples.isEmpty()) {
            return null;
        }
        final GenericMetricSample sample = samples.get(0);
        return sample;
    }

    @Nullable
    public GenericMetricSample getLatestSample(@NonNull final MetricSample.Metric metric) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final AbstractDao<GenericMetricSample, ?> dao = getSampleDao();
        final QueryBuilder<GenericMetricSample> qb = dao.queryBuilder();
        final List<GenericMetricSample> samples = qb
                .where(
                        GenericMetricSampleDao.Properties.DeviceId.eq(dbDevice.getId()),
                        GenericMetricSampleDao.Properties.MetricType.eq(metric.dbId)
                )
                .orderDesc(GenericMetricSampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        if (samples.isEmpty()) {
            return null;
        }
        final GenericMetricSample sample = samples.get(0);
        return sample;
    }

    @Nullable
    public GenericMetricSample getLatestSample(@NonNull final MetricSample.Metric metric, long tsFromMillis, long tsToMillis) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final AbstractDao<GenericMetricSample, ?> dao = getSampleDao();
        final QueryBuilder<GenericMetricSample> qb = dao.queryBuilder();
        final List<GenericMetricSample> samples = qb
                .where(
                        GenericMetricSampleDao.Properties.DeviceId.eq(dbDevice.getId()),
                        GenericMetricSampleDao.Properties.MetricType.eq(metric.dbId),
                        GenericMetricSampleDao.Properties.Timestamp.between(tsFromMillis, tsToMillis)
                )
                .orderDesc(GenericMetricSampleDao.Properties.Timestamp)
                .limit(1)
                .list();
        if (samples.isEmpty()) {
            return null;
        }
        final GenericMetricSample sample = samples.get(0);
        return sample;
    }
}

