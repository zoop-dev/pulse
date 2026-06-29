/*  Copyright (C) 2016-2026 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo, Petr Vaněk, Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.ToIntFunction;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractTimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Base class for all sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 * @param <T> the sample type
 */
public abstract class AbstractSampleProvider<T extends AbstractActivitySample> implements SampleProvider<T>, PersistenceProvider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSampleProvider.class);

    private static final WhereCondition[] NO_CONDITIONS = new WhereCondition[0];
    private static final int CUMULATIVE_COUNTER_DAY_BOUNDARY_MAX_GAP_SECONDS = 30 * 60;
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractSampleProvider(GBDevice device, DaoSession session) {
        mDevice = device;
        mSession = session;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @NonNull
    @Override
    public List<T> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to);
    }

    @NonNull
    @Override
    public List<T> getAllActivitySamplesHighRes(int timestamp_from, int timestamp_to) {
        return getGBActivitySamplesHighRes(timestamp_from, timestamp_to);
    }

    @Override
    public boolean hasHighResData() {
        return false;
    }

    @NonNull
    @Override
    @Deprecated // use getAllActivitySamples
    public List<T> getActivitySamples(int timestamp_from, int timestamp_to) {
        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, Collections.singleton(ActivityKind.ACTIVITY));
        }
    }

    @Override
    public void addGBActivitySample(T activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addGBActivitySamples(@NonNull List<T> activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    @Nullable
    @Override
    public T getLatestActivitySample() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderDesc(getTimestampSampleProperty()).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    @Nullable
    @Override
    public T getLatestActivitySample(final int until) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        Property timestampProperty = getTimestampSampleProperty();
        qb.where(timestampProperty.le(until))
                .where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    @Nullable
    @Override
    public T getFirstActivitySample() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderAsc(getTimestampSampleProperty()).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    @Nullable
    @Override
    public T getFirstActivitySample(final int after) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        Property timestampProperty = getTimestampSampleProperty();
        qb.where(timestampProperty.gt(after))
                .where(deviceProperty.eq(dbDevice.getId()))
                .orderAsc(timestampProperty).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    /**
     * Get the activity samples between two timestamps (inclusive). Exactly one every minute.
     * @param timestamp_from Start timestamp
     * @param timestamp_to End timestamp
     * @return Exactly one sample for every minute
     */
    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestamp_from))
            .where(timestampProperty.le(timestamp_to))
            .orderAsc(timestampProperty);
        List<T> samples = qb.build().list();
        for (T sample : samples) {
            sample.setProvider(this);
        }
        detachFromSession();
        return samples;
    }

    /**
     * Get the activity samples between two timestamps (inclusive).
     * Differs from {@link #getGBActivitySamples(int, int)} in that it supplies as many samples as
     * available.
     * It assumes {@link #getGBActivitySamples(int, int)} returns the highest resolution data unless
     * this is overwritten.
     * @param timestamp_from Start timestamp
     * @param timestamp_to End timestamp
     * @return All the samples between start and end timestamp (inclusive)
     */
    protected List<T> getGBActivitySamplesHighRes(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to);
    }

    /**
     * Detaches all samples of this type from the session. Changes to them may not be
     * written back to the database.
     * <p>
     * Subclasses should call this method after performing custom queries.
     */
    protected void detachFromSession() {
        getSampleDao().detachAll();
    }

    private List<T> getActivitySamplesByActivityFilter(int timestamp_from, int timestamp_to, Set<ActivityKind> activityFilter) {
        List<T> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<T> filteredSamples = new ArrayList<>();

        for (T sample : samples) {
            if (activityFilter.contains(sample.getKind())) {
                filteredSamples.add(sample);
            }
        }
        return filteredSamples;
    }

    public abstract AbstractDao<T,?> getSampleDao();

    @Nullable
    protected abstract Property getRawKindSampleProperty();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();

    public void convertCumulativeSteps(final List<T> samples, final Property stepsSampleProperty) {
        convertCumulativeSteps(samples, stepsSampleProperty, 0);
    }

    protected void convertCumulativeSteps(final List<T> samples,
                                          final Property stepsSampleProperty,
                                          final int previousSampleTimestampOffsetSeconds) {
        final T firstSample = samples.get(0);
        final int firstTimestamp = firstSample.getTimestamp();
        final int firstSteps = firstSample.getSteps();
        final int firstDistance = firstSample.getDistanceCm();
        final int firstActiveCalories = firstSample.getActiveCalories();

        // Fix over-counting at the turn of day
        final T lastSample = getLastSampleWithStepsBefore(firstTimestamp, stepsSampleProperty);
        if (lastSample != null) {
            final int previousTimestamp = lastSample.getTimestamp() + previousSampleTimestampOffsetSeconds;
            firstSample.setSteps(convertFirstCumulativeValue(
                    previousTimestamp,
                    firstTimestamp,
                    samples,
                    0,
                    firstSteps,
                    lastSample.getSteps(),
                    AbstractActivitySample::getSteps
            ));
            firstSample.setDistanceCm(convertFirstCumulativeValue(
                    previousTimestamp,
                    firstTimestamp,
                    samples,
                    0,
                    firstDistance,
                    lastSample.getDistanceCm(),
                    AbstractActivitySample::getDistanceCm
            ));
            firstSample.setActiveCalories(convertFirstCumulativeValue(
                    previousTimestamp,
                    firstTimestamp,
                    samples,
                    0,
                    firstActiveCalories,
                    lastSample.getActiveCalories(),
                    AbstractActivitySample::getActiveCalories
            ));
        }

        // This slightly breaks activity recognition, because we don't have per-minute granularity...
        final CumulativeCounterState stepsState = initialCounterState(
                firstTimestamp,
                firstSteps,
                lastSample,
                previousSampleTimestampOffsetSeconds,
                AbstractActivitySample::getSteps
        );
        final CumulativeCounterState distanceState = initialCounterState(
                firstTimestamp,
                firstDistance,
                lastSample,
                previousSampleTimestampOffsetSeconds,
                AbstractActivitySample::getDistanceCm
        );
        final CumulativeCounterState activeCaloriesState = initialCounterState(
                firstTimestamp,
                firstActiveCalories,
                lastSample,
                previousSampleTimestampOffsetSeconds,
                AbstractActivitySample::getActiveCalories
        );
        // Round timestamp to the nearest minute
        samples.get(0).setTimestamp((firstTimestamp / 60) * 60);

        for (int i = 1; i < samples.size(); i++) {
            final T s1 = samples.get(i - 1);
            final T s2 = samples.get(i);
            final int timestamp = (s2.getTimestamp() / 60) * 60;
            final int steps = s2.getSteps();
            final int distance = s2.getDistanceCm();
            final int activeCalories = s2.getActiveCalories();
            s2.setTimestamp(timestamp);

            s2.setSteps(convertCumulativeValue(
                    samples,
                    i,
                    s1.getTimestamp(),
                    timestamp,
                    steps,
                    stepsState,
                    AbstractActivitySample::getSteps,
                    "steps"
            ));
            s2.setDistanceCm(convertCumulativeValue(
                    samples,
                    i,
                    s1.getTimestamp(),
                    timestamp,
                    distance,
                    distanceState,
                    AbstractActivitySample::getDistanceCm,
                    "distance"
            ));
            s2.setActiveCalories(convertCumulativeValue(
                    samples,
                    i,
                    s1.getTimestamp(),
                    timestamp,
                    activeCalories,
                    activeCaloriesState,
                    AbstractActivitySample::getActiveCalories,
                    "active calories"
            ));
        }
    }

    private int convertFirstCumulativeValue(final int previousTimestamp,
                                            final int currentTimestamp,
                                            final List<T> samples,
                                            final int currentIndex,
                                            final int currentValue,
                                            final int previousValue,
                                            final ToIntFunction<T> counterValue) {
        if (shouldRebaseCumulativeValueAfter(
                previousTimestamp,
                currentTimestamp,
                samples,
                currentIndex,
                currentValue,
                previousValue,
                counterValue
        )) {
            return rebaseContinuedCumulativeValue(currentValue, previousValue);
        }
        return currentValue;
    }

    private CumulativeCounterState initialCounterState(final int firstTimestamp,
                                                       final int firstValue,
                                                       @Nullable final T lastSample,
                                                       final int previousSampleTimestampOffsetSeconds,
                                                       final ToIntFunction<T> counterValue) {
        if (measuredCounterValue(firstValue) || lastSample == null) {
            return new CumulativeCounterState(firstTimestamp, firstValue);
        }

        final int previousTimestamp = lastSample.getTimestamp() + previousSampleTimestampOffsetSeconds;
        return new CumulativeCounterState(
                previousTimestamp,
                counterValue.applyAsInt(lastSample),
                isPotentialDayBoundaryGap(previousTimestamp, firstTimestamp)
        );
    }

    private int convertCumulativeValue(final List<T> samples,
                                       final int currentIndex,
                                       final int previousSampleTimestamp,
                                       final int currentTimestamp,
                                       final int currentValue,
                                       final CumulativeCounterState state,
                                       final ToIntFunction<T> counterValue,
                                       final String counterName) {
        final boolean crossedDayBoundary = !sameDay(previousSampleTimestamp, currentTimestamp);

        if (crossedDayBoundary || state.pendingDayBoundary) {
            if (!measuredCounterValue(currentValue)) {
                if (crossedDayBoundary) {
                    state.pendingDayBoundary = true;
                }
                return currentValue;
            }

            final int convertedValue = convertFirstCumulativeValue(
                    state.previousTimestamp,
                    currentTimestamp,
                    samples,
                    currentIndex,
                    currentValue,
                    state.previousValue,
                    counterValue
            );
            state.update(currentTimestamp, currentValue);
            state.pendingDayBoundary = false;
            return convertedValue;
        }

        if (!measuredCounterValue(currentValue)) {
            return currentValue;
        }

        if (counterDecreased(currentValue, state.previousValue)) {
            // This is likely a bug, since cumulative activity counters should not go down within the same day.
            // Mitigate it by ignoring the current sample, but keep the new baseline for following samples.
            LOG.warn(
                    "Cumulative {} went down from {} to {} ({} to {}) within the same day",
                    counterName,
                    state.previousTimestamp,
                    currentTimestamp,
                    state.previousValue,
                    currentValue
            );
            state.update(currentTimestamp, currentValue);
            return ActivitySample.NOT_MEASURED;
        }

        if (currentValue > 0) {
            final int previousValue = state.previousValue;
            state.update(currentTimestamp, currentValue);
            return currentValue - previousValue;
        }

        state.update(currentTimestamp, currentValue);
        return currentValue;
    }

    private boolean shouldRebaseCumulativeValueAfter(final int previousTimestamp,
                                                     final int currentTimestamp,
                                                     final List<T> samples,
                                                     final int currentIndex,
                                                     final int currentValue,
                                                     final int previousValue,
                                                     final ToIntFunction<T> counterValue) {
        if (!continuedCumulativeValue(currentValue, previousValue)) {
            return false;
        }

        if (sameDay(previousTimestamp, currentTimestamp)) {
            return true;
        }

        if (currentTimestamp <= previousTimestamp
                || currentTimestamp - previousTimestamp > CUMULATIVE_COUNTER_DAY_BOUNDARY_MAX_GAP_SECONDS) {
            return false;
        }

        return currentValue == previousValue || hasCounterDecreaseNear(samples, currentIndex, counterValue);
    }

    private boolean hasCounterDecreaseNear(final List<T> samples,
                                           final int startIndex,
                                           final ToIntFunction<T> counterValue) {
        final T firstSample = samples.get(startIndex);
        final int firstTimestamp = firstSample.getTimestamp();
        int previousTimestamp = firstTimestamp;
        int previousValue = counterValue.applyAsInt(firstSample);

        for (int i = startIndex + 1; i < samples.size(); i++) {
            final T sample = samples.get(i);
            if (!sameDay(firstTimestamp, sample.getTimestamp())
                    || sample.getTimestamp() - firstTimestamp > CUMULATIVE_COUNTER_DAY_BOUNDARY_MAX_GAP_SECONDS) {
                break;
            }

            final int currentValue = counterValue.applyAsInt(sample);
            if (sameDay(previousTimestamp, sample.getTimestamp())
                    && counterDecreased(currentValue, previousValue)) {
                return true;
            }

            if (measuredCounterValue(currentValue)) {
                previousTimestamp = sample.getTimestamp();
                previousValue = currentValue;
            }
        }

        return false;
    }

    private int rebaseContinuedCumulativeValue(final int currentValue, final int previousValue) {
        if (continuedCumulativeValue(currentValue, previousValue)) {
            return currentValue - previousValue;
        }
        return currentValue;
    }

    private boolean continuedCumulativeValue(final int currentValue, final int previousValue) {
        return currentValue > 0 && previousValue > 0 && currentValue >= previousValue;
    }

    private boolean measuredCounterValue(final int value) {
        return value >= 0;
    }

    private boolean counterDecreased(final int currentValue, final int previousValue) {
        return currentValue >= 0 && previousValue > 0 && currentValue < previousValue;
    }

    private boolean isPotentialDayBoundaryGap(final int previousTimestamp, final int currentTimestamp) {
        if (currentTimestamp < previousTimestamp
                || currentTimestamp - previousTimestamp > CUMULATIVE_COUNTER_DAY_BOUNDARY_MAX_GAP_SECONDS) {
            return false;
        }

        return !sameDay(previousTimestamp, currentTimestamp);
    }

    private static final class CumulativeCounterState {
        private int previousTimestamp;
        private int previousValue;
        private boolean pendingDayBoundary;

        private CumulativeCounterState(final int previousTimestamp, final int previousValue) {
            this(previousTimestamp, previousValue, false);
        }

        private CumulativeCounterState(final int previousTimestamp,
                                       final int previousValue,
                                       final boolean pendingDayBoundary) {
            update(previousTimestamp, previousValue);
            this.pendingDayBoundary = pendingDayBoundary;
        }

        private void update(final int timestamp, final int value) {
            previousTimestamp = timestamp;
            previousValue = value > 0 ? value : 0;
        }
    }

    @Nullable
    public T getLastSampleWithStepsBefore(final int timestampTo, final Property stepsSampleProperty) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final List<T> samples = getSampleDao().queryBuilder()
                .where(
                        getDeviceIdentifierSampleProperty().eq(dbDevice.getId()),
                        getTimestampSampleProperty().le(timestampTo),
                        stepsSampleProperty.gt(-1)
                ).orderDesc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    public boolean sameDay(final int t1, final int t2) {
        final Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(t1 * 1000L - 1000L);
        final LocalDate d1 = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTimeInMillis(t2 * 1000L - 1000L);
        final LocalDate d2 = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        return d1.equals(d2);
    }

    public LocalDate getLocalDate(final long timestampMillis) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMillis);
        return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    protected List<T> fillGaps(final List<T> samples, final int timestamp_from, final int timestamp_to) {
        if (samples.isEmpty()) {
            return samples;
        }

        final long nanoStart = System.nanoTime();

        final List<T> ret = new LinkedList<>(samples);

        //ret.sort(Comparator.comparingLong(T::getTimestamp));

        final int firstTimestamp = ret.get(0).getTimestamp();
        if (firstTimestamp - timestamp_from > 60) {
            // Gap at the start
            for (int ts = firstTimestamp - 60; ts >= timestamp_from; ts -= 60) {
                ret.add(0, createDummySample(ts));
            }
        }

        final int lastTimestamp = ret.get(ret.size() - 1).getTimestamp();
        // Do not generate fake samples into the future
        final long minTo = Math.min(timestamp_to, System.currentTimeMillis() / 1000L);
        if (minTo - lastTimestamp > 60) {
            // Gap at the end
            for (int ts = lastTimestamp + 60; ts <= minTo; ts += 60) {
                ret.add(createDummySample(ts));
            }
        }

        final ListIterator<T> it = ret.listIterator();
        T previousSample = it.next();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting filling gaps at {}", DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(previousSample.getTimestamp() * 1000L)));
        }

        while (it.hasNext()) {
            final T sample = it.next();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Processing for gaps at {}", DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(sample.getTimestamp() * 1000L)));
            }
            if (sample.getTimestamp() - previousSample.getTimestamp() > 60) {
                // Rewind before we insert the dummy samples so the list stays in order
                it.previous();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Filling gap between {} and {}", Instant.ofEpochSecond(previousSample.getTimestamp() + 60), Instant.ofEpochSecond(sample.getTimestamp()));
                }
                for (int ts = previousSample.getTimestamp() + 60; ts < sample.getTimestamp(); ts += 60) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Inserting dummy sample at {}", DateTimeUtils.formatDateTime(DateTimeUtils.parseTimestampMillis(ts * 1000L)));
                    }
                    it.add(createDummySample(ts));
                }
                // Move forward again
                it.next();
            }
            previousSample = sample;
        }

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        final int dummyCount = ret.size() - samples.size();
        LOG.trace("Filled gaps with {} samples in {}ms", dummyCount, executionTime);

        return ret;
    }

    protected T createDummySample(final int ts) {
        final T dummySample = createActivitySample();
        dummySample.setTimestamp(ts);
        dummySample.setRawKind(ActivityKind.UNKNOWN.getCode());
        dummySample.setRawIntensity(ActivitySample.NOT_MEASURED);
        dummySample.setSteps(ActivitySample.NOT_MEASURED);
        dummySample.setHeartRate(ActivitySample.NOT_MEASURED);
        dummySample.setDistanceCm(ActivitySample.NOT_MEASURED);
        dummySample.setActiveCalories(ActivitySample.NOT_MEASURED);
        dummySample.setProvider(this);
        return dummySample;
    }

    @Override
    public boolean persistSamples(@NonNull final List<T> samples, @Nullable final Context context) {
        if (samples.isEmpty()) {
            return true;
        }

        LOG.debug(
                "Will persist {} {} samples",
                samples.size(),
                getClass().getSimpleName().replace("Provider", "")
        );

        try {
            final DaoSession session = getSession();

            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.findDevice(gbDevice, session);
            if (device == null) {
                LOG.warn("Device not found in database for '{}'", gbDevice.getAliasOrName());
                return false;
            }
            final long deviceId = device.getId();

            final User user = DBHelper.getUser(session);
            final long userId = user.getId();

            for (final T sample : samples) {
                sample.setProvider(this);
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
            }

            addGBActivitySamples(samples);
        } catch (final Exception e) {
            LOG.error("Error saving samples", e);
            final Context ctx = (context != null) ? context : GBApplication.getContext();
            final String message = ctx.getString(R.string.persisting_samples_failed, e.getLocalizedMessage());
            toast(ctx, message, Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }
        return true;
    }
}
