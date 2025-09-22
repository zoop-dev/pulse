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
public class FitSegmentPoint extends RecordData {
    public FitSegmentPoint(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 150) {
            throw new IllegalArgumentException("FitSegmentPoint expects global messages of " + 150 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getPositionLat() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Double getPositionLong() {
        return (Double) getFieldByNumber(2);
    }

    @Nullable
    public Double getDistance() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Float getAltitude() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Number[] getLeaderTime() {
        final Object object = getFieldByNumber(5);
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
    public Double getEnhancedAltitude() {
        return (Double) getFieldByNumber(6);
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
            super(150);
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setAltitude(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLeaderTime(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setEnhancedAltitude(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSegmentPoint build() {
            return (FitSegmentPoint) super.build();
        }
    }
}
