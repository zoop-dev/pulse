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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.fitcloud.ak102.Ak102Constants;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/*
 * Stateless decoders for the AK102 history-sync byte stream.
 * The concatenated sync buffer is a series of blocks, each an 8-byte header
 * followed by itemCount fixed-size items:
 * [0..1] itemCount   u16 BE
 * [2..5] baseTime    packed minute-resolution timestamp (see decodeMinuteTs)
 * [6..7] interval    u16 BE   minutes (milliseconds for SPORT/ECG)
 * Item i is timestamped baseTime + i * interval.
 */
final class Ak102SyncParser {

    private Ak102SyncParser() {
    }

    // One activity bucket. Steps/distance/calories are per-interval increments.
    static final class ActivityPoint {
        final int timestamp; // unix seconds
        int steps;
        int distanceCm;
        int activeCalories; // calories, not kcal
        int heartRate;
        int rawKind = ActivityKind.UNKNOWN.getCode();

        ActivityPoint(final int timestamp) {
            this.timestamp = timestamp;
        }
    }

    // One timestamped scalar measurement (SpO2, stress, ...).
    static final class ValuePoint {
        final long timestampMillis;
        final int value;

        ValuePoint(final long timestampMillis, final int value) {
            this.timestampMillis = timestampMillis;
            this.value = value;
        }
    }

    // One workout summary plus its per-record heart-rate and cadence series.
    static final class Workout {
        long startMillis;
        int durationSeconds;
        int sportId;
        int steps;
        int distanceMeters;
        int caloriesKcal;
        int avgHeartRate;
        final List<ActivityPoint> hrPoints = new ArrayList<>();
        final List<ValuePoint> cadencePoints = new ArrayList<>(); // steps/min
    }

    // One GPS track: a base time and its trackpoints.
    static final class GpsTrack {
        final long baseMillis;
        final List<GpsPoint> points = new ArrayList<>();

        GpsTrack(final long baseMillis) {
            this.baseMillis = baseMillis;
        }
    }

    // One GPS trackpoint (absolute time + WGS84 position). restart marks the
    // first point after a pause/GPS restart, i.e. the start of a new segment.
    static final class GpsPoint {
        final long timestampMillis;
        final double latitude;
        final double longitude;
        final double altitude;
        final boolean restart;

        GpsPoint(final long timestampMillis, final double latitude, final double longitude,
                 final double altitude, final boolean restart) {
            this.timestampMillis = timestampMillis;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.restart = restart;
        }
    }

    // Block iteration ------------------------------------------------------

    private interface ItemSink {
        void accept(long timestampMillis, byte[] buffer, int offset);
    }

    // Iterate fixed-size items across all blocks, invoking sink per item.
    private static void iterate(final byte[] buf, final int type, final int itemSize,
                                final ItemSink sink) {
        int off = 0;
        while (off + 8 <= buf.length) {
            final int count = u16be(buf, off);
            final long baseMillis = decodeMinuteTs(buf, off + 2);
            final int interval = u16be(buf, off + 6);
            final long intervalMillis = (type == 16 || type == 7) ? interval : interval * 60000L;
            off += 8;
            if (count == 0) {
                continue;
            }
            for (int i = 0; i < count; i++) {
                if (off + itemSize > buf.length) {
                    return;
                }
                sink.accept(baseMillis + i * intervalMillis, buf, off);
                off += itemSize;
            }
        }
    }

    // Steps (type 1) -------------------------------------------------------

    static List<ActivityPoint> parseSteps(final byte[] buf, final boolean hasExtra,
                                          final boolean hasDuration) {
        final int itemSize = hasExtra ? (hasDuration ? 8 : 6) : 2;
        final List<ActivityPoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_STEP, itemSize, (ts, b, o) -> {
            final int steps = u16be(b, o);
            if (steps <= 0) {
                return;
            }
            final ActivityPoint p = new ActivityPoint((int) (ts / 1000L));
            p.steps = steps;
            p.rawKind = ActivityKind.ACTIVITY.getCode();
            if (hasExtra) {
                p.distanceCm = u16be(b, o + 2); // raw is km/100000 == cm
                p.activeCalories = u16be(b, o + 4); // raw is kcal/1000 == cal
            }
            out.add(p);
        });
        return out;
    }

    // Heart rate history (type 3, 1B per item) -----------------------------

    static List<ActivityPoint> parseHeartRate(final byte[] buf) {
        final List<ActivityPoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_HEART_RATE, 1, (ts, b, o) -> {
            final int bpm = b[o] & 0xFF;
            if (bpm <= 0) {
                return;
            }
            final ActivityPoint p = new ActivityPoint((int) (ts / 1000L));
            p.heartRate = bpm;
            out.add(p);
        });
        return out;
    }

    // Heart rate manual measurements (type 131, 5B: [ts section 5.3][bpm]) --------

    static List<ActivityPoint> parseHeartRateMeasure(final byte[] buf) {
        final List<ActivityPoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_HEART_RATE_MEASURE, 5, (ts, b, o) -> {
            final int bpm = b[o + 4] & 0xFF;
            if (bpm <= 0) {
                return;
            }
            final ActivityPoint p = new ActivityPoint((int) (decodeSecondTs(b, o) / 1000L));
            p.heartRate = bpm;
            out.add(p);
        });
        return out;
    }

    // SpO2 history (type 4, 1B) and measure (type 132, 5B) -----------------

    static List<ValuePoint> parseOxygen(final byte[] buf) {
        final List<ValuePoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_OXYGEN, 1, (ts, b, o) -> {
            final int spo2 = b[o] & 0xFF;
            if (spo2 > 0) {
                out.add(new ValuePoint(ts, spo2));
            }
        });
        return out;
    }

    static List<ValuePoint> parseOxygenMeasure(final byte[] buf) {
        final List<ValuePoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_OXYGEN_MEASURE, 5, (ts, b, o) -> {
            final int spo2 = b[o + 4] & 0xFF;
            if (spo2 > 0) {
                out.add(new ValuePoint(decodeSecondTs(b, o), spo2));
            }
        });
        return out;
    }

    // Stress/pressure history (type 18, 1B) and measure (type 146, 5B) -----

    static List<ValuePoint> parsePressure(final byte[] buf) {
        final List<ValuePoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_PRESSURE, 1, (ts, b, o) -> {
            final int stress = b[o] & 0xFF;
            if (stress > 0) {
                out.add(new ValuePoint(ts, stress));
            }
        });
        return out;
    }

    static List<ValuePoint> parsePressureMeasure(final byte[] buf) {
        final List<ValuePoint> out = new ArrayList<>();
        iterate(buf, Ak102Constants.SYNC_TYPE_PRESSURE_MEASURE, 5, (ts, b, o) -> {
            final int stress = b[o + 4] & 0xFF;
            if (stress > 0) {
                out.add(new ValuePoint(decodeSecondTs(b, o), stress));
            }
        });
        return out;
    }

    // Resting heart rate (type 254, 5B array, no block header) -------------

    static List<ActivityPoint> parseRestingHeartRate(final byte[] buf) {
        final List<ActivityPoint> out = new ArrayList<>();
        for (int o = 0; o + 5 <= buf.length; o += 5) {
            final int bpm = buf[o + 4] & 0xFF;
            if (bpm <= 0) {
                continue;
            }
            final ActivityPoint p = new ActivityPoint((int) (decodeSecondTs(buf, o) / 1000L));
            p.heartRate = bpm;
            out.add(p);
        }
        return out;
    }

    // Sleep (type 2) -------------------------------------------------------

    static List<ActivityPoint> parseSleep(final byte[] buf, final boolean newProtocol,
                                          final boolean hasRem) {
        final List<ActivityPoint> out = new ArrayList<>();
        if (!newProtocol) {
            // Old protocol: one status byte per header-interval minute.
            iterate(buf, Ak102Constants.SYNC_TYPE_SLEEP, 1, (ts, b, o) -> {
                final int mapped = mapSleepStatus(b[o] & 0xFF, false, hasRem);
                if (mapped < 0) {
                    return;
                }
                final ActivityPoint p = new ActivityPoint((int) (ts / 1000L));
                p.rawKind = mapped;
                out.add(p);
            });
            return out;
        }
        // New protocol: items are [minute-ts 4B][status 1B] STATE-CHANGE markers.
        // A marker's status describes the interval ENDING at its timestamp,
        // i.e. [marker[i-1].ts .. marker[i].ts) was spent in marker[i]'s state
        // (verified against on-watch stage totals). The first marker only
        // closes the unknown interval before recording started (typically the
        // 'awake' sleep-onset boundary), so it emits nothing.
        // Expand each interval into per-minute samples for GB's analysis.
        final List<long[]> markers = new ArrayList<>(); // {tsMillis, status}
        iterate(buf, Ak102Constants.SYNC_TYPE_SLEEP, 5, (ts, b, o) ->
                markers.add(new long[]{decodeMinuteTs(b, o), b[o + 4] & 0xFF}));
        Collections.sort(markers, (a, b) -> Long.compare(a[0], b[0]));
        final int awakeCode = ActivityKind.AWAKE_SLEEP.getCode();
        for (int i = 1; i < markers.size(); i++) {
            final int mapped = mapSleepStatus((int) markers.get(i)[1], true, hasRem);
            if (mapped < 0) {
                continue;
            }
            final long start = markers.get(i - 1)[0];
            final long end = markers.get(i)[0];
            if (end <= start || end - start > 8 * 3600_000L) {
                continue; // duplicate ts or implausible gap (e.g. across sync blocks)
            }
            if (mapped == awakeCode && end - start > 3600_000L) {
                continue; // hours-long awake = between sessions, not awake in bed
            }
            for (long t = start; t < end; t += 60_000L) {
                final ActivityPoint p = new ActivityPoint((int) (t / 1000L));
                p.rawKind = mapped;
                out.add(p);
            }
        }
        return out;
    }

    // Returns an ActivityKind code, or -1 to drop the sample.
    private static int mapSleepStatus(final int status, final boolean newProtocol,
                                      final boolean hasRem) {
        int s = status;
        if (hasRem) {
            if (s < 0 || s > 4) {
                return -1;
            }
        } else if (newProtocol) {
            if (s < 0 || s > 3) {
                return -1;
            }
        } else if (s < 1 || s > 3) {
            return -1;
        }
        if (newProtocol && s == 0) {
            s = 3; // new-protocol 0 == awake
        }
        switch (s) {
            case 1:
                return ActivityKind.DEEP_SLEEP.getCode();
            case 2:
                return ActivityKind.LIGHT_SLEEP.getCode();
            case 3:
                return ActivityKind.AWAKE_SLEEP.getCode();
            case 4:
                return ActivityKind.REM_SLEEP.getCode();
            default:
                return -1;
        }
    }

    // Today total (type 255) ----------------------------------------------

    /*
     * {steps, distanceMeters, calories, heartRate} from the today-total blob:
     * [step u32][distance u32][calorie u32][deepSleep u32][lightSleep u32]
     * [heartRate u32][deltaStep u16][deltaDist u16][deltaCal u16], big-endian.
     */
    static int[] parseTodayTotal(final byte[] data) {
        if (data == null || data.length < 24) {
            return null;
        }
        return new int[]{
                u32be(data, 0),
                u32be(data, 4),
                u32be(data, 8),
                u32be(data, 20),
        };
    }

    // Sport (type 16) + GPS (type 10) --------------------------------------

    /*
     * Parse sport (type 16). Each block: 8B header (interval in MILLISECONDS
     * for this type), then - on firmwares with GPS (feature 24) - a sport-type
     * string `[len][4B section 5.3 start-ts][2B id]`, then count records of
     * 12 + itemHrCount bytes:
     * [0..1] sportId, [2..3] cumulative duration s, [4..5] steps,
     * [6..7] distance m, [8..9] calories cal, [12..] per-item HR.
     * Record i covers baseTime + i * interval; its HR bytes are spread evenly
     * across that interval and collected as timestamped points.
     */
    static List<Workout> parseSport(final byte[] buf, final int itemHrCount,
                                    final boolean hasTypeString) {
        final List<Workout> out = new ArrayList<>();
        final int recordLen = itemHrCount + 12;
        int off = 0;
        while (off + 8 <= buf.length) {
            final int count = u16be(buf, off);
            long baseMillis = decodeMinuteTs(buf, off + 2);
            final int headerInterval = u16be(buf, off + 6);
            final long intervalMillis = headerInterval >= 1000 ? headerInterval : 60_000L;
            off += 8;
            if (count == 0) {
                continue;
            }
            if (hasTypeString) {
                if (off >= buf.length) {
                    break;
                }
                final int strLen = buf[off] & 0xFF;
                // The true session start is embedded in the string's first 4 bytes.
                if (strLen >= 6 && off + 1 + strLen <= buf.length) {
                    baseMillis = decodeSecondTs(buf, off + 1);
                }
                off += 1 + strLen;
            }
            Workout workout = null;
            int blockSportId = -1;
            long hrSum = 0;
            int hrCount = 0;
            long rawCalories = 0;
            for (int i = 0; i < count; i++) {
                if (off + recordLen > buf.length) {
                    break;
                }
                final int sportId = u16be(buf, off);
                if (blockSportId < 0) {
                    blockSportId = sportId;
                    workout = new Workout();
                    workout.startMillis = baseMillis;
                    workout.sportId = sportId;
                }
                if (sportId != blockSportId) {
                    off += recordLen;
                    continue;
                }
                workout.durationSeconds = u16be(buf, off + 2); // cumulative, last wins
                final int recordSteps = u16be(buf, off + 4);
                workout.steps += recordSteps;
                workout.distanceMeters += u16be(buf, off + 6);
                rawCalories += u16be(buf, off + 8);
                // Per-record step delta over the record interval = cadence.
                workout.cadencePoints.add(new ValuePoint(baseMillis + i * intervalMillis,
                        (int) (recordSteps * 60_000L / intervalMillis)));
                for (int h = 0; h < itemHrCount; h++) {
                    final int hr = buf[off + 12 + h] & 0xFF;
                    if (hr > 0) {
                        hrSum += hr;
                        hrCount++;
                        final long ts = baseMillis + i * intervalMillis
                                + h * (intervalMillis / itemHrCount);
                        final ActivityPoint p = new ActivityPoint((int) (ts / 1000L));
                        p.heartRate = hr;
                        workout.hrPoints.add(p);
                    }
                }
                off += recordLen;
            }
            if (workout != null) {
                workout.caloriesKcal = (int) (rawCalories / 1000);
                workout.avgHeartRate = hrCount > 0 ? (int) (hrSum / hrCount) : 0;
                out.add(workout);
            }
        }
        return out;
    }

    /*
     * Parse GPS (type 10) into tracks. Each block carries a sport-type string
     * whose leading 4 bytes are the session start (section 5.3, second resolution);
     * item time = that base + per-item second offset. Blocks sharing a base
     * time are merged into one track so a workout maps to a single GPX.
     */
    static List<GpsTrack> parseGps(final byte[] buf) {
        final List<GpsTrack> out = new ArrayList<>();
        GpsTrack current = null;
        int off = 0;
        while (off + 8 <= buf.length) {
            final int count = u16be(buf, off);
            final long blockMinuteMillis = decodeMinuteTs(buf, off + 2);
            off += 8;
            if (count == 0) {
                continue;
            }
            if (off >= buf.length) {
                break;
            }
            final int strLen = buf[off] & 0xFF; // sport-type string
            // First 4 bytes of the string are the section 5.3 session start.
            final long base = strLen >= 6 ? decodeSecondTs(buf, off + 1) : blockMinuteMillis;
            off += 1 + strLen;
            if (current == null || current.baseMillis != base) {
                current = new GpsTrack(base);
                out.add(current);
            }
            for (int i = 0; i < count; i++) {
                if (off + 14 > buf.length) {
                    return out;
                }
                // [0..1] tOff s, [2..5] LONGITUDE, [6..9] LATITUDE (SDK order),
                // [10..11] altitude m, [12] satellites, [13] restart flag.
                final int timeOffset = u16be(buf, off);
                final int lonRaw = s32be(buf, off + 2);
                final int latRaw = s32be(buf, off + 6);
                final int alt = u16be(buf, off + 10);
                if (latRaw != 0 && lonRaw != 0) {
                    current.points.add(new GpsPoint(base + timeOffset * 1000L,
                            latRaw / 100000.0, lonRaw / 100000.0, alt,
                            buf[off + 13] != 0));
                }
                off += 14;
            }
        }
        return out;
    }

    // Returns the HR of the point closest to ts (unix seconds) within
    // tolerance, or 0 when there is none.
    static int nearestHeartRate(final List<ActivityPoint> hrPoints, final int ts,
                                final int toleranceSeconds) {
        int best = 0;
        int bestDelta = toleranceSeconds + 1;
        for (final ActivityPoint p : hrPoints) {
            final int delta = Math.abs(p.timestamp - ts);
            if (delta < bestDelta && p.heartRate > 0) {
                bestDelta = delta;
                best = p.heartRate;
            }
        }
        return best;
    }

    // Returns the value of the point closest to tsMillis within tolerance,
    // or -1 when there is none. Zero is a valid value (e.g. cadence 0).
    static int nearestValue(final List<ValuePoint> points, final long tsMillis,
                            final long toleranceMillis) {
        int best = -1;
        long bestDelta = toleranceMillis + 1;
        for (final ValuePoint p : points) {
            final long delta = Math.abs(p.timestampMillis - tsMillis);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = p.value;
            }
        }
        return best;
    }

    // Timestamp decoders ---------------------------------------------------

    // section 5.2 packed minute-resolution timestamp -> unix millis.
    static long decodeMinuteTs(final byte[] b, final int o) {
        final int year = 2000 + ((b[o] & 0x7E) >> 1);
        final int month = ((b[o] & 1) << 3) | ((b[o + 1] >> 5) & 7);
        final int day = b[o + 1] & 0x1F;
        final int minuteOfDay = u16be(b, o + 2);
        final Calendar c = GregorianCalendar.getInstance();
        c.clear();
        c.set(year, Math.max(0, month - 1), Math.max(1, day), minuteOfDay / 60, minuteOfDay % 60, 0);
        return c.getTimeInMillis();
    }

    // section 5.3 second-resolution timestamp (inverse of packDateTime) -> unix millis.
    static long decodeSecondTs(final byte[] b, final int o) {
        final int year = 2000 + ((b[o] & 0xFC) >> 2);
        final int month = ((b[o] & 3) << 2) | ((b[o + 1] & 0xC0) >> 6);
        final int day = (b[o + 1] & 0x3E) >> 1;
        final int hour = ((b[o + 1] & 1) << 4) | ((b[o + 2] & 0xF0) >> 4);
        final int minute = ((b[o + 2] & 0xF) << 2) | ((b[o + 3] & 0xC0) >> 6);
        final int second = b[o + 3] & 0x3F;
        final Calendar c = GregorianCalendar.getInstance();
        c.clear();
        c.set(year, Math.max(0, month - 1), Math.max(1, day), hour, minute, second);
        return c.getTimeInMillis();
    }

    // Byte helpers ---------------------------------------------------------

    private static int u16be(final byte[] b, final int o) {
        return ((b[o] & 0xFF) << 8) | (b[o + 1] & 0xFF);
    }

    private static int s32be(final byte[] b, final int o) {
        return ((b[o] & 0xFF) << 24) | ((b[o + 1] & 0xFF) << 16)
                | ((b[o + 2] & 0xFF) << 8) | (b[o + 3] & 0xFF);
    }

    private static int u32be(final byte[] b, final int o) {
        return s32be(b, o);
    }
}
