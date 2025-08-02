/*  Copyright (C) 2024 José Rebelo

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.WorkoutLoadSample;

/**
 * An implementation of {@link WorkoutLoadSampleProvider} that extracts VO2 Max values from
 * workouts, for devices that provide them as part of the workout data.
 */
public class WorkoutLoadSampleProvider implements TimeSampleProvider<WorkoutLoadSample> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutLoadSampleProvider.class);

    private final GBDevice device;
    private final DaoSession session;

    public WorkoutLoadSampleProvider(final GBDevice device, final DaoSession session) {
        this.device = device;
        this.session = session;
    }

    @NonNull
    @Override
    public List<WorkoutLoadSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .where(BaseActivitySummaryDao.Properties.StartTime.gt(new Date(timestampFrom)))
                .where(BaseActivitySummaryDao.Properties.StartTime.lt(new Date(timestampTo)))
                .orderAsc(BaseActivitySummaryDao.Properties.StartTime);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();
        fillSummaryData(coordinator, samples);

        return samples.stream()
                .map(GarminWorkoutLoadSample::fromActivitySummary)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addSample(final WorkoutLoadSample timeSample) {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Override
    public void addSamples(final List<WorkoutLoadSample> timeSamples) {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Override
    public WorkoutLoadSample createSample() {
        throw new UnsupportedOperationException("Read-only sample provider");
    }

    @Nullable
    @Override
    public WorkoutLoadSample getLatestSample(final long until) {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return null;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();

        if (until != 0) {
            qb.where(BaseActivitySummaryDao.Properties.StartTime.le(new Date(until)));
        }

        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .orderDesc(BaseActivitySummaryDao.Properties.StartTime)
                .limit(1);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();
        fillSummaryData(coordinator, samples);

        return !samples.isEmpty() ? GarminWorkoutLoadSample.fromActivitySummary(samples.get(0)) : null;
    }

    private void fillSummaryData(final DeviceCoordinator coordinator,
                                 final Collection<BaseActivitySummary> summaries) {
        ActivitySummaryParser activitySummaryParser = coordinator.getActivitySummaryParser(device, GBApplication.getContext());
        for (final BaseActivitySummary summary : summaries) {
            if (summary.getSummaryData() == null) {
                activitySummaryParser.parseBinaryData(summary, true);
            }
        }
    }

    @Nullable
    @Override
    public WorkoutLoadSample getLatestSample() {
        return getLatestSample(0);
    }

    @Nullable
    @Override
    public WorkoutLoadSample getFirstSample() {
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final Device dbDevice = DBHelper.findDevice(device, session);
        if (dbDevice == null) {
            // no device, no samples
            return null;
        }

        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId()))
                .orderAsc(BaseActivitySummaryDao.Properties.StartTime)
                .limit(1);

        final List<BaseActivitySummary> samples = qb.build().list();
        summaryDao.detachAll();

        return !samples.isEmpty() ? GarminWorkoutLoadSample.fromActivitySummary(samples.get(0)) : null;
    }

    public static class GarminWorkoutLoadSample implements WorkoutLoadSample {
        private final long timestamp;
        private final int value;

        public GarminWorkoutLoadSample(final long timestamp, final int value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int getValue() {
            return value;
        }

        @Nullable
        public static GarminWorkoutLoadSample fromActivitySummary(final BaseActivitySummary summary) {
            final String summaryDataJson = summary.getSummaryData();
            if (summaryDataJson == null) {
                return null;
            }

            if (!summaryDataJson.contains(ActivitySummaryEntries.TRAINING_LOAD)) {
                return null;
            }

            final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summaryDataJson);
            if (summaryData == null) {
                return null;
            }

            final int value = summaryData.getNumber(ActivitySummaryEntries.TRAINING_LOAD, 0).intValue();
            if (value == 0) {
                return null;
            }

            return new GarminWorkoutLoadSample(summary.getStartTime().getTime(), value);
        }
    }
}
