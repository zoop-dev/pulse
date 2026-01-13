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
public class FitHr extends RecordData {
    public FitHr(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 132) {
            throw new IllegalArgumentException("FitHr expects global messages of " + 132 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getFractionalTimestamp() {
        return getFieldByNumber(0, Float.class);
    }

    @Nullable
    public Float getTime256() {
        return getFieldByNumber(1, Float.class);
    }

    @Nullable
    public Number[] getFilteredBpm() {
        return getArrayFieldByNumber(6, Number.class);
    }

    @Nullable
    public Number[] getEventTimestamp() {
        return getArrayFieldByNumber(9, Number.class);
    }

    @Nullable
    public Number[] getEventTimestamp12() {
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
            super(132);
        }

        public Builder setFractionalTimestamp(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTime256(final Float value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setFilteredBpm(final Number[] value) {
            setFieldByNumber(6, (Object[]) value);
            return this;
        }

        public Builder setEventTimestamp(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setEventTimestamp12(final Number[] value) {
            setFieldByNumber(10, (Object[]) value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitHr build() {
            return (FitHr) super.build();
        }

        @Override
        public FitHr build(final int localMessageType) {
            return (FitHr) super.build(localMessageType);
        }
    }
}
