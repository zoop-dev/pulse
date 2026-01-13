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
public class FitTrainingSettings extends RecordData {
    public FitTrainingSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 13) {
            throw new IllegalArgumentException("FitTrainingSettings expects global messages of " + 13 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getTargetDistance() {
        return getFieldByNumber(31, Double.class);
    }

    @Nullable
    public Float getTargetSpeed() {
        return getFieldByNumber(32, Float.class);
    }

    @Nullable
    public Long getTargetTime() {
        return getFieldByNumber(33, Long.class);
    }

    @Nullable
    public Double getPreciseTargetSpeed() {
        return getFieldByNumber(153, Double.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(13);
        }

        public Builder setTargetDistance(final Double value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setTargetSpeed(final Float value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setTargetTime(final Long value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setPreciseTargetSpeed(final Double value) {
            setFieldByNumber(153, value);
            return this;
        }

        @Override
        public FitTrainingSettings build() {
            return (FitTrainingSettings) super.build();
        }

        @Override
        public FitTrainingSettings build(final int localMessageType) {
            return (FitTrainingSettings) super.build(localMessageType);
        }
    }
}
