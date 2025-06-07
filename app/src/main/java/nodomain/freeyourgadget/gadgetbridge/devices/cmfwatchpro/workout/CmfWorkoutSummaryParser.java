/*  Copyright (C) 2024-2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SCORE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_AEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_ANAEROBIC;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_FAT_BURN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_MAXIMUM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_ZONE_WARM_UP;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.RECOVERY_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TRAINING_LOAD;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STEPS;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro.CmfActivityType;

public class CmfWorkoutSummaryParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(CmfWorkoutSummaryParser.class);

    private final GBDevice gbDevice;
    private final Context context;

    public CmfWorkoutSummaryParser(final GBDevice device, final Context context) {
        this.gbDevice = device;
        this.context = context;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        final byte[] rawSummaryData = summary.getRawSummaryData();
        if (rawSummaryData == null) {
            return summary;
        }

        final ByteBuffer buf = ByteBuffer.wrap(rawSummaryData).order(ByteOrder.LITTLE_ENDIAN);

        final ActivitySummaryData summaryData = new ActivitySummaryData();

        // We should be able to get at least the first 3 right
        final int startTime = buf.getInt();
        summary.setStartTime(new Date(startTime * 1000L));

        final int duration = buf.getShort();
        summaryData.add(ACTIVE_SECONDS, duration, UNIT_SECONDS);

        final byte workoutType = buf.get();
        final CmfActivityType cmfActivityType = CmfActivityType.fromCode(workoutType);
        if (cmfActivityType != null) {
            summary.setActivityKind(cmfActivityType.getActivityKind().getCode());
        } else {
            summary.setActivityKind(ActivityKind.UNKNOWN.getCode());
        }

        try {
            final int hrAvg = buf.get() & 0xff;
            summaryData.add(HR_AVG, hrAvg, UNIT_BPM);

            final int calories = buf.getShort();
            summaryData.add(CALORIES_BURNT, calories, UNIT_KCAL);

            final int steps = buf.getInt();
            summaryData.add(STEPS, steps, UNIT_STEPS);

            final int distanceMeters = buf.getInt();
            summaryData.add(DISTANCE_METERS, distanceMeters, UNIT_METERS);

            buf.get(new byte[8]); //?

            final int endTime = buf.getInt();
            summary.setEndTime(new Date(endTime * 1000L));

            final boolean gps = buf.get() == 1;
            buf.get(); // ?

            if (buf.position() < buf.limit()) {
                // Watch 2 has more information
                final int trainingLoad = buf.getShort();
                summaryData.add(TRAINING_LOAD, trainingLoad, UNIT_NONE);

                buf.get(new byte[4]); //?

                final int recoveryTimeMinutes = buf.getShort();
                summaryData.add(RECOVERY_TIME, recoveryTimeMinutes * 60, UNIT_SECONDS);

                final List<String> zones = Arrays.asList(HR_ZONE_WARM_UP, HR_ZONE_FAT_BURN, HR_ZONE_AEROBIC, HR_ZONE_ANAEROBIC, HR_ZONE_MAXIMUM);
                final int[] zoneTimes = new int[zones.size()];

                zoneTimes[0] = buf.getShort() * 60;
                zoneTimes[1] = buf.getShort() * 60;
                zoneTimes[2] = buf.getShort() * 60;
                zoneTimes[3] = buf.getShort() * 60;
                zoneTimes[4] = buf.getShort() * 60;
                final float totalTimeInZone = Arrays.stream(zoneTimes).asLongStream().sum();
                if (totalTimeInZone > 0) {
                    final int[] zoneColors = new int[]{
                            context.getResources().getColor(R.color.hr_zone_warm_up_color),
                            context.getResources().getColor(R.color.hr_zone_easy_color),
                            context.getResources().getColor(R.color.hr_zone_aerobic_color),
                            context.getResources().getColor(R.color.hr_zone_threshold_color),
                            context.getResources().getColor(R.color.hr_zone_maximum_color),
                    };

                    for (int i = 0; i < zones.size(); i++) {
                        summaryData.add(
                                zones.get(i),
                                new ActivitySummaryProgressEntry(
                                        zoneTimes[i],
                                        UNIT_SECONDS,
                                        (int) ((100 * zoneTimes[i]) / totalTimeInZone),
                                        zoneColors[i]
                                )
                        );
                    }
                }

                final int activityScore = buf.getShort();
                summaryData.add(ACTIVE_SCORE, Math.round(activityScore / 1000f), UNIT_NONE);
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse workout binary data", e);
        }

        summary.setSummaryData(summaryData.toString());

        return summary;
    }
}
