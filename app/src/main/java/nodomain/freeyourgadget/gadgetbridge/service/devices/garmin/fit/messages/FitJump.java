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
public class FitJump extends RecordData {
    public FitJump(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 285) {
            throw new IllegalArgumentException("FitJump expects global messages of " + 285 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getDistance() {
        return getFieldByNumber(0, Float.class);
    }

    @Nullable
    public Float getHeigh() {
        return getFieldByNumber(1, Float.class);
    }

    @Nullable
    public Integer getRotations() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Float getHangTime() {
        return getFieldByNumber(3, Float.class);
    }

    @Nullable
    public Float getScore() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Double getPositionLat() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Double getPositionLong() {
        return getFieldByNumber(6, Double.class);
    }

    @Nullable
    public Float getSpeed() {
        return getFieldByNumber(7, Float.class);
    }

    @Nullable
    public Double getEnhancedSpeed() {
        return getFieldByNumber(8, Double.class);
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
            super(285);
        }

        public Builder setDistance(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setHeigh(final Float value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setRotations(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHangTime(final Float value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setScore(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setSpeed(final Float value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setEnhancedSpeed(final Double value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitJump build() {
            return (FitJump) super.build();
        }

        @Override
        public FitJump build(final int localMessageType) {
            return (FitJump) super.build(localMessageType);
        }
    }
}
