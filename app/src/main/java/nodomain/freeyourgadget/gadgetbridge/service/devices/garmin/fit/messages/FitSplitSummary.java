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
public class FitSplitSummary extends RecordData {
    public FitSplitSummary(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 313) {
            throw new IllegalArgumentException("FitSplitSummary expects global messages of " + 313 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSplitType() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getNumSplits() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Double getTotalDistance() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Double getAvgSpeed() {
        return getFieldByNumber(6, Double.class);
    }

    @Nullable
    public Double getMaxSpeed() {
        return getFieldByNumber(7, Double.class);
    }

    @Nullable
    public Integer getTotalAscent() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getTotalDescent() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getAvgHeartRate() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Double getAvgVertSpeed() {
        return getFieldByNumber(12, Double.class);
    }

    @Nullable
    public Long getTotalCalories() {
        return getFieldByNumber(13, Long.class);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return getFieldByNumber(77, Double.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(313);
        }

        public Builder setSplitType(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setNumSplits(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTotalTimerTime(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTotalDistance(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setAvgSpeed(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setMaxSpeed(final Double value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setAvgHeartRate(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setAvgVertSpeed(final Double value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setTotalCalories(final Long value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setTotalMovingTime(final Double value) {
            setFieldByNumber(77, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSplitSummary build() {
            return (FitSplitSummary) super.build();
        }

        @Override
        public FitSplitSummary build(final int localMessageType) {
            return (FitSplitSummary) super.build(localMessageType);
        }
    }
}
