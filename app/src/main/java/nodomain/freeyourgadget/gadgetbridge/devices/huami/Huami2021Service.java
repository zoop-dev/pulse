/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

public class Huami2021Service {
    /**
     * Endpoints for 2021 chunked protocol
     */
    public static final short CHUNKED2021_ENDPOINT_STEPS = 0x0016;
    public static final short CHUNKED2021_ENDPOINT_WORKOUT = 0x0019;
    public static final short CHUNKED2021_ENDPOINT_HEARTRATE = 0x001d;
    public static final short CHUNKED2021_ENDPOINT_AUTH = 0x0082;
    public static final short CHUNKED2021_ENDPOINT_COMPAT = 0x0090;

    /**
     * Steps, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_STEPS}.
     */
    public static final byte STEPS_CMD_GET = 0x03;
    public static final byte STEPS_CMD_REPLY = 0x04;
    public static final byte STEPS_CMD_ENABLE_REALTIME = 0x05;
    public static final byte STEPS_CMD_ENABLE_REALTIME_ACK = 0x06;
    public static final byte STEPS_CMD_REALTIME_NOTIFICATION = 0x07;

    /**
     * Notifications, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_HEARTRATE}.
     */
    public static final byte HEART_RATE_CMD_REALTIME_SET = 0x04;
    public static final byte HEART_RATE_CMD_REALTIME_ACK = 0x05;
    public static final byte HEART_RATE_CMD_SLEEP = 0x06;
    public static final byte HEART_RATE_FALL_ASLEEP = 0x01;
    public static final byte HEART_RATE_WAKE_UP = 0x00;
    public static final byte HEART_RATE_REALTIME_MODE_STOP = 0x00;
    public static final byte HEART_RATE_REALTIME_MODE_START = 0x01;
    public static final byte HEART_RATE_REALTIME_MODE_CONTINUE = 0x02;

    /**
     * Workout, for {@link Huami2021Service#CHUNKED2021_ENDPOINT_WORKOUT}.
     */
    public static final byte WORKOUT_CMD_GPS_LOCATION = 0x04;
    public static final byte WORKOUT_CMD_APP_OPEN = 0x20;
    public static final byte WORKOUT_CMD_STATUS = 0x11;
    public static final int WORKOUT_GPS_FLAG_STATUS = 0x1;
    public static final int WORKOUT_GPS_FLAG_POSITION = 0x40000;
    public static final byte WORKOUT_STATUS_START = 0x01;
    public static final byte WORKOUT_STATUS_END = 0x04;

    /**
     * Raw sensor control.
     */
    public static final byte[] CMD_RAW_SENSOR_START_1 = new byte[]{0x01, 0x03, 0x19}; // band replies 10:01:03:05
    public static final byte[] CMD_RAW_SENSOR_START_2 = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x19}; // band replies 10:01:01:05
    public static final byte[] CMD_RAW_SENSOR_START_3 = new byte[]{0x02}; // band replies 10:02:01
    public static final byte[] CMD_RAW_SENSOR_STOP = new byte[]{0x03}; // band replies 10:03:01
}
