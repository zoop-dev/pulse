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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminNapSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.util.RangeMap;

public class GarminActivitySampleProvider extends AbstractSampleProvider<GarminActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(GarminActivitySampleProvider.class);

    public GarminActivitySampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<GarminActivitySample, ?> getSampleDao() {
        return getSession().getGarminActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return GarminActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GarminActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GarminActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(final int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(final ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity / 100f;
    }

    @Override
    public GarminActivitySample createActivitySample() {
        return new GarminActivitySample();
    }

    @Override
    protected List<GarminActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        LOG.trace(
                "Getting garmin activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );

        final long nanoStart = System.nanoTime();

        // Each Garmin sample contains the cumulative value measured up until that specific timestamp. For example the
        // sample at midnight will actually contain the number of steps taken in the entire previous day.
        // This goes against what Gb expects (each sample actually corresponds to the value at the end of the minute).
        // Therefore, we fetch the data with an offset and then adjust by 1 minute
        final List<GarminActivitySample> samples = fillGaps(
                super.getGBActivitySamples(timestamp_from + 60, timestamp_to + 60),
                timestamp_from + 60,
                timestamp_to + 60
        );

        samples.forEach(s -> s.setTimestamp(s.getTimestamp() - 60));

        if (!samples.isEmpty()) {
            convertCumulativeSteps(samples, GarminActivitySampleDao.Properties.Steps);
        }

        convertCalories(samples);
        overlaySleep(samples, timestamp_from, timestamp_to);

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        LOG.trace("Getting Garmin samples took {}ms", executionTime);

        return samples;
    }

    /**
     * Converts the calories from kcal to cal
     */
    private void convertCalories(List<GarminActivitySample> samples) {
        for (GarminActivitySample sample : samples) {
            sample.setActiveCalories(sample.getActiveCalories() * 1000);
        }
    }

    public void overlaySleep(final List<GarminActivitySample> samples, final int timestamp_from, final int timestamp_to) {
        // The samples provided by Garmin are upper-bound timestamps of the sleep stage
        final RangeMap<Long, ActivityKind> stagesMap = new RangeMap<>(RangeMap.Mode.UPPER_BOUND);

        final GarminEventSampleProvider eventSampleProvider = new GarminEventSampleProvider(getDevice(), getSession());
        final List<GarminEventSample> sleepEventSamples = eventSampleProvider.getSleepEvents(
                timestamp_from * 1000L,
                timestamp_to * 1000L
        );
        if (!sleepEventSamples.isEmpty()) {
            LOG.debug("Found {} sleep event samples between {} and {}", sleepEventSamples.size(), timestamp_from, timestamp_to);
            for (final GarminEventSample event : sleepEventSamples) {
                switch (event.getEventType()) {
                    case 0: // start
                        // We only need the start event as an upper-bound timestamp (anything before it is unknown)
                        stagesMap.put(event.getTimestamp(), ActivityKind.UNKNOWN);
                        break;
                    case 1: // stop
                        // See FitImporter#processRawSleepSamples / #4048
                        if (event.getData() != null && event.getData() == -1) {
                            stagesMap.put(event.getTimestamp(), ActivityKind.LIGHT_SLEEP);
                        }
                    default:
                }
            }
        }

        final GarminSleepStageSampleProvider sleepStagesSampleProvider = new GarminSleepStageSampleProvider(getDevice(), getSession());

        // Retrieve the next stage after this time range
        final GarminEventSample nextSleepStageAfterRange = eventSampleProvider.getNextSleepEventAfter(timestamp_to * 1000L);
        if (nextSleepStageAfterRange != null && nextSleepStageAfterRange.getEventType() == 1) {
            // Sleep session actually ends outside of this range, we need to fetch the next sleep stage
            final GarminSleepStageSample nextStage = sleepStagesSampleProvider.getNextSampleAfter(timestamp_to * 1000L);
            if (nextStage != null) {
                stagesMap.put(nextStage.getTimestamp(), toActivityKind(nextStage));
            }
        }

        final List<GarminSleepStageSample> stageSamples = sleepStagesSampleProvider.getAllSamples(
                timestamp_from * 1000L,
                timestamp_to * 1000L
        );

        if (!stageSamples.isEmpty()) {
            // We got actual sleep stages
            LOG.debug("Found {} sleep stage samples between {} and {}", stageSamples.size(), timestamp_from, timestamp_to);

            for (final GarminSleepStageSample stageSample : stageSamples) {
                stagesMap.put(stageSample.getTimestamp(), toActivityKind(stageSample));
            }
        }

        // Overlap nap samples as light sleep
        // TODO: Dedicated nap support in Gb?
        final GarminNapSampleProvider napSampleProvider = new GarminNapSampleProvider(getDevice(), getSession());
        final List<GarminNapSample> napSamples = new ArrayList<>(napSampleProvider.getAllSamples(
                timestamp_from * 1000L,
                timestamp_to * 1000L
        ));
        final GarminNapSample lastNapSample = napSampleProvider.getLastSampleBefore(timestamp_from * 1000L);
        if (lastNapSample != null) {
            napSamples.add(lastNapSample);
        }
        for (final GarminNapSample napSample : napSamples) {
            stagesMap.put(napSample.getTimestamp(), ActivityKind.UNKNOWN);
            stagesMap.put(napSample.getEndTimestamp(), ActivityKind.LIGHT_SLEEP);
        }

        if (!stagesMap.isEmpty()) {
            if (!samples.isEmpty()) {
                for (final GarminActivitySample sample : samples) {
                    final long ts = sample.getTimestamp() * 1000L;
                    final ActivityKind sleepType = stagesMap.get(ts);
                    if (sleepType != null && !sleepType.equals(ActivityKind.UNKNOWN)) {
                        sample.setRawKind(sleepType.getCode());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    }
                }
            } else {
                for (int ts = timestamp_from; ts <= timestamp_to; ts += 60) {
                    final GarminActivitySample sample = createDummySample(ts);
                    final ActivityKind sleepType = stagesMap.get(ts * 1000L);
                    if (sleepType != null && !sleepType.equals(ActivityKind.UNKNOWN)) {
                        sample.setRawKind(sleepType.getCode());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    }
                    samples.add(sample);
                }
            }
        }
    }

    private ActivityKind toActivityKind(final GarminSleepStageSample stageSample) {
        final FieldDefinitionSleepStage.SleepStage sleepStage = FieldDefinitionSleepStage.SleepStage.fromId(stageSample.getStage());
        if (sleepStage == null) {
            LOG.error("Unknown sleep stage for {}", stageSample.getStage());
            return ActivityKind.UNKNOWN;
        }

        return switch (sleepStage) {
            case AWAKE -> ActivityKind.AWAKE_SLEEP;
            case LIGHT -> ActivityKind.LIGHT_SLEEP;
            case DEEP -> ActivityKind.DEEP_SLEEP;
            case REM -> ActivityKind.REM_SLEEP;
            default -> ActivityKind.UNKNOWN;
        };

    }
}
