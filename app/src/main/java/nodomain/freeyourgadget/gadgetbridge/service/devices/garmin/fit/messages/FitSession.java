package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
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
    public Double getStartLatitude() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartLongitude() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Long getTotalElapsedTime() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Long getTotalTimerTime() {
        return (Long) getFieldByNumber(8);
    }

    @Nullable
    public Long getTotalDistance() {
        return (Long) getFieldByNumber(9);
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
    public Integer getAverageHeartRate() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getAvgCadence() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Integer getMaxCadence() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Integer getAvgPower() {
        return (Integer) getFieldByNumber(20);
    }

    @Nullable
    public Integer getMaxPower() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Float getTotalTrainingEffect() {
        return (Float) getFieldByNumber(24);
    }

    @Nullable
    public Integer getFirstLapIndex() {
        return (Integer) getFieldByNumber(25);
    }

    @Nullable
    public Integer getNumLaps() {
        return (Integer) getFieldByNumber(26);
    }

    @Nullable
    public Double getNecLatitude() {
        return (Double) getFieldByNumber(29);
    }

    @Nullable
    public Double getNecLongitude() {
        return (Double) getFieldByNumber(30);
    }

    @Nullable
    public Double getSwcLatitude() {
        return (Double) getFieldByNumber(31);
    }

    @Nullable
    public Double getSwcLongitude() {
        return (Double) getFieldByNumber(32);
    }

    @Nullable
    public Integer getNumLengths() {
        return (Integer) getFieldByNumber(33);
    }

    @Nullable
    public Integer getNormalizedPower() {
        return (Integer) getFieldByNumber(34);
    }

    @Nullable
    public Float getTrainingStressScore() {
        return (Float) getFieldByNumber(35);
    }

    @Nullable
    public Float getIntensityFactor() {
        return (Float) getFieldByNumber(36);
    }

    @Nullable
    public Integer getLeftRightBalance() {
        return (Integer) getFieldByNumber(37);
    }

    @Nullable
    public Double getEndLatitude() {
        return (Double) getFieldByNumber(38);
    }

    @Nullable
    public Double getEndLongitude() {
        return (Double) getFieldByNumber(39);
    }

    @Nullable
    public Float getPoolLength() {
        return (Float) getFieldByNumber(44);
    }

    @Nullable
    public Integer getThresholdPower() {
        return (Integer) getFieldByNumber(45);
    }

    @Nullable
    public Integer getNumActiveLengths() {
        return (Integer) getFieldByNumber(47);
    }

    @Nullable
    public Long getTotalWork() {
        return (Long) getFieldByNumber(48);
    }

    @Nullable
    public Float getAvgSwimCadence() {
        return (Float) getFieldByNumber(79);
    }

    @Nullable
    public Integer getAvgSwolf() {
        return (Integer) getFieldByNumber(80);
    }

    @Nullable
    public Float getAvgVerticalOscillation() {
        return (Float) getFieldByNumber(89);
    }

    @Nullable
    public Float getAvgStanceTimePercent() {
        return (Float) getFieldByNumber(90);
    }

    @Nullable
    public Float getAvgStanceTime() {
        return (Float) getFieldByNumber(91);
    }

    @Nullable
    public Float getAvgFractionalCadence() {
        return (Float) getFieldByNumber(92);
    }

    @Nullable
    public Float getMaxFractionalCadence() {
        return (Float) getFieldByNumber(93);
    }

    @Nullable
    public Float getAvgLeftTorqueEffectiveness() {
        return (Float) getFieldByNumber(101);
    }

    @Nullable
    public Float getAvgRightTorqueEffectiveness() {
        return (Float) getFieldByNumber(102);
    }

    @Nullable
    public Float getAvgLeftPedalSmoothness() {
        return (Float) getFieldByNumber(103);
    }

    @Nullable
    public Float getAvgRightPedalSmoothness() {
        return (Float) getFieldByNumber(104);
    }

    @Nullable
    public Integer getFrontShifts() {
        return (Integer) getFieldByNumber(107);
    }

    @Nullable
    public Integer getRearShifts() {
        return (Integer) getFieldByNumber(108);
    }

    @Nullable
    public String getSportProfileName() {
        return (String) getFieldByNumber(110);
    }

    @Nullable
    public Long getStandTime() {
        return (Long) getFieldByNumber(112);
    }

    @Nullable
    public Integer getStandCount() {
        return (Integer) getFieldByNumber(113);
    }

    @Nullable
    public Integer getAvgLeftPco() {
        return (Integer) getFieldByNumber(114);
    }

    @Nullable
    public Integer getAvgRightPco() {
        return (Integer) getFieldByNumber(115);
    }

    @Nullable
    public Number[] getAvgLeftPowerPhase() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(116);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAvgLeftPowerPhasePeak() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(117);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAvgRightPowerPhase() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(118);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAvgRightPowerPhasePeak() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(119);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAvgPowerPosition() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(120);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getMaxPowerPosition() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(121);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAvgCadencePosition() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(122);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getMaxCadencePosition() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(123);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Double getEnhancedAvgSpeed() {
        return (Double) getFieldByNumber(124);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return (Double) getFieldByNumber(125);
    }

    @Nullable
    public Float getAvgVerticalRatio() {
        return (Float) getFieldByNumber(132);
    }

    @Nullable
    public Float getAvgStanceTimeBalance() {
        return (Float) getFieldByNumber(133);
    }

    @Nullable
    public Float getAvgStepLength() {
        return (Float) getFieldByNumber(134);
    }

    @Nullable
    public Float getTotalAnaerobicTrainingEffect() {
        return (Float) getFieldByNumber(137);
    }

    @Nullable
    public Double getTrainingLoadPeak() {
        return (Double) getFieldByNumber(168);
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return (Float) getFieldByNumber(169);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return (Float) getFieldByNumber(170);
    }

    @Nullable
    public Integer getEstimatedSweatLoss() {
        return (Integer) getFieldByNumber(178);
    }

    @Nullable
    public Float getEnhancedMinRespirationRate() {
        return (Float) getFieldByNumber(180);
    }

    @Nullable
    public Integer getPrimaryBenefit() {
        return (Integer) getFieldByNumber(188);
    }

    @Nullable
    public Integer getAvgSpo2() {
        return (Integer) getFieldByNumber(194);
    }

    @Nullable
    public Integer getAvgStress() {
        return (Integer) getFieldByNumber(195);
    }

    @Nullable
    public Integer getRestingCalories() {
        return (Integer) getFieldByNumber(196);
    }

    @Nullable
    public Integer getHrvSdrr() {
        return (Integer) getFieldByNumber(197);
    }

    @Nullable
    public Integer getHrvRmssd() {
        return (Integer) getFieldByNumber(198);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

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

        public Builder setPoolLength(final Float value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setThresholdPower(final Integer value) {
            setFieldByNumber(45, value);
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

        public Builder setAvgSwimCadence(final Float value) {
            setFieldByNumber(79, value);
            return this;
        }

        public Builder setAvgSwolf(final Integer value) {
            setFieldByNumber(80, value);
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
            setFieldByNumber(116, value);
            return this;
        }

        public Builder setAvgLeftPowerPhasePeak(final Number[] value) {
            setFieldByNumber(117, value);
            return this;
        }

        public Builder setAvgRightPowerPhase(final Number[] value) {
            setFieldByNumber(118, value);
            return this;
        }

        public Builder setAvgRightPowerPhasePeak(final Number[] value) {
            setFieldByNumber(119, value);
            return this;
        }

        public Builder setAvgPowerPosition(final Number[] value) {
            setFieldByNumber(120, value);
            return this;
        }

        public Builder setMaxPowerPosition(final Number[] value) {
            setFieldByNumber(121, value);
            return this;
        }

        public Builder setAvgCadencePosition(final Number[] value) {
            setFieldByNumber(122, value);
            return this;
        }

        public Builder setMaxCadencePosition(final Number[] value) {
            setFieldByNumber(123, value);
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

        public Builder setEstimatedSweatLoss(final Integer value) {
            setFieldByNumber(178, value);
            return this;
        }

        public Builder setEnhancedMinRespirationRate(final Float value) {
            setFieldByNumber(180, value);
            return this;
        }

        public Builder setPrimaryBenefit(final Integer value) {
            setFieldByNumber(188, value);
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

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitSession build() {
            return (FitSession) super.build();
        }
    }
}
