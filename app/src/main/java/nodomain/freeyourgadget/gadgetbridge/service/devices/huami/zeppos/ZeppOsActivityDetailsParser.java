/*  Copyright (C) 2022-2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ZeppOsActivityDetailsParser extends AbstractHuamiActivityDetailsParser {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsActivityDetailsParser.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final ZeppOsActivityTrack activityTrack;

    // We need to keep track of these separately because of the offsets
    private long timestamp;
    private int offset;
    private int longitude;
    private int latitude;

    private int lastSwimIntervalIndex = 0;

    private final ActivityPoint.Builder activityPointBuilder = new ActivityPoint.Builder();

    public ZeppOsActivityDetailsParser(final BaseActivitySummary summary) {
        this.activityTrack = new ZeppOsActivityTrack();
        this.activityTrack.setUser(summary.getUser());
        this.activityTrack.setDevice(summary.getDevice());
        this.activityTrack.setName(createActivityName(summary));
    }

    /**
     * Sequence of TLVs, encoded in
     * <a href="https://www.oss.com/asn1/resources/asn1-made-simple/asn1-quick-reference/basic-encoding-rules.html">ASN.1 BER</a>
     */
    @Override
    public ZeppOsActivityTrack parse(final byte[] bytes) throws GBException {
        final ByteBuffer buf = ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Keep track of unknown type codes so we can print them without spamming the logs
        final Map<Integer, Integer> unknownTypeCodes = new HashMap<>();

        while (buf.position() < buf.limit()) {
            final int typeCode = consumeTag(buf);
            final int length = consumeLength(buf);
            final int initialPosition = buf.position();

            final Type type = Type.fromCode(typeCode);

            trace("Read typeCode={}, type={}, length={}, initialPosition={}", typeCode, type, length, initialPosition);

            if (type == null) {
                if (!unknownTypeCodes.containsKey(typeCode)) {
                    unknownTypeCodes.put(typeCode, 0);
                }

                unknownTypeCodes.put(typeCode, Objects.requireNonNull(unknownTypeCodes.get(typeCode)) + 1);
                //LOG.warn("Unknown type code {} of length {}", String.format("0x%X", typeCode), length);
                // Consume the reported length
                buf.get(new byte[length]);
                continue;
            } else if (!isValidLength(type, length)) {
                LOG.warn("Unexpected length {} for type {}", length, type);
                // Consume the reported length
                buf.get(new byte[length]);
                continue;
            }

            // Consume
            switch (type) {
                case TIMESTAMP:
                    consumeTimestamp(buf);
                    break;
                case GPS_COORDS:
                    consumeGpsCoords(buf, length);
                    break;
                case GPS_DELTA:
                    consumeGpsDelta(buf, length);
                    break;
                case STATUS:
                    consumeStatus(buf);
                    break;
                case SPEED:
                    consumeSpeed(buf);
                    break;
                case ALTITUDE:
                    consumeAltitude(buf, length);
                    break;
                case HEARTRATE:
                    consumeHeartRate(buf);
                    break;
                case TEMPERATURE:
                    consumeTemperature(buf);
                    break;
                case STRENGTH_SET:
                    consumeStrengthSet(buf);
                    break;
                case SWIMMING_INTERVAL:
                    consumeSwimmingInterval(buf);
                    break;
                case LAP:
                    consumeLap(buf);
                    break;
                default:
                    // Consume the reported length
                    final byte[] unkBytes = new byte[length];
                    buf.get(unkBytes);
                    LOG.warn(
                            "No consumer for for type {} at {}: {}",
                            type,
                            timestamp,
                            GB.hexdump(unkBytes)
                    );
                    continue;
            }

            final int expectedPosition = initialPosition + length;
            if (buf.position() != expectedPosition) {
                // Should never happen unless there's a bug in one of the consumers
                throw new IllegalStateException("Unexpected position " + buf.position() + ", expected " + expectedPosition + ", after consuming " + type);
            }
        }

        if (!unknownTypeCodes.isEmpty()) {
            for (final Map.Entry<Integer, Integer> e : unknownTypeCodes.entrySet()) {
                LOG.warn("Unknown type code {} seen {} times", e.getKey(), e.getValue());
            }
        }

        return this.activityTrack;
    }

    private static int consumeTag(final ByteBuffer buf) {
        final int first = buf.get() & 0xFF;

        if ((first & 0x1F) != 0x1F) {
            // single-byte tag
            return first;
        }

        // multi-byte tag
        int tag = first;
        while (buf.hasRemaining()) {
            int b = buf.get() & 0xFF;
            tag = (tag << 8) | b;
            if ((b & 0x80) == 0) break; // continuation bit cleared
        }
        return tag;
    }

    private static int consumeLength(final ByteBuffer buf) {
        final int first = buf.get() & 0xFF;
        if ((first & 0x80) == 0) {
            // short form
            return first;
        }

        // long form
        final int numBytes = first & 0x7F;
        if (numBytes == 0 || numBytes > 4) {
            throw new IllegalStateException("Unsupported length encoding: " + numBytes);
        }
        int value = 0;
        for (int i = 0; i < numBytes; i++) {
            value = (value << 8) | (buf.get() & 0xFF);
        }
        return value;
    }

    private boolean isValidLength(final Type type, final int length) {
        return switch (type) {
            // Support both old format (20 bytes) and new Balance 2 format (28 bytes)
            case GPS_COORDS -> length == 20 || length == 28;
            // Support both old format (8 bytes) and new Balance 2 format (16 bytes)
            case GPS_DELTA -> length == 8 || length == 16;
            // Support both old format (6 bytes) and new Balance 2 format (7 bytes)
            case ALTITUDE -> length == 6 || length == 7;
            default -> length == type.getExpectedLength();
        };
    }

    private void consumeTimestamp(final ByteBuffer buf) {
        addNewActivityPoint();

        buf.getInt(); // ?
        this.timestamp = buf.getLong();
        this.offset = 0;

        trace("Consumed timestamp: {}", timestamp);
    }

    private void consumeTimestampOffset(final ByteBuffer buf) {
        addNewActivityPoint();

        this.offset = buf.getShort();

        trace("Consumed offset: {}", offset);
    }

    private void consumeGpsCoords(final ByteBuffer buf, final int length) {
        buf.get(new byte[6]); // ?
        this.longitude = buf.getInt();
        this.latitude = buf.getInt();

        // Handle different formats
        if (length == 20) {
            // Old format: skip remaining 6 bytes
            buf.get(new byte[6]); // ?
        } else if (length == 28) {
            // Balance 2 format: skip remaining 14 bytes (6 old + 8 new)
            buf.get(new byte[14]); // ? + additional Balance 2 data
        }

        // TODO which one is the time offset? Not sure it is the first

        final double longitudeDeg = convertHuamiValueToDecimalDegrees(longitude);
        final double latitudeDeg = convertHuamiValueToDecimalDegrees(latitude);

        trace("Consumed GPS coords: {} {}", longitudeDeg, latitudeDeg);
    }

    private void consumeGpsDelta(final ByteBuffer buf, final int length) {
        consumeTimestampOffset(buf);
        final short longitudeDelta = buf.getShort();
        final short latitudeDelta = buf.getShort();
        final short two = buf.getShort(); // ? seems to always be 2

        // Handle additional data in Balance 2 format (16 bytes total)
        if (length == 16) {
            // Skip additional 8 bytes: 2-byte flag + 2x 4-byte floats (likely speed/accuracy)
            buf.get(new byte[8]);
        }

        if (this.longitude == 0 && this.latitude == 0) {
            final String timestampStr = SDF.format(new Date(timestamp + offset));
            LOG.warn("{}: Got GPS delta before GPS coords, ignoring", timestampStr);
        } else {
            this.longitude += longitudeDelta;
            this.latitude += latitudeDelta;
        }

        trace("Consumed GPS delta: {} {} {}", longitudeDelta, latitudeDelta, two);
    }

    private void consumeStatus(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        final int statusCode = buf.getShort();
        final String status;
        switch (statusCode) {
            case 1:
                status = "start";
                break;
            case 4:
                status = "pause";
                activityTrack.startNewSegment();
                break;
            case 5:
                status = "resume";
                activityTrack.startNewSegment();
                break;
            case 6:
                status = "stop";
                addNewActivityPoint();
                break;
            default:
                status = String.format("unknown (0x%X)", statusCode);
                LOG.warn("Unknown status code {}", String.format("0x%X", statusCode));
        }

        trace("Consumed Status: {}", status);
    }

    private void consumeSpeed(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        final short cadence = buf.getShort(); // spm
        final short stride = buf.getShort(); // cm
        final short pace = buf.getShort(); // sec/km

        activityPointBuilder.setCadence(cadence);
        activityPointBuilder.setStrideCm(stride);
        if (pace != 0) {
            activityPointBuilder.setSpeed(1000f / pace); // s/km -> m/s
        }

        trace("Consumed speed: cadence={}, stride={}, pace={}", cadence, stride, pace);
    }

    private void consumeAltitude(final ByteBuffer buf, final int length) {
        consumeTimestampOffset(buf);
        final int altitudeRaw = buf.getInt();

        // Check for Balance 2 format with validity flag
        final double newAltitude;
        if (length == 7) {
            final byte validityFlag = buf.get();
            // 0xFF or 0xFFFFFFFF indicates invalid/no altitude data
            if (altitudeRaw == -1 || validityFlag == (byte) 0xFF) {
                // Skip invalid altitude data - don't update altitude at all
                return;
            }
            // Balance 2 barometric altitude: stored in 0.01mm, convert to meters
            newAltitude = altitudeRaw / 100000.0f;
        } else {
            // Old 6-byte format - check for invalid altitude
            if (altitudeRaw == -1) {
                return;
            }
            // Old format: GPS altitude in centimeters, convert to meters
            newAltitude = altitudeRaw / 100.0f;
        }

        activityPointBuilder.setAltitude(newAltitude);

        trace("Consumed altitude: {}", newAltitude);
    }

    private void consumeHeartRate(final ByteBuffer buf) {
        consumeTimestampOffset(buf);
        final int heartRate = buf.get() & 0xff;

        activityPointBuilder.setHeartRate(heartRate);

        trace("Consumed HeartRate: {}", heartRate);
    }

    private void consumeTemperature(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        final float temperature = buf.getFloat();
        buf.get(); // 0?

        activityPointBuilder.setTemperature(temperature);

        trace("Consumed temperature: {}", temperature);
    }

    private void consumeStrengthSet(final ByteBuffer buf) {
        buf.get(new byte[15]); // ?
        final int reps = buf.getShort() & 0xffff;
        buf.get(); // 0?
        final int weight = buf.getShort() & 0xffff;
        buf.get(new byte[14]); // ffff... ?

        activityTrack.addStrengthSet(reps, weight != 0xffff ? weight / 10f : -1);

        trace("Consumed strength set: reps={}, weightKg={}", reps, weight);
    }

    private void consumeSwimmingInterval(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        buf.get(); // 0?
        final int interval = buf.getShort(); // starting from 1
        final int poolLengthMeters = buf.getShort();
        buf.get(); // 0?
        final int hr = buf.get() & 0xff;
        buf.getShort(); // 1?
        final int style = buf.getShort() & 0xffff;
        final int pace = buf.getShort() & 0xffff; // s/km
        final int swolf = buf.getShort() & 0xffff;
        final int strokeRate = buf.getShort() & 0xffff;
        final int durationMillis = buf.getInt();
        buf.getInt(); // 0x00000000?
        final int strokeDistance = buf.getShort() & 0xffff;
        final int calories = buf.getShort() & 0xffff;

        activityTrack.addSwimmingInterval(
                interval,
                poolLengthMeters,
                hr,
                style,
                pace,
                swolf,
                strokeRate,
                durationMillis,
                strokeDistance,
                calories
        );

        final List<ActivityPoint> allPoints = activityTrack.getAllPoints();
        // Fill all activity points since the last interval so we can chart it
        for (int i = lastSwimIntervalIndex; i < allPoints.size(); i++) {
            allPoints.get(i).setSpeed(1000f / pace);
            allPoints.get(i).setCadence(strokeRate);
        }
        lastSwimIntervalIndex = allPoints.size();

        trace(
                "Consumed swimming interval {}: hr={}, style={}, pace={}, swolf={}, strokeRate={}, durationMillis={}, strokeDistance={}, calories={}",
                interval,
                hr,
                style,
                pace,
                swolf,
                strokeRate,
                durationMillis,
                strokeDistance,
                calories
        );
    }

    private void consumeLap(final ByteBuffer buf) {
        buf.get(new byte[2]); // ?
        final int number = buf.getShort() & 0xffff;
        buf.get(); // 3 ?
        final int hr = buf.get() & 0xff;
        final int pace = buf.getShort() & 0xffff; // s/km
        final int calories = buf.getShort() & 0xffff;
        final int distance = buf.getShort() & 0xffff; // m
        buf.get(new byte[4]); // ?
        final int duration = buf.getInt(); // ms
        buf.get(new byte[99 - 20]); // ?

        activityTrack.addLap(number, hr, pace, calories, distance, duration);

        trace("Consumed lap: number={}, hr={}, pace={}, calories={}, distance={}, duration={}", number, hr, pace, calories, distance, duration);
    }

    private void addNewActivityPoint() {
        if (timestamp == 0) {
            return;
        }
        activityPointBuilder.setTime(Math.round((timestamp + offset) / 1000d) * 1000L);
        if (longitude != 0) {
            activityPointBuilder.setLongitude(convertHuamiValueToDecimalDegrees(longitude));
        }
        if (latitude != 0) {
            activityPointBuilder.setLatitude(convertHuamiValueToDecimalDegrees(latitude));
        }
        final ActivityPoint ap = activityPointBuilder.build();
        final List<ActivityPoint> currentSegment = activityTrack.getSegments().get(activityTrack.getSegments().size() - 1);
        // We get timestamp increments by milliseconds, and data interleaved with those
        // Upsert the last value for the same second if we got an update, as it will almost
        // surely contain updated information
        if (!currentSegment.isEmpty()) {
            if (currentSegment.get(currentSegment.size() - 1).getTime().equals(ap.getTime())) {
                trace("Upserting activity point at {}", ap.getTime());
                currentSegment.set(currentSegment.size() - 1, ap);
            } else {
                trace("Adding activity point at {}", ap.getTime());
                activityTrack.addTrackPoint(ap);
            }
        } else {
            trace("Adding activity point at {}", ap.getTime());
            activityTrack.addTrackPoint(ap);
        }
    }

    private void trace(final String format, final Object... args) {
        if (LOG.isTraceEnabled()) {
            final Object[] argsWithDate = ArrayUtils.insert(0, args, SDF.format(new Date(activityPointBuilder.getTime())));

            //noinspection StringConcatenationArgumentToLogCall
            LOG.trace("{}: " + format, argsWithDate);
        }
    }

    private enum Type {
        TIMESTAMP(1, 12),
        GPS_COORDS(2, 20),
        GPS_DELTA(3, 8),
        STATUS(4, 4),
        SPEED(5, 8),
        ALTITUDE(7, 6),
        HEARTRATE(8, 3),
        LAP(11, 99),
        TEMPERATURE(13, 7),
        STRENGTH_SET(15, 34),
        SWIMMING_INTERVAL(20, 31),
        //UNKNOWN_7945(7945, 6),
        ;

        private final int code;
        private final int expectedLength;

        Type(final int code, final int expectedLength) {
            this.code = code;
            this.expectedLength = expectedLength;
        }

        public int getCode() {
            return this.code;
        }

        public int getExpectedLength() {
            return this.expectedLength;
        }

        public static Type fromCode(final int code) {
            for (final Type type : values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return null;
        }
    }
}
