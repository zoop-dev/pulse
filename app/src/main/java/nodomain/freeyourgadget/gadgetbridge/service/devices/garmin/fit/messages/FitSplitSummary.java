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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getNumSplits() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Double getTotalDistance() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Double getAvgSpeed() {
        return (Double) getFieldByNumber(6);
    }

    @Nullable
    public Double getMaxSpeed() {
        return (Double) getFieldByNumber(7);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getAvgHeartRate() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Double getAvgVertSpeed() {
        return (Double) getFieldByNumber(12);
    }

    @Nullable
    public Long getTotalCalories() {
        return (Long) getFieldByNumber(13);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return (Double) getFieldByNumber(77);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
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
    }
}
