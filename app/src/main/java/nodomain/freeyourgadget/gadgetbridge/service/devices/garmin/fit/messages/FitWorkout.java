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
public class FitWorkout extends RecordData {
    public FitWorkout(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 26) {
            throw new IllegalArgumentException("FitWorkout expects global messages of " + 26 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getCapabilities() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getNumValidSteps() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public String getNotes() {
        return (String) getFieldByNumber(17);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(26);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setCapabilities(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setNumValidSteps(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setNotes(final String value) {
            setFieldByNumber(17, value);
            return this;
        }

        @Override
        public FitWorkout build() {
            return (FitWorkout) super.build();
        }
    }
}
