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

import java.time.DayOfWeek;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi.AQI_LEVELS;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition.Condition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherReport.Type;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitWeather extends RecordData {
    public FitWeather(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 128) {
            throw new IllegalArgumentException("FitWeather expects global messages of " + 128 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Type getWeatherReport() {
        return getFieldByNumber(0, Type.class);
    }

    @Nullable
    public Integer getTemperature() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Condition getCondition() {
        return getFieldByNumber(2, Condition.class);
    }

    @Nullable
    public Integer getWindDirection() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Float getWindSpeed() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Integer getPrecipitationProbability() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getTemperatureFeelsLike() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getRelativeHumidity() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public String getLocation() {
        return getFieldByNumber(8, String.class);
    }

    @Nullable
    public Long getObservedAtTime() {
        return getFieldByNumber(9, Long.class);
    }

    @Nullable
    public Long getObservedLocationLat() {
        return getFieldByNumber(10, Long.class);
    }

    @Nullable
    public Long getObservedLocationLong() {
        return getFieldByNumber(11, Long.class);
    }

    @Nullable
    public DayOfWeek getDayOfWeek() {
        return getFieldByNumber(12, DayOfWeek.class);
    }

    @Nullable
    public Integer getHighTemperature() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getLowTemperature() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getDewPoint() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Float getUvIndex() {
        return getFieldByNumber(16, Float.class);
    }

    @Nullable
    public AQI_LEVELS getAirQuality() {
        return getFieldByNumber(17, AQI_LEVELS.class);
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
            super(128);
        }

        public Builder setWeatherReport(final Type value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTemperature(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setCondition(final Condition value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setWindDirection(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setWindSpeed(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setPrecipitationProbability(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTemperatureFeelsLike(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setRelativeHumidity(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setLocation(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setObservedAtTime(final Long value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setObservedLocationLat(final Long value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setObservedLocationLong(final Long value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setDayOfWeek(final DayOfWeek value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setHighTemperature(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setLowTemperature(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setDewPoint(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setUvIndex(final Float value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setAirQuality(final AQI_LEVELS value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitWeather build() {
            return (FitWeather) super.build();
        }

        @Override
        public FitWeather build(final int localMessageType) {
            return (FitWeather) super.build(localMessageType);
        }
    }
}
