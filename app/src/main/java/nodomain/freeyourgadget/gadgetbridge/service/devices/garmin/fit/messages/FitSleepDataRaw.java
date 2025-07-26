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
public class FitSleepDataRaw extends RecordData {
    public FitSleepDataRaw(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 274) {
            throw new IllegalArgumentException("FitSleepDataRaw expects global messages of " + 274 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getBytes() {
        return (Integer) getFieldByNumber(0);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(274);
        }

        public Builder setBytes(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        @Override
        public FitSleepDataRaw build() {
            return (FitSleepDataRaw) super.build();
        }
    }
}
