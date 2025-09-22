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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getEventType() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Double getTotalElapsedTime() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getTotalTimerTime() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Integer getTotalStrokes() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Float getAvgSpeed() {
        return (Float) getFieldByNumber(6);
    }

    @Nullable
    public Integer getSwimStroke() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getAvgSwimmingCadence() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getEventGroup() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getTotalCalories() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getLengthType() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getPlayerScore() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Integer getOpponentScore() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Number[] getStrokeCount() {
        final Object object = getFieldByNumber(20);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getZoneCount() {
        final Object object = getFieldByNumber(21);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Float getEnhancedAvgRespirationRate() {
        return (Float) getFieldByNumber(22);
    }

    @Nullable
    public Float getEnhancedMaxRespirationRate() {
        return (Float) getFieldByNumber(23);
    }

    @Nullable
    public Integer getAvgRespirationRate() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Integer getMaxRespirationRate() {
        return (Integer) getFieldByNumber(25);
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
    }
}
