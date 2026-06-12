/*  Copyright (C) 2024-2026 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.workouts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_EPOC_TIME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FOOT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FOOT_PER_HOUR;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KILOMETERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KNOTS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_LB;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_HOUR;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MILE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MILE_PER_HOUR;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_YARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_MILE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NAUTICAL_MILES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_RAW_STRING;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_YARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_M;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_SPORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit;
import nodomain.freeyourgadget.gadgetbridge.model.WeightUnit;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class WorkoutValueFormatter {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutValueFormatter.class);

    private boolean show_raw_data = false;

    private ActivityKind activityKind;
    private final DistanceUnit distanceUnit;
    private final WeightUnit weightUnit;
    private final boolean useNauticalUnits;
    private final DecimalFormat df2 = new DecimalFormat("#.##");
    private final DecimalFormat df1 = new DecimalFormat("#.#");

    public WorkoutValueFormatter() {
        this(ActivityKind.UNKNOWN);
    }

    public WorkoutValueFormatter(final ActivityKind activityKind) {
        this.activityKind = activityKind;
        this.distanceUnit = GBApplication.getPrefs().getDistanceUnit();
        this.weightUnit = GBApplication.getPrefs().getWeightUnit();
        this.useNauticalUnits = GBApplication.getPrefs().getBoolean("units_nautical", true);
    }

    public void setActivityKind(final ActivityKind activityKind) {
        this.activityKind = activityKind;
    }

    public void toggleRawData() {
        this.show_raw_data = !show_raw_data;
    }

    public String formatValue(final Object rawValue, String unit, boolean showUnit) {
        if (rawValue == null) {
            return GBApplication.getContext().getString(R.string.stats_empty_value);
        }

        if (UNIT_RAW_STRING.equals(unit)) {
            return String.valueOf(rawValue);
        }

        if (rawValue instanceof CharSequence || ActivitySummaryEntries.UNIT_STRING.equals(unit)) {
            // we could optimize here a bit and only do this for particular activities (swim at the moment...)
            try {
                return getStringResourceByName(String.valueOf(rawValue));
            } catch (final Exception e) {
                LOG.error("Failed to get string resource by name for {}", rawValue);
                return String.valueOf(rawValue);
            }
        }

        if (!(rawValue instanceof Number)) {
            return String.valueOf(rawValue);
        }

        double value = ((Number) rawValue).doubleValue();

        if (!show_raw_data) {
            //special casing here + imperial units handling

            if (UNIT_MM.equals(unit)) {
                if (value > 1000) {
                    unit = UNIT_METERS;
                    value /= 1000d;
                } else if (value > 100) {
                    unit = UNIT_CM;
                    value /= 10d;
                }
            }

            switch (unit) {
                case UNIT_KG:
                    return WeightUnit.Companion.formatWeight(GBApplication.getContext(), value, weightUnit);
                case UNIT_LB:
                    return WeightUnit.Companion.formatWeight(GBApplication.getContext(), value / 2.2046226f, weightUnit);
                case UNIT_CM:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 0.0328084;
                        unit = UNIT_FOOT;
                    }
                    break;
                case UNIT_METERS_PER_SECOND:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 2.236936D;
                        unit = UNIT_MILE_PER_HOUR;
                    } else { //metric
                        value = value * 3.6;
                        unit = UNIT_KMPH;
                    }
                    break;
                case UNIT_METERS_PER_HOUR:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 3.28084D;
                        unit = UNIT_FOOT_PER_HOUR;
                    }
                    break;
                case UNIT_SECONDS_PER_M:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * (1609.344 / 60D);
                        unit = UNIT_MINUTES_PER_MILE;
                    } else { //metric
                        value = value * (1000 / 60D);
                        unit = UNIT_MINUTES_PER_KM;
                    }
                    break;
                case UNIT_SECONDS_PER_KM:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value / 60D * 1.609344;
                        unit = UNIT_MINUTES_PER_MILE;
                    } else { //metric
                        value = value / 60D;
                        unit = UNIT_MINUTES_PER_KM;
                    }
                    break;
                case UNIT_KILOMETERS:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 0.621371D;
                        unit = UNIT_MILE;
                    }
                    break;
                case UNIT_METERS:
                    if (useNauticalUnits && ActivityKind.isNauticalActivity(activityKind)) {
                        value = value / 1852D;
                        unit = UNIT_NAUTICAL_MILES;
                    } else if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 3.28084D;
                        unit = UNIT_FOOT;
                        if (value > 6000) {
                            value = value * 0.0001893939D;
                            unit = UNIT_MILE;
                        }
                    } else { //metric
                        if (value > 2000) {
                            value = value / 1000;
                            unit = UNIT_KILOMETERS;
                        }
                    }
                    break;
                case UNIT_KMPH:
                    if (useNauticalUnits && ActivityKind.isNauticalActivity(activityKind)) {
                        value = value / 1.852D;
                        unit = UNIT_KNOTS;
                    } else if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value * 0.621371D;
                        unit = UNIT_MILE_PER_HOUR;
                    }
                    break;
                case UNIT_SECONDS_PER_100_METERS:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = (value * 0.9144) / 60D;
                        unit = UNIT_MINUTES_PER_100_YARDS;
                    } else { //metric
                        value = value / 60D;
                        unit = UNIT_MINUTES_PER_100_METERS;
                    }
                    break;
                case UNIT_SECONDS_PER_100_YARDS:
                    if (distanceUnit == DistanceUnit.IMPERIAL) {
                        value = value / 60D;
                        unit = UNIT_MINUTES_PER_100_YARDS;
                    } else { //metric
                        value = (value * 1.0936133D) / 60D;
                        unit = UNIT_MINUTES_PER_100_METERS;
                    }
                    break;
                case ActivitySummaryEntries.UNIT_JOULE:
                    if (value > 10000) {
                        value = value / 1000D;
                        unit = "unit_kilojoule";
                    }
            }
        }

        if (unit.equals(UNIT_SECONDS) && !show_raw_data && showUnit) { //rather than plain seconds, show formatted duration
            return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
        }else if (unit.equals(UNIT_SECONDS_SPORT) && !show_raw_data && showUnit) {
                return DateTimeUtils.formatSportsDuration(Math.round(1000L * (double) value), TimeUnit.MILLISECONDS);
        } else if (UNIT_EPOC_TIME.equals(unit) && !show_raw_data) {
            long epoc = ((Number) rawValue).longValue();
            return DateTimeUtils.formatLocalTime(epoc * 1000L);
        } else if (unit.equals(UNIT_MINUTES_PER_KM) || unit.equals(UNIT_MINUTES_PER_MILE) || unit.equals(UNIT_MINUTES_PER_100_METERS) || unit.equals(UNIT_MINUTES_PER_100_YARDS)) {
            // Format pace
            String format = showUnit ? "%d:%02d %s" : "%d:%02d";
            return String.format(
                    Locale.getDefault(),
                    format,
                    (int) Math.floor(value), (int) Math.round(60 * (value - (int) Math.floor(value))),
                    getStringResourceByName(unit)
            );
        } else {
            String format = showUnit ? "%s %s" : "%s";
            // Reduce precision for certain measurements
            final String formattedValue = switch (unit) {
                case ActivitySummaryEntries.UNIT_ML_KG_MIN -> df1.format(value);
                case ActivitySummaryEntries.UNIT_BREATHS_PER_MIN -> String.valueOf(Math.round(value));
                default -> df2.format(value);
            };
            return String.format(format, formattedValue, getStringResourceByName(unit));
        }
    }

    public String formatValue(final Object rawValue, String unit) {
        return formatValue(rawValue, unit, true);
    }

    public String getStringResourceByName(String aString) {
        String packageName = BuildConfig.APPLICATION_ID;
        int resId = GBApplication.getContext().getResources().getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            //LOG.warn("SportsActivity " + "Missing string in strings:" + aString);
            return aString;
        } else {
            return GBApplication.getContext().getString(resId);
        }
    }
}
