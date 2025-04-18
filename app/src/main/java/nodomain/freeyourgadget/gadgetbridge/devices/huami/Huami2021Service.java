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
     * Raw sensor control.
     */
    public static final byte[] CMD_RAW_SENSOR_START_1 = new byte[]{0x01, 0x03, 0x19}; // band replies 10:01:03:05
    public static final byte[] CMD_RAW_SENSOR_START_2 = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x19}; // band replies 10:01:01:05
    public static final byte[] CMD_RAW_SENSOR_START_3 = new byte[]{0x02}; // band replies 10:02:01
    public static final byte[] CMD_RAW_SENSOR_STOP = new byte[]{0x03}; // band replies 10:03:01
}
