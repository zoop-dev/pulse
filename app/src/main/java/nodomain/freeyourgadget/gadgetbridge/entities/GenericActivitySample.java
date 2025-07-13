/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.entities;

public class GenericActivitySample extends AbstractActivitySample {
    private int timestamp;
    private long userId;
    private long deviceId;

    private int rawKind = NOT_MEASURED;
    private int rawIntensity = NOT_MEASURED;
    private int steps = NOT_MEASURED;
    private int distanceCm = NOT_MEASURED;
    private int activeCalories = NOT_MEASURED;
    private int heartRate = NOT_MEASURED;

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(final int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public void setUserId(final long userId) {
        this.userId = userId;
    }

    @Override
    public long getDeviceId() {
        return deviceId;
    }

    @Override
    public void setDeviceId(final long deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public int getRawKind() {
        return rawKind;
    }

    @Override
    public void setRawKind(final int rawKind) {
        this.rawKind = rawKind;
    }

    @Override
    public int getRawIntensity() {
        return rawIntensity;
    }

    public void setRawIntensity(final int rawIntensity) {
        this.rawIntensity = rawIntensity;
    }

    @Override
    public int getSteps() {
        return steps;
    }

    @Override
    public void setSteps(final int steps) {
        this.steps = steps;
    }

    @Override
    public int getDistanceCm() {
        return distanceCm;
    }

    @Override
    public void setDistanceCm(final int distanceCm) {
        this.distanceCm = distanceCm;
    }

    @Override
    public int getActiveCalories() {
        return activeCalories;
    }

    @Override
    public void setActiveCalories(final int activeCalories) {
        this.activeCalories = activeCalories;
    }

    @Override
    public int getHeartRate() {
        return heartRate;
    }

    @Override
    public void setHeartRate(final int heartRate) {
        this.heartRate = heartRate;
    }
}
