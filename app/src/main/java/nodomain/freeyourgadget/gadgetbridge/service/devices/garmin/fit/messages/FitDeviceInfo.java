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
public class FitDeviceInfo extends RecordData {
    public FitDeviceInfo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 23) {
            throw new IllegalArgumentException("FitDeviceInfo expects global messages of " + 23 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getManufacturer() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getSerialNumber() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Integer getProduct() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSoftwareVersion() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(23);
        }

        public Builder setManufacturer(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSerialNumber(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setProduct(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSoftwareVersion(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitDeviceInfo build() {
            return (FitDeviceInfo) super.build();
        }
    }
}
