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
public class FitSport extends RecordData {
    public FitSport(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 12) {
            throw new IllegalArgumentException("FitSport expects global messages of " + 12 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(3);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(12);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(3, value);
            return this;
        }

        @Override
        public FitSport build() {
            return (FitSport) super.build();
        }
    }
}
