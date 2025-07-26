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
public class FitTrainingLoad extends RecordData {
    public FitTrainingLoad(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 378) {
            throw new IllegalArgumentException("FitTrainingLoad expects global messages of " + 378 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTrainingLoadAcute() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getTrainingLoadChronic() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(378);
        }

        public Builder setTrainingLoadAcute(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTrainingLoadChronic(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitTrainingLoad build() {
            return (FitTrainingLoad) super.build();
        }
    }
}
