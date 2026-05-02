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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SWIM_STYLE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_END;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.TIME_START;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_UNIX_EPOCH_SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiWorkoutType;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiSimpleActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSimpleActivityParser.class);

    public static final String XIAOMI_WORKOUT_TYPE = "xiaomiWorkoutType";

    /** Keys that should render even when the parsed value is zero. Default
     *  {@link ActivitySummaryData#add} drops zeros; for high-signal Xiaomi
     *  fields users still want to see the row (e.g. so an absent vitality
     *  gain is visibly "0" rather than missing). */
    private static final Set<String> ALWAYS_SHOW_KEYS = Set.of(
            ActivitySummaryEntries.VITALITY_GAIN,
            ActivitySummaryEntries.WORKOUT_LOAD
    );

    private final int headerSize;
    private final List<XiaomiSimpleDataEntry> dataEntries;

    public XiaomiSimpleActivityParser(final int headerSize, final List<XiaomiSimpleDataEntry> dataEntries) {
        this.headerSize = headerSize;
        this.dataEntries = dataEntries;
    }

    public void parse(final BaseActivitySummary summary, final ByteBuffer buf) {
        final ActivitySummaryData summaryData = new ActivitySummaryData();

        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Header: {}", GB.hexdump(header));

        final Map<String, Number> hrZones = new HashMap<>(5);

        for (int i = 0; i < dataEntries.size(); i++) {
            final int before = buf.position();

            final XiaomiSimpleDataEntry dataEntry = dataEntries.get(i);

            final Number value = dataEntry.get(buf);
            final int fieldSize = buf.position() - before;
            if (value == null) {
                LOG.debug("Field {} = {} unknown bytes, skipping", i, fieldSize);
                continue;
            }

            LOG.debug("Field {} = {} as {} ({}b)", i, value, dataEntry, fieldSize);

            // Each bit in the header marks whether the data is valid or not, in order of the fields
            final boolean validData = (header[i / 8] & (1 << (7 - (i % 8)))) != 0;
            // FIXME: We can't use the header before identifying the correct field lengths for unknown fields
            // or parsing gets out of sync with the header and we will potentially ignore valid data
            //if (!validData) {
            //    LOG.debug("Ignoring non-valid data {}", i);
            //    continue;
            //}

            if (dataEntry.getKey().equals(TIME_END)) {
                if (dataEntry.getUnit().equals(UNIT_UNIX_EPOCH_SECONDS)) {
                    summary.setEndTime(new Date(value.intValue() * 1000L));
                } else {
                    throw new IllegalArgumentException("endTime should be an unix epoch");
                }
            } else if (dataEntry.getKey().equals(TIME_START)) {
                // ignored
            } else if (dataEntry.getKey().equals(SWIM_STYLE)) {
                String swimStyleName = "unknown";
                final float swimStyle = value.floatValue();

                if (swimStyle == 0) {
                    swimStyleName = "medley";
                } else if (swimStyle == 1) {
                    swimStyleName = "breaststroke";
                } else if (swimStyle == 2) {
                    swimStyleName = "freestyle";
                } else if (swimStyle == 3) {
                    swimStyleName = "backstroke";
                } else if (swimStyle == 4) {
                    swimStyleName = "butterfly";
                }

                summaryData.add(dataEntry.getKey(), swimStyleName);
            } else if (dataEntry.getKey().equals(XIAOMI_WORKOUT_TYPE)) {
                final ActivityKind activityKind = XiaomiWorkoutType.fromCode(value.intValue());
                summary.setActivityKind(activityKind.getCode());
                if (activityKind == ActivityKind.UNKNOWN) {
                    summaryData.add(dataEntry.getKey(), value, UNIT_NONE);
                }
            } else if (ActivitySummaryEntries.HR_ZONES.containsKey(dataEntry.getKey())) {
                // Save the HR zones so we can add them later in order
                hrZones.put(dataEntry.getKey(), value);
            } else {
                final String key = dataEntry.getKey();
                final boolean force = ALWAYS_SHOW_KEYS.contains(key);
                summaryData.add(key, value.floatValue(), dataEntry.getUnit(), force);
            }
        }

        if (!hrZones.isEmpty()) {
            final int totalTime = hrZones.values().stream().mapToInt(Number::intValue).sum();
            if (totalTime != 0) {
                for (Map.Entry<String, Integer> zone : ActivitySummaryEntries.HR_ZONES.entrySet()) {
                    final String zoneKey = zone.getKey();
                    if (!hrZones.containsKey(zoneKey)) {
                        continue;
                    }
                    final int zoneColor = zone.getValue();
                    final int zoneTime = Objects.requireNonNull(hrZones.get(zoneKey)).intValue();

                    final int resolvedColor;
                    if (zoneColor != 0 && GBApplication.getContext() != null) {
                        resolvedColor = GBApplication.getContext().getResources().getColor(zoneColor);
                    } else {
                        resolvedColor = 0;
                    }
                    summaryData.add(
                            zoneKey,
                            new ActivitySummaryProgressEntry(
                                    zoneTime,
                                    UNIT_SECONDS,
                                    ((100 * zoneTime) / totalTime),
                                    resolvedColor
                            )
                    );
                }
            }
        }

        // hasGps is derived from rawDetailsPath: WorkoutGpsParser sets the path only when
        // a non-empty GPS_TRACK file is saved. WorkoutGpsParser also re-runs this derivation
        // when it persists, so order does not matter.
        summaryData.setHasGps(summary.getRawDetailsPath() != null);

        computeGoalPercents(summaryData);

        summary.setSummaryData(summaryData.toString());
    }

    /** Derive *_goal_percent rows from each (actual, goal) pair the parser emitted.
     *  Skipped when the goal is zero or the actual is missing. For pace, lower actual
     *  is better, so percent = goal / actual; for everything else, percent = actual / goal.
     *  Values are clamped to [0, 999] to keep the rendered chip stable on outliers. */
    private static void computeGoalPercents(final ActivitySummaryData data) {
        computeGoalPercent(data, ActivitySummaryEntries.ACTIVE_SECONDS,
                ActivitySummaryEntries.TIME_GOAL, ActivitySummaryEntries.TIME_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.CALORIES_BURNT,
                ActivitySummaryEntries.CALORIES_GOAL, ActivitySummaryEntries.CALORIES_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.DISTANCE_METERS,
                ActivitySummaryEntries.DISTANCE_GOAL, ActivitySummaryEntries.DISTANCE_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.SPEED_AVG,
                ActivitySummaryEntries.SPEED_GOAL, ActivitySummaryEntries.SPEED_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.CADENCE_AVG,
                ActivitySummaryEntries.CADENCE_GOAL, ActivitySummaryEntries.CADENCE_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.LAPS,
                ActivitySummaryEntries.LENGTHS_GOAL, ActivitySummaryEntries.LENGTHS_GOAL_PERCENT, false);
        computeGoalPercent(data, ActivitySummaryEntries.PACE_AVG_SECONDS_KM,
                ActivitySummaryEntries.PACE_GOAL, ActivitySummaryEntries.PACE_GOAL_PERCENT, true);
    }

    private static void computeGoalPercent(final ActivitySummaryData data,
                                           final String actualKey,
                                           final String goalKey,
                                           final String percentKey,
                                           final boolean lowerIsBetter) {
        final Number actualNum = data.getNumber(actualKey, null);
        final Number goalNum = data.getNumber(goalKey, null);
        if (actualNum == null || goalNum == null) return;
        final double actual = actualNum.doubleValue();
        final double goal = goalNum.doubleValue();
        if (goal <= 0) return;
        final double percent;
        if (lowerIsBetter) {
            if (actual <= 0) return;
            percent = goal / actual * 100.0;
        } else {
            percent = actual / goal * 100.0;
        }
        if (!Double.isFinite(percent)) return;
        final double clamped = Math.max(0.0, Math.min(percent, 999.0));
        data.add(percentKey, (float) clamped, ActivitySummaryEntries.UNIT_PERCENTAGE, true);
    }

    public static class Builder {
        private int headerSize;
        private final List<XiaomiSimpleDataEntry> dataEntries = new ArrayList<>();

        public Builder setHeaderSize(final int headerSize) {
            this.headerSize = headerSize;
            return this;
        }

        public Builder addByte(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, buf -> buf.get() & 0xff));
            return this;
        }

        public Builder addShort(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getShort));
            return this;
        }

        public Builder addShort(final String key, final String unit, final double multiplier) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getShort, multiplier));
            return this;
        }

        public Builder addInt(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getInt));
            return this;
        }

        public Builder addFloat(final String key, final String unit) {
            dataEntries.add(new XiaomiSimpleDataEntry(key, unit, ByteBuffer::getFloat));
            return this;
        }

        public Builder addUnknown(final int sizeBytes) {
            dataEntries.add(new XiaomiSimpleDataEntry(null, null, buf -> {
                buf.get(new byte[sizeBytes]);
                return null;
            }));
            return this;
        }

        public XiaomiSimpleActivityParser build() {
            return new XiaomiSimpleActivityParser(headerSize, dataEntries);
        }
    }
}
