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
    public Integer getWeatherReport() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getTemperature() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Condition getCondition() {
        return (Condition) getFieldByNumber(2);
    }

    @Nullable
    public Integer getWindDirection() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Float getWindSpeed() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Integer getPrecipitationProbability() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getTemperatureFeelsLike() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getRelativeHumidity() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public String getLocation() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public Long getObservedAtTime() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public Long getObservedLocationLat() {
        return (Long) getFieldByNumber(10);
    }

    @Nullable
    public Long getObservedLocationLong() {
        return (Long) getFieldByNumber(11);
    }

    @Nullable
    public DayOfWeek getDayOfWeek() {
        return (DayOfWeek) getFieldByNumber(12);
    }

    @Nullable
    public Integer getHighTemperature() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getLowTemperature() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Integer getDewPoint() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public Float getUvIndex() {
        return (Float) getFieldByNumber(16);
    }

    @Nullable
    public AQI_LEVELS getAirQuality() {
        return (AQI_LEVELS) getFieldByNumber(17);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(128);
        }

        public Builder setWeatherReport(final Integer value) {
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
    }
}
