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
public class FitHole extends RecordData {
    public FitHole(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 193) {
            throw new IllegalArgumentException("FitHole expects native messages of " + 193 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getHoleNumber() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Double getDistance() {
        return getFieldByNumber(1, Double.class);
    }

    @Nullable
    public Integer getPar() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getHandicap() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Double getPositionLat() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public Double getPositionLong() {
        return getFieldByNumber(5, Double.class);
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
            super(193);
        }

        public Builder setHoleNumber(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setPar(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHandicap(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitHole build() {
            return (FitHole) super.build();
        }

        @Override
        public FitHole build(final int localMessageType) {
            return (FitHole) super.build(localMessageType);
        }
    }
}
