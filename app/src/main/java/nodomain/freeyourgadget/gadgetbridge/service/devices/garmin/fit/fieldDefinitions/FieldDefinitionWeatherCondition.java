/*  Copyright (C) 2024-2026 Daniele Gobbetti, José Rebelo, Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.WeatherCondition.*;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.model.weather.OwmCondition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.WeatherCondition;

public class FieldDefinitionWeatherCondition extends FieldDefinition {

    public FieldDefinitionWeatherCondition(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    public static WeatherCondition fromId(final int id) {
        for (final WeatherCondition candidate : WeatherCondition.values()) {
            if (id == candidate.num) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public WeatherCondition decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof final WeatherCondition weatherCondition) {
            o = weatherCondition.num;
        } else if(o instanceof final Integer code) {
            final WeatherCondition condition = openWeatherCodeToFitWeatherStatus(code);
            o = condition != null ? condition.num : 255;
        } else {
            o = null;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    @Nullable
    public static WeatherCondition openWeatherCodeToFitWeatherStatus(int openWeatherCode) {
        final OwmCondition openWeatherCondition = OwmCondition.Companion.fromCode(openWeatherCode);
        if (openWeatherCondition == null) {
            return null;
        }

        return switch (openWeatherCondition) {
            case THUNDERSTORM_WITH_LIGHT_RAIN -> THUNDERSTORMS;
            case THUNDERSTORM_WITH_RAIN -> THUNDERSTORMS;
            case THUNDERSTORM_WITH_HEAVY_RAIN -> THUNDERSTORMS;
            case LIGHT_THUNDERSTORM -> THUNDERSTORMS;
            case THUNDERSTORM -> THUNDERSTORMS;
            case HEAVY_THUNDERSTORM -> THUNDERSTORMS;
            case RAGGED_THUNDERSTORM -> SCATTERED_THUNDERSTORMS;
            case THUNDERSTORM_WITH_LIGHT_DRIZZLE -> THUNDERSTORMS;
            case THUNDERSTORM_WITH_DRIZZLE -> THUNDERSTORMS;
            case THUNDERSTORM_WITH_HEAVY_DRIZZLE -> THUNDERSTORMS;
            case LIGHT_INTENSITY_DRIZZLE -> LIGHT_RAIN;
            case DRIZZLE -> LIGHT_RAIN;
            case HEAVY_INTENSITY_DRIZZLE -> HEAVY_RAIN;
            case LIGHT_INTENSITY_DRIZZLE_RAIN -> LIGHT_RAIN;
            case DRIZZLE_RAIN -> RAIN;
            case HEAVY_INTENSITY_DRIZZLE_RAIN -> HEAVY_RAIN;
            case SHOWER_RAIN_AND_DRIZZLE -> LIGHT_RAIN;
            case HEAVY_SHOWER_RAIN_AND_DRIZZLE -> HEAVY_RAIN;
            case SHOWER_DRIZZLE -> SCATTERED_SHOWERS;
            case LIGHT_RAIN -> LIGHT_RAIN;
            case MODERATE_RAIN -> RAIN;
            case HEAVY_INTENSITY_RAIN -> HEAVY_RAIN;
            case VERY_HEAVY_RAIN -> HEAVY_RAIN;
            case EXTREME_RAIN -> HEAVY_RAIN;
            case FREEZING_RAIN -> UNKNOWN_PRECIPITATION;
            case LIGHT_INTENSITY_SHOWER_RAIN -> LIGHT_RAIN;
            case SHOWER_RAIN -> LIGHT_RAIN;
            case HEAVY_INTENSITY_SHOWER_RAIN -> HEAVY_RAIN;
            case RAGGED_SHOWER_RAIN -> RAIN;
            case LIGHT_SNOW -> LIGHT_SNOW;
            case SNOW -> SNOW;
            case HEAVY_SNOW -> HEAVY_SNOW;
            case SLEET -> WINTRY_MIX;
            case LIGHT_SHOWER_SLEET -> WINTRY_MIX;
            case SHOWER_SLEET -> WINTRY_MIX;
            case LIGHT_RAIN_AND_SNOW -> LIGHT_RAIN_SNOW;
            case RAIN_AND_SNOW -> HEAVY_RAIN_SNOW;
            case LIGHT_SHOWER_SNOW -> SNOW;
            case SHOWER_SNOW -> SNOW;
            case HEAVY_SHOWER_SNOW -> HEAVY_SNOW;
            case MIST -> HAZY;
            case SMOKE -> HAZY;
            case HAZE -> HAZY;
            case SAND_OR_DUST_WHIRLS -> HAZY;
            case FOG -> FOG;
            case SAND -> HAZY;
            case DUST -> HAZY;
            case VOLCANIC_ASH -> HAZY;
            case SQUALLS -> WINDY;
            case TORNADO -> WINDY;
            case CLEAR_SKY -> CLEAR;
            case FEW_CLOUDS -> PARTLY_CLOUDY;
            case SCATTERED_CLOUDS -> PARTLY_CLOUDY;
            case BROKEN_CLOUDS -> MOSTLY_CLOUDY;
            case OVERCAST_CLOUDS -> CLOUDY;
            case TORNADO_900 -> THUNDERSTORMS;
            case TROPICAL_STORM -> THUNDERSTORMS;
            case HURRICANE_902 -> THUNDERSTORMS;
            case COLD -> null;
            case HOT -> null;
            case WINDY -> WINDY;
            case HAIL -> HAIL;
            case CALM -> null;
            case LIGHT_BREEZE -> null;
            case GENTLE_BREEZE -> null;
            case MODERATE_BREEZE -> null;
            case FRESH_BREEZE -> null;
            case STRONG_BREEZE -> null;
            case HIGH_WINDCASE_NEAR_GALE -> null;
            case GALE -> null;
            case SEVERE_GALE -> null;
            case STORM -> null;
            case VIOLENT_STORM -> null;
            case HURRICANE_962 -> THUNDERSTORMS;
            default -> null;
        };
    }

}
