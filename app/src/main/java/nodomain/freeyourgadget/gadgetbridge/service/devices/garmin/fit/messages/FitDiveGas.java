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
public class FitDiveGas extends RecordData {
    public FitDiveGas(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 259) {
            throw new IllegalArgumentException("FitDiveGas expects global messages of " + 259 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getHeliumContent() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getOxygenContent() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getStatus() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(259);
        }

        public Builder setHeliumContent(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setOxygenContent(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStatus(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitDiveGas build() {
            return (FitDiveGas) super.build();
        }
    }
}
