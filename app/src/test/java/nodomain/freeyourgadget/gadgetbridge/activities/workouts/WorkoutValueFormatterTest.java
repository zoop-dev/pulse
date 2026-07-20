package nodomain.freeyourgadget.gadgetbridge.activities.workouts;

import static org.junit.Assert.assertEquals;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_FOOT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KILOMETERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KMPH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MILE;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_MM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_100_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_500_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_M;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit;
import nodomain.freeyourgadget.gadgetbridge.model.WeightUnit;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

/**
 * Exhaustive per-unit checks for {@link WorkoutValueFormatter}: every distance/speed/pace unit
 * is asserted in both metric and imperial, including the rowing 500 m pace which stays metric.
 */
public class WorkoutValueFormatterTest extends TestBase {
    private WorkoutValueFormatter metric;
    private WorkoutValueFormatter imperial;
    private Locale previousLocale;

    @Before
    public void setUpFormatters() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        metric = new WorkoutValueFormatter(ActivityKind.UNKNOWN, DistanceUnit.METRIC, WeightUnit.KILOGRAM, false);
        imperial = new WorkoutValueFormatter(ActivityKind.UNKNOWN, DistanceUnit.IMPERIAL, WeightUnit.KILOGRAM, false);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(previousLocale);
    }

    private String metric(final Object value, final String unit) {
        return metric.formatValue(value, unit, true);
    }

    private String imperial(final Object value, final String unit) {
        return imperial.formatValue(value, unit, true);
    }

    // --- Pace (min:sec) ---

    @Test
    public void paceSecondsPerKm() {
        assertEquals("6:00 min/km", metric(360, UNIT_SECONDS_PER_KM));
        assertEquals("9:39 min/mi", imperial(360, UNIT_SECONDS_PER_KM));
    }

    @Test
    public void paceSecondsPerMeter() {
        assertEquals("5:00 min/km", metric(0.3, UNIT_SECONDS_PER_M));
        assertEquals("8:03 min/mi", imperial(0.3, UNIT_SECONDS_PER_M));
    }

    @Test
    public void paceSecondsPer100Meters() {
        assertEquals("1:30 min/100m", metric(90, UNIT_SECONDS_PER_100_METERS));
        assertEquals("1:22 min/100yd", imperial(90, UNIT_SECONDS_PER_100_METERS));
    }

    @Test
    public void paceSecondsPer500MetersStaysMetric() {
        // rowing 500 m pace is conventionally kept metric regardless of the imperial setting
        assertEquals("2:00 min/500m", metric(120, UNIT_SECONDS_PER_500_METERS));
        assertEquals("2:00 min/500m", imperial(120, UNIT_SECONDS_PER_500_METERS));
    }

    // --- Speed ---

    @Test
    public void speedMetersPerSecond() {
        assertEquals("36 km/h", metric(10, UNIT_METERS_PER_SECOND));
        assertEquals("22.37 mi/h", imperial(10, UNIT_METERS_PER_SECOND));
    }

    @Test
    public void speedKilometersPerHour() {
        assertEquals("10 km/h", metric(10, UNIT_KMPH));
        assertEquals("6.21 mi/h", imperial(10, UNIT_KMPH));
    }

    // --- Distance ---

    @Test
    public void distanceKilometers() {
        assertEquals("10 km", metric(10, UNIT_KILOMETERS));
        assertEquals("6.21 mi", imperial(10, UNIT_KILOMETERS));
    }

    @Test
    public void distanceMetersShort() {
        assertEquals("100 m", metric(100, UNIT_METERS));
        assertEquals("328.08 ft", imperial(100, UNIT_METERS));
    }

    @Test
    public void distanceMetersLong() {
        // metric rolls up to km above 2 km; imperial rolls up to miles above 6000 ft
        assertEquals("5 km", metric(5000, UNIT_METERS));
        assertEquals("3.11 mi", imperial(5000, UNIT_METERS));
    }

    @Test
    public void stepLengthMillimeters() {
        assertEquals("50 mm", metric(50, UNIT_MM));
        // imperial now shows inches (also fixes the summary step-length gap in Codeberg #6185)
        assertEquals("1.97 in", imperial(50, UNIT_MM));
    }

    // --- convert(): the shared authority also used by the charts ---

    @Test
    public void convertFixedUnitSkipsMagnitudeRollups() {
        // non-fixed rolls a long distance up to miles; fixed keeps feet for a stable chart axis
        WorkoutValueFormatter.Converted auto = imperial.convert(5000, UNIT_METERS, false);
        assertEquals(UNIT_MILE, auto.unit);
        assertEquals(3.106855, auto.value, 1e-4);

        WorkoutValueFormatter.Converted fixed = imperial.convert(5000, UNIT_METERS, true);
        assertEquals(UNIT_FOOT, fixed.unit);
        assertEquals(16404.2, fixed.value, 1e-1);

        // metric fixed keeps meters instead of rolling up to km
        assertEquals(UNIT_METERS, metric.convert(5000, UNIT_METERS, true).unit);
        assertEquals(5000.0, metric.convert(5000, UNIT_METERS, true).value, 1e-6);
    }
}
