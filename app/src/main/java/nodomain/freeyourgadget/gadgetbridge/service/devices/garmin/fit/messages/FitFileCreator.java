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
public class FitFileCreator extends RecordData {
    public FitFileCreator(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 49) {
            throw new IllegalArgumentException("FitFileCreator expects global messages of " + 49 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSoftwareVersion() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getHardwareVersion() {
        return (Integer) getFieldByNumber(1);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(49);
        }

        public Builder setSoftwareVersion(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setHardwareVersion(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        @Override
        public FitFileCreator build() {
            return (FitFileCreator) super.build();
        }
    }
}
