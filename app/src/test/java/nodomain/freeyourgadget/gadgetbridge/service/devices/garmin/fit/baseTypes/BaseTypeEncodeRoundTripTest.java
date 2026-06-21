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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Round-trip tests for the FIT base-type encode path.
 *
 * <p>Background: commit {@code f2f6536ea} (Aug 2024) introduced the spec-correct
 * {@code stored = (physical + offset) * scale} formula in {@code BaseTypeShort/Int/Byte
 * .encode}, but kept an {@code intValue()} / {@code longValue()} cast that truncated
 * fractional {@code Float} / {@code Double} inputs before applying the scale multiplier.
 * Commit {@code fd1e81ff6} (Aug 2024) fixed the symmetric truncation on the decode path
 * but missed the encode side. The result: every fractional value passed to a scaled
 * UINT8/16/32/64 field was silently rounded toward zero on disk.
 *
 * <p>For example, on a UINT16 scale=1000 field (FIT {@code session.avg_speed}):
 * {@code setAvgSpeed(2.5f)} → encoded {@code (int)2.5 * 1000 = 2000} → decoded {@code 2.0}
 * instead of {@code 2.5}. {@code setAvgSpeed(0.778f)} → encoded {@code 0} → decoded {@code 0}.
 *
 * <p>This test class encodes a fractional value, decodes it back through the same
 * BaseType, and asserts the value survives the round-trip. Each assertion fails
 * deterministically against the pre-fix encoder (the decoded value is the truncated
 * integer rather than the original fractional value) and passes against the fixed
 * encoder. The tests therefore double as regression guards.
 */
public class BaseTypeEncodeRoundTripTest {

    private static double roundTrip(final BaseType type, final Object physical,
                                    final double scale, final int offset) {
        final ByteBuffer enc = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        type.encode(enc, physical, scale, offset);
        enc.flip();
        final Object decoded = type.decode(enc, scale, offset);
        return ((Number) decoded).doubleValue();
    }

    // ---------------- UINT16 (BaseTypeShort) ----------------

    @Test
    public void uint16_avgSpeedScale1000_preservesHalfMs() {
        // Real bug-bait: FIT session.avg_speed (UINT16 scale=1000 unit=m/s).
        // Pre-fix encoder: (int)2.5 * 1000 = 2000 stored → 2.0 decoded.
        assertEquals(2.5, roundTrip(BaseType.UINT16, 2.5f, 1000, 0), 0.0005);
    }

    @Test
    public void uint16_avgSpeedScale1000_preservesSlowSubMeterPerSecond() {
        // Pre-fix: (int)0.778 * 1000 = 0 → decoded 0.0 (the running-pace bug).
        assertEquals(0.778, roundTrip(BaseType.UINT16, 0.778f, 1000, 0), 0.001);
    }

    @Test
    public void uint16_avgStrokeDistanceScale100_preservesQuarterMeter() {
        // FIT session.avg_stroke_distance (UINT16 scale=100 unit=m).
        // Pre-fix: (int)2.5 * 100 = 200 → decoded 2.0.
        assertEquals(2.5, roundTrip(BaseType.UINT16, 2.5f, 100, 0), 0.005);
    }

    @Test
    public void uint16_altitudeScale5Offset500_preservesHalfMeter() {
        // FIT record.altitude (UINT16 scale=5 offset=500).
        // Encoder: (123.5 + 500) * 5 = 3117.5 → stored 3117. Pre-fix: ((int)123.5+500)*5 = 3115.
        // Decoded: 3117/5 - 500 = 123.4. Pre-fix: 3115/5 - 500 = 123.
        assertEquals(123.4, roundTrip(BaseType.UINT16, 123.5, 5, 500), 0.05);
    }

    @Test
    public void uint16_integerInputUnchanged() {
        // Integer/Long inputs should be unaffected by the doubleValue() change.
        assertEquals(1500.0, roundTrip(BaseType.UINT16, 1500, 1, 0), 0.0001);
        assertEquals(2.0, roundTrip(BaseType.UINT16, 2, 1000, 0), 0.0001);
    }

    // ---------------- UINT32 (BaseTypeInt) ----------------

    @Test
    public void uint32_speedScale1000_preservesHalfMs() {
        // FIT enhanced_avg_speed (UINT32 scale=1000 unit=m/s).
        // Pre-fix: (long)3.7 * 1000 = 3000 → 3.0.
        assertEquals(3.7, roundTrip(BaseType.UINT32, 3.7f, 1000, 0), 0.0005);
    }

    @Test
    public void uint32_distanceScale100_preservesHalfMeter() {
        // FIT lap.total_distance (UINT32 scale=100 unit=m).
        // Pre-fix: (long)1234.5 * 100 = 123400 → 1234.0. Fix: 123450 → 1234.5.
        assertEquals(1234.5, roundTrip(BaseType.UINT32, 1234.5, 100, 0), 0.005);
    }

    @Test
    public void uint32_integerInputUnchanged() {
        assertEquals(38600.0, roundTrip(BaseType.UINT32, 38600L, 1, 0), 0.0001);
    }

    // ---------------- UINT8 (BaseTypeByte) ----------------

    @Test
    public void uint8_scale10_preservesTenth() {
        // FIT pct fields (UINT8 scale=10 unit=%).
        // Pre-fix: (int)5.5 * 10 = 50 → 5.0. Fix: 55 → 5.5.
        assertEquals(5.5, roundTrip(BaseType.UINT8, 5.5f, 10, 0), 0.05);
    }

    @Test
    public void uint8_integerInputUnchanged() {
        assertEquals(75.0, roundTrip(BaseType.UINT8, 75, 1, 0), 0.0001);
    }

    // ---------------- SINT16 ----------------

    @Test
    public void sint16_negativeFractional_preserved() {
        // Negative fractional. Pre-fix: (int)-1.5 = -1, *100 = -100 → -1.0. Fix: -150 → -1.5.
        assertEquals(-1.5, roundTrip(BaseType.SINT16, -1.5f, 100, 0), 0.005);
    }

    // ---------------- UINT64 (BaseTypeLong) ----------------
    // BaseTypeLong shares the same encoder pattern. No production fields with non-1
    // scale currently use it, so this test guards against future regressions only.

    @Test
    public void uint64_integerInputUnchanged() {
        assertEquals(123456789L, ((Number) roundTrip(BaseType.UINT64, 123456789L, 1, 0)).longValue());
    }
}
