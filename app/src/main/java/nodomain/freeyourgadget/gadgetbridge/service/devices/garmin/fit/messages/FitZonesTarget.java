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
public class FitZonesTarget extends RecordData {
    public FitZonesTarget(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 7) {
            throw new IllegalArgumentException("FitZonesTarget expects global messages of " + 7 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getFunctionalThresholdPower() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getThresholdHeartRate() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHrCalcType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getPwrCalcType() {
        return (Integer) getFieldByNumber(7);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(7);
        }

        public Builder setFunctionalThresholdPower(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setThresholdHeartRate(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHrCalcType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setPwrCalcType(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitZonesTarget build() {
            return (FitZonesTarget) super.build();
        }
    }
}
