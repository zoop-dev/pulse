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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileCreator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLength;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSession;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSet;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitSplit;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitWorkout;

/**
 * Exports an arbitrary workout (summary + optional ActivityTrack) as a FIT ACTIVITY file
 * by reusing the auto-generated Garmin FIT message builders. Manufacturer is set to
 * "development" (255) since the file is synthesized by Gadgetbridge rather than emitted
 * by a Garmin device. The exporter is device-agnostic: it consumes the generic
 * BaseActivitySummary / ActivitySummaryData / ActivityTrack types and works for any
 * device whose parser populates those objects.
 */
public class FitExporter {
    private static final Logger LOG = LoggerFactory.getLogger(FitExporter.class);

    // FIT manufacturer ID. 255 = "development" (anyone can use; some strict importers
    // such as old Strava clients reject this — switch to a registered ID if needed).
    public static final int DEFAULT_MANUFACTURER_ID = 255;

    private final int manufacturerId;

    // FIT event enum values (subset)
    private static final int EVENT_TIMER = 0;
    private static final int EVENT_ACTIVITY = 26;
    private static final int EVENT_TYPE_START = 0;
    private static final int EVENT_TYPE_STOP = 1;
    private static final int EVENT_TYPE_STOP_ALL = 9;
    private static final int ACTIVITY_TYPE_MANUAL = 0;

    // Local message type slots (per-file message numbering)
    private static final int LMT_FILE_ID = 0;
    private static final int LMT_FILE_CREATOR = 1;
    private static final int LMT_EVENT = 2;
    private static final int LMT_RECORD = 3;
    private static final int LMT_LAP = 4;
    private static final int LMT_SESSION = 5;
    private static final int LMT_ACTIVITY = 6;
    private static final int LMT_WORKOUT = 7;
    private static final int LMT_LENGTH = 8;
    private static final int LMT_SPLIT = 9;
    private static final int LMT_SET = 10;

    // FIT lap intensity codes (from intensity_t in the FIT spec)
    private static final int FIT_INTENSITY_ACTIVE = 0;
    private static final int FIT_INTENSITY_REST = 1;

    // Tiny-segment threshold: ActivityTrack segments smaller than this are dropped
    // from the lap stream (their records still emit in the FIT record stream — only
    // the lap boundary is suppressed). Avoids 1-record "rest blip" noise from parsers
    // that signal a transient pause as its own segment.
    private static final int TINY_SEGMENT_MIN_RECORDS = 5;
    private static final long TINY_SEGMENT_MIN_SECONDS = 10L;

    // Default per-stroke distance (m) used to synthesize total_distance for rowing
    // workouts where the watch records strokes but no measured distance — see
    // deriveRowingDistanceMeters. Indoor erg averages 6-8 m/stroke at recreational
    // 24-32 spm (Concept2 published averages); on-water sculling runs longer per stroke
    // (8-10 m). TODO: expose as a user preference once a settings host is identified.
    static final double DEFAULT_INDOOR_ROWING_STROKE_LENGTH_M = 6.0;
    static final double DEFAULT_OUTDOOR_ROWING_STROKE_LENGTH_M = 9.0;

    public FitExporter() {
        this(DEFAULT_MANUFACTURER_ID);
    }

    public FitExporter(final int manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    /**
     * If the summary points to an original FIT file (e.g. Garmin / iGPSPORT, where
     * {@code FitImporter} stored the device's own .fit at {@code rawDetailsPath}),
     * returns that File so callers can export it verbatim — preserving full fidelity —
     * instead of regenerating one. Returns null when there is no usable raw FIT, in
     * which case the caller should fall back to {@link #performExport}.
     */
    @Nullable
    public static File resolveRawFitFile(@Nullable final BaseActivitySummary summary) {
        if (summary == null) {
            return null;
        }
        final String rawPath = summary.getRawDetailsPath();
        if (rawPath == null || rawPath.isEmpty()) {
            return null;
        }
        final File file = new File(rawPath);
        if (!file.isFile() || !file.canRead()) {
            return null;
        }
        return isFitFile(file) ? file : null;
    }

    /** True when the file carries the ".FIT" magic at header bytes 8-11 (FIT spec). */
    private static boolean isFitFile(@NonNull final File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            final byte[] header = new byte[12];
            if (in.read(header) < header.length) {
                return false;
            }
            return header[8] == '.' && header[9] == 'F' && header[10] == 'I' && header[11] == 'T';
        } catch (final IOException e) {
            LOG.warn("Could not read FIT header of {}", file, e);
            return false;
        }
    }

    private static int mapIntensity(final ActivityTrack.SegmentIntensity intensity) {
        if (intensity == ActivityTrack.SegmentIntensity.REST) return FIT_INTENSITY_REST;
        // ACTIVE and UNKNOWN both map to ACTIVE — UNKNOWN defaults to active so that
        // importers that filter by intensity still surface the lap.
        return FIT_INTENSITY_ACTIVE;
    }

    /// Detects FIT sport+sub_sport pairs that represent rowing of any kind. Used to gate
    /// the rowing-only stroke→distance fallback. Covers ROWING (15,*), ROW_INDOOR (15,14),
    /// and the alternate FITNESS_EQUIPMENT-class indoor rowing (4,14).
    private static boolean isRowingSport(final int sport, final int subSport) {
        return sport == 15 || (sport == 4 && subSport == 14);
    }

    /// Sports whose Lap/Session messages may carry the num_lengths field. Lap swimming
    /// (5,17) plus TRACK_RUN (1,4) and INDOOR_TRACK (1,45) where a "length" maps to a
    /// track loop. Currently a no-op for the running variants until FitLength records are
    /// emitted for them.
    private static boolean sportSupportsNumLengths(final int sport, final int subSport) {
        return (sport == 5 && subSport == 17)
                || (sport == 1 && (subSport == 4 || subSport == 45));
    }

    /// True when the activity's distance-over-time has a meaningful "speed" interpretation.
    /// Used to gate the avg_speed = distance/elapsed pace fallback in buildLap/buildSession —
    /// for stationary or non-locomotion sports (yoga, breathing, strength, stopwatch,
    /// meditation) the fallback would invent a bogus speed.
    private static boolean isLocomotionSport(final int sport, final int subSport) {
        // Sport-level non-locomotion families.
        if (sport == 52) return false;        // stopwatch
        if (sport == 60) return false;        // health snapshot
        if (sport == 67) return false;        // meditation
        // Generic family — only breathing(62) is non-locomotion among the common subsports.
        if (sport == 0 && subSport == 62) return false;
        // Training family — strength/yoga/stretching/breathing/cardio do not have a
        // meaningful speed even when distance is logged.
        if (sport == 10) {
            return subSport != 19   // stretching
                    && subSport != 20   // strength
                    && subSport != 43   // yoga
                    && subSport != 62;  // breathing
        }
        // Fitness equipment family — strength/yoga/pilates do not have meaningful speed.
        if (sport == 4) {
            return subSport != 20   // calisthenics/strength
                    && subSport != 43   // yoga equipment
                    && subSport != 44;  // pilates
        }
        return true;
    }

    /// Picks the per-stroke distance default for the given rowing sport/sub-sport pair.
    /// Indoor erg (sub_sport=14) uses a shorter stroke; on-water rowing uses a longer one.
    /// Caller is expected to gate with isRowingSport first.
    private static double rowingStrokeLengthMeters(final int sport, final int subSport) {
        return subSport == 14
                ? DEFAULT_INDOOR_ROWING_STROKE_LENGTH_M
                : DEFAULT_OUTDOOR_ROWING_STROKE_LENGTH_M;
    }

    /// Synthesizes a total-distance value for stroke-based workouts (rowing) where the
    /// watch only reports strokes. Returns null for non-positive strokes/length so callers
    /// can skip emitting the field entirely.
    @Nullable
    private static Double deriveRowingDistanceMeters(@Nullable final Long strokes,
                                                     final double strokeLengthM) {
        if (strokes == null || strokes <= 0L || strokeLengthM <= 0.0) return null;
        return strokes * strokeLengthM;
    }

    // Mean Earth radius (m) for the haversine great-circle distance below.
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /// Great-circle distance in metres between two coordinates. Pure Java (haversine) —
    /// deliberately NOT GPSCoordinate.getDistance, which delegates to
    /// android.location.Location and is unavailable in the exporter's plain-JVM unit tests.
    private static double haversineMeters(@NonNull final GPSCoordinate a,
                                          @NonNull final GPSCoordinate b) {
        final double lat1 = Math.toRadians(a.getLatitude());
        final double lat2 = Math.toRadians(b.getLatitude());
        final double dLat = lat2 - lat1;
        final double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        final double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    public void performExport(@Nullable final ActivityTrack track,
                              @NonNull final BaseActivitySummary summary,
                              @Nullable final ActivitySummaryData summaryData,
                              @NonNull final File targetFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            performExport(track, summary, summaryData, fos);
        }
    }

    public void performExport(@Nullable final ActivityTrack track,
                              @NonNull final BaseActivitySummary summary,
                              @Nullable final ActivitySummaryData summaryData,
                              @NonNull final OutputStream outputStream) throws IOException {
        final long startMs = summary.getStartTime().getTime();
        final long endMs = summary.getEndTime() != null ? summary.getEndTime().getTime() : startMs;
        final long startSeconds = startMs / 1000L;
        final long endSeconds = Math.max(startSeconds, endMs / 1000L);
        final long elapsedSeconds = endSeconds - startSeconds;

        if (track == null) {
            LOG.warn("performExport: track is null for summary {} — emitting fallback single-lap shell file",
                    summary.getId());
        } else {
            int totalPoints = 0;
            for (final List<ActivityPoint> seg : track.getSegments()) totalPoints += seg.size();
            LOG.info("performExport: summary {} track has {} segments / {} points",
                    summary.getId(), track.getSegments().size(), totalPoints);
        }

        final List<List<ActivityPoint>> segments = (track != null && !track.getSegments().isEmpty())
                ? track.getSegments()
                : List.of(new ArrayList<>());
        final List<ActivityTrack.SegmentInfo> segmentInfos = (track != null && !track.getSegmentInfos().isEmpty())
                ? track.getSegmentInfos()
                : List.of(new ActivityTrack.SegmentInfo());

        // Count non-empty segments — used to decide whether per-lap aggregates can
        // safely come from the session-level summaryData (only when there is exactly
        // one lap covering the whole session).
        int nonEmptySegments = 0;
        for (final List<ActivityPoint> seg : segments) {
            if (!seg.isEmpty()) nonEmptySegments++;
        }
        final boolean singleSegment = nonEmptySegments <= 1;

        final ActivityKind kind = ActivityKind.fromCode(summary.getActivityKind());
        final Optional<GarminSport> garminSport = GarminSport.fromActivityKind(kind);
        final int sport = garminSport.map(GarminSport::getType).orElse(GarminSport.GENERIC.getType());
        final int subSport = garminSport.map(GarminSport::getSubtype).orElse(GarminSport.GENERIC.getSubtype());
        // Gate GPS-derived distance recovery: only sports with a meaningful distance-over-
        // time (walking, running, cycling, on-water…). Stationary sports never get an
        // invented distance from any stray GPS jitter.
        final boolean locomotion = isLocomotionSport(sport, subSport);

        // Sensor-presence pre-pass: when a track-wide cadence or power stream is all
        // zero, the source has no cadence/power sensor — emitting "0" per record
        // pollutes downstream graphs in Strava/Garmin Connect/Endurain. Detect once
        // up front and pass the flags to buildRecord so it can suppress those fields
        // on every record. HR is already gated on >0 inside buildRecord.
        boolean trackHasCadence = false;
        boolean trackHasPower = false;
        for (final List<ActivityPoint> seg : segments) {
            for (final ActivityPoint p : seg) {
                if (!trackHasCadence && p.getCadence() > 0) trackHasCadence = true;
                if (!trackHasPower && Float.isFinite(p.getPower()) && p.getPower() > 0f) trackHasPower = true;
                if (trackHasCadence && trackHasPower) break;
            }
            if (trackHasCadence && trackHasPower) break;
        }

        final List<RecordData> records = new ArrayList<>();
        records.add(buildFileId(startSeconds));
        records.add(buildFileCreator());
        // wkt_name lets importers (Endurain) display the user-facing activity name
        // instead of falling back to a generic "Workout" label.
        final String workoutName = summary.getName();
        if (workoutName != null && !workoutName.isEmpty()) {
            records.add(buildWorkout(workoutName, sport, subSport));
        }
        records.add(buildEvent(startSeconds, EVENT_TYPE_START));

        final PointAggregates totalAgg = new PointAggregates();
        final List<LapDescriptor> lapDescriptors = new ArrayList<>();
        // Content-aware dedup: drop a record only when its timestamp AND every
        // value-bearing field exactly matches the previously emitted point. Sources
        // that emit multiple records per second with DIFFERENT field values (Strava
        // E-Bike, Wattbike trainers) keep both — the strict 1s timestamp dedup the
        // earlier code did discarded ~50% of those.
        long lastEventTs = startSeconds;
        long lastEmittedSig = 0L;
        boolean haveLastSig = false;
        // Running cumulative GPS distance across the whole track (monotonic), used to fill
        // record.distance when the source supplied GPS positions but no measured per-record
        // distance. Advances on every located point, including deduped duplicates.
        GPSCoordinate prevGpsLoc = null;
        double gpsCumulativeDistance = 0.0;

        for (int s = 0; s < segments.size(); s++) {
            final List<ActivityPoint> seg = segments.get(s);
            if (seg.isEmpty()) continue;

            final ActivityTrack.SegmentInfo info = s < segmentInfos.size()
                    ? segmentInfos.get(s)
                    : new ActivityTrack.SegmentInfo();

            long segStartTs = Long.MAX_VALUE;
            long segEndTs = Long.MIN_VALUE;
            final PointAggregates segAgg = new PointAggregates();

            for (final ActivityPoint p : seg) {
                segAgg.accumulate(p);
                totalAgg.accumulate(p);
                if (p.getTime() == null) continue;
                final long ts = p.getTime().getTime() / 1000L;
                if (ts < segStartTs) segStartTs = ts;
                if (ts > segEndTs) segEndTs = ts;
                // Advance the cumulative GPS distance for this point (before any dedup skip
                // so the running total stays correct across duplicates). Non-located points
                // leave it unchanged and get a null fallback in buildRecord.
                Double gpsDistanceForPoint = null;
                final GPSCoordinate pointLoc = p.getLocation();
                if (pointLoc != null) {
                    if (prevGpsLoc != null) {
                        gpsCumulativeDistance += haversineMeters(prevGpsLoc, pointLoc);
                    }
                    prevGpsLoc = pointLoc;
                    gpsDistanceForPoint = gpsCumulativeDistance;
                }
                // Pause / segment-break markers — most non-Garmin parsers signal these via
                // ActivityPoint.description. Emit a TIMER STOP_ALL event so importers that
                // honour pauses (Strava, Garmin Connect) see them. Skip if a STOP/START
                // event was already emitted at this same second.
                if (p.getDescription() != null && !p.getDescription().isEmpty() && ts != lastEventTs) {
                    records.add(buildEvent(ts, EVENT_TYPE_STOP_ALL));
                    lastEventTs = ts;
                }
                // Skip only when this point is byte-identical to the previously emitted one
                // (same ts, same fields). Keeps multi-record-per-second sources intact.
                final long sig = pointSignature(p);
                if (haveLastSig && sig == lastEmittedSig) continue;
                final RecordData rec = buildRecord(p, trackHasCadence, trackHasPower,
                        locomotion ? gpsDistanceForPoint : null);
                if (rec != null) {
                    records.add(rec);
                    lastEmittedSig = sig;
                    haveLastSig = true;
                }
            }

            if (segStartTs == Long.MAX_VALUE) continue; // no timestamped points in segment
            final long segElapsed = Math.max(0L, segEndTs - segStartTs);
            // Tiny-segment skip: keeps the lap stream meaningful while preserving every
            // record. The first segment always emits a lap so importers that require a
            // lap before the session see one.
            final boolean tiny = (seg.size() < TINY_SEGMENT_MIN_RECORDS || segElapsed < TINY_SEGMENT_MIN_SECONDS)
                    && !lapDescriptors.isEmpty();
            if (tiny) continue;

            lapDescriptors.add(new LapDescriptor(segStartTs, segElapsed, segAgg, info));
        }

        // Per-lap totals come ONLY from per-segment binary fields parsed by the source
        // (SegmentInfo.distanceMeters, SegmentInfo.strokes). When the source does not
        // encode a per-segment metric, the corresponding FIT lap field is omitted —
        // we never derive or distribute session-level aggregates across laps.
        final List<RecordData> lapRecords = new ArrayList<>();
        // Lap-window length count for swim laps. Build once: for each lap descriptor,
        // count FitLength records whose startTime is within [start, start+elapsed).
        // For the no-descriptor fallback path the whole-track length count is used.
        final int totalLengths = (track != null) ? track.getLengths().size() : 0;
        if (lapDescriptors.isEmpty()) {
            // Edge case: no segments yielded a lap (e.g. all empty / no track). Emit one
            // fallback lap covering the whole session so importers always see numLaps >= 1.
            lapRecords.add(buildLap(summaryData, totalAgg, sport, subSport, startSeconds, elapsedSeconds,
                    0, ActivityTrack.SegmentIntensity.UNKNOWN, true, LapTotals.EMPTY,
                    totalLengths > 0 ? totalLengths : null));
        } else {
            for (int i = 0; i < lapDescriptors.size(); i++) {
                final LapDescriptor d = lapDescriptors.get(i);
                final LapTotals overrides = singleSegment
                        ? LapTotals.EMPTY
                        : new LapTotals(
                                d.info.getDistanceMeters() != null
                                        ? d.info.getDistanceMeters().doubleValue() : null,
                                d.info.getStrokes() != null
                                        ? d.info.getStrokes().longValue() : null);
                Integer lapLengthCount = null;
                if (track != null && !track.getLengths().isEmpty()) {
                    int c = 0;
                    final long lapEnd = d.startTs + d.elapsed;
                    for (final ActivityTrack.LengthInfo li : track.getLengths()) {
                        if (li.startTimeSec >= d.startTs && li.startTimeSec < lapEnd) c++;
                    }
                    if (c > 0) lapLengthCount = c;
                }
                lapRecords.add(buildLap(summaryData, d.agg, sport, subSport, d.startTs, d.elapsed,
                        i, d.info.getIntensity(), singleSegment, overrides, lapLengthCount));
            }
        }
        final int emittedLaps = Math.max(1, lapDescriptors.size());

        // Final stop event — skip if a pause STOP_ALL already lands on endSeconds.
        if (endSeconds != lastEventTs) {
            records.add(buildEvent(endSeconds, EVENT_TYPE_STOP_ALL));
        }
        records.addAll(lapRecords);
        // Per-length swim records — only emitted for lap-swimming workouts (sport=5,
        // sub_sport=17). Strava + Endurain render per-length stats in their swim
        // detail views; without these the lap-swimming activity is reduced to a
        // single distance bar.
        if (sport == 5 && subSport == 17 && track != null && !track.getLengths().isEmpty()) {
            int lengthIdx = 0;
            for (final ActivityTrack.LengthInfo li : track.getLengths()) {
                records.add(buildLength(li, lengthIdx++));
            }
        }
        // Per-split records — Garmin Edge/Forerunner emit auto-splits (per km/mile).
        // Strava surfaces them on the lap chart, Endurain reads them for split summary.
        if (track != null && !track.getSplits().isEmpty()) {
            int splitIdx = 0;
            for (final ActivityTrack.SplitInfo si : track.getSplits()) {
                records.add(buildSplit(si, splitIdx++));
            }
        }
        // Per-set records — strength training. Endurain renders these in its workout
        // set table; Garmin Connect uses them to drive the strength workout view.
        if (track != null && !track.getSets().isEmpty()) {
            for (final ActivityTrack.SetInfo si : track.getSets()) {
                records.add(buildSet(si));
            }
        }
        // Sum per-segment strokes across all laps as a fallback when summaryData lacks
        // STROKES — used by buildSession to synthesize rowing distance.
        Long sumLapStrokes = null;
        for (final LapDescriptor d : lapDescriptors) {
            final Integer s = d.info.getStrokes();
            if (s == null) continue;
            if (sumLapStrokes == null) sumLapStrokes = 0L;
            sumLapStrokes += s.longValue();
        }
        records.add(buildSession(summaryData, totalAgg, sport, subSport, startSeconds, elapsedSeconds, emittedLaps, sumLapStrokes,
                track != null ? track.getLengths().size() : 0, summary.getName()));
        records.add(buildActivity(endSeconds, elapsedSeconds));

        final FitFile fitFile = new FitFile(records);
        final byte[] bytes = fitFile.getOutgoingMessage();

        outputStream.write(bytes);

        LOG.info("Exported FIT activity ({} bytes, {} laps) for summary {}",
                bytes.length, emittedLaps, summary.getId());
    }

    /** Per-lap state collected during the segment walk; used in pass 2 to emit laps. */
    private static final class LapDescriptor {
        final long startTs;
        final long elapsed;
        final PointAggregates agg;
        final ActivityTrack.SegmentInfo info;

        LapDescriptor(final long startTs, final long elapsed,
                      final PointAggregates agg,
                      final ActivityTrack.SegmentInfo info) {
            this.startTs = startTs;
            this.elapsed = elapsed;
            this.agg = agg;
            this.info = info;
        }
    }

    /** Per-lap totals taken DIRECTLY from per-segment binary fields parsed by the
     *  source (not derived from session aggregates). When the source does not encode
     *  a per-segment metric, the corresponding field stays null and buildLap omits
     *  the FIT lap field rather than fabricating a value.
     *  {@link #EMPTY} is used for the single-lap path (lap == session, all aggregates
     *  pulled from summaryData). */
    private static final class LapTotals {
        static final LapTotals EMPTY = new LapTotals(null, null);

        @Nullable final Double distance;
        @Nullable final Long strokes;

        LapTotals(@Nullable final Double distance,
                  @Nullable final Long strokes) {
            this.distance = distance;
            this.strokes = strokes;
        }
    }

    private RecordData buildFileId(final long startSeconds) {
        return new FitFileId.Builder()
                .setType(FileType.FILETYPE.ACTIVITY)
                .setManufacturer(manufacturerId)
                .setProduct(0)
                .setSerialNumber(1L)
                .setTimeCreated(startSeconds)
                .setNumber(0)
                .setProductName("Gadgetbridge")
                .build(LMT_FILE_ID);
    }

    private RecordData buildFileCreator() {
        return new FitFileCreator.Builder()
                .setSoftwareVersion(BuildConfig.VERSION_CODE)
                .setHardwareVersion(0)
                .build(LMT_FILE_CREATOR);
    }

    private RecordData buildWorkout(@NonNull final String name, final int sport, final int subSport) {
        return new FitWorkout.Builder()
                .setName(name)
                .setSport(sport)
                .setSubSport(subSport)
                .setNumValidSteps(0)
                .build(LMT_WORKOUT);
    }

    private RecordData buildSplit(@NonNull final ActivityTrack.SplitInfo info,
                                  final int messageIndex) {
        final FitSplit.Builder b = new FitSplit.Builder();
        b.setMessageIndex(messageIndex);
        b.setStartTime(info.startTimeSec);
        if (info.endTimeSec != null) b.setEndTime(info.endTimeSec);
        if (info.splitType != null) b.setSplitType(info.splitType);
        if (info.totalElapsedTimeSec != null) b.setTotalElapsedTime(info.totalElapsedTimeSec);
        if (info.totalTimerTimeSec != null) b.setTotalTimerTime(info.totalTimerTimeSec);
        if (info.totalDistanceMeters != null) b.setTotalDistance(info.totalDistanceMeters);
        if (info.avgSpeedMps != null) b.setAvgSpeed(info.avgSpeedMps);
        if (info.maxSpeedMps != null) b.setMaxSpeed(info.maxSpeedMps);
        if (info.totalAscentMeters != null) b.setTotalAscent(info.totalAscentMeters);
        if (info.totalDescentMeters != null) b.setTotalDescent(info.totalDescentMeters);
        if (info.totalCalories != null) b.setTotalCalories(info.totalCalories);
        if (info.startElevationMeters != null) b.setStartElevation(info.startElevationMeters);
        if (info.startPositionLat != null) b.setStartPositionLat(info.startPositionLat);
        if (info.startPositionLong != null) b.setStartPositionLong(info.startPositionLong);
        if (info.endPositionLat != null) b.setEndPositionLat(info.endPositionLat);
        if (info.endPositionLong != null) b.setEndPositionLong(info.endPositionLong);
        return b.build(LMT_SPLIT);
    }

    private RecordData buildSet(@NonNull final ActivityTrack.SetInfo info) {
        final FitSet.Builder b = new FitSet.Builder();
        if (info.messageIndex != null) b.setMessageIndex(info.messageIndex);
        b.setStartTime(info.startTimeSec);
        b.setTimestamp(info.durationSec != null
                ? info.startTimeSec + Math.round(info.durationSec)
                : info.startTimeSec);
        if (info.durationSec != null) b.setDuration(info.durationSec);
        if (info.repetitions != null) b.setRepetitions(info.repetitions);
        if (info.weightKg != null) b.setWeight(info.weightKg);
        if (info.setType != null) b.setSetType(info.setType);
        if (info.weightDisplayUnit != null) b.setWeightDisplayUnit(info.weightDisplayUnit);
        return b.build(LMT_SET);
    }

    private RecordData buildLength(@NonNull final ActivityTrack.LengthInfo info,
                                   final int messageIndex) {
        final FitLength.Builder b = new FitLength.Builder();
        b.setMessageIndex(messageIndex);
        b.setStartTime(info.startTimeSec);
        b.setTimestamp(info.startTimeSec + (long) Math.round(info.totalElapsedTimeSec));
        b.setEvent(EVENT_TIMER);
        b.setEventType(EVENT_TYPE_STOP);
        b.setTotalElapsedTime(info.totalElapsedTimeSec);
        b.setTotalTimerTime(info.totalTimerTimeSec);
        if (info.totalStrokes != null) b.setTotalStrokes(info.totalStrokes);
        if (info.avgSpeed != null) b.setAvgSpeed(info.avgSpeed);
        if (info.swimStroke != null) b.setSwimStroke(info.swimStroke);
        if (info.lengthType != null) b.setLengthType(info.lengthType);
        if (info.avgSwimmingCadence != null) b.setAvgSwimmingCadence(info.avgSwimmingCadence);
        return b.build(LMT_LENGTH);
    }

    private RecordData buildEvent(final long timestampSeconds, final int eventType) {
        return new FitEvent.Builder()
                .setTimestamp(timestampSeconds)
                .setEvent(EVENT_TIMER)
                .setEventGroup(0)
                .setEventType(eventType)
                .build(LMT_EVENT);
    }

    @Nullable
    private RecordData buildRecord(@NonNull final ActivityPoint p,
                                   final boolean trackHasCadence,
                                   final boolean trackHasPower,
                                   @Nullable final Double gpsCumulativeDistance) {
        if (p.getTime() == null) {
            return null;
        }
        final FitRecord.Builder b = new FitRecord.Builder();
        b.setTimestamp(Math.round(p.getTime().getTime() / 1000.0));

        final GPSCoordinate loc = p.getLocation();
        if (loc != null) {
            b.setLatitude(loc.getLatitude());
            b.setLongitude(loc.getLongitude());
            // gps_accuracy (field 31, UINT8 metres, scale 1 — the byte IS the accuracy in
            // metres, no proportionate scaling). GB stores horizontal accuracy in metres in
            // the GPSCoordinate hdop slot (documented UNIT_METERS in ActivityPoint.Builder),
            // so it maps 1:1. Emit only when the metre value fits the field's valid range;
            // an accuracy worse than 254 m is no usable fix, so omit it rather than saturate
            // (255 = FIT invalid, i.e. field absent anyway).
            if (loc.hasHdop()) {
                final long acc = Math.round(loc.getHdop());
                if (acc >= 0 && acc <= 254) {
                    b.setGpsAccuracy((int) acc);
                }
            }
        }

        final double altitude = p.getAltitude();
        if (altitude > GPSCoordinate.UNKNOWN_ALTITUDE) {
            b.setEnhancedAltitude(altitude);
        }

        if (p.getHeartRate() > 0) {
            b.setHeartRate(p.getHeartRate());
        }

        // Cadence: emit only when the track has at least one non-zero sample; a
        // track-wide stream of zeros means the device has no cadence sensor and the
        // zeros are sentinels, not measurements.
        if (trackHasCadence && p.getCadence() >= 0) {
            b.setCadence(p.getCadence());
        }

        final float speed = p.getSpeed();
        if (Float.isFinite(speed) && speed >= 0f) {
            b.setEnhancedSpeed((double) speed);
        }

        final double distance = p.getDistance();
        if (distance >= 0.0) {
            b.setDistance(distance);
        } else if (gpsCumulativeDistance != null) {
            // No measured per-record distance, but the source had GPS positions — fill the
            // running cumulative haversine distance so the per-record distance stream (used
            // by Strava / Garmin Connect) is present and monotonic. Caller passes null for
            // non-locomotion sports.
            b.setDistance(gpsCumulativeDistance);
        }

        // Same sensor-presence gate as cadence — see comment above.
        final float power = p.getPower();
        if (trackHasPower && Float.isFinite(power) && power >= 0f) {
            b.setPower((int) power);
        }

        final double temperature = p.getTemperature();
        // ActivityPoint default temperature is -273 (i.e. unset)
        if (temperature > -273.0 && Double.isFinite(temperature)) {
            b.setTemperature((int) temperature);
        }

        final double depth = p.getDepth();
        if (depth >= 0.0 && Double.isFinite(depth)) {
            b.setDepth(depth);
        }

        // step_length (FIT field 85, mm). Both current ActivityPoint sources store mm:
        // FitRecord.toActivityPoint round-trips the FIT value (already mm), and
        // ZeppOsActivityDetailsParser converts its cm stride to a mm step. Emit when set
        // (ActivityPoint default is -1 = unset; 0 mm is not a meaningful step).
        final int stepLength = p.getStepLength();
        if (stepLength > 0) {
            b.setStepLength((float) stepLength);
        }

        final float respirationRate = p.getRespiratoryRate();
        if (Float.isFinite(respirationRate) && respirationRate > 0f) {
            b.setEnhancedRespirationRate(respirationRate);
        }

        final float bodyEnergy = p.getBodyEnergy();
        if (Float.isFinite(bodyEnergy) && bodyEnergy >= 0f) {
            b.setBodyBattery((int) bodyEnergy);
        }

        final float stamina = p.getStamina();
        if (Float.isFinite(stamina) && stamina >= 0f) {
            b.setStamina((int) stamina);
        }

        final float cnsToxicity = p.getCnsToxicity();
        if (Float.isFinite(cnsToxicity) && cnsToxicity >= 0f) {
            b.setCnsLoad((int) cnsToxicity);
        }

        final float n2Load = p.getN2Load();
        if (Float.isFinite(n2Load) && n2Load >= 0f) {
            b.setN2Load((int) n2Load);
        }

        // Running dynamics — only emitted when the source carried them. Non-running
        // sports leave these unset on ActivityPoint, so the gates skip the field.
        final float verticalOscillation = p.getVerticalOscillation();
        if (Float.isFinite(verticalOscillation) && verticalOscillation >= 0f) {
            b.setOscillation(verticalOscillation);
        }
        final float stanceTimePercent = p.getStanceTimePercent();
        if (Float.isFinite(stanceTimePercent) && stanceTimePercent >= 0f) {
            b.setStanceTimePercent(stanceTimePercent);
        }
        final float stanceTime = p.getStanceTime();
        if (Float.isFinite(stanceTime) && stanceTime >= 0f) {
            b.setStanceTime(stanceTime);
        }
        final float verticalRatio = p.getVerticalRatio();
        if (Float.isFinite(verticalRatio) && verticalRatio >= 0f) {
            b.setVerticalRatio(verticalRatio);
        }
        final float stanceTimeBalance = p.getStanceTimeBalance();
        if (Float.isFinite(stanceTimeBalance) && stanceTimeBalance >= 0f) {
            b.setStanceTimeBalance(stanceTimeBalance);
        }
        final int performanceCondition = p.getPerformanceCondition();
        if (performanceCondition >= 0) {
            b.setPerformanceCondition(performanceCondition);
        }

        return b.build(LMT_RECORD);
    }

    private RecordData buildLap(@Nullable final ActivitySummaryData summaryData,
                                @NonNull final PointAggregates agg,
                                final int sport,
                                final int subSport,
                                final long startSeconds,
                                final long elapsedSeconds,
                                final int messageIndex,
                                @NonNull final ActivityTrack.SegmentIntensity intensity,
                                final boolean applySummaryAggregates,
                                @NonNull final LapTotals overrides,
                                @Nullable final Integer numLengths) {
        // summaryData is a session-level aggregate. For multi-lap exports we must NOT
        // splat it onto each lap (would falsely claim each lap has the full-session
        // calorie/ascent/etc.). When applySummaryAggregates is false, the aliased
        // local `data` is null so every readX(data, ...) helper falls back to the
        // per-lap PointAggregates value. The pre-computed `overrides` then supplies
        // the distributable session totals (distance, calories, ascent, descent, work).
        final ActivitySummaryData data = applySummaryAggregates ? summaryData : null;

        final FitLap.Builder b = new FitLap.Builder();
        b.setMessageIndex(messageIndex);
        b.setTimestamp(startSeconds + elapsedSeconds);
        b.setStartTime(startSeconds);
        b.setEvent(EVENT_TIMER);
        b.setEventType(EVENT_TYPE_STOP);
        b.setSport(sport);
        b.setLapTrigger(0); // manual
        b.setIntensity(mapIntensity(intensity));
        b.setTotalElapsedTime((double) elapsedSeconds);
        b.setTotalTimerTime((double) elapsedSeconds);
        // num_lengths — gate via sportSupportsNumLengths. Caller pre-computes the count
        // of FitLength records whose startTime falls in the lap's time window.
        if (sportSupportsNumLengths(sport, subSport) && numLengths != null && numLengths > 0) {
            b.setNumLengths(numLengths);
        }

        final GPSCoordinate first = agg.getFirstLocation();
        final GPSCoordinate last = agg.getLastLocation();
        if (first != null) {
            b.setStartLat(first.getLatitude());
            b.setStartLong(first.getLongitude());
        }
        if (last != null) {
            b.setEndLat(last.getLatitude());
            b.setEndLong(last.getLongitude());
        }
        // Bounding box — drives map preview in Strava / Garmin Connect / Endurain.
        // Note: codegen currently maps setNecLat to FIT field 25 and setNecLong to 26
        // (legacy event_group slot); reader uses field 26 for nec_long and 27/28 for
        // swc_lat/long so round-trip is self-consistent. Fix codegen mapping later.
        if (agg.hasBoundingBox()) {
            b.setNecLat(agg.getMaxLat());
            b.setNecLong(agg.getMaxLong());
            b.setSwcLat(agg.getMinLat());
            b.setSwcLong(agg.getMinLong());
        }

        Double distance = first(overrides.distance, readDistanceMeters(data, agg.getLastDistance()));
        // GPS fallback: no measured/summary distance but the lap's points carried GPS
        // positions → use the per-lap cumulative haversine. Gated on locomotion sports.
        if (distance == null && isLocomotionSport(sport, subSport)) {
            distance = agg.getGpsDistance();
        }
        if (overrides.strokes != null) {
            // Per-segment stroke count parsed from device payload (e.g. Xiaomi rowing v4).
            // FIT lap.total_cycles covers strokes for paddle/row sports.
            b.setTotalCycles(overrides.strokes);
        }
        // Rowing-only synth: when no measured distance is available but per-lap strokes are,
        // derive distance from strokes × default stroke length. avg_stroke_distance is set so
        // any FIT consumer that inspects it can tell the value is stroke-derived rather than
        // measured.
        if (distance == null && isRowingSport(sport, subSport) && overrides.strokes != null) {
            final double strokeLen = rowingStrokeLengthMeters(sport, subSport);
            final Double derived = deriveRowingDistanceMeters(overrides.strokes, strokeLen);
            if (derived != null) {
                distance = derived;
                b.setAvgStrokeDistance((int) Math.round(strokeLen * 100.0));
            }
        }
        if (distance != null) {
            b.setTotalDistance(distance);
        }
        // Ascent/descent: prefer the source summary, fall back to GPS-derived elevation
        // (null when the altitude stream was absent/constant, e.g. Xiaomi GPS V1/V2).
        final Double ascent = first(readMeters(data, ActivitySummaryEntries.ASCENT_METERS), agg.getGpsAscent());
        if (ascent != null) {
            b.setTotalAscent((int) Math.round(ascent));
        }
        final Double descent = first(readMeters(data, ActivitySummaryEntries.DESCENT_METERS), agg.getGpsDescent());
        if (descent != null) {
            b.setTotalDescent((int) Math.round(descent));
        }
        final Integer calories = readInt(data, ActivitySummaryEntries.CALORIES_BURNT, null);
        if (calories != null) {
            b.setTotalCalories(calories);
        }
        final Integer avgHr = readInt(data, ActivitySummaryEntries.HR_AVG, agg.getAvgHr());
        if (avgHr != null) {
            b.setAvgHeartRate(avgHr);
        }
        final Integer maxHr = readInt(data, ActivitySummaryEntries.HR_MAX, agg.getMaxHr());
        if (maxHr != null) {
            b.setMaxHeartRate(maxHr);
        }
        Float avgSpeed = first(readMetersPerSecond(data, ActivitySummaryEntries.SPEED_AVG), agg.getAvgSpeed());
        // Pace fallback: when the source provided neither summary SPEED_AVG nor per-record
        // speed, but distance and elapsed time are both known, the FIT importer's pace
        // computation needs an explicit avg_speed (FIT has no per-lap "pace" field — pace is
        // always derived from avg_speed). Skip when an explicit non-zero value is present.
        // Also gated on isLocomotionSport so stopwatch/yoga/strength/breathing don't get a
        // bogus speed invented from any distance the source happened to log.
        if (isLocomotionSport(sport, subSport)
                && (avgSpeed == null || avgSpeed == 0f) && distance != null && distance > 0.0 && elapsedSeconds > 0L) {
            avgSpeed = (float) (distance / (double) elapsedSeconds);
        }
        if (avgSpeed != null) {
            b.setAvgSpeed(avgSpeed);
            b.setEnhancedAvgSpeed(avgSpeed.doubleValue());
        }
        final Float maxSpeed = first(readMetersPerSecond(data, ActivitySummaryEntries.SPEED_MAX), agg.getMaxSpeed());
        if (maxSpeed != null) {
            b.setMaxSpeed(maxSpeed);
            b.setEnhancedMaxSpeed(maxSpeed.doubleValue());
        }
        final Integer avgCadence = readInt(data, ActivitySummaryEntries.CADENCE_AVG, agg.getAvgCadence());
        if (avgCadence != null) {
            b.setAvgCadence(avgCadence);
        }
        final Integer maxCadence = readInt(data, ActivitySummaryEntries.CADENCE_MAX, agg.getMaxCadence());
        if (maxCadence != null) {
            b.setMaxCadence(maxCadence);
        }

        final Integer minHr = readInt(data, ActivitySummaryEntries.HR_MIN, null);
        if (minHr != null) {
            b.setMinHeartRate(minHr);
        }
        // Temperature: prefer summary aggregates, fall back to per-segment point stream.
        final Integer avgTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_AVG, agg.getAvgTemp());
        if (avgTemp != null) {
            b.setAvgTemperature(avgTemp);
        }
        final Integer maxTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_MAX, agg.getMaxTemp());
        if (maxTemp != null) {
            b.setMaxTemperature(maxTemp);
        }
        final Integer minTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_MIN, agg.getMinTemp());
        if (minTemp != null) {
            b.setMinTemperature(minTemp);
        }
        final Double avgAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_AVG);
        if (avgAlt != null) {
            b.setEnhancedAvgAltitude(avgAlt);
        }
        final Double maxAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_MAX);
        if (maxAlt != null) {
            b.setEnhancedMaxAltitude(maxAlt);
        }
        final Double minAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_MIN);
        if (minAlt != null) {
            b.setEnhancedMinAltitude(minAlt);
        }
        // Power: same fallback pattern — derive from points when summary lacks it.
        final Integer avgPower = readInt(data, ActivitySummaryEntries.AVG_POWER, agg.getAvgPower());
        if (avgPower != null) {
            b.setAvgPower(avgPower);
        }
        final Integer maxPower = readInt(data, ActivitySummaryEntries.MAX_POWER, agg.getMaxPower());
        if (maxPower != null) {
            b.setMaxPower(maxPower);
        }
        final Integer normalizedPower = readInt(data, ActivitySummaryEntries.NORMALIZED_POWER, null);
        if (normalizedPower != null) {
            b.setNormalizedPower(normalizedPower);
        }
        final Long totalWork = readLong(data, ActivitySummaryEntries.TOTAL_WORK, null);
        if (totalWork != null) {
            b.setTotalWork(totalWork);
        }
        final Float avgVerticalOscillation = readMillimeters(data, ActivitySummaryEntries.AVG_VERTICAL_OSCILLATION);
        if (avgVerticalOscillation != null) {
            b.setAvgVerticalOscillation(avgVerticalOscillation);
        }
        final Float avgVerticalRatio = readFloat(data, ActivitySummaryEntries.AVG_VERTICAL_RATIO, null);
        if (avgVerticalRatio != null) {
            b.setAvgVerticalRatio(avgVerticalRatio);
        }
        final Float avgStanceTime = readMilliseconds(data, ActivitySummaryEntries.AVG_GROUND_CONTACT_TIME);
        if (avgStanceTime != null) {
            b.setAvgStanceTime(avgStanceTime);
        }
        final Float avgStanceTimeBalance = readFloat(data, ActivitySummaryEntries.AVG_GROUND_CONTACT_TIME_BALANCE, null);
        if (avgStanceTimeBalance != null) {
            b.setAvgStanceTimeBalance(avgStanceTimeBalance);
        }
        final Float avgStepLength = readMillimeters(data, ActivitySummaryEntries.STEP_LENGTH_AVG);
        if (avgStepLength != null) {
            b.setAvgStepLength(avgStepLength);
        }
        final Float stepSpeedLossPercentage = readFloat(data, ActivitySummaryEntries.STEP_SPEED_LOSS_PERCENTAGE, null);
        if (stepSpeedLossPercentage != null) {
            b.setStepSpeedLossPercentage(stepSpeedLossPercentage);
        }
        final Float avgRespiration = readFloat(data, ActivitySummaryEntries.RESPIRATION_AVG, null);
        if (avgRespiration != null) {
            b.setEnhancedAvgRespirationRate(avgRespiration);
        }
        final Float maxRespiration = readFloat(data, ActivitySummaryEntries.RESPIRATION_MAX, null);
        if (maxRespiration != null) {
            b.setEnhancedMaxRespirationRate(maxRespiration);
        }
        // Diving — Lap-level depth aggregates
        final Double avgDepth = readMeters(data, ActivitySummaryEntries.AVG_DEPTH);
        if (avgDepth != null) {
            b.setAvgDepth(avgDepth);
        }
        final Double maxDepth = readMeters(data, ActivitySummaryEntries.MAX_DEPTH);
        if (maxDepth != null) {
            b.setMaxDepth(maxDepth);
        }

        return b.build(LMT_LAP);
    }

    private RecordData buildSession(@Nullable final ActivitySummaryData data,
                                    @NonNull final PointAggregates agg,
                                    final int sport,
                                    final int subSport,
                                    final long startSeconds,
                                    final long elapsedSeconds,
                                    final int numLaps,
                                    @Nullable final Long lapStrokesFallback,
                                    final int numLengths,
                                    @Nullable final String workoutName) {
        final FitSession.Builder b = new FitSession.Builder();
        b.setMessageIndex(0);
        b.setTimestamp(startSeconds + elapsedSeconds);
        b.setStartTime(startSeconds);
        b.setEvent(EVENT_TIMER);
        b.setEventType(EVENT_TYPE_STOP);
        b.setSport(sport);
        b.setSubSport(subSport);
        // SESSION field 7 (total_elapsed_time) is unscaled in NativeFITMessage and stored
        // as-is in ms. Field 8 (total_timer_time) carries scale=1000 — the codec multiplies
        // raw seconds by 1000 on encode, so pass seconds.
        b.setTotalElapsedTime(elapsedSeconds * 1000L);
        b.setTotalTimerTime((double) elapsedSeconds);
        b.setNumLaps(numLaps);
        b.setFirstLapIndex(0);
        b.setTrigger(0); // activity_end

        final GPSCoordinate first = agg.getFirstLocation();
        final GPSCoordinate last = agg.getLastLocation();
        if (first != null) {
            b.setStartLatitude(first.getLatitude());
            b.setStartLongitude(first.getLongitude());
        }
        if (last != null) {
            b.setEndLatitude(last.getLatitude());
            b.setEndLongitude(last.getLongitude());
        }
        // Bounding box across the whole track. Codegen here is correct (29/30/31/32 per
        // FIT spec).
        if (agg.hasBoundingBox()) {
            b.setNecLatitude(agg.getMaxLat());
            b.setNecLongitude(agg.getMaxLong());
            b.setSwcLatitude(agg.getMinLat());
            b.setSwcLongitude(agg.getMinLong());
        }
        // num_lengths — gate via sportSupportsNumLengths. Endurain + Strava show this as
        // the session's "lengths" headline number.
        if (sportSupportsNumLengths(sport, subSport) && numLengths > 0) {
            b.setNumLengths(numLengths);
        }
        // sport_profile_name — Strava uses this as workout title fallback when
        // file_id.product_name is generic. Pass through the user-facing summary name.
        if (workoutName != null && !workoutName.isEmpty()) {
            b.setSportProfileName(workoutName);
        }

        Double distance = readDistanceMeters(data, agg.getLastDistance());
        // GPS fallback: no measured/summary distance but the track carried GPS positions →
        // use the whole-track cumulative haversine. Gated on locomotion sports so a
        // stationary workout never gets an invented distance. Runs before the rowing synth
        // so on-water rowing prefers real GPS distance over the stroke estimate.
        if (distance == null && isLocomotionSport(sport, subSport)) {
            distance = agg.getGpsDistance();
        }
        boolean totalCyclesSet = false;
        // Rowing-only synth: when no measured distance is available, derive from total
        // strokes × default stroke length. Strokes prefer summary STROKES (single
        // authoritative figure parsed from device), fall back to sum of per-lap strokes.
        // total_cycles + avg_stroke_distance let any FIT consumer reconstruct the
        // derivation and tell it apart from a measured distance.
        if (distance == null && isRowingSport(sport, subSport)) {
            final Long totalStrokes = readLong(data, ActivitySummaryEntries.STROKES, lapStrokesFallback);
            final double strokeLen = rowingStrokeLengthMeters(sport, subSport);
            final Double derived = deriveRowingDistanceMeters(totalStrokes, strokeLen);
            if (derived != null) {
                distance = derived;
                b.setTotalCycles(totalStrokes);
                b.setAvgStrokeDistance((float) strokeLen);
                totalCyclesSet = true;
            }
        }
        if (distance != null) {
            b.setTotalDistance(distance);
        }
        // total_cycles (FIT subfield = total_strides for running/walking/hiking,
        // total_strokes for paddle/swim, total_cycles for cycling). Strava reads it
        // as total_strides for running. Skip if rowing branch already set it.
        if (!totalCyclesSet) {
            final Long stepCount = readLong(data, ActivitySummaryEntries.STEPS, null);
            if (stepCount != null) {
                b.setTotalCycles(stepCount);
                totalCyclesSet = true;
            }
        }
        if (!totalCyclesSet) {
            final Long strokeCount = readLong(data, ActivitySummaryEntries.STROKES, lapStrokesFallback);
            if (strokeCount != null) {
                b.setTotalCycles(strokeCount);
            }
        }
        // Ascent/descent: prefer the source summary, fall back to GPS-derived elevation
        // (null when the altitude stream was absent/constant, e.g. Xiaomi GPS V1/V2).
        final Double ascent = first(readMeters(data, ActivitySummaryEntries.ASCENT_METERS), agg.getGpsAscent());
        if (ascent != null) {
            b.setTotalAscent((int) Math.round(ascent));
        }
        final Double descent = first(readMeters(data, ActivitySummaryEntries.DESCENT_METERS), agg.getGpsDescent());
        if (descent != null) {
            b.setTotalDescent((int) Math.round(descent));
        }
        final Integer calories = readInt(data, ActivitySummaryEntries.CALORIES_BURNT, null);
        if (calories != null) {
            b.setTotalCalories(calories);
        }
        final Integer avgHr = readInt(data, ActivitySummaryEntries.HR_AVG, agg.getAvgHr());
        if (avgHr != null) {
            b.setAverageHeartRate(avgHr);
        }
        final Integer maxHr = readInt(data, ActivitySummaryEntries.HR_MAX, agg.getMaxHr());
        if (maxHr != null) {
            b.setMaxHeartRate(maxHr);
        }
        Float avgSpeed = first(readMetersPerSecond(data, ActivitySummaryEntries.SPEED_AVG), agg.getAvgSpeed());
        // Same pace fallback as buildLap — see comment there.
        if (isLocomotionSport(sport, subSport)
                && (avgSpeed == null || avgSpeed == 0f) && distance != null && distance > 0.0 && elapsedSeconds > 0L) {
            avgSpeed = (float) (distance / (double) elapsedSeconds);
        }
        if (avgSpeed != null) {
            b.setAvgSpeed(avgSpeed);
            b.setEnhancedAvgSpeed(avgSpeed.doubleValue());
        }
        final Float maxSpeed = first(readMetersPerSecond(data, ActivitySummaryEntries.SPEED_MAX), agg.getMaxSpeed());
        if (maxSpeed != null) {
            b.setMaxSpeed(maxSpeed);
            b.setEnhancedMaxSpeed(maxSpeed.doubleValue());
        }
        final Integer avgCadence = readInt(data, ActivitySummaryEntries.CADENCE_AVG, agg.getAvgCadence());
        if (avgCadence != null) {
            b.setAvgCadence(avgCadence);
        }
        final Integer maxCadence = readInt(data, ActivitySummaryEntries.CADENCE_MAX, agg.getMaxCadence());
        if (maxCadence != null) {
            b.setMaxCadence(maxCadence);
        }

        // ---- Common extensions ----
        final Integer minHr = readInt(data, ActivitySummaryEntries.HR_MIN, null);
        if (minHr != null) {
            b.setMinHeartRate(minHr);
        }
        // Temperature aggregates: prefer summary, fall back to track-wide point stream.
        final Integer avgTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_AVG, agg.getAvgTemp());
        if (avgTemp != null) {
            b.setAvgTemperature(avgTemp);
        }
        final Integer maxTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_MAX, agg.getMaxTemp());
        if (maxTemp != null) {
            b.setMaxTemperature(maxTemp);
        }
        final Integer minTemp = readInt(data, ActivitySummaryEntries.TEMPERATURE_MIN, agg.getMinTemp());
        if (minTemp != null) {
            b.setMinTemperature(minTemp);
        }
        final Double avgAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_AVG);
        if (avgAlt != null) {
            b.setEnhancedAvgAltitude(avgAlt);
        }
        final Double maxAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_MAX);
        if (maxAlt != null) {
            b.setEnhancedMaxAltitude(maxAlt);
        }
        final Double minAlt = readMeters(data, ActivitySummaryEntries.ALTITUDE_MIN);
        if (minAlt != null) {
            b.setEnhancedMinAltitude(minAlt);
        }
        final Float avgRespiration = readFloat(data, ActivitySummaryEntries.RESPIRATION_AVG, null);
        if (avgRespiration != null) {
            b.setEnhancedAvgRespirationRate(avgRespiration);
        }
        final Float maxRespiration = readFloat(data, ActivitySummaryEntries.RESPIRATION_MAX, null);
        if (maxRespiration != null) {
            b.setEnhancedMaxRespirationRate(maxRespiration);
        }
        final Float minRespiration = readFloat(data, ActivitySummaryEntries.RESPIRATION_MIN, null);
        if (minRespiration != null) {
            b.setEnhancedMinRespirationRate(minRespiration);
        }
        // Power aggregates: prefer summary, fall back to point stream.
        final Integer avgPower = readInt(data, ActivitySummaryEntries.AVG_POWER, agg.getAvgPower());
        if (avgPower != null) {
            b.setAvgPower(avgPower);
        }
        final Integer maxPower = readInt(data, ActivitySummaryEntries.MAX_POWER, agg.getMaxPower());
        if (maxPower != null) {
            b.setMaxPower(maxPower);
        }
        final Integer normalizedPower = readInt(data, ActivitySummaryEntries.NORMALIZED_POWER, null);
        if (normalizedPower != null) {
            b.setNormalizedPower(normalizedPower);
        }
        final Long totalWork = readLong(data, ActivitySummaryEntries.TOTAL_WORK, null);
        if (totalWork != null) {
            b.setTotalWork(totalWork);
        }
        // FIXME: scale unverified — Cmf parser divides by 100 (0-5 range), Xiaomi parser
        // stores raw float of unknown range. FIT total_training_effect expects 0.0-5.0
        // (scale=10). Re-import test against Garmin Connect / Strava needed.
        final Float trainingEffectAerobic = readFloat(data, ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC, null);
        if (trainingEffectAerobic != null) {
            b.setTotalTrainingEffect(trainingEffectAerobic);
        }
        // FIXME: same scale uncertainty as TRAINING_EFFECT_AEROBIC above.
        final Float trainingEffectAnaerobic = readFloat(data, ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC, null);
        if (trainingEffectAnaerobic != null) {
            b.setTotalAnaerobicTrainingEffect(trainingEffectAnaerobic);
        }
        // FIXME: source range unverified (Garmin tags UNIT_NONE). FIT training_stress_score
        // expects natural TSS (scale=10).
        final Float trainingStressScore = readFloat(data, ActivitySummaryEntries.TRAINING_STRESS_SCORE, null);
        if (trainingStressScore != null) {
            b.setTrainingStressScore(trainingStressScore);
        }
        // Training-load score from the device summary → FIT session.training_load_peak
        // (field 168). Stored on the Gadgetbridge side as a Number (typically captured via
        // addShort(WORKOUT_LOAD, UNIT_NONE)); readFloat surfaces it as a Double.
        final Float trainingLoadPeak = readFloat(data, ActivitySummaryEntries.WORKOUT_LOAD, null);
        if (trainingLoadPeak != null) {
            b.setTrainingLoadPeak(trainingLoadPeak.doubleValue());
        }
        // Per-zone seconds → FIT session.time_in_hr_zone (5-element array, scale=1000 — codec
        // multiplies on encode, so pass raw seconds). Mapping: warm-up → idx 0, fat-burn →
        // idx 1 (the conventional "easy" zone), aerobic → idx 2, anaerobic → idx 3,
        // extreme/threshold → idx 4. Emit only when at least one zone has a value; FIT array
        // slots cannot be null individually, so missing zones encode as 0.
        final Long zWarm  = readSeconds(data, ActivitySummaryEntries.HR_ZONE_WARM_UP);
        final Long zEasy  = readSeconds(data, ActivitySummaryEntries.HR_ZONE_FAT_BURN);
        final Long zAer   = readSeconds(data, ActivitySummaryEntries.HR_ZONE_AEROBIC);
        final Long zAnaer = readSeconds(data, ActivitySummaryEntries.HR_ZONE_ANAEROBIC);
        final Long zMax   = readSeconds(data, ActivitySummaryEntries.HR_ZONE_EXTREME);
        if (zWarm != null || zEasy != null || zAer != null || zAnaer != null || zMax != null) {
            b.setTimeInHrZone(new Number[]{
                    nz(zWarm), nz(zEasy), nz(zAer), nz(zAnaer), nz(zMax)
            });
        }
        final Integer hrvSdrr = readInt(data, ActivitySummaryEntries.HRV_SDRR, null);
        if (hrvSdrr != null) {
            b.setHrvSdrr(hrvSdrr);
        }
        final Integer hrvRmssd = readInt(data, ActivitySummaryEntries.HRV_RMSSD, null);
        if (hrvRmssd != null) {
            b.setHrvRmssd(hrvRmssd);
        }
        final Integer recoveryHr = readInt(data, ActivitySummaryEntries.RECOVERY_HR, null);
        if (recoveryHr != null) {
            b.setRecoveryHeartRate(recoveryHr);
        }
        // FIXME: source range unverified — Garmin parser tags UNIT_PERCENTAGE (0-100).
        // FIT workout_feel is 0-100 (scale=1) so likely OK, but other parsers may scale
        // differently.
        final Integer workoutFeel = readInt(data, ActivitySummaryEntries.WORKOUT_FEEL, null);
        if (workoutFeel != null) {
            b.setWorkoutFeel(workoutFeel);
        }
        // FIXME: source range unverified — Garmin tags UNIT_PERCENTAGE but FIT workout_rpe
        // expects 0-10 (scale=10). If Garmin actually stores 0-100, we'd 10x-overshoot.
        final Integer rpe = readInt(data, ActivitySummaryEntries.RATING_OF_PERCEIVED_EXERTION, null);
        if (rpe != null) {
            b.setWorkoutRpe(rpe);
        }
        final Integer avgSpo2 = readInt(data, ActivitySummaryEntries.SPO2_AVG, null);
        if (avgSpo2 != null) {
            b.setAvgSpo2(avgSpo2);
        }
        final Integer avgStress = readInt(data, ActivitySummaryEntries.STRESS_AVG, null);
        if (avgStress != null) {
            b.setAvgStress(avgStress);
        }
        final Integer beginningBodyBattery = readInt(data, ActivitySummaryEntries.BODY_ENERGY_AT_START, null);
        if (beginningBodyBattery != null) {
            b.setBeginningBodyBattery(beginningBodyBattery);
        }
        final Integer endingBodyBattery = readInt(data, ActivitySummaryEntries.BODY_ENERGY_AT_END, null);
        if (endingBodyBattery != null) {
            b.setEndingBodyBattery(endingBodyBattery);
        }
        // FIXME: unit ambiguous — Garmin parser tags UNIT_SECONDS but FIT battery_gain spec
        // is unclear (some sources: percent change, others: minutes). Downstream
        // interpretation varies.
        final Long batteryGain = readLong(data, ActivitySummaryEntries.BATTERY_GAIN, null);
        if (batteryGain != null) {
            b.setBatteryGain(batteryGain);
        }
        // FIXME: FIT solar_intensity scale/unit unverified beyond Garmin parser's
        // UNIT_PERCENTAGE assumption.
        final Float solarIntensity = readFloat(data, ActivitySummaryEntries.SOLAR_INTENSITY, null);
        if (solarIntensity != null) {
            b.setSolarIntensity(solarIntensity);
        }
        final Integer estimatedSweatLoss = readInt(data, ActivitySummaryEntries.ESTIMATED_SWEAT_LOSS, null);
        if (estimatedSweatLoss != null) {
            b.setEstimatedSweatLoss(estimatedSweatLoss);
        }
        final Integer fluidConsumed = readInt(data, ActivitySummaryEntries.FLUID_CONSUMED, null);
        if (fluidConsumed != null) {
            b.setFluidConsumed(fluidConsumed);
        }
        final Integer caloriesConsumed = readInt(data, ActivitySummaryEntries.CALORIES_CONSUMED, null);
        if (caloriesConsumed != null) {
            b.setCaloriesConsumed(caloriesConsumed);
        }
        final Integer restingCalories = readInt(data, ActivitySummaryEntries.CALORIES_RESTING, null);
        if (restingCalories != null) {
            b.setRestingCalories(restingCalories);
        }

        // ---- Running form ----
        final Float avgVerticalOscillation = readMillimeters(data, ActivitySummaryEntries.AVG_VERTICAL_OSCILLATION);
        if (avgVerticalOscillation != null) {
            b.setAvgVerticalOscillation(avgVerticalOscillation);
        }
        final Float avgVerticalRatio = readFloat(data, ActivitySummaryEntries.AVG_VERTICAL_RATIO, null);
        if (avgVerticalRatio != null) {
            b.setAvgVerticalRatio(avgVerticalRatio);
        }
        final Float avgStanceTime = readMilliseconds(data, ActivitySummaryEntries.AVG_GROUND_CONTACT_TIME);
        if (avgStanceTime != null) {
            b.setAvgStanceTime(avgStanceTime);
        }
        final Float avgStanceTimeBalance = readFloat(data, ActivitySummaryEntries.AVG_GROUND_CONTACT_TIME_BALANCE, null);
        if (avgStanceTimeBalance != null) {
            b.setAvgStanceTimeBalance(avgStanceTimeBalance);
        }
        final Float avgStepLength = readMillimeters(data, ActivitySummaryEntries.STEP_LENGTH_AVG);
        if (avgStepLength != null) {
            b.setAvgStepLength(avgStepLength);
        }
        final Float stepSpeedLossPercentage = readFloat(data, ActivitySummaryEntries.STEP_SPEED_LOSS_PERCENTAGE, null);
        if (stepSpeedLossPercentage != null) {
            b.setStepSpeedLossPercentage(stepSpeedLossPercentage);
        }
        final Float stepSpeedLoss = readFloat(data, ActivitySummaryEntries.STEP_SPEED_LOSS, null);
        if (stepSpeedLoss != null) {
            b.setStepSpeedLoss(stepSpeedLoss);
        }

        // ---- Cycling power ----
        // FIXME: Garmin parser stores this as a STRING ("45-55%"), so readInt returns null
        // and nothing emits. Also FIT left_right_balance uses bit-7 as a right-vs-left flag
        // — non-Garmin sources passing a plain percent would need bit-7 set explicitly.
        final Integer leftRightBalance = readInt(data, ActivitySummaryEntries.LEFT_RIGHT_BALANCE, null);
        if (leftRightBalance != null) {
            b.setLeftRightBalance(leftRightBalance);
        }
        // FIXME: Garmin parser stores this as a STRING (range_percentage), so readFloat
        // silently returns null. Only useful for non-Garmin sources that store a Number.
        final Float avgPedalSmoothness = readFloat(data, ActivitySummaryEntries.AVG_PEDAL_SMOOTHNESS, null);
        if (avgPedalSmoothness != null) {
            b.setAvgCombinedPedalSmoothness(avgPedalSmoothness);
        }
        // FIXME: Garmin parser stores this as a STRING — silent null from readFloat. Also
        // we duplicate the value into both setAvgLeftTorqueEffectiveness and
        // setAvgRightTorqueEffectiveness, which loses the L/R split when the source
        // distinguishes them.
        final Float avgTorqueEffectiveness = readFloat(data, ActivitySummaryEntries.AVG_TORQUE_EFFECTIVENESS, null);
        if (avgTorqueEffectiveness != null) {
            b.setAvgLeftTorqueEffectiveness(avgTorqueEffectiveness);
            b.setAvgRightTorqueEffectiveness(avgTorqueEffectiveness);
        }
        // FIXME: source range unverified — assume natural ratio (0-1). FIT intensity_factor
        // scale=1000. Encoder will multiply, so passing 0.85 stores 850 (correct). If a
        // parser stores 85 (percent) we'd 100x-overshoot.
        final Float intensityFactor = readFloat(data, ActivitySummaryEntries.INTENSITY_FACTOR, null);
        if (intensityFactor != null) {
            b.setIntensityFactor(intensityFactor);
        }
        final Integer frontShifts = readInt(data, ActivitySummaryEntries.FRONT_GEAR_SHIFTS, null);
        if (frontShifts != null) {
            b.setFrontShifts(frontShifts);
        }
        final Integer rearShifts = readInt(data, ActivitySummaryEntries.REAR_GEAR_SHIFTS, null);
        if (rearShifts != null) {
            b.setRearShifts(rearShifts);
        }
        final Long standTime = readSeconds(data, ActivitySummaryEntries.STANDING_TIME);
        if (standTime != null) {
            b.setStandTime((double) standTime);
        }
        final Integer standCount = readInt(data, ActivitySummaryEntries.STANDING_COUNT, null);
        if (standCount != null) {
            b.setStandCount(standCount);
        }
        final Integer avgLeftPco = readMillimetersFromInt(data, ActivitySummaryEntries.AVG_LEFT_PCO);
        if (avgLeftPco != null) {
            b.setAvgLeftPco(avgLeftPco);
        }
        final Integer avgRightPco = readMillimetersFromInt(data, ActivitySummaryEntries.AVG_RIGHT_PCO);
        if (avgRightPco != null) {
            b.setAvgRightPco(avgRightPco);
        }

        // ---- Swimming ----
        final Double poolLength = readMeters(data, ActivitySummaryEntries.POOL_LENGTH);
        if (poolLength != null) {
            b.setPoolLength(poolLength.floatValue());
        }
        final Integer swolfAvg = readInt(data, ActivitySummaryEntries.SWOLF_AVG, null);
        if (swolfAvg != null) {
            b.setAvgSwolf(swolfAvg);
        }
        // FIXME: source unit unverified — likely strokes/min. FIT avg_swim_cadence scale=1,
        // unit=strokes/min. If a parser stores Hz (strokes/sec) we'd 60x-undershoot.
        final Float strokeRateAvg = readFloat(data, ActivitySummaryEntries.STROKE_RATE_AVG, null);
        if (strokeRateAvg != null) {
            b.setAvgSwimCadence(strokeRateAvg);
        }
        // Predominant stroke style → FIT session.swim_stroke (enum). Source codes are
        // device-specific; mapXiaomiSwimStyleToFit handles the convention used by Xiaomi
        // band firmware (0=free,1=back,2=breast,3=fly,4=mixed). Unknown sources whose
        // codes already match the FIT enum will pass through unchanged for 0-3.
        final Integer swimStyleRaw = readInt(data, ActivitySummaryEntries.SWIM_STYLE, null);
        final Integer swimStroke = mapXiaomiSwimStyleToFit(swimStyleRaw);
        if (swimStroke != null) {
            b.setSwimStroke(swimStroke);
        }

        // ---- Diving ----
        final Double avgDepth = readMeters(data, ActivitySummaryEntries.AVG_DEPTH);
        if (avgDepth != null) {
            b.setAvgDepth(avgDepth);
        }
        final Double maxDepth = readMeters(data, ActivitySummaryEntries.MAX_DEPTH);
        if (maxDepth != null) {
            b.setMaxDepth(maxDepth);
        }
        final Long surfaceInterval = readSeconds(data, ActivitySummaryEntries.SURFACE_INTERVAL);
        if (surfaceInterval != null) {
            b.setSurfaceInterval(surfaceInterval);
        }
        final Long diveNumber = readLong(data, ActivitySummaryEntries.DIVE_NUMBER, null);
        if (diveNumber != null) {
            b.setDiveNumber(diveNumber);
        }
        final Integer startCns = readInt(data, ActivitySummaryEntries.START_CNS, null);
        if (startCns != null) {
            b.setStartCns(startCns);
        }
        final Integer endCns = readInt(data, ActivitySummaryEntries.END_CNS, null);
        if (endCns != null) {
            b.setEndCns(endCns);
        }
        final Integer startN2 = readInt(data, ActivitySummaryEntries.START_N2, null);
        if (startN2 != null) {
            b.setStartN2(startN2);
        }
        final Integer endN2 = readInt(data, ActivitySummaryEntries.END_N2, null);
        if (endN2 != null) {
            b.setEndN2(endN2);
        }
        final Integer o2Toxicity = readInt(data, ActivitySummaryEntries.OXYGEN_TOXICITY, null);
        if (o2Toxicity != null) {
            b.setO2Toxicity(o2Toxicity);
        }

        // ---- MTB ----
        final Float totalGrit = readFloat(data, ActivitySummaryEntries.MOUNTAIN_BIKE_GRIT_SCORE, null);
        if (totalGrit != null) {
            b.setTotalGrit(totalGrit);
        }
        final Float totalFlow = readFloat(data, ActivitySummaryEntries.MOUNTAIN_BIKE_FLOW_SCORE, null);
        if (totalFlow != null) {
            b.setTotalFlow(totalFlow);
        }

        // ---- Jumps / sets ----
        final Integer jumpCount = readInt(data, ActivitySummaryEntries.JUMPS, null);
        if (jumpCount != null) {
            b.setJumpCount(jumpCount);
        }
        final Integer totalSets = readInt(data, ActivitySummaryEntries.SETS, null);
        if (totalSets != null) {
            b.setTotalSets(totalSets);
        }

        return b.build(LMT_SESSION);
    }

    private RecordData buildActivity(final long endSeconds, final long elapsedSeconds) {
        // NativeFITMessage.ACTIVITY field 0 (total_timer_time) is declared without scale —
        // FIT spec is scale=1000 unit=s, so pre-multiply seconds → milliseconds.
        // Field 5 (local_timestamp) likewise lacks the TIMESTAMP marker, so the encoder
        // does not subtract the Garmin epoch — do it manually so importers read the
        // right wall-clock time (we don't track timezone offset, so use endSeconds as-is).
        return new FitActivity.Builder()
                .setTimestamp(endSeconds)
                .setLocalTimestamp(endSeconds - GarminTimeUtils.GARMIN_TIME_EPOCH)
                .setTotalTimerTime(elapsedSeconds * 1000L)
                .setNumSessions(1)
                .setType(ACTIVITY_TYPE_MANUAL)
                .setEvent(EVENT_ACTIVITY)
                .setEventType(EVENT_TYPE_STOP)
                .setEventGroup(0)
                .build(LMT_ACTIVITY);
    }

    /// Single-pass accumulator for per-point aggregates needed by Lap and Session.
    /// Same gating rules as the previous standalone helpers; getters return {@code null}
    /// when no valid samples were seen, matching the prior behaviour exactly.
    private static final class PointAggregates {
        private GPSCoordinate firstLocation;
        private GPSCoordinate lastLocation;
        private Double lastDistance;
        private long hrSum;
        private int hrCount;
        private int hrMax;
        private double speedSum;
        private int speedCount;
        private float speedMax = -1f;
        private long cadSum;
        private int cadCount;
        private int cadMax;
        // Bounding box across all GPS samples seen. NEC = north-east corner (max lat,
        // max long); SWC = south-west corner (min lat, min long). FIT lap+session use
        // these to drive the map preview in Garmin Connect / Strava.
        private double minLat = Double.POSITIVE_INFINITY;
        private double maxLat = Double.NEGATIVE_INFINITY;
        private double minLong = Double.POSITIVE_INFINITY;
        private double maxLong = Double.NEGATIVE_INFINITY;
        // Temperature: skip the −273 sentinel ActivityPoint uses to mean "unset".
        private double tempSum;
        private int tempCount;
        private int tempMax = Integer.MIN_VALUE;
        private int tempMin = Integer.MAX_VALUE;
        // Power. ActivityPoint defaults to −1; only count finite, non-negative samples.
        private double powerSum;
        private int powerCount;
        private float powerMax = -1f;
        // GPS-derived distance: cumulative haversine over consecutive located points, used
        // as the last-resort fallback when the source supplied positions but no measured
        // distance (DISTANCE_METERS / per-record distance).
        private GPSCoordinate prevLocForDistance;
        private double gpsDistanceSum;
        // GPS-derived elevation: ascent/descent summed from altitude deltas between
        // consecutive altitude-bearing points, plus min/max to gate emission. altMin==altMax
        // (e.g. Xiaomi GPS V1/V2 which store a constant altitude=0) → no real elevation data,
        // so the getters return null and the exporter omits ascent/descent rather than 0.
        private double altMin = Double.POSITIVE_INFINITY;
        private double altMax = Double.NEGATIVE_INFINITY;
        private double ascentSum;
        private double descentSum;
        private double prevAltForElevation = Double.NaN;

        void accumulate(@NonNull final ActivityPoint p) {
            final GPSCoordinate loc = p.getLocation();
            if (loc != null) {
                if (firstLocation == null) firstLocation = loc;
                lastLocation = loc;
                final double lat = loc.getLatitude();
                final double lon = loc.getLongitude();
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lon < minLong) minLong = lon;
                if (lon > maxLong) maxLong = lon;
                if (prevLocForDistance != null) {
                    gpsDistanceSum += haversineMeters(prevLocForDistance, loc);
                }
                prevLocForDistance = loc;
                if (loc.hasAltitude()) {
                    final double alt = loc.getAltitude();
                    if (alt < altMin) altMin = alt;
                    if (alt > altMax) altMax = alt;
                    if (!Double.isNaN(prevAltForElevation)) {
                        final double dAlt = alt - prevAltForElevation;
                        if (dAlt > 0) ascentSum += dAlt;
                        else descentSum += -dAlt;
                    }
                    prevAltForElevation = alt;
                }
            }

            final double dist = p.getDistance();
            if (dist > 0.0) {
                lastDistance = dist;
            }

            final int hr = p.getHeartRate();
            if (hr > 0) {
                hrSum += hr;
                hrCount++;
                if (hr > hrMax) hrMax = hr;
            }

            final float speed = p.getSpeed();
            if (Float.isFinite(speed) && speed >= 0f) {
                speedSum += speed;
                speedCount++;
                if (speed > speedMax) speedMax = speed;
            }

            final int cad = p.getCadence();
            if (cad > 0) {
                cadSum += cad;
                cadCount++;
                if (cad > cadMax) cadMax = cad;
            }

            final double temp = p.getTemperature();
            if (Double.isFinite(temp) && temp > -273.0) {
                final int t = (int) Math.round(temp);
                tempSum += t;
                tempCount++;
                if (t > tempMax) tempMax = t;
                if (t < tempMin) tempMin = t;
            }

            final float power = p.getPower();
            if (Float.isFinite(power) && power >= 0f) {
                powerSum += power;
                powerCount++;
                if (power > powerMax) powerMax = power;
            }
        }

        @Nullable GPSCoordinate getFirstLocation() { return firstLocation; }
        @Nullable GPSCoordinate getLastLocation()  { return lastLocation; }
        @Nullable Double        getLastDistance()  { return lastDistance; }
        @Nullable Integer       getAvgHr()         { return hrCount > 0 ? (int) (hrSum / hrCount) : null; }
        @Nullable Integer       getMaxHr()         { return hrMax > 0 ? hrMax : null; }
        @Nullable Float         getAvgSpeed()      { return speedCount > 0 ? (float) (speedSum / speedCount) : null; }
        @Nullable Float         getMaxSpeed()      { return speedMax >= 0f ? speedMax : null; }
        @Nullable Integer       getAvgCadence()    { return cadCount > 0 ? (int) (cadSum / cadCount) : null; }
        @Nullable Integer       getMaxCadence()    { return cadMax > 0 ? cadMax : null; }

        boolean   hasBoundingBox() { return Double.isFinite(minLat) && Double.isFinite(minLong); }
        double    getMaxLat()      { return maxLat; }
        double    getMinLat()      { return minLat; }
        double    getMaxLong()     { return maxLong; }
        double    getMinLong()     { return minLong; }

        @Nullable Integer getAvgTemp() { return tempCount > 0 ? (int) Math.round(tempSum / tempCount) : null; }
        @Nullable Integer getMaxTemp() { return tempCount > 0 ? tempMax : null; }
        @Nullable Integer getMinTemp() { return tempCount > 0 ? tempMin : null; }

        @Nullable Integer getAvgPower() { return powerCount > 0 ? (int) Math.round(powerSum / powerCount) : null; }
        @Nullable Integer getMaxPower() { return powerCount > 0 ? (int) Math.round((double) powerMax) : null; }

        /// Cumulative GPS (haversine) distance in metres, or null when no located points
        /// contributed a segment (fewer than two positions).
        @Nullable Double getGpsDistance() { return gpsDistanceSum > 0.0 ? gpsDistanceSum : null; }
        /// GPS-derived ascent/descent in metres, or null when the altitude stream was
        /// absent or constant (e.g. all-zero) so there is no real elevation profile.
        @Nullable Double getGpsAscent()  { return altMax > altMin ? ascentSum : null; }
        @Nullable Double getGpsDescent() { return altMax > altMin ? descentSum : null; }
    }

    @Nullable
    private static Integer readInt(@Nullable final ActivitySummaryData data,
                                   @NonNull final String key,
                                   @Nullable final Integer fallback) {
        if (data != null) {
            final Number n = data.getNumber(key, null);
            if (n != null) return n.intValue();
        }
        return fallback;
    }

    @Nullable
    private static Float readFloat(@Nullable final ActivitySummaryData data,
                                   @NonNull final String key,
                                   @Nullable final Float fallback) {
        if (data != null) {
            final Number n = data.getNumber(key, null);
            if (n != null) return n.floatValue();
        }
        return fallback;
    }

    @Nullable
    private static Double readDouble(@Nullable final ActivitySummaryData data,
                                     @NonNull final String key,
                                     @Nullable final Double fallback) {
        if (data != null) {
            final Number n = data.getNumber(key, null);
            if (n != null) return n.doubleValue();
        }
        return fallback;
    }

    @Nullable
    private static Long readLong(@Nullable final ActivitySummaryData data,
                                 @NonNull final String key,
                                 @Nullable final Long fallback) {
        if (data != null) {
            final Number n = data.getNumber(key, null);
            if (n != null) return n.longValue();
        }
        return fallback;
    }

    /// Returns {@code a} if non-null, else {@code b}. Used to combine a unit-aware
    /// summaryData read with a point-aggregate fallback.
    @Nullable
    private static <T> T first(@Nullable final T a, @Nullable final T b) {
        return a != null ? a : b;
    }

    /// Coerces a nullable Long to a non-null Number (0 for null). Used by setTimeInHrZone
    /// where individual array slots cannot be null but missing zones should encode as 0.
    private static Number nz(@Nullable final Long v) {
        return v != null ? v : 0L;
    }

    /// Maps a Xiaomi-band SWIM_STYLE byte to the FIT swim_stroke enum.
    /// Xiaomi convention (empirical): 0=freestyle, 1=backstroke, 2=breaststroke,
    /// 3=butterfly, 4=mixed. FIT enum: 0=freestyle, 1=backstroke, 2=breaststroke,
    /// 3=butterfly, 4=drill, 5=mixed, 6=IM. Returns null for unknown codes so callers
    /// can omit the field entirely.
    @Nullable
    private static Integer mapXiaomiSwimStyleToFit(@Nullable final Integer raw) {
        if (raw == null) return null;
        switch (raw) {
            case 0: return 0; // freestyle
            case 1: return 1; // backstroke
            case 2: return 2; // breaststroke
            case 3: return 3; // butterfly
            case 4: return 5; // mixed
            default: return null;
        }
    }

    /// Hash-based fingerprint of an ActivityPoint's value-bearing fields. Used by
    /// the content-aware dedup in performExport — two consecutive points sharing the
    /// same fingerprint are considered identical and only the first is emitted. Two
    /// points with different fingerprints are always emitted, even when their
    /// timestamps collide (multi-record-per-second sources like Wattbike trainers).
    private static long pointSignature(@NonNull final ActivityPoint p) {
        final GPSCoordinate loc = p.getLocation();
        final long ts = p.getTime() != null ? p.getTime().getTime() : 0L;
        return java.util.Objects.hash(
                ts,
                loc != null ? loc.getLatitude() : null,
                loc != null ? loc.getLongitude() : null,
                p.getAltitude(),
                p.getHeartRate(),
                p.getCadence(),
                p.getSpeed(),
                p.getDistance(),
                p.getPower(),
                p.getTemperature(),
                p.getDepth(),
                p.getRespiratoryRate(),
                p.getBodyEnergy(),
                p.getStamina(),
                p.getCnsToxicity(),
                p.getN2Load(),
                p.getVerticalOscillation(),
                p.getStanceTimePercent(),
                p.getStanceTime(),
                p.getVerticalRatio(),
                p.getStanceTimeBalance(),
                p.getPerformanceCondition());
    }

    /// Reads workout distance in meters, preferring DISTANCE_METERS_CALIBRATED (when
    /// non-zero) over DISTANCE_METERS — per Xiaomi parser comment, the calibrated value
    /// "displaces the raw DISTANCE_METERS for display" when calibration was applied.
    /// Falls back to the point-aggregate last distance when no summary value is set.
    @Nullable
    private static Double readDistanceMeters(@Nullable final ActivitySummaryData data,
                                             @Nullable final Double pointFallback) {
        final Double calibrated = readMeters(data, ActivitySummaryEntries.DISTANCE_METERS_CALIBRATED);
        if (calibrated != null && calibrated > 0.0) {
            return calibrated;
        }
        return first(readMeters(data, ActivitySummaryEntries.DISTANCE_METERS), pointFallback);
    }

    /// Reads (value, unit) for a numeric entry, or null if absent / non-numeric.
    /// Returned array is {@code [Number value, String unit]}.
    @Nullable
    private static Object[] readEntry(@Nullable final ActivitySummaryData data,
                                      @NonNull final String key) {
        if (data == null) return null;
        final ActivitySummaryEntry entry = data.get(key);
        if (!(entry instanceof ActivitySummarySimpleEntry simple)) return null;
        final Object value = simple.getValue();
        if (!(value instanceof Number)) return null;
        return new Object[]{ value, simple.getUnit() };
    }

    /// Reads a length value and converts it to millimetres, regardless of the
    /// source unit (mm, cm, m, km). Required for FIT fields like step_length and
    /// avg_vertical_oscillation where the natural unit is mm but Xiaomi stores cm.
    @Nullable
    private static Float readMillimeters(@Nullable final ActivitySummaryData data,
                                         @NonNull final String key) {
        final Object[] entry = readEntry(data, key);
        if (entry == null) return null;
        final double v = ((Number) entry[0]).doubleValue();
        final String unit = (String) entry[1];
        if (unit == null) return (float) v; // assume already mm
        return switch (unit) {
            case ActivitySummaryEntries.UNIT_MM -> (float) v;
            case ActivitySummaryEntries.UNIT_CM -> (float) (v * 10.0);
            case ActivitySummaryEntries.UNIT_METERS -> (float) (v * 1000.0);
            case ActivitySummaryEntries.UNIT_KILOMETERS -> (float) (v * 1_000_000.0);
            default -> {
                LOG.warn("Unknown length unit '{}' for key {}, assuming mm", unit, key);
                yield (float) v;
            }
        };
    }

    /// Same as {@link #readMillimeters} but returns Integer (for FIT fields like
    /// avg_left/right_pco where the builder takes Integer mm).
    @Nullable
    private static Integer readMillimetersFromInt(@Nullable final ActivitySummaryData data,
                                                  @NonNull final String key) {
        final Float mm = readMillimeters(data, key);
        return mm == null ? null : Math.round(mm);
    }

    /// Reads a length and converts to metres (mm/cm/m/km accepted).
    @Nullable
    private static Double readMeters(@Nullable final ActivitySummaryData data,
                                     @NonNull final String key) {
        final Object[] entry = readEntry(data, key);
        if (entry == null) return null;
        final double v = ((Number) entry[0]).doubleValue();
        final String unit = (String) entry[1];
        if (unit == null) return v;
        return switch (unit) {
            case ActivitySummaryEntries.UNIT_METERS -> v;
            case ActivitySummaryEntries.UNIT_KILOMETERS -> v * 1000.0;
            case ActivitySummaryEntries.UNIT_CM -> v / 100.0;
            case ActivitySummaryEntries.UNIT_MM -> v / 1000.0;
            default -> {
                LOG.warn("Unknown length unit '{}' for key {}, assuming m", unit, key);
                yield v;
            }
        };
    }

    /// Reads a speed and converts to m/s (m/s, km/h, knots, cm/s accepted).
    @Nullable
    private static Float readMetersPerSecond(@Nullable final ActivitySummaryData data,
                                             @NonNull final String key) {
        final Object[] entry = readEntry(data, key);
        if (entry == null) return null;
        final double v = ((Number) entry[0]).doubleValue();
        final String unit = (String) entry[1];
        if (unit == null) return (float) v;
        return switch (unit) {
            case ActivitySummaryEntries.UNIT_METERS_PER_SECOND -> (float) v;
            case ActivitySummaryEntries.UNIT_KMPH -> (float) (v / 3.6);
            case ActivitySummaryEntries.UNIT_KNOTS -> (float) (v * 0.514444);
            case ActivitySummaryEntries.UNIT_CENTIMETERS_PER_SECOND -> (float) (v / 100.0);
            case ActivitySummaryEntries.UNIT_METERS_PER_HOUR -> (float) (v / 3600.0);
            default -> {
                LOG.warn("Unknown speed unit '{}' for key {}, assuming m/s", unit, key);
                yield (float) v;
            }
        };
    }

    /// Reads a duration and converts to milliseconds (ms, s, min, h accepted).
    @Nullable
    private static Float readMilliseconds(@Nullable final ActivitySummaryData data,
                                          @NonNull final String key) {
        final Object[] entry = readEntry(data, key);
        if (entry == null) return null;
        final double v = ((Number) entry[0]).doubleValue();
        final String unit = (String) entry[1];
        if (unit == null) return (float) v;
        return switch (unit) {
            case ActivitySummaryEntries.UNIT_MILLISECONDS -> (float) v;
            case ActivitySummaryEntries.UNIT_SECONDS -> (float) (v * 1000.0);
            case ActivitySummaryEntries.UNIT_HOURS -> (float) (v * 3_600_000.0);
            default -> {
                LOG.warn("Unknown duration unit '{}' for key {}, assuming ms", unit, key);
                yield (float) v;
            }
        };
    }

    /// Reads a duration and converts to seconds (ms, s, min, h accepted).
    @Nullable
    private static Long readSeconds(@Nullable final ActivitySummaryData data,
                                    @NonNull final String key) {
        final Object[] entry = readEntry(data, key);
        if (entry == null) return null;
        final double v = ((Number) entry[0]).doubleValue();
        final String unit = (String) entry[1];
        if (unit == null) return Math.round(v);
        return switch (unit) {
            case ActivitySummaryEntries.UNIT_SECONDS -> Math.round(v);
            case ActivitySummaryEntries.UNIT_MILLISECONDS -> Math.round(v / 1000.0);
            case ActivitySummaryEntries.UNIT_HOURS -> Math.round(v * 3600.0);
            default -> {
                LOG.warn("Unknown duration unit '{}' for key {}, assuming s", unit, key);
                yield Math.round(v);
            }
        };
    }
}
