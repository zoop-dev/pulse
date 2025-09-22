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
public class FitSegmentLap extends RecordData {
    public FitSegmentLap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 142) {
            throw new IllegalArgumentException("FitSegmentLap expects global messages of " + 142 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getEvent() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getEventType() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Double getStartPositionLat() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartPositionLong() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Double getEndPositionLat() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Double getEndPositionLong() {
        return (Double) getFieldByNumber(6);
    }

    @Nullable
    public Double getTotalElapsedTime() {
        return (Double) getFieldByNumber(7);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return (Double) getFieldByNumber(8);
    }

    @Nullable
    public Double getTotalDistance() {
        return (Double) getFieldByNumber(9);
    }

    @Nullable
    public Long getTotalCycles() {
        return (Long) getFieldByNumber(10);
    }

    @Nullable
    public Integer getTotalCalories() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getTotalFatCalories() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Float getAvgSpeed() {
        return (Float) getFieldByNumber(13);
    }

    @Nullable
    public Float getMaxSpeed() {
        return (Float) getFieldByNumber(14);
    }

    @Nullable
    public Integer getAvgHeartRate() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getAvgCadence() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getMaxCadence() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Integer getAvgPower() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Integer getMaxPower() {
        return (Integer) getFieldByNumber(20);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Integer getEventGroup() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Double getNecLat() {
        return (Double) getFieldByNumber(25);
    }

    @Nullable
    public Double getNecLong() {
        return (Double) getFieldByNumber(26);
    }

    @Nullable
    public Double getSwcLat() {
        return (Double) getFieldByNumber(27);
    }

    @Nullable
    public Double getSwcLong() {
        return (Double) getFieldByNumber(28);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(29);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return (Integer) getFieldByNumber(30);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return (Integer) getFieldByNumber(31);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(32);
    }

    @Nullable
    public Long getTotalWork() {
        return (Long) getFieldByNumber(33);
    }

    @Nullable
    public Float getAvgAltitude() {
        return (Float) getFieldByNumber(34);
    }

    @Nullable
    public Float getMaxAltitude() {
        return (Float) getFieldByNumber(35);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return (Integer) getFieldByNumber(36);
    }

    @Nullable
    public Float getAvgGrade() {
        return (Float) getFieldByNumber(37);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return (Float) getFieldByNumber(38);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return (Float) getFieldByNumber(39);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return (Float) getFieldByNumber(40);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return (Float) getFieldByNumber(41);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return (Integer) getFieldByNumber(42);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return (Integer) getFieldByNumber(43);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return (Double) getFieldByNumber(44);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return (Float) getFieldByNumber(45);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return (Float) getFieldByNumber(46);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return (Float) getFieldByNumber(47);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return (Float) getFieldByNumber(48);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        final Object object = getFieldByNumber(49);
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
    public Number[] getTimeInSpeedZone() {
        final Object object = getFieldByNumber(50);
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
    public Number[] getTimeInCadenceZone() {
        final Object object = getFieldByNumber(51);
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
    public Number[] getTimeInPowerZone() {
        final Object object = getFieldByNumber(52);
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
    public Integer getRepetitionNum() {
        return (Integer) getFieldByNumber(53);
    }

    @Nullable
    public Float getMinAltitude() {
        return (Float) getFieldByNumber(54);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return (Integer) getFieldByNumber(55);
    }

    @Nullable
    public Double getActiveTime() {
        return (Double) getFieldByNumber(56);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return (Integer) getFieldByNumber(57);
    }

    @Nullable
    public Integer getSportEvent() {
        return (Integer) getFieldByNumber(58);
    }

    @Nullable
    public Float getAvgLeftTorqueEffectiveness() {
        return (Float) getFieldByNumber(59);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return (Float) getFieldByNumber(60);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return (Float) getFieldByNumber(61);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return (Float) getFieldByNumber(62);
    }

    @Nullable
    public Float getAvgCombinedPedalSmoothness() {
        return (Float) getFieldByNumber(63);
    }

    @Nullable
    public Integer getStatus() {
        return (Integer) getFieldByNumber(64);
    }

    @Nullable
    public String getUuid() {
        return (String) getFieldByNumber(65);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return (Float) getFieldByNumber(66);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return (Float) getFieldByNumber(67);
    }

    @Nullable
    public Float getTotalFractionalCycles() {
        return (Float) getFieldByNumber(68);
    }

    @Nullable
    public Integer getFrontGearShiftCount() {
        return (Integer) getFieldByNumber(69);
    }

    @Nullable
    public Integer getRearGearShiftCount() {
        return (Integer) getFieldByNumber(70);
    }

    @Nullable
    public Double getTimeStanding() {
        return (Double) getFieldByNumber(71);
    }

    @Nullable
    public Integer getStandCount() {
        return (Integer) getFieldByNumber(72);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return (Integer) getFieldByNumber(73);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return (Integer) getFieldByNumber(74);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        final Object object = getFieldByNumber(75);
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
    public Number[] getAvgLeftPowerPhasePeak() {
        final Object object = getFieldByNumber(76);
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
    public Number[] getAvgRightPowerPhase() {
        final Object object = getFieldByNumber(77);
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
    public Number[] getAvgRightPowerPhasePeak() {
        final Object object = getFieldByNumber(78);
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
    public Number[] getAvgPowerPosition() {
        final Object object = getFieldByNumber(79);
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
    public Number[] getMaxPowerPosition() {
        final Object object = getFieldByNumber(80);
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
    public Number[] getAvgCadencePosition() {
        final Object object = getFieldByNumber(81);
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
    public Number[] getMaxCadencePosition() {
        final Object object = getFieldByNumber(82);
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
    public Integer getManufacturer() {
        return (Integer) getFieldByNumber(83);
    }

    @Nullable
    public Float getTotalGrit() {
        return (Float) getFieldByNumber(84);
    }

    @Nullable
    public Float getTotalFlow() {
        return (Float) getFieldByNumber(85);
    }

    @Nullable
    public Float getAvgGrit() {
        return (Float) getFieldByNumber(86);
    }

    @Nullable
    public Float getAvgFlow() {
        return (Float) getFieldByNumber(87);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return (Float) getFieldByNumber(89);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return (Float) getFieldByNumber(90);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return (Double) getFieldByNumber(91);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return (Double) getFieldByNumber(92);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return (Double) getFieldByNumber(93);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(142);
        }

        public Builder setEvent(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setEventType(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setStartPositionLat(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setStartPositionLong(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setEndPositionLat(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setEndPositionLong(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTotalElapsedTime(final Double value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTotalTimerTime(final Double value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setTotalDistance(final Double value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTotalCycles(final Long value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTotalCalories(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setTotalFatCalories(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAvgSpeed(final Float value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setMaxSpeed(final Float value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setAvgHeartRate(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setAvgCadence(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setMaxCadence(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setAvgPower(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setMaxPower(final Integer value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setEventGroup(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setNecLat(final Double value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setNecLong(final Double value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setSwcLat(final Double value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setSwcLong(final Double value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setNormalizedPower(final Integer value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setLeftRightBalance(final Integer value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setTotalWork(final Long value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setAvgAltitude(final Float value) {
            setFieldByNumber(34, value);
            return this;
        }

        public Builder setMaxAltitude(final Float value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setGpsAccuracy(final Integer value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setAvgGrade(final Float value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setAvgPosGrade(final Float value) {
            setFieldByNumber(38, value);
            return this;
        }

        public Builder setAvgNegGrade(final Float value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setMaxPosGrade(final Float value) {
            setFieldByNumber(40, value);
            return this;
        }

        public Builder setMaxNegGrade(final Float value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setAvgTemperature(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setMaxTemperature(final Integer value) {
            setFieldByNumber(43, value);
            return this;
        }

        public Builder setTotalMovingTime(final Double value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setAvgPosVerticalSpeed(final Float value) {
            setFieldByNumber(45, value);
            return this;
        }

        public Builder setAvgNegVerticalSpeed(final Float value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setMaxPosVerticalSpeed(final Float value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setMaxNegVerticalSpeed(final Float value) {
            setFieldByNumber(48, value);
            return this;
        }

        public Builder setTimeInHrZone(final Number[] value) {
            setFieldByNumber(49, (Object[]) value);
            return this;
        }

        public Builder setTimeInSpeedZone(final Number[] value) {
            setFieldByNumber(50, (Object[]) value);
            return this;
        }

        public Builder setTimeInCadenceZone(final Number[] value) {
            setFieldByNumber(51, (Object[]) value);
            return this;
        }

        public Builder setTimeInPowerZone(final Number[] value) {
            setFieldByNumber(52, (Object[]) value);
            return this;
        }

        public Builder setRepetitionNum(final Integer value) {
            setFieldByNumber(53, value);
            return this;
        }

        public Builder setMinAltitude(final Float value) {
            setFieldByNumber(54, value);
            return this;
        }

        public Builder setMinHeartRate(final Integer value) {
            setFieldByNumber(55, value);
            return this;
        }

        public Builder setActiveTime(final Double value) {
            setFieldByNumber(56, value);
            return this;
        }

        public Builder setWktStepIndex(final Integer value) {
            setFieldByNumber(57, value);
            return this;
        }

        public Builder setSportEvent(final Integer value) {
            setFieldByNumber(58, value);
            return this;
        }

        public Builder setAvgLeftTorqueEffectiveness(final Float value) {
            setFieldByNumber(59, value);
            return this;
        }

        public Builder setAvgRightTorqueEffectiveness(final Float value) {
            setFieldByNumber(60, value);
            return this;
        }

        public Builder setAvgLeftPedalSmoothness(final Float value) {
            setFieldByNumber(61, value);
            return this;
        }

        public Builder setAvgRightPedalSmoothness(final Float value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setAvgCombinedPedalSmoothness(final Float value) {
            setFieldByNumber(63, value);
            return this;
        }

        public Builder setStatus(final Integer value) {
            setFieldByNumber(64, value);
            return this;
        }

        public Builder setUuid(final String value) {
            setFieldByNumber(65, value);
            return this;
        }

        public Builder setAvgFractionalCadence(final Float value) {
            setFieldByNumber(66, value);
            return this;
        }

        public Builder setMaxFractionalCadence(final Float value) {
            setFieldByNumber(67, value);
            return this;
        }

        public Builder setTotalFractionalCycles(final Float value) {
            setFieldByNumber(68, value);
            return this;
        }

        public Builder setFrontGearShiftCount(final Integer value) {
            setFieldByNumber(69, value);
            return this;
        }

        public Builder setRearGearShiftCount(final Integer value) {
            setFieldByNumber(70, value);
            return this;
        }

        public Builder setTimeStanding(final Double value) {
            setFieldByNumber(71, value);
            return this;
        }

        public Builder setStandCount(final Integer value) {
            setFieldByNumber(72, value);
            return this;
        }

        public Builder setAvgLeftPco(final Integer value) {
            setFieldByNumber(73, value);
            return this;
        }

        public Builder setAvgRightPco(final Integer value) {
            setFieldByNumber(74, value);
            return this;
        }

        public Builder setAvgLeftPowerPhase(final Number[] value) {
            setFieldByNumber(75, (Object[]) value);
            return this;
        }

        public Builder setAvgLeftPowerPhasePeak(final Number[] value) {
            setFieldByNumber(76, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhase(final Number[] value) {
            setFieldByNumber(77, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhasePeak(final Number[] value) {
            setFieldByNumber(78, (Object[]) value);
            return this;
        }

        public Builder setAvgPowerPosition(final Number[] value) {
            setFieldByNumber(79, (Object[]) value);
            return this;
        }

        public Builder setMaxPowerPosition(final Number[] value) {
            setFieldByNumber(80, (Object[]) value);
            return this;
        }

        public Builder setAvgCadencePosition(final Number[] value) {
            setFieldByNumber(81, (Object[]) value);
            return this;
        }

        public Builder setMaxCadencePosition(final Number[] value) {
            setFieldByNumber(82, (Object[]) value);
            return this;
        }

        public Builder setManufacturer(final Integer value) {
            setFieldByNumber(83, value);
            return this;
        }

        public Builder setTotalGrit(final Float value) {
            setFieldByNumber(84, value);
            return this;
        }

        public Builder setTotalFlow(final Float value) {
            setFieldByNumber(85, value);
            return this;
        }

        public Builder setAvgGrit(final Float value) {
            setFieldByNumber(86, value);
            return this;
        }

        public Builder setAvgFlow(final Float value) {
            setFieldByNumber(87, value);
            return this;
        }

        public Builder setTotalFractionalAscent(final Float value) {
            setFieldByNumber(89, value);
            return this;
        }

        public Builder setTotalFractionalDescent(final Float value) {
            setFieldByNumber(90, value);
            return this;
        }

        public Builder setEnhancedAvgAltitude(final Double value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setEnhancedMaxAltitude(final Double value) {
            setFieldByNumber(92, value);
            return this;
        }

        public Builder setEnhancedMinAltitude(final Double value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSegmentLap build() {
            return (FitSegmentLap) super.build();
        }
    }
}
