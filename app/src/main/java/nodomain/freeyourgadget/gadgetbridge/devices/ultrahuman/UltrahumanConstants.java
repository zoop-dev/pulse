/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman;

import java.util.UUID;

public final class UltrahumanConstants {
    private UltrahumanConstants() {
    }

    public static final String ACTION_AIRPLANE_MODE = "nodomain.freeyourgadget.gadgetbridge.ultrahuman.action.AIRPLANE_MODE";
    public static final String ACTION_CHANGE_EXERCISE = "nodomain.freeyourgadget.gadgetbridge.ultrahuman.action.CHANGE_EXERCISE";
    public static final String ACTION_EXERCISE_UPDATE = "nodomain.freeyourgadget.gadgetbridge.ultrahuman.action.EXERCISE_UPDATE";

    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_EXERCISE = "exercise";

    public static final byte OPERATION_ACTIVATE_AIRPLANE_MODE = 0x70;
    public static final byte OPERATION_BREATHING_START = 0x72;
    public static final byte OPERATION_BREATHING_STOP = 0x74;
    public static final byte OPERATION_ENABLE_SPO2 = (byte) 0xD1;
    public static final byte OPERATION_DISABLE_SPO2 = (byte) 0xD2;
    public static final byte OPERATION_EXERCISE_START = 0x62;
    public static final byte OPERATION_EXERCISE_STOP = 0x64;
    public static final byte OPERATION_GET_FIRST_RECORDING_NR = 0x07;
    public static final byte OPERATION_GET_LAST_RECORDING_NR = 0x08;
    public static final byte OPERATION_GET_RECORDINGS = 0x04;
    public static final byte OPERATION_CHECK_DATA = 0x59;
    public static final byte OPERATION_RESET = (byte) 0x98;
    public static final byte OPERATION_SETTIME = 0x02;

    public static final UUID UUID_SERVICE_DATA = UUID.fromString("86f66000-f706-58a0-95b2-1fb9261e4dc7");
    public static final UUID UUID_DATA = UUID.fromString("86f66001-f706-58a0-95b2-1fb9261e4dc7");

    public static final UUID UUID_SERVICE_REQUEST = UUID.fromString("86f65000-f706-58a0-95b2-1fb9261e4dc7");
    public static final UUID UUID_COMMAND = UUID.fromString("86f65001-f706-58a0-95b2-1fb9261e4dc7");
    public static final UUID UUID_RESPONSE = UUID.fromString("86f65002-f706-58a0-95b2-1fb9261e4dc7");

    public static final UUID UUID_SERVICE_STATE = UUID.fromString("86f61000-f706-58a0-95b2-1fb9261e4dc7");
    public static final UUID UUID_STATE = UUID.fromString("86f61001-f706-58a0-95b2-1fb9261e4dc7");

    public static final UUID UUID_SERVICE_TODO = UUID.fromString("86f63000-f706-58a0-95b2-1fb9261e4dc7");
    public static final UUID UUID_TODO = UUID.fromString("86f63001-f706-58a0-95b2-1fb9261e4dc7");
}
