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
public class FitNap extends RecordData {
    public FitNap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 412) {
            throw new IllegalArgumentException("FitNap expects global messages of " + 412 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getStartTimestamp() {
        return (Long) getFieldByNumber(0);
    }

    @Nullable
    public Integer getUnknown1() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getEndTimestamp() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Integer getUnknown3() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getUnknown4() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getUnknown6() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Long getTimestamp7() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(412);
        }

        public Builder setStartTimestamp(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setUnknown1(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setEndTimestamp(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setUnknown3(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setUnknown4(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setUnknown6(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTimestamp7(final Long value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitNap build() {
            return (FitNap) super.build();
        }
    }
}
