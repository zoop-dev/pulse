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
 * @noinspection unused
 */
public class FitSegmentLap extends RecordData {
    public FitSegmentLap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 142) {
            throw new IllegalArgumentException("FitSegmentLap expects global messages of " + 142 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getStartPositionLat() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Double getStartPositionLong() {
        return (Double) getFieldByNumber(4);
    }

    @Nullable
    public Double getEndPositionLat() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Double getEndPositionLong() {
        return (Double) getFieldByNumber(6);
    }

    @Nullable
    public Double getTotalDistance() {
        return (Double) getFieldByNumber(9);
    }

    @Nullable
    public Integer getTotalAscent() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getTotalDescent() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Double getNecLat() {
        return (Double) getFieldByNumber(25);
    }

    @Nullable
    public Double getNecLong() {
        return (Double) getFieldByNumber(26);
    }

    @Nullable
    public Double getSwcLat() {
        return (Double) getFieldByNumber(27);
    }

    @Nullable
    public Double getSwcLong() {
        return (Double) getFieldByNumber(28);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(29);
    }

    @Nullable
    public Integer getAvgTemperature() {
        return (Integer) getFieldByNumber(42);
    }

    @Nullable
    public Integer getMaxTemperature() {
        return (Integer) getFieldByNumber(43);
    }

    @Nullable
    public String getUuid() {
        return (String) getFieldByNumber(65);
    }

    @Nullable
    public Float getTotalFractionalAscent() {
        return (Float) getFieldByNumber(89);
    }

    @Nullable
    public Float getTotalFractionalDescent() {
        return (Float) getFieldByNumber(90);
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
            super(142);
        }

        public Builder setStartPositionLat(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setStartPositionLong(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setEndPositionLat(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setEndPositionLong(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTotalDistance(final Double value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTotalAscent(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setTotalDescent(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setNecLat(final Double value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setNecLong(final Double value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setSwcLat(final Double value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setSwcLong(final Double value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setAvgTemperature(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setMaxTemperature(final Integer value) {
            setFieldByNumber(43, value);
            return this;
        }

        public Builder setUuid(final String value) {
            setFieldByNumber(65, value);
            return this;
        }

        public Builder setTotalFractionalAscent(final Float value) {
            setFieldByNumber(89, value);
            return this;
        }

        public Builder setTotalFractionalDescent(final Float value) {
            setFieldByNumber(90, value);
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
        public FitSegmentLap build() {
            return (FitSegmentLap) super.build();
        }
    }
}
