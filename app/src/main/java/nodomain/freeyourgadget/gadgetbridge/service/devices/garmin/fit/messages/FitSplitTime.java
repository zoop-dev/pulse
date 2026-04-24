/*  Copyright (C) 2026 Freeyourgadget

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
public class FitSplitTime extends RecordData {
    public FitSplitTime(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 311) {
            throw new IllegalArgumentException("FitSplitTime expects native messages of " + 311 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Double getTime() {
        return getFieldByNumber(0, Double.class);
    }

    @Nullable
    public Double getDistance() {
        return getFieldByNumber(1, Double.class);
    }

    @Nullable
    public Double getSplitTime() {
        return getFieldByNumber(2, Double.class);
    }

    @Nullable
    public Double getSplitDistance() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Float getSplitSpeed() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Double getStartPositionLat() {
        return getFieldByNumber(9, Double.class);
    }

    @Nullable
    public Double getStartPositionLong() {
        return getFieldByNumber(10, Double.class);
    }

    @Nullable
    public Double getEndPositionLat() {
        return getFieldByNumber(11, Double.class);
    }

    @Nullable
    public Double getEndPositionLong() {
        return getFieldByNumber(12, Double.class);
    }

    @Nullable
    public Double getStartAltitude() {
        return getFieldByNumber(13, Double.class);
    }

    @Nullable
    public Double getEndAltitude() {
        return getFieldByNumber(14, Double.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(311);
        }

        public Builder setTime(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setSplitTime(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSplitDistance(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setSplitSpeed(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setStartPositionLat(final Double value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setStartPositionLong(final Double value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setEndPositionLat(final Double value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setEndPositionLong(final Double value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setStartAltitude(final Double value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setEndAltitude(final Double value) {
            setFieldByNumber(14, value);
            return this;
        }

        @Override
        public FitSplitTime build() {
            return (FitSplitTime) super.build();
        }

        @Override
        public FitSplitTime build(final int localMessageType) {
            return (FitSplitTime) super.build(localMessageType);
        }
    }
}
