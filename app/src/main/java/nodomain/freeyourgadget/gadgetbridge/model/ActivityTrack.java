/*  Copyright (C) 2017-2024 Carsten Pfeiffer, José Rebelo, Dany Mestas

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;

public class ActivityTrack {

    /** Intensity tag for a workout segment / interval. ACTIVE = work phase,
     *  REST = recovery / pause. UNKNOWN if the source parser did not provide it. */
    public enum SegmentIntensity {
        ACTIVE, REST, UNKNOWN
    }

    /** Per-segment metadata. Populated by parsers that expose interval structure
     *  (e.g. Xiaomi rowing's active/rest phase byte). Length always equals
     *  {@link #segments} length.
     *
     *  Optional per-segment metric fields ({@link #distanceMeters}, {@link #strokes})
     *  carry values that are PARSED DIRECTLY from the device's per-segment binary
     *  payload — they are never derived or distributed from session-level aggregates.
     *  When the source does not encode a per-segment metric, the field stays null and
     *  the FIT exporter omits the corresponding lap field rather than fabricating it. */
    public static final class SegmentInfo {
        private final SegmentIntensity intensity;
        private final Integer distanceMeters;
        private final Integer strokes;

        public SegmentInfo() {
            this(SegmentIntensity.UNKNOWN, null, null);
        }

        public SegmentInfo(final SegmentIntensity intensity) {
            this(intensity, null, null);
        }

        public SegmentInfo(final SegmentIntensity intensity,
                           final Integer distanceMeters,
                           final Integer strokes) {
            this.intensity = intensity != null ? intensity : SegmentIntensity.UNKNOWN;
            this.distanceMeters = distanceMeters;
            this.strokes = strokes;
        }

        public SegmentIntensity getIntensity() {
            return intensity;
        }

        public Integer getDistanceMeters() {
            return distanceMeters;
        }

        public Integer getStrokes() {
            return strokes;
        }
    }

    /** Per-length swim metadata captured from FIT length messages (msg 101). Carried
     *  on the track so the exporter can re-emit length records for lap-swimming
     *  workouts (sport=5, sub_sport=17). All fields are nullable — the source
     *  parser populates whichever the device wrote. Length-level data is independent
     *  of segments: a single lap usually contains multiple lengths (one per pool
     *  length) which Strava + Endurain both render in their swim detail views. */
    /** Per-split metadata captured from FIT split messages (msg 312). Garmin
     *  Edge/Forerunner devices emit one split per auto-paused interval — Strava
     *  surfaces these in the lap chart, Endurain in its split summary. Carried on
     *  the track so the exporter can re-emit split records on round-trip. */
    public static final class SplitInfo {
        public final long startTimeSec;
        public final Long endTimeSec;
        public final Integer splitType;
        public final Double totalElapsedTimeSec;
        public final Double totalTimerTimeSec;
        public final Double totalDistanceMeters;
        public final Double avgSpeedMps;
        public final Double maxSpeedMps;
        public final Integer totalAscentMeters;
        public final Integer totalDescentMeters;
        public final Long totalCalories;
        public final Double startElevationMeters;
        public final Double startPositionLat;
        public final Double startPositionLong;
        public final Double endPositionLat;
        public final Double endPositionLong;

        public SplitInfo(final long startTimeSec,
                         final Long endTimeSec,
                         final Integer splitType,
                         final Double totalElapsedTimeSec,
                         final Double totalTimerTimeSec,
                         final Double totalDistanceMeters,
                         final Double avgSpeedMps,
                         final Double maxSpeedMps,
                         final Integer totalAscentMeters,
                         final Integer totalDescentMeters,
                         final Long totalCalories,
                         final Double startElevationMeters,
                         final Double startPositionLat,
                         final Double startPositionLong,
                         final Double endPositionLat,
                         final Double endPositionLong) {
            this.startTimeSec = startTimeSec;
            this.endTimeSec = endTimeSec;
            this.splitType = splitType;
            this.totalElapsedTimeSec = totalElapsedTimeSec;
            this.totalTimerTimeSec = totalTimerTimeSec;
            this.totalDistanceMeters = totalDistanceMeters;
            this.avgSpeedMps = avgSpeedMps;
            this.maxSpeedMps = maxSpeedMps;
            this.totalAscentMeters = totalAscentMeters;
            this.totalDescentMeters = totalDescentMeters;
            this.totalCalories = totalCalories;
            this.startElevationMeters = startElevationMeters;
            this.startPositionLat = startPositionLat;
            this.startPositionLong = startPositionLong;
            this.endPositionLat = endPositionLat;
            this.endPositionLong = endPositionLong;
        }
    }

    /** Per-set strength-training metadata from FIT set messages (msg 27).
     *  Endurain renders these in its workout-set table. Carried on the track so the
     *  exporter can re-emit set records for strength activities. */
    public static final class SetInfo {
        public final long startTimeSec;
        public final Double durationSec;
        public final Integer repetitions;
        public final Float weightKg;
        public final Integer setType;            // 0=rest, 1=active
        public final Integer weightDisplayUnit;
        public final Integer messageIndex;

        public SetInfo(final long startTimeSec,
                       final Double durationSec,
                       final Integer repetitions,
                       final Float weightKg,
                       final Integer setType,
                       final Integer weightDisplayUnit,
                       final Integer messageIndex) {
            this.startTimeSec = startTimeSec;
            this.durationSec = durationSec;
            this.repetitions = repetitions;
            this.weightKg = weightKg;
            this.setType = setType;
            this.weightDisplayUnit = weightDisplayUnit;
            this.messageIndex = messageIndex;
        }
    }

    public static final class LengthInfo {
        public final long startTimeSec;
        public final double totalElapsedTimeSec;
        public final double totalTimerTimeSec;
        public final Integer totalStrokes;
        public final Float avgSpeed;
        public final Integer swimStroke;
        public final Integer lengthType;
        public final Integer avgSwimmingCadence;

        public LengthInfo(final long startTimeSec,
                          final double totalElapsedTimeSec,
                          final double totalTimerTimeSec,
                          final Integer totalStrokes,
                          final Float avgSpeed,
                          final Integer swimStroke,
                          final Integer lengthType,
                          final Integer avgSwimmingCadence) {
            this.startTimeSec = startTimeSec;
            this.totalElapsedTimeSec = totalElapsedTimeSec;
            this.totalTimerTimeSec = totalTimerTimeSec;
            this.totalStrokes = totalStrokes;
            this.avgSpeed = avgSpeed;
            this.swimStroke = swimStroke;
            this.lengthType = lengthType;
            this.avgSwimmingCadence = avgSwimmingCadence;
        }
    }

    private Date baseTime;
    private Device device;
    private User user;
    private String name;
    private List<ActivityPoint> currentSegment = new ArrayList<>();
    private final List<List<ActivityPoint>> segments = new ArrayList<>() {{
        add(currentSegment);
    }};
    private final List<SegmentInfo> segmentInfos = new ArrayList<>() {{
        add(new SegmentInfo());
    }};
    private final List<LengthInfo> lengths = new ArrayList<>();
    private final List<SplitInfo> splits = new ArrayList<>();
    private final List<SetInfo> sets = new ArrayList<>();

    public void setBaseTime(Date baseTime) {
        this.baseTime = baseTime;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Add a track point to the current segment.
     */
    public void addTrackPoint(final ActivityPoint point) {
        currentSegment.add(point);
    }

    public void addTrackPoints(final Collection<ActivityPoint> points) {
        currentSegment.addAll(points);
    }

    public void startNewSegment() {
        startNewSegment(new SegmentInfo());
    }

    /** Start a new segment with explicit per-segment metadata. Mirrors the no-arg
     *  startNewSegment() (only opens a new segment if the current one is non-empty)
     *  and keeps {@link #segments} / {@link #segmentInfos} length-synchronised. */
    public void startNewSegment(final SegmentInfo info) {
        if (!currentSegment.isEmpty()) {
            currentSegment = new ArrayList<>();
            segments.add(currentSegment);
            segmentInfos.add(info != null ? info : new SegmentInfo());
        }
    }

    /** Replace the SegmentInfo for the current (last) segment. Use when a parser
     *  knows the intensity of the very first segment before adding any further ones. */
    public void setCurrentSegmentInfo(final SegmentInfo info) {
        final SegmentInfo nonNull = info != null ? info : new SegmentInfo();
        if (segmentInfos.isEmpty()) {
            segmentInfos.add(nonNull);
        } else {
            segmentInfos.set(segmentInfos.size() - 1, nonNull);
        }
    }

    /** Replace all segments and per-segment metadata in one atomic step. Lengths must
     *  match. Used when a downstream parser knows segment boundaries that were not
     *  available when the track was first built (e.g. Xiaomi GPS+DETAILS merge,
     *  where segment ranges come from DETAILS but points come from the GPS file).
     *  Reseats {@link #currentSegment} to the trailing entry so subsequent
     *  {@link #addTrackPoint(ActivityPoint)} calls keep targeting the last segment. */
    public void replaceSegments(final List<List<ActivityPoint>> newSegments,
                                final List<SegmentInfo> newSegmentInfos) {
        if (newSegments == null || newSegmentInfos == null
                || newSegments.size() != newSegmentInfos.size()
                || newSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "replaceSegments requires non-empty length-matched lists");
        }
        segments.clear();
        segmentInfos.clear();
        for (int i = 0; i < newSegments.size(); i++) {
            final List<ActivityPoint> seg = newSegments.get(i);
            segments.add(seg != null ? seg : new ArrayList<>());
            final SegmentInfo info = newSegmentInfos.get(i);
            segmentInfos.add(info != null ? info : new SegmentInfo());
        }
        currentSegment = segments.get(segments.size() - 1);
    }

    public List<List<ActivityPoint>> getSegments() {
        return segments;
    }

    public List<SegmentInfo> getSegmentInfos() {
        return segmentInfos;
    }

    public List<LengthInfo> getLengths() {
        return lengths;
    }

    public void addLength(final LengthInfo info) {
        if (info != null) lengths.add(info);
    }

    public List<SplitInfo> getSplits() {
        return splits;
    }

    public void addSplit(final SplitInfo info) {
        if (info != null) splits.add(info);
    }

    public List<SetInfo> getSets() {
        return sets;
    }

    public void addSet(final SetInfo info) {
        if (info != null) sets.add(info);
    }

    public List<ActivityPoint> getAllPoints() {
        return getSegments().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /** Sort points within each segment by timestamp ascending. Use after a merge that
     *  may have appended points out of order, so the chart renders monotonically. */
    public void sortPointsByTime() {
        final Comparator<ActivityPoint> byTime = Comparator.comparing(ActivityPoint::getTime);
        for (final List<ActivityPoint> segment : segments) {
            segment.sort(byTime);
        }
    }

    public Date getBaseTime() {
        return baseTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
