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
public class FitGolfCourse extends RecordData {
    public FitGolfCourse(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 190) {
            throw new IllegalArgumentException("FitGolfCourse expects native messages of " + 190 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Long getCourseId() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(1, String.class);
    }

    @Nullable
    public Long getLocalTime() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Long getStartTime() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public Long getEndTime() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Integer getOut() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getIn() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getTotal() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public String getTee() {
        return getFieldByNumber(11, String.class);
    }

    @Nullable
    public Integer getSlope() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Float getRating() {
        return getFieldByNumber(21, Float.class);
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
            super(190);
        }

        public Builder setCourseId(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLocalTime(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setEndTime(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setOut(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setIn(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTotal(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTee(final String value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setSlope(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setRating(final Float value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitGolfCourse build() {
            return (FitGolfCourse) super.build();
        }

        @Override
        public FitGolfCourse build(final int localMessageType) {
            return (FitGolfCourse) super.build(localMessageType);
        }
    }
}
