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
public class FitSpo2 extends RecordData {
    public FitSpo2(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 269) {
            throw new IllegalArgumentException("FitSpo2 expects global messages of " + 269 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getReadingSpo2() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getReadingConfidence() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getMode() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(269);
        }

        public Builder setReadingSpo2(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setReadingConfidence(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setMode(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitSpo2 build() {
            return (FitSpo2) super.build();
        }
    }
}
