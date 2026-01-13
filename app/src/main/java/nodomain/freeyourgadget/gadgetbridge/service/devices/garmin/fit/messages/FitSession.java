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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
public class FitSession extends RecordData {
    public FitSession(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 18) {
            throw new IllegalArgumentException("FitSession expects global messages of " + 18 + ", got " + globalNumber);
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
    public Double getStartLatitude() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Double getStartLongitude() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Long getTotalElapsedTime() {
        return getFieldByNumber(7, Long.class);
    }

    @Nullable
    public Long getTotalTimerTime() {
        return getFieldByNumber(8, Long.class);
    }

    @Nullable
    public Long getTotalDistance() {
        return getFieldByNumber(9, Long.class);
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
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Float getAvgSpeed() {
        return getFieldByNumber(14, Float.class);
    }

    @Nullable
    public Float getMaxSpeed() {
        return getFieldByNumber(15, Float.class);
    }

    @Nullable
    public Integer getAverageHeartRate() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getAvgCadence() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getMaxCadence() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Integer getAvgPower() {
        return getFieldByNumber(20, Integer.class);
    }

    @Nullable
    public Integer getMaxPower() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getTotalAscent() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Integer getTotalDescent() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Float getTotalTrainingEffect() {
        return getFieldByNumber(24, Float.class);
    }

    @Nullable
    public Integer getFirstLapIndex() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getNumLaps() {
        return getFieldByNumber(26, Integer.class);
    }

    @Nullable
    public Integer getEventGroup() {
        return getFieldByNumber(27, Integer.class);
    }

    @Nullable
    public Integer getTrigger() {
        return getFieldByNumber(28, Integer.class);
    }

    @Nullable
    public Double getNecLatitude() {
        return getFieldByNumber(29, Double.class);
    }

    @Nullable
    public Double getNecLongitude() {
        return getFieldByNumber(30, Double.class);
    }

    @Nullable
    public Double getSwcLatitude() {
        return getFieldByNumber(31, Double.class);
    }

    @Nullable
    public Double getSwcLongitude() {
        return getFieldByNumber(32, Double.class);
    }

    @Nullable
    public Integer getNumLengths() {
        return getFieldByNumber(33, Integer.class);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return getFieldByNumber(34, Integer.class);
    }

    @Nullable
    public Float getTrainingStressScore() {
        return getFieldByNumber(35, Float.class);
    }

    @Nullable
    public Float getIntensityFactor() {
        return getFieldByNumber(36, Float.class);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return getFieldByNumber(37, Integer.class);
    }

    @Nullable
    public Double getEndLatitude() {
        return getFieldByNumber(38, Double.class);
    }

    @Nullable
    public Double getEndLongitude() {
        return getFieldByNumber(39, Double.class);
    }

    @Nullable
    public Long getAvgStrokeCount() {
        return getFieldByNumber(41, Long.class);
    }

    @Nullable
    public Float getAvgStrokeDistance() {
        return getFieldByNumber(42, Float.class);
    }

    @Nullable
    public Integer getSwimStroke() {
        return getFieldByNumber(43, Integer.class);
    }

    @Nullable
    public Float getPoolLength() {
        return getFieldByNumber(44, Float.class);
    }

    @Nullable
    public Integer getThresholdPower() {
        return getFieldByNumber(45, Integer.class);
    }

    @Nullable
    public Integer getPoolLengthUnit() {
        return getFieldByNumber(46, Integer.class);
    }

    @Nullable
    public Integer getNumActiveLengths() {
        return getFieldByNumber(47, Integer.class);
    }

    @Nullable
    public Long getTotalWork() {
        return getFieldByNumber(48, Long.class);
    }

    @Nullable
    public Float getAvgAltitude() {
        return getFieldByNumber(49, Float.class);
    }

    @Nullable
    public Float getMaxAltitude() {
        return getFieldByNumber(50, Float.class);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return getFieldByNumber(51, Integer.class);
    }

    @Nullable
    public Float getAvgGrade() {
        return getFieldByNumber(52, Float.class);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return getFieldByNumber(53, Float.class);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return getFieldByNumber(54, Float.class);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return getFieldByNumber(55, Float.class);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return getFieldByNumber(56, Float.class);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return getFieldByNumber(57, Integer.class);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return getFieldByNumber(58, Integer.class);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return getFieldByNumber(59, Double.class);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return getFieldByNumber(60, Float.class);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return getFieldByNumber(61, Float.class);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return getFieldByNumber(62, Float.class);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return getFieldByNumber(63, Float.class);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return getFieldByNumber(64, Integer.class);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        return getArrayFieldByNumber(65, Number.class);
    }

    @Nullable
    public Number[] getTimeInSpeedZone() {
        return getArrayFieldByNumber(66, Number.class);
    }

    @Nullable
    public Number[] getTimeInCadenceZone() {
        return getArrayFieldByNumber(67, Number.class);
    }

    @Nullable
    public Number[] getTimeInPowerZone() {
        return getArrayFieldByNumber(68, Number.class);
    }

    @Nullable
    public Double getAvgLapTime() {
        return getFieldByNumber(69, Double.class);
    }

    @Nullable
    public Integer getBestLapIndex() {
        return getFieldByNumber(70, Integer.class);
    }

    @Nullable
    public Float getMinAltitude() {
        return getFieldByNumber(71, Float.class);
    }

    @Nullable
    public Float getAvgSwimCadence() {
        return getFieldByNumber(79, Float.class);
    }

    @Nullable
    public Integer getAvgSwolf() {
        return getFieldByNumber(80, Integer.class);
    }

    @Nullable
    public Integer getPlayerScore() {
        return getFieldByNumber(82, Integer.class);
    }

    @Nullable
    public Integer getOpponentScore() {
        return getFieldByNumber(83, Integer.class);
    }

    @Nullable
    public String getOpponentName() {
        return getFieldByNumber(84, String.class);
    }

    @Nullable
    public Number[] getStrokeCount() {
        return getArrayFieldByNumber(85, Number.class);
    }

    @Nullable
    public Number[] getZoneCount() {
        return getArrayFieldByNumber(86, Number.class);
    }

    @Nullable
    public Float getMaxBallSpeed() {
        return getFieldByNumber(87, Float.class);
    }

    @Nullable
    public Float getAvgBallSpeed() {
        return getFieldByNumber(88, Float.class);
    }

    @Nullable
    public Float getAvgVerticalOscillation() {
        return getFieldByNumber(89, Float.class);
    }

    @Nullable
    public Float getAvgStanceTimePercent() {
        return getFieldByNumber(90, Float.class);
    }

    @Nullable
    public Float getAvgStanceTime() {
        return getFieldByNumber(91, Float.class);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return getFieldByNumber(92, Float.class);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return getFieldByNumber(93, Float.class);
    }

    @Nullable
    public Float getTotalFractionalCycles() {
        return getFieldByNumber(94, Float.class);
    }

    @Nullable
    public Number[] getAvgTotalHemoglobinConc() {
        return getArrayFieldByNumber(95, Number.class);
    }

    @Nullable
    public Number[] getMinTotalHemoglobinConc() {
        return getArrayFieldByNumber(96, Number.class);
    }

    @Nullable
    public Number[] getMaxTotalHemoglobinConc() {
        return getArrayFieldByNumber(97, Number.class);
    }

    @Nullable
    public Number[] getAvgSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(98, Number.class);
    }

    @Nullable
    public Number[] getMinSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(99, Number.class);
    }

    @Nullable
    public Number[] getMaxSaturatedHemoglobinPercent() {
        return getArrayFieldByNumber(100, Number.class);
    }

    @Nullable
    public Float getAvgLeftTorqueEffectiveness() {
        return getFieldByNumber(101, Float.class);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return getFieldByNumber(102, Float.class);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return getFieldByNumber(103, Float.class);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return getFieldByNumber(104, Float.class);
    }

    @Nullable
    public Float getAvgCombinedPedalSmoothness() {
        return getFieldByNumber(105, Float.class);
    }

    @Nullable
    public Integer getFrontShifts() {
        return getFieldByNumber(107, Integer.class);
    }

    @Nullable
    public Integer getRearShifts() {
        return getFieldByNumber(108, Integer.class);
    }

    @Nullable
    public String getSportProfileName() {
        return getFieldByNumber(110, String.class);
    }

    @Nullable
    public Integer getSportIndex() {
        return getFieldByNumber(111, Integer.class);
    }

    @Nullable
    public Long getStandTime() {
        return getFieldByNumber(112, Long.class);
    }

    @Nullable
    public Integer getStandCount() {
        return getFieldByNumber(113, Integer.class);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return getFieldByNumber(114, Integer.class);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return getFieldByNumber(115, Integer.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        return getArrayFieldByNumber(116, Number.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhasePeak() {
        return getArrayFieldByNumber(117, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhase() {
        return getArrayFieldByNumber(118, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhasePeak() {
        return getArrayFieldByNumber(119, Number.class);
    }

    @Nullable
    public Number[] getAvgPowerPosition() {
        return getArrayFieldByNumber(120, Number.class);
    }

    @Nullable
    public Number[] getMaxPowerPosition() {
        return getArrayFieldByNumber(121, Number.class);
    }

    @Nullable
    public Number[] getAvgCadencePosition() {
        return getArrayFieldByNumber(122, Number.class);
    }

    @Nullable
    public Number[] getMaxCadencePosition() {
        return getArrayFieldByNumber(123, Number.class);
    }

    @Nullable
    public Double getEnhancedAvgSpeed() {
        return getFieldByNumber(124, Double.class);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return getFieldByNumber(125, Double.class);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return getFieldByNumber(126, Double.class);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return getFieldByNumber(127, Double.class);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return getFieldByNumber(128, Double.class);
    }

    @Nullable
    public Integer getAvgLevMotorPower() {
        return getFieldByNumber(129, Integer.class);
    }

    @Nullable
    public Integer getMaxLevMotorPower() {
        return getFieldByNumber(130, Integer.class);
    }

    @Nullable
    public Float getLevBatteryConsumption() {
        return getFieldByNumber(131, Float.class);
    }

    @Nullable
    public Float getAvgVerticalRatio() {
        return getFieldByNumber(132, Float.class);
    }

    @Nullable
    public Float getAvgStanceTimeBalance() {
        return getFieldByNumber(133, Float.class);
    }

    @Nullable
    public Float getAvgStepLength() {
        return getFieldByNumber(134, Float.class);
    }

    @Nullable
    public Float getTotalAnaerobicTrainingEffect() {
        return getFieldByNumber(137, Float.class);
    }

    @Nullable
    public Float getAvgVam() {
        return getFieldByNumber(139, Float.class);
    }

    @Nullable
    public Double getAvgDepth() {
        return getFieldByNumber(140, Double.class);
    }

    @Nullable
    public Double getMaxDepth() {
        return getFieldByNumber(141, Double.class);
    }

    @Nullable
    public Long getSurfaceInterval() {
        return getFieldByNumber(142, Long.class);
    }

    @Nullable
    public Integer getStartCns() {
        return getFieldByNumber(143, Integer.class);
    }

    @Nullable
    public Integer getEndCns() {
        return getFieldByNumber(144, Integer.class);
    }

    @Nullable
    public Integer getStartN2() {
        return getFieldByNumber(145, Integer.class);
    }

    @Nullable
    public Integer getEndN2() {
        return getFieldByNumber(146, Integer.class);
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
    public Integer getMinRespirationRate() {
        return getFieldByNumber(149, Integer.class);
    }

    @Nullable
    public Integer getMinTemperature() {
        return getFieldByNumber(150, Integer.class);
    }

    @Nullable
    public Integer getO2Toxicity() {
        return getFieldByNumber(155, Integer.class);
    }

    @Nullable
    public Long getDiveNumber() {
        return getFieldByNumber(156, Long.class);
    }

    @Nullable
    public Double getTrainingLoadPeak() {
        return getFieldByNumber(168, Double.class);
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return getFieldByNumber(169, Float.class);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return getFieldByNumber(170, Float.class);
    }

    @Nullable
    public Integer getCaloriesConsumed() {
        return getFieldByNumber(177, Integer.class);
    }

    @Nullable
    public Integer getEstimatedSweatLoss() {
        return getFieldByNumber(178, Integer.class);
    }

    @Nullable
    public Integer getFluidConsumed() {
        return getFieldByNumber(179, Integer.class);
    }

    @Nullable
    public Float getEnhancedMinRespirationRate() {
        return getFieldByNumber(180, Float.class);
    }

    @Nullable
    public Float getTotalGrit() {
        return getFieldByNumber(181, Float.class);
    }

    @Nullable
    public Float getTotalFlow() {
        return getFieldByNumber(182, Float.class);
    }

    @Nullable
    public Integer getJumpCount() {
        return getFieldByNumber(183, Integer.class);
    }

    @Nullable
    public Float getAvgGrit() {
        return getFieldByNumber(186, Float.class);
    }

    @Nullable
    public Float getAvgFlow() {
        return getFieldByNumber(187, Float.class);
    }

    @Nullable
    public Integer getPrimaryBenefit() {
        return getFieldByNumber(188, Integer.class);
    }

    @Nullable
    public Integer getWorkoutFeel() {
        return getFieldByNumber(192, Integer.class);
    }

    @Nullable
    public Integer getWorkoutRpe() {
        return getFieldByNumber(193, Integer.class);
    }

    @Nullable
    public Integer getAvgSpo2() {
        return getFieldByNumber(194, Integer.class);
    }

    @Nullable
    public Integer getAvgStress() {
        return getFieldByNumber(195, Integer.class);
    }

    @Nullable
    public Integer getRestingCalories() {
        return getFieldByNumber(196, Integer.class);
    }

    @Nullable
    public Integer getHrvSdrr() {
        return getFieldByNumber(197, Integer.class);
    }

    @Nullable
    public Integer getHrvRmssd() {
        return getFieldByNumber(198, Integer.class);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return getFieldByNumber(199, Float.class);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return getFieldByNumber(200, Float.class);
    }

    @Nullable
    public Long getBatteryGain() {
        return getFieldByNumber(203, Long.class);
    }

    @Nullable
    public Float getSolarIntensity() {
        return getFieldByNumber(204, Float.class);
    }

    @Nullable
    public Integer getBeginningPotential() {
        return getFieldByNumber(205, Integer.class);
    }

    @Nullable
    public Integer getEndingPotential() {
        return getFieldByNumber(206, Integer.class);
    }

    @Nullable
    public Integer getMinStamina() {
        return getFieldByNumber(207, Integer.class);
    }

    @Nullable
    public Float getAvgCoreTemperature() {
        return getFieldByNumber(208, Float.class);
    }

    @Nullable
    public Float getMinCoreTemperature() {
        return getFieldByNumber(209, Float.class);
    }

    @Nullable
    public Float getMaxCoreTemperature() {
        return getFieldByNumber(210, Float.class);
    }

    @Nullable
    public Float getStepSpeedLoss() {
        return getFieldByNumber(222, Float.class);
    }

    @Nullable
    public Float getStepSpeedLossPercentage() {
        return getFieldByNumber(223, Float.class);
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
            super(18);
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

        public Builder setStartLatitude(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setStartLongitude(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTotalElapsedTime(final Long value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTotalTimerTime(final Long value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setTotalDistance(final Long value) {
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
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setAvgSpeed(final Float value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setMaxSpeed(final Float value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setAverageHeartRate(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setAvgCadence(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setMaxCadence(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setAvgPower(final Integer value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setMaxPower(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setTotalTrainingEffect(final Float value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setFirstLapIndex(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setNumLaps(final Integer value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setEventGroup(final Integer value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setTrigger(final Integer value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setNecLatitude(final Double value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setNecLongitude(final Double value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setSwcLatitude(final Double value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setSwcLongitude(final Double value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setNumLengths(final Integer value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setNormalizedPower(final Integer value) {
            setFieldByNumber(34, value);
            return this;
        }

        public Builder setTrainingStressScore(final Float value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setIntensityFactor(final Float value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setLeftRightBalance(final Integer value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setEndLatitude(final Double value) {
            setFieldByNumber(38, value);
            return this;
        }

        public Builder setEndLongitude(final Double value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setAvgStrokeCount(final Long value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setAvgStrokeDistance(final Float value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setSwimStroke(final Integer value) {
            setFieldByNumber(43, value);
            return this;
        }

        public Builder setPoolLength(final Float value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setThresholdPower(final Integer value) {
            setFieldByNumber(45, value);
            return this;
        }

        public Builder setPoolLengthUnit(final Integer value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setNumActiveLengths(final Integer value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setTotalWork(final Long value) {
            setFieldByNumber(48, value);
            return this;
        }

        public Builder setAvgAltitude(final Float value) {
            setFieldByNumber(49, value);
            return this;
        }

        public Builder setMaxAltitude(final Float value) {
            setFieldByNumber(50, value);
            return this;
        }

        public Builder setGpsAccuracy(final Integer value) {
            setFieldByNumber(51, value);
            return this;
        }

        public Builder setAvgGrade(final Float value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setAvgPosGrade(final Float value) {
            setFieldByNumber(53, value);
            return this;
        }

        public Builder setAvgNegGrade(final Float value) {
            setFieldByNumber(54, value);
            return this;
        }

        public Builder setMaxPosGrade(final Float value) {
            setFieldByNumber(55, value);
            return this;
        }

        public Builder setMaxNegGrade(final Float value) {
            setFieldByNumber(56, value);
            return this;
        }

        public Builder setAvgTemperature(final Integer value) {
            setFieldByNumber(57, value);
            return this;
        }

        public Builder setMaxTemperature(final Integer value) {
            setFieldByNumber(58, value);
            return this;
        }

        public Builder setTotalMovingTime(final Double value) {
            setFieldByNumber(59, value);
            return this;
        }

        public Builder setAvgPosVerticalSpeed(final Float value) {
            setFieldByNumber(60, value);
            return this;
        }

        public Builder setAvgNegVerticalSpeed(final Float value) {
            setFieldByNumber(61, value);
            return this;
        }

        public Builder setMaxPosVerticalSpeed(final Float value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setMaxNegVerticalSpeed(final Float value) {
            setFieldByNumber(63, value);
            return this;
        }

        public Builder setMinHeartRate(final Integer value) {
            setFieldByNumber(64, value);
            return this;
        }

        public Builder setTimeInHrZone(final Number[] value) {
            setFieldByNumber(65, (Object[]) value);
            return this;
        }

        public Builder setTimeInSpeedZone(final Number[] value) {
            setFieldByNumber(66, (Object[]) value);
            return this;
        }

        public Builder setTimeInCadenceZone(final Number[] value) {
            setFieldByNumber(67, (Object[]) value);
            return this;
        }

        public Builder setTimeInPowerZone(final Number[] value) {
            setFieldByNumber(68, (Object[]) value);
            return this;
        }

        public Builder setAvgLapTime(final Double value) {
            setFieldByNumber(69, value);
            return this;
        }

        public Builder setBestLapIndex(final Integer value) {
            setFieldByNumber(70, value);
            return this;
        }

        public Builder setMinAltitude(final Float value) {
            setFieldByNumber(71, value);
            return this;
        }

        public Builder setAvgSwimCadence(final Float value) {
            setFieldByNumber(79, value);
            return this;
        }

        public Builder setAvgSwolf(final Integer value) {
            setFieldByNumber(80, value);
            return this;
        }

        public Builder setPlayerScore(final Integer value) {
            setFieldByNumber(82, value);
            return this;
        }

        public Builder setOpponentScore(final Integer value) {
            setFieldByNumber(83, value);
            return this;
        }

        public Builder setOpponentName(final String value) {
            setFieldByNumber(84, value);
            return this;
        }

        public Builder setStrokeCount(final Number[] value) {
            setFieldByNumber(85, (Object[]) value);
            return this;
        }

        public Builder setZoneCount(final Number[] value) {
            setFieldByNumber(86, (Object[]) value);
            return this;
        }

        public Builder setMaxBallSpeed(final Float value) {
            setFieldByNumber(87, value);
            return this;
        }

        public Builder setAvgBallSpeed(final Float value) {
            setFieldByNumber(88, value);
            return this;
        }

        public Builder setAvgVerticalOscillation(final Float value) {
            setFieldByNumber(89, value);
            return this;
        }

        public Builder setAvgStanceTimePercent(final Float value) {
            setFieldByNumber(90, value);
            return this;
        }

        public Builder setAvgStanceTime(final Float value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setAvgFractionalCadence(final Float value) {
            setFieldByNumber(92, value);
            return this;
        }

        public Builder setMaxFractionalCadence(final Float value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setTotalFractionalCycles(final Float value) {
            setFieldByNumber(94, value);
            return this;
        }

        public Builder setAvgTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(95, (Object[]) value);
            return this;
        }

        public Builder setMinTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(96, (Object[]) value);
            return this;
        }

        public Builder setMaxTotalHemoglobinConc(final Number[] value) {
            setFieldByNumber(97, (Object[]) value);
            return this;
        }

        public Builder setAvgSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(98, (Object[]) value);
            return this;
        }

        public Builder setMinSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(99, (Object[]) value);
            return this;
        }

        public Builder setMaxSaturatedHemoglobinPercent(final Number[] value) {
            setFieldByNumber(100, (Object[]) value);
            return this;
        }

        public Builder setAvgLeftTorqueEffectiveness(final Float value) {
            setFieldByNumber(101, value);
            return this;
        }

        public Builder setAvgRightTorqueEffectiveness(final Float value) {
            setFieldByNumber(102, value);
            return this;
        }

        public Builder setAvgLeftPedalSmoothness(final Float value) {
            setFieldByNumber(103, value);
            return this;
        }

        public Builder setAvgRightPedalSmoothness(final Float value) {
            setFieldByNumber(104, value);
            return this;
        }

        public Builder setAvgCombinedPedalSmoothness(final Float value) {
            setFieldByNumber(105, value);
            return this;
        }

        public Builder setFrontShifts(final Integer value) {
            setFieldByNumber(107, value);
            return this;
        }

        public Builder setRearShifts(final Integer value) {
            setFieldByNumber(108, value);
            return this;
        }

        public Builder setSportProfileName(final String value) {
            setFieldByNumber(110, value);
            return this;
        }

        public Builder setSportIndex(final Integer value) {
            setFieldByNumber(111, value);
            return this;
        }

        public Builder setStandTime(final Long value) {
            setFieldByNumber(112, value);
            return this;
        }

        public Builder setStandCount(final Integer value) {
            setFieldByNumber(113, value);
            return this;
        }

        public Builder setAvgLeftPco(final Integer value) {
            setFieldByNumber(114, value);
            return this;
        }

        public Builder setAvgRightPco(final Integer value) {
            setFieldByNumber(115, value);
            return this;
        }

        public Builder setAvgLeftPowerPhase(final Number[] value) {
            setFieldByNumber(116, (Object[]) value);
            return this;
        }

        public Builder setAvgLeftPowerPhasePeak(final Number[] value) {
            setFieldByNumber(117, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhase(final Number[] value) {
            setFieldByNumber(118, (Object[]) value);
            return this;
        }

        public Builder setAvgRightPowerPhasePeak(final Number[] value) {
            setFieldByNumber(119, (Object[]) value);
            return this;
        }

        public Builder setAvgPowerPosition(final Number[] value) {
            setFieldByNumber(120, (Object[]) value);
            return this;
        }

        public Builder setMaxPowerPosition(final Number[] value) {
            setFieldByNumber(121, (Object[]) value);
            return this;
        }

        public Builder setAvgCadencePosition(final Number[] value) {
            setFieldByNumber(122, (Object[]) value);
            return this;
        }

        public Builder setMaxCadencePosition(final Number[] value) {
            setFieldByNumber(123, (Object[]) value);
            return this;
        }

        public Builder setEnhancedAvgSpeed(final Double value) {
            setFieldByNumber(124, value);
            return this;
        }

        public Builder setEnhancedMaxSpeed(final Double value) {
            setFieldByNumber(125, value);
            return this;
        }

        public Builder setEnhancedAvgAltitude(final Double value) {
            setFieldByNumber(126, value);
            return this;
        }

        public Builder setEnhancedMinAltitude(final Double value) {
            setFieldByNumber(127, value);
            return this;
        }

        public Builder setEnhancedMaxAltitude(final Double value) {
            setFieldByNumber(128, value);
            return this;
        }

        public Builder setAvgLevMotorPower(final Integer value) {
            setFieldByNumber(129, value);
            return this;
        }

        public Builder setMaxLevMotorPower(final Integer value) {
            setFieldByNumber(130, value);
            return this;
        }

        public Builder setLevBatteryConsumption(final Float value) {
            setFieldByNumber(131, value);
            return this;
        }

        public Builder setAvgVerticalRatio(final Float value) {
            setFieldByNumber(132, value);
            return this;
        }

        public Builder setAvgStanceTimeBalance(final Float value) {
            setFieldByNumber(133, value);
            return this;
        }

        public Builder setAvgStepLength(final Float value) {
            setFieldByNumber(134, value);
            return this;
        }

        public Builder setTotalAnaerobicTrainingEffect(final Float value) {
            setFieldByNumber(137, value);
            return this;
        }

        public Builder setAvgVam(final Float value) {
            setFieldByNumber(139, value);
            return this;
        }

        public Builder setAvgDepth(final Double value) {
            setFieldByNumber(140, value);
            return this;
        }

        public Builder setMaxDepth(final Double value) {
            setFieldByNumber(141, value);
            return this;
        }

        public Builder setSurfaceInterval(final Long value) {
            setFieldByNumber(142, value);
            return this;
        }

        public Builder setStartCns(final Integer value) {
            setFieldByNumber(143, value);
            return this;
        }

        public Builder setEndCns(final Integer value) {
            setFieldByNumber(144, value);
            return this;
        }

        public Builder setStartN2(final Integer value) {
            setFieldByNumber(145, value);
            return this;
        }

        public Builder setEndN2(final Integer value) {
            setFieldByNumber(146, value);
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

        public Builder setMinRespirationRate(final Integer value) {
            setFieldByNumber(149, value);
            return this;
        }

        public Builder setMinTemperature(final Integer value) {
            setFieldByNumber(150, value);
            return this;
        }

        public Builder setO2Toxicity(final Integer value) {
            setFieldByNumber(155, value);
            return this;
        }

        public Builder setDiveNumber(final Long value) {
            setFieldByNumber(156, value);
            return this;
        }

        public Builder setTrainingLoadPeak(final Double value) {
            setFieldByNumber(168, value);
            return this;
        }

        public Builder setEnhancedAvgRespirationRate(final Float value) {
            setFieldByNumber(169, value);
            return this;
        }

        public Builder setEnhancedMaxRespirationRate(final Float value) {
            setFieldByNumber(170, value);
            return this;
        }

        public Builder setCaloriesConsumed(final Integer value) {
            setFieldByNumber(177, value);
            return this;
        }

        public Builder setEstimatedSweatLoss(final Integer value) {
            setFieldByNumber(178, value);
            return this;
        }

        public Builder setFluidConsumed(final Integer value) {
            setFieldByNumber(179, value);
            return this;
        }

        public Builder setEnhancedMinRespirationRate(final Float value) {
            setFieldByNumber(180, value);
            return this;
        }

        public Builder setTotalGrit(final Float value) {
            setFieldByNumber(181, value);
            return this;
        }

        public Builder setTotalFlow(final Float value) {
            setFieldByNumber(182, value);
            return this;
        }

        public Builder setJumpCount(final Integer value) {
            setFieldByNumber(183, value);
            return this;
        }

        public Builder setAvgGrit(final Float value) {
            setFieldByNumber(186, value);
            return this;
        }

        public Builder setAvgFlow(final Float value) {
            setFieldByNumber(187, value);
            return this;
        }

        public Builder setPrimaryBenefit(final Integer value) {
            setFieldByNumber(188, value);
            return this;
        }

        public Builder setWorkoutFeel(final Integer value) {
            setFieldByNumber(192, value);
            return this;
        }

        public Builder setWorkoutRpe(final Integer value) {
            setFieldByNumber(193, value);
            return this;
        }

        public Builder setAvgSpo2(final Integer value) {
            setFieldByNumber(194, value);
            return this;
        }

        public Builder setAvgStress(final Integer value) {
            setFieldByNumber(195, value);
            return this;
        }

        public Builder setRestingCalories(final Integer value) {
            setFieldByNumber(196, value);
            return this;
        }

        public Builder setHrvSdrr(final Integer value) {
            setFieldByNumber(197, value);
            return this;
        }

        public Builder setHrvRmssd(final Integer value) {
            setFieldByNumber(198, value);
            return this;
        }

        public Builder setTotalFractionalAscent(final Float value) {
            setFieldByNumber(199, value);
            return this;
        }

        public Builder setTotalFractionalDescent(final Float value) {
            setFieldByNumber(200, value);
            return this;
        }

        public Builder setBatteryGain(final Long value) {
            setFieldByNumber(203, value);
            return this;
        }

        public Builder setSolarIntensity(final Float value) {
            setFieldByNumber(204, value);
            return this;
        }

        public Builder setBeginningPotential(final Integer value) {
            setFieldByNumber(205, value);
            return this;
        }

        public Builder setEndingPotential(final Integer value) {
            setFieldByNumber(206, value);
            return this;
        }

        public Builder setMinStamina(final Integer value) {
            setFieldByNumber(207, value);
            return this;
        }

        public Builder setAvgCoreTemperature(final Float value) {
            setFieldByNumber(208, value);
            return this;
        }

        public Builder setMinCoreTemperature(final Float value) {
            setFieldByNumber(209, value);
            return this;
        }

        public Builder setMaxCoreTemperature(final Float value) {
            setFieldByNumber(210, value);
            return this;
        }

        public Builder setStepSpeedLoss(final Float value) {
            setFieldByNumber(222, value);
            return this;
        }

        public Builder setStepSpeedLossPercentage(final Float value) {
            setFieldByNumber(223, value);
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
        public FitSession build() {
            return (FitSession) super.build();
        }

        @Override
        public FitSession build(final int localMessageType) {
            return (FitSession) super.build(localMessageType);
        }
    }

    // manual changes below

    public List<ActivityPoint> toActivityPoints() {
        final List<ActivityPoint> activityPoints = new ArrayList<ActivityPoint>();
        final ActivityPoint startActivityPoint = new ActivityPoint();
        startActivityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getStartLatitude() != null && getStartLongitude() != null) {
            startActivityPoint.setLocation(new GPSCoordinate(
                    getStartLongitude(),
                    getStartLatitude(),
                    GPSCoordinate.UNKNOWN_ALTITUDE
            ));
            activityPoints.add(startActivityPoint);
        }
        final ActivityPoint endActivityPoint = new ActivityPoint();
        endActivityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getEndLatitude() != null && getEndLongitude() != null) {
            endActivityPoint.setLocation(new GPSCoordinate(
                    getEndLongitude(),
                    getEndLatitude(),
                    GPSCoordinate.UNKNOWN_ALTITUDE
            ));
            activityPoints.add(endActivityPoint);
        }

        return activityPoints;
    }
}
