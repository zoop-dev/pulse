/*  Copyright (C) 2026 Dany Mestas

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;

/**
 * Tests for {@link WorkoutSummaryParser} using captured device data. Each base64 blob
 * is the verbatim contents of a SUMMARY .bin file pulled from a real workout, so the
 * tests pin the parser against actual byte layouts rather than fabricated examples.
 */
public class WorkoutSummaryParserTest {

    /** Treadmill v11, 24 Apr 2026, 4.48 km run (calibrated to 4.5 km), goal 4 km. */
    private static final String TREADMILL_V11_24APR =
            "ErHraQgLjQD7/+/vkf/AAD8SsetpMLjraRgHAAB/EQAAiQCVAQAAWAEAABMDAAAnEwAAWACj"
                    + "AKgAp7NeZmaGQAAAAAAoAOkBAADHBAAAPQAAAA4AAAAQAAAAugAXBwAAZmbmPwADAAAA"
                    + "AAAAAAAAoA8AAAAAAAAAAAAAAACUEQAAaAADAAAAAAAAAAAAAAAAAAAMAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAADyuEua";

    /** Treadmill v9, 21 Jun 2026 — 192 cal, 135 avg HR, 28 training load, +15 vitality. */
    private static final String TREADMILL_V9_21JUN =
            "WCA4aggJjQD//H+QDiBYIDhqhyY4aigGAACgCgAAwAByAQAAlAUAAJAMAAC0AIelZDMz8z8C"
                    + "AAAAIAAAAAAAcAAAAPwCAAB0AQAAOgEAAOgAKAYAAAAAAAABAwAAAFgCAAAsAYgTAABK"
                    + "AQAAAADIQQAAoAoAABwAAQAAAAAAAA8AAAAAAAAAAAAAAAAAAwAAh6VkA1B6ALQAhdLG"
                    + "QAisHEFYIDhqhyY4agAAAAAoBgAAKAYAAAAAAAAAAAAAwAAAAKAKAACQDAAAQwIAAHIB"
                    + "AAAAAAAAAAAAAAAAAAADAAAAAAAAAAAAAAAAAAAAAAAAAHAAAAD8AgAAdAEAADoBAAAA"
                    + "AAIBMzPzPwAAAAAAAAAADwAcACAAAAAAAAAAAAAAAAAAAAAAAaiMNkSR";

    /** Rowing v7, 20 Apr 2026 — 0.4 anaerobic effect, 71 max stroke rate. */
    private static final String ROWING_V7_20APR =
            "+l3maQgHtQD/v/N4//pd5mnwZ+Zp9QkAANYAjqRcmplZQAAAFwAAAAAAoQIAAGoFAACwAAA"
                    + "AFAEAABsBuAQAABwAAABHAAAAAAAAAAAAAABnAQAABfUJAADNzMw+AAAAAAAAAAAAAABF"
                    + "AAIAAJGg1DA=";

    /** Rowing v7, 15 Apr 2026 — 1.1 anaerobic effect. */
    private static final String ROWING_V7_15APR =
            "8srfaQgHtQD/v/N4//LK32nQ1N9p3QkAANQAkapfAABgQAAAHAAAAAAAGwUAAKwCAAD+AAA"
                    + "AAwEAABkBTAQAABoAAAApAAAAAAAAAAAAAAAAAAAAAd0JAADNzIw/AAAAAAAAAAAAAABN"
                    + "AAMAAAusxH0=";

    /** Freestyle v10, 17 Mar 2026 — frisbee workout, 105/93/7 throws low/medium/high. */
    private static final String FREESTYLE_V10_FRISBEE =
            "HIK5aQQKoQD+Bv7/wHccgrlpcJO5aVMRAABwAZG8WgAAAAAAAAAAgEAAACMAWgEAAEIEAAA"
                    + "FBwAAtAMAANUAAADoAVMRAABmZiZAACcDAAAAAAAAAACIAAMAAAAAAAAAAAAAAAAAAAAA"
                    + "aQBdAAcAAEblwNo=";

    private static ActivitySummaryData parse(final String base64) {
        final byte[] bytes = Base64.getDecoder().decode(base64);
        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(bytes);
        new WorkoutSummaryParser().parseBinaryData(summary, true);
        final String json = summary.getSummaryData();
        assertNotNull("parser should populate summaryData", json);
        return ActivitySummaryData.fromJson(json);
    }

    private static double num(final ActivitySummaryData data, final String key) {
        final Number n = data.getNumber(key, null);
        assertNotNull("missing entry: " + key, n);
        return n.doubleValue();
    }

    @Test
    public void treadmillV11_extractsRecoveryDistanceGoalCalibratedLoadVitality() {
        final ActivitySummaryData data = parse(TREADMILL_V11_24APR);

        // Pre-existing fields (baseline sanity check)
        assertEquals(4479d, num(data, ActivitySummaryEntries.DISTANCE_METERS), 0.001);
        assertEquals(4.2d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC), 0.01);
        assertEquals(167d, num(data, ActivitySummaryEntries.HR_AVG), 0.001);
        assertEquals(163d, num(data, ActivitySummaryEntries.CADENCE_AVG), 0.001);

        // New trailing-zone fields verified against the device UI.
        // The float at the prior "recoveryValue" offset is the anaerobic training effect.
        assertEquals(1.8d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC), 0.01);
        assertEquals(4000d, num(data, ActivitySummaryEntries.DISTANCE_GOAL), 0.001);
        assertEquals(4500d, num(data, ActivitySummaryEntries.DISTANCE_METERS_CALIBRATED), 0.001);
        assertEquals(104d, num(data, ActivitySummaryEntries.WORKOUT_LOAD), 0.001);
        assertEquals(12d, num(data, ActivitySummaryEntries.VITALITY_GAIN), 0.001);
    }

    @Test
    public void treadmillV9_extractsHrStepsRecoveryLoadVitality() {
        final ActivitySummaryData data = parse(TREADMILL_V9_21JUN);

        // All values verified against the on-watch UI for this workout.
        assertEquals(192d, num(data, ActivitySummaryEntries.CALORIES_BURNT), 0.001);
        assertEquals(2720d, num(data, ActivitySummaryEntries.DISTANCE_METERS), 0.001);
        assertEquals(3216d, num(data, ActivitySummaryEntries.STEPS), 0.001);
        assertEquals(135d, num(data, ActivitySummaryEntries.HR_AVG), 0.001);
        assertEquals(180d, num(data, ActivitySummaryEntries.CADENCE_MAX), 0.001);
        assertEquals(1.9d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC), 0.01);
        assertEquals(32d, num(data, ActivitySummaryEntries.RECOVERY_TIME), 0.001);
        assertEquals(28d, num(data, ActivitySummaryEntries.WORKOUT_LOAD), 0.001);
        assertEquals(15d, num(data, ActivitySummaryEntries.VITALITY_GAIN), 0.001);
    }

    @Test
    public void rowingV7_extractsAerobicAnaerobicStrokeMaxLoad() {
        final ActivitySummaryData data = parse(ROWING_V7_20APR);

        // Sanity checks
        assertEquals(142d, num(data, ActivitySummaryEntries.HR_AVG), 0.001);
        assertEquals(2549d, num(data, ActivitySummaryEntries.ACTIVE_SECONDS), 0.001);
        assertEquals(1208d, num(data, ActivitySummaryEntries.STROKES), 0.001);
        assertEquals(28d, num(data, ActivitySummaryEntries.STROKE_RATE_AVG), 0.001);

        // New fields verified against the device UI
        assertEquals(3.4d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC), 0.01);
        assertEquals(0.4d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC), 0.01);
        assertEquals(71d, num(data, ActivitySummaryEntries.STROKE_RATE_MAX), 0.001);
        assertEquals(23d, num(data, ActivitySummaryEntries.RECOVERY_TIME), 0.001);
        assertEquals(69d, num(data, ActivitySummaryEntries.WORKOUT_LOAD), 0.001);
        // Vitality_gain = 0 in this workout; XiaomiSimpleActivityParser force-displays it.
        assertEquals(0d, num(data, ActivitySummaryEntries.VITALITY_GAIN), 0.001);
    }

    @Test
    public void rowingV7_15Apr_extractsAnaerobic() {
        final ActivitySummaryData data = parse(ROWING_V7_15APR);

        assertEquals(1.1d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC), 0.01);
        assertEquals(3.5d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC), 0.01);
        assertEquals(41d, num(data, ActivitySummaryEntries.STROKE_RATE_MAX), 0.001);
    }

    @Test
    public void freestyleV10_extractsFrisbeeThrows() {
        final ActivitySummaryData data = parse(FREESTYLE_V10_FRISBEE);

        // Sanity checks
        assertEquals(2.6d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC), 0.01);
        assertEquals(4.0d, num(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC), 0.01);

        // New frisbee throw-force buckets
        assertEquals(105d, num(data, ActivitySummaryEntries.THROWS_LOW), 0.001);
        assertEquals(93d, num(data, ActivitySummaryEntries.THROWS_MEDIUM), 0.001);
        assertEquals(7d, num(data, ActivitySummaryEntries.THROWS_HIGH), 0.001);

        // ActivityKind override via XIAOMI_WORKOUT_TYPE = 807 → FRISBEE
        // (sample's BaseActivitySummary doesn't expose summary.getActivityKind() through
        // ActivitySummaryData; the override is verified indirectly by the workout-type
        // mapping in XiaomiWorkoutType.fromCode). Throws-force values are the parser-level
        // assertion.
    }
}
