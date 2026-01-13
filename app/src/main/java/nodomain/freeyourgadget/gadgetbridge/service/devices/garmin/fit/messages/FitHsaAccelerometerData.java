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
public class FitHsaAccelerometerData extends RecordData {
    public FitHsaAccelerometerData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 302) {
            throw new IllegalArgumentException("FitHsaAccelerometerData expects global messages of " + 302 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTimestampMs() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getSamplingInterval() {
        return getFieldByNumber(1, Integer.class);
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
    public Long getTimestamp32k() {
        return getFieldByNumber(5, Long.class);
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
            super(302);
        }

        public Builder setTimestampMs(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSamplingInterval(final Integer value) {
            setFieldByNumber(1, value);
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

        public Builder setTimestamp32k(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitHsaAccelerometerData build() {
            return (FitHsaAccelerometerData) super.build();
        }

        @Override
        public FitHsaAccelerometerData build(final int localMessageType) {
            return (FitHsaAccelerometerData) super.build(localMessageType);
        }
    }
}
