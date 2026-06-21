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
package nodomain.freeyourgadget.gadgetbridge.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;

/**
 * Covers the FitExporter Part A (avg_speed fallback) and Part B (rowing distance synth)
 * paths added to address the Endurain rowing/running pace bug.
 *
 * <p>Tests build synthetic BaseActivitySummary + ActivityTrack inputs, run FitExporter
 * against a temp file, then decode the FIT bytes via FitFile.parseIncoming and assert
 * Session/Lap fields. No Android context, DB, or device is involved.
 */
public class FitExporterPaceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** Indoor rowing (erg) session: 9 segments (4 active 0x81 of ~256 strokes each, 5 rest
     *  0x82 of ~5 strokes), totalling 1208 strokes / 2550 s — matches the user's 20-Apr
     *  workout. ActivityKind.ROWING_MACHINE → FIT (sport=15, sub_sport=14) → indoor
     *  6.0 m/stroke default. */
    @Test
    public void indoorRowingSession_synthesizesDistanceCyclesStrokeDistanceAvgSpeed() throws Exception {
        final long start = 1776705018L;
        final long elapsed = 2550L;
        final BaseActivitySummary summary = newSummary(start, elapsed, ActivityKind.ROWING_MACHINE);
        final ActivitySummaryData data = new ActivitySummaryData();
        // STROKES is normally populated by WorkoutSummaryParser.getRowingParser; mirror that.
        data.add(ActivitySummaryEntries.STROKES, 1208, ActivitySummaryEntries.UNIT_STROKES);
        data.add(ActivitySummaryEntries.HR_AVG, 142, ActivitySummaryEntries.UNIT_BPM);
        data.add(ActivitySummaryEntries.CALORIES_BURNT, 214, ActivitySummaryEntries.UNIT_KCAL);

        final ActivityTrack track = newRowingTrack(start);

        final File out = tmp.newFile("rowing-indoor.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);
        final List<FitLap> laps = laps(fit);

        // ---- Session expectations ----
        assertNotNull("session.totalDistance", session.getTotalDistance());
        // 1208 strokes * 6.0 m/stroke = 7248 m. Getter returns raw meters (codec applies scale=100).
        assertEquals(7248.0, session.getTotalDistance(), 0.5);
        assertEquals(Long.valueOf(1208), session.getTotalCycles());
        assertNotNull("session.avgStrokeDistance", session.getAvgStrokeDistance());
        assertEquals(6.0f, session.getAvgStrokeDistance(), 0.01f);
        assertNotNull("session.avgSpeed", session.getAvgSpeed());
        // 7248 / 2550 ≈ 2.842 m/s
        assertEquals(2.842f, session.getAvgSpeed(), 0.005f);
        // ActivityKind.ROWING_MACHINE → reverse-lookup picks ROW_INDOOR(15,14) — the
        // modern Garmin code — over the legacy FITNESS_EQUIPMENT/INDOOR_ROWING(4,14).
        // FitExporter.isRowingSport accepts both forms for the stroke→distance synth.
        assertEquals(Integer.valueOf(15), session.getSport());
        assertEquals(Integer.valueOf(14), session.getSubSport());

        // ---- Lap expectations: every active rowing lap has distance/cycles/speed set ----
        // Both FitLap.getTotalDistance() and FitSession.getTotalDistance() return raw
        // meters (codec applies scale=100 on decode).
        double sumLapDistanceMeters = 0;
        for (final FitLap lap : laps) {
            assertNotNull("lap distance", lap.getTotalDistance());
            sumLapDistanceMeters += lap.getTotalDistance();
            assertNotNull("lap cycles", lap.getTotalCycles());
            assertNotNull("lap avgStrokeDistance", lap.getAvgStrokeDistance());
            // FIT lap.avg_stroke_distance is Integer cm-scaled (600 = 6.0 m)
            assertEquals(600, lap.getAvgStrokeDistance().intValue());
            // avg_speed must be set on every lap including rest laps (≈0 there).
            assertNotNull("lap avgSpeed", lap.getAvgSpeed());
        }
        // Sum of lap distances should equal session distance.
        assertEquals(7248.0, sumLapDistanceMeters, 0.5);
    }

    /** Outdoor (on-water) rowing uses a longer 9.0 m/stroke default. Same 1208/2550 input
     *  pinned to the ROWING (15, 0) FIT mapping to lock the indoor-vs-outdoor split. */
    @Test
    public void outdoorRowingSession_usesLongerStrokeLengthDefault() throws Exception {
        final long start = 1776705018L;
        final long elapsed = 2550L;
        final BaseActivitySummary summary = newSummary(start, elapsed, ActivityKind.ROWING);
        final ActivitySummaryData data = new ActivitySummaryData();
        data.add(ActivitySummaryEntries.STROKES, 1208, ActivitySummaryEntries.UNIT_STROKES);

        final ActivityTrack track = newRowingTrack(start);

        final File out = tmp.newFile("rowing-outdoor.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);
        final List<FitLap> laps = laps(fit);

        // 1208 * 9.0 = 10872 m. Getter returns raw meters.
        assertNotNull("session.totalDistance", session.getTotalDistance());
        assertEquals(10872.0, session.getTotalDistance(), 0.5);
        assertEquals(9.0f, session.getAvgStrokeDistance(), 0.01f);
        // 10872 / 2550 ≈ 4.264 m/s
        assertEquals(4.264f, session.getAvgSpeed(), 0.005f);
        assertEquals(Integer.valueOf(15), session.getSport());
        assertEquals(Integer.valueOf(0), session.getSubSport());

        for (final FitLap lap : laps) {
            assertNotNull("lap avgStrokeDistance", lap.getAvgStrokeDistance());
            assertEquals(900, lap.getAvgStrokeDistance().intValue());
        }
    }

    /**
     * Outdoor running with measured distance but no per-record speed (recorded summary
     * SPEED_AVG=0). After fix, both lap and session avg_speed are derived from
     * distance/elapsed so importers can show pace.
     */
    @Test
    public void outdoorRunningSession_derivesAvgSpeedFromDistanceAndTime() throws Exception {
        final long start = 1777747544L;
        final long elapsed = 496L;
        final BaseActivitySummary summary = newSummary(start, elapsed, ActivityKind.OUTDOOR_RUNNING);
        final ActivitySummaryData data = new ActivitySummaryData();
        data.add(ActivitySummaryEntries.DISTANCE_METERS, 386, ActivitySummaryEntries.UNIT_METERS);
        // Watch reported SPEED_AVG=0 for the interval session — fallback should kick in.
        data.add(ActivitySummaryEntries.SPEED_AVG, 0, ActivitySummaryEntries.UNIT_KMPH);

        final ActivityTrack track = singleSegmentTrack(start, elapsed);

        final File out = tmp.newFile("running.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);
        final List<FitLap> laps = laps(fit);

        // 386 / 496 ≈ 0.7782 m/s
        assertNotNull("session.avgSpeed", session.getAvgSpeed());
        assertEquals(0.778f, session.getAvgSpeed(), 0.005f);
        // Sport remapped via the OUTDOOR_RUNNING alias added earlier to GarminSport.
        assertEquals(Integer.valueOf(1), session.getSport());

        // Single-lap fallback path → exactly one lap with the same derivation.
        assertEquals(1, laps.size());
        final FitLap lap = laps.get(0);
        assertNotNull("lap.avgSpeed", lap.getAvgSpeed());
        assertEquals(0.778f, lap.getAvgSpeed(), 0.005f);
    }

    /**
     * Treadmill session that already has a non-zero summary SPEED_AVG (per-record speed
     * existed): the fallback must NOT overwrite it.
     */
    @Test
    public void treadmillSession_keepsExistingNonZeroAvgSpeed() throws Exception {
        final long start = 1777656743L;
        final long elapsed = 2004L;
        final BaseActivitySummary summary = newSummary(start, elapsed, ActivityKind.TREADMILL);
        final ActivitySummaryData data = new ActivitySummaryData();
        data.add(ActivitySummaryEntries.DISTANCE_METERS, 5000, ActivitySummaryEntries.UNIT_METERS);
        // 9 km/h — matches the 30-Apr treadmill workout's per-record speed avg.
        data.add(ActivitySummaryEntries.SPEED_AVG, 9.0, ActivitySummaryEntries.UNIT_KMPH);

        final ActivityTrack track = singleSegmentTrack(start, elapsed);

        final File out = tmp.newFile("treadmill.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);

        assertNotNull("session.avgSpeed", session.getAvgSpeed());
        // Existing 2.5 m/s (= 9 km/h) preserved, NOT replaced by 5000/2004 ≈ 2.495.
        assertEquals(2.5f, session.getAvgSpeed(), 0.005f);
    }

    /**
     * Non-rowing distanceless workout (HIIT) — no synthesis should occur even though
     * strokes/distance are absent.
     */
    @Test
    public void hiitSession_doesNotSynthesizeDistance() throws Exception {
        final long start = 1777656743L;
        final long elapsed = 1200L;
        final BaseActivitySummary summary = newSummary(start, elapsed, ActivityKind.HIIT);
        final ActivitySummaryData data = new ActivitySummaryData();
        data.add(ActivitySummaryEntries.HR_AVG, 150, ActivitySummaryEntries.UNIT_BPM);

        final File out = tmp.newFile("hiit.fit");
        new FitExporter().performExport(null, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);
        // No distance and no avg_speed should be emitted — HIIT genuinely has none.
        assertNull("session.totalDistance", session.getTotalDistance());
        assertNull("session.avgSpeed", session.getAvgSpeed());
    }

    // ---------- helpers ----------

    private static BaseActivitySummary newSummary(final long startSec,
                                                  final long elapsedSec,
                                                  final ActivityKind kind) {
        final BaseActivitySummary s = new BaseActivitySummary();
        s.setStartTime(new Date(startSec * 1000L));
        s.setEndTime(new Date((startSec + elapsedSec) * 1000L));
        s.setActivityKind(kind.getCode());
        return s;
    }

    /** Build a 9-segment rowing track matching the 20-Apr workout structure (4 active + 5 rest)
     *  with the same per-segment strokes (151+4+256+6+261+9+257+1+263 = 1208 total). */
    private static ActivityTrack newRowingTrack(final long startSec) {
        final ActivityTrack track = new ActivityTrack();

        final int[][] segs = {
                // {durationSec, strokes, isActive(0x81 == 1)}
                {372, 151, 1}, {16, 4, 0}, {461, 256, 1}, {114, 6, 0},
                {461, 261, 1}, {111, 9, 0}, {455, 257, 1}, {118, 1, 0}, {442, 263, 1},
        };

        long ts = startSec;
        boolean first = true;
        for (final int[] s : segs) {
            final ActivityTrack.SegmentInfo info = new ActivityTrack.SegmentInfo(
                    s[2] == 1 ? ActivityTrack.SegmentIntensity.ACTIVE : ActivityTrack.SegmentIntensity.REST,
                    null,
                    s[1]);
            if (first) {
                track.setCurrentSegmentInfo(info);
                first = false;
            } else {
                track.startNewSegment(info);
            }
            // Add enough records that segments are not "tiny" (>= 5 records, >= 10s) so
            // the FitExporter keeps them as separate laps. Use one record per second.
            for (int i = 0; i < s[0]; i++) {
                final ActivityPoint p = new ActivityPoint(new Date((ts + i) * 1000L));
                p.setHeartRate(140);
                track.addTrackPoint(p);
            }
            ts += s[0];
        }
        return track;
    }

    /** Single-segment track with `nSec` heart-rate-only records — no per-record distance
     *  or speed. Forces the exporter to rely on summary fields and the new fallbacks. */
    private static ActivityTrack singleSegmentTrack(final long startSec, final long nSec) {
        final ActivityTrack track = new ActivityTrack();
        track.setCurrentSegmentInfo(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
        for (long i = 0; i < nSec; i++) {
            final ActivityPoint p = new ActivityPoint(new Date((startSec + i) * 1000L));
            p.setHeartRate(150);
            track.addTrackPoint(p);
        }
        return track;
    }

    private static FitSession onlySession(final FitFile fit) {
        FitSession s = null;
        for (final RecordData r : fit.getRecords()) {
            if (r instanceof FitSession) {
                if (s != null) {
                    throw new AssertionError("multiple sessions");
                }
                s = (FitSession) r;
            }
        }
        assertNotNull("no session record", s);
        return s;
    }

    private static List<FitLap> laps(final FitFile fit) {
        final List<FitLap> out = new ArrayList<>();
        for (final RecordData r : fit.getRecords()) {
            if (r instanceof FitLap) out.add((FitLap) r);
        }
        assertTrue("expected at least one lap", !out.isEmpty());
        return out;
    }
}
