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

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
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
public class FitRecord extends RecordData {
    public FitRecord(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 20) {
            throw new IllegalArgumentException("FitRecord expects global messages of " + 20 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getLatitude() {
        return getFieldByNumber(0, Double.class);
    }

    @Nullable
    public Double getLongitude() {
        return getFieldByNumber(1, Double.class);
    }

    @Nullable
    public Float getAltitude() {
        return getFieldByNumber(2, Float.class);
    }

    @Nullable
    public Integer getHeartRate() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getCadence() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Double getDistance() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Float getSpeed() {
        return getFieldByNumber(6, Float.class);
    }

    @Nullable
    public Integer getPower() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Number[] getCompressedSpeedDistance() {
        return getArrayFieldByNumber(8, Number.class);
    }

    @Nullable
    public Float getGrade() {
        return getFieldByNumber(9, Float.class);
    }

    @Nullable
    public Integer getResistance() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Double getTimeFromCourse() {
        return getFieldByNumber(11, Double.class);
    }

    @Nullable
    public Float getCycleLength() {
        return getFieldByNumber(12, Float.class);
    }

    @Nullable
    public Integer getTemperature() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Number[] getSpeed1s() {
        return getArrayFieldByNumber(17, Number.class);
    }

    @Nullable
    public Integer getCycles() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Long getTotalCycles() {
        return getFieldByNumber(19, Long.class);
    }

    @Nullable
    public Integer getCompressedAccumulatedPower() {
        return getFieldByNumber(28, Integer.class);
    }

    @Nullable
    public Long getAccumulatedPower() {
        return getFieldByNumber(29, Long.class);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return getFieldByNumber(30, Integer.class);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return getFieldByNumber(31, Integer.class);
    }

    @Nullable
    public Float getVerticalSpeed() {
        return getFieldByNumber(32, Float.class);
    }

    @Nullable
    public Integer getCalories() {
        return getFieldByNumber(33, Integer.class);
    }

    @Nullable
    public Float getOscillation() {
        return getFieldByNumber(39, Float.class);
    }

    @Nullable
    public Float getStanceTimePercent() {
        return getFieldByNumber(40, Float.class);
    }

    @Nullable
    public Float getStanceTime() {
        return getFieldByNumber(41, Float.class);
    }

    @Nullable
    public Integer getActivity() {
        return getFieldByNumber(42, Integer.class);
    }

    @Nullable
    public Float getLeftTorqueEffectiveness() {
        return getFieldByNumber(43, Float.class);
    }

    @Nullable
    public Float getRightTorqueEffectiveness() {
        return getFieldByNumber(44, Float.class);
    }

    @Nullable
    public Float getLeftPedalSmoothness() {
        return getFieldByNumber(45, Float.class);
    }

    @Nullable
    public Float getRightPedalSmoothness() {
        return getFieldByNumber(46, Float.class);
    }

    @Nullable
    public Float getCombinedPedalSmoothness() {
        return getFieldByNumber(47, Float.class);
    }

    @Nullable
    public Float getTime128() {
        return getFieldByNumber(48, Float.class);
    }

    @Nullable
    public Integer getStrokeType() {
        return getFieldByNumber(49, Integer.class);
    }

    @Nullable
    public Integer getZone() {
        return getFieldByNumber(50, Integer.class);
    }

    @Nullable
    public Float getBallSpeed() {
        return getFieldByNumber(51, Float.class);
    }

    @Nullable
    public Float getCadence256() {
        return getFieldByNumber(52, Float.class);
    }

    @Nullable
    public Float getFractionalCadence() {
        return getFieldByNumber(53, Float.class);
    }

    @Nullable
    public Float getAvgTotalHemoglobinConc() {
        return getFieldByNumber(54, Float.class);
    }

    @Nullable
    public Float getMinTotalHemoglobinConc() {
        return getFieldByNumber(55, Float.class);
    }

    @Nullable
    public Float getMaxTotalHemoglobinConc() {
        return getFieldByNumber(56, Float.class);
    }

    @Nullable
    public Float getAvgSaturatedHemoglobinPercent() {
        return getFieldByNumber(57, Float.class);
    }

    @Nullable
    public Float getMinSaturatedHemoglobinPercent() {
        return getFieldByNumber(58, Float.class);
    }

    @Nullable
    public Float getMaxSaturatedHemoglobinPercent() {
        return getFieldByNumber(59, Float.class);
    }

    @Nullable
    public Integer getDeviceIndex() {
        return getFieldByNumber(62, Integer.class);
    }

    @Nullable
    public Integer getLeftPco() {
        return getFieldByNumber(67, Integer.class);
    }

    @Nullable
    public Integer getRightPco() {
        return getFieldByNumber(68, Integer.class);
    }

    @Nullable
    public Number[] getLeftPowerPhase() {
        return getArrayFieldByNumber(69, Number.class);
    }

    @Nullable
    public Number[] getLeftPowerPhasePeak() {
        return getArrayFieldByNumber(70, Number.class);
    }

    @Nullable
    public Number[] getRightPowerPhase() {
        return getArrayFieldByNumber(71, Number.class);
    }

    @Nullable
    public Number[] getRightPowerPhasePeak() {
        return getArrayFieldByNumber(72, Number.class);
    }

    @Nullable
    public Double getEnhancedSpeed() {
        return getFieldByNumber(73, Double.class);
    }

    @Nullable
    public Double getEnhancedAltitude() {
        return getFieldByNumber(78, Double.class);
    }

    @Nullable
    public Float getBatterySoc() {
        return getFieldByNumber(81, Float.class);
    }

    @Nullable
    public Integer getMotorPower() {
        return getFieldByNumber(82, Integer.class);
    }

    @Nullable
    public Float getVerticalRatio() {
        return getFieldByNumber(83, Float.class);
    }

    @Nullable
    public Float getStanceTimeBalance() {
        return getFieldByNumber(84, Float.class);
    }

    @Nullable
    public Float getStepLength() {
        return getFieldByNumber(85, Float.class);
    }

    @Nullable
    public Float getCycleLength16() {
        return getFieldByNumber(87, Float.class);
    }

    @Nullable
    public Long getAbsolutePressure() {
        return getFieldByNumber(91, Long.class);
    }

    @Nullable
    public Double getDepth() {
        return getFieldByNumber(92, Double.class);
    }

    @Nullable
    public Double getNextStopDepth() {
        return getFieldByNumber(93, Double.class);
    }

    @Nullable
    public Long getNextStopTime() {
        return getFieldByNumber(94, Long.class);
    }

    @Nullable
    public Long getTimeToSurface() {
        return getFieldByNumber(95, Long.class);
    }

    @Nullable
    public Long getNdlTime() {
        return getFieldByNumber(96, Long.class);
    }

    @Nullable
    public Integer getCnsLoad() {
        return getFieldByNumber(97, Integer.class);
    }

    @Nullable
    public Integer getN2Load() {
        return getFieldByNumber(98, Integer.class);
    }

    @Nullable
    public Integer getRespirationRate() {
        return getFieldByNumber(99, Integer.class);
    }

    @Nullable
    public Float getEnhancedRespirationRate() {
        return getFieldByNumber(108, Float.class);
    }

    @Nullable
    public Float getGrit() {
        return getFieldByNumber(114, Float.class);
    }

    @Nullable
    public Float getFlow() {
        return getFieldByNumber(115, Float.class);
    }

    @Nullable
    public Float getCurrentStress() {
        return getFieldByNumber(116, Float.class);
    }

    @Nullable
    public Integer getEbikeTravelRang() {
        return getFieldByNumber(117, Integer.class);
    }

    @Nullable
    public Integer getEbikeBatteryLevel() {
        return getFieldByNumber(118, Integer.class);
    }

    @Nullable
    public Integer getEbikeAssistMode() {
        return getFieldByNumber(119, Integer.class);
    }

    @Nullable
    public Integer getEbikeAssistLevelPercent() {
        return getFieldByNumber(120, Integer.class);
    }

    @Nullable
    public Long getAirTimeRemaining() {
        return getFieldByNumber(123, Long.class);
    }

    @Nullable
    public Float getPressureSac() {
        return getFieldByNumber(124, Float.class);
    }

    @Nullable
    public Float getVolumeSac() {
        return getFieldByNumber(125, Float.class);
    }

    @Nullable
    public Float getRmv() {
        return getFieldByNumber(126, Float.class);
    }

    @Nullable
    public Double getAscentRate() {
        return getFieldByNumber(127, Double.class);
    }

    @Nullable
    public Float getPo2() {
        return getFieldByNumber(129, Float.class);
    }

    @Nullable
    public Integer getWristHeartRate() {
        return getFieldByNumber(136, Integer.class);
    }

    @Nullable
    public Float getCoreTemperature() {
        return getFieldByNumber(139, Float.class);
    }

    @Nullable
    public Integer getBodyBattery() {
        return getFieldByNumber(143, Integer.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(20);
        }

        public Builder setLatitude(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setLongitude(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAltitude(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHeartRate(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setCadence(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setSpeed(final Float value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setPower(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setCompressedSpeedDistance(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setGrade(final Float value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setResistance(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTimeFromCourse(final Double value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setCycleLength(final Float value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setTemperature(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setSpeed1s(final Number[] value) {
            setFieldByNumber(17, (Object[]) value);
            return this;
        }

        public Builder setCycles(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setTotalCycles(final Long value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setCompressedAccumulatedPower(final Integer value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setAccumulatedPower(final Long value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setLeftRightBalance(final Integer value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setGpsAccuracy(final Integer value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setVerticalSpeed(final Float value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setCalories(final Integer value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setOscillation(final Float value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setStanceTimePercent(final Float value) {
            setFieldByNumber(40, value);
            return this;
        }

        public Builder setStanceTime(final Float value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setActivity(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setLeftTorqueEffectiveness(final Float value) {
            setFieldByNumber(43, value);
            return this;
        }

        public Builder setRightTorqueEffectiveness(final Float value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setLeftPedalSmoothness(final Float value) {
            setFieldByNumber(45, value);
            return this;
        }

        public Builder setRightPedalSmoothness(final Float value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setCombinedPedalSmoothness(final Float value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setTime128(final Float value) {
            setFieldByNumber(48, value);
            return this;
        }

        public Builder setStrokeType(final Integer value) {
            setFieldByNumber(49, value);
            return this;
        }

        public Builder setZone(final Integer value) {
            setFieldByNumber(50, value);
            return this;
        }

        public Builder setBallSpeed(final Float value) {
            setFieldByNumber(51, value);
            return this;
        }

        public Builder setCadence256(final Float value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setFractionalCadence(final Float value) {
            setFieldByNumber(53, value);
            return this;
        }

        public Builder setAvgTotalHemoglobinConc(final Float value) {
            setFieldByNumber(54, value);
            return this;
        }

        public Builder setMinTotalHemoglobinConc(final Float value) {
            setFieldByNumber(55, value);
            return this;
        }

        public Builder setMaxTotalHemoglobinConc(final Float value) {
            setFieldByNumber(56, value);
            return this;
        }

        public Builder setAvgSaturatedHemoglobinPercent(final Float value) {
            setFieldByNumber(57, value);
            return this;
        }

        public Builder setMinSaturatedHemoglobinPercent(final Float value) {
            setFieldByNumber(58, value);
            return this;
        }

        public Builder setMaxSaturatedHemoglobinPercent(final Float value) {
            setFieldByNumber(59, value);
            return this;
        }

        public Builder setDeviceIndex(final Integer value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setLeftPco(final Integer value) {
            setFieldByNumber(67, value);
            return this;
        }

        public Builder setRightPco(final Integer value) {
            setFieldByNumber(68, value);
            return this;
        }

        public Builder setLeftPowerPhase(final Number[] value) {
            setFieldByNumber(69, (Object[]) value);
            return this;
        }

        public Builder setLeftPowerPhasePeak(final Number[] value) {
            setFieldByNumber(70, (Object[]) value);
            return this;
        }

        public Builder setRightPowerPhase(final Number[] value) {
            setFieldByNumber(71, (Object[]) value);
            return this;
        }

        public Builder setRightPowerPhasePeak(final Number[] value) {
            setFieldByNumber(72, (Object[]) value);
            return this;
        }

        public Builder setEnhancedSpeed(final Double value) {
            setFieldByNumber(73, value);
            return this;
        }

        public Builder setEnhancedAltitude(final Double value) {
            setFieldByNumber(78, value);
            return this;
        }

        public Builder setBatterySoc(final Float value) {
            setFieldByNumber(81, value);
            return this;
        }

        public Builder setMotorPower(final Integer value) {
            setFieldByNumber(82, value);
            return this;
        }

        public Builder setVerticalRatio(final Float value) {
            setFieldByNumber(83, value);
            return this;
        }

        public Builder setStanceTimeBalance(final Float value) {
            setFieldByNumber(84, value);
            return this;
        }

        public Builder setStepLength(final Float value) {
            setFieldByNumber(85, value);
            return this;
        }

        public Builder setCycleLength16(final Float value) {
            setFieldByNumber(87, value);
            return this;
        }

        public Builder setAbsolutePressure(final Long value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setDepth(final Double value) {
            setFieldByNumber(92, value);
            return this;
        }

        public Builder setNextStopDepth(final Double value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setNextStopTime(final Long value) {
            setFieldByNumber(94, value);
            return this;
        }

        public Builder setTimeToSurface(final Long value) {
            setFieldByNumber(95, value);
            return this;
        }

        public Builder setNdlTime(final Long value) {
            setFieldByNumber(96, value);
            return this;
        }

        public Builder setCnsLoad(final Integer value) {
            setFieldByNumber(97, value);
            return this;
        }

        public Builder setN2Load(final Integer value) {
            setFieldByNumber(98, value);
            return this;
        }

        public Builder setRespirationRate(final Integer value) {
            setFieldByNumber(99, value);
            return this;
        }

        public Builder setEnhancedRespirationRate(final Float value) {
            setFieldByNumber(108, value);
            return this;
        }

        public Builder setGrit(final Float value) {
            setFieldByNumber(114, value);
            return this;
        }

        public Builder setFlow(final Float value) {
            setFieldByNumber(115, value);
            return this;
        }

        public Builder setCurrentStress(final Float value) {
            setFieldByNumber(116, value);
            return this;
        }

        public Builder setEbikeTravelRang(final Integer value) {
            setFieldByNumber(117, value);
            return this;
        }

        public Builder setEbikeBatteryLevel(final Integer value) {
            setFieldByNumber(118, value);
            return this;
        }

        public Builder setEbikeAssistMode(final Integer value) {
            setFieldByNumber(119, value);
            return this;
        }

        public Builder setEbikeAssistLevelPercent(final Integer value) {
            setFieldByNumber(120, value);
            return this;
        }

        public Builder setAirTimeRemaining(final Long value) {
            setFieldByNumber(123, value);
            return this;
        }

        public Builder setPressureSac(final Float value) {
            setFieldByNumber(124, value);
            return this;
        }

        public Builder setVolumeSac(final Float value) {
            setFieldByNumber(125, value);
            return this;
        }

        public Builder setRmv(final Float value) {
            setFieldByNumber(126, value);
            return this;
        }

        public Builder setAscentRate(final Double value) {
            setFieldByNumber(127, value);
            return this;
        }

        public Builder setPo2(final Float value) {
            setFieldByNumber(129, value);
            return this;
        }

        public Builder setWristHeartRate(final Integer value) {
            setFieldByNumber(136, value);
            return this;
        }

        public Builder setCoreTemperature(final Float value) {
            setFieldByNumber(139, value);
            return this;
        }

        public Builder setBodyBattery(final Integer value) {
            setFieldByNumber(143, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitRecord build() {
            return (FitRecord) super.build();
        }

        @Override
        public FitRecord build(final int localMessageType) {
            return (FitRecord) super.build(localMessageType);
        }
    }

    // manual changes below

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getLatitude() != null && getLongitude() != null) {
            activityPoint.setLocation(new GPSCoordinate(
                    getLongitude(),
                    getLatitude(),
                    getEnhancedAltitude() != null ? getEnhancedAltitude() : GPSCoordinate.UNKNOWN_ALTITUDE
            ));
        }
        if (getHeartRate() != null) {
            activityPoint.setHeartRate(getHeartRate());
        }
        if (getEnhancedSpeed() != null) {
            activityPoint.setSpeed(getEnhancedSpeed().floatValue());
        }
        if (getCadence() != null) {
            activityPoint.setCadence(getCadence());
        }
        if (getPower() != null) {
            activityPoint.setPower(getPower());
        }
        if (getEnhancedRespirationRate() != null) {
            activityPoint.setRespiratoryRate(getEnhancedRespirationRate());
        }
        if (getTemperature() != null) {
            activityPoint.setTemperature(getTemperature());
        }
        if (getDepth() != null) {
            activityPoint.setDepth(getDepth());
        }

        return activityPoint;
    }
}
