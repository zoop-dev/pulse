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

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;

public class ZeppOsActivityDetailsParser extends AbstractHuamiActivityDetailsParser {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsActivityDetailsParser.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Date timestamp;
    private long offset = 0;

    private long longitude;
    private long latitude;
    private double altitude;

    private final ZeppOsActivityTrack activityTrack;
    private ActivityPoint lastActivityPoint;

    public ZeppOsActivityDetailsParser(final BaseActivitySummary summary) {
        this.timestamp = summary.getStartTime();

        this.longitude = summary.getBaseLongitude();
        this.latitude = summary.getBaseLatitude();
        this.altitude = summary.getBaseAltitude();

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
                case STRENGTH_SET:
                    consumeStrengthSet(buf);
                    break;
                case LAP:
                    consumeLap(buf);
                    break;
                default:
                    LOG.warn("No consumer for for type {}", type);
                    // Consume the reported length
                    buf.get(new byte[length]);
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
                LOG.warn("Unknown type code {} seen {} times", String.format("0x%X", e.getKey()), e.getValue());
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
        buf.getInt(); // ?
        this.timestamp = new Date(buf.getLong());
        this.offset = 0;

        trace("Consumed timestamp");
    }

    private void consumeTimestampOffset(final ByteBuffer buf) {
        this.offset = buf.getShort();
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

        addNewGpsCoordinates();

        final double longitudeDeg = convertHuamiValueToDecimalDegrees(longitude);
        final double latitudeDeg = convertHuamiValueToDecimalDegrees(latitude);

        trace("Consumed GPS coords: {} {}", longitudeDeg, latitudeDeg);
    }

    private void consumeGpsDelta(final ByteBuffer buf, final int length) {
        consumeTimestampOffset(buf);
        final short longitudeDelta = buf.getShort();
        final short latitudeDelta = buf.getShort();
        final short two = buf.getShort(); // ? seems to always be 2

        this.longitude += longitudeDelta;
        this.latitude += latitudeDelta;

        // Handle additional data in Balance 2 format (16 bytes total)
        if (length == 16) {
            // Skip additional 8 bytes: 2-byte flag + 2x 4-byte floats (likely speed/accuracy)
            buf.get(new byte[8]);
        }

        if (lastActivityPoint == null) {
            final String timestampStr = SDF.format(new Date(timestamp.getTime() + offset));
            LOG.warn("{}: Got GPS delta before GPS coords, ignoring", timestampStr);
            return;
        }

        addNewGpsCoordinates();

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

        final ActivityPoint ap = getCurrentActivityPoint();
        if (ap != null) {
            ap.setCadence(cadence);
            if (pace != 0) {
                ap.setSpeed(1000f / pace); // s/km -> m/s
            }
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

        final ActivityPoint ap = getCurrentActivityPoint();
        if (ap != null) {
            final GPSCoordinate newCoordinate = new GPSCoordinate(
                    ap.getLocation().getLongitude(),
                    ap.getLocation().getLatitude(),
                    newAltitude
            );

            ap.setLocation(newCoordinate);
        }

        // Only update the instance altitude if we have valid data
        altitude = newAltitude;

        trace("Consumed altitude: {}", altitude);
    }

    private void consumeHeartRate(final ByteBuffer buf) {
        consumeTimestampOffset(buf);
        final int heartRate = buf.get() & 0xff;

        final ActivityPoint ap = getCurrentActivityPoint();
        if (ap != null) {
            ap.setHeartRate(heartRate);
        }

        trace("Consumed HeartRate: {}", heartRate);
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

    @Nullable
    private ActivityPoint getCurrentActivityPoint() {
        if (lastActivityPoint == null) {
            return null;
        }

        // Round to the nearest second
        final long currentTime = timestamp.getTime() + offset;
        if (currentTime - lastActivityPoint.getTime().getTime() > 500) {
            addNewGpsCoordinates();
            return lastActivityPoint;
        }

        return lastActivityPoint;
    }

    private void addNewGpsCoordinates() {
        final GPSCoordinate coordinate = new GPSCoordinate(
                convertHuamiValueToDecimalDegrees(longitude),
                convertHuamiValueToDecimalDegrees(latitude),
                altitude
        );

        if (lastActivityPoint != null && lastActivityPoint.getLocation() != null && lastActivityPoint.getLocation().equals(coordinate)) {
            // Ignore repeated location
            return;
        }

        final ActivityPoint ap = new ActivityPoint(new Date(timestamp.getTime() + offset));
        ap.setLocation(coordinate);
        add(ap);
    }

    private void add(final ActivityPoint ap) {
        if (ap == lastActivityPoint) {
            LOG.debug("skipping point!");
            return;
        }

        lastActivityPoint = ap;
        activityTrack.addTrackPoint(ap);
    }

    private void trace(final String format, final Object... args) {
        final Object[] argsWithDate;
        if (timestamp != null) {
            argsWithDate = ArrayUtils.insert(0, args, SDF.format(new Date(timestamp.getTime() + offset)));
        } else {
            argsWithDate = ArrayUtils.insert(0, args, "(null)");
        }

        //noinspection StringConcatenationArgumentToLogCall
        LOG.trace("{}: " + format, argsWithDate);
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
        STRENGTH_SET(15, 34),
        ;

        private final byte code;
        private final int expectedLength;

        Type(final int code, final int expectedLength) {
            this.code = (byte) code;
            this.expectedLength = expectedLength;
        }

        public byte getCode() {
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
