/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.fitcloud.ak102;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class Ak102SyncParserTest {

    // Buffer builders --------------------------------------------------------

    // section 5.2 packed minute-resolution timestamp (inverse of decodeMinuteTs).
    private static byte[] packMinuteTs(final int year, final int month, final int day,
                                       final int minuteOfDay) {
        return new byte[]{
                (byte) (((year - 2000) << 1) | ((month >> 3) & 1)),
                (byte) (((month & 7) << 5) | (day & 0x1F)),
                (byte) ((minuteOfDay >> 8) & 0xFF),
                (byte) (minuteOfDay & 0xFF),
        };
    }

    // section 5.3 packed second-resolution timestamp (inverse of decodeSecondTs).
    private static byte[] packSecondTs(final int year, final int month, final int day,
                                       final int hour, final int minute, final int second) {
        return new byte[]{
                (byte) (((year - 2000) << 2) | (month >> 2)),
                (byte) (((month & 3) << 6) | (day << 1) | (hour >> 4)),
                (byte) (((hour & 0xF) << 4) | (minute >> 2)),
                (byte) (((minute & 3) << 6) | second),
        };
    }

    // Expected unix millis for a local wall-clock time, as the decoders compute it.
    private static long millis(final int year, final int month, final int day,
                               final int hour, final int minute, final int second) {
        final Calendar c = GregorianCalendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        return c.getTimeInMillis();
    }

    private static void writeU16(final ByteArrayOutputStream out, final int value) {
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeU32(final ByteArrayOutputStream out, final int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    // 8-byte block header: [count u16][minute-ts 4B][interval u16].
    private static void writeHeader(final ByteArrayOutputStream out, final int count,
                                    final byte[] minuteTs, final int interval) {
        writeU16(out, count);
        out.write(minuteTs, 0, 4);
        writeU16(out, interval);
    }

    // Timestamp decoders -----------------------------------------------------

    @Test
    public void testDecodeMinuteTs() {
        final byte[] packed = packMinuteTs(2026, 7, 7, 23 * 60 + 15);
        assertEquals(millis(2026, 7, 7, 23, 15, 0), Ak102SyncParser.decodeMinuteTs(packed, 0));
    }

    @Test
    public void testDecodeSecondTs() {
        final byte[] packed = packSecondTs(2026, 12, 31, 23, 59, 58);
        assertEquals(millis(2026, 12, 31, 23, 59, 58), Ak102SyncParser.decodeSecondTs(packed, 0));
    }

    // Steps ------------------------------------------------------------------

    @Test
    public void testParseStepsSkipsEmptyBuckets() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 3, packMinuteTs(2026, 7, 7, 10 * 60), 5);
        writeU16(buf, 100);
        writeU16(buf, 0); // empty bucket, dropped
        writeU16(buf, 200);

        final List<Ak102SyncParser.ActivityPoint> points =
                Ak102SyncParser.parseSteps(buf.toByteArray(), false, false);

        assertEquals(2, points.size());
        assertEquals(100, points.get(0).steps);
        assertEquals(200, points.get(1).steps);
        assertEquals(ActivityKind.ACTIVITY.getCode(), points.get(0).rawKind);
        final long base = millis(2026, 7, 7, 10, 0, 0);
        assertEquals(base / 1000L, points.get(0).timestamp);
        // Third item: two 5-minute intervals after the base.
        assertEquals((base + 2 * 5 * 60000L) / 1000L, points.get(1).timestamp);
    }

    @Test
    public void testParseStepsWithExtraScalesDistanceAndCalories() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 1, packMinuteTs(2026, 7, 7, 10 * 60), 5);
        writeU16(buf, 1000); // steps
        writeU16(buf, 2500); // raw distance -> 25 m
        writeU16(buf, 3000); // raw calories -> 3 kcal

        final List<Ak102SyncParser.ActivityPoint> points =
                Ak102SyncParser.parseSteps(buf.toByteArray(), true, false);

        assertEquals(1, points.size());
        assertEquals(1000, points.get(0).steps);
        assertEquals(25, points.get(0).distanceCm);
        assertEquals(3, points.get(0).activeCalories);
    }

    // Sleep ------------------------------------------------------------------

    @Test
    public void testParseSleepOldProtocolOneSamplePerInterval() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 4, packMinuteTs(2026, 7, 7, 2 * 60), 1);
        buf.write(1); // deep
        buf.write(2); // light
        buf.write(3); // awake
        buf.write(0); // invalid in the old protocol, dropped

        final List<Ak102SyncParser.ActivityPoint> points =
                Ak102SyncParser.parseSleep(buf.toByteArray(), false, false);

        assertEquals(3, points.size());
        assertEquals(ActivityKind.DEEP_SLEEP.getCode(), points.get(0).rawKind);
        assertEquals(ActivityKind.LIGHT_SLEEP.getCode(), points.get(1).rawKind);
        assertEquals(ActivityKind.AWAKE_SLEEP.getCode(), points.get(2).rawKind);
        assertEquals(millis(2026, 7, 7, 2, 1, 0) / 1000L, points.get(1).timestamp);
    }

    /*
     * New-protocol items are state-change markers whose status describes the
     * interval ENDING at their timestamp. Each interval must be expanded into
     * per-minute samples; the first marker only closes the unknown interval
     * before recording started and emits nothing.
     */
    @Test
    public void testParseSleepNewProtocolExpandsPrecedingIntervals() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 4, packMinuteTs(2026, 7, 7, 23 * 60), 1);
        // Onset at 23:00; deep until 23:50, light until 00:49, awake until 00:50.
        buf.write(packMinuteTs(2026, 7, 7, 23 * 60), 0, 4);
        buf.write(3);
        buf.write(packMinuteTs(2026, 7, 7, 23 * 60 + 50), 0, 4);
        buf.write(1);
        buf.write(packMinuteTs(2026, 7, 8, 49), 0, 4);
        buf.write(2);
        buf.write(packMinuteTs(2026, 7, 8, 50), 0, 4);
        buf.write(3);

        final List<Ak102SyncParser.ActivityPoint> points =
                Ak102SyncParser.parseSleep(buf.toByteArray(), true, false);

        int deep = 0;
        int light = 0;
        int awake = 0;
        for (final Ak102SyncParser.ActivityPoint p : points) {
            if (p.rawKind == ActivityKind.DEEP_SLEEP.getCode()) {
                deep++;
            } else if (p.rawKind == ActivityKind.LIGHT_SLEEP.getCode()) {
                light++;
            } else if (p.rawKind == ActivityKind.AWAKE_SLEEP.getCode()) {
                awake++;
            }
        }
        assertEquals(50, deep);
        assertEquals(59, light);
        assertEquals(1, awake);
        assertEquals(110, points.size());
        assertEquals(millis(2026, 7, 7, 23, 0, 0) / 1000L, points.get(0).timestamp);
        assertEquals(millis(2026, 7, 8, 0, 49, 0) / 1000L,
                points.get(points.size() - 1).timestamp);
    }

    @Test
    public void testParseSleepNewProtocolSkipsImplausibleIntervals() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 4, packMinuteTs(2026, 7, 7, 8 * 60), 1);
        // Onset at 08:00, 10 min deep, then a 90 min awake stretch (between
        // sessions, dropped) and a 9h20m gap to the next marker (implausible,
        // dropped).
        buf.write(packMinuteTs(2026, 7, 7, 8 * 60), 0, 4);
        buf.write(3);
        buf.write(packMinuteTs(2026, 7, 7, 8 * 60 + 10), 0, 4);
        buf.write(1);
        buf.write(packMinuteTs(2026, 7, 7, 9 * 60 + 40), 0, 4);
        buf.write(3);
        buf.write(packMinuteTs(2026, 7, 7, 19 * 60), 0, 4);
        buf.write(2);

        final List<Ak102SyncParser.ActivityPoint> points =
                Ak102SyncParser.parseSleep(buf.toByteArray(), true, false);

        assertEquals(10, points.size());
        for (final Ak102SyncParser.ActivityPoint p : points) {
            assertEquals(ActivityKind.DEEP_SLEEP.getCode(), p.rawKind);
        }
    }

    // GPS --------------------------------------------------------------------

    @Test
    public void testParseGpsFieldOrderAndZeroSkip() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 3, packMinuteTs(2026, 7, 7, 9 * 60), 0);
        // Sport-type string: [len][section 5.3 session start 4B][sport id 2B].
        buf.write(6);
        buf.write(packSecondTs(2026, 7, 7, 9, 0, 30), 0, 4);
        writeU16(buf, 8);
        // Point 1: valid. Longitude comes first on the wire.
        writeU16(buf, 0);          // time offset s
        writeU32(buf, 2054000);    // longitude 20.54
        writeU32(buf, 4437000);    // latitude 44.37
        writeU16(buf, 120);        // altitude m
        buf.write(9);              // satellites
        buf.write(0);              // restart flag
        // Point 2: zero latitude, dropped.
        writeU16(buf, 5);
        writeU32(buf, 2054100);
        writeU32(buf, 0);
        writeU16(buf, 121);
        buf.write(9);
        buf.write(0);
        // Point 3: valid, first point after a pause (restart flag set).
        writeU16(buf, 10);
        writeU32(buf, 2054200);
        writeU32(buf, 4437100);
        writeU16(buf, 125);
        buf.write(9);
        buf.write(1);

        final List<Ak102SyncParser.GpsTrack> tracks =
                Ak102SyncParser.parseGps(buf.toByteArray());

        assertEquals(1, tracks.size());
        final Ak102SyncParser.GpsTrack track = tracks.get(0);
        final long base = millis(2026, 7, 7, 9, 0, 30);
        assertEquals(base, track.baseMillis);
        assertEquals(2, track.points.size());
        assertEquals(20.54, track.points.get(0).longitude, 0.000001);
        assertEquals(44.37, track.points.get(0).latitude, 0.000001);
        assertEquals(120.0, track.points.get(0).altitude, 0.000001);
        assertEquals(base, track.points.get(0).timestampMillis);
        assertEquals(base + 10_000L, track.points.get(1).timestampMillis);
        assertEquals(false, track.points.get(0).restart);
        assertEquals(true, track.points.get(1).restart);
    }

    // Sport ------------------------------------------------------------------

    @Test
    public void testParseSportCumulativeDurationAndSums() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeHeader(buf, 3, packMinuteTs(2026, 7, 7, 18 * 60), 1000);
        // Sport-type string carries the true (second-resolution) session start.
        buf.write(6);
        buf.write(packSecondTs(2026, 7, 7, 18, 0, 30), 0, 4);
        writeU16(buf, 13);
        // Three records of 12 + 1 (heart rate) bytes.
        final int[][] records = {
                // sportId, cumulative duration s, steps, distance m, calories cal, hr
                {13, 60, 100, 200, 500, 120},
                {13, 120, 100, 200, 500, 0},
                {13, 180, 100, 200, 1000, 130},
        };
        for (final int[] r : records) {
            writeU16(buf, r[0]);
            writeU16(buf, r[1]);
            writeU16(buf, r[2]);
            writeU16(buf, r[3]);
            writeU16(buf, r[4]);
            writeU16(buf, 0); // padding
            buf.write(r[5]);
        }

        final List<Ak102SyncParser.Workout> workouts =
                Ak102SyncParser.parseSport(buf.toByteArray(), 1, true);

        assertEquals(1, workouts.size());
        final Ak102SyncParser.Workout w = workouts.get(0);
        assertEquals(13, w.sportId);
        assertEquals(millis(2026, 7, 7, 18, 0, 30), w.startMillis);
        assertEquals(180, w.durationSeconds); // cumulative, not summed
        assertEquals(300, w.steps);
        assertEquals(600, w.distanceMeters);
        assertEquals(2, w.caloriesKcal); // (500 + 500 + 1000) / 1000
        assertEquals(125, w.avgHeartRate); // zero readings excluded
        // Non-zero per-record HR becomes timestamped points, one header
        // interval (1000 ms here) apart.
        assertEquals(2, w.hrPoints.size());
        assertEquals(120, w.hrPoints.get(0).heartRate);
        assertEquals(millis(2026, 7, 7, 18, 0, 30) / 1000L, w.hrPoints.get(0).timestamp);
        assertEquals(130, w.hrPoints.get(1).heartRate);
        assertEquals((millis(2026, 7, 7, 18, 0, 30) + 2000L) / 1000L,
                w.hrPoints.get(1).timestamp);
        // Every record yields a cadence point: step delta scaled to steps/min
        // (100 steps per 1000 ms record = 6000 spm).
        assertEquals(3, w.cadencePoints.size());
        assertEquals(6000, w.cadencePoints.get(0).value);
        assertEquals(millis(2026, 7, 7, 18, 0, 30) + 1000L,
                w.cadencePoints.get(1).timestampMillis);
    }

    @Test
    public void testNearestValuePicksClosestAndAllowsZero() {
        final List<Ak102SyncParser.ValuePoint> points = new java.util.ArrayList<>();
        points.add(new Ak102SyncParser.ValuePoint(100_000L, 0));
        points.add(new Ak102SyncParser.ValuePoint(160_000L, 90));

        assertEquals(0, Ak102SyncParser.nearestValue(points, 110_000L, 90_000L));
        assertEquals(90, Ak102SyncParser.nearestValue(points, 140_000L, 90_000L));
        assertEquals(-1, Ak102SyncParser.nearestValue(points, 400_000L, 90_000L));
    }

    @Test
    public void testNearestHeartRatePicksClosestWithinTolerance() {
        final List<Ak102SyncParser.ActivityPoint> hr = new java.util.ArrayList<>();
        final Ak102SyncParser.ActivityPoint a = new Ak102SyncParser.ActivityPoint(100);
        a.heartRate = 120;
        final Ak102SyncParser.ActivityPoint b = new Ak102SyncParser.ActivityPoint(160);
        b.heartRate = 130;
        final Ak102SyncParser.ActivityPoint zero = new Ak102SyncParser.ActivityPoint(112);
        zero.heartRate = 0; // invalid readings are never returned
        hr.add(a);
        hr.add(zero);
        hr.add(b);

        assertEquals(120, Ak102SyncParser.nearestHeartRate(hr, 110, 90));
        assertEquals(130, Ak102SyncParser.nearestHeartRate(hr, 131, 90));
        assertEquals(130, Ak102SyncParser.nearestHeartRate(hr, 240, 90));
        assertEquals(0, Ak102SyncParser.nearestHeartRate(hr, 400, 90));
    }

    // Today total ------------------------------------------------------------

    @Test
    public void testParseTodayTotal() {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeU32(buf, 12345); // steps
        writeU32(buf, 6789);  // distance m
        writeU32(buf, 222);   // calories
        writeU32(buf, 0);     // deep sleep
        writeU32(buf, 0);     // light sleep
        writeU32(buf, 77);    // heart rate

        final int[] totals = Ak102SyncParser.parseTodayTotal(buf.toByteArray());

        assertEquals(12345, totals[0]);
        assertEquals(6789, totals[1]);
        assertEquals(222, totals[2]);
        assertEquals(77, totals[3]);
    }

    @Test
    public void testParseTodayTotalRejectsShortBuffer() {
        assertNull(Ak102SyncParser.parseTodayTotal(new byte[8]));
    }
}
