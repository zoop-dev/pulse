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
public class FitObdiiData extends RecordData {
    public FitObdiiData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 174) {
            throw new IllegalArgumentException("FitObdiiData expects global messages of " + 174 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTimestampMs() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Number[] getTimeOffset() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Integer getPid() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Number[] getRawData() {
        return getArrayFieldByNumber(3, Number.class);
    }

    @Nullable
    public Number[] getPidDataSize() {
        return getArrayFieldByNumber(4, Number.class);
    }

    @Nullable
    public Number[] getSystemTime() {
        return getArrayFieldByNumber(5, Number.class);
    }

    @Nullable
    public Long getStartTimestamp() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public Integer getStartTimestampMs() {
        return getFieldByNumber(7, Integer.class);
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
            super(174);
        }

        public Builder setTimestampMs(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTimeOffset(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setPid(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setRawData(final Number[] value) {
            setFieldByNumber(3, (Object[]) value);
            return this;
        }

        public Builder setPidDataSize(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setSystemTime(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setStartTimestamp(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setStartTimestampMs(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitObdiiData build() {
            return (FitObdiiData) super.build();
        }

        @Override
        public FitObdiiData build(final int localMessageType) {
            return (FitObdiiData) super.build(localMessageType);
        }
    }
}
