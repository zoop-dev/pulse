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
    public Integer getTotalFatCalories() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Float getAvgSpeed() {
        return (Float) getFieldByNumber(14);
    }

    @Nullable
    public Float getMaxSpeed() {
        return (Float) getFieldByNumber(15);
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
    public Integer getEventGroup() {
        return (Integer) getFieldByNumber(27);
    }

    @Nullable
    public Integer getTrigger() {
        return (Integer) getFieldByNumber(28);
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
    public Long getAvgStrokeCount() {
        return (Long) getFieldByNumber(41);
    }

    @Nullable
    public Float getAvgStrokeDistance() {
        return (Float) getFieldByNumber(42);
    }

    @Nullable
    public Integer getSwimStroke() {
        return (Integer) getFieldByNumber(43);
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
    public Integer getPoolLengthUnit() {
        return (Integer) getFieldByNumber(46);
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
    public Float getAvgAltitude() {
        return (Float) getFieldByNumber(49);
    }

    @Nullable
    public Float getMaxAltitude() {
        return (Float) getFieldByNumber(50);
    }

    @Nullable
    public Integer getGpsAccuracy() {
        return (Integer) getFieldByNumber(51);
    }

    @Nullable
    public Float getAvgGrade() {
        return (Float) getFieldByNumber(52);
    }

    @Nullable
    public Float getAvgPosGrade() {
        return (Float) getFieldByNumber(53);
    }

    @Nullable
    public Float getAvgNegGrade() {
        return (Float) getFieldByNumber(54);
    }

    @Nullable
    public Float getMaxPosGrade() {
        return (Float) getFieldByNumber(55);
    }

    @Nullable
    public Float getMaxNegGrade() {
        return (Float) getFieldByNumber(56);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return (Integer) getFieldByNumber(57);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return (Integer) getFieldByNumber(58);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return (Double) getFieldByNumber(59);
    }

    @Nullable
    public Float getAvgPosVerticalSpeed() {
        return (Float) getFieldByNumber(60);
    }

    @Nullable
    public Float getAvgNegVerticalSpeed() {
        return (Float) getFieldByNumber(61);
    }

    @Nullable
    public Float getMaxPosVerticalSpeed() {
        return (Float) getFieldByNumber(62);
    }

    @Nullable
    public Float getMaxNegVerticalSpeed() {
        return (Float) getFieldByNumber(63);
    }

    @Nullable
    public Integer getMinHeartRate() {
        return (Integer) getFieldByNumber(64);
    }

    @Nullable
    public Number[] getTimeInHrZone() {
        final Object object = getFieldByNumber(65);
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
        final Object object = getFieldByNumber(66);
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
        final Object object = getFieldByNumber(67);
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
        final Object object = getFieldByNumber(68);
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
    public Double getAvgLapTime() {
        return (Double) getFieldByNumber(69);
    }

    @Nullable
    public Integer getBestLapIndex() {
        return (Integer) getFieldByNumber(70);
    }

    @Nullable
    public Float getMinAltitude() {
        return (Float) getFieldByNumber(71);
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
    public Integer getPlayerScore() {
        return (Integer) getFieldByNumber(82);
    }

    @Nullable
    public Integer getOpponentScore() {
        return (Integer) getFieldByNumber(83);
    }

    @Nullable
    public String getOpponentName() {
        return (String) getFieldByNumber(84);
    }

    @Nullable
    public Number[] getStrokeCount() {
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
    public Number[] getZoneCount() {
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
    public Float getMaxBallSpeed() {
        return (Float) getFieldByNumber(87);
    }

    @Nullable
    public Float getAvgBallSpeed() {
        return (Float) getFieldByNumber(88);
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
    public Float getTotalFractionalCycles() {
        return (Float) getFieldByNumber(94);
    }

    @Nullable
    public Number[] getAvgTotalHemoglobinConc() {
        final Object object = getFieldByNumber(95);
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
        final Object object = getFieldByNumber(96);
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
        final Object object = getFieldByNumber(97);
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
        final Object object = getFieldByNumber(98);
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
        final Object object = getFieldByNumber(99);
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
        final Object object = getFieldByNumber(100);
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
    public Float getAvgCombinedPedalSmoothness() {
        return (Float) getFieldByNumber(105);
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
    public Integer getSportIndex() {
        return (Integer) getFieldByNumber(111);
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
        final Object object = getFieldByNumber(116);
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
        final Object object = getFieldByNumber(117);
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
        final Object object = getFieldByNumber(118);
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
        final Object object = getFieldByNumber(119);
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
        final Object object = getFieldByNumber(120);
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
        final Object object = getFieldByNumber(121);
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
        final Object object = getFieldByNumber(122);
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
        final Object object = getFieldByNumber(123);
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
        return (Double) getFieldByNumber(124);
    }

    @Nullable
    public Double getEnhancedMaxSpeed() {
        return (Double) getFieldByNumber(125);
    }

    @Nullable
    public Double getEnhancedAvgAltitude() {
        return (Double) getFieldByNumber(126);
    }

    @Nullable
    public Double getEnhancedMinAltitude() {
        return (Double) getFieldByNumber(127);
    }

    @Nullable
    public Double getEnhancedMaxAltitude() {
        return (Double) getFieldByNumber(128);
    }

    @Nullable
    public Integer getAvgLevMotorPower() {
        return (Integer) getFieldByNumber(129);
    }

    @Nullable
    public Integer getMaxLevMotorPower() {
        return (Integer) getFieldByNumber(130);
    }

    @Nullable
    public Float getLevBatteryConsumption() {
        return (Float) getFieldByNumber(131);
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
    public Float getAvgVam() {
        return (Float) getFieldByNumber(139);
    }

    @Nullable
    public Double getAvgDepth() {
        return (Double) getFieldByNumber(140);
    }

    @Nullable
    public Double getMaxDepth() {
        return (Double) getFieldByNumber(141);
    }

    @Nullable
    public Long getSurfaceInterval() {
        return (Long) getFieldByNumber(142);
    }

    @Nullable
    public Integer getStartCns() {
        return (Integer) getFieldByNumber(143);
    }

    @Nullable
    public Integer getEndCns() {
        return (Integer) getFieldByNumber(144);
    }

    @Nullable
    public Integer getStartN2() {
        return (Integer) getFieldByNumber(145);
    }

    @Nullable
    public Integer getEndN2() {
        return (Integer) getFieldByNumber(146);
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
    public Integer getMinRespirationRate() {
        return (Integer) getFieldByNumber(149);
    }

    @Nullable
    public Integer getMinTemperature() {
        return (Integer) getFieldByNumber(150);
    }

    @Nullable
    public Integer getO2Toxicity() {
        return (Integer) getFieldByNumber(155);
    }

    @Nullable
    public Long getDiveNumber() {
        return (Long) getFieldByNumber(156);
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
    public Float getTotalGrit() {
        return (Float) getFieldByNumber(181);
    }

    @Nullable
    public Float getTotalFlow() {
        return (Float) getFieldByNumber(182);
    }

    @Nullable
    public Integer getJumpCount() {
        return (Integer) getFieldByNumber(183);
    }

    @Nullable
    public Float getAvgGrit() {
        return (Float) getFieldByNumber(186);
    }

    @Nullable
    public Float getAvgFlow() {
        return (Float) getFieldByNumber(187);
    }

    @Nullable
    public Integer getPrimaryBenefit() {
        return (Integer) getFieldByNumber(188);
    }

    @Nullable
    public Integer getWorkoutFeel() {
        return (Integer) getFieldByNumber(192);
    }

    @Nullable
    public Integer getWorkoutRpe() {
        return (Integer) getFieldByNumber(193);
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
    public Float getTotalFractionalAscent() {
        return (Float) getFieldByNumber(199);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return (Float) getFieldByNumber(200);
    }

    @Nullable
    public Integer getBeginningPotential() {
        return (Integer) getFieldByNumber(205);
    }

    @Nullable
    public Integer getEndingPotential() {
        return (Integer) getFieldByNumber(206);
    }

    @Nullable
    public Integer getMinStamina() {
        return (Integer) getFieldByNumber(207);
    }

    @Nullable
    public Float getAvgCoreTemperature() {
        return (Float) getFieldByNumber(208);
    }

    @Nullable
    public Float getMinCoreTemperature() {
        return (Float) getFieldByNumber(209);
    }

    @Nullable
    public Float getMaxCoreTemperature() {
        return (Float) getFieldByNumber(210);
    }

    @Nullable
    public Float getStepSpeedLoss() {
        return (Float) getFieldByNumber(222);
    }

    @Nullable
    public Float getStepSpeedLossPercentage() {
        return (Float) getFieldByNumber(223);
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

        public Builder setEstimatedSweatLoss(final Integer value) {
            setFieldByNumber(178, value);
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
    }
}
