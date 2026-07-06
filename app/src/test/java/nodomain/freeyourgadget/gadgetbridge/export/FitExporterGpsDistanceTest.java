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
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;

/**
 * Covers the FitExporter GPS-recovery paths (A1 distance, A2 elevation, B3 gps_accuracy):
 * when a device parser emits a GPS track but no measured distance / elevation, the
 * exporter recovers total_distance / per-record distance / ascent+descent from the GPS
 * stream, and passes through gps_accuracy from the coordinate hdop slot.
 *
 * <p>Pure JVM — builds synthetic ActivityTrack inputs, exports to a temp file, decodes
 * the FIT bytes via FitFile.parseIncoming, and asserts Session/Lap/Record fields. No
 * Android context, DB, or device involved. Notably does NOT use GPSCoordinate.getDistance
 * (android.location.Location) — the exporter's haversine is pure Java, matched here.
 */
public class FitExporterGpsDistanceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // A GPS walking track starting at (48.10, 11.50), stepping north ~lat 0.0001/s.
    private static final double BASE_LAT = 48.10;
    private static final double BASE_LON = 11.50;
    private static final double LAT_STEP = 0.0001; // ~11.1 m per second

    /** 1. Walking, GPS-only, no measured distance → distance recovered from GPS haversine;
     *  per-record distance monotonic; avg_speed derived from distance/elapsed. */
    @Test
    public void walkingGpsOnly_recoversDistanceFromGps() throws Exception {
        final long start = 1777747544L;
        final int n = 120;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.WALKING);
        final ActivitySummaryData data = new ActivitySummaryData();
        data.add(ActivitySummaryEntries.HR_AVG, 110, ActivitySummaryEntries.UNIT_BPM);

        final ActivityTrack track = gpsTrack(start, n, 0.0 /*altitude*/, false /*setHdop*/);

        final File out = tmp.newFile("walking.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitFile fit = FitFile.parseIncoming(out);
        final FitSession session = onlySession(fit);
        final List<FitLap> laps = laps(fit);
        final List<FitRecord> records = records(fit);

        final double expected = expectedGpsDistanceMeters(n);
        assertNotNull("session.totalDistance", session.getTotalDistance());
        assertEquals(expected, session.getTotalDistance(), expected * 0.02);

        // Single segment → single lap covering the whole session, distance ≈ session.
        assertEquals(1, laps.size());
        assertNotNull("lap.totalDistance", laps.get(0).getTotalDistance());
        assertEquals(expected, laps.get(0).getTotalDistance(), expected * 0.02);

        // avg_speed derived: expected / elapsed.
        assertNotNull("session.avgSpeed", session.getAvgSpeed());
        assertEquals((float) (expected / (n - 1)), session.getAvgSpeed(), 0.05f);

        // Per-record distance present and monotonically non-decreasing.
        double prev = -1;
        int withDistance = 0;
        for (final FitRecord r : records) {
            final Double d = r.getDistance();
            if (d == null) continue;
            withDistance++;
            assertTrue("record distance monotonic", d >= prev);
            prev = d;
        }
        assertTrue("most records carry recovered distance", withDistance >= n - 2);
    }

    /** 2. Rising altitude (V3-style) → total_ascent > 0, total_descent ≈ 0. */
    @Test
    public void risingAltitude_recoversAscent() throws Exception {
        final long start = 1777747544L;
        final int n = 100;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.HIKING);
        final ActivitySummaryData data = new ActivitySummaryData();

        // altitude rises 0.5 m each second from 100 m → ascent ≈ (n-1)*0.5 = 49.5 m.
        final ActivityTrack track = new ActivityTrack();
        track.setCurrentSegmentInfo(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
        for (int i = 0; i < n; i++) {
            final ActivityPoint p = new ActivityPoint(new Date((start + i) * 1000L));
            final GPSCoordinate loc = new GPSCoordinate(BASE_LON, BASE_LAT + i * LAT_STEP, 100.0 + i * 0.5);
            p.setLocation(loc);
            track.addTrackPoint(p);
        }

        final File out = tmp.newFile("hiking-up.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitSession session = onlySession(FitFile.parseIncoming(out));
        assertNotNull("session.totalAscent", session.getTotalAscent());
        assertEquals(49, session.getTotalAscent().intValue(), 2);
        // Strictly rising → no descent.
        assertTrue("no descent on a strict climb",
                session.getTotalDescent() == null || session.getTotalDescent() == 0);
    }

    /** 3. All-zero altitude (Xiaomi GPS V1/V2) → ascent/descent omitted, NOT emitted as 0. */
    @Test
    public void flatZeroAltitude_omitsAscentDescent() throws Exception {
        final long start = 1777747544L;
        final int n = 100;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.WALKING);

        final ActivityTrack track = gpsTrack(start, n, 0.0 /*altitude all zero*/, false);

        final File out = tmp.newFile("walking-flat.fit");
        new FitExporter().performExport(track, summary, new ActivitySummaryData(), out);

        final FitSession session = onlySession(FitFile.parseIncoming(out));
        assertNull("no ascent when altitude is constant/zero", session.getTotalAscent());
        assertNull("no descent when altitude is constant/zero", session.getTotalDescent());
    }

    /** 4. hdop set on the coordinate → record.gps_accuracy emitted (clamped metres). */
    @Test
    public void hdopSet_emitsGpsAccuracy() throws Exception {
        final long start = 1777747544L;
        final int n = 30;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.WALKING);

        final ActivityTrack track = gpsTrack(start, n, 0.0, true /*setHdop=5m*/);

        final File out = tmp.newFile("walking-hdop.fit");
        new FitExporter().performExport(track, summary, new ActivitySummaryData(), out);

        boolean found = false;
        for (final FitRecord r : records(FitFile.parseIncoming(out))) {
            final Integer acc = r.getGpsAccuracy();
            if (acc != null) {
                assertEquals(5, acc.intValue());
                found = true;
            }
        }
        assertTrue("at least one record carries gps_accuracy", found);
    }

    /** 5. Measured summary distance present → GPS fallback NOT used (measured value wins). */
    @Test
    public void measuredDistance_gpsFallbackNotUsed() throws Exception {
        final long start = 1777747544L;
        final int n = 120;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.WALKING);
        final ActivitySummaryData data = new ActivitySummaryData();
        // Deliberately different from the GPS-derived distance so we can tell them apart.
        data.add(ActivitySummaryEntries.DISTANCE_METERS, 5000, ActivitySummaryEntries.UNIT_METERS);

        final ActivityTrack track = gpsTrack(start, n, 0.0, false);

        final File out = tmp.newFile("walking-measured.fit");
        new FitExporter().performExport(track, summary, data, out);

        final FitSession session = onlySession(FitFile.parseIncoming(out));
        assertNotNull("session.totalDistance", session.getTotalDistance());
        assertEquals("measured distance must win over GPS", 5000.0, session.getTotalDistance(), 1.0);
    }

    /** 6. Non-locomotion sport (yoga) with a stray GPS track → no invented distance. */
    @Test
    public void nonLocomotionSport_noInventedDistance() throws Exception {
        final long start = 1777747544L;
        final int n = 60;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.YOGA);

        final ActivityTrack track = gpsTrack(start, n, 0.0, false);

        final File out = tmp.newFile("yoga.fit");
        new FitExporter().performExport(track, summary, new ActivitySummaryData(), out);

        final FitSession session = onlySession(FitFile.parseIncoming(out));
        assertNull("yoga must not get a GPS-invented distance", session.getTotalDistance());
    }

    /** 7. step_length (mm) present on points → emitted on record and round-trips. */
    @Test
    public void stepLength_roundTrips() throws Exception {
        final long start = 1777747544L;
        final int n = 20;
        final BaseActivitySummary summary = newSummary(start, n - 1, ActivityKind.OUTDOOR_RUNNING);

        final ActivityTrack track = new ActivityTrack();
        track.setCurrentSegmentInfo(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
        for (int i = 0; i < n; i++) {
            final ActivityPoint.Builder b = new ActivityPoint.Builder(new Date((start + i) * 1000L));
            b.setLatitude(BASE_LAT + i * LAT_STEP);
            b.setLongitude(BASE_LON);
            b.setStepLength(1200); // mm
            track.addTrackPoint(b.build());
        }

        final File out = tmp.newFile("running-steplen.fit");
        new FitExporter().performExport(track, summary, new ActivitySummaryData(), out);

        boolean found = false;
        for (final FitRecord r : records(FitFile.parseIncoming(out))) {
            final Float sl = r.getStepLength();
            if (sl != null) {
                assertEquals(1200f, sl, 0.5f);
                found = true;
            }
        }
        assertTrue("at least one record carries step_length", found);
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

    /** Single-segment GPS track: n points, 1/sec, stepping north by LAT_STEP. altitude &lt;= 0
     *  means "set literal altitude" (0 == V1/V2 garbage). setHdop tags each coordinate 5 m. */
    private static ActivityTrack gpsTrack(final long startSec, final int n,
                                          final double altitude, final boolean setHdop) {
        final ActivityTrack track = new ActivityTrack();
        track.setCurrentSegmentInfo(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
        for (int i = 0; i < n; i++) {
            final ActivityPoint p = new ActivityPoint(new Date((startSec + i) * 1000L));
            final GPSCoordinate loc = new GPSCoordinate(BASE_LON, BASE_LAT + i * LAT_STEP, altitude);
            if (setHdop) loc.setHdop(5.0);
            p.setLocation(loc);
            track.addTrackPoint(p);
        }
        return track;
    }

    /** Expected total distance for an n-point track stepping LAT_STEP north each record,
     *  using a test-local haversine (mirrors the exporter's pure-Java formula). */
    private static double expectedGpsDistanceMeters(final int n) {
        double sum = 0;
        for (int i = 1; i < n; i++) {
            sum += haversine(BASE_LAT + (i - 1) * LAT_STEP, BASE_LON,
                    BASE_LAT + i * LAT_STEP, BASE_LON);
        }
        return sum;
    }

    private static double haversine(final double lat1d, final double lon1d,
                                    final double lat2d, final double lon2d) {
        final double R = 6_371_000.0;
        final double lat1 = Math.toRadians(lat1d), lat2 = Math.toRadians(lat2d);
        final double dLat = lat2 - lat1;
        final double dLon = Math.toRadians(lon2d - lon1d);
        final double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    private static FitSession onlySession(final FitFile fit) {
        FitSession s = null;
        for (final RecordData r : fit.getRecords()) {
            if (r instanceof FitSession) {
                if (s != null) throw new AssertionError("multiple sessions");
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

    private static List<FitRecord> records(final FitFile fit) {
        final List<FitRecord> out = new ArrayList<>();
        for (final RecordData r : fit.getRecords()) {
            if (r instanceof FitRecord) out.add((FitRecord) r);
        }
        return out;
    }
}
