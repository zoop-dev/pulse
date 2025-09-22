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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus.HrvStatus;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitHrvSummary extends RecordData {
    public FitHrvSummary(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 370) {
            throw new IllegalArgumentException("FitHrvSummary expects global messages of " + 370 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getWeeklyAverage() {
        return (Float) getFieldByNumber(0);
    }

    @Nullable
    public Float getLastNightAverage() {
        return (Float) getFieldByNumber(1);
    }

    @Nullable
    public Float getLastNight5MinHigh() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Float getBaselineLowUpper() {
        return (Float) getFieldByNumber(3);
    }

    @Nullable
    public Float getBaselineBalancedLower() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Float getBaselineBalancedUpper() {
        return (Float) getFieldByNumber(5);
    }

    @Nullable
    public HrvStatus getStatus() {
        return (HrvStatus) getFieldByNumber(6);
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
            super(370);
        }

        public Builder setWeeklyAverage(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setLastNightAverage(final Float value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLastNight5MinHigh(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setBaselineLowUpper(final Float value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setBaselineBalancedLower(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setBaselineBalancedUpper(final Float value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setStatus(final HrvStatus value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitHrvSummary build() {
            return (FitHrvSummary) super.build();
        }
    }
}
