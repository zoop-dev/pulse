package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.WeatherAqi;

public class FieldDefinitionWeatherAqi extends FieldDefinition {

    public FieldDefinitionWeatherAqi(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    public static WeatherAqi fromId(final int id) {
        for (final WeatherAqi candidate : WeatherAqi.values()) {
            if (id == candidate.num) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public WeatherAqi decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof final WeatherAqi aqi) {
            baseType.encode(byteBuffer, aqi.num, scale, offset);
            return;
        }

        final WeatherAqi aqiLevel;
        if (o instanceof final Number number){
            final int rawValue = number.intValue();
            aqiLevel = aqiAbsoluteValueToEnum(rawValue);
        } else {
            aqiLevel = null;
        }
        baseType.encode(byteBuffer, aqiLevel != null ? aqiLevel.num : o, scale, offset);
    }

    public static WeatherAqi aqiAbsoluteValueToEnum(int rawValue) { //see https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/domain/weather/index/PollutantIndex.kt#L38
        if (rawValue == -1) {
            return null; //invalid
        }
        if (rawValue < 20) {
            return WeatherAqi.GOOD;
        } else if (rawValue < 50) {
            return WeatherAqi.MODERATE;
        } else if (rawValue < 100) {
            return WeatherAqi.UNHEALTHY_SENSITIVE;
        } else if (rawValue < 150) {
            return WeatherAqi.UNHEALTHY;
        } else if (rawValue < 250) {
            return WeatherAqi.VERY_UNHEALTHY;
        } else {
            return WeatherAqi.HAZARDOUS;
        }
    }
}
