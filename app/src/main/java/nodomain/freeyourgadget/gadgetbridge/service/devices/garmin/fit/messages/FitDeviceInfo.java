/*  Copyright (C) 2025 Freeyourgadget

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitDeviceInfo extends RecordData {
    public FitDeviceInfo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 23) {
            throw new IllegalArgumentException("FitDeviceInfo expects global messages of " + 23 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getDeviceIndex() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getDeviceType() {
        return (Integer) getFieldByNumber(1);
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
    public Integer getHardwareVersion() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Long getCumOperatingTime() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Float getBatteryVoltage() {
        return (Float) getFieldByNumber(10);
    }

    @Nullable
    public Integer getBatteryStatus() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getSensorPosition() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public String getDescriptor() {
        return (String) getFieldByNumber(19);
    }

    @Nullable
    public Integer getAntTransmissionType() {
        return (Integer) getFieldByNumber(20);
    }

    @Nullable
    public Integer getAntDeviceNumber() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getAntNetwork() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Long getAntId() {
        return (Long) getFieldByNumber(24);
    }

    @Nullable
    public Integer getSourceType() {
        return (Integer) getFieldByNumber(25);
    }

    @Nullable
    public String getProductName() {
        return (String) getFieldByNumber(27);
    }

    @Nullable
    public Integer getBatteryLevel() {
        return (Integer) getFieldByNumber(32);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(23);
        }

        public Builder setDeviceIndex(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDeviceType(final Integer value) {
            setFieldByNumber(1, value);
            return this;
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

        public Builder setHardwareVersion(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setCumOperatingTime(final Long value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setBatteryVoltage(final Float value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setBatteryStatus(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setSensorPosition(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setDescriptor(final String value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setAntTransmissionType(final Integer value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setAntDeviceNumber(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setAntNetwork(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setAntId(final Long value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setSourceType(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setProductName(final String value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setBatteryLevel(final Integer value) {
            setFieldByNumber(32, value);
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
