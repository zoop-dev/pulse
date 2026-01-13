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
    public Double getStartPositionLat() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Double getStartPositionLong() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Double getEndPositionLat() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Double getEndPositionLong() {
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
    public Integer getSport() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getEventGroup() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Double getNecLat() {
        return getFieldByNumber(25, Double.class);
    }

    @Nullable
    public Double getNecLong() {
        return getFieldByNumber(26, Double.class);
    }

    @Nullable
    public Double getSwcLat() {
        return getFieldByNumber(27, Double.class);
    }

    @Nullable
    public Double getSwcLong() {
        return getFieldByNumber(28, Double.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(29, String.class);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return getFieldByNumber(30, Integer.class);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return getFieldByNumber(31, Integer.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(32, Integer.class);
    }

    @Nullable
    public Long getTotalWork() {
        return getFieldByNumber(33, Long.class);
    }

    @Nullable
    public Float getAvgAltitude() {
        return getFieldByNumber(34, Float.class);
    }

    @Nullable
    public Float getMaxAltitude() {
        return getFieldByNumber(35, Float.class);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return getFieldByNumber(36, Integer.class);
    }

    @Nullable
    public Float getAvgGrade() {
        return getFieldByNumber(37, Float.class);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return getFieldByNumber(38, Float.class);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return getFieldByNumber(39, Float.class);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return getFieldByNumber(40, Float.class);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return getFieldByNumber(41, Float.class);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return getFieldByNumber(42, Integer.class);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return getFieldByNumber(43, Integer.class);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return getFieldByNumber(44, Double.class);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return getFieldByNumber(45, Float.class);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return getFieldByNumber(46, Float.class);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return getFieldByNumber(47, Float.class);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return getFieldByNumber(48, Float.class);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        return getArrayFieldByNumber(49, Number.class);
    }

    @Nullable
    public Number[] getTimeInSpeedZone() {
        return getArrayFieldByNumber(50, Number.class);
    }

    @Nullable
    public Number[] getTimeInCadenceZone() {
        return getArrayFieldByNumber(51, Number.class);
    }

    @Nullable
    public Number[] getTimeInPowerZone() {
        return getArrayFieldByNumber(52, Number.class);
    }

    @Nullable
    public Integer getRepetitionNum() {
        return getFieldByNumber(53, Integer.class);
    }

    @Nullable
    public Float getMinAltitude() {
        return getFieldByNumber(54, Float.class);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return getFieldByNumber(55, Integer.class);
    }

    @Nullable
    public Double getActiveTime() {
        return getFieldByNumber(56, Double.class);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return getFieldByNumber(57, Integer.class);
    }

    @Nullable
    public Integer getSportEvent() {
        return getFieldByNumber(58, Integer.class);
    }

    @Nullable
    public Float getAvgLeftTorqueEffectiveness() {
        return getFieldByNumber(59, Float.class);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return getFieldByNumber(60, Float.class);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return getFieldByNumber(61, Float.class);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return getFieldByNumber(62, Float.class);
    }

    @Nullable
    public Float getAvgCombinedPedalSmoothness() {
        return getFieldByNumber(63, Float.class);
    }

    @Nullable
    public Integer getStatus() {
        return getFieldByNumber(64, Integer.class);
    }

    @Nullable
    public String getUuid() {
        return getFieldByNumber(65, String.class);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return getFieldByNumber(66, Float.class);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return getFieldByNumber(67, Float.class);
    }

    @Nullable
    public Float getTotalFractionalCycles() {
        return getFieldByNumber(68, Float.class);
    }

    @Nullable
    public Integer getFrontGearShiftCount() {
        return getFieldByNumber(69, Integer.class);
    }

    @Nullable
    public Integer getRearGearShiftCount() {
        return getFieldByNumber(70, Integer.class);
    }

    @Nullable
    public Double getTimeStanding() {
        return getFieldByNumber(71, Double.class);
    }

    @Nullable
    public Integer getStandCount() {
        return getFieldByNumber(72, Integer.class);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return getFieldByNumber(73, Integer.class);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return getFieldByNumber(74, Integer.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        return getArrayFieldByNumber(75, Number.class);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhasePeak() {
        return getArrayFieldByNumber(76, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhase() {
        return getArrayFieldByNumber(77, Number.class);
    }

    @Nullable
    public Number[] getAvgRightPowerPhasePeak() {
        return getArrayFieldByNumber(78, Number.class);
    }

    @Nullable
    public Number[] getAvgPowerPosition() {
        return getArrayFieldByNumber(79, Number.class);
    }

    @Nullable
    public Number[] getMaxPowerPosition() {
        return getArrayFieldByNumber(80, Number.class);
    }

    @Nullable
    public Number[] getAvgCadencePosition() {
        return getArrayFieldByNumber(81, Number.class);
    }

    @Nullable
    public Number[] getMaxCadencePosition() {
        return getArrayFieldByNumber(82, Number.class);
    }

    @Nullable
    public Integer getManufacturer() {
        return getFieldByNumber(83, Integer.class);
    }

    @Nullable
    public Float getTotalGrit() {
        return getFieldByNumber(84, Float.class);
    }

    @Nullable
    public Float getTotalFlow() {
        return getFieldByNumber(85, Float.class);
    }

    @Nullable
    public Float getAvgGrit() {
        return getFieldByNumber(86, Float.class);
    }

    @Nullable
    public Float getAvgFlow() {
        return getFieldByNumber(87, Float.class);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return getFieldByNumber(89, Float.class);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return getFieldByNumber(90, Float.class);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return getFieldByNumber(91, Double.class);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return getFieldByNumber(92, Double.class);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return getFieldByNumber(93, Double.class);
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

        @Override
        public FitSegmentLap build(final int localMessageType) {
            return (FitSegmentLap) super.build(localMessageType);
        }
    }
}
