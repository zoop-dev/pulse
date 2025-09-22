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
public class FitPhysiologicalMetrics extends RecordData {
    public FitPhysiologicalMetrics(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 140) {
            throw new IllegalArgumentException("FitPhysiologicalMetrics expects global messages of " + 140 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getAerobicEffect() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Double getMetMax() {
        return (Double) getFieldByNumber(7);
    }

    @Nullable
    public Integer getRecoveryTime() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getLactateThresholdHeartRate() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Float getAnaerobicEffect() {
        return (Float) getFieldByNumber(20);
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
            super(140);
        }

        public Builder setAerobicEffect(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMetMax(final Double value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setRecoveryTime(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setLactateThresholdHeartRate(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setAnaerobicEffect(final Float value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitPhysiologicalMetrics build() {
            return (FitPhysiologicalMetrics) super.build();
        }
    }
}
