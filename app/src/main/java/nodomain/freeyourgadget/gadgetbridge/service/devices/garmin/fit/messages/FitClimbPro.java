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
public class FitClimbPro extends RecordData {
    public FitClimbPro(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 317) {
            throw new IllegalArgumentException("FitClimbPro expects global messages of " + 317 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getPositionLat() {
        return getFieldByNumber(0, Double.class);
    }

    @Nullable
    public Double getPositionLong() {
        return getFieldByNumber(1, Double.class);
    }

    @Nullable
    public Integer getClimbProEvent() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getClimbNumber() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getClimbCategory() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Float getCurrentDist() {
        return getFieldByNumber(5, Float.class);
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
            super(317);
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setClimbProEvent(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setClimbNumber(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setClimbCategory(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setCurrentDist(final Float value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitClimbPro build() {
            return (FitClimbPro) super.build();
        }

        @Override
        public FitClimbPro build(final int localMessageType) {
            return (FitClimbPro) super.build(localMessageType);
        }
    }
}
