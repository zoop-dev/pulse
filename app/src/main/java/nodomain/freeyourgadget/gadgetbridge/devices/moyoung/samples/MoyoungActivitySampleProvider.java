/*  Copyright (C) 2019 krzys_h

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.SqlUtils;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class MoyoungActivitySampleProvider extends AbstractSampleProvider<MoyoungActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(MoyoungActivitySampleProvider.class);

    public static final int SOURCE_NOT_MEASURED = -1;
    public static final int SOURCE_STEPS_REALTIME = 1;     // steps gathered at realtime from the steps characteristic
    public static final int SOURCE_STEPS_SUMMARY = 2;      // steps gathered from the daily summary
    public static final int SOURCE_STEPS_IDLE = 3;         // idle sample inserted because the user was not moving (to differentiate from missing data because watch not connected)
    public static final int SOURCE_SLEEP_SUMMARY = 4;      // data collected from the sleep function
    public static final int SOURCE_SINGLE_MEASURE = 5;     // heart rate / blood data gathered from the "single measurement" function
    public static final int SOURCE_TRAINING_HEARTRATE = 6; // heart rate data collected from the training function
    public static final int SOURCE_BATTERY = 7;            // battery report

    public static final int ACTIVITY_NOT_MEASURED = -1;
    public static final int ACTIVITY_SLEEP_LIGHT = 16;
    public static final int ACTIVITY_SLEEP_RESTFUL = 17;
    public static final int ACTIVITY_SLEEP_START = 18;
    public static final int ACTIVITY_SLEEP_END = 19;
    public static final int ACTIVITY_SLEEP_REM = 20;

    public MoyoungActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<MoyoungActivitySample, ?> getSampleDao() {
        return getSession().getMoyoungActivitySampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return MoyoungActivitySampleDao.Properties.Timestamp;
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return MoyoungActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return MoyoungActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public MoyoungActivitySample createActivitySample() {
        return new MoyoungActivitySample();
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        if (rawType == ACTIVITY_NOT_MEASURED)
            return ActivityKind.NOT_MEASURED;
        else if (rawType == ACTIVITY_SLEEP_LIGHT)
            return ActivityKind.LIGHT_SLEEP;
        else if (rawType == ACTIVITY_SLEEP_RESTFUL)
            return ActivityKind.DEEP_SLEEP;
        else if (rawType == ACTIVITY_SLEEP_REM)
            return ActivityKind.REM_SLEEP;
        else if (rawType == ACTIVITY_SLEEP_START || rawType == ACTIVITY_SLEEP_END)
            return ActivityKind.NOT_MEASURED;
        else
            return MoyoungConstants.WORKOUT_TYPES_TO_ACTIVITY_KIND.getOrDefault((byte) rawType, ActivityKind.ACTIVITY);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        if (activityKind == ActivityKind.NOT_MEASURED)
            return ACTIVITY_NOT_MEASURED;
        else if (activityKind == ActivityKind.LIGHT_SLEEP)
            return ACTIVITY_SLEEP_LIGHT;
        else if (activityKind == ActivityKind.DEEP_SLEEP)
            return ACTIVITY_SLEEP_RESTFUL;
        else if (activityKind == ActivityKind.REM_SLEEP)
            return ACTIVITY_SLEEP_REM;
        else if (activityKind == ActivityKind.ACTIVITY)
            return ACTIVITY_NOT_MEASURED; // TODO: ?
        else
            throw new IllegalArgumentException("Invalid Gadgetbridge activity kind: " + activityKind);
    }

    final ActivityKind sleepStageToActivityKind(final int sleepStage) {
        switch (sleepStage) {
            case MoyoungConstants.SLEEP_LIGHT:
                return ActivityKind.LIGHT_SLEEP;
            case MoyoungConstants.SLEEP_RESTFUL:
                return ActivityKind.DEEP_SLEEP;
            case MoyoungConstants.SLEEP_REM:
                return ActivityKind.REM_SLEEP;
            case MoyoungConstants.SLEEP_SOBER:
                return ActivityKind.AWAKE_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        if (rawIntensity == ActivitySample.NOT_MEASURED)
            return Float.NEGATIVE_INFINITY;
        else
            return rawIntensity;
    }

    @Override
    protected List<MoyoungActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        LOG.trace(
                "Getting Moyoung activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );
        final long nanoStart = System.nanoTime();

        final List<MoyoungActivitySample> samples = fillGaps(
                super.getGBActivitySamples(timestamp_from, timestamp_to),
                timestamp_from,
                timestamp_to
        );

        final Map<Integer, MoyoungActivitySample> sampleByTs = new HashMap<>();
        for (final MoyoungActivitySample sample : samples) {
            sampleByTs.put(sample.getTimestamp(), sample);
        }

        overlayHeartRate(sampleByTs, timestamp_from, timestamp_to);
        overlaySleep(sampleByTs, timestamp_from, timestamp_to);

        final List<MoyoungActivitySample> finalSamples = new ArrayList<>(sampleByTs.values());
        Collections.sort(finalSamples, Comparator.comparingInt(MoyoungActivitySample::getTimestamp));

        final long nanoEnd = System.nanoTime();
        final long executionTime = (nanoEnd - nanoStart) / 1000000;
        LOG.trace("Getting Moyoung samples took {}ms", executionTime);

        return finalSamples;
    }

    private void overlayHeartRate(final Map<Integer, MoyoungActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final MoyoungHeartRateSampleProvider heartRateSampleProvider = new MoyoungHeartRateSampleProvider(getDevice(), getSession());
        final List<MoyoungHeartRateSample> hrSamples = heartRateSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        for (final MoyoungHeartRateSample hrSample : hrSamples) {
            // round to the nearest minute, we don't need per-second granularity
            final int tsSeconds = (int) ((hrSample.getTimestamp() / 1000) / 60) * 60;
            MoyoungActivitySample sample = sampleByTs.get(tsSeconds);
            if (sample == null) {
                sample = new MoyoungActivitySample();
                sample.setTimestamp(tsSeconds);
                sample.setProvider(this);
                sampleByTs.put(tsSeconds, sample);
            }

            sample.setHeartRate(hrSample.getHeartRate());
        }
    }

    private void overlaySleep(final Map<Integer, MoyoungActivitySample> sampleByTs, final int timestamp_from, final int timestamp_to) {
        final MoyoungSleepStageSampleProvider sleepStageSampleProvider = new MoyoungSleepStageSampleProvider(getDevice(), getSession());
        final List<MoyoungSleepStageSample> sleepStageSamples = sleepStageSampleProvider.getAllSamples(timestamp_from * 1000L, timestamp_to * 1000L);

        // Retrieve the last stage before this time range, as the user could have been asleep during
        // the range transition
        final MoyoungSleepStageSample lastSleepStageBeforeRange = sleepStageSampleProvider.getLastSampleBefore(timestamp_from * 1000L);
        if (lastSleepStageBeforeRange != null && lastSleepStageBeforeRange.getStage() != MoyoungConstants.SLEEP_SOBER) {
            LOG.debug("Last sleep stage before range: ts={}, stage={}", lastSleepStageBeforeRange.getTimestamp(), lastSleepStageBeforeRange.getStage());
            sleepStageSamples.add(0, lastSleepStageBeforeRange);
        }
        // Retrieve the next sample after the time range, as the last stage could exceed it
        final MoyoungSleepStageSample nextSleepStageAfterRange = sleepStageSampleProvider.getNextSampleAfter(timestamp_to * 1000L);
        if (nextSleepStageAfterRange != null) {
            LOG.debug("Next sleep stage after range: ts={}, stage={}", nextSleepStageAfterRange.getTimestamp(), nextSleepStageAfterRange.getStage());
            sleepStageSamples.add(nextSleepStageAfterRange);
        }

        if (sleepStageSamples.size() > 1) {
            LOG.debug("Overlaying with data from {} sleep stage samples", sleepStageSamples.size());
        } else {
            LOG.warn("Not overlaying sleep data because more than 1 sleep stage sample is required");
            return;
        }

        MoyoungSleepStageSample prevSample = null;
        for (final MoyoungSleepStageSample sleepStageSample : sleepStageSamples) {
            if (prevSample == null) {
                prevSample = sleepStageSample;
                continue;
            }
            final ActivityKind sleepRawKind = sleepStageToActivityKind(prevSample.getStage());
            if (sleepRawKind.equals(ActivityKind.AWAKE_SLEEP)) {
                prevSample = sleepStageSample;
                continue;
            }
            // round to the nearest minute, we don't need per-second granularity
            final int tsSecondsPrev = (int) ((prevSample.getTimestamp() / 1000) / 60) * 60;
            final int tsSecondsCur = (int) ((sleepStageSample.getTimestamp() / 1000) / 60) * 60;
            for (int i = tsSecondsPrev; i < tsSecondsCur; i++) {
                if (i < timestamp_from || i > timestamp_to) continue;
                MoyoungActivitySample sample = sampleByTs.get(i);
                if (sample == null && i % 60 == 0) {
                    sample = new MoyoungActivitySample();
                    sample.setTimestamp(i);
                    sample.setProvider(this);
                    sampleByTs.put(i, sample);
                } else if (sample == null) {
                    continue;
                }
                sample.setRawKind(toRawActivityKind(sleepRawKind));
                sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            }
            prevSample = sleepStageSample;
        }
        if (prevSample != null && !sleepStageToActivityKind(prevSample.getStage()).equals(ActivityKind.AWAKE_SLEEP)) {
            LOG.warn("Last sleep stage sample was not of type awake");
        }
    }

    /**
     * Set the activity kind from NOT_MEASURED to new_raw_activity_kind on the given range
     *
     * @param timestamp_from        the start timestamp
     * @param timestamp_to          the end timestamp
     * @param new_raw_activity_kind the activity kind to set
     */
    public void updateActivityInRange(int timestamp_from, int timestamp_to, int new_raw_activity_kind) {
        // greenDAO does not provide a bulk update functionality, and manual update fails because
        // of no primary key

        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            throw new IllegalStateException("No device found");
        Property deviceProperty = getDeviceIdentifierSampleProperty();

        /*QueryBuilder<MoyoungActivitySample> qb = getSampleDao().queryBuilder();
        qb.where(deviceProperty.eq(dbDevice.getId()))
            .where(timestampProperty.ge(timestamp_from), timestampProperty.le(timestamp_to))
            .where(getRawKindSampleProperty().eq(ACTIVITY_NOT_MEASURED));
        List<MoyoungActivitySample> samples = qb.build().list();
        for (MoyoungActivitySample sample : samples) {
            sample.setProvider(this);
            sample.setRawKind(new_raw_activity_kind);
            sample.update();
        }*/

        String tablename = getSampleDao().getTablename();
        String baseSql = SqlUtils.createSqlUpdate(tablename, new String[]{getRawKindSampleProperty().columnName}, new String[]{});
        StringBuilder builder = new StringBuilder(baseSql);

        List<Object> values = new ArrayList<>();
        values.add(new_raw_activity_kind);
        List<WhereCondition> whereConditions = new ArrayList<>();
        whereConditions.add(deviceProperty.eq(dbDevice.getId()));
        whereConditions.add(timestampProperty.ge(timestamp_from));
        whereConditions.add(timestampProperty.le(timestamp_to));
        whereConditions.add(getRawKindSampleProperty().eq(ACTIVITY_NOT_MEASURED));

        ListIterator<WhereCondition> iter = whereConditions.listIterator();
        while (iter.hasNext()) {
            if (iter.hasPrevious()) {
                builder.append(" AND ");
            }
            WhereCondition condition = iter.next();
            condition.appendTo(builder, tablename);
            condition.appendValuesTo(values);
        }
        getSampleDao().getDatabase().execSQL(builder.toString(), values.toArray());
    }
}