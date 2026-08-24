/*  Copyright (C) 2026 Thomas Kuehne

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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.WeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.WeatherCondition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;

public class FitWeather extends AbstractFitWeather {
    public FitWeather(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);
    }

    public static class Builder extends AbstractFitWeather.Builder {
        public void weatherWindDirection(int degree) {
            if (0 <= degree) {
                degree %= 360;
                setWindDirection(degree);
            } else {
                setWindDirection(null);
            }
        }

        public void weatherPrecipitationProbability(int probability) {
            if (0 <= probability) {
                if (probability > 100) {
                    probability = 100;
                }
                setPrecipitationProbability(probability);
            } else {
                setPrecipitationProbability(null);
            }
        }

        public void weatherRelativeHumidity(int humidity) {
            if (0 <= humidity) {
                if (humidity > 100) {
                    humidity = 100;
                }
                setRelativeHumidity(humidity);
            } else {
                setRelativeHumidity(null);
            }
        }

        public void weatherUvIndex(float uvIndex) {
            // WeatherSpec: 0-15
            // FIT: 0-10
            if (0.0f <= uvIndex) {
                // clamp, don't scale, to keep in line with direct web retriveal
                // >=10 is extreme UV exposure in both scales
                if (uvIndex > 10.0f) {
                    uvIndex = 10.0f;
                }
                setUvIndex(uvIndex);
            } else {
                setUvIndex(null);
            }
        }

        public void weatherWindSpeed(final float kilometerPerHour) {
            // WeatherSpec: kilometer per hour
            // FIT: millimeter per second
            if (kilometerPerHour >= 0.0f) {
                int speed = Math.round(kilometerPerHour / 3.6f * 1000.0f);
                if (speed >= 0xFFFF) {
                    speed = 0xFFFE;
                }
                setWindSpeed(speed);
            } else {
                setWindSpeed(null);
            }
        }

        public void weatherTemperature(final int kelvin) {
            // #4313 - We do a "wrong" conversion to celsius on purpose
            if (kelvin > 0) {
                setTemperature(kelvin - 273);
            } else {
                setTemperature(null);
            }
        }

        public void weatherTemperatureFeelsLike(final int kelvin) {
            // #4313 - We do a "wrong" conversion to celsius on purpose
            if (kelvin > 0) {
                setTemperatureFeelsLike(kelvin - 273);
            } else {
                setTemperatureFeelsLike(null);
            }
        }

        public void weatherHighTemperature(final int kelvin) {
            // #4313 - We do a "wrong" conversion to celsius on purpose
            if (kelvin > 0) {
                setHighTemperature(kelvin - 273);
            } else {
                setHighTemperature(null);
            }
        }

        public void weatherLowTemperature(final int kelvin) {
            // #4313 - We do a "wrong" conversion to celsius on purpose
            if (kelvin > 0) {
                setLowTemperature(kelvin - 273);
            } else {
                setLowTemperature(null);
            }
        }

        public void weatherDewPoint(final int kelvin) {
            // #4313 - We do a "wrong" conversion to celsius on purpose
            if (kelvin > 0) {
                setDewPoint(kelvin - 273);
            } else {
                setDewPoint(null);
            }
        }

        public void weatherCondition(final int openWeatherCode) {
            WeatherCondition weatherCondition = FieldDefinitionWeatherCondition.openWeatherCodeToFitWeatherStatus(openWeatherCode);
            setCondition(weatherCondition);
        }

        public void weatherAirQuality(final WeatherSpec.AirQuality quality) {
            if (quality != null) {
                int aqi = quality.getAqi();
                WeatherAqi weatherAqi = FieldDefinitionWeatherAqi.aqiAbsoluteValueToEnum(aqi);
                setAirQuality(weatherAqi);
            } else {
                setAirQuality(null);
            }
        }

        public void weatherDayOfWeek(final long timestamp) {
            Instant instant = Instant.ofEpochSecond(timestamp);
            ZoneId zone = ZoneId.systemDefault();
            ZonedDateTime date = instant.atZone(zone);
            DayOfWeek day = date.getDayOfWeek();
            setDayOfWeek(day);
        }

        public void weatherAtmosphericPressure(final float millibar) {
            if (millibar > 0.0f) {
                long pascal = Math.round(millibar * 100.0);
                setAtmosphericPressure(pascal);
            } else {
                setAtmosphericPressure(null);
            }
        }
    }
}
