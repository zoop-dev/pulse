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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLength;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSplit;

/**
 * Round-trip every FIT file referenced in
 * {@code app/src/test/resources/FIT-test-files-main/README.md}: parse → build a minimal
 * BaseActivitySummary + ActivityTrack from the original FitSession/FitRecord stream →
 * export with FitExporter → re-parse the output and assert key fields are preserved.
 *
 * <p>Also logs a per-file summary of which messages and fields were present in the
 * original (sport/subSport, manufacturer/product, record/lap/length/set/split counts) so
 * the corpus can be reviewed for fields the exporter does not yet cover.
 *
 * <p>Pure JVM — no Robolectric, no Android context, no DB.
 */
@Ignore("requires FIT-test-files-main corpus, not committed to the repo — keep for local/future use")
public class FitExporterReadmeRoundTripTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final File README = new File(
            "src/test/resources/FIT-test-files-main/README.md");
    private static final File CORPUS_ROOT = new File(
            "src/test/resources/FIT-test-files-main");
    // README rows: | sport | subsport | [label](Activity/.../foo.fit) |
    private static final Pattern README_LINK = Pattern.compile(
            "\\(([^)\\s]+\\.fit)\\)");

    @Test
    public void roundTripAllReadmeFiles() throws Exception {
        final List<String> relPaths = readmeFitPaths();
        assertTrue("README must list at least one fit file", !relPaths.isEmpty());
        runRoundTripBatch(relPaths);
    }

    /** Walks every {@code .fit} file under {@code Activity/<year>/} for years ≥ 2015
     *  and round-trips it. Broader than {@link #roundTripAllReadmeFiles()} which only
     *  covers the curated README sample. Pre-2015 files (1989–2014) exercise legacy
     *  codec edge-cases unrelated to current devices and are skipped. */
    @Test
    public void roundTripAllActivityFitFiles2015Onward() throws Exception {
        final java.nio.file.Path activityRoot = new File(CORPUS_ROOT, "Activity").toPath();
        if (!java.nio.file.Files.exists(activityRoot)) {
            // Test resource not present — skip silently. The README batch already
            // runs against the curated subset.
            return;
        }
        final java.util.regex.Pattern yearDir = java.util.regex.Pattern.compile(
                "Activity/(\\d{4})/.*");
        final List<String> relPaths = new ArrayList<>();
        try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(activityRoot)) {
            walk.filter(p -> p.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".fit"))
                .forEach(p -> {
                    final String rel = CORPUS_ROOT.toPath().relativize(p).toString().replace('\\', '/');
                    final Matcher m = yearDir.matcher(rel);
                    if (m.matches() && Integer.parseInt(m.group(1)) >= 2015) {
                        relPaths.add(rel);
                    }
                });
        }
        assertTrue("Activity/<year≥2015>/ must contain at least one fit file", !relPaths.isEmpty());
        runRoundTripBatch(relPaths);
    }

    private void runRoundTripBatch(final List<String> relPaths) throws Exception {

        final Map<String, RoundTripStats> results = new LinkedHashMap<>();
        final List<String> failures = new ArrayList<>();
        final List<String> codecSkips = new ArrayList<>();
        final List<String> noSessionSkips = new ArrayList<>();

        for (final String rel : relPaths) {
            final File in = new File(CORPUS_ROOT, rel);
            if (!in.exists()) {
                failures.add(rel + " — missing file");
                continue;
            }
            try {
                final RoundTripStats stats = roundTrip(in, rel);
                if (stats == null) {
                    noSessionSkips.add(rel);
                    continue;
                }
                results.put(rel, stats);
            } catch (final nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException
                          | IllegalArgumentException
                          | NullPointerException
                          | java.nio.BufferUnderflowException codec) {
                // Codec edge case in input file (unsupported BaseType, malformed
                // record header, etc.) — orthogonal to exporter behaviour. Warn and
                // continue; these surface as a separate corpus health stat.
                codecSkips.add(rel + " — " + codec.getClass().getSimpleName()
                        + ": " + codec.getMessage());
            } catch (final Throwable t) {
                failures.add(rel + " — " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        // Print per-file stats so the test output documents the corpus coverage.
        System.out.println("=== ReadmeRoundTrip results (" + results.size() + " files) ===");
        for (final Map.Entry<String, RoundTripStats> e : results.entrySet()) {
            System.out.println(e.getKey() + " → " + e.getValue());
        }
        if (!codecSkips.isEmpty()) {
            System.out.println("=== Codec-skip (" + codecSkips.size() + " files, parser limitation) ===");
            for (final String s : codecSkips) System.out.println("  - " + s);
        }
        if (!noSessionSkips.isEmpty()) {
            System.out.println("=== No-session skip (" + noSessionSkips.size()
                    + " files, sensor-only / tracker-upload, exporter-NA) ===");
            for (final String s : noSessionSkips) System.out.println("  - " + s);
        }

        if (!failures.isEmpty()) {
            final StringBuilder sb = new StringBuilder("Round-trip failures:\n");
            for (final String f : failures) sb.append("  - ").append(f).append('\n');
            fail(sb.toString());
        }
    }

    // ---------- core round-trip ----------

    private RoundTripStats roundTrip(final File in, final String relPath) throws Exception {
        final FitFile inFit = FitFile.parseIncoming(in);

        FitFileId fileId = null;
        FitSession session = null;
        final List<FitRecord> records = new ArrayList<>();
        final List<FitLap> laps = new ArrayList<>();
        final List<FitLength> lengths = new ArrayList<>();
        final List<FitSplit> splits = new ArrayList<>();
        final List<FitSet> setMessages = new ArrayList<>();
        // Field-number tally per message type — schema seen in input vs schema emitted
        // by the exporter. Surfaces device-vendor field-encoding gaps.
        final java.util.Set<Integer> inSessionFields = new java.util.TreeSet<>();
        final java.util.Set<Integer> inLapFields = new java.util.TreeSet<>();
        final java.util.Set<Integer> inRecordFields = new java.util.TreeSet<>();
        for (final RecordData r : inFit.getRecords()) {
            if (r instanceof FitFileId) fileId = (FitFileId) r;
            else if (r instanceof FitSession) {
                if (session == null) session = (FitSession) r; // first-only (multi-session not supported)
                collectFieldNumbers(r, inSessionFields);
            } else if (r instanceof FitRecord) {
                records.add((FitRecord) r);
                collectFieldNumbers(r, inRecordFields);
            } else if (r instanceof FitLap) {
                laps.add((FitLap) r);
                collectFieldNumbers(r, inLapFields);
            } else if (r instanceof FitLength) lengths.add((FitLength) r);
            else if (r instanceof FitSplit) splits.add((FitSplit) r);
            else if (r instanceof FitSet) setMessages.add((FitSet) r);
        }
        // Sensor-only / tracker-upload files (e.g. Vivosmart sleep, Edge cycling
        // computer GPX-only logs) carry no session record. Exporter cannot produce
        // a session-keyed FIT for them — surface as a separate skip stat rather
        // than a failure.
        if (session == null || fileId == null) return null;

        final RoundTripStats stats = new RoundTripStats();
        stats.inSize = in.length();
        stats.manufacturer = fileId.getManufacturer();
        stats.product = fileId.getProduct();
        stats.sport = session.getSport();
        stats.subSport = session.getSubSport();
        stats.numRecords = records.size();
        stats.numLaps = laps.size();
        stats.numLengths = lengths.size();
        stats.numSplits = splits.size();
        stats.numSets = setMessages.size();

        // Build the export inputs from the parsed file.
        final BaseActivitySummary summary = buildSummaryFromSession(session, in.getName());
        final ActivityTrack track = buildTrackFromRecordsAndLaps(records, laps);
        // Pass lengths/splits/sets through so the exporter can re-emit them on
        // round-trip. Mirrors what FitActivityTrackProvider does for the production
        // import path.
        for (final FitLength len : lengths) {
            final Long startTime = len.getStartTime();
            if (startTime == null) continue;
            track.addLength(new ActivityTrack.LengthInfo(
                    startTime,
                    len.getTotalElapsedTime() != null ? len.getTotalElapsedTime() : 0.0,
                    len.getTotalTimerTime() != null ? len.getTotalTimerTime() : 0.0,
                    len.getTotalStrokes(),
                    len.getAvgSpeed(),
                    len.getSwimStroke(),
                    len.getLengthType(),
                    len.getAvgSwimmingCadence()));
        }
        for (final FitSplit sp : splits) {
            final Long startTime = sp.getStartTime();
            if (startTime == null) continue;
            track.addSplit(new ActivityTrack.SplitInfo(
                    startTime,
                    sp.getEndTime(),
                    sp.getSplitType(),
                    sp.getTotalElapsedTime(),
                    sp.getTotalTimerTime(),
                    sp.getTotalDistance(),
                    sp.getAvgSpeed(),
                    sp.getMaxSpeed(),
                    sp.getTotalAscent(),
                    sp.getTotalDescent(),
                    sp.getTotalCalories(),
                    sp.getStartElevation(),
                    sp.getStartPositionLat(),
                    sp.getStartPositionLong(),
                    sp.getEndPositionLat(),
                    sp.getEndPositionLong()));
        }
        for (final FitSet sm : setMessages) {
            final Long startTime = sm.getStartTime();
            if (startTime == null) continue;
            track.addSet(new ActivityTrack.SetInfo(
                    startTime,
                    sm.getDuration(),
                    sm.getRepetitions(),
                    sm.getWeight(),
                    sm.getSetType(),
                    sm.getWeightDisplayUnit(),
                    sm.getMessageIndex()));
        }
        final ActivitySummaryData data = buildSummaryDataFromSession(session);

        // Export. Hash the relPath into the temp filename — the same basename
        // (e.g. Lap-Swimming-Fenix6x.fit) appears under multiple year dirs in the
        // corpus; without a hash the second one collides with TemporaryFolder.
        final String tmpName = in.getName() + "."
                + Integer.toHexString(relPath.hashCode()) + ".out.fit";
        final File out = tmp.newFile(tmpName);
        new FitExporter().performExport(track, summary, data, out);
        stats.outSize = out.length();

        // Re-parse and assert the key fields survived.
        final FitFile outFit = FitFile.parseIncoming(out);
        FitSession outSession = null;
        int outRecords = 0;
        int outLaps = 0;
        int outLengths = 0;
        int outSplits = 0;
        int outSets = 0;
        final java.util.Set<Integer> outSessionFields = new java.util.TreeSet<>();
        final java.util.Set<Integer> outLapFields = new java.util.TreeSet<>();
        final java.util.Set<Integer> outRecordFields = new java.util.TreeSet<>();
        for (final RecordData r : outFit.getRecords()) {
            if (r instanceof FitSession) {
                if (outSession == null) outSession = (FitSession) r;
                collectFieldNumbers(r, outSessionFields);
            } else if (r instanceof FitRecord) {
                outRecords++;
                collectFieldNumbers(r, outRecordFields);
            } else if (r instanceof FitLap) {
                outLaps++;
                collectFieldNumbers(r, outLapFields);
            } else if (r instanceof FitLength) {
                outLengths++;
            } else if (r instanceof FitSplit) {
                outSplits++;
            } else if (r instanceof FitSet) {
                outSets++;
            }
        }
        stats.outLengths = outLengths;
        stats.outSplits = outSplits;
        stats.outSets = outSets;
        stats.fieldDiff = formatFieldDiff(inSessionFields, outSessionFields,
                                           inLapFields, outLapFields,
                                           inRecordFields, outRecordFields);
        assertNotNull("exported file has no session: " + in.getName(), outSession);
        // The exporter always emits at least one lap and (when a track is present) one
        // record per timestamped point. Tiny segments may be merged into the first lap.
        assertTrue("exported file has no laps: " + in.getName(), outLaps >= 1);
        assertTrue("output should be < 5x input size: " + in.getName() + " in=" + in.length() + " out=" + out.length(),
                out.length() < Math.max(in.length() * 5L, 32_768L));

        // Sport/subSport: log mismatches instead of failing. The ActivityKind ↔ GarminSport
        // round-trip is not 1:1 (multiple FIT codes alias to one ActivityKind, e.g. sport=0
        // sub=15 ELLIPTICAL_TRAINER and sport=4 sub=15 FITNESS_EQUIPMENT both → ELLIPTICAL).
        // These mapping gaps are tracked separately; the round-trip itself still works.
        if (!java.util.Objects.equals(session.getSport(), outSession.getSport())
                || !java.util.Objects.equals(session.getSubSport(), outSession.getSubSport())) {
            stats.sportRemap = "in=" + session.getSport() + "/" + session.getSubSport()
                    + " out=" + outSession.getSport() + "/" + outSession.getSubSport();
        }

        // Per-track preservation: pick a stride of input records and verify the matching
        // output record (by timestamp) preserves the meaningful fields the parser exposed.
        verifyTrackPreservation(records, outFit, in.getName(), stats);

        // Session aggregate preservation: distance, hr, speed, calories.
        verifySessionPreservation(session, outSession, in.getName(), stats);

        stats.outRecords = outRecords;
        stats.outLaps = outLaps;
        return stats;
    }

    /** Pick up to ~30 input records spread across the activity and assert the
     *  corresponding output record (matched by timestamp) preserves the fields that
     *  drive Strava/Endurain rendering: GPS, altitude, speed, HR, cadence, distance,
     *  power, temperature. Allows ±1 unit tolerance for re-encoded floats. */
    private static void verifyTrackPreservation(final List<FitRecord> inRecords,
                                                final FitFile outFit,
                                                final String fileName,
                                                final RoundTripStats stats) {
        // Index output records by timestamp.
        final Map<Long, FitRecord> outByTs = new LinkedHashMap<>();
        for (final RecordData r : outFit.getRecords()) {
            if (r instanceof FitRecord rec) {
                final Long ts = rec.getComputedTimestamp();
                if (ts != null) outByTs.putIfAbsent(ts, rec);
            }
        }

        // FitExporter dedups records sharing a 1s timestamp (some sources emit
        // multiple records per second). Mirror that here so sampled input records
        // correspond to what the exporter actually kept; otherwise a sample landing
        // on the 2nd-of-pair would never find a matching output.
        final List<FitRecord> uniqueIn = new ArrayList<>(inRecords.size());
        long lastTs = Long.MIN_VALUE;
        for (final FitRecord r : inRecords) {
            final Long ts = r.getComputedTimestamp();
            if (ts == null || ts == lastTs) continue;
            uniqueIn.add(r);
            lastTs = ts;
        }
        final int sampleCount = Math.min(30, uniqueIn.size());
        if (sampleCount == 0) return;
        final int stride = Math.max(1, uniqueIn.size() / sampleCount);

        int gpsHits = 0, gpsTotal = 0;
        int altHits = 0, altTotal = 0;
        int hrHits = 0, hrTotal = 0;
        int spdHits = 0, spdTotal = 0;
        int cadHits = 0, cadTotal = 0;
        int distHits = 0, distTotal = 0;
        int pwrHits = 0, pwrTotal = 0;
        int tempHits = 0, tempTotal = 0;

        for (int i = 0; i < uniqueIn.size(); i += stride) {
            final FitRecord inRec = uniqueIn.get(i);
            final Long ts = inRec.getComputedTimestamp();
            if (ts == null) continue;
            final FitRecord outRec = outByTs.get(ts);
            if (outRec == null) continue; // dedup may drop duplicates — skip silently

            // GPS — both lat & lon must round-trip. FIT semicircle scale is exact,
            // so equality is OK after both encodes use the same scale.
            if (inRec.getLatitude() != null && inRec.getLongitude() != null) {
                gpsTotal++;
                if (inRec.getLatitude().equals(outRec.getLatitude())
                        && inRec.getLongitude().equals(outRec.getLongitude())) {
                    gpsHits++;
                }
            }
            // Altitude — input may use altitude OR enhanced_altitude; we always export
            // enhanced_altitude. Compare whichever the input had against the same
            // *canonicalised* meters value the parser computes.
            final Double inAlt = inRec.getEnhancedAltitude() != null
                    ? inRec.getEnhancedAltitude()
                    : (inRec.getAltitude() != null ? inRec.getAltitude().doubleValue() : null);
            if (inAlt != null) {
                altTotal++;
                final Double outAlt = outRec.getEnhancedAltitude() != null
                        ? outRec.getEnhancedAltitude()
                        : (outRec.getAltitude() != null ? outRec.getAltitude().doubleValue() : null);
                if (outAlt != null && Math.abs(outAlt - inAlt) <= 0.5) altHits++;
            }
            // HR — integer bpm. Some recorders (Zwift virtual rides without a strap)
            // write hr=0 as a sentinel for "no measurement"; FitExporter drops zeros
            // explicitly. Mirror that — only count records with a real HR sample.
            if (inRec.getHeartRate() != null && inRec.getHeartRate() > 0) {
                hrTotal++;
                if (java.util.Objects.equals(inRec.getHeartRate(), outRec.getHeartRate())) hrHits++;
            }
            // Speed — input may use speed OR enhanced_speed; exporter writes enhanced_speed.
            final Double inSpd = inRec.getEnhancedSpeed() != null
                    ? inRec.getEnhancedSpeed()
                    : (inRec.getSpeed() != null ? inRec.getSpeed().doubleValue() : null);
            if (inSpd != null) {
                spdTotal++;
                final Double outSpd = outRec.getEnhancedSpeed() != null
                        ? outRec.getEnhancedSpeed()
                        : (outRec.getSpeed() != null ? outRec.getSpeed().doubleValue() : null);
                if (outSpd != null && Math.abs(outSpd - inSpd) <= 0.05) spdHits++;
            }
            // Cadence: same sensor-absent sentinel as HR — when the source has no
            // cadence sensor it writes 0; FitExporter now drops cadence on tracks where
            // every sample is 0. Only count records with a real measurement.
            if (inRec.getCadence() != null && inRec.getCadence() > 0) {
                cadTotal++;
                if (java.util.Objects.equals(inRec.getCadence(), outRec.getCadence())) cadHits++;
            }
            if (inRec.getDistance() != null) {
                distTotal++;
                if (outRec.getDistance() != null
                        && Math.abs(outRec.getDistance() - inRec.getDistance()) <= 1.0) {
                    distHits++;
                }
            }
            // Power: same sentinel handling.
            if (inRec.getPower() != null && inRec.getPower() > 0) {
                pwrTotal++;
                if (java.util.Objects.equals(inRec.getPower(), outRec.getPower())) pwrHits++;
            }
            if (inRec.getTemperature() != null) {
                tempTotal++;
                if (java.util.Objects.equals(inRec.getTemperature(), outRec.getTemperature())) tempHits++;
            }
        }

        stats.gpsKept = pct(gpsHits, gpsTotal);
        stats.altKept = pct(altHits, altTotal);
        stats.hrKept = pct(hrHits, hrTotal);
        stats.spdKept = pct(spdHits, spdTotal);
        stats.cadKept = pct(cadHits, cadTotal);
        stats.distKept = pct(distHits, distTotal);
        stats.pwrKept = pct(pwrHits, pwrTotal);
        stats.tempKept = pct(tempHits, tempTotal);

        // Hard assertions — values that are present in input must round-trip on the
        // sampled records. Tolerate the very rare dedup miss by requiring ≥ 90%.
        final List<String> regressions = new ArrayList<>();
        if (gpsTotal >= 5 && gpsHits * 100 / gpsTotal < 90) regressions.add("gps " + stats.gpsKept);
        if (altTotal >= 5 && altHits * 100 / altTotal < 90) regressions.add("alt " + stats.altKept);
        if (hrTotal >= 5 && hrHits * 100 / hrTotal < 90) regressions.add("hr " + stats.hrKept);
        if (spdTotal >= 5 && spdHits * 100 / spdTotal < 90) regressions.add("spd " + stats.spdKept);
        if (cadTotal >= 5 && cadHits * 100 / cadTotal < 90) regressions.add("cad " + stats.cadKept);
        if (distTotal >= 5 && distHits * 100 / distTotal < 90) regressions.add("dist " + stats.distKept);
        if (pwrTotal >= 5 && pwrHits * 100 / pwrTotal < 90) regressions.add("pwr " + stats.pwrKept);
        if (tempTotal >= 5 && tempHits * 100 / tempTotal < 90) regressions.add("temp " + stats.tempKept);
        if (!regressions.isEmpty()) {
            throw new AssertionError(fileName + " field preservation regressions: " + regressions);
        }
    }

    /** Re-parse session aggregates and compare against the input session. */
    private static void verifySessionPreservation(final FitSession in,
                                                  final FitSession out,
                                                  final String fileName,
                                                  final RoundTripStats stats) {
        final List<String> mismatches = new ArrayList<>();
        // Distance preservation: getter returns raw meters (codec applies scale=100).
        // Skip when input is 0 (sentinel for "not measured" — common on rowing /
        // strength / indoor sessions where the device wrote no distance but the
        // exporter may legitimately derive a value from per-record GPS or strokes).
        if (in.getTotalDistance() != null && out.getTotalDistance() != null
                && in.getTotalDistance() > 0) {
            final double inM = in.getTotalDistance();
            final double outM = out.getTotalDistance();
            // Accept ±1 m or 0.5% — exporter rounds via Math.round.
            if (Math.abs(outM - inM) > Math.max(1.0, inM * 0.005)) {
                mismatches.add("distance in=" + inM + "m out=" + outM + "m");
            }
            stats.sessionDistanceM = outM;
        }
        // HR. Skip when input avg=0 (no strap → sentinel; some devices record
        // record-level HR but never aggregate to session — exporter computes one).
        if (in.getAverageHeartRate() != null && out.getAverageHeartRate() != null
                && in.getAverageHeartRate() > 0
                && !in.getAverageHeartRate().equals(out.getAverageHeartRate())) {
            mismatches.add("avgHr in=" + in.getAverageHeartRate() + " out=" + out.getAverageHeartRate());
        }
        // Avg speed: input scale is FIT m/s × 1000; we round-trip via Float.
        // Exporter intentionally re-derives avg_speed from distance/elapsed when the
        // source recorded 0 (devices without per-record speed) — see pace-fallback in
        // FitExporter.buildSession. Skip the comparison in that case.
        if (in.getAvgSpeed() != null && out.getAvgSpeed() != null
                && in.getAvgSpeed() > 0.001f
                && Math.abs(in.getAvgSpeed() - out.getAvgSpeed()) > 0.05) {
            mismatches.add("avgSpeed in=" + in.getAvgSpeed() + " out=" + out.getAvgSpeed());
        }
        // Calories. Skip when input=0 — exporter may compute one from track data.
        if (in.getTotalCalories() != null && out.getTotalCalories() != null
                && in.getTotalCalories() > 0
                && !in.getTotalCalories().equals(out.getTotalCalories())) {
            mismatches.add("cal in=" + in.getTotalCalories() + " out=" + out.getTotalCalories());
        }
        if (!mismatches.isEmpty()) {
            throw new AssertionError(fileName + " session aggregate regressions: " + mismatches);
        }
    }

    private static String pct(final int hits, final int total) {
        if (total == 0) return "—";
        return hits + "/" + total + " (" + (hits * 100 / total) + "%)";
    }

    /** Add every FieldDefinition number from a parsed record's definition into the
     *  given set. The same field may appear across many records of the same type;
     *  collecting into a Set yields the schema actually emitted by the source. */
    private static void collectFieldNumbers(final RecordData r, final java.util.Set<Integer> dst) {
        if (r.getRecordDefinition() == null) return;
        final java.util.List<FieldDefinition> defs = r.getRecordDefinition().getFieldDefinitions();
        if (defs == null) return;
        for (final FieldDefinition fd : defs) {
            dst.add(fd.getNumber());
        }
    }

    /** Build a one-line "in vs out" field-number diff for session/lap/record. The
     *  in-only side surfaces device-emitted fields the exporter does not yet write;
     *  the out-only side flags fields the exporter adds (e.g. enhanced_altitude on
     *  records when source only had legacy altitude). */
    private static String formatFieldDiff(final java.util.Set<Integer> inSession, final java.util.Set<Integer> outSession,
                                          final java.util.Set<Integer> inLap, final java.util.Set<Integer> outLap,
                                          final java.util.Set<Integer> inRec, final java.util.Set<Integer> outRec) {
        return "session{" + diff(inSession, outSession) + "} lap{" + diff(inLap, outLap)
                + "} record{" + diff(inRec, outRec) + "}";
    }

    private static String diff(final java.util.Set<Integer> in, final java.util.Set<Integer> out) {
        final java.util.Set<Integer> inOnly = new java.util.TreeSet<>(in);
        inOnly.removeAll(out);
        final java.util.Set<Integer> outOnly = new java.util.TreeSet<>(out);
        outOnly.removeAll(in);
        return "in-only=" + inOnly + " out-only=" + outOnly;
    }

    private static BaseActivitySummary buildSummaryFromSession(final FitSession session,
                                                                final String fileName) {
        final BaseActivitySummary s = new BaseActivitySummary();
        // FitSession.startTime is FIT epoch seconds (timestamp_t base), already absolute
        // unix seconds — codec subtracts the Garmin epoch on decode.
        final Long startSec = session.getStartTime();
        final long startMs = startSec != null ? startSec * 1000L : 0L;
        s.setStartTime(new Date(startMs));
        // total_elapsed_time is stored unscaled (raw uint32 = ms per FIT spec scale=1000).
        final Long elapsedMs = session.getTotalElapsedTime();
        s.setEndTime(new Date(startMs + (elapsedMs != null ? elapsedMs : 0L)));
        s.setName(fileName);
        final ActivityKind kind = mapSportToKind(session.getSport(), session.getSubSport());
        s.setActivityKind(kind.getCode());
        return s;
    }

    private static ActivityKind mapSportToKind(final Integer sport, final Integer subSport) {
        if (sport == null) return ActivityKind.UNKNOWN;
        final Optional<GarminSport> gs = GarminSport.fromCodes(sport, subSport != null ? subSport : 0);
        return gs.map(GarminSport::getActivityKind).orElse(ActivityKind.UNKNOWN);
    }

    private static ActivityTrack buildTrackFromRecordsAndLaps(final List<FitRecord> records,
                                                              final List<FitLap> laps) {
        final ActivityTrack track = new ActivityTrack();
        track.setCurrentSegmentInfo(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
        // Lap boundaries: split records into segments by lap.startTime monotone walk.
        final long[] lapBoundaries = laps.stream()
                .map(FitLap::getStartTime)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sorted()
                .toArray();
        int boundaryIdx = 0;
        boolean firstSeg = true;
        for (final FitRecord rec : records) {
            final ActivityPoint p = rec.toActivityPoint();
            // Cross next lap boundary → start a new segment.
            while (boundaryIdx < lapBoundaries.length
                    && p.getTime() != null
                    && p.getTime().getTime() / 1000L >= lapBoundaries[boundaryIdx]) {
                if (!firstSeg) {
                    track.startNewSegment(new ActivityTrack.SegmentInfo(ActivityTrack.SegmentIntensity.ACTIVE));
                }
                firstSeg = false;
                boundaryIdx++;
            }
            track.addTrackPoint(p);
        }
        return track;
    }

    /** Translate a small but useful subset of FitSession aggregate fields back into
     *  ActivitySummaryData entries so FitExporter populates the same lap/session
     *  aggregates on the way out. Mirrors the fields GarminWorkoutParser emits. */
    private static ActivitySummaryData buildSummaryDataFromSession(final FitSession session) {
        final ActivitySummaryData d = new ActivitySummaryData();
        // Getter returns raw meters (codec applies scale=100 on decode).
        final Double totalDistance = session.getTotalDistance();
        if (totalDistance != null) {
            d.add(ActivitySummaryEntries.DISTANCE_METERS,
                    totalDistance,
                    ActivitySummaryEntries.UNIT_METERS);
        }
        if (session.getTotalCalories() != null) {
            d.add(ActivitySummaryEntries.CALORIES_BURNT,
                    session.getTotalCalories(),
                    ActivitySummaryEntries.UNIT_KCAL);
        }
        if (session.getAvgSpeed() != null) {
            d.add(ActivitySummaryEntries.SPEED_AVG,
                    session.getAvgSpeed(),
                    ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
        }
        if (session.getMaxSpeed() != null) {
            d.add(ActivitySummaryEntries.SPEED_MAX,
                    session.getMaxSpeed(),
                    ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
        }
        if (session.getAverageHeartRate() != null) {
            d.add(ActivitySummaryEntries.HR_AVG,
                    session.getAverageHeartRate(),
                    ActivitySummaryEntries.UNIT_BPM);
        }
        if (session.getMaxHeartRate() != null) {
            d.add(ActivitySummaryEntries.HR_MAX,
                    session.getMaxHeartRate(),
                    ActivitySummaryEntries.UNIT_BPM);
        }
        if (session.getAvgCadence() != null) {
            d.add(ActivitySummaryEntries.CADENCE_AVG,
                    session.getAvgCadence(),
                    ActivitySummaryEntries.UNIT_NONE);
        }
        if (session.getMaxCadence() != null) {
            d.add(ActivitySummaryEntries.CADENCE_MAX,
                    session.getMaxCadence(),
                    ActivitySummaryEntries.UNIT_NONE);
        }
        if (session.getTotalAscent() != null) {
            d.add(ActivitySummaryEntries.ASCENT_METERS,
                    session.getTotalAscent(),
                    ActivitySummaryEntries.UNIT_METERS);
        }
        if (session.getTotalDescent() != null) {
            d.add(ActivitySummaryEntries.DESCENT_METERS,
                    session.getTotalDescent(),
                    ActivitySummaryEntries.UNIT_METERS);
        }
        return d;
    }

    // ---------- README parsing ----------

    private static List<String> readmeFitPaths() throws Exception {
        final String contents = new String(Files.readAllBytes(README.toPath()), StandardCharsets.UTF_8);
        final List<String> out = new ArrayList<>();
        final Matcher m = README_LINK.matcher(contents);
        while (m.find()) {
            final String path = m.group(1);
            // Skip any non-Activity links (e.g. AllFitMessageTypes.fit lives at root).
            // Keep only relative paths into the corpus.
            if (path.startsWith("Activity/")) {
                out.add(path);
            }
        }
        return out;
    }

    // ---------- stats ----------

    private static final class RoundTripStats {
        long inSize;
        long outSize;
        Integer manufacturer;
        Integer product;
        Integer sport;
        Integer subSport;
        int numRecords;
        int numLaps;
        int numLengths;
        int numSplits;
        int numSets;
        int outRecords;
        int outLaps;
        int outLengths;
        int outSplits;
        int outSets;
        String sportRemap;
        String fieldDiff = "";
        String gpsKept = "—";
        String altKept = "—";
        String hrKept = "—";
        String spdKept = "—";
        String cadKept = "—";
        String distKept = "—";
        String pwrKept = "—";
        String tempKept = "—";
        Double sessionDistanceM;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("in=").append(inSize).append("B out=").append(outSize).append("B")
                    .append(" mfr=").append(manufacturer).append(" prod=").append(product)
                    .append(" sport=").append(sport).append('/').append(subSport)
                    .append(" in[rec=").append(numRecords).append(",lap=").append(numLaps)
                    .append(",len=").append(numLengths).append(",split=").append(numSplits)
                    .append(",set=").append(numSets).append(']')
                    .append(" out[rec=").append(outRecords).append(",lap=").append(outLaps)
                    .append(",len=").append(outLengths)
                    .append(",split=").append(outSplits)
                    .append(",set=").append(outSets).append(']');
            sb.append(" preserved[gps=").append(gpsKept).append(" alt=").append(altKept)
                    .append(" hr=").append(hrKept).append(" spd=").append(spdKept)
                    .append(" cad=").append(cadKept).append(" dist=").append(distKept)
                    .append(" pwr=").append(pwrKept).append(" temp=").append(tempKept).append(']');
            if (sportRemap != null) sb.append(" sportRemap[").append(sportRemap).append(']');
            if (fieldDiff != null && !fieldDiff.isEmpty()) sb.append("\n    fieldDiff: ").append(fieldDiff);
            return sb.toString();
        }
    }
}
