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
public class FitMaxMetData extends RecordData {
    public FitMaxMetData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 229) {
            throw new IllegalArgumentException("FitMaxMetData expects global messages of " + 229 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getUpdateTime() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public Float getVo2Max() {
        return getFieldByNumber(2, Float.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getMaxMetCategory() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getCalibratedData() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getHrSource() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getSpeedSource() {
        return getFieldByNumber(13, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(229);
        }

        public Builder setUpdateTime(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setVo2Max(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setMaxMetCategory(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setCalibratedData(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setHrSource(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setSpeedSource(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        @Override
        public FitMaxMetData build() {
            return (FitMaxMetData) super.build();
        }

        @Override
        public FitMaxMetData build(final int localMessageType) {
            return (FitMaxMetData) super.build(localMessageType);
        }
    }
}
