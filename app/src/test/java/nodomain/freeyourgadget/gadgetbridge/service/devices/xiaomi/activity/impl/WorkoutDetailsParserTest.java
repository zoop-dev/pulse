/*  Copyright (C) 2025 Dany Mestas

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
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutDetailsParser.WorkoutDetailRecord;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class WorkoutDetailsParserTest {
    // SPORTS=1, OUTDOOR_RUNNING subtype=0x01, DETAILS detailType=0x00
    private static final int TYPE_SPORTS = 1;
    private static final int SUBTYPE_OUTDOOR_RUNNING = 0x01;
    private static final int DETAIL_TYPE_DETAILS = 0x00;

    // ---- helpers ----

    private static XiaomiActivityFileId makeFileId(final int version) {
        return new XiaomiActivityFileId(
                new Date(1700000000000L), // arbitrary timestamp
                0,                        // timezone
                TYPE_SPORTS,
                SUBTYPE_OUTDOOR_RUNNING,
                DETAIL_TYPE_DETAILS,
                version
        );
    }

    /**
     * Build a DETAILS binary payload for the given version with one or more segments.
     *
     * @param version  2, 3, 4, 5, or 6
     * @param segments each element is a Segment: {startTs, records[][]}
     *                 records[][] is an int[] per record with field values
     *                 v2: [hr, calories]
     *                 v3: [calories, hr, speed_int32]
     *                 v4: [hr, events, stroke_rate]
     *                 v5: [steps, hr, events, calories, spo2, cadence, pace_int16]
     *                 v6: [steps, hr, events, cadence, speedRaw_int24]
     */
    private static byte[] buildBytes(final int version, final Segment... segments) {
        final byte[] signature;
        final int segmentHeaderSize;
        final int recordSize;
        final int tsPosition;

        switch (version) {
            case 2:
                signature = new byte[]{(byte) 0xC0};
                segmentHeaderSize = 9;
                recordSize = 2;
                tsPosition = 4;
                break;
            case 3:
                signature = new byte[]{(byte) 0xCC, (byte) 0xCC, (byte) 0xC0};
                segmentHeaderSize = 17;
                recordSize = 6;
                tsPosition = 8;
                break;
            case 4:
                signature = new byte[]{(byte) 0xFF, (byte) 0xFF};
                segmentHeaderSize = 13;
                recordSize = 3;
                tsPosition = 4;
                break;
            case 5:
                signature = new byte[]{(byte) 0xEC, (byte) 0xCC, (byte) 0xC0, (byte) 0x0C, (byte) 0xC0};
                segmentHeaderSize = 17;
                recordSize = 8;
                tsPosition = 8;
                break;
            case 6:
                signature = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0x8B, (byte) 0xFF};
                segmentHeaderSize = 13;
                recordSize = 12;
                tsPosition = 4;
                break;
            default:
                throw new IllegalArgumentException("Unknown version: " + version);
        }

        final int nrPosition = tsPosition - 4;

        // Compute total size: 7 (fileId) + 1 (padding) + sig + segments + 4 (CRC)
        int dataSize = 7 + 1 + signature.length;
        for (final Segment seg : segments) {
            dataSize += segmentHeaderSize + seg.records.length * recordSize;
        }
        dataSize += 4; // CRC32

        final ByteBuffer buf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);

        // fileId placeholder (7 bytes) + padding
        buf.put(new byte[7]);
        buf.put((byte) 0);

        // signature
        buf.put(signature);

        // segments
        for (final Segment seg : segments) {
            // Build segment header
            final byte[] hdr = new byte[segmentHeaderSize];
            final ByteBuffer hdrBuf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN);
            hdrBuf.putInt(nrPosition, seg.records.length);
            hdrBuf.putInt(tsPosition, seg.startTs);
            if (version == 4) {
                // Rowing v4 segment header: offset 8 = phase, offset 9-12 = strokes.
                hdrBuf.put(8, (byte) seg.phase);
                hdrBuf.putInt(9, seg.strokes);
            }
            buf.put(hdr);

            // records
            for (final int[] rec : seg.records) {
                switch (version) {
                    case 2:
                        buf.put((byte) rec[0]); // hr
                        buf.put((byte) rec[1]); // calories
                        break;
                    case 3:
                        buf.put((byte) rec[0]); // calories
                        buf.put((byte) rec[1]); // hr
                        buf.putInt(rec[2]);      // speed
                        break;
                    case 4:
                        buf.put((byte) rec[0]); // hr
                        buf.put((byte) rec[1]); // events
                        buf.put((byte) rec[2]); // stroke rate
                        break;
                    case 5:
                        buf.put((byte) rec[0]); // steps
                        buf.put((byte) rec[1]); // hr
                        buf.put((byte) rec[2]); // events
                        buf.put((byte) rec[3]); // calories
                        buf.put((byte) rec[4]); // spo2
                        buf.put((byte) rec[5]); // cadence
                        buf.putShort((short) rec[6]); // pace
                        break;
                    case 6:
                        buf.put((byte) rec[0]); // steps
                        buf.put((byte) rec[1]); // hr
                        buf.put((byte) rec[2]); // events
                        buf.put((byte) rec[3]); // cadence
                        buf.putInt(0);          // 4 unknown bytes (zero)
                        buf.put((byte) 0);      // 1 unknown byte (zero)
                        // speedRaw as 3-byte LE uint24
                        final int spd = rec[4];
                        buf.put((byte) (spd & 0xFF));
                        buf.put((byte) ((spd >> 8) & 0xFF));
                        buf.put((byte) ((spd >> 16) & 0xFF));
                        break;
                }
            }
        }

        // CRC32 over everything except the last 4 bytes
        final byte[] arr = buf.array();
        final int crc = CheckSums.getCRC32(arr, 0, arr.length - 4);
        buf.putInt(crc);

        return buf.array();
    }

    private static class Segment {
        final int startTs;
        final int[][] records;
        final int phase;   // rowing v4 only: 0x81 active / 0x82 rest
        final int strokes; // rowing v4 only: per-segment strokes

        Segment(final int startTs, final int[][] records) {
            this(startTs, records, 0, 0);
        }

        Segment(final int startTs, final int[][] records, final int phase, final int strokes) {
            this.startTs = startTs;
            this.records = records;
            this.phase = phase;
            this.strokes = strokes;
        }
    }

    // ---- tests ----

    @Test
    public void testV2SingleSegment() {
        final int startTs = 1700000100;
        final byte[] bytes = buildBytes(2,
                new Segment(startTs, new int[][]{
                        {72, 5},   // hr=72, calories=5
                        {85, 6},   // hr=85, calories=6
                        {90, 7},   // hr=90, calories=7
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(2), bytes);

        assertNotNull(records);
        assertEquals(3, records.size());

        assertEquals(startTs,     records.get(0).ts);
        assertEquals(72,          records.get(0).hr);

        assertEquals(startTs + 1, records.get(1).ts);
        assertEquals(85,          records.get(1).hr);

        assertEquals(startTs + 2, records.get(2).ts);
        assertEquals(90,          records.get(2).hr);
    }

    @Test
    public void testV3SingleSegment() {
        final int startTs = 1700001000;
        final byte[] bytes = buildBytes(3,
                new Segment(startTs, new int[][]{
                        {10, 120, 500},  // calories=10, hr=120, speed=500
                        {12, 135, 520},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(3), bytes);

        assertNotNull(records);
        assertEquals(2, records.size());

        assertEquals(startTs, records.get(0).ts);
        assertEquals(120,     records.get(0).hr);

        assertEquals(startTs + 1, records.get(1).ts);
        assertEquals(135,         records.get(1).hr);
    }

    @Test
    public void testV5SingleSegment() {
        final int startTs = 1700002000;
        final byte[] bytes = buildBytes(5,
                new Segment(startTs, new int[][]{
                        {3, 155, 0, 8, 98, 170, 360},  // steps=3, hr=155, spo2=98, cadence=170, pace=360
                        {4, 160, 0, 9, 97, 172, 355},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(5), bytes);

        assertNotNull(records);
        assertEquals(2, records.size());

        assertEquals(startTs, records.get(0).ts);
        assertEquals(155,     records.get(0).hr);
        assertEquals(Integer.valueOf(3),  records.get(0).steps);
        assertEquals(Integer.valueOf(98), records.get(0).spo2);

        assertEquals(startTs + 1, records.get(1).ts);
        assertEquals(160,         records.get(1).hr);
        assertEquals(Integer.valueOf(97), records.get(1).spo2);

        assertEquals(Integer.valueOf(170), records.get(0).cadence);
        assertEquals(Integer.valueOf(172), records.get(1).cadence);
    }

    @Test
    public void testV2MultiSegment() {
        final int ts1 = 1700003000;
        final int ts2 = 1700003600; // second segment after a pause

        final byte[] bytes = buildBytes(2,
                new Segment(ts1, new int[][]{
                        {70, 4},
                        {75, 5},
                }),
                new Segment(ts2, new int[][]{
                        {80, 6},
                        {82, 7},
                        {84, 8},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(2), bytes);

        assertNotNull(records);
        assertEquals(5, records.size());

        assertEquals(ts1,     records.get(0).ts);
        assertEquals(70,      records.get(0).hr);
        assertEquals(ts1 + 1, records.get(1).ts);
        assertEquals(75,      records.get(1).hr);

        assertEquals(ts2,     records.get(2).ts);
        assertEquals(80,      records.get(2).hr);
        assertEquals(ts2 + 1, records.get(3).ts);
        assertEquals(ts2 + 2, records.get(4).ts);
        assertEquals(84,      records.get(4).hr);
    }

    @Test
    public void testWrongSignatureReturnsNull() {
        // Build valid v2 bytes, then corrupt the signature byte
        final byte[] bytes = buildBytes(2,
                new Segment(1700000000, new int[][]{{60, 3}}));
        bytes[8] = (byte) 0xAB; // corrupt signature

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(2), bytes);

        assertNull(records);
    }

    @Test
    public void testUnknownVersionReturnsNull() {
        // Version 99 is not supported
        final XiaomiActivityFileId fileId = new XiaomiActivityFileId(
                new Date(1700000000000L), 0, TYPE_SPORTS, SUBTYPE_OUTDOOR_RUNNING, DETAIL_TYPE_DETAILS, 99);

        // Provide minimal byte array (content irrelevant since version check fails first)
        final byte[] bytes = new byte[20];

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(fileId, bytes);

        assertNull(records);
    }

    @Test
    public void testV3PreservesSpeedRaw() {
        final int startTs = 1700006000;
        final byte[] bytes = buildBytes(3,
                new Segment(startTs, new int[][]{
                        {10, 120, 500},   // speedRaw=500
                        {12, 135, 1024},  // speedRaw=1024
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(3), bytes);

        assertNotNull(records);
        assertEquals(2, records.size());
        assertEquals(Integer.valueOf(500),  records.get(0).speedRaw);
        assertEquals(Integer.valueOf(1024), records.get(1).speedRaw);
    }

    @Test
    public void testGetActivityTrackV5() {
        final int startTs = 1700007000;
        final byte[] bytes = buildBytes(5,
                new Segment(startTs, new int[][]{
                        {3, 155, 0, 8, 98, 170, 360},
                        {4, 160, 0, 9, 97, 172, 355},
                }));

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(5), bytes);

        assertNotNull(track);
        final List<ActivityPoint> points = track.getAllPoints();
        assertEquals(2, points.size());

        assertEquals(startTs * 1000L, points.get(0).getTime().getTime());
        assertEquals(155, points.get(0).getHeartRate());
        assertEquals(170, points.get(0).getCadence());
        assertNull(points.get(0).getLocation()); // no GPS for non-GPS activities

        assertEquals((startTs + 1) * 1000L, points.get(1).getTime().getTime());
        assertEquals(160, points.get(1).getHeartRate());
        assertEquals(172, points.get(1).getCadence());

        // Non-rowing layouts have no confirmed interval phases → one segment, no markers.
        assertEquals(1, track.getSegments().size());
        assertNull(records(makeFileId(5), bytes).get(0).segmentIntensity);
    }

    /** Rowing v4 multi-segment → one ActivityTrack segment per interval, with the
     *  active/rest intensity and per-segment strokes carried through to SegmentInfo
     *  so the FIT exporter can emit a lap per interval. */
    @Test
    public void testGetActivityTrackV4MultiSegmentLaps() {
        final int ts1 = 1700005000;
        final int ts2 = ts1 + 350;
        final int ts3 = ts2 + 400;

        final byte[] bytes = buildBytes(4,
                new Segment(ts1, new int[][]{
                        {140, 0, 32},
                        {142, 0, 33},
                }, 0x81, 48),   // active, 48 strokes
                new Segment(ts2, new int[][]{
                        {110, 0, 0},
                        {108, 0, 0},
                }, 0x82, 0),    // rest, no strokes
                new Segment(ts3, new int[][]{
                        {150, 0, 36},
                        {152, 0, 37},
                        {154, 0, 38},
                }, 0x81, 79));  // active, 79 strokes

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(4), bytes);
        assertNotNull(track);

        // One segment per interval; points partitioned by interval.
        assertEquals(3, track.getSegments().size());
        assertEquals(2, track.getSegments().get(0).size());
        assertEquals(2, track.getSegments().get(1).size());
        assertEquals(3, track.getSegments().get(2).size());

        final List<ActivityTrack.SegmentInfo> infos = track.getSegmentInfos();
        assertEquals(3, infos.size());
        assertEquals(ActivityTrack.SegmentIntensity.ACTIVE, infos.get(0).getIntensity());
        assertEquals(ActivityTrack.SegmentIntensity.REST, infos.get(1).getIntensity());
        assertEquals(ActivityTrack.SegmentIntensity.ACTIVE, infos.get(2).getIntensity());
        assertEquals(Integer.valueOf(48), infos.get(0).getStrokes());
        assertNull(infos.get(1).getStrokes()); // 0 strokes → not encoded
        assertEquals(Integer.valueOf(79), infos.get(2).getStrokes());
    }

    private static List<WorkoutDetailRecord> records(final XiaomiActivityFileId id, final byte[] bytes) {
        return WorkoutDetailsParser.parseBytes(id, bytes);
    }

    @Test
    public void testV4SingleSegment() {
        // Rowing: hr 92-164 typical, stroke rate 30-40 spm typical
        final int startTs = 1700004000;
        final byte[] bytes = buildBytes(4,
                new Segment(startTs, new int[][]{
                        // hr, events, stroke_rate
                        {142, 0, 33},
                        {145, 1, 34},
                        {148, 0, 35},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(4), bytes);

        assertNotNull(records);
        assertEquals(3, records.size());

        assertEquals(startTs,     records.get(0).ts);
        assertEquals(142,         records.get(0).hr);
        assertEquals(Integer.valueOf(33), records.get(0).cadence);

        assertEquals(startTs + 1, records.get(1).ts);
        assertEquals(145,         records.get(1).hr);
        assertEquals(Integer.valueOf(34), records.get(1).cadence);

        assertEquals(startTs + 2, records.get(2).ts);
        assertEquals(148,         records.get(2).hr);
        assertEquals(Integer.valueOf(35), records.get(2).cadence);
    }

    @Test
    public void testV4MultiSegment() {
        // Rowing workouts are split into multiple segments (rest periods between rowing intervals)
        final int ts1 = 1700005000;
        final int ts2 = ts1 + 350;

        final byte[] bytes = buildBytes(4,
                new Segment(ts1, new int[][]{
                        {140, 0, 32},
                        {142, 0, 33},
                }),
                new Segment(ts2, new int[][]{
                        {150, 1, 36},
                        {152, 0, 37},
                        {154, 0, 38},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(4), bytes);

        assertNotNull(records);
        assertEquals(5, records.size());

        assertEquals(ts1,     records.get(0).ts);
        assertEquals(140,     records.get(0).hr);
        assertEquals(Integer.valueOf(32), records.get(0).cadence);

        assertEquals(ts2,     records.get(2).ts);
        assertEquals(150,     records.get(2).hr);
        assertEquals(Integer.valueOf(36), records.get(2).cadence);

        assertEquals(ts2 + 2, records.get(4).ts);
        assertEquals(154,     records.get(4).hr);
        assertEquals(Integer.valueOf(38), records.get(4).cadence);
    }

    @Test
    public void testV6SingleSegment() {
        final int startTs = 1700009000;
        final byte[] bytes = buildBytes(6,
                new Segment(startTs, new int[][]{
                        // steps, hr, events, cadence (stride rate, ×2 -> spm), speedRaw (24-bit)
                        {3, 165, 0x15, 82, 201579},
                        {2, 167, 0x24, 84, 194159},
                }));

        final List<WorkoutDetailRecord> records = WorkoutDetailsParser.parseBytes(makeFileId(6), bytes);

        assertNotNull(records);
        assertEquals(2, records.size());

        assertEquals(startTs, records.get(0).ts);
        assertEquals(165,     records.get(0).hr);
        assertEquals(Integer.valueOf(3), records.get(0).steps);
        // 82 strides/min × 2 = 164 spm
        assertEquals(Integer.valueOf(164),    records.get(0).cadence);
        assertEquals(Integer.valueOf(201579), records.get(0).speedRaw);

        assertEquals(startTs + 1, records.get(1).ts);
        assertEquals(167,         records.get(1).hr);
        // 84 strides/min × 2 = 168 spm
        assertEquals(Integer.valueOf(168),    records.get(1).cadence);
        assertEquals(Integer.valueOf(194159), records.get(1).speedRaw);
    }

    @Test
    public void testGetActivityTrackV6() {
        final int startTs = 1700010000;
        final byte[] bytes = buildBytes(6,
                new Segment(startTs, new int[][]{
                        {3, 170, 0x15, 85, 200000},
                        {3, 172, 0x24, 86, 195000},
                }));

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(6), bytes);

        assertNotNull(track);
        final List<ActivityPoint> points = track.getAllPoints();
        assertEquals(2, points.size());

        assertEquals(startTs * 1000L, points.get(0).getTime().getTime());
        assertEquals(170, points.get(0).getHeartRate());
        // 85 strides/min × 2 = 170 spm
        assertEquals(170, points.get(0).getCadence());
        assertNull(points.get(0).getLocation());
        // Speed conversion: 256000 / 200000 = 1.28 m/s
        assertEquals(1.28f, points.get(0).getSpeed(), 0.001f);

        assertEquals((startTs + 1) * 1000L, points.get(1).getTime().getTime());
        assertEquals(172, points.get(1).getHeartRate());
        // 86 strides/min × 2 = 172 spm
        assertEquals(172, points.get(1).getCadence());
        // 256000 / 195000 ≈ 1.3128 m/s
        assertEquals(1.3128f, points.get(1).getSpeed(), 0.001f);
    }

    @Test
    public void testGetActivityTrackV6_dropsZeroSpeed() {
        final int startTs = 1700011000;
        final byte[] bytes = buildBytes(6,
                new Segment(startTs, new int[][]{
                        // speedRaw=0 → treadmill paused, no speed value set
                        {0, 80, 0x00, 0, 0},
                }));

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(6), bytes);

        assertNotNull(track);
        final ActivityPoint p = track.getAllPoints().get(0);
        // ActivityPoint.speed default = -1 when unset
        assertEquals(-1f, p.getSpeed(), 0.0001f);
    }

    @Test
    public void testGetActivityTrackV6_capsUnrealisticSpeed() {
        final int startTs = 1700012000;
        final byte[] bytes = buildBytes(6,
                new Segment(startTs, new int[][]{
                        // speedRaw=10 → 25600 m/s, far above 20 m/s cap, should be dropped
                        {1, 100, 0x00, 80, 10},
                }));

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(6), bytes);

        assertNotNull(track);
        final ActivityPoint p = track.getAllPoints().get(0);
        assertEquals(-1f, p.getSpeed(), 0.0001f);
    }

    @Test
    public void testGetActivityTrackV2() {
        final int startTs = 1700008000;
        final byte[] bytes = buildBytes(2,
                new Segment(startTs, new int[][]{
                        {72, 5},
                        {85, 6},
                }));

        final ActivityTrack track = new WorkoutDetailsParser().getActivityTrack(makeFileId(2), bytes);

        assertNotNull(track);
        final List<ActivityPoint> points = track.getAllPoints();
        assertEquals(2, points.size());
        assertEquals(startTs * 1000L, points.get(0).getTime().getTime());
        assertEquals(72, points.get(0).getHeartRate());
        assertNull(points.get(0).getLocation());
        assertEquals(85, points.get(1).getHeartRate());
    }

    @Test
    public void testMergeOntoTrackPatchesHrAndCadence() {
        // Two ActivityPoints (e.g. from GPS) at ts and ts+1; merge a v4 DETAILS file
        // with overlapping timestamps and verify the points pick up HR + cadence.
        final int startTs = 1700020000;
        final byte[] bytes = buildBytes(4,
                new Segment(startTs, new int[][]{
                        {142, 0, 33},
                        {145, 0, 34},
                }));

        final ActivityPoint p0 = new ActivityPoint(new Date(startTs * 1000L));
        final ActivityPoint p1 = new ActivityPoint(new Date((startTs + 1) * 1000L));
        final ActivityTrack track = new ActivityTrack();
        track.addTrackPoint(p0);
        track.addTrackPoint(p1);

        WorkoutDetailsParser.mergeOntoTrack(track, makeFileId(4), bytes);

        assertEquals(2, track.getAllPoints().size());
        assertEquals(142, p0.getHeartRate());
        assertEquals(33,  p0.getCadence());
        assertEquals(145, p1.getHeartRate());
        assertEquals(34,  p1.getCadence());
    }

    @Test
    public void testMergeOntoTrackAppendsRecordsMissingFromGps() {
        // GPS file lost fix mid-trip: track has points at ts and ts+2, DETAILS has
        // continuous records at ts, ts+1, ts+2. Merge should patch matching points and
        // append a location-less point at ts+1 so the HR chart stays continuous.
        final int startTs = 1700030000;
        final byte[] bytes = buildBytes(4,
                new Segment(startTs, new int[][]{
                        {110, 0, 60},
                        {120, 0, 62}, // ts+1 — no GPS point at this ts
                        {130, 0, 64},
                }));

        final ActivityPoint pGps0 = new ActivityPoint(new Date(startTs * 1000L));
        final ActivityPoint pGps2 = new ActivityPoint(new Date((startTs + 2) * 1000L));
        final ActivityTrack track = new ActivityTrack();
        track.addTrackPoint(pGps0);
        track.addTrackPoint(pGps2);

        WorkoutDetailsParser.mergeOntoTrack(track, makeFileId(4), bytes);

        final List<ActivityPoint> merged = track.getAllPoints();
        assertEquals(3, merged.size());
        assertEquals(startTs * 1000L,       merged.get(0).getTime().getTime());
        assertEquals((startTs + 1) * 1000L, merged.get(1).getTime().getTime());
        assertEquals((startTs + 2) * 1000L, merged.get(2).getTime().getTime());
        assertEquals(110, merged.get(0).getHeartRate());
        assertEquals(120, merged.get(1).getHeartRate());
        assertEquals(130, merged.get(2).getHeartRate());
        // Appended point has HR but no location (GPS fix loss).
        assertNull(merged.get(1).getLocation());
    }

    // ---- treadmill v5 (signature EC CC 80 28 06) ----

    /**
     * Build a treadmill-v5 DETAILS payload. Distinct signature on the same version 5 as the
     * cycling layout, so it needs its own builder. Single segment, no record-count field in
     * the 115-byte header (parser falls back to consuming the whole buffer); start ts lives at
     * header offset 2. Each record is 8 bytes: [reserved][hr][reserved][speed][4×reserved],
     * where speed is in units of 0.1 km/h.
     *
     * @param records each element is {hr, speed01kmh}
     */
    private static byte[] buildTreadmillV5Bytes(final int startTs, final int[][] records) {
        final byte[] signature = {(byte) 0xEC, (byte) 0xCC, (byte) 0x80, (byte) 0x28, (byte) 0x06};
        final int segmentHeaderSize = 115;
        final int recordSize = 8;

        final int dataSize = 7 + 1 + signature.length + segmentHeaderSize
                + records.length * recordSize + 4;
        final ByteBuffer buf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(new byte[7]);      // fileId placeholder
        buf.put((byte) 0);         // padding
        buf.put(signature);

        final byte[] hdr = new byte[segmentHeaderSize];
        ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN).putInt(2, startTs);
        buf.put(hdr);

        for (final int[] rec : records) {
            buf.put((byte) 0);          // reserved
            buf.put((byte) rec[0]);     // hr
            buf.put((byte) 0);          // reserved
            buf.put((byte) rec[1]);     // speed (0.1 km/h)
            buf.putInt(0);              // reserved
        }

        final byte[] arr = buf.array();
        buf.putInt(CheckSums.getCRC32(arr, 0, arr.length - 4));
        return buf.array();
    }

    @Test
    public void testV5TreadmillSingleSegment() {
        final int startTs = 1782063192;
        final byte[] bytes = buildTreadmillV5Bytes(startTs, new int[][]{
                {100, 50}, // 5.0 km/h
                {135, 78}, // 7.8 km/h
                {165, 94}, // 9.4 km/h
        });

        final List<WorkoutDetailRecord> records =
                WorkoutDetailsParser.parseBytes(makeFileId(5), bytes);

        assertNotNull(records);
        assertEquals(3, records.size());
        assertEquals(100, records.get(0).hr);
        assertEquals(135, records.get(1).hr);
        assertEquals(165, records.get(2).hr);
        // speed stored raw in 0.1 km/h units
        assertEquals(Integer.valueOf(50), records.get(0).speedRaw);
        assertEquals(Integer.valueOf(94), records.get(2).speedRaw);
        // timestamps increment per second from the segment start
        assertEquals(startTs, records.get(0).ts);
        assertEquals(startTs + 2, records.get(2).ts);
    }

    @Test
    public void testGetActivityTrackV5Treadmill() {
        final int startTs = 1782063192;
        final byte[] bytes = buildTreadmillV5Bytes(startTs, new int[][]{
                {120, 0},  // speed 0 → no speed set
                {140, 90}, // 9.0 km/h → 2.5 m/s
        });

        final ActivityTrack track =
                new WorkoutDetailsParser().getActivityTrack(makeFileId(5), bytes);

        assertNotNull(track);
        final List<ActivityPoint> points = track.getAllPoints();
        assertEquals(2, points.size());
        assertEquals(120, points.get(0).getHeartRate());
        assertEquals(140, points.get(1).getHeartRate());
        // speed: raw 90 (0.1 km/h) / 36 = 2.5 m/s
        assertEquals(2.5f, points.get(1).getSpeed(), 0.001f);
    }
}
