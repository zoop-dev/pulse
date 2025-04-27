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

import java.io.Serializable;

public class UltrahumanExerciseData implements Serializable {
    public int BatteryLevel;
    public int Exercise;
    public int HR;
    public int HRV;
    public float Temperature;
    public int Timestamp;

    public UltrahumanExerciseData() {
        BatteryLevel = -1;
        Exercise = -1;
        HR = -1;
        HRV = -1;
        Temperature = -1;
        Timestamp = -1;
    }

    public UltrahumanExerciseData(int batteryLevel, int exercise) {
        this();
        BatteryLevel = batteryLevel;
        Exercise = exercise;
    }
}
