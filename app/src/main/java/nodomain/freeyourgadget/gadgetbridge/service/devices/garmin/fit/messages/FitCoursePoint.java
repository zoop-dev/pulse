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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoursePoint.CoursePoint;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitCoursePoint extends RecordData {
    public FitCoursePoint(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 32) {
            throw new IllegalArgumentException("FitCoursePoint expects global messages of " + 32 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Double getPositionLat() {
        return getFieldByNumber(2, Double.class);
    }

    @Nullable
    public Double getPositionLong() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Double getDistance() {
        return getFieldByNumber(4, Double.class);
    }

    @Nullable
    public CoursePoint getType() {
        return getFieldByNumber(5, CoursePoint.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(6, String.class);
    }

    @Nullable
    public Integer getFavorite() {
        return getFieldByNumber(8, Integer.class);
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
            super(32);
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setType(final CoursePoint value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setFavorite(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitCoursePoint build() {
            return (FitCoursePoint) super.build();
        }

        @Override
        public FitCoursePoint build(final int localMessageType) {
            return (FitCoursePoint) super.build(localMessageType);
        }
    }
}
