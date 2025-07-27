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
public class FitEcgRawSample extends RecordData {
    public FitEcgRawSample(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 337) {
            throw new IllegalArgumentException("FitEcgRawSample expects global messages of " + 337 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getValue() {
        return (Float) getFieldByNumber(0);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(337);
        }

        public Builder setValue(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        @Override
        public FitEcgRawSample build() {
            return (FitEcgRawSample) super.build();
        }
    }
}
