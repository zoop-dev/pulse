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
public class FitRaceEvent extends RecordData {
    public FitRaceEvent(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 358) {
            throw new IllegalArgumentException("FitRaceEvent expects native messages of " + 358 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public String getEventId() {
        return getFieldByNumber(1, String.class);
    }

    @Nullable
    public Long getStartTime() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(4, String.class);
    }

    @Nullable
    public String getLocation() {
        return getFieldByNumber(5, String.class);
    }

    @Nullable
    public Double getStartPointLat() {
        return getFieldByNumber(6, Double.class);
    }

    @Nullable
    public Double getStartPointLong() {
        return getFieldByNumber(7, Double.class);
    }

    @Nullable
    public Long getDurationValue() {
        return getFieldByNumber(10, Long.class);
    }

    @Nullable
    public Integer getDurationUnits() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Long getTargetValue() {
        return getFieldByNumber(12, Long.class);
    }

    @Nullable
    public Integer getTargetUnits() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public String getCity() {
        return getFieldByNumber(24, String.class);
    }

    @Nullable
    public String getCountry() {
        return getFieldByNumber(26, String.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(358);
        }

        public Builder setEventId(final String value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLocation(final String value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setStartPointLat(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setStartPointLong(final Double value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setDurationValue(final Long value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setDurationUnits(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setTargetValue(final Long value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setTargetUnits(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setCity(final String value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setCountry(final String value) {
            setFieldByNumber(26, value);
            return this;
        }

        @Override
        public FitRaceEvent build() {
            return (FitRaceEvent) super.build();
        }

        @Override
        public FitRaceEvent build(final int localMessageType) {
            return (FitRaceEvent) super.build(localMessageType);
        }
    }
}
