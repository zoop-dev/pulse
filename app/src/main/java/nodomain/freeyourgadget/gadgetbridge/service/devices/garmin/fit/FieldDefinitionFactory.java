package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionArray;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionBoolean;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionDayOfWeek;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrZoneHighBoundary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTemperature;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;

public class FieldDefinitionFactory {
    public static FieldDefinition create(int localNumber, int size, FIELD field, BaseType baseType, String name, int scale, int offset) {
        if (null == field) {
            return new FieldDefinition(localNumber, size, baseType, name, scale, offset);
        }
        return switch (field) {
            case ALARM -> new FieldDefinitionAlarm(localNumber, size, baseType, name);
            case ARRAY -> new FieldDefinitionArray(localNumber, size, baseType, name, scale, offset);
            case BOOLEAN -> new FieldDefinitionBoolean(localNumber, size, baseType, name);
            case DAY_OF_WEEK -> new FieldDefinitionDayOfWeek(localNumber, size, baseType, name);
            case EXERCISE_CATEGORY -> new FieldDefinitionExerciseCategory(localNumber, size, baseType, name);
            case ALARM_LABEL -> new FieldDefinitionAlarmLabel(localNumber, size, baseType, name);
            case FILE_TYPE -> new FieldDefinitionFileType(localNumber, size, baseType, name);
            case GOAL_SOURCE -> new FieldDefinitionGoalSource(localNumber, size, baseType, name);
            case GOAL_TYPE -> new FieldDefinitionGoalType(localNumber, size, baseType, name);
            case HRV_STATUS -> new FieldDefinitionHrvStatus(localNumber, size, baseType, name);
            case HR_TIME_IN_ZONE -> new FieldDefinitionHrTimeInZone(localNumber, size, baseType, name);
            case HR_ZONE_HIGH_BOUNDARY -> new FieldDefinitionHrZoneHighBoundary(localNumber, size, baseType, name);
            case MEASUREMENT_SYSTEM -> new FieldDefinitionMeasurementSystem(localNumber, size, baseType, name);
            case TEMPERATURE -> new FieldDefinitionTemperature(localNumber, size, baseType, name);
            case TIMESTAMP -> new FieldDefinitionTimestamp(localNumber, size, baseType, name);
            case WEATHER_CONDITION -> new FieldDefinitionWeatherCondition(localNumber, size, baseType, name);
            case LANGUAGE -> new FieldDefinitionLanguage(localNumber, size, baseType, name);
            case SLEEP_STAGE -> new FieldDefinitionSleepStage(localNumber, size, baseType, name);
            case WEATHER_AQI -> new FieldDefinitionWeatherAqi(localNumber, size, baseType, name);
            case COORDINATE -> new FieldDefinitionCoordinate(localNumber, size, baseType, name);
            case SWIM_STYLE -> new FieldDefinitionSwimStyle(localNumber, size, baseType, name);
            case LOCATION_SYMBOL -> new FieldDefinitionLocationSymbol(localNumber, size, baseType, name, scale, offset);
        };
    }

    public enum FIELD {
        ALARM,
        ARRAY,
        BOOLEAN,
        DAY_OF_WEEK,
        EXERCISE_CATEGORY,
        ALARM_LABEL,
        FILE_TYPE,
        GOAL_SOURCE,
        GOAL_TYPE,
        HRV_STATUS,
        HR_TIME_IN_ZONE,
        HR_ZONE_HIGH_BOUNDARY,
        MEASUREMENT_SYSTEM,
        TEMPERATURE,
        TIMESTAMP,
        WEATHER_CONDITION,
        LANGUAGE,
        SLEEP_STAGE,
        WEATHER_AQI,
        COORDINATE,
        SWIM_STYLE,
        LOCATION_SYMBOL
    }
}
