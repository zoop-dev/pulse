package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
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
    public Integer getTotalCalores() {
        return (Integer) getFieldByNumber(11);
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
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(22);
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
    public Integer getNumLengths() {
        return (Integer) getFieldByNumber(32);
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
    public Integer getSwimStyle() {
        return (Integer) getFieldByNumber(38);
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
    public Integer getAvgSwolf() {
        return (Integer) getFieldByNumber(73);
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
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
