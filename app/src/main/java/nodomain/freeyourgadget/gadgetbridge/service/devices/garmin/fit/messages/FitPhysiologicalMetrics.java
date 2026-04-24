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

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 140) {
            throw new IllegalArgumentException("FitPhysiologicalMetrics expects native messages of " + 140 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getNewHrMax() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Float getAerobicEffect() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Double getMetMax() {
        return getFieldByNumber(7, Double.class);
    }

    @Nullable
    public Integer getRecoveryTime() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getLactateThresholdHeartRate() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getLactateThresholdPower() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Float getLactateThresholdSpeed() {
        return getFieldByNumber(16, Float.class);
    }

    @Nullable
    public Integer getEndingPerformanceCondition() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Float getAnaerobicEffect() {
        return getFieldByNumber(20, Float.class);
    }

    @Nullable
    public Integer getEndingBodyBattery() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Double getFirstVo2Max() {
        return getFieldByNumber(29, Double.class);
    }

    @Nullable
    public Integer getPrimaryBenefit() {
        return getFieldByNumber(41, Integer.class);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(48, Long.class);
    }

    @Nullable
    public Integer getEndingPotential() {
        return getFieldByNumber(50, Integer.class);
    }

    @Nullable
    public Integer getTotalAscent() {
        return getFieldByNumber(60, Integer.class);
    }

    @Nullable
    public Integer getTotalDescent() {
        return getFieldByNumber(61, Integer.class);
    }

    @Nullable
    public Integer getAveragePower() {
        return getFieldByNumber(62, Integer.class);
    }

    @Nullable
    public Integer getAverageHeartRate() {
        return getFieldByNumber(63, Integer.class);
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
            super(140);
        }

        public Builder setNewHrMax(final Integer value) {
            setFieldByNumber(1, value);
            return this;
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

        public Builder setSport(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setLactateThresholdHeartRate(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setLactateThresholdPower(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setLactateThresholdSpeed(final Float value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setEndingPerformanceCondition(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setAnaerobicEffect(final Float value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setEndingBodyBattery(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setFirstVo2Max(final Double value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setPrimaryBenefit(final Integer value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(48, value);
            return this;
        }

        public Builder setEndingPotential(final Integer value) {
            setFieldByNumber(50, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(60, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(61, value);
            return this;
        }

        public Builder setAveragePower(final Integer value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setAverageHeartRate(final Integer value) {
            setFieldByNumber(63, value);
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

        @Override
        public FitPhysiologicalMetrics build(final int localMessageType) {
            return (FitPhysiologicalMetrics) super.build(localMessageType);
        }
    }
}
