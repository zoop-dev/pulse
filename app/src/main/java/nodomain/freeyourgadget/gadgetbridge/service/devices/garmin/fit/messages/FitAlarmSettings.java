package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import java.time.LocalTime;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel.Label;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitAlarmSettings extends RecordData {
    public FitAlarmSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 222) {
            throw new IllegalArgumentException("FitAlarmSettings expects global messages of " + 222 + ", got " + globalNumber);
        }
    }

    @Nullable
    public LocalTime getTime() {
        return (LocalTime) getFieldByNumber(0);
    }

    @Nullable
    public Long getRepeat() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Integer getEnabled() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getSound() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getBacklight() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getSomeTimestamp() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getUnknown7() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Label getLabel() {
        return (Label) getFieldByNumber(8);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(222);
        }

        public Builder setTime(final LocalTime value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setRepeat(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setEnabled(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSound(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setBacklight(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSomeTimestamp(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setUnknown7(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setLabel(final Label value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitAlarmSettings build() {
            return (FitAlarmSettings) super.build();
        }
    }
}
