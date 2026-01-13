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
public class FitLength extends RecordData {
    public FitLength(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 101) {
            throw new IllegalArgumentException("FitLength expects global messages of " + 101 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getEvent() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getEventType() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Long getStartTime() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Double getTotalElapsedTime() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Integer getTotalStrokes() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Float getAvgSpeed() {
        return getFieldByNumber(6, Float.class);
    }

    @Nullable
    public Integer getSwimStroke() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getAvgSwimmingCadence() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getEventGroup() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getTotalCalories() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getLengthType() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getPlayerScore() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getOpponentScore() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Number[] getStrokeCount() {
        return getArrayFieldByNumber(20, Number.class);
    }

    @Nullable
    public Number[] getZoneCount() {
        return getArrayFieldByNumber(21, Number.class);
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return getFieldByNumber(22, Float.class);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return getFieldByNumber(23, Float.class);
    }

    @Nullable
    public Integer getAvgRespirationRate() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Integer getMaxRespirationRate() {
        return getFieldByNumber(25, Integer.class);
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
            super(101);
        }

        public Builder setEvent(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setEventType(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setTotalElapsedTime(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTotalTimerTime(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTotalStrokes(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setAvgSpeed(final Float value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setSwimStroke(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setAvgSwimmingCadence(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setEventGroup(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTotalCalories(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setLengthType(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setPlayerScore(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setOpponentScore(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setStrokeCount(final Number[] value) {
            setFieldByNumber(20, (Object[]) value);
            return this;
        }

        public Builder setZoneCount(final Number[] value) {
            setFieldByNumber(21, (Object[]) value);
            return this;
        }

        public Builder setEnhancedAvgRespirationRate(final Float value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setEnhancedMaxRespirationRate(final Float value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setAvgRespirationRate(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setMaxRespirationRate(final Integer value) {
            setFieldByNumber(25, value);
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
        public FitLength build() {
            return (FitLength) super.build();
        }

        @Override
        public FitLength build(final int localMessageType) {
            return (FitLength) super.build(localMessageType);
        }
    }
}
