package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionWeatherAqi extends FieldDefinition {

    public FieldDefinitionWeatherAqi(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return AQI_LEVELS.values()[raw];
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof AQI_LEVELS) {
            baseType.encode(byteBuffer, ((AQI_LEVELS) o).ordinal(), scale, offset);
            return;
        }
        final AQI_LEVELS aqiLevel = o != null ? aqiAbsoluteValueToEnum((int) o) : null;
        baseType.encode(byteBuffer, aqiLevel != null ? aqiLevel.ordinal() : o, scale, offset);
    }

    public static AQI_LEVELS aqiAbsoluteValueToEnum(int rawValue) { //see https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/domain/weather/index/PollutantIndex.kt#L38
        if (rawValue == -1) {
            return null; //invalid
        }
        if (rawValue < 20) {
            return AQI_LEVELS.GOOD;
        } else if (rawValue < 50) {
            return AQI_LEVELS.MODERATE;
        } else if (rawValue < 100) {
            return AQI_LEVELS.UNHEALTHY_SENSITIVE;
        } else if (rawValue < 150) {
            return AQI_LEVELS.UNHEALTHY;
        } else if (rawValue < 250) {
            return AQI_LEVELS.VERY_UNHEALTHY;
        } else {
            return AQI_LEVELS.HAZARDOUS;
        }
    }

    public enum AQI_LEVELS {
        GOOD,
        MODERATE,
        UNHEALTHY_SENSITIVE,
        UNHEALTHY,
        VERY_UNHEALTHY,
        HAZARDOUS,
    }
}
