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
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getEventType() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Long getStartTime() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Double getStartLat() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Double getStartLong() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Double getEndLat() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Double getEndLong() {
        return getFieldByNumber(6, Double.class);
    }

    @Nullable
    public Double getTotalElapsedTime() {
        return getFieldByNumber(7, Double.class);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return getFieldByNumber(8, Double.class);
    }

    @Nullable
    public Double getTotalDistance() {
        return getFieldByNumber(9, Double.class);
    }

    @Nullable
    public Long getTotalCycles() {
        return getFieldByNumber(10, Long.class);
    }

    @Nullable
    public Integer getTotalCalories() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getTotalFatCalories() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Float getAvgSpeed() {
        return getFieldByNumber(13, Float.class);
    }

    @Nullable
    public Float getMaxSpeed() {
        return getFieldByNumber(14, Float.class);
    }

    @Nullable
    public Integer getAvgHeartRate() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getAvgCadence() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getMaxCadence() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getAvgPower() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Integer getMaxPower() {
        return getFieldByNumber(20, Integer.class);
    }

    @Nullable
    public Integer getTotalAscent() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getTotalDescent() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Integer getIntensity() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getLapTrigger() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getEventGroup() {
        return getFieldByNumber(26, Integer.class);
    }

    @Nullable
    public Integer getNumLengths() {
        return getFieldByNumber(32, Integer.class);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return getFieldByNumber(33, Integer.class);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return getFieldByNumber(34, Integer.class);
    }

    @Nullable
    public Integer getFirstLengthIndex() {
        return getFieldByNumber(35, Integer.class);
    }

    @Nullable
    public Integer getAvgStrokeDistance() {
        return getFieldByNumber(37, Integer.class);
    }

    @Nullable
    public SwimStyle getSwimStyle() {
        return getFieldByNumber(38, SwimStyle.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(39, Integer.class);
    }

    @Nullable
    public Integer getNumActiveLengths() {
        return getFieldByNumber(40, Integer.class);
    }

    @Nullable
    public Long getTotalWork() {
        return getFieldByNumber(41, Long.class);
    }

    @Nullable
    public Float getAvgAltitude() {
        return getFieldByNumber(42, Float.class);
    }

    @Nullable
    public Float getMaxAltitude() {
        return getFieldByNumber(43, Float.class);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return getFieldByNumber(44, Integer.class);
    }

    @Nullable
    public Float getAvgGrade() {
        return getFieldByNumber(45, Float.class);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return getFieldByNumber(46, Float.class);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return getFieldByNumber(47, Float.class);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return getFieldByNumber(48, Float.class);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return getFieldByNumber(49, Float.class);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return getFieldByNumber(50, Integer.class);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return getFieldByNumber(51, Integer.class);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return getFieldByNumber(52, Double.class);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return getFieldByNumber(53, Float.class);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return getFieldByNumber(54, Float.class);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return getFieldByNumber(55, Float.class);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return getFieldByNumber(56, Float.class);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        return getArrayFieldByNumber(57, Number.class);
    }

    @Nullable
    public Number[] getTimeInSpeedZone() {
        return getArrayFieldByNumber(58, Number.class);
    }

    @Nullable
    public Number[] getTimeInCadenceZone() {
        return getArrayFieldByNumber(59, Number.class);
    }

    @Nullable
    public Number[] getTimeInPowerZone() {
        return getArrayFieldByNumber(60, Number.class);
    }

    @Nullable
    public Integer getRepetitionNum() {
        return getFieldByNumber(61, Integer.class);
    }

    @Nullable
    public Float getMinAltitude() {
        return getFieldByNumber(62, Float.class);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return getFieldByNumber(63, Integer.class);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return getFieldByNumber(71, Integer.class);
    }

    @Nullable
    public Integer getAvgSwolf() {
        return getFieldByNumber(73, Integer.class);
    }

    @Nullable
    public Integer getOpponentScore() {
        return getFieldByNumber(74, Integer.class);
    }

    @Nullable
    public Number[] getStrokeCount() {
        return getArrayFieldByNumber(75, Number.class);
    }

    @Nullable
    public Number[] getZoneCount() {
        return getArrayFieldByNumber(76, Number.class);
    }

    @Nullable
    public Float getAvgVerticalOscillation() {
        return getFieldByNumber(77, Float.class);
    }

    @Nullable
    public Float getAvgStanceTimePercent() {
        return getFieldByNumber(78, Float.class);
    }

    @Nullable
    public Float getAvgStanceTime() {
        return getFieldByNumber(79, Float.class);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return getFieldByNumber(80, Float.class);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return getFieldByNumber(81, Float.class);
    }

    @Nullable
    public Float getTotalFractionalCycles() {
        return getFieldByNumber(82, Float.class);
    }

    @Nullable
    public Integer getPlayerScore() {
        return getFieldByNumber(83, Integer.class);
    }

    @Nullable
    public Number[] getAvgTotalHemoglobinConc() {
        return getArrayFieldByNumber(84, Number.class);
    }

    @Nullable
    public Number[] getMinTotalHemoglobinConc() {
        return getArrayFieldByNumber(85, Number.class);
    }

    @Nullable
    public Number[] getMaxTotalHemoglobinConc() {
        return getArrayFieldByNumber(86, Number.class);
    }

    @Nullable
    public Number[] getAvgSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(87, Number.class);
    }

    @Nullable
    public Number[] getMinSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(88, Number.class);
    }

    @Nullable
    public Number[] getMaxSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(89, Number.class);
    }

    @Nullable
    public Float getAvgLeftTorqueEffectiveness() {
        return getFieldByNumber(91, Float.class);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return getFieldByNumber(92, Float.class);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return getFieldByNumber(93, Float.class);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return getFieldByNumber(94, Float.class);
    }

    @Nullable
    public Float getAvgCombinedPedalSmoothness() {
        return getFieldByNumber(95, Float.class);
    }

    @Nullable
    public Double getTimeStanding() {
        return getFieldByNumber(98, Double.class);
    }

    @Nullable
    public Integer getStandCount() {
        return getFieldByNumber(99, Integer.class);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return getFieldByNumber(100, Integer.class);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return getFieldByNumber(101, Integer.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        return getArrayFieldByNumber(102, Number.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhasePeak() {
        return getArrayFieldByNumber(103, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhase() {
        return getArrayFieldByNumber(104, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhasePeak() {
        return getArrayFieldByNumber(105, Number.class);
    }

    @Nullable
    public Number[] getAvgPowerPosition() {
        return getArrayFieldByNumber(106, Number.class);
    }

    @Nullable
    public Number[] getMaxPowerPosition() {
        return getArrayFieldByNumber(107, Number.class);
    }

    @Nullable
    public Number[] getAvgCadencePosition() {
        return getArrayFieldByNumber(108, Number.class);
    }

    @Nullable
    public Number[] getMaxCadencePosition() {
        return getArrayFieldByNumber(109, Number.class);
    }

    @Nullable
    public Double getEnhancedAvgSpeed() {
        return getFieldByNumber(110, Double.class);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return getFieldByNumber(111, Double.class);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return getFieldByNumber(112, Double.class);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return getFieldByNumber(113, Double.class);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return getFieldByNumber(114, Double.class);
    }

    @Nullable
    public Integer getAvgLevMotorPower() {
        return getFieldByNumber(115, Integer.class);
    }

    @Nullable
    public Integer getMaxLevMotorPower() {
        return getFieldByNumber(116, Integer.class);
    }

    @Nullable
    public Float getLevBatteryConsumption() {
        return getFieldByNumber(117, Float.class);
    }

    @Nullable
    public Float getAvgVerticalRatio() {
        return getFieldByNumber(118, Float.class);
    }

    @Nullable
    public Float getAvgStanceTimeBalance() {
        return getFieldByNumber(119, Float.class);
    }

    @Nullable
    public Float getAvgStepLength() {
        return getFieldByNumber(120, Float.class);
    }

    @Nullable
    public Float getAvgVam() {
        return getFieldByNumber(121, Float.class);
    }

    @Nullable
    public Double getAvgDepth() {
        return getFieldByNumber(122, Double.class);
    }

    @Nullable
    public Double getMaxDepth() {
        return getFieldByNumber(123, Double.class);
    }

    @Nullable
    public Integer getMinTemperature() {
        return getFieldByNumber(124, Integer.class);
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return getFieldByNumber(136, Float.class);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return getFieldByNumber(137, Float.class);
    }

    @Nullable
    public Integer getAvgRespirationRate() {
        return getFieldByNumber(147, Integer.class);
    }

    @Nullable
    public Integer getMaxRespirationRate() {
        return getFieldByNumber(148, Integer.class);
    }

    @Nullable
    public Float getTotalGrit() {
        return getFieldByNumber(149, Float.class);
    }

    @Nullable
    public Float getTotalFlow() {
        return getFieldByNumber(150, Float.class);
    }

    @Nullable
    public Integer getJumpCount() {
        return getFieldByNumber(151, Integer.class);
    }

    @Nullable
    public Float getAvgGrit() {
        return getFieldByNumber(153, Float.class);
    }

    @Nullable
    public Float getAvgFlow() {
        return getFieldByNumber(154, Float.class);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return getFieldByNumber(156, Float.class);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return getFieldByNumber(157, Float.class);
    }

    @Nullable
    public Float getAvgCoreTemperature() {
        return getFieldByNumber(158, Float.class);
    }

    @Nullable
    public Float getMinCoreTemperature() {
        return getFieldByNumber(159, Float.class);
    }

    @Nullable
    public Float getMaxCoreTemperature() {
        return getFieldByNumber(160, Float.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
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

        @Override
        public FitLap build(final int localMessageType) {
            return (FitLap) super.build(localMessageType);
        }
    }
}
