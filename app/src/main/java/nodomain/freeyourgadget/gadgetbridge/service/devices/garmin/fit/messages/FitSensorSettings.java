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
public class FitSensorSettings extends RecordData {
    public FitSensorSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 147) {
            throw new IllegalArgumentException("FitSensorSettings expects native messages of " + 147 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Long getAntId() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(2, String.class);
    }

    @Nullable
    public Integer getWheelSizeManual() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getCalibrationFactor() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getWheelSizeAuto() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getProduct() {
        return getFieldByNumber(32, Integer.class);
    }

    @Nullable
    public Integer getManufacturer() {
        return getFieldByNumber(33, Integer.class);
    }

    @Nullable
    public Integer getUseForSpeed() {
        return getFieldByNumber(45, Integer.class);
    }

    @Nullable
    public Integer getUseForDistance() {
        return getFieldByNumber(46, Integer.class);
    }

    @Nullable
    public Integer getConnectionType() {
        return getFieldByNumber(51, Integer.class);
    }

    @Nullable
    public Integer getSensorType() {
        return getFieldByNumber(52, Integer.class);
    }

    @Nullable
    public String getProductName() {
        return getFieldByNumber(91, String.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(147);
        }

        public Builder setAntId(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setWheelSizeManual(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setCalibrationFactor(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setWheelSizeAuto(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setProduct(final Integer value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setManufacturer(final Integer value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setUseForSpeed(final Integer value) {
            setFieldByNumber(45, value);
            return this;
        }

        public Builder setUseForDistance(final Integer value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setConnectionType(final Integer value) {
            setFieldByNumber(51, value);
            return this;
        }

        public Builder setSensorType(final Integer value) {
            setFieldByNumber(52, value);
            return this;
        }

        public Builder setProductName(final String value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSensorSettings build() {
            return (FitSensorSettings) super.build();
        }

        @Override
        public FitSensorSettings build(final int localMessageType) {
            return (FitSensorSettings) super.build(localMessageType);
        }
    }
}
