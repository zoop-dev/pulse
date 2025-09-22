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

    /**
     * @noinspection unused
     */
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
