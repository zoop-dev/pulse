package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle.SwimStyle;

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

        public Builder setTotalCalores(final Integer value) {
            setFieldByNumber(11, value);
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

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(22, value);
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

        public Builder setNumLengths(final Integer value) {
            setFieldByNumber(32, value);
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

        public Builder setAvgSwolf(final Integer value) {
            setFieldByNumber(73, value);
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

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitLap build() {
            return (FitLap) super.build();
        }
    }
}
