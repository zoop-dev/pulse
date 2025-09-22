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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle.SwimStyle;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitLap extends RecordData {
    public FitLap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 19) {
            throw new IllegalArgumentException("FitLap expects global messages of " + 19 + ", got " + globalNumber);
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
    public Double getStartLat() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartLong() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Double getEndLat() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Double getEndLong() {
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
    public Integer getIntensity() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Integer getLapTrigger() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(25);
    }

    @Nullable
    public Integer getEventGroup() {
        return (Integer) getFieldByNumber(26);
    }

    @Nullable
    public Integer getNumLengths() {
        return (Integer) getFieldByNumber(32);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return (Integer) getFieldByNumber(33);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return (Integer) getFieldByNumber(34);
    }

    @Nullable
    public Integer getFirstLengthIndex() {
        return (Integer) getFieldByNumber(35);
    }

    @Nullable
    public Integer getAvgStrokeDistance() {
        return (Integer) getFieldByNumber(37);
    }

    @Nullable
    public SwimStyle getSwimStyle() {
        return (SwimStyle) getFieldByNumber(38);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(39);
    }

    @Nullable
    public Integer getNumActiveLengths() {
        return (Integer) getFieldByNumber(40);
    }

    @Nullable
    public Long getTotalWork() {
        return (Long) getFieldByNumber(41);
    }

    @Nullable
    public Float getAvgAltitude() {
        return (Float) getFieldByNumber(42);
    }

    @Nullable
    public Float getMaxAltitude() {
        return (Float) getFieldByNumber(43);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return (Integer) getFieldByNumber(44);
    }

    @Nullable
    public Float getAvgGrade() {
        return (Float) getFieldByNumber(45);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return (Float) getFieldByNumber(46);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return (Float) getFieldByNumber(47);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return (Float) getFieldByNumber(48);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return (Float) getFieldByNumber(49);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return (Integer) getFieldByNumber(50);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return (Integer) getFieldByNumber(51);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return (Double) getFieldByNumber(52);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return (Float) getFieldByNumber(53);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return (Float) getFieldByNumber(54);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return (Float) getFieldByNumber(55);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return (Float) getFieldByNumber(56);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        final Object object = getFieldByNumber(57);
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
        final Object object = getFieldByNumber(58);
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
        final Object object = getFieldByNumber(59);
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
        final Object object = getFieldByNumber(60);
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
        return (Integer) getFieldByNumber(61);
    }

    @Nullable
    public Float getMinAltitude() {
        return (Float) getFieldByNumber(62);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return (Integer) getFieldByNumber(63);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return (Integer) getFieldByNumber(71);
    }

    @Nullable
    public Integer getAvgSwolf() {
        return (Integer) getFieldByNumber(73);
    }

    @Nullable
    public Integer getOpponentScore() {
        return (Integer) getFieldByNumber(74);
    }

    @Nullable
    public Number[] getStrokeCount() {
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
    public Number[] getZoneCount() {
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
    public Float getAvgVerticalOscillation() {
        return (Float) getFieldByNumber(77);
    }

    @Nullable
    public Float getAvgStanceTimePercent() {
        return (Float) getFieldByNumber(78);
    }

    @Nullable
    public Float getAvgStanceTime() {
        return (Float) getFieldByNumber(79);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return (Float) getFieldByNumber(80);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return (Float) getFieldByNumber(81);
    }

    @Nullable
    public Float getTotalFractionalCycles() {
        return (Float) getFieldByNumber(82);
    }

    @Nullable
    public Integer getPlayerScore() {
        return (Integer) getFieldByNumber(83);
    }

    @Nullable
    public Number[] getAvgTotalHemoglobinConc() {
        final Object object = getFieldByNumber(84);
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
    public Number[] getMinTotalHemoglobinConc() {
        final Object object = getFieldByNumber(85);
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
    public Number[] getMaxTotalHemoglobinConc() {
        final Object object = getFieldByNumber(86);
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
    public Number[] getAvgSaturatedHemoglobinPercent() {
        final Object object = getFieldByNumber(87);
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
    public Number[] getMinSaturatedHemoglobinPercent() {
        final Object object = getFieldByNumber(88);
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
    public Number[] getMaxSaturatedHemoglobinPercent() {
        final Object object = getFieldByNumber(89);
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
    public Float getAvgLeftTorqueEffectiveness() {
        return (Float) getFieldByNumber(91);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return (Float) getFieldByNumber(92);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return (Float) getFieldByNumber(93);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return (Float) getFieldByNumber(94);
    }

    @Nullable
    public Float getAvgCombinedPedalSmoothness() {
        return (Float) getFieldByNumber(95);
    }

    @Nullable
    public Double getTimeStanding() {
        return (Double) getFieldByNumber(98);
    }

    @Nullable
    public Integer getStandCount() {
        return (Integer) getFieldByNumber(99);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return (Integer) getFieldByNumber(100);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return (Integer) getFieldByNumber(101);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        final Object object = getFieldByNumber(102);
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
        final Object object = getFieldByNumber(103);
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
        final Object object = getFieldByNumber(104);
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
        final Object object = getFieldByNumber(105);
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
        final Object object = getFieldByNumber(106);
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
        final Object object = getFieldByNumber(107);
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
        final Object object = getFieldByNumber(108);
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
        final Object object = getFieldByNumber(109);
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
    public Double getEnhancedAvgSpeed() {
        return (Double) getFieldByNumber(110);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return (Double) getFieldByNumber(111);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return (Double) getFieldByNumber(112);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return (Double) getFieldByNumber(113);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return (Double) getFieldByNumber(114);
    }

    @Nullable
    public Integer getAvgLevMotorPower() {
        return (Integer) getFieldByNumber(115);
    }

    @Nullable
    public Integer getMaxLevMotorPower() {
        return (Integer) getFieldByNumber(116);
    }

    @Nullable
    public Float getLevBatteryConsumption() {
        return (Float) getFieldByNumber(117);
    }

    @Nullable
    public Float getAvgVerticalRatio() {
        return (Float) getFieldByNumber(118);
    }

    @Nullable
    public Float getAvgStanceTimeBalance() {
        return (Float) getFieldByNumber(119);
    }

    @Nullable
    public Float getAvgStepLength() {
        return (Float) getFieldByNumber(120);
    }

    @Nullable
    public Float getAvgVam() {
        return (Float) getFieldByNumber(121);
    }

    @Nullable
    public Double getAvgDepth() {
        return (Double) getFieldByNumber(122);
    }

    @Nullable
    public Double getMaxDepth() {
        return (Double) getFieldByNumber(123);
    }

    @Nullable
    public Integer getMinTemperature() {
        return (Integer) getFieldByNumber(124);
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return (Float) getFieldByNumber(136);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return (Float) getFieldByNumber(137);
    }

    @Nullable
    public Integer getAvgRespirationRate() {
        return (Integer) getFieldByNumber(147);
    }

    @Nullable
    public Integer getMaxRespirationRate() {
        return (Integer) getFieldByNumber(148);
    }

    @Nullable
    public Float getTotalGrit() {
        return (Float) getFieldByNumber(149);
    }

    @Nullable
    public Float getTotalFlow() {
        return (Float) getFieldByNumber(150);
    }

    @Nullable
    public Integer getJumpCount() {
        return (Integer) getFieldByNumber(151);
    }

    @Nullable
    public Float getAvgGrit() {
        return (Float) getFieldByNumber(153);
    }

    @Nullable
    public Float getAvgFlow() {
        return (Float) getFieldByNumber(154);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return (Float) getFieldByNumber(156);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return (Float) getFieldByNumber(157);
    }

    @Nullable
    public Float getAvgCoreTemperature() {
        return (Float) getFieldByNumber(158);
    }

    @Nullable
    public Float getMinCoreTemperature() {
        return (Float) getFieldByNumber(159);
    }

    @Nullable
    public Float getMaxCoreTemperature() {
        return (Float) getFieldByNumber(160);
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
            super(19);
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

        public Builder setStartLat(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setStartLong(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setEndLat(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setEndLong(final Double value) {
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

        public Builder setIntensity(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setLapTrigger(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setEventGroup(final Integer value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setNumLengths(final Integer value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setNormalizedPower(final Integer value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setLeftRightBalance(final Integer value) {
            setFieldByNumber(34, value);
            return this;
        }

        public Builder setFirstLengthIndex(final Integer value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setAvgStrokeDistance(final Integer value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setSwimStyle(final SwimStyle value) {
            setFieldByNumber(38, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setNumActiveLengths(final Integer value) {
            setFieldByNumber(40, value);
            return this;
        }

        public Builder setTotalWork(final Long value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setAvgAltitude(final Float value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setMaxAltitude(final Float value) {
            setFieldByNumber(43, value);
            return this;
        }

        public Builder setGpsAccuracy(final Integer value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setAvgGrade(final Float value) {
            setFieldByNumber(45, value);
            return this;
        }

        public Builder setAvgPosGrade(final Float value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setAvgNegGrade(final Float value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setMaxPosGrade(final Float value) {
            setFieldByNumber(48, value);
            return this;
        }

        public Builder setMaxNegGrade(final Float value) {
            setFieldByNumber(49, value);
            return this;
        }

        public Builder setAvgTemperature(final Integer value) {
            setFieldByNumber(50, value);
            return this;
        }

        public Builder setMaxTemperature(final Integer value) {
            setFieldByNumber(51, value);
            return this;
        }

        public Builder setTotalMovingTime(final Double value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setAvgPosVerticalSpeed(final Float value) {
            setFieldByNumber(53, value);
            return this;
        }

        public Builder setAvgNegVerticalSpeed(final Float value) {
            setFieldByNumber(54, value);
            return this;
        }

        public Builder setMaxPosVerticalSpeed(final Float value) {
            setFieldByNumber(55, value);
            return this;
        }

        public Builder setMaxNegVerticalSpeed(final Float value) {
            setFieldByNumber(56, value);
            return this;
        }

        public Builder setTimeInHrZone(final Number[] value) {
            setFieldByNumber(57, (Object[]) value);
            return this;
        }

        public Builder setTimeInSpeedZone(final Number[] value) {
            setFieldByNumber(58, (Object[]) value);
            return this;
        }

        public Builder setTimeInCadenceZone(final Number[] value) {
            setFieldByNumber(59, (Object[]) value);
            return this;
        }

        public Builder setTimeInPowerZone(final Number[] value) {
            setFieldByNumber(60, (Object[]) value);
            return this;
        }

        public Builder setRepetitionNum(final Integer value) {
            setFieldByNumber(61, value);
            return this;
        }

        public Builder setMinAltitude(final Float value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setMinHeartRate(final Integer value) {
            setFieldByNumber(63, value);
            return this;
        }

        public Builder setWktStepIndex(final Integer value) {
            setFieldByNumber(71, value);
            return this;
        }

        public Builder setAvgSwolf(final Integer value) {
            setFieldByNumber(73, value);
            return this;
        }

        public Builder setOpponentScore(final Integer value) {
            setFieldByNumber(74, value);
            return this;
        }

        public Builder setStrokeCount(final Number[] value) {
            setFieldByNumber(75, (Object[]) value);
            return this;
        }

        public Builder setZoneCount(final Number[] value) {
            setFieldByNumber(76, (Object[]) value);
            return this;
        }

        public Builder setAvgVerticalOscillation(final Float value) {
            setFieldByNumber(77, value);
            return this;
        }

        public Builder setAvgStanceTimePercent(final Float value) {
            setFieldByNumber(78, value);
            return this;
        }

        public Builder setAvgStanceTime(final Float value) {
            setFieldByNumber(79, value);
            return this;
        }

        public Builder setAvgFractionalCadence(final Float value) {
            setFieldByNumber(80, value);
            return this;
        }

        public Builder setMaxFractionalCadence(final Float value) {
            setFieldByNumber(81, value);
            return this;
        }

        public Builder setTotalFractionalCycles(final Float value) {
            setFieldByNumber(82, value);
            return this;
        }

        public Builder setPlayerScore(final Integer value) {
            setFieldByNumber(83, value);
            return this;
        }

        public Builder setAvgTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(84, (Object[]) value);
            return this;
        }

        public Builder setMinTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(85, (Object[]) value);
            return this;
        }

        public Builder setMaxTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(86, (Object[]) value);
            return this;
        }

        public Builder setAvgSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(87, (Object[]) value);
            return this;
        }

        public Builder setMinSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(88, (Object[]) value);
            return this;
        }

        public Builder setMaxSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(89, (Object[]) value);
            return this;
        }

        public Builder setAvgLeftTorqueEffectiveness(final Float value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setAvgRightTorqueEffectiveness(final Float value) {
            setFieldByNumber(92, value);
            return this;
        }

        public Builder setAvgLeftPedalSmoothness(final Float value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setAvgRightPedalSmoothness(final Float value) {
            setFieldByNumber(94, value);
            return this;
        }

        public Builder setAvgCombinedPedalSmoothness(final Float value) {
            setFieldByNumber(95, value);
            return this;
        }

        public Builder setTimeStanding(final Double value) {
            setFieldByNumber(98, value);
            return this;
        }

        public Builder setStandCount(final Integer value) {
            setFieldByNumber(99, value);
            return this;
        }

        public Builder setAvgLeftPco(final Integer value) {
            setFieldByNumber(100, value);
            return this;
        }

        public Builder setAvgRightPco(final Integer value) {
            setFieldByNumber(101, value);
            return this;
        }

        public Builder setAvgLeftPowerPhase(final Number[] value) {
            setFieldByNumber(102, (Object[]) value);
            return this;
        }

        public Builder setAvgLeftPowerPhasePeak(final Number[] value) {
            setFieldByNumber(103, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhase(final Number[] value) {
            setFieldByNumber(104, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhasePeak(final Number[] value) {
            setFieldByNumber(105, (Object[]) value);
            return this;
        }

        public Builder setAvgPowerPosition(final Number[] value) {
            setFieldByNumber(106, (Object[]) value);
            return this;
        }

        public Builder setMaxPowerPosition(final Number[] value) {
            setFieldByNumber(107, (Object[]) value);
            return this;
        }

        public Builder setAvgCadencePosition(final Number[] value) {
            setFieldByNumber(108, (Object[]) value);
            return this;
        }

        public Builder setMaxCadencePosition(final Number[] value) {
            setFieldByNumber(109, (Object[]) value);
            return this;
        }

        public Builder setEnhancedAvgSpeed(final Double value) {
            setFieldByNumber(110, value);
            return this;
        }

        public Builder setEnhancedMaxSpeed(final Double value) {
            setFieldByNumber(111, value);
            return this;
        }

        public Builder setEnhancedAvgAltitude(final Double value) {
            setFieldByNumber(112, value);
            return this;
        }

        public Builder setEnhancedMinAltitude(final Double value) {
            setFieldByNumber(113, value);
            return this;
        }

        public Builder setEnhancedMaxAltitude(final Double value) {
            setFieldByNumber(114, value);
            return this;
        }

        public Builder setAvgLevMotorPower(final Integer value) {
            setFieldByNumber(115, value);
            return this;
        }

        public Builder setMaxLevMotorPower(final Integer value) {
            setFieldByNumber(116, value);
            return this;
        }

        public Builder setLevBatteryConsumption(final Float value) {
            setFieldByNumber(117, value);
            return this;
        }

        public Builder setAvgVerticalRatio(final Float value) {
            setFieldByNumber(118, value);
            return this;
        }

        public Builder setAvgStanceTimeBalance(final Float value) {
            setFieldByNumber(119, value);
            return this;
        }

        public Builder setAvgStepLength(final Float value) {
            setFieldByNumber(120, value);
            return this;
        }

        public Builder setAvgVam(final Float value) {
            setFieldByNumber(121, value);
            return this;
        }

        public Builder setAvgDepth(final Double value) {
            setFieldByNumber(122, value);
            return this;
        }

        public Builder setMaxDepth(final Double value) {
            setFieldByNumber(123, value);
            return this;
        }

        public Builder setMinTemperature(final Integer value) {
            setFieldByNumber(124, value);
            return this;
        }

        public Builder setEnhancedAvgRespirationRate(final Float value) {
            setFieldByNumber(136, value);
            return this;
        }

        public Builder setEnhancedMaxRespirationRate(final Float value) {
            setFieldByNumber(137, value);
            return this;
        }

        public Builder setAvgRespirationRate(final Integer value) {
            setFieldByNumber(147, value);
            return this;
        }

        public Builder setMaxRespirationRate(final Integer value) {
            setFieldByNumber(148, value);
            return this;
        }

        public Builder setTotalGrit(final Float value) {
            setFieldByNumber(149, value);
            return this;
        }

        public Builder setTotalFlow(final Float value) {
            setFieldByNumber(150, value);
            return this;
        }

        public Builder setJumpCount(final Integer value) {
            setFieldByNumber(151, value);
            return this;
        }

        public Builder setAvgGrit(final Float value) {
            setFieldByNumber(153, value);
            return this;
        }

        public Builder setAvgFlow(final Float value) {
            setFieldByNumber(154, value);
            return this;
        }

        public Builder setTotalFractionalAscent(final Float value) {
            setFieldByNumber(156, value);
            return this;
        }

        public Builder setTotalFractionalDescent(final Float value) {
            setFieldByNumber(157, value);
            return this;
        }

        public Builder setAvgCoreTemperature(final Float value) {
            setFieldByNumber(158, value);
            return this;
        }

        public Builder setMinCoreTemperature(final Float value) {
            setFieldByNumber(159, value);
            return this;
        }

        public Builder setMaxCoreTemperature(final Float value) {
            setFieldByNumber(160, value);
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
        public FitLap build() {
            return (FitLap) super.build();
        }
    }
}
