package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

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
}
