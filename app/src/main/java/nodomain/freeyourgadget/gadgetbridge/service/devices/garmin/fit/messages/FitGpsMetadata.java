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
public class FitGpsMetadata extends RecordData {
    public FitGpsMetadata(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 160) {
            throw new IllegalArgumentException("FitGpsMetadata expects global messages of " + 160 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getEnhancedAltitude() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Long getEnhancedSpeed() {
        return (Long) getFieldByNumber(4);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(160);
        }

        public Builder setEnhancedAltitude(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setEnhancedSpeed(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        @Override
        public FitGpsMetadata build() {
            return (FitGpsMetadata) super.build();
        }
    }
}
