/*  Copyright (C) 2025 Freeyourgadget

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitTrainingSettings extends RecordData {
    public FitTrainingSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 13) {
            throw new IllegalArgumentException("FitTrainingSettings expects native messages of " + 13 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Float getVirtualPartnerPace() {
        return getFieldByNumber(2, Float.class);
    }

    @Nullable
    public Integer getAutoLapMode() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Double getAutoLapDistance() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Integer getAutoPause() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Float getAutoPauseThreshold() {
        return getFieldByNumber(8, Float.class);
    }

    @Nullable
    public Integer getPowerAveraging() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getAutoScroll() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getTimerStartPrompt() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Float getPoolLength() {
        return getFieldByNumber(22, Float.class);
    }

    @Nullable
    public Integer getAutoSleep() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getSatellites() {
        return getFieldByNumber(27, Integer.class);
    }

    @Nullable
    public Double getTargetDistance() {
        return getFieldByNumber(31, Double.class);
    }

    @Nullable
    public Float getTargetSpeed() {
        return getFieldByNumber(32, Float.class);
    }

    @Nullable
    public Long getTargetTime() {
        return getFieldByNumber(33, Long.class);
    }

    @Nullable
    public Integer getSpeed3D() {
        return getFieldByNumber(35, Integer.class);
    }

    @Nullable
    public Integer getDistance3D() {
        return getFieldByNumber(36, Integer.class);
    }

    @Nullable
    public Integer getAutoClimb() {
        return getFieldByNumber(37, Integer.class);
    }

    @Nullable
    public Integer getAutoClimbInvertColors() {
        return getFieldByNumber(40, Integer.class);
    }

    @Nullable
    public Long getAutoClimbVerticalSpeed() {
        return getFieldByNumber(41, Long.class);
    }

    @Nullable
    public Integer getAutoClimbModeSwitch() {
        return getFieldByNumber(42, Integer.class);
    }

    @Nullable
    public Integer getLapKey() {
        return getFieldByNumber(46, Integer.class);
    }

    @Nullable
    public Integer getWorkoutTargetAlerts() {
        return getFieldByNumber(50, Integer.class);
    }

    @Nullable
    public Integer getTimerStartAuto() {
        return getFieldByNumber(51, Integer.class);
    }

    @Nullable
    public Float getTimerStartSpeed() {
        return getFieldByNumber(52, Float.class);
    }

    @Nullable
    public Integer getSegmentAlerts() {
        return getFieldByNumber(52, Integer.class);
    }

    @Nullable
    public Integer getCountdownStart() {
        return getFieldByNumber(57, Integer.class);
    }

    @Nullable
    public Integer getClimbPro() {
        return getFieldByNumber(63, Integer.class);
    }

    @Nullable
    public Integer getTrackConsumption() {
        return getFieldByNumber(67, Integer.class);
    }

    @Nullable
    public Integer getBottleSize() {
        return getFieldByNumber(69, Integer.class);
    }

    @Nullable
    public Integer getVolume() {
        return getFieldByNumber(70, Integer.class);
    }

    @Nullable
    public Integer getMinimumRideDuration() {
        return getFieldByNumber(80, Integer.class);
    }

    @Nullable
    public Integer getLaneNumber() {
        return getFieldByNumber(86, Integer.class);
    }

    @Nullable
    public Integer getBroadcastHeartRate() {
        return getFieldByNumber(87, Integer.class);
    }

    @Nullable
    public Integer getSelfEvaluation() {
        return getFieldByNumber(93, Integer.class);
    }

    @Nullable
    public Integer getSpeedPro() {
        return getFieldByNumber(102, Integer.class);
    }

    @Nullable
    public Integer getTouch() {
        return getFieldByNumber(103, Integer.class);
    }

    @Nullable
    public Integer getRecordTemperature() {
        return getFieldByNumber(106, Integer.class);
    }

    @Nullable
    public Integer getRunningPowerMode() {
        return getFieldByNumber(109, Integer.class);
    }

    @Nullable
    public Integer getAccountForWind() {
        return getFieldByNumber(110, Integer.class);
    }

    @Nullable
    public Integer getClimbProMode() {
        return getFieldByNumber(111, Integer.class);
    }

    @Nullable
    public Integer getClimbDetection() {
        return getFieldByNumber(117, Integer.class);
    }

    @Nullable
    public Integer getClimbProTerrain() {
        return getFieldByNumber(119, Integer.class);
    }

    @Nullable
    public Double getPreciseTargetSpeed() {
        return getFieldByNumber(153, Double.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(13);
        }

        public Builder setVirtualPartnerPace(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setAutoLapMode(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setAutoLapDistance(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setAutoPause(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setAutoPauseThreshold(final Float value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setPowerAveraging(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAutoScroll(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setTimerStartPrompt(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setPoolLength(final Float value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setAutoSleep(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setSatellites(final Integer value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setTargetDistance(final Double value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setTargetSpeed(final Float value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setTargetTime(final Long value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setSpeed3D(final Integer value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setDistance3D(final Integer value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setAutoClimb(final Integer value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setAutoClimbInvertColors(final Integer value) {
            setFieldByNumber(40, value);
            return this;
        }

        public Builder setAutoClimbVerticalSpeed(final Long value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setAutoClimbModeSwitch(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setLapKey(final Integer value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setWorkoutTargetAlerts(final Integer value) {
            setFieldByNumber(50, value);
            return this;
        }

        public Builder setTimerStartAuto(final Integer value) {
            setFieldByNumber(51, value);
            return this;
        }

        public Builder setTimerStartSpeed(final Float value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setSegmentAlerts(final Integer value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setCountdownStart(final Integer value) {
            setFieldByNumber(57, value);
            return this;
        }

        public Builder setClimbPro(final Integer value) {
            setFieldByNumber(63, value);
            return this;
        }

        public Builder setTrackConsumption(final Integer value) {
            setFieldByNumber(67, value);
            return this;
        }

        public Builder setBottleSize(final Integer value) {
            setFieldByNumber(69, value);
            return this;
        }

        public Builder setVolume(final Integer value) {
            setFieldByNumber(70, value);
            return this;
        }

        public Builder setMinimumRideDuration(final Integer value) {
            setFieldByNumber(80, value);
            return this;
        }

        public Builder setLaneNumber(final Integer value) {
            setFieldByNumber(86, value);
            return this;
        }

        public Builder setBroadcastHeartRate(final Integer value) {
            setFieldByNumber(87, value);
            return this;
        }

        public Builder setSelfEvaluation(final Integer value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setSpeedPro(final Integer value) {
            setFieldByNumber(102, value);
            return this;
        }

        public Builder setTouch(final Integer value) {
            setFieldByNumber(103, value);
            return this;
        }

        public Builder setRecordTemperature(final Integer value) {
            setFieldByNumber(106, value);
            return this;
        }

        public Builder setRunningPowerMode(final Integer value) {
            setFieldByNumber(109, value);
            return this;
        }

        public Builder setAccountForWind(final Integer value) {
            setFieldByNumber(110, value);
            return this;
        }

        public Builder setClimbProMode(final Integer value) {
            setFieldByNumber(111, value);
            return this;
        }

        public Builder setClimbDetection(final Integer value) {
            setFieldByNumber(117, value);
            return this;
        }

        public Builder setClimbProTerrain(final Integer value) {
            setFieldByNumber(119, value);
            return this;
        }

        public Builder setPreciseTargetSpeed(final Double value) {
            setFieldByNumber(153, value);
            return this;
        }

        @Override
        public FitTrainingSettings build() {
            return (FitTrainingSettings) super.build();
        }

        @Override
        public FitTrainingSettings build(final int localMessageType) {
            return (FitTrainingSettings) super.build(localMessageType);
        }
    }
}
