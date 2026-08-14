package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionBatteryStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoursePoint;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSwimStyle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWaterType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherReport;

public enum FitFieldType implements FieldDefinitionFactory.FieldDefinitionCreator {
    ALARM,
    ARRAY,
    BOOLEAN,
    DAY_OF_WEEK,
    EXERCISE_CATEGORY,
    FILE_TYPE,
    HR_TIME_IN_ZONE,
    HR_ZONE_HIGH_BOUNDARY,
    TEMPERATURE,
    TIMESTAMP,
    COORDINATE,
    AlarmLabel {
        @Override
        public FieldDefinitionAlarmLabel create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionAlarmLabel(localNumber, size, baseType, name, scale, offset);
        }
    },
    BatteryStatus {
        @Override
        public FieldDefinitionBatteryStatus create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionBatteryStatus(localNumber, size, baseType, name, scale, offset);
        }
    },
    CoursePoint {
        @Override
        public FieldDefinitionCoursePoint create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionCoursePoint(localNumber, size, baseType, name, scale, offset);
        }
    },
    GoalSource {
        @Override
        public FieldDefinitionGoalSource create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionGoalSource(localNumber, size, baseType, name, scale, offset);
        }
    },
    GoalType {
        @Override
        public FieldDefinitionGoalType create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionGoalType(localNumber, size, baseType, name, scale, offset);
        }
    },
    HrvStatus {
        @Override
        public FieldDefinitionHrvStatus create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionHrvStatus(localNumber, size, baseType, name, scale, offset);
        }
    },
    Language {
        @Override
        public FieldDefinitionLanguage create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionLanguage(localNumber, size, baseType, name, scale, offset);
        }
    },
    LocationSymbol {
        @Override
        public FieldDefinitionLocationSymbol create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionLocationSymbol(localNumber, size, baseType, name, scale, offset);
        }
    },
    MeasurementSystem {
        @Override
        public FieldDefinitionMeasurementSystem create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionMeasurementSystem(localNumber, size, baseType, name, scale, offset);
        }
    },
    SleepStage {
        @Override
        public FieldDefinitionSleepStage create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionSleepStage(localNumber, size, baseType, name, scale, offset);
        }
    },
    SwimStyle {
        @Override
        public FieldDefinitionSwimStyle create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionSwimStyle(localNumber, size, baseType, name, scale, offset);
        }
    },
    WaterType {
        @Override
        public FieldDefinitionWaterType create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionWaterType(localNumber, size, baseType, name, scale, offset);
        }
    },
    WeatherAqi {
        @Override
        public FieldDefinitionWeatherAqi create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionWeatherAqi(localNumber, size, baseType, name, scale, offset);
        }
    },
    WeatherCondition {
        @Override
        public FieldDefinitionWeatherCondition create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionWeatherCondition(localNumber, size, baseType, name, scale, offset);
        }
    },
    WeatherReport {
        @Override
        public FieldDefinitionWeatherReport create(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
            return new FieldDefinitionWeatherReport(localNumber, size, baseType, name, scale, offset);
        }
    }
}
