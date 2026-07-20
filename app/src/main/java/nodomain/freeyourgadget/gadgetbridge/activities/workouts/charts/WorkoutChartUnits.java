/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_CELSIUS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FAHRENHEIT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KILOMETERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureUnit;
import nodomain.freeyourgadget.gadgetbridge.model.WeightUnit;

/**
 * Resolves the unit shown on a workout chart. Both the display unit token and the value conversion
 * come from {@link WorkoutValueFormatter} – the single source of truth also used by the
 * workout summary rows – so charts and summary can never disagree, and each conversion is
 * defined exactly once.
 * <p>
 * All chart data is stored metric (meters, m/s, mm, °C). Values are converted per point via
 * {@link #convert} in {@code fixedUnit} mode (no per-value roll-ups, so a continuous axis keeps one
 * unit) before plotting, and the axis/marker is labelled with {@link #token}. Pace is inverse (min
 * per distance) and keeps its own value formatter; only its {@link #token} is taken from here.
 * Temperature follows its own preference ({@link TemperatureUnit}) rather than the distance unit and
 * is therefore the one conversion not delegated to the distance authority.
 */
public class WorkoutChartUnits {
    public enum Quantity {
        SPEED,        // data m/s
        PACE,         // inverse, data m/s -> min/distance (value formatted elsewhere)
        PACE_SWIM,    // inverse
        ELEVATION,    // data meters
        DEPTH,        // data meters
        DISTANCE,     // data meters (shown as km/mi, which read better than m/ft on a distance axis)
        STEP_LENGTH,  // data mm
        TEMPERATURE,  // data °C
    }

    private final WorkoutValueFormatter authority;
    private final boolean fahrenheit;

    public WorkoutChartUnits(final DistanceUnit distanceUnit, final TemperatureUnit temperatureUnit) {
        // Weight is irrelevant here; nautical stays off and the activity is unknown until the
        // activity-specific conventions are wired in, keeping convert() on its generic path.
        this.authority = new WorkoutValueFormatter(ActivityKind.UNKNOWN, distanceUnit, WeightUnit.KILOGRAM, false);
        this.fahrenheit = temperatureUnit == TemperatureUnit.FAHRENHEIT;
    }

    /** Display unit token for the axis/marker label. */
    public String token(final Quantity quantity) {
        switch (quantity) {
            case SPEED:
                return authority.convert(1, UNIT_METERS_PER_SECOND, true).unit;
            case PACE:
                return authority.convert(1, UNIT_SECONDS_PER_KM, true).unit;
            case PACE_SWIM:
                return authority.convert(1, UNIT_SECONDS_PER_100_METERS, true).unit;
            case ELEVATION:
            case DEPTH:
                return authority.convert(1, UNIT_METERS, true).unit;
            case DISTANCE:
                return authority.convert(1, UNIT_KILOMETERS, true).unit;
            case STEP_LENGTH:
                return authority.convert(1, UNIT_MM, true).unit;
            case TEMPERATURE:
                return fahrenheit ? UNIT_FAHRENHEIT : UNIT_CELSIUS;
            default:
                throw new IllegalArgumentException("Unknown chart quantity: " + quantity);
        }
    }

    /**
     * Converts one plotted metric value to the display unit. Not meaningful for the inverse PACE
     * quantities, whose values are produced by their own formatter.
     */
    public double convert(final Quantity quantity, final double value) {
        switch (quantity) {
            case SPEED:
                return authority.convert(value, UNIT_METERS_PER_SECOND, true).value;
            case ELEVATION:
            case DEPTH:
                return authority.convert(value, UNIT_METERS, true).value;
            case DISTANCE:
                return authority.convert(value / 1000.0, UNIT_KILOMETERS, true).value;
            case STEP_LENGTH:
                return authority.convert(value, UNIT_MM, true).value;
            case TEMPERATURE:
                return fahrenheit ? value * 1.8 + 32 : value;
            default:
                return value;
        }
    }

    /** Decimal places for the axis/marker of a linear quantity. */
    public int decimals(final Quantity quantity) {
        switch (quantity) {
            case SPEED:
            case DISTANCE:
                return 2;
            case STEP_LENGTH:
                return 1;
            default:
                return 0;
        }
    }
}
