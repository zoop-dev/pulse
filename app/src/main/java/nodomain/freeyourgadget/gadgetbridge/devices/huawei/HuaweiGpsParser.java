/*  Copyright (C) 2024 Martin.JM, Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class HuaweiGpsParser {

    public static class GpsPoint {
        public int timestamp;
        public double latitude;
        public double longitude;
        public boolean pause;
        public boolean altitudeSupported;
        public double altitude;

        @NonNull
        @Override
        public String toString() {
            return "GpsPoint{" +
                    "timestamp=" + timestamp +
                    ", longitude=" + longitude +
                    ", latitude=" + latitude +
                    ", pause=" + pause +
                    ", altitudeSupported=" + altitudeSupported +
                    ", altitude=" + altitude +
                    '}';
        }
    }

    public static GpsPoint[] parseHuaweiGps(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip trim
        buffer.position(0x20);

        int timestamp;
        double lon_start;
        double lat_start;
        boolean alt_support;
        double alt_start;

        byte fileType = buffer.get();
        if ((fileType & 0x03) != 0x03) {
            alt_support = false;
            timestamp = buffer.getInt();
            lon_start = buffer.getDouble();
            lat_start = buffer.getDouble();
            alt_start = 0;
            buffer.position(62); // Skip past unknown fields/padding
        } else {
            alt_support = true;
            timestamp = buffer.getInt();
            lon_start = buffer.getDouble();
            lat_start = buffer.getDouble();
            alt_start = buffer.getDouble();
            buffer.position(70); // Skip past unknown fields/padding
        }

        lat_start = lat_start * 0.017453292519943;
        lon_start = lon_start * 0.017453292519943;

        // Working values
        int time = timestamp;
        double lat = lat_start;
        double lon = lon_start;
        double alt = alt_start;

        int data_size = 15;
        if (alt_support)
            data_size += 4;

        ArrayList<GpsPoint> retv = new ArrayList<>(buffer.remaining() / data_size);
        while (buffer.remaining() > data_size) {
            short time_delta = buffer.getShort();
            buffer.getShort(); // Unknown value, possible "bearing" (buffer.getShort() & 0xFFFF) * 0.01.
            float lon_delta = buffer.getFloat();
            float lat_delta = buffer.getFloat();
            buffer.get(); // Unknown values, possible "accuracy"
            buffer.get(); // Unknown values, possible "velocity"  (buffer.get() & 0xFF) * 0.1
            byte pause = buffer.get();

            time = time + time_delta;
            lat = lat + lat_delta;
            lon = lon + lon_delta;

            GpsPoint point = new GpsPoint();
            point.timestamp = time;
            // NOTE: instead of 6383807.0d should be 6378245.0 (Krassovsky 1940 ellipsoid).
            // According to my research it provides better result. But I am not sure.
            // Additional research required.
            point.latitude = (lat / 6383807.0d + lat_start) / 0.017453292519943d;
            point.longitude = (lon / 6383807.0d / Math.cos(lat_start) + lon_start) / 0.017453292519943d;
            point.pause = pause == 1;
            point.altitudeSupported = alt_support;
            if (alt_support) {
                // NOTE: in the modern devices e.g Watch Gt6 Pro altitude values absent or completely broken.
                alt = buffer.getShort();
                buffer.getShort(); // Unknown values related to altitude.
                point.altitude = alt;
            }
            retv.add(point);
        }

        return retv.toArray(new GpsPoint[0]);
    }
}
