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
public class FitEcgSmoothSample extends RecordData {
    public FitEcgSmoothSample(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 338) {
            throw new IllegalArgumentException("FitEcgSmoothSample expects global messages of " + 338 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getValue() {
        return (Float) getFieldByNumber(0);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(338);
        }

        public Builder setValue(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        @Override
        public FitEcgSmoothSample build() {
            return (FitEcgSmoothSample) super.build();
        }
    }
}
