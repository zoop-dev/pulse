package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts;

import static org.junit.Assert.assertEquals;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_CELSIUS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FAHRENHEIT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FOOT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_INCH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KILOMETERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MILE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MILE_PER_HOUR;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_100_YARDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MINUTES_PER_MILE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.WorkoutChartUnits.Quantity;
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureUnit;

public class WorkoutChartUnitsTest {
    private static final double EPS = 1e-6;

    private final WorkoutChartUnits metric =
            new WorkoutChartUnits(DistanceUnit.METRIC, TemperatureUnit.CELSIUS);
    private final WorkoutChartUnits imperial =
            new WorkoutChartUnits(DistanceUnit.IMPERIAL, TemperatureUnit.FAHRENHEIT);

    @Test
    public void metricTokens() {
        assertEquals(UNIT_KMPH, metric.token(Quantity.SPEED));
        assertEquals(UNIT_MINUTES_PER_KM, metric.token(Quantity.PACE));
        assertEquals(UNIT_MINUTES_PER_100_METERS, metric.token(Quantity.PACE_SWIM));
        assertEquals(UNIT_METERS, metric.token(Quantity.ELEVATION));
        assertEquals(UNIT_METERS, metric.token(Quantity.DEPTH));
        assertEquals(UNIT_KILOMETERS, metric.token(Quantity.DISTANCE));
        assertEquals(UNIT_MM, metric.token(Quantity.STEP_LENGTH));
        assertEquals(UNIT_CELSIUS, metric.token(Quantity.TEMPERATURE));
    }

    @Test
    public void imperialTokens() {
        assertEquals(UNIT_MILE_PER_HOUR, imperial.token(Quantity.SPEED));
        assertEquals(UNIT_MINUTES_PER_MILE, imperial.token(Quantity.PACE));
        assertEquals(UNIT_MINUTES_PER_100_YARDS, imperial.token(Quantity.PACE_SWIM));
        assertEquals(UNIT_FOOT, imperial.token(Quantity.ELEVATION));
        assertEquals(UNIT_FOOT, imperial.token(Quantity.DEPTH));
        assertEquals(UNIT_MILE, imperial.token(Quantity.DISTANCE));
        assertEquals(UNIT_INCH, imperial.token(Quantity.STEP_LENGTH));
        assertEquals(UNIT_FAHRENHEIT, imperial.token(Quantity.TEMPERATURE));
    }

    @Test
    public void metricConversionsAreIdentityExceptDistanceScale() {
        assertEquals(36.0, metric.convert(Quantity.SPEED, 10), EPS);      // m/s -> km/h
        assertEquals(100.0, metric.convert(Quantity.ELEVATION, 100), EPS); // meters kept
        assertEquals(5.0, metric.convert(Quantity.DISTANCE, 5000), EPS);   // meters -> km
        assertEquals(50.0, metric.convert(Quantity.STEP_LENGTH, 50), EPS); // mm kept
        assertEquals(20.0, metric.convert(Quantity.TEMPERATURE, 20), EPS); // °C kept
    }

    @Test
    public void imperialConversions() {
        assertEquals(22.36936, imperial.convert(Quantity.SPEED, 10), EPS);      // m/s -> mph
        assertEquals(328.084, imperial.convert(Quantity.ELEVATION, 100), EPS);  // m -> ft
        assertEquals(328.084, imperial.convert(Quantity.DEPTH, 100), EPS);
        assertEquals(3.106855, imperial.convert(Quantity.DISTANCE, 5000), EPS); // m -> mi
        assertEquals(1.0, imperial.convert(Quantity.STEP_LENGTH, 25.4), EPS);   // mm -> in
        assertEquals(68.0, imperial.convert(Quantity.TEMPERATURE, 20), EPS);    // °C -> °F
        assertEquals(32.0, imperial.convert(Quantity.TEMPERATURE, 0), EPS);
    }

    @Test
    public void temperatureIsIndependentOfDistanceUnit() {
        final WorkoutChartUnits imperialCelsius =
                new WorkoutChartUnits(DistanceUnit.IMPERIAL, TemperatureUnit.CELSIUS);
        assertEquals(UNIT_CELSIUS, imperialCelsius.token(Quantity.TEMPERATURE));
        assertEquals(20.0, imperialCelsius.convert(Quantity.TEMPERATURE, 20), EPS);
        assertEquals(UNIT_FOOT, imperialCelsius.token(Quantity.ELEVATION));

        final WorkoutChartUnits metricFahrenheit =
                new WorkoutChartUnits(DistanceUnit.METRIC, TemperatureUnit.FAHRENHEIT);
        assertEquals(UNIT_FAHRENHEIT, metricFahrenheit.token(Quantity.TEMPERATURE));
        assertEquals(68.0, metricFahrenheit.convert(Quantity.TEMPERATURE, 20), EPS);
        assertEquals(UNIT_METERS, metricFahrenheit.token(Quantity.ELEVATION));
    }

    @Test
    public void decimals() {
        assertEquals(2, metric.decimals(Quantity.SPEED));
        assertEquals(2, metric.decimals(Quantity.DISTANCE));
        assertEquals(1, metric.decimals(Quantity.STEP_LENGTH));
        assertEquals(0, metric.decimals(Quantity.ELEVATION));
        assertEquals(0, metric.decimals(Quantity.TEMPERATURE));
    }
}
