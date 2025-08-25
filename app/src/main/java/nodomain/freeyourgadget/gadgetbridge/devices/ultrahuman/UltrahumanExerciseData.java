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

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

/**
 * Helper for DeviceService#ACTION_REALTIME_SAMPLES and UltrahumanConstants.ACTION_EXERCISE_UPDATE intents.
  */
public class UltrahumanExerciseData implements ActivitySample, Serializable{
    public int BatteryLevel;
    public int Exercise;

    public int Timestamp;
    public int MeasurementType;
    public int HR;
    public int HRV;
    public float Temperature;

    public String Mystery;

    public UltrahumanExerciseData() {
        BatteryLevel = -1;
        Exercise = -1;
        HR = -1;
        HRV = -1;
        Temperature = -1.0f;
        Timestamp = -1;
    }

    public UltrahumanExerciseData(int batteryLevel, int exercise) {
        this();
        BatteryLevel = batteryLevel;
        Exercise = exercise;
    }

    public UltrahumanExerciseData(int batteryLevel, int exercise, byte type) {
        this(batteryLevel,exercise);
        MeasurementType = type;
    }

    @Override
    public SampleProvider<?> getProvider() {
        return null;
    }

    @Override
    public int getRawKind() {
        return 0;
    }

    @Override
    public ActivityKind getKind() {
        return ActivityKind.UNKNOWN;
    }

    @Override
    public int getRawIntensity() {
        return 0;
    }

    @Override
    public float getIntensity() {
        return 0.0f;
    }

    @Override
    public int getSteps() {
        return 0;
    }

    @Override
    public int getDistanceCm() {
        return 0;
    }

    @Override
    public int getActiveCalories() {
        return 0;
    }

    @Override
    public int getHeartRate() {
        return HR;
    }

    @Override
    public void setHeartRate(int value) {
        HR = value;
    }

    @Override
    public int getTimestamp() {
        return Timestamp;
    }
}
