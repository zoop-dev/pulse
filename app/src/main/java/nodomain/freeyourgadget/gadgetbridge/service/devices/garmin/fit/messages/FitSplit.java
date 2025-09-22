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
public class FitSplit extends RecordData {
    public FitSplit(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 312) {
            throw new IllegalArgumentException("FitSplit expects global messages of " + 312 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSplitType() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Double getTotalElapsedTime() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return (Double) getFieldByNumber(2);
    }

    @Nullable
    public Double getTotalDistance() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getAvgSpeed() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Double getStartPositionLat() {
        return (Double) getFieldByNumber(21);
    }

    @Nullable
    public Double getStartPositionLong() {
        return (Double) getFieldByNumber(22);
    }

    @Nullable
    public Double getEndPositionLat() {
        return (Double) getFieldByNumber(23);
    }

    @Nullable
    public Double getEndPositionLong() {
        return (Double) getFieldByNumber(24);
    }

    @Nullable
    public Double getMaxSpeed() {
        return (Double) getFieldByNumber(25);
    }

    @Nullable
    public Double getAvgVertSpeed() {
        return (Double) getFieldByNumber(26);
    }

    @Nullable
    public Long getEndTime() {
        return (Long) getFieldByNumber(27);
    }

    @Nullable
    public Long getTotalCalories() {
        return (Long) getFieldByNumber(28);
    }

    @Nullable
    public Double getStartElevation() {
        return (Double) getFieldByNumber(74);
    }

    @Nullable
    public Double getTotalMovingTime() {
        return (Double) getFieldByNumber(110);
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
            super(312);
        }

        public Builder setSplitType(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTotalElapsedTime(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setTotalTimerTime(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setTotalDistance(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setAvgSpeed(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setStartPositionLat(final Double value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setStartPositionLong(final Double value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setEndPositionLat(final Double value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setEndPositionLong(final Double value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setMaxSpeed(final Double value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setAvgVertSpeed(final Double value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setEndTime(final Long value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setTotalCalories(final Long value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setStartElevation(final Double value) {
            setFieldByNumber(74, value);
            return this;
        }

        public Builder setTotalMovingTime(final Double value) {
            setFieldByNumber(110, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSplit build() {
            return (FitSplit) super.build();
        }
    }
}
