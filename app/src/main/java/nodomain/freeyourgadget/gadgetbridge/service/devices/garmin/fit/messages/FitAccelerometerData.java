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
public class FitAccelerometerData extends RecordData {
    public FitAccelerometerData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 165) {
            throw new IllegalArgumentException("FitAccelerometerData expects global messages of " + 165 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTimestampMs() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Number[] getSampleTimeOffset() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Number[] getAccelX() {
        return getArrayFieldByNumber(2, Number.class);
    }

    @Nullable
    public Number[] getAccelY() {
        return getArrayFieldByNumber(3, Number.class);
    }

    @Nullable
    public Number[] getAccelZ() {
        return getArrayFieldByNumber(4, Number.class);
    }

    @Nullable
    public Number[] getCalibratedAccelX() {
        return getArrayFieldByNumber(5, Number.class);
    }

    @Nullable
    public Number[] getCalibratedAccelY() {
        return getArrayFieldByNumber(6, Number.class);
    }

    @Nullable
    public Number[] getCalibratedAccelZ() {
        return getArrayFieldByNumber(7, Number.class);
    }

    @Nullable
    public Number[] getCompressedCalibratedAccelX() {
        return getArrayFieldByNumber(8, Number.class);
    }

    @Nullable
    public Number[] getCompressedCalibratedAccelY() {
        return getArrayFieldByNumber(9, Number.class);
    }

    @Nullable
    public Number[] getCompressedCalibratedAccelZ() {
        return getArrayFieldByNumber(10, Number.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(165);
        }

        public Builder setTimestampMs(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSampleTimeOffset(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setAccelX(final Number[] value) {
            setFieldByNumber(2, (Object[]) value);
            return this;
        }

        public Builder setAccelY(final Number[] value) {
            setFieldByNumber(3, (Object[]) value);
            return this;
        }

        public Builder setAccelZ(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setCalibratedAccelX(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setCalibratedAccelY(final Number[] value) {
            setFieldByNumber(6, (Object[]) value);
            return this;
        }

        public Builder setCalibratedAccelZ(final Number[] value) {
            setFieldByNumber(7, (Object[]) value);
            return this;
        }

        public Builder setCompressedCalibratedAccelX(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setCompressedCalibratedAccelY(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setCompressedCalibratedAccelZ(final Number[] value) {
            setFieldByNumber(10, (Object[]) value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitAccelerometerData build() {
            return (FitAccelerometerData) super.build();
        }

        @Override
        public FitAccelerometerData build(final int localMessageType) {
            return (FitAccelerometerData) super.build(localMessageType);
        }
    }
}
