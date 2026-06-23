/*  Copyright (C) 2025 José Rebelo, Martin Schitter, Dany Mestas

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

import android.content.Context;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorkoutDetailsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutDetailsParser.class);

    /** Per-record output of {@link #parseBytes}. Fields are nullable when the version
     *  format does not encode them. Decoupled from {@link XiaomiActivitySample} so parsed
     *  metrics never touch the sample table — {@link nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityTrackProvider}
     *  re-parses raw bytes on demand. */
    public static final class WorkoutDetailRecord {
        public int ts;
        public int hr;
        public Integer steps;
        public Integer spo2;
        public Integer cadence;
        public Integer speedRaw;
        /** True on the first record of each interval/segment, for layouts whose phase
         *  semantics are confirmed (currently rowing v4 only). Drives
         *  {@link ActivityTrack} segment boundaries → one FIT lap per interval. */
        public boolean segmentStart;
        /** Intensity of the segment this record opens. Only meaningful when
         *  {@link #segmentStart} is set; null otherwise. */
        public ActivityTrack.SegmentIntensity segmentIntensity;
        /** Per-segment strokes parsed directly from the segment header (rowing v4).
         *  Only set on a {@link #segmentStart} record; null when not encoded. */
        public Integer segmentStrokes;
    }

    @Override
    public boolean parse(final Context context, final GBDevice gbDevice, final XiaomiActivityFileId fileId, final byte[] bytes) {
        // DETAILS files are not persisted to the sample table — XiaomiActivityTrackProvider
        // re-parses the raw bytes on demand. Returning success here just acks the file.
        return parseBytes(fileId, bytes) != null;
    }

    /**
     * Build an {@link ActivityTrack} from DETAILS binary data for non-GPS workouts.
     * {@link ActivityPoint}s carry HR, cadence, and (v6) speed but no GPS location.
     * Returns null on parse failure or if there are no records.
     */
    @Nullable
    public ActivityTrack getActivityTrack(final XiaomiActivityFileId fileId, final byte[] bytes) {
        final List<WorkoutDetailRecord> records = parseBytes(fileId, bytes);
        if (records == null || records.isEmpty()) {
            return null;
        }
        final ActivityTrack track = new ActivityTrack();
        boolean firstSegment = true;
        for (final WorkoutDetailRecord r : records) {
            if (r.segmentStart) {
                // Parsers that expose confirmed interval phases (rowing v4) mark the first
                // record of each segment. The first marked segment sets the metadata of the
                // implicit initial segment; later ones open new segments → one FIT lap each.
                final ActivityTrack.SegmentInfo info = new ActivityTrack.SegmentInfo(
                        r.segmentIntensity, null, r.segmentStrokes);
                if (firstSegment) {
                    track.setCurrentSegmentInfo(info);
                } else {
                    track.startNewSegment(info);
                }
                firstSegment = false;
            }
            final ActivityPoint.Builder builder = new ActivityPoint.Builder(new Date(r.ts * 1000L));
            applyMetrics(builder, fileId.getVersion(), r);
            // No GPS location for non-GPS activities — map will not render, charts still work
            track.addTrackPoint(builder.build());
        }
        return track;
    }

    /** v6 treadmill: speedRaw is uint24 inverse pace, m/s ≈ 256000 / speedRaw.
     *  Bound: treadmills cap around 25 km/h (~7 m/s); reject anything above 20 m/s as noise. */
    private static void applyMetrics(final ActivityPoint.Builder builder,
                                     final int version,
                                     final WorkoutDetailRecord r) {
        if (r.hr > 0) builder.setHeartRate(r.hr);
        if (r.cadence != null && r.cadence > 0) builder.setCadence(r.cadence);
        if (version == 6 && r.speedRaw != null && r.speedRaw > 0) {
            final float speedMps = 256000f / r.speedRaw;
            if (speedMps > 0f && speedMps < 20f) {
                builder.setSpeed(speedMps);
            }
        } else if (version == 5 && r.speedRaw != null && r.speedRaw > 0) {
            // Treadmill v5 stores belt speed directly in 0.1 km/h units → m/s = raw / 36.
            final float speedMps = r.speedRaw / 36f;
            if (speedMps > 0f && speedMps < 20f) {
                builder.setSpeed(speedMps);
            }
        }
    }

    /** Apply parsed DETAILS metrics onto an existing track (e.g. from GPS) by matching
     *  unix-second timestamps. Used by
     *  {@link nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityTrackProvider}
     *  when a workout has both GPS_TRACK and DETAILS files.
     *
     *  DETAILS records whose timestamp has no matching point in the track (e.g. mid-trip
     *  GPS fix loss, where the watch emits sparse or no GPS records but still records HR
     *  at 1 Hz) are appended as location-less {@link ActivityPoint}s so the HR / cadence
     *  chart stays continuous; the segment is sorted by time afterwards.
     */
    public static void mergeOntoTrack(final ActivityTrack track,
                                      final XiaomiActivityFileId fileId,
                                      final byte[] bytes) {
        final List<WorkoutDetailRecord> records = parseBytes(fileId, bytes);
        if (records == null || records.isEmpty()) return;
        final java.util.Map<Integer, WorkoutDetailRecord> byTs = new java.util.HashMap<>(records.size());
        for (final WorkoutDetailRecord r : records) byTs.put(r.ts, r);
        final int version = fileId.getVersion();
        for (final ActivityPoint p : track.getAllPoints()) {
            final int ts = (int) (p.getTime().getTime() / 1000L);
            final WorkoutDetailRecord r = byTs.remove(ts);
            if (r == null) continue;
            applyMetricsToPoint(p, version, r);
        }
        if (byTs.isEmpty()) return;
        for (final WorkoutDetailRecord r : byTs.values()) {
            final ActivityPoint.Builder builder = new ActivityPoint.Builder(new Date(r.ts * 1000L));
            applyMetrics(builder, version, r);
            track.addTrackPoint(builder.build());
        }
        track.sortPointsByTime();
    }

    private static void applyMetricsToPoint(final ActivityPoint p,
                                            final int version,
                                            final WorkoutDetailRecord r) {
        if (r.hr > 0) p.setHeartRate(r.hr);
        if (r.cadence != null && r.cadence > 0) p.setCadence(r.cadence);
        if (version == 6 && r.speedRaw != null && r.speedRaw > 0) {
            final float speedMps = 256000f / r.speedRaw;
            if (speedMps > 0f && speedMps < 20f) p.setSpeed(speedMps);
        } else if (version == 5 && r.speedRaw != null && r.speedRaw > 0) {
            // Treadmill v5 stores belt speed directly in 0.1 km/h units → m/s = raw / 36.
            final float speedMps = r.speedRaw / 36f;
            if (speedMps > 0f && speedMps < 20f) p.setSpeed(speedMps);
        }
    }

    /**
     * Parse DETAILS binary data into a list of records.
     * Returns null on parse failure (bad version/signature), empty list if no records.
     */
    @Nullable
    public static List<WorkoutDetailRecord> parseBytes(final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        // Layout code keys the segment-header + record-read switches. Defaults to `version`,
        // but signature-keyed sub-dispatch (e.g. v3 FFBB53, v5 FFCFF8BFFF) can override it
        // to a synthetic code (>= 100) so the record loop can read the variant layout.
        int layoutCode = version;
        final int segmentHeaderSize;
        final int recordSize;
        // Byte offsets within the segment header for the record-count and start-timestamp fields.
        // Most layouts derive nrPosition from tsPosition (nr immediately precedes ts), but
        // signature-keyed variants can override both independently.
        final int tsPosition;
        final int nrPosition;
        final byte[] expectedSignature;

        switch (version) {
            case 2:
                // Signature: 0xC0 (1 byte), segment header 9 bytes, 2-byte records: [hr][calories]
                expectedSignature = new byte[]{(byte) 0xC0};
                segmentHeaderSize = 9;
                recordSize = 2;
                tsPosition = 4;
                nrPosition = 0;
                break;
            case 3:
                // v3 reused across sport types. Dispatch by signature.
                if (bytes.length >= 11
                        && bytes[8] == (byte) 0xCC && bytes[9] == (byte) 0xCC && bytes[10] == (byte) 0xC0) {
                    // Existing v3 layout (cycling / running pre-v5): CC CC C0 + 17-byte hdr + 6-byte records.
                    expectedSignature = new byte[]{(byte) 0xCC, (byte) 0xCC, (byte) 0xC0};
                    segmentHeaderSize = 17;
                    recordSize = 6;
                    tsPosition = 8;
                    nrPosition = 4;
                } else if (bytes.length >= 11
                        && bytes[8] == (byte) 0xFF && bytes[9] == (byte) 0xBB && bytes[10] == (byte) 0x53) {
                    // SPORTS_FREESTYLE v3 (Mi Band 8): signature FF BB 53.
                    //   16-byte segment header:
                    //     offset 0:    byte marker (=0x11; semantic unknown)
                    //     offset 1-2:  2 byte pad
                    //     offset 3-6:  int32 ts (start, unix seconds)
                    //     offset 7:    byte phase
                    //     offset 8-11: int32 distance
                    //     offset 12-15: 4 byte pad
                    //   4-byte records: [hr][events][unk][unk]
                    // No reliable nr field in the header — overridden after read to consume the
                    // remaining buffer as a single segment.
                    expectedSignature = new byte[]{(byte) 0xFF, (byte) 0xBB, (byte) 0x53};
                    segmentHeaderSize = 16;
                    recordSize = 4;
                    tsPosition = 3;
                    nrPosition = 12; // arbitrary 4-byte aligned offset; nr is overridden below
                    layoutCode = 103; // synthetic: v3-freestyle record shape
                } else {
                    LOG.warn("Unknown v3 DETAILS signature: {}",
                            GB.hexdump(bytes, 8, Math.min(3, bytes.length - 8)));
                    return null;
                }
                break;
            case 4:
                // v4 is reused across multiple sport types with different payload layouts.
                // Dispatch by the leading signature bytes at offset 8 to pick the format.
                if (bytes.length >= 10 && bytes[8] == (byte) 0xFF && bytes[9] == (byte) 0xFF) {
                    // Rowing: FF FF (2-byte sig) + 13-byte segment header + 3-byte records
                    //   [hr][events][stroke_rate]. Multi-segment, alternating active/rest.
                    // Segment header layout:
                    //   offset 0-3:  int32 nr      — record count for this segment
                    //   offset 4-7:  int32 ts      — segment start, unix seconds
                    //   offset 8:    byte  phase   — 0x81 = active rowing, 0x82 = rest/transition
                    //   offset 9-12: int32 strokes — strokes count for this segment;
                    //                                sum across segments matches summary STROKES.
                    expectedSignature = new byte[]{(byte) 0xFF, (byte) 0xFF};
                    segmentHeaderSize = 13;
                    recordSize = 3;
                    tsPosition = 4;
                    nrPosition = 0;
                } else if (bytes.length >= 13
                        && bytes[8] == (byte) 0xEC && bytes[9] == (byte) 0xCC && bytes[10] == (byte) 0xC8) {
                    // Mi Band 8 walking-style v4: signature EC CC C8 00 00. Layout not yet
                    // decoded; appears to be a type-tagged TLV stream.
                    // Returning null routes the workout to GpxActivityTrackProvider so summary
                    // metrics still render; HR / cadence / SpO2 charts will be empty until a
                    // binary fixture lets us implement the TLV decoder.
                    LOG.info("Mi Band 8 walking-style DETAILS payload (sig EC CC C8) not yet supported; falling back to GPX");
                    return null;
                } else {
                    LOG.warn("Unknown v4 DETAILS signature: {}",
                            GB.hexdump(bytes, 8, Math.min(5, bytes.length - 8)));
                    return null;
                }
                break;
            case 5:
                // v5 reused across sport types. Dispatch by signature.
                if (bytes.length >= 13
                        && bytes[8] == (byte) 0xEC && bytes[9] == (byte) 0xCC && bytes[10] == (byte) 0xC0
                        && bytes[11] == (byte) 0x0C && bytes[12] == (byte) 0xC0) {
                    // Existing v5 layout (cycling / running): EC CC C0 0C C0 + 17-byte hdr + 8-byte records.
                    expectedSignature = new byte[]{(byte) 0xEC, (byte) 0xCC, (byte) 0xC0, (byte) 0x0C, (byte) 0xC0};
                    segmentHeaderSize = 17;
                    recordSize = 8;
                    tsPosition = 8;
                    nrPosition = 4;
                } else if (bytes.length >= 13
                        && bytes[8] == (byte) 0xFF && bytes[9] == (byte) 0xCF && bytes[10] == (byte) 0xF8
                        && bytes[11] == (byte) 0xBF && bytes[12] == (byte) 0xFF) {
                    // Mi Band 8 SPORTS_OUTDOOR_WALKING_V2 v5: signature FF CF F8 BF FF.
                    //   17-byte segment header: 4 pad | int32 nr | int32 ts | byte phase | int32 distance
                    //   13-byte records: [unk][hr][steps_lo][steps_hi][events][cadence][?][?][calories][?][?][?][?]
                    // Phase byte: 0x7F observed (short workouts), 0x81/0x82 across multi-segment runs.
                    // Multi-segment workouts split into active/transition phases similar to v6 treadmill.
                    expectedSignature = new byte[]{(byte) 0xFF, (byte) 0xCF, (byte) 0xF8, (byte) 0xBF, (byte) 0xFF};
                    segmentHeaderSize = 17;
                    recordSize = 13;
                    tsPosition = 8;
                    nrPosition = 4;
                    layoutCode = 105; // synthetic: v5-walking record shape
                } else if (bytes.length >= 13
                        && bytes[8] == (byte) 0xEC && bytes[9] == (byte) 0xCC && bytes[10] == (byte) 0x80
                        && bytes[11] == (byte) 0x28 && bytes[12] == (byte) 0x06) {
                    // SPORTS_TREADMILL v5: signature EC CC 80 28 06.
                    //   115-byte segment header: int32 start ts at offset 2; byte 0x7f at offset 6
                    //     (same 0x7f-only "phase" byte seen in the v6 treadmill header; remainder is zero).
                    //   No record-count field in the header → single-segment fallback below.
                    //   8-byte records — layout decoded in case 205. Validated against the paired
                    //   treadmill summary: mean HR matched HR_AVG and the top speed matched PACE_MAX.
                    expectedSignature = new byte[]{(byte) 0xEC, (byte) 0xCC, (byte) 0x80, (byte) 0x28, (byte) 0x06};
                    segmentHeaderSize = 115;
                    recordSize = 8;
                    tsPosition = 2;
                    nrPosition = 0;
                    layoutCode = 205; // synthetic: treadmill-v5 record shape
                } else {
                    LOG.warn("Unknown v5 DETAILS signature: {}",
                            GB.hexdump(bytes, 8, Math.min(5, bytes.length - 8)));
                    return null;
                }
                break;
            case 6:
                // Signature: FF FF 8B FF (4 bytes), segment header 13 bytes, 12-byte records.
                // Used by treadmill workouts with high-res HR + cadence + raw speed.
                // Segment header layout:
                //   offset 0-3:  int32 nr            — record count for this segment
                //   offset 4-7:  int32 ts            — segment start, unix seconds
                //   offset 8:    byte  phase         — only 0x7f observed; semantic unconfirmed
                //   offset 9-12: int32 distance      — meters; matches summary DISTANCE_METERS
                expectedSignature = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0x8B, (byte) 0xFF};
                segmentHeaderSize = 13;
                recordSize = 12;
                tsPosition = 4;
                nrPosition = 0;
                break;
            case 8:
                // SPORTS_OUTDOOR_WALKING_V2 v8: extended walking layout.
                //   7-byte data-valid bitmap: FF CF F8 BF FB BB BF (one nibble per field group).
                //   27-byte segment header:
                //     offset  0- 3: int32 initHeight  (always 0 in captured data)
                //     offset  4- 7: int32 recordCount
                //     offset  8-11: int32 startTs     (unix seconds)
                //     offset 12:    byte  itState
                //     offset 13-16: int32 itTotalDistance
                //     offset 17-20: int32 itTotalSteps
                //     offset 21-22: int16 itTotalPaces
                //     offset 23-26: int32 itTotalDuration
                //   21-byte records — layout decoded below in case 108.
                expectedSignature = new byte[]{
                        (byte) 0xFF, (byte) 0xCF, (byte) 0xF8, (byte) 0xBF,
                        (byte) 0xFB, (byte) 0xBB, (byte) 0xBF
                };
                segmentHeaderSize = 27;
                recordSize = 21;
                tsPosition = 8;
                nrPosition = 4;
                layoutCode = 108;
                break;
            default:
                LOG.warn("Unable to parse workout details version {}", version);
                return null;
        }

        // Validate signature at byte offset 8 (after 7-byte fileId + 1 padding)
        final byte[] actualSignature = Arrays.copyOfRange(bytes, 8, 8 + expectedSignature.length);
        if (!Arrays.equals(expectedSignature, actualSignature)) {
            LOG.warn("Unexpected signature for v{}: expected {} got {}",
                    version, GB.hexdump(expectedSignature), GB.hexdump(actualSignature));
            return null;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.limit(buf.limit() - 4); // strip CRC32

        buf.get(new byte[7]); // skip fileId
        final byte padding = buf.get();
        if (padding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {}", padding);
        }
        buf.get(new byte[expectedSignature.length]); // skip signature

        final List<WorkoutDetailRecord> records = new ArrayList<>();

        while (buf.position() < buf.limit()) {
            // Read segment header
            if (buf.remaining() < segmentHeaderSize) {
                LOG.warn("Not enough bytes for segment header, stopping");
                break;
            }
            final byte[] segmentHeaderBytes = new byte[segmentHeaderSize];
            buf.get(segmentHeaderBytes);
            final ByteBuffer segHdr = ByteBuffer.wrap(segmentHeaderBytes).order(ByteOrder.LITTLE_ENDIAN);
            int nr = segHdr.getInt(nrPosition);
            int ts = segHdr.getInt(tsPosition);
            // v3 FREESTYLE: the nr field in the segment header is unreliable (observed value 17 vs
            // ~4433 actual records). Treat the file as single-segment and consume all remaining
            // record bytes instead.
            if (layoutCode == 103 || layoutCode == 205) {
                nr = buf.remaining() / recordSize;
            }
            LOG.debug("Segment: {} records starting at ts={}", nr, ts);

            // Per-segment interval metadata — only for layouts with confirmed phase semantics.
            // Rowing v4: header offset 8 = phase (0x81 active / 0x82 rest), offset 9-12 = strokes.
            // Stamped onto the first record of the segment so getActivityTrack can open one
            // ActivityTrack segment (→ FIT lap) per interval.
            final boolean segmentedLayout = layoutCode == 4;
            ActivityTrack.SegmentIntensity segmentIntensity = null;
            Integer segmentStrokes = null;
            if (segmentedLayout) {
                final byte phase = segHdr.get(8);
                segmentIntensity = phase == (byte) 0x81 ? ActivityTrack.SegmentIntensity.ACTIVE
                        : phase == (byte) 0x82 ? ActivityTrack.SegmentIntensity.REST
                        : ActivityTrack.SegmentIntensity.UNKNOWN;
                final int strokes = segHdr.getInt(9);
                segmentStrokes = strokes > 0 ? strokes : null;
            }
            boolean firstInSegment = true;

            final int segmentEnd = buf.position() + nr * recordSize;
            if (segmentEnd > buf.limit()) {
                LOG.warn("Segment claims {} records but only {} bytes remain, clamping",
                        nr, buf.remaining());
            }

            while (buf.position() < Math.min(segmentEnd, buf.limit())) {
                final WorkoutDetailRecord r = new WorkoutDetailRecord();
                r.ts = ts;
                if (segmentedLayout && firstInSegment) {
                    r.segmentStart = true;
                    r.segmentIntensity = segmentIntensity;
                    r.segmentStrokes = segmentStrokes;
                }
                firstInSegment = false;

                switch (layoutCode) {
                    case 2:
                        r.hr = buf.get() & 0xFF;
                        buf.get(); // calories — not stored (see DailyDetailsParser for calorie parsing)
                        break;
                    case 3:
                        buf.get(); // calories
                        r.hr = buf.get() & 0xFF;
                        // Raw speed int32 from device; unit TBD — kept for potential future calibration,
                        // not exposed to ActivityPoint until unit is confirmed empirically.
                        r.speedRaw = buf.getInt();
                        break;
                    case 4:
                        // Rowing 3-byte record. Stroke rate already in strokes/min (matches summary
                        // averageCadence unit). Verified against rowing workout: per-record stroke
                        // rate range 12-71 with avg ~34, summary range typically 30-40 spm.
                        r.hr = buf.get() & 0xFF;
                        buf.get(); // events/flags (mostly 0, occasional 1)
                        r.cadence = buf.get() & 0xFF;
                        break;
                    case 5:
                        r.steps = buf.get() & 0xFF;
                        r.hr = buf.get() & 0xFF;
                        buf.get(); // events/flags
                        buf.get(); // calories — not stored (see DailyDetailsParser for calorie parsing)
                        r.spo2 = buf.get() & 0xFF;
                        r.cadence = buf.get() & 0xFF;
                        buf.getShort(); // pace
                        break;
                    case 6:
                        r.steps = buf.get() & 0xFF;
                        r.hr = buf.get() & 0xFF;
                        buf.get(); // events/flags
                        // Stored as stride rate (strides/min); × 2 yields steps/min to match the
                        // summary's averageCadence unit. Verified against a 9 km/h treadmill workout
                        // whose summary averageCadence = 163 spm matched 2 × mean(byte3) once
                        // warmup/cooldown were included.
                        r.cadence = (buf.get() & 0xFF) * 2;
                        buf.getInt(); // 4 unknown bytes (typically zero)
                        buf.get();    // 1 unknown byte (typically zero)
                        // Raw speed as 3-byte LE uint24; appears to encode inverse pace (≈ 256000 / speedRaw m/s),
                        // not yet calibrated against more workouts.
                        final int speedLo = buf.get() & 0xFF;
                        final int speedMid = buf.get() & 0xFF;
                        final int speedHi = buf.get() & 0xFF;
                        r.speedRaw = speedLo | (speedMid << 8) | (speedHi << 16);
                        break;
                    case 103:
                        // v3 FREESTYLE (Mi Band 8). 4-byte record: HR at byte 0, byte 1 events/flags,
                        // bytes 2-3 unknown (mostly zero; occasional small values).
                        r.hr = buf.get() & 0xFF;
                        buf.get(); // events/flags
                        buf.get(); // unknown
                        buf.get(); // unknown
                        break;
                    case 105:
                        // SPORTS_OUTDOOR_WALKING_V2 v5: 13-byte record.
                        //   byte 0:    nibble-packed: high4=caloriesInc, low4=stepsInc
                        //   byte 1:    HR (uint8 bpm)
                        //   byte 2:    bit-packed: bit7=kmMarker, bit6=heightSign, bit0-5=heightChange
                        //   byte 3:    distanceInc (uint8 dm)
                        //   byte 4:    stride (uint8 cm)
                        //   bytes 5-9: reserved (5 bytes)
                        //   byte 10:   cadence (uint8 spm)
                        //   bytes 11-12: pace (uint16 LE, s/km)
                    {
                        final int caloriesAndSteps = buf.get() & 0xFF;
                        r.steps = caloriesAndSteps & 0x0F;
                        r.hr = buf.get() & 0xFF;
                        buf.get();     // bit-packed flags + heightChange
                        buf.get();     // distanceInc
                        buf.get();     // stride
                        buf.getInt();
                        buf.get();
                        r.cadence = buf.get() & 0xFF;
                        buf.getShort();// pace
                        break;
                    }
                    case 205:
                        // SPORTS_TREADMILL v5: 8-byte record.
                        buf.get();                       // reserved (1 byte)
                        r.hr = buf.get() & 0xFF;
                        buf.get();                       // reserved (1 byte)
                        // Belt speed shown on the treadmill display, in units of 0.1 km/h.
                        r.speedRaw = buf.get() & 0xFF;
                        buf.getInt();                    // reserved (4 bytes)
                        break;
                    case 108:
                        // SPORTS_OUTDOOR_WALKING_V2 v8: 21-byte record. Same prefix as v5 + 8 trailing bytes.
                    {
                        final int caloriesAndSteps = buf.get() & 0xFF;
                        r.steps = caloriesAndSteps & 0x0F;
                        r.hr = buf.get() & 0xFF;
                        buf.get();     // bit-packed flags + heightChange
                        buf.get();     // distanceInc
                        buf.get();     // stride
                        buf.getInt();
                        buf.get();
                        r.cadence = buf.get() & 0xFF;
                        buf.getShort();// pace
                        buf.getShort();
                        buf.getShort();
                        buf.getShort();
                        buf.getShort();
                        break;
                    }
                }

                LOG.trace("Sample: ts={} hr={}", ts, r.hr);
                records.add(r);
                ts++;
            }
        }

        return records;
    }
}
