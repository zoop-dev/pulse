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
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Double getLongitude() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Float getAltitude() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHeartRate() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getCadence() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Double getDistance() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Float getSpeed() {
        return (Float) getFieldByNumber(6);
    }

    @Nullable
    public Integer getPower() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Number[] getCompressedSpeedDistance() {
        final Object object = getFieldByNumber(8);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Float getGrade() {
        return (Float) getFieldByNumber(9);
    }

    @Nullable
    public Integer getResistance() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Double getTimeFromCourse() {
        return (Double) getFieldByNumber(11);
    }

    @Nullable
    public Float getCycleLength() {
        return (Float) getFieldByNumber(12);
    }

    @Nullable
    public Integer getTemperature() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Number[] getSpeed1s() {
        final Object object = getFieldByNumber(17);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getCycles() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Long getTotalCycles() {
        return (Long) getFieldByNumber(19);
    }

    @Nullable
    public Integer getCompressedAccumulatedPower() {
        return (Integer) getFieldByNumber(28);
    }

    @Nullable
    public Long getAccumulatedPower() {
        return (Long) getFieldByNumber(29);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return (Integer) getFieldByNumber(30);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return (Integer) getFieldByNumber(31);
    }

    @Nullable
    public Float getVerticalSpeed() {
        return (Float) getFieldByNumber(32);
    }

    @Nullable
    public Integer getCalories() {
        return (Integer) getFieldByNumber(33);
    }

    @Nullable
    public Float getOscillation() {
        return (Float) getFieldByNumber(39);
    }

    @Nullable
    public Float getStanceTimePercent() {
        return (Float) getFieldByNumber(40);
    }

    @Nullable
    public Float getStanceTime() {
        return (Float) getFieldByNumber(41);
    }

    @Nullable
    public Integer getActivity() {
        return (Integer) getFieldByNumber(42);
    }

    @Nullable
    public Float getLeftTorqueEffectiveness() {
        return (Float) getFieldByNumber(43);
    }

    @Nullable
    public Float getRightTorqueEffectiveness() {
        return (Float) getFieldByNumber(44);
    }

    @Nullable
    public Float getLeftPedalSmoothness() {
        return (Float) getFieldByNumber(45);
    }

    @Nullable
    public Float getRightPedalSmoothness() {
        return (Float) getFieldByNumber(46);
    }

    @Nullable
    public Float getCombinedPedalSmoothness() {
        return (Float) getFieldByNumber(47);
    }

    @Nullable
    public Float getTime128() {
        return (Float) getFieldByNumber(48);
    }

    @Nullable
    public Integer getStrokeType() {
        return (Integer) getFieldByNumber(49);
    }

    @Nullable
    public Integer getZone() {
        return (Integer) getFieldByNumber(50);
    }

    @Nullable
    public Float getBallSpeed() {
        return (Float) getFieldByNumber(51);
    }

    @Nullable
    public Float getCadence256() {
        return (Float) getFieldByNumber(52);
    }

    @Nullable
    public Float getFractionalCadence() {
        return (Float) getFieldByNumber(53);
    }

    @Nullable
    public Float getAvgTotalHemoglobinConc() {
        return (Float) getFieldByNumber(54);
    }

    @Nullable
    public Float getMinTotalHemoglobinConc() {
        return (Float) getFieldByNumber(55);
    }

    @Nullable
    public Float getMaxTotalHemoglobinConc() {
        return (Float) getFieldByNumber(56);
    }

    @Nullable
    public Float getAvgSaturatedHemoglobinPercent() {
        return (Float) getFieldByNumber(57);
    }

    @Nullable
    public Float getMinSaturatedHemoglobinPercent() {
        return (Float) getFieldByNumber(58);
    }

    @Nullable
    public Float getMaxSaturatedHemoglobinPercent() {
        return (Float) getFieldByNumber(59);
    }

    @Nullable
    public Integer getDeviceIndex() {
        return (Integer) getFieldByNumber(62);
    }

    @Nullable
    public Integer getLeftPco() {
        return (Integer) getFieldByNumber(67);
    }

    @Nullable
    public Integer getRightPco() {
        return (Integer) getFieldByNumber(68);
    }

    @Nullable
    public Number[] getLeftPowerPhase() {
        final Object object = getFieldByNumber(69);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getLeftPowerPhasePeak() {
        final Object object = getFieldByNumber(70);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getRightPowerPhase() {
        final Object object = getFieldByNumber(71);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getRightPowerPhasePeak() {
        final Object object = getFieldByNumber(72);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Double getEnhancedSpeed() {
        return (Double) getFieldByNumber(73);
    }

    @Nullable
    public Double getEnhancedAltitude() {
        return (Double) getFieldByNumber(78);
    }

    @Nullable
    public Float getBatterySoc() {
        return (Float) getFieldByNumber(81);
    }

    @Nullable
    public Integer getMotorPower() {
        return (Integer) getFieldByNumber(82);
    }

    @Nullable
    public Float getVerticalRatio() {
        return (Float) getFieldByNumber(83);
    }

    @Nullable
    public Float getStanceTimeBalance() {
        return (Float) getFieldByNumber(84);
    }

    @Nullable
    public Float getStepLength() {
        return (Float) getFieldByNumber(85);
    }

    @Nullable
    public Float getCycleLength16() {
        return (Float) getFieldByNumber(87);
    }

    @Nullable
    public Long getAbsolutePressure() {
        return (Long) getFieldByNumber(91);
    }

    @Nullable
    public Double getDepth() {
        return (Double) getFieldByNumber(92);
    }

    @Nullable
    public Double getNextStopDepth() {
        return (Double) getFieldByNumber(93);
    }

    @Nullable
    public Long getNextStopTime() {
        return (Long) getFieldByNumber(94);
    }

    @Nullable
    public Long getTimeToSurface() {
        return (Long) getFieldByNumber(95);
    }

    @Nullable
    public Long getNdlTime() {
        return (Long) getFieldByNumber(96);
    }

    @Nullable
    public Integer getCnsLoad() {
        return (Integer) getFieldByNumber(97);
    }

    @Nullable
    public Integer getN2Load() {
        return (Integer) getFieldByNumber(98);
    }

    @Nullable
    public Integer getRespirationRate() {
        return (Integer) getFieldByNumber(99);
    }

    @Nullable
    public Float getEnhancedRespirationRate() {
        return (Float) getFieldByNumber(108);
    }

    @Nullable
    public Float getGrit() {
        return (Float) getFieldByNumber(114);
    }

    @Nullable
    public Float getFlow() {
        return (Float) getFieldByNumber(115);
    }

    @Nullable
    public Float getCurrentStress() {
        return (Float) getFieldByNumber(116);
    }

    @Nullable
    public Integer getEbikeTravelRang() {
        return (Integer) getFieldByNumber(117);
    }

    @Nullable
    public Integer getEbikeBatteryLevel() {
        return (Integer) getFieldByNumber(118);
    }

    @Nullable
    public Integer getEbikeAssistMode() {
        return (Integer) getFieldByNumber(119);
    }

    @Nullable
    public Integer getEbikeAssistLevelPercent() {
        return (Integer) getFieldByNumber(120);
    }

    @Nullable
    public Long getAirTimeRemaining() {
        return (Long) getFieldByNumber(123);
    }

    @Nullable
    public Float getPressureSac() {
        return (Float) getFieldByNumber(124);
    }

    @Nullable
    public Float getVolumeSac() {
        return (Float) getFieldByNumber(125);
    }

    @Nullable
    public Float getRmv() {
        return (Float) getFieldByNumber(126);
    }

    @Nullable
    public Double getAscentRate() {
        return (Double) getFieldByNumber(127);
    }

    @Nullable
    public Float getPo2() {
        return (Float) getFieldByNumber(129);
    }

    @Nullable
    public Integer getWristHeartRate() {
        return (Integer) getFieldByNumber(136);
    }

    @Nullable
    public Float getCoreTemperature() {
        return (Float) getFieldByNumber(139);
    }

    @Nullable
    public Integer getBodyBattery() {
        return (Integer) getFieldByNumber(143);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
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
        return activityPoint;
    }
}
