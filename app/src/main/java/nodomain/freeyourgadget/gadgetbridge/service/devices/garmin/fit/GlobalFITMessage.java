package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

/**
 * @noinspection ArraysAsListWithZeroOrOneArgument
 */
public class GlobalFITMessage {
    public static GlobalFITMessage FILE_ID = new GlobalFITMessage(0, "FILE_ID", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "type", FieldDefinitionFactory.FIELD.FILE_TYPE),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "product"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32Z, "serial_number"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "time_created", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "number"),
            new FieldDefinitionPrimitive(6, BaseType.UINT16, "manufacturer_partner"),
            new FieldDefinitionPrimitive(8, BaseType.STRING, 20, "product_name")
    ));

    public static GlobalFITMessage CAPABILITIES = new GlobalFITMessage(1, "CAPABILITIES", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8Z, "languages", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(1, BaseType.UINT8Z, "sports", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(21, BaseType.UINT32Z, "workouts_supported"),
            new FieldDefinitionPrimitive(23, BaseType.UINT32Z, "connectivity_supported")
    ));

    public static GlobalFITMessage DEVICE_SETTINGS = new GlobalFITMessage(2, "DEVICE_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "active_time_zone"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "utc_offset"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "time_offset", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "time_mode", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(5, BaseType.SINT8, "time_zone_offset", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "alarms_time", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "alarms_unk5", FieldDefinitionFactory.FIELD.ARRAY), // [5, 5]
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "backlight_mode"),
            new FieldDefinitionPrimitive(28, BaseType.ENUM, "alarms_enabled", FieldDefinitionFactory.FIELD.ARRAY), // [1,1]
            new FieldDefinitionPrimitive(36, BaseType.ENUM, "activity_tracker_enabled"),
            new FieldDefinitionPrimitive(39, BaseType.UINT32, "clock_time"),
            new FieldDefinitionPrimitive(40, BaseType.UINT16, "pages_enabled", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(46, BaseType.ENUM, "move_alert_enabled"),
            new FieldDefinitionPrimitive(47, BaseType.ENUM, "date_mode"),
            new FieldDefinitionPrimitive(55, BaseType.ENUM, "display_orientation"),
            new FieldDefinitionPrimitive(56, BaseType.ENUM, "mounting_side"),
            new FieldDefinitionPrimitive(57, BaseType.UINT16, "default_page", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(58, BaseType.UINT16, "autosync_min_steps"),
            new FieldDefinitionPrimitive(59, BaseType.UINT16, "autosync_min_time"),
            new FieldDefinitionPrimitive(80, BaseType.ENUM, "lactate_threshold_autodetect_enabled"),
            new FieldDefinitionPrimitive(86, BaseType.ENUM, "ble_auto_upload_enabled"),
            new FieldDefinitionPrimitive(89, BaseType.ENUM, "auto_sync_frequency"),
            new FieldDefinitionPrimitive(90, BaseType.UINT32, "auto_activity_detect"),
            new FieldDefinitionPrimitive(92, BaseType.UINT32Z, "alarms_repeat", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(94, BaseType.UINT8, "number_of_screens"),
            new FieldDefinitionPrimitive(95, BaseType.ENUM, "smart_notification_display_orientation"),
            new FieldDefinitionPrimitive(134, BaseType.ENUM, "tap_interface"),
            new FieldDefinitionPrimitive(174, BaseType.ENUM, "tap_sensitivity")
    ));

    public static GlobalFITMessage USER_PROFILE = new GlobalFITMessage(3, "USER_PROFILE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, 8, "friendly_name"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "gender"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "age"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "height"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "weight", 10, 0),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "language", FieldDefinitionFactory.FIELD.LANGUAGE),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "elev_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "weight_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "resting_heart_rate"),
            new FieldDefinitionPrimitive(9, BaseType.UINT8, "default_max_running_heart_rate"),
            new FieldDefinitionPrimitive(10, BaseType.UINT8, "default_max_biking_heart_rate"),
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "default_max_heart_rate"),
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "hr_setting"),
            new FieldDefinitionPrimitive(13, BaseType.ENUM, "speed_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(14, BaseType.ENUM, "dist_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(16, BaseType.ENUM, "power_setting"),
            new FieldDefinitionPrimitive(17, BaseType.ENUM, "activity_class"),
            new FieldDefinitionPrimitive(18, BaseType.ENUM, "position_setting"),
            new FieldDefinitionPrimitive(21, BaseType.ENUM, "temperature_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(22, BaseType.UINT16, "local_id"),
            new FieldDefinitionPrimitive(23, BaseType.BASE_TYPE_BYTE, 6, "global_id", FieldDefinitionFactory.FIELD.ARRAY, 1, 0),
            new FieldDefinitionPrimitive(24, BaseType.UINT8, "year_of_birth", 1, -1900),
            new FieldDefinitionPrimitive(28, BaseType.UINT32, "wake_time"),
            new FieldDefinitionPrimitive(29, BaseType.UINT32, "sleep_time"),
            new FieldDefinitionPrimitive(30, BaseType.ENUM, "height_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(31, BaseType.UINT16, "user_running_step_length"),
            new FieldDefinitionPrimitive(32, BaseType.UINT16, "user_walking_step_length"),
            new FieldDefinitionPrimitive(37, BaseType.UINT16, "ltspeed", 10, 0), // km/h
            new FieldDefinitionPrimitive(41, BaseType.UINT32, "time_last_lthr_update"),
            new FieldDefinitionPrimitive(47, BaseType.ENUM, "depth_setting", FieldDefinitionFactory.FIELD.MEASUREMENT_SYSTEM),
            new FieldDefinitionPrimitive(49, BaseType.UINT32, "dive_count"),
            new FieldDefinitionPrimitive(62, BaseType.ENUM, "gender_x"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage HRM_PROFILE = new GlobalFITMessage(4, "HRM_PROFILE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(1, BaseType.UINT16Z, "hrm_ant_id"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "log_hrv"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8Z, "hrm_ant_id_trans_type"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SDM_PROFILE = new GlobalFITMessage(5, "SDM_PROFILE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(1, BaseType.UINT16Z, "sdm_ant_id"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "sdm_cal_factor", 10, 0), // %
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "odometer", 100, 0), // m
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "speed_source"),
            new FieldDefinitionPrimitive(5, BaseType.UINT8Z, "sdm_ant_id_trans_type"),
            new FieldDefinitionPrimitive(7, BaseType.UINT8, "odometer_rollover"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage BIKE_PROFILE = new GlobalFITMessage(6, "BIKE_PROFILE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "odometer", 100, 0), // m
            new FieldDefinitionPrimitive(4, BaseType.UINT16Z, "bike_spd_ant_id"),
            new FieldDefinitionPrimitive(5, BaseType.UINT16Z, "bike_cad_ant_id"),
            new FieldDefinitionPrimitive(6, BaseType.UINT16Z, "bike_spdcad_ant_id"),
            new FieldDefinitionPrimitive(7, BaseType.UINT16Z, "bike_power_ant_id"),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "custom_wheelsize", 1000, 0), // m
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "auto_wheelsize", 1000, 0), // m
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "bike_weight", 10, 0), // kg
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "power_cal_factor", 10, 0), // %
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "auto_wheel_cal", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(13, BaseType.ENUM, "auto_power_zero", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(14, BaseType.UINT8, "id"),
            new FieldDefinitionPrimitive(15, BaseType.ENUM, "spd_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(16, BaseType.ENUM, "cad_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(17, BaseType.ENUM, "spdcad_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(18, BaseType.ENUM, "power_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(19, BaseType.UINT8, "crank_length", 2, -100), // mm
            new FieldDefinitionPrimitive(20, BaseType.ENUM, "enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(21, BaseType.UINT8Z, "bike_spd_ant_id_trans_type"),
            new FieldDefinitionPrimitive(22, BaseType.UINT8Z, "bike_cad_ant_id_trans_type"),
            new FieldDefinitionPrimitive(23, BaseType.UINT8Z, "bike_spdcad_ant_id_trans_type"),
            new FieldDefinitionPrimitive(24, BaseType.UINT8Z, "bike_power_ant_id_trans_type"),
            new FieldDefinitionPrimitive(37, BaseType.UINT8, "odometer_rollover"),
            new FieldDefinitionPrimitive(38, BaseType.UINT8Z, "front_gear_num"),
            new FieldDefinitionPrimitive(39, BaseType.UINT8Z, "front_gear", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(40, BaseType.UINT8Z, "rear_gear_num"),
            new FieldDefinitionPrimitive(41, BaseType.UINT8Z, "rear_gear", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(44, BaseType.ENUM, "shimano_di2_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage ZONES_TARGET = new GlobalFITMessage(7, "ZONES_TARGET", Arrays.asList(
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "functional_threshold_power"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "max_heart_rate"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "threshold_heart_rate"),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "hr_calc_type"), //1=percent_max_hr
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "pwr_calc_type") //1=percent_ftp
    ));

    public static GlobalFITMessage HR_ZONE = new GlobalFITMessage(8, "HR_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "high_bpm"),
            new FieldDefinitionPrimitive(2, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage POWER_ZONE = new GlobalFITMessage(9, "POWER_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "high_value"), // watt
            new FieldDefinitionPrimitive(2, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage MET_ZONE = new GlobalFITMessage(10, "MET_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "high_bpm"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "calories", 10, 0), // kcal/min
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "fat_calories", 10, 0), // kcal/min
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SPORT = new GlobalFITMessage(12, "SPORT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(3, BaseType.STRING, 24, "name")
    ));

    public static GlobalFITMessage TRAINING_SETTINGS = new GlobalFITMessage(13, "TRAINING_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(31, BaseType.UINT32, "target_distance", 100, 0), // m
            new FieldDefinitionPrimitive(32, BaseType.UINT16, "target_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(33, BaseType.UINT32, "target_time", 1, 0), // s
            new FieldDefinitionPrimitive(153, BaseType.UINT32, "precise_target_speed", 1000000, 0) // m/s
    ));

    public static GlobalFITMessage GOALS = new GlobalFITMessage(15, "GOALS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "start_date"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "end_date"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "type", FieldDefinitionFactory.FIELD.GOAL_TYPE),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "value"),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "repeat"),
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "target_value"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "recurrence"),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "recurrence_value"),
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "enabled"),
            new FieldDefinitionPrimitive(11, BaseType.ENUM, "source", FieldDefinitionFactory.FIELD.GOAL_SOURCE),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SESSION = new GlobalFITMessage(18, "SESSION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "event"), // 8 session 9 lap
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "event_type"), // 1 stop
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(3, BaseType.SINT32, "start_latitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(4, BaseType.SINT32, "start_longitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "total_elapsed_time"), // with pauses
            new FieldDefinitionPrimitive(8, BaseType.UINT32, "total_timer_time"), // no pauses
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "total_distance"), // cm
            new FieldDefinitionPrimitive(10, BaseType.UINT32, "total_cycles"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "total_calories"),
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "total_fat_calories"), // kcal
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(15, BaseType.UINT16, "max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(16, BaseType.UINT8, "average_heart_rate"),
            new FieldDefinitionPrimitive(17, BaseType.UINT8, "max_heart_rate"),
            new FieldDefinitionPrimitive(18, BaseType.UINT8, "avg_cadence"), // rpm
            new FieldDefinitionPrimitive(19, BaseType.UINT8, "max_cadence"), // rpm
            new FieldDefinitionPrimitive(20, BaseType.UINT16, "avg_power"), // watt
            new FieldDefinitionPrimitive(21, BaseType.UINT16, "max_power"), // watt
            new FieldDefinitionPrimitive(22, BaseType.UINT16, "total_ascent"), // m
            new FieldDefinitionPrimitive(23, BaseType.UINT16, "total_descent"), // m
            new FieldDefinitionPrimitive(24, BaseType.UINT8, "total_training_effect", 10, 0),
            new FieldDefinitionPrimitive(25, BaseType.UINT16, "first_lap_index"),
            new FieldDefinitionPrimitive(26, BaseType.UINT16, "num_laps"),
            new FieldDefinitionPrimitive(27, BaseType.UINT8, "event_group"),
            new FieldDefinitionPrimitive(28, BaseType.ENUM, "trigger"),
            new FieldDefinitionPrimitive(29, BaseType.SINT32, "nec_latitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(30, BaseType.SINT32, "nec_longitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(31, BaseType.SINT32, "swc_latitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(32, BaseType.SINT32, "swc_longitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(33, BaseType.UINT16, "num_lengths"),
            new FieldDefinitionPrimitive(34, BaseType.UINT16, "normalized_power"), // watt
            new FieldDefinitionPrimitive(35, BaseType.UINT16, "training_stress_score", 10, 0), // tss
            new FieldDefinitionPrimitive(36, BaseType.UINT16, "intensity_factor", 1000, 0), // if
            new FieldDefinitionPrimitive(37, BaseType.UINT16, "left_right_balance"),
            new FieldDefinitionPrimitive(38, BaseType.SINT32, "end_latitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(39, BaseType.SINT32, "end_longitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(41, BaseType.UINT32, "avg_stroke_count"), // strokes/lap
            new FieldDefinitionPrimitive(42, BaseType.UINT16, "avg_stroke_distance", 100, 0), // m
            new FieldDefinitionPrimitive(43, BaseType.ENUM, "swim_stroke"),
            new FieldDefinitionPrimitive(44, BaseType.UINT16, "pool_length", 100, 0), // m
            new FieldDefinitionPrimitive(45, BaseType.UINT16, "threshold_power"), // watt
            new FieldDefinitionPrimitive(46, BaseType.ENUM, "pool_length_unit"),
            new FieldDefinitionPrimitive(47, BaseType.UINT16, "num_active_lengths"),
            new FieldDefinitionPrimitive(48, BaseType.UINT32, "total_work"), // joule
            new FieldDefinitionPrimitive(49, BaseType.UINT16, "avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(50, BaseType.UINT16, "max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(51, BaseType.UINT8, "gps_accuracy"), // m
            new FieldDefinitionPrimitive(52, BaseType.SINT16, "avg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(53, BaseType.SINT16, "avg_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(54, BaseType.SINT16, "avg_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(55, BaseType.SINT16, "max_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(56, BaseType.SINT16, "max_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(57, BaseType.SINT8, "avg_temperature"), // °C
            new FieldDefinitionPrimitive(58, BaseType.SINT8, "max_temperature"), // °C
            new FieldDefinitionPrimitive(59, BaseType.UINT32, "total_moving_time", 1000, 0), // s
            new FieldDefinitionPrimitive(60, BaseType.SINT16, "avg_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(61, BaseType.SINT16, "avg_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(62, BaseType.SINT16, "max_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(63, BaseType.SINT16, "max_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(64, BaseType.UINT8, "min_heart_rate"), // bpm
            new FieldDefinitionPrimitive(65, BaseType.UINT32, "time_in_hr_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(66, BaseType.UINT32, "time_in_speed_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(67, BaseType.UINT32, "time_in_cadence_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(68, BaseType.UINT32, "time_in_power_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(69, BaseType.UINT32, "avg_lap_time", 1000, 0), // s
            new FieldDefinitionPrimitive(70, BaseType.UINT16, "best_lap_index"),
            new FieldDefinitionPrimitive(71, BaseType.UINT16, "min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(79, BaseType.UINT16, "avg_swim_cadence", 10, 0), // rpm
            new FieldDefinitionPrimitive(80, BaseType.UINT16, "avg_swolf"),
            new FieldDefinitionPrimitive(82, BaseType.UINT16, "player_score"),
            new FieldDefinitionPrimitive(83, BaseType.UINT16, "opponent_score"),
            new FieldDefinitionPrimitive(84, BaseType.STRING, "opponent_name"),
            new FieldDefinitionPrimitive(85, BaseType.UINT16, "stroke_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(86, BaseType.UINT16, "zone_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(87, BaseType.UINT16, "max_ball_speed", 100, 0), // m/s
            new FieldDefinitionPrimitive(88, BaseType.UINT16, "avg_ball_speed", 100, 0), // m/s
            new FieldDefinitionPrimitive(89, BaseType.UINT16, "avg_vertical_oscillation", 10, 0), // mm
            new FieldDefinitionPrimitive(90, BaseType.UINT16, "avg_stance_time_percent", 100, 0), // %
            new FieldDefinitionPrimitive(91, BaseType.UINT16, "avg_stance_time", 10, 0), // ms
            new FieldDefinitionPrimitive(92, BaseType.UINT8, "avg_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(93, BaseType.UINT8, "max_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(94, BaseType.UINT8, "total_fractional_cycles", 128, 0), // cycles
            new FieldDefinitionPrimitive(95, BaseType.UINT16, "avg_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(96, BaseType.UINT16, "min_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(97, BaseType.UINT16, "max_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(98, BaseType.UINT16, "avg_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(99, BaseType.UINT16, "min_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(100, BaseType.UINT16, "max_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(101, BaseType.UINT8, "avg_left_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(102, BaseType.UINT8, "avg_right_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(103, BaseType.UINT8, "avg_left_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(104, BaseType.UINT8, "avg_right_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(105, BaseType.UINT8, "avg_combined_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(107, BaseType.UINT16, "front_shifts"),
            new FieldDefinitionPrimitive(108, BaseType.UINT16, "rear_shifts"),
            new FieldDefinitionPrimitive(110, BaseType.STRING, 64, "sport_profile_name"),
            new FieldDefinitionPrimitive(111, BaseType.UINT8, "sport_index"),
            new FieldDefinitionPrimitive(112, BaseType.UINT32, "stand_time"), // s
            new FieldDefinitionPrimitive(113, BaseType.UINT16, "stand_count"),
            new FieldDefinitionPrimitive(114, BaseType.SINT8, "avg_left_pco"), // mm
            new FieldDefinitionPrimitive(115, BaseType.SINT8, "avg_right_pco"), // mm
            new FieldDefinitionPrimitive(116, BaseType.UINT8, "avg_left_power_phase", FieldDefinitionFactory.FIELD.ARRAY), // degrees (start angle, end angle, arc length, center)
            new FieldDefinitionPrimitive(117, BaseType.UINT8, "avg_left_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY), // degrees (start angle, end angle, arc length, center)
            new FieldDefinitionPrimitive(118, BaseType.UINT8, "avg_right_power_phase", FieldDefinitionFactory.FIELD.ARRAY), // degrees (start angle, end angle, arc length, center)
            new FieldDefinitionPrimitive(119, BaseType.UINT8, "avg_right_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY), // degrees (start angle, end angle, arc length, center)
            new FieldDefinitionPrimitive(120, BaseType.UINT16, "avg_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt, 2 items (seat / stand)
            new FieldDefinitionPrimitive(121, BaseType.UINT16, "max_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt, 2 items (seat / stand)
            new FieldDefinitionPrimitive(122, BaseType.UINT8, "avg_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // watt, 2 items (seat / stand)
            new FieldDefinitionPrimitive(123, BaseType.UINT8, "max_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // watt, 2 items (seat / stand)
            new FieldDefinitionPrimitive(124, BaseType.UINT32, "enhanced_avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(125, BaseType.UINT32, "enhanced_max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(126, BaseType.UINT32, "enhanced_avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(127, BaseType.UINT32, "enhanced_min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(128, BaseType.UINT32, "enhanced_max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(129, BaseType.UINT16, "avg_lev_motor_power"), // watt
            new FieldDefinitionPrimitive(130, BaseType.UINT16, "max_lev_motor_power"), // watt
            new FieldDefinitionPrimitive(131, BaseType.UINT8, "lev_battery_consumption", 2, 0), // %
            new FieldDefinitionPrimitive(132, BaseType.UINT16, "avg_vertical_ratio", 100, 0), // %
            new FieldDefinitionPrimitive(133, BaseType.UINT16, "avg_stance_time_balance", 100, 0), // %
            new FieldDefinitionPrimitive(134, BaseType.UINT16, "avg_step_length", 10, 0), // mm
            new FieldDefinitionPrimitive(137, BaseType.UINT8, "total_anaerobic_training_effect", 10, 0),
            new FieldDefinitionPrimitive(139, BaseType.UINT16, "avg_vam", 1000, 0), // m/s
            new FieldDefinitionPrimitive(140, BaseType.UINT32, "avg_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(141, BaseType.UINT32, "max_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(142, BaseType.UINT32, "surface_interval"), // s
            new FieldDefinitionPrimitive(143, BaseType.UINT8, "start_cns"), // %
            new FieldDefinitionPrimitive(144, BaseType.UINT8, "end_cns"), // %
            new FieldDefinitionPrimitive(145, BaseType.UINT16, "start_n2"), // %
            new FieldDefinitionPrimitive(146, BaseType.UINT16, "end_n2"), // %
            new FieldDefinitionPrimitive(147, BaseType.UINT8, "avg_respiration_rate"),
            new FieldDefinitionPrimitive(148, BaseType.UINT8, "max_respiration_rate"),
            new FieldDefinitionPrimitive(149, BaseType.UINT8, "min_respiration_rate"),
            new FieldDefinitionPrimitive(150, BaseType.SINT8, "min_temperature"), // C
            new FieldDefinitionPrimitive(155, BaseType.UINT16, "o2_toxicity"), // OTUs
            new FieldDefinitionPrimitive(156, BaseType.UINT32, "dive_number"),
            new FieldDefinitionPrimitive(168, BaseType.SINT32, "training_load_peak", 65536, 0),
            new FieldDefinitionPrimitive(169, BaseType.UINT16, "enhanced_avg_respiration_rate", 100, 0), // breaths/min
            new FieldDefinitionPrimitive(170, BaseType.UINT16, "enhanced_max_respiration_rate", 100, 0), // breaths/min
            new FieldDefinitionPrimitive(178, BaseType.UINT16, "estimated_sweat_loss"), // ml
            new FieldDefinitionPrimitive(180, BaseType.UINT16, "enhanced_min_respiration_rate", 100, 0), // breaths/min
            new FieldDefinitionPrimitive(181, BaseType.FLOAT32, "total_grit"),
            new FieldDefinitionPrimitive(182, BaseType.FLOAT32, "total_flow"),
            new FieldDefinitionPrimitive(183, BaseType.UINT16, "jump_count"),
            new FieldDefinitionPrimitive(186, BaseType.FLOAT32, "avg_grit"),
            new FieldDefinitionPrimitive(187, BaseType.FLOAT32, "avg_flow"),
            new FieldDefinitionPrimitive(188, BaseType.ENUM, "primary_benefit"), // 1 recovery
            new FieldDefinitionPrimitive(192, BaseType.UINT8, "workout_feel"), // 0, bad 1 - 100 good
            new FieldDefinitionPrimitive(193, BaseType.UINT8, "workout_rpe"), // 0, very weak effort 1 - 10 maximum effort
            new FieldDefinitionPrimitive(194, BaseType.UINT8, "avg_spo2"),
            new FieldDefinitionPrimitive(195, BaseType.UINT8, "avg_stress"),
            new FieldDefinitionPrimitive(196, BaseType.UINT16, "resting_calories"), // kcal
            new FieldDefinitionPrimitive(197, BaseType.UINT8, "hrv_sdrr"), // ms
            new FieldDefinitionPrimitive(198, BaseType.UINT8, "hrv_rmssd"), // ms
            new FieldDefinitionPrimitive(199, BaseType.UINT8, "total_fractional_ascent", 100, 0), // m
            new FieldDefinitionPrimitive(200, BaseType.UINT8, "total_fractional_descent", 100, 0), // m
            new FieldDefinitionPrimitive(205, BaseType.UINT8, "beginning_potential"),
            new FieldDefinitionPrimitive(206, BaseType.UINT8, "ending_potential"),
            new FieldDefinitionPrimitive(207, BaseType.UINT8, "min_stamina"),
            new FieldDefinitionPrimitive(208, BaseType.UINT16, "avg_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(209, BaseType.UINT16, "min_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(210, BaseType.UINT16, "max_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(222, BaseType.UINT16, "step_speed_loss", 100, 0),
            new FieldDefinitionPrimitive(223, BaseType.UINT16, "step_speed_loss_percentage", 100, 0),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage LAP = new GlobalFITMessage(19, "LAP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "event"), // 9 lap
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "event_type"), // 1 stop
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(3, BaseType.SINT32, "start_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(4, BaseType.SINT32, "start_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(5, BaseType.SINT32, "end_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(6, BaseType.SINT32, "end_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "total_elapsed_time", 1000, 0), // s
            new FieldDefinitionPrimitive(8, BaseType.UINT32, "total_timer_time", 1000, 0), // s
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "total_distance", 100, 0), // m
            new FieldDefinitionPrimitive(10, BaseType.UINT32, "total_cycles"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "total_calories"),
            new FieldDefinitionPrimitive(12, BaseType.UINT16, "total_fat_calories"),
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(15, BaseType.UINT8, "avg_heart_rate"),
            new FieldDefinitionPrimitive(16, BaseType.UINT8, "max_heart_rate"),
            new FieldDefinitionPrimitive(17, BaseType.UINT8, "avg_cadence"), // rpm
            new FieldDefinitionPrimitive(18, BaseType.UINT8, "max_cadence"), // rpm
            new FieldDefinitionPrimitive(19, BaseType.UINT16, "avg_power"), // watt
            new FieldDefinitionPrimitive(20, BaseType.UINT16, "max_power"), // watt
            new FieldDefinitionPrimitive(21, BaseType.UINT16, "total_ascent"), // m
            new FieldDefinitionPrimitive(22, BaseType.UINT16, "total_descent"), // m
            new FieldDefinitionPrimitive(23, BaseType.ENUM, "intensity"),
            new FieldDefinitionPrimitive(24, BaseType.ENUM, "lap_trigger"), // 0 manual
            new FieldDefinitionPrimitive(25, BaseType.ENUM, "sport"), // 5 swimming
            new FieldDefinitionPrimitive(26, BaseType.UINT8, "event_group"),
            new FieldDefinitionPrimitive(32, BaseType.UINT16, "num_lengths"),
            new FieldDefinitionPrimitive(33, BaseType.UINT16, "normalized_power"), // watt
            new FieldDefinitionPrimitive(34, BaseType.UINT16, "left_right_balance"),
            new FieldDefinitionPrimitive(35, BaseType.UINT16, "first_length_index"),
            new FieldDefinitionPrimitive(37, BaseType.UINT16, "avg_stroke_distance"),
            new FieldDefinitionPrimitive(38, BaseType.ENUM, "swim_style", FieldDefinitionFactory.FIELD.SWIM_STYLE),
            new FieldDefinitionPrimitive(39, BaseType.ENUM, "sub_sport"), // 17 = lap swimming
            new FieldDefinitionPrimitive(40, BaseType.UINT16, "num_active_lengths"),
            new FieldDefinitionPrimitive(41, BaseType.UINT32, "total_work"), // Joule
            new FieldDefinitionPrimitive(42, BaseType.UINT16, "avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(43, BaseType.UINT16, "max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(44, BaseType.UINT8, "gps_accuracy"), // m
            new FieldDefinitionPrimitive(45, BaseType.SINT16, "avg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(46, BaseType.SINT16, "avg_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(47, BaseType.SINT16, "avg_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(48, BaseType.SINT16, "max_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(49, BaseType.SINT16, "max_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(50, BaseType.SINT8, "avg_temperature"), // C
            new FieldDefinitionPrimitive(51, BaseType.SINT8, "max_temperature"), // C
            new FieldDefinitionPrimitive(52, BaseType.UINT32, "total_moving_time", 1000, 0), // s
            new FieldDefinitionPrimitive(53, BaseType.SINT16, "avg_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(54, BaseType.SINT16, "avg_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(55, BaseType.SINT16, "max_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(56, BaseType.SINT16, "max_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(57, BaseType.UINT32, "time_in_hr_zone", FieldDefinitionFactory.FIELD.ARRAY,1000, 0), // s
            new FieldDefinitionPrimitive(58, BaseType.UINT32, "time_in_speed_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(59, BaseType.UINT32, "time_in_cadence_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(60, BaseType.UINT32, "time_in_power_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(61, BaseType.UINT16, "repetition_num"),
            new FieldDefinitionPrimitive(62, BaseType.UINT16, "min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(63, BaseType.UINT8, "min_heart_rate"),
            new FieldDefinitionPrimitive(71, BaseType.UINT16, "wkt_step_index"),
            new FieldDefinitionPrimitive(73, BaseType.UINT16, "avg_swolf"),
            new FieldDefinitionPrimitive(74, BaseType.UINT16, "opponent_score"),
            new FieldDefinitionPrimitive(75, BaseType.UINT16, "stroke_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(76, BaseType.UINT16, "zone_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(77, BaseType.UINT16, "avg_vertical_oscillation", 10, 0), // mm
            new FieldDefinitionPrimitive(78, BaseType.UINT16, "avg_stance_time_percent", 100, 0), // %
            new FieldDefinitionPrimitive(79, BaseType.UINT16, "avg_stance_time", 10, 0), // ms
            new FieldDefinitionPrimitive(80, BaseType.UINT8, "avg_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(81, BaseType.UINT8, "max_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(82, BaseType.UINT8, "total_fractional_cycles", 128, 0), // cycles
            new FieldDefinitionPrimitive(83, BaseType.UINT16, "player_score"),
            new FieldDefinitionPrimitive(84, BaseType.UINT16, "avg_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(85, BaseType.UINT16, "min_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(86, BaseType.UINT16, "max_total_hemoglobin_conc", FieldDefinitionFactory.FIELD.ARRAY,100, 0), // g/dL
            new FieldDefinitionPrimitive(87, BaseType.UINT16, "avg_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(88, BaseType.UINT16, "min_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(89, BaseType.UINT16, "max_saturated_hemoglobin_percent", FieldDefinitionFactory.FIELD.ARRAY,10, 0), // %
            new FieldDefinitionPrimitive(91, BaseType.UINT8, "avg_left_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(92, BaseType.UINT8, "avg_right_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(93, BaseType.UINT8, "avg_left_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(94, BaseType.UINT8, "avg_right_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(95, BaseType.UINT8, "avg_combined_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(98, BaseType.UINT32, "time_standing", 1000, 0), // s
            new FieldDefinitionPrimitive(99, BaseType.UINT16, "stand_count"),
            new FieldDefinitionPrimitive(100, BaseType.SINT8, "avg_left_pco"), // mm
            new FieldDefinitionPrimitive(101, BaseType.SINT8, "avg_right_pco"), // mm
            new FieldDefinitionPrimitive(102, BaseType.UINT8, "avg_left_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(103, BaseType.UINT8, "avg_left_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(104, BaseType.UINT8, "avg_right_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(105, BaseType.UINT8, "avg_right_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(106, BaseType.UINT16, "avg_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt
            new FieldDefinitionPrimitive(107, BaseType.UINT16, "max_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt
            new FieldDefinitionPrimitive(108, BaseType.UINT8, "avg_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // rpm
            new FieldDefinitionPrimitive(109, BaseType.UINT8, "max_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // rpm
            new FieldDefinitionPrimitive(110, BaseType.UINT32, "enhanced_avg_speed", 100, 0), // m/s
            new FieldDefinitionPrimitive(111, BaseType.UINT32, "enhanced_max_speed", 100, 0), // m/s
            new FieldDefinitionPrimitive(112, BaseType.UINT32, "enhanced_avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(113, BaseType.UINT32, "enhanced_min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(114, BaseType.UINT32, "enhanced_max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(115, BaseType.UINT16, "avg_lev_motor_power"), // watt
            new FieldDefinitionPrimitive(116, BaseType.UINT16, "max_lev_motor_power"), // watt
            new FieldDefinitionPrimitive(117, BaseType.UINT8, "lev_battery_consumption", 2, 0), // %
            new FieldDefinitionPrimitive(118, BaseType.UINT16, "avg_vertical_ratio", 100, 0), // %
            new FieldDefinitionPrimitive(119, BaseType.UINT16, "avg_stance_time_balance", 100, 0), // %
            new FieldDefinitionPrimitive(120, BaseType.UINT16, "avg_step_length", 10, 0), // mm
            new FieldDefinitionPrimitive(121, BaseType.UINT16, "avg_vam", 1000, 0), // m/s
            new FieldDefinitionPrimitive(122, BaseType.UINT32, "avg_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(123, BaseType.UINT32, "max_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(124, BaseType.SINT8, "min_temperature"), // °C
            new FieldDefinitionPrimitive(136, BaseType.UINT16, "enhanced_avg_respiration_rate", 100, 0), // breath / min
            new FieldDefinitionPrimitive(137, BaseType.UINT16, "enhanced_max_respiration_rate", 100, 0), // breath / min
            new FieldDefinitionPrimitive(147, BaseType.UINT8, "avg_respiration_rate"),
            new FieldDefinitionPrimitive(148, BaseType.UINT8, "max_respiration_rate"),
            new FieldDefinitionPrimitive(149, BaseType.FLOAT32, "total_grit"),
            new FieldDefinitionPrimitive(150, BaseType.FLOAT32, "total_flow"),
            new FieldDefinitionPrimitive(151, BaseType.UINT16, "jump_count"),
            new FieldDefinitionPrimitive(153, BaseType.FLOAT32, "avg_grit"),
            new FieldDefinitionPrimitive(154, BaseType.FLOAT32, "avg_flow"),
            new FieldDefinitionPrimitive(156, BaseType.UINT8, "total_fractional_ascent", 100, 0), // m
            new FieldDefinitionPrimitive(157, BaseType.UINT8, "total_fractional_descent", 100, 0), // m
            new FieldDefinitionPrimitive(158, BaseType.UINT16, "avg_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(159, BaseType.UINT16, "min_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(160, BaseType.UINT16, "max_core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage RECORD = new GlobalFITMessage(20, "RECORD", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.SINT32, "latitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "longitude", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "altitude", 5, 500), // m
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "heart_rate"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "cadence"), // rpm
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "distance", 100, 0), // m
            new FieldDefinitionPrimitive(6, BaseType.UINT16, "speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "power"), // watt
            new FieldDefinitionPrimitive(8, BaseType.BASE_TYPE_BYTE, 3, "compressed_speed_distance", FieldDefinitionFactory.FIELD.ARRAY, 1, 0), // 12 bit: speed|distance with scale 100|16
            new FieldDefinitionPrimitive(9, BaseType.SINT16,"grade", 100, 0), // %
            new FieldDefinitionPrimitive(10, BaseType.UINT8,"resistance"),
            new FieldDefinitionPrimitive(11, BaseType.SINT32,"time_from_course", 1000, 0), // s
            new FieldDefinitionPrimitive(12, BaseType.UINT8,"cycle_length", 100, 0), // m
            new FieldDefinitionPrimitive(13, BaseType.SINT8, "temperature", 1, 0), // C
            new FieldDefinitionPrimitive(17, BaseType.UINT8, "speed_1s", FieldDefinitionFactory.FIELD.ARRAY, 16, 0), // m/s
            new FieldDefinitionPrimitive(18, BaseType.UINT8, "cycles"),
            new FieldDefinitionPrimitive(19, BaseType.UINT32, "total_cycles"),
            new FieldDefinitionPrimitive(28, BaseType.UINT16, "compressed_accumulated_power"), // watt
            new FieldDefinitionPrimitive(29, BaseType.UINT32, "accumulated_power"), // watt
            new FieldDefinitionPrimitive(30, BaseType.UINT8, "left_right_balance"),
            new FieldDefinitionPrimitive(31, BaseType.UINT8, "gps_accuracy"), // m
            new FieldDefinitionPrimitive(32, BaseType.SINT16, "vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(33, BaseType.UINT16, "calories"), // kcal
            new FieldDefinitionPrimitive(39, BaseType.UINT16, "oscillation", 10, 0), // mm
            new FieldDefinitionPrimitive(40, BaseType.UINT16, "stance_time_percent", 100, 0), // %
            new FieldDefinitionPrimitive(41, BaseType.UINT16, "stance_time", 10, 0), // ms
            new FieldDefinitionPrimitive(42, BaseType.ENUM, "activity"),
            new FieldDefinitionPrimitive(43, BaseType.UINT8, "left_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(44, BaseType.UINT8, "right_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(45, BaseType.UINT8, "left_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(46, BaseType.UINT8, "right_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(47, BaseType.UINT8, "combined_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(48, BaseType.UINT8, "time128", 128, 0), // s
            new FieldDefinitionPrimitive(49, BaseType.ENUM, "stroke_type"),
            new FieldDefinitionPrimitive(50, BaseType.UINT8, "zone"),
            new FieldDefinitionPrimitive(51, BaseType.UINT16, "ball_speed", 100, 0), // m/s
            new FieldDefinitionPrimitive(52, BaseType.UINT16, "cadence256", 256, 0), // RPM
            new FieldDefinitionPrimitive(53, BaseType.UINT8, "fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(54, BaseType.UINT16, "avg_total_hemoglobin_conc", 100, 0), // g/dL
            new FieldDefinitionPrimitive(55, BaseType.UINT16, "min_total_hemoglobin_conc", 100, 0), // g/dL
            new FieldDefinitionPrimitive(56, BaseType.UINT16, "max_total_hemoglobin_conc", 100, 0), // g/dL
            new FieldDefinitionPrimitive(57, BaseType.UINT16, "avg_saturated_hemoglobin_percent", 10, 0), // %
            new FieldDefinitionPrimitive(58, BaseType.UINT16, "min_saturated_hemoglobin_percent", 10, 0), // %
            new FieldDefinitionPrimitive(59, BaseType.UINT16, "max_saturated_hemoglobin_percent", 10, 0), // %
            new FieldDefinitionPrimitive(62, BaseType.UINT8, "device_index"),
            new FieldDefinitionPrimitive(67, BaseType.SINT8, "left_pco"), // mm
            new FieldDefinitionPrimitive(68, BaseType.SINT8, "right_pco"), // mm
            new FieldDefinitionPrimitive(69, BaseType.UINT8, "left_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(70, BaseType.UINT8, "left_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(71, BaseType.UINT8, "right_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(72, BaseType.UINT8, "right_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(73, BaseType.UINT32, "enhanced_speed", 1000, 0), // mm/s
            new FieldDefinitionPrimitive(78, BaseType.UINT32, "enhanced_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(81, BaseType.UINT8, "battery_soc", 2, 0), // %
            new FieldDefinitionPrimitive(82, BaseType.UINT16, "motor_power"), // watt
            new FieldDefinitionPrimitive(83, BaseType.UINT16, "vertical_ratio", 100, 0), // %
            new FieldDefinitionPrimitive(84, BaseType.UINT16, "stance_time_balance", 100, 0), // %
            new FieldDefinitionPrimitive(85, BaseType.UINT16, "step_length", 10, 0), // mm
            new FieldDefinitionPrimitive(87, BaseType.UINT16, "cycle_length16", 100, 0), // m
            new FieldDefinitionPrimitive(91, BaseType.UINT32, "absolute_pressure", 1, 0), // Pa
            new FieldDefinitionPrimitive(92, BaseType.UINT32, "depth", 1000, 0), // m
            new FieldDefinitionPrimitive(93, BaseType.UINT32, "next_stop_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(94, BaseType.UINT32, "next_stop_time", 1, 0), // s
            new FieldDefinitionPrimitive(95, BaseType.UINT32, "time_to_surface", 1, 0), // s
            new FieldDefinitionPrimitive(96, BaseType.UINT32, "ndl_time", 1, 0), // s
            new FieldDefinitionPrimitive(97, BaseType.UINT8, "cns_load", 1, 0), // %
            new FieldDefinitionPrimitive(98, BaseType.UINT16, "n2_load", 1, 0), // %
            new FieldDefinitionPrimitive(99, BaseType.UINT8, "respiration_rate", 1, 0), // s
            new FieldDefinitionPrimitive(108, BaseType.UINT16, "enhanced_respiration_rate", 100, 0), // breaths / min
            new FieldDefinitionPrimitive(114, BaseType.FLOAT32, "grit"),
            new FieldDefinitionPrimitive(115, BaseType.FLOAT32, "flow"),
            new FieldDefinitionPrimitive(116, BaseType.UINT16, "current_stress", 100, 0),
            new FieldDefinitionPrimitive(117, BaseType.UINT16, "ebike_travel_rang"), // km
            new FieldDefinitionPrimitive(118, BaseType.UINT8, "ebike_battery_level"), // %
            new FieldDefinitionPrimitive(119, BaseType.UINT8, "ebike_assist_mode"),
            new FieldDefinitionPrimitive(120, BaseType.UINT8, "ebike_assist_level_percent"), // %
            new FieldDefinitionPrimitive(123, BaseType.UINT32, "air_time_remaining"), // s
            new FieldDefinitionPrimitive(124, BaseType.UINT16, "pressure_sac", 100, 0), // bar / min
            new FieldDefinitionPrimitive(125, BaseType.UINT16, "volume_sac", 100, 0), // liter / min
            new FieldDefinitionPrimitive(126, BaseType.UINT16, "rmv", 100, 0), // liter / min
            new FieldDefinitionPrimitive(127, BaseType.SINT32, "ascent_rate", 1000, 0), // m/s
            new FieldDefinitionPrimitive(129, BaseType.UINT8, "po2", 100, 0), // %
            new FieldDefinitionPrimitive(136, BaseType.UINT8, "wrist_heart_rate"),
            new FieldDefinitionPrimitive(139, BaseType.UINT16, "core_temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(143, BaseType.UINT8, "body_battery"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage EVENT = new GlobalFITMessage(21, "EVENT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "event"), // 0 timer, 74 sleep
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "event_type"), // sleep: 0 start 1 stop, timer: 0 start 4 stop all
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "data16"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "data"), // in sleep they're timestamps in garmin epoch? in timer, 0 for manual - datatype depends on field 2 (event_type) ...
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "event_group"), // 0?
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "score"),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "opponent_score"),
            new FieldDefinitionPrimitive(9, BaseType.UINT8Z, "front_gear_num"),
            new FieldDefinitionPrimitive(10, BaseType.UINT8Z, "front_gear"),
            new FieldDefinitionPrimitive(11, BaseType.UINT8Z, "rear_gear_num"),
            new FieldDefinitionPrimitive(12, BaseType.UINT8Z, "rear_gear"),
            new FieldDefinitionPrimitive(13, BaseType.UINT8, "device_index"),
            new FieldDefinitionPrimitive(14, BaseType.ENUM, "activity_type"),
            new FieldDefinitionPrimitive(15, BaseType.UINT32, "start_timestamp"), // s
            new FieldDefinitionPrimitive(21, BaseType.ENUM, "radar_threat_level_max"),
            new FieldDefinitionPrimitive(22, BaseType.UINT8, "radar_threat_count"),
            new FieldDefinitionPrimitive(23, BaseType.UINT8, "radar_threat_avg_approach_speed", 10, 0), // m/s
            new FieldDefinitionPrimitive(24, BaseType.UINT8, "radar_threat_max_approach_speed", 10, 0), // m/s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage DEVICE_USED = new GlobalFITMessage(22, "DEVICE_USED", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "speed"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "distance"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "cadence"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "elevation"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "heart_rate"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "power"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage DEVICE_INFO = new GlobalFITMessage(23, "DEVICE_INFO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "device_index"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "device_type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32Z, "serial_number"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "product"),
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "software_version"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "hardware_version"),
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "cum_operating_time"), // s
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "battery_voltage", 256, 0), // V
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "battery_status"),
            new FieldDefinitionPrimitive(18, BaseType.ENUM, "sensor_position"),
            new FieldDefinitionPrimitive(19, BaseType.STRING, "descriptor"),
            new FieldDefinitionPrimitive(20, BaseType.UINT8Z, "ant_transmission_type"),
            new FieldDefinitionPrimitive(21, BaseType.UINT16Z, "ant_device_number"),
            new FieldDefinitionPrimitive(22, BaseType.ENUM, "ant_network"),
            new FieldDefinitionPrimitive(24, BaseType.UINT32Z, "ant_id"),
            new FieldDefinitionPrimitive(25, BaseType.ENUM, "source_type"),
            new FieldDefinitionPrimitive(27, BaseType.STRING, "product_name"),
            new FieldDefinitionPrimitive(32, BaseType.UINT8, "battery_level"), // %
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage WORKOUT = new GlobalFITMessage(26, "WORKOUT", Arrays.asList(
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(5, BaseType.UINT32Z, "capabilities"),
            new FieldDefinitionPrimitive(6, BaseType.UINT16, "num_valid_steps"),
            new FieldDefinitionPrimitive(8, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(11, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "pool_length", 100, 0), // m
            new FieldDefinitionPrimitive(15, BaseType.ENUM, "pool_length_unit"),
            new FieldDefinitionPrimitive(17, BaseType.STRING, "notes"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage WORKOUT_STEP = new GlobalFITMessage(27, "WORKOUT_STEP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, "wkt_step_name"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "duration_type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "duration_value"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "target_type"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "target_value"),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "custom_target_value_low"),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "custom_target_value_high"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "intensity"),
            new FieldDefinitionPrimitive(8, BaseType.STRING, "notes"),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "equipment"),
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "exercise_category"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "exercise_name"),
            new FieldDefinitionPrimitive(12, BaseType.UINT16, "exercise_weight", 100, 0), // kg
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "weight_display_unit"),
            new FieldDefinitionPrimitive(19, BaseType.ENUM, "secondary_target_type"),
            new FieldDefinitionPrimitive(20, BaseType.UINT32, "secondary_target_value"),
            new FieldDefinitionPrimitive(21, BaseType.UINT32, "secondary_custom_target_value_low"),
            new FieldDefinitionPrimitive(22, BaseType.UINT32, "secondary_custom_target_value_high"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SCHEDULE = new GlobalFITMessage(28, "SCHEDULE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "product"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32Z, "serial_number"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "time_created"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "completed", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "scheduled_time")
    ));

    public static GlobalFITMessage LOCATION = new GlobalFITMessage(29, "LOCATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, 32, "name"),
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(2, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "symbol", FieldDefinitionFactory.FIELD.LOCATION_SYMBOL),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "altitude", 5, 500), // m
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "enhanced_altitude"),
            new FieldDefinitionPrimitive(6, BaseType.STRING, "description"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage WEIGHT_SCALE = new GlobalFITMessage(30, "WEIGHT_SCALE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "weight", 100, 0), // kg
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "percent_fat", 100, 0), // %
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "percent_hydration", 100, 0), // %
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "visceral_fat_mass", 100, 0), // kg
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "bone_mass", 100, 0), // kg
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "muscle_mass", 100, 0), // kg
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "basal_met", 4, 0), // kcal/day
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "physique_rating"),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "active_met", 4, 0), // kcal/day
            new FieldDefinitionPrimitive(10, BaseType.UINT8, "metabolic_age"), // years
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "visceral_fat_rating"),
            new FieldDefinitionPrimitive(12, BaseType.UINT16, "user_profile_index"),
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "bmi", 10, 0), // kg/m^2
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage COURSE = new GlobalFITMessage(31, "COURSE", Arrays.asList(
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(5, BaseType.STRING, 16, "name"),
            new FieldDefinitionPrimitive(6, BaseType.UINT32Z, "capabilities"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "sub_sport")
    ));

    public static GlobalFITMessage COURSE_POINT = new GlobalFITMessage(32, "COURSE_POINT", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(2, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(3, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "distance", 100, 0), // m
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(6, BaseType.STRING, 16, "name"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "favorite"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage TOTALS = new GlobalFITMessage(33, "TOTALS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "timer_time"), // s
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "distance"), // m
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "calories"), // kcal
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "elapsed_time"), // s
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "sessions"),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "active_time"), // s
            new FieldDefinitionPrimitive(9, BaseType.UINT8, "sport_index"),
            new FieldDefinitionPrimitive(10, BaseType.STRING, "activity_profile"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage ACTIVITY = new GlobalFITMessage(34, "ACTIVITY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "total_timer_time"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "num_sessions"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "type"), // 0 manual
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "event"), // 26 activity
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "event_type"), // 1 stop
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "event_group"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SOFTWARE = new GlobalFITMessage(35, "SOFTWARE", Arrays.asList(
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "version", 100, 0),
            new FieldDefinitionPrimitive(5, BaseType.STRING, "part_number"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage FILE_CAPABILITIES = new GlobalFITMessage(37, "FILE_CAPABILITIES", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8Z, "flags"),
            new FieldDefinitionPrimitive(2, BaseType.STRING, 16, "directory"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "max_count"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "max_size"), // byte
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage MESG_CAPABILITIES = new GlobalFITMessage(38, "MESG_CAPABILITIES", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "file"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "mesg_num"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "count_type"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "max_count"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "count"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage FIELD_CAPABILITIES = new GlobalFITMessage(39, "FIELD_CAPABILITIES", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "file"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "mesg_num"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "field_num"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "count"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage FILE_CREATOR = new GlobalFITMessage(49, "FILE_CREATOR", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "software_version"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "hardware_version")
    ));

    public static GlobalFITMessage BLOOD_PRESSURE = new GlobalFITMessage(51, "BLOOD_PRESSURE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "systolic_pressure"), // mmHg
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "diastolic_pressure"), // mmHg
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "mean_arterial_pressure"), // mmHg
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "map_3_sample_mean"), // mmHg
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "map_morning_values"), // mmHg
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "map_evening_values"), // mmHg
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "heart_rate"), // bpm
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "heart_rate_type"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "status"),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "user_profile_index"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SPEED_ZONE = new GlobalFITMessage(53, "SPEED_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "high_value", 1000, 0), // m/s
            new FieldDefinitionPrimitive(1, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage MONITORING = new GlobalFITMessage(55, "MONITORING", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "device_index"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "calories"), // kcal
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "distance"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "cycles"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "active_time"),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "activity_type"),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "activity_subtype"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "activity_level"),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "distance_16"),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "cycles_16"),
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "active_time_16"),
            new FieldDefinitionPrimitive(11, BaseType.UINT32, "local_timestamp"),
            new FieldDefinitionPrimitive(12, BaseType.SINT16, "temperature", 100, 0), // °C
            new FieldDefinitionPrimitive(14, BaseType.SINT16, "temperature_min", 100, 0), // °C
            new FieldDefinitionPrimitive(15, BaseType.SINT16, "temperature_max", 100, 0), // °C
            new FieldDefinitionPrimitive(16, BaseType.UINT16, 8, "activity_time", FieldDefinitionFactory.FIELD.ARRAY, 1, 0), // minutes
            new FieldDefinitionPrimitive(19, BaseType.UINT16, "active_calories"),
            new FieldDefinitionPrimitive(29, BaseType.UINT16, "duration_min"),
            new FieldDefinitionPrimitive(24, BaseType.BASE_TYPE_BYTE, "current_activity_type_intensity"),
            new FieldDefinitionPrimitive(26, BaseType.UINT16, "timestamp_16"),
            new FieldDefinitionPrimitive(25, BaseType.UINT8, "timestamp_min_8"), // min
            new FieldDefinitionPrimitive(27, BaseType.UINT8, "heart_rate"),
            new FieldDefinitionPrimitive(28, BaseType.UINT8, "intensity", 10, 0),
            new FieldDefinitionPrimitive(30, BaseType.UINT32, "duration"), // seconds
            new FieldDefinitionPrimitive(31, BaseType.UINT32, "ascent", 1000, 0), // m
            new FieldDefinitionPrimitive(32, BaseType.UINT32, "descent", 1000, 0), // m
            new FieldDefinitionPrimitive(33, BaseType.UINT16, "moderate_activity_minutes"),
            new FieldDefinitionPrimitive(34, BaseType.UINT16, "vigorous_activity_minutes"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage MAP_LAYER = new GlobalFITMessage(70, "MAP_LAYER", Arrays.asList(
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "relief_shading"),
            new FieldDefinitionPrimitive(11, BaseType.ENUM, "orientation"),
            new FieldDefinitionPrimitive(13, BaseType.ENUM, "user_locations"),
            new FieldDefinitionPrimitive(14, BaseType.ENUM, "auto_zoom"),
            new FieldDefinitionPrimitive(15, BaseType.ENUM, "guide_text"),
            new FieldDefinitionPrimitive(16, BaseType.ENUM, "track_log"),
            new FieldDefinitionPrimitive(20, BaseType.ENUM, "courses"),
            new FieldDefinitionPrimitive(23, BaseType.ENUM, "spot_soundings"),
            new FieldDefinitionPrimitive(24, BaseType.ENUM, "light_sectors"),
            new FieldDefinitionPrimitive(27, BaseType.ENUM, "segments"),
            new FieldDefinitionPrimitive(28, BaseType.ENUM, "contours"),
            new FieldDefinitionPrimitive(31, BaseType.ENUM, "popularity"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TRAINING_FILE = new GlobalFITMessage(72, "TRAINING_FILE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "product"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32Z, "serial_number"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "time_created"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HRV = new GlobalFITMessage(78, "HRV", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "time", FieldDefinitionFactory.FIELD.ARRAY)
    ));

    public static GlobalFITMessage USER_METRICS = new GlobalFITMessage(79, "USER_METRICS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "vo2_max"), // scale: 292.5714286
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "age"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "height", 100, 0), // m
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "weight", 10, 0), // kg
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "gender"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "max_hr"),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "remaining_recovery_time"), // min
            new FieldDefinitionPrimitive(15, BaseType.UINT8, "initial_body_battery"),
            new FieldDefinitionPrimitive(16, BaseType.UINT32, "start_of_activity"),
            new FieldDefinitionPrimitive(32, BaseType.UINT8, "beginning_potential"),
            new FieldDefinitionPrimitive(35, BaseType.UINT32, "end_of_previous_activity"),
            new FieldDefinitionPrimitive(39, BaseType.UINT32, "wake_up_time"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ANT_RX = new GlobalFITMessage(80, "ANT_RX", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "fractional_timestamp", 32768, 0),
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, "mesg_id"),
            new FieldDefinitionPrimitive(2, BaseType.BASE_TYPE_BYTE, "mesg_data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "channel_number"),
            new FieldDefinitionPrimitive(4, BaseType.BASE_TYPE_BYTE, "data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ANT_TX = new GlobalFITMessage(81, "ANT_TX", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "fractional_timestamp", 32768, 0),
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, "mesg_id"),
            new FieldDefinitionPrimitive(2, BaseType.BASE_TYPE_BYTE, "mesg_data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "channel_number"),
            new FieldDefinitionPrimitive(4, BaseType.BASE_TYPE_BYTE, "data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ANT_CHANNEL_ID = new GlobalFITMessage(82, "ANT_CHANNEL_ID", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "channel_number"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8Z, "device_type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16Z, "device_number"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8Z, "transmission_type"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "device_index")
    ));

    public static GlobalFITMessage LENGTH = new GlobalFITMessage(101, "LENGTH", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "event"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "event_type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "total_elapsed_time", 1000, 0), // s
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "total_timer_time", 1000, 0), // s
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "total_strokes"),
            new FieldDefinitionPrimitive(6, BaseType.UINT16, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "swim_stroke"),
            new FieldDefinitionPrimitive(9, BaseType.UINT8, "avg_swimming_cadence"), // stroke / min
            new FieldDefinitionPrimitive(10, BaseType.UINT8, "event_group"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "total_calories"), // kcal
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "length_type"),
            new FieldDefinitionPrimitive(18, BaseType.UINT16, "player_score"),
            new FieldDefinitionPrimitive(19, BaseType.UINT16, "opponent_score"),
            new FieldDefinitionPrimitive(20, BaseType.UINT16, "stroke_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(21, BaseType.UINT16, "zone_count", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(22, BaseType.UINT16, "enhanced_avg_respiration_rate", 1000, 0), // breath / min
            new FieldDefinitionPrimitive(23, BaseType.UINT16, "enhanced_max_respiration_rate", 1000, 0), // breath / min
            new FieldDefinitionPrimitive(24, BaseType.UINT8, "avg_respiration_rate"),
            new FieldDefinitionPrimitive(25, BaseType.UINT8, "max_respiration_rate"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage MONITORING_INFO = new GlobalFITMessage(103, "MONITORING_INFO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "activity_type", FieldDefinitionFactory.FIELD.ARRAY), // 6 walking, 1 running, 13 ?
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "steps_to_distance", FieldDefinitionFactory.FIELD.ARRAY, 5000, 0), // same size as activity_type?
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "steps_to_calories", FieldDefinitionFactory.FIELD.ARRAY, 5000, 0), // same size as activity_type?
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "resting_metabolic_rate"), // kcal/day
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage DEVICE_STATUS = new GlobalFITMessage(104, "DEVICE_STATUS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "battery_voltage", 1000, 0), // V
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "battery_level"), // 0 - 100%
            new FieldDefinitionPrimitive(3, BaseType.SINT8, "temperature"), // °C
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage PAD = new GlobalFITMessage(105, "PAD", Arrays.asList(
            // only used to align other messages to memory boundaries
    ));

    public static GlobalFITMessage SLAVE_DEVICE = new GlobalFITMessage(106, "SLAVE_DEVICE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "product")
    ));

    public static GlobalFITMessage CONNECTIVITY = new GlobalFITMessage(127, "CONNECTIVITY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "bluetooth_enabled"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "bluetooth_le_enabled"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "ant_enabled"),
            new FieldDefinitionPrimitive(3, BaseType.STRING, 20, "name"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "live_tracking_enabled"),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "weather_conditions_enabled"),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "weather_alerts_enabled"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "auto_activity_upload_enabled"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "course_download_enabled"),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "workout_download_enabled"),
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "gps_ephemeris_download_enabled"),
            new FieldDefinitionPrimitive(11, BaseType.ENUM, "incident_detection_enabled"),
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "grouptrack_enabled")
    ));

    public static GlobalFITMessage WEATHER = new GlobalFITMessage(128, "WEATHER", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "weather_report"),
            new FieldDefinitionPrimitive(1, BaseType.SINT8, "temperature", FieldDefinitionFactory.FIELD.TEMPERATURE),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "condition", FieldDefinitionFactory.FIELD.WEATHER_CONDITION),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "wind_direction"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "wind_speed", 298, 0),
            new FieldDefinitionPrimitive(5, BaseType.UINT8, "precipitation_probability"),
            new FieldDefinitionPrimitive(6, BaseType.SINT8, "temperature_feels_like", FieldDefinitionFactory.FIELD.TEMPERATURE),
            new FieldDefinitionPrimitive(7, BaseType.UINT8, "relative_humidity"),
            new FieldDefinitionPrimitive(8, BaseType.STRING, 15, "location"),
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "observed_at_time", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(10, BaseType.SINT32, "observed_location_lat"),
            new FieldDefinitionPrimitive(11, BaseType.SINT32, "observed_location_long"),
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "day_of_week", FieldDefinitionFactory.FIELD.DAY_OF_WEEK),
            new FieldDefinitionPrimitive(13, BaseType.SINT8, "high_temperature", FieldDefinitionFactory.FIELD.TEMPERATURE),
            new FieldDefinitionPrimitive(14, BaseType.SINT8, "low_temperature", FieldDefinitionFactory.FIELD.TEMPERATURE),
            new FieldDefinitionPrimitive(15, BaseType.SINT8, "dew_point", FieldDefinitionFactory.FIELD.TEMPERATURE),
            new FieldDefinitionPrimitive(16, BaseType.FLOAT32, "uv_index"),
            new FieldDefinitionPrimitive(17, BaseType.ENUM, "air_quality", FieldDefinitionFactory.FIELD.WEATHER_AQI),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage WEATHER_ALERT = new GlobalFITMessage(129, "WEATHER_ALERT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, "report_id"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "issue_time"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "expire_time"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "severity"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage CADENCE_ZONE = new GlobalFITMessage(131, "CADENCE_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "high_value"), // rpm
            new FieldDefinitionPrimitive(1, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage HR = new GlobalFITMessage(132, "HR", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "fractional_timestamp", 32768, 0), // s
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "time256", 256, 0), // s
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "filtered_bpm", FieldDefinitionFactory.FIELD.ARRAY), // bpm
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "event_timestamp", FieldDefinitionFactory.FIELD.ARRAY, 1024, 0), // s
            new FieldDefinitionPrimitive(10, BaseType.BASE_TYPE_BYTE, "event_timestamp_12", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    // https://github.com/GoldenCheetah/GoldenCheetah/blob/71e3928bc614f3209d9977d90cc50b942999b855/src/FileIO/FitRideFile.cpp#L1998
    public static GlobalFITMessage PHYSIOLOGICAL_METRICS = new GlobalFITMessage(140, "PHYSIOLOGICAL_METRICS", Arrays.asList(
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "aerobic_effect", 10, 0),
            new FieldDefinitionPrimitive(7, BaseType.SINT32, "met_max", 65536, 0),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "recovery_time", 1, 0), // minutes
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "lactate_threshold_heart_rate", 1, 0), // bpm
            //new FieldDefinitionPrimitive(15, BaseType.UINT16, "lactate_threshold_speed", 1, 0), // m/s // TODO confirm scale
            new FieldDefinitionPrimitive(20, BaseType.UINT8, "anaerobic_effect", 10, 0),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage EPO_STATUS = new GlobalFITMessage(141, "EPO_STATUS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "status"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "end_time"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SEGMENT_LAP = new GlobalFITMessage(142, "SEGMENT_LAP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "event"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "event_type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(3, BaseType.SINT32, "start_position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(4, BaseType.SINT32, "start_position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(5, BaseType.SINT32, "end_position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(6, BaseType.SINT32, "end_position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "total_elapsed_time", 1000, 0), // s
            new FieldDefinitionPrimitive(8, BaseType.UINT32, "total_timer_time", 1000, 0), // s
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "total_distance", 100, 0), // m
            new FieldDefinitionPrimitive(10, BaseType.UINT32, "total_cycles"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "total_calories"), // kcal
            new FieldDefinitionPrimitive(12, BaseType.UINT16, "total_fat_calories"), // kcal
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(15, BaseType.UINT8, "avg_heart_rate"),
            new FieldDefinitionPrimitive(16, BaseType.UINT8, "max_heart_rate"),
            new FieldDefinitionPrimitive(17, BaseType.UINT8, "avg_cadence"), // bpm
            new FieldDefinitionPrimitive(18, BaseType.UINT8, "max_cadence"), // bpm
            new FieldDefinitionPrimitive(19, BaseType.UINT16, "avg_power"), // watt
            new FieldDefinitionPrimitive(20, BaseType.UINT16, "max_power"), // watt
            new FieldDefinitionPrimitive(21, BaseType.UINT16, "total_ascent"), // m
            new FieldDefinitionPrimitive(22, BaseType.UINT16, "total_descent"), // m
            new FieldDefinitionPrimitive(23, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(24, BaseType.UINT8, "event_group"),
            new FieldDefinitionPrimitive(25, BaseType.SINT32, "nec_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(26, BaseType.SINT32, "nec_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(27, BaseType.SINT32, "swc_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(28, BaseType.SINT32, "swc_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(29, BaseType.STRING, 50, "name"),
            new FieldDefinitionPrimitive(30, BaseType.UINT16, "normalized_power"), // watt
            new FieldDefinitionPrimitive(31, BaseType.UINT16, "left_right_balance"),
            new FieldDefinitionPrimitive(32, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(33, BaseType.UINT32, "total_work"), // Joule
            new FieldDefinitionPrimitive(34, BaseType.UINT16, "avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(35, BaseType.UINT16, "max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(36, BaseType.UINT8, "gps_accuracy"), // m
            new FieldDefinitionPrimitive(37, BaseType.SINT16, "avg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(38, BaseType.SINT16, "avg_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(39, BaseType.SINT16, "avg_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(40, BaseType.SINT16, "max_pos_grade", 100, 0), // %
            new FieldDefinitionPrimitive(41, BaseType.SINT16, "max_neg_grade", 100, 0), // %
            new FieldDefinitionPrimitive(42, BaseType.SINT8, "avg_temperature"), // °C
            new FieldDefinitionPrimitive(43, BaseType.SINT8, "max_temperature"), // °C
            new FieldDefinitionPrimitive(44, BaseType.UINT32, "total_moving_time", 1000, 0), // s
            new FieldDefinitionPrimitive(45, BaseType.SINT16, "avg_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(46, BaseType.SINT16, "avg_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(47, BaseType.SINT16, "max_pos_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(48, BaseType.SINT16, "max_neg_vertical_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(49, BaseType.UINT32, "time_in_hr_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(50, BaseType.UINT32, "time_in_speed_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(51, BaseType.UINT32, "time_in_cadence_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(52, BaseType.UINT32, "time_in_power_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(53, BaseType.UINT16, "repetition_num"),
            new FieldDefinitionPrimitive(54, BaseType.UINT16, "min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(55, BaseType.UINT8, "min_heart_rate"),
            new FieldDefinitionPrimitive(56, BaseType.UINT32, "active_time", 1000, 0), // s
            new FieldDefinitionPrimitive(57, BaseType.UINT16, "wkt_step_index"),
            new FieldDefinitionPrimitive(58, BaseType.ENUM, "sport_event"),
            new FieldDefinitionPrimitive(59, BaseType.UINT8, "avg_left_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(60, BaseType.UINT8, "avg_right_torque_effectiveness", 2, 0), // %
            new FieldDefinitionPrimitive(61, BaseType.UINT8, "avg_left_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(62, BaseType.UINT8, "avg_right_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(63, BaseType.UINT8, "avg_combined_pedal_smoothness", 2, 0), // %
            new FieldDefinitionPrimitive(64, BaseType.ENUM, "status"),
            new FieldDefinitionPrimitive(65, BaseType.STRING, 33, "uuid"),
            new FieldDefinitionPrimitive(66, BaseType.UINT8, "avg_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(67, BaseType.UINT8, "max_fractional_cadence", 128, 0), // rpm
            new FieldDefinitionPrimitive(68, BaseType.UINT8, "total_fractional_cycles", 128, 0), // cycles
            new FieldDefinitionPrimitive(69, BaseType.UINT16, "front_gear_shift_count"),
            new FieldDefinitionPrimitive(70, BaseType.UINT16, "rear_gear_shift_count"),
            new FieldDefinitionPrimitive(71, BaseType.UINT32, "time_standing", 1000, 0), // s
            new FieldDefinitionPrimitive(72, BaseType.UINT16, "stand_count"),
            new FieldDefinitionPrimitive(73, BaseType.SINT8, "avg_left_pco"), // mm
            new FieldDefinitionPrimitive(74, BaseType.SINT8, "avg_right_pco"), // mm
            new FieldDefinitionPrimitive(75, BaseType.UINT8, "avg_left_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(76, BaseType.UINT8, "avg_left_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(77, BaseType.UINT8, "avg_right_power_phase", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(78, BaseType.UINT8, "avg_right_power_phase_peak", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(79, BaseType.UINT16, "avg_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt
            new FieldDefinitionPrimitive(80, BaseType.UINT16, "max_power_position", FieldDefinitionFactory.FIELD.ARRAY), // watt
            new FieldDefinitionPrimitive(81, BaseType.UINT8, "avg_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // rpm
            new FieldDefinitionPrimitive(82, BaseType.UINT8, "max_cadence_position", FieldDefinitionFactory.FIELD.ARRAY), // rpm
            new FieldDefinitionPrimitive(83, BaseType.UINT16, "manufacturer"),
            new FieldDefinitionPrimitive(84, BaseType.FLOAT32, "total_grit"),
            new FieldDefinitionPrimitive(85, BaseType.FLOAT32, "total_flow"),
            new FieldDefinitionPrimitive(86, BaseType.FLOAT32, "avg_grit"),
            new FieldDefinitionPrimitive(87, BaseType.FLOAT32, "avg_flow"),
            new FieldDefinitionPrimitive(89, BaseType.UINT8, "total_fractional_ascent", 100, 0), // m
            new FieldDefinitionPrimitive(90, BaseType.UINT8, "total_fractional_descent", 100, 0), // m
            new FieldDefinitionPrimitive(91, BaseType.UINT32, "enhanced_avg_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(92, BaseType.UINT32, "enhanced_max_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(93, BaseType.UINT32, "enhanced_min_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage MEMO_GLOB = new GlobalFITMessage(145, "MEMO_GLOB", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.BASE_TYPE_BYTE, "memo", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "mesg_num"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "parent_index"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "field_num"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8Z, "data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(250, BaseType.UINT32, "part_index")
    ));

    public static GlobalFITMessage SENSOR_SETTINGS = new GlobalFITMessage(147, "SENSOR_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32Z, "ant_id"),
            new FieldDefinitionPrimitive(2, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(45, BaseType.ENUM, "use_for_speed"),
            new FieldDefinitionPrimitive(46, BaseType.ENUM, "use_for_distance"),
            new FieldDefinitionPrimitive(51, BaseType.ENUM, "connection_type"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SEGMENT_ID = new GlobalFITMessage(148, "SEGMENT_ID", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, 50, "name"),
            new FieldDefinitionPrimitive(1, BaseType.STRING, 33, "uuid"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "enabled"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "user_profile_primary_key"),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "device_id"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "default_race_leader"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "delete_status"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "selection_type")
    ));

    public static GlobalFITMessage SEGMENT_LEADERBOARD_ENTRY = new GlobalFITMessage(149, "SEGMENT_LEADERBOARD_ENTRY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, 100, "name"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "type"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "group_primary_key"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "activity_id"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "segment_time", 1000, 0), // s
            new FieldDefinitionPrimitive(5, BaseType.STRING, 22, "activity_id_string"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SEGMENT_POINT = new GlobalFITMessage(150, "SEGMENT_POINT", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(2, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "distance", 100, 0), // m
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "altitude", 5, 500), // m
            new FieldDefinitionPrimitive(5, BaseType.UINT32, 6,"leader_time",  FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "enhanced_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SEGMENT_FILE = new GlobalFITMessage(151, "SEGMENT_FILE", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.STRING, "file_uuid"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "enabled"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "user_profile_primary_key"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "leader_type", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(8, BaseType.UINT32, "leader_group_primary_key", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "leader_activity_id", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(10, BaseType.STRING, "leader_activity_id_string", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "default_race_leader"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage WORKOUT_SESSION = new GlobalFITMessage(158, "WORKOUT_SESSION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "num_valid_steps"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "first_step_index"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "pool_length", 100, 0), // m
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "pool_length_unit"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage WATCHFACE_SETTINGS = new GlobalFITMessage(159, "WATCHFACE_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "mode"), //1=analog
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, "layout"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage GPS_METADATA = new GlobalFITMessage(160, "GPS_METADATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(2, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "enhanced_altitude", 5, 500), // m
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "enhanced_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "heading", 100, 0), // degrees
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "utc_timestamp"), // s
            new FieldDefinitionPrimitive(7, BaseType.SINT16, 3, "velocity", FieldDefinitionFactory.FIELD.ARRAY, 100, 0), // m/s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage CAMERA_EVENT = new GlobalFITMessage(161, "CAMERA_EVENT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "camera_event_type"),
            new FieldDefinitionPrimitive(2, BaseType.STRING, "camera_file_uuid"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "camera_orientation"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TIMESTAMP_CORRELATION = new GlobalFITMessage(162, "TIMESTAMP_CORRELATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "fractional_timestamp", 32768, 0),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "system_timestamp"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "fractional_system_timestamp", 32768, 0),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "timestamp_ms"),
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "system_timestamp_ms"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage GYROSCOPE_DATA = new GlobalFITMessage(164, "GYROSCOPE_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sample_time_offset", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "gyro_x", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "gyro_y", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "gyro_z", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(5, BaseType.FLOAT32, "calibrated_gyro_x", FieldDefinitionFactory.FIELD.ARRAY), // deg/s
            new FieldDefinitionPrimitive(6, BaseType.FLOAT32, "calibrated_gyro_y", FieldDefinitionFactory.FIELD.ARRAY), // deg/s
            new FieldDefinitionPrimitive(7, BaseType.FLOAT32, "calibrated_gyro_z", FieldDefinitionFactory.FIELD.ARRAY), // deg/s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ACCELEROMETER_DATA = new GlobalFITMessage(165, "ACCELEROMETER_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sample_time_offset", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "accel_x", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "accel_y", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "accel_z", FieldDefinitionFactory.FIELD.ARRAY), // count
            new FieldDefinitionPrimitive(5, BaseType.FLOAT32, "calibrated_accel_x", FieldDefinitionFactory.FIELD.ARRAY), // g
            new FieldDefinitionPrimitive(6, BaseType.FLOAT32, "calibrated_accel_y", FieldDefinitionFactory.FIELD.ARRAY), // g
            new FieldDefinitionPrimitive(7, BaseType.FLOAT32, "calibrated_accel_z", FieldDefinitionFactory.FIELD.ARRAY), // g
            new FieldDefinitionPrimitive(8, BaseType.SINT16, "compressed_calibrated_accel_x", FieldDefinitionFactory.FIELD.ARRAY), // mili g
            new FieldDefinitionPrimitive(9, BaseType.SINT16, "compressed_calibrated_accel_y", FieldDefinitionFactory.FIELD.ARRAY), // mili g
            new FieldDefinitionPrimitive(10, BaseType.SINT16, "compressed_calibrated_accel_z", FieldDefinitionFactory.FIELD.ARRAY), // mili g
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage THREE_D_SENSOR_CALIBRATION = new GlobalFITMessage(167, "THREE_D_SENSOR_CALIBRATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sensor_type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "calibration_factor"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "calibration_divisor"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "level_shift"),
            new FieldDefinitionPrimitive(4, BaseType.SINT32, 3, "offset_cal", FieldDefinitionFactory.FIELD.ARRAY, 1, 0),
            new FieldDefinitionPrimitive(5, BaseType.SINT32, 9, "orientation_matrix", FieldDefinitionFactory.FIELD.ARRAY, 65535, 0),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage VIDEO_FRAME = new GlobalFITMessage(169, "VIDEO_FRAME", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "frame_number"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage OBDII_DATA = new GlobalFITMessage(174, "OBDII_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "time_offset", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(2, BaseType.BASE_TYPE_BYTE, "pid"),
            new FieldDefinitionPrimitive(3, BaseType.BASE_TYPE_BYTE, "raw_data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "pid_data_size", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "system_time", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "start_timestamp"),
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "start_timestamp_ms"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage NMEA_SENTENCE = new GlobalFITMessage(177, "NMEA_SENTENCE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.STRING, "sentence"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage AVIATION_ATTITUDE = new GlobalFITMessage(178, "AVIATION_ATTITUDE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "system_time", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(2, BaseType.SINT16, "pitch", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.SINT16, "roll", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.SINT16, "accel_lateral", FieldDefinitionFactory.FIELD.ARRAY, 100, 0), // m/s^2
            new FieldDefinitionPrimitive(5, BaseType.SINT16, "accel_normal", FieldDefinitionFactory.FIELD.ARRAY, 100, 0), // m/s^2
            new FieldDefinitionPrimitive(6, BaseType.SINT16, "turn_rate", FieldDefinitionFactory.FIELD.ARRAY, 1024, 0), // radians/second
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "stage", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "attitude_stage_complete", FieldDefinitionFactory.FIELD.ARRAY), // %
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "track", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "validity", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage VIDEO = new GlobalFITMessage(184, "VIDEO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, "url"),
            new FieldDefinitionPrimitive(1, BaseType.STRING, "hosting_provider"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "duration")
    ));

    public static GlobalFITMessage VIDEO_TITLE = new GlobalFITMessage(185, "VIDEO_TITLE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "message_count"),
            new FieldDefinitionPrimitive(1, BaseType.STRING, "text"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage VIDEO_DESCRIPTION = new GlobalFITMessage(186, "VIDEO_DESCRIPTION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "message_count"),
            new FieldDefinitionPrimitive(1, BaseType.STRING, "text"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage VIDEO_CLIP = new GlobalFITMessage(187, "VIDEO_CLIP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "clip_number"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "start_timestamp"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "start_timestamp_ms"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "end_timestamp"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "end_timestamp_ms"),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "clip_start"), // ms
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "clip_end") // ms
    ));

    public static GlobalFITMessage OHR_SETTINGS = new GlobalFITMessage(188, "OHR_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "enabled"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage EXD_SCREEN_CONFIGURATION = new GlobalFITMessage(200, "EXD_SCREEN_CONFIGURATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "screen_index"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "field_count"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "layout"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "screen_enabled", FieldDefinitionFactory.FIELD.BOOLEAN)
    ));

    public static GlobalFITMessage EXD_DATA_FIELD_CONFIGURATION = new GlobalFITMessage(201, "EXD_DATA_FIELD_CONFIGURATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "screen_index"),
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, "concept_field"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "field_id"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "concept_count"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "display_type"),
            new FieldDefinitionPrimitive(5, BaseType.STRING, 32, "title")
    ));

    public static GlobalFITMessage EXD_DATA_CONCEPT_CONFIGURATION = new GlobalFITMessage(202, "EXD_DATA_CONCEPT_CONFIGURATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "screen_index"),
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, "concept_field"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "field_id"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "concept_count"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "data_page"),
            new FieldDefinitionPrimitive(5, BaseType.UINT8, "concept_key"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "scaling"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "data_units"),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "qualifier"),
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "descriptor"),
            new FieldDefinitionPrimitive(11, BaseType.ENUM, "is_signed", FieldDefinitionFactory.FIELD.BOOLEAN)
    ));

    public static GlobalFITMessage FIELD_DESCRIPTION = new GlobalFITMessage(206, "FIELD_DESCRIPTION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "developer_data_index"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "field_definition_number"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "fit_base_type_id"),
            new FieldDefinitionPrimitive(3, BaseType.STRING, 64, "field_name"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "array"),
            new FieldDefinitionPrimitive(5, BaseType.STRING, "components"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "scale"),
            new FieldDefinitionPrimitive(7, BaseType.SINT8, "offset"),
            new FieldDefinitionPrimitive(8, BaseType.STRING, 16, "units"),
            new FieldDefinitionPrimitive(9, BaseType.STRING, "bits"),
            new FieldDefinitionPrimitive(10, BaseType.STRING, "accumulate"),
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "fit_base_unit_id"),
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "native_mesg_num"),
            new FieldDefinitionPrimitive(15, BaseType.UINT8, "native_field_num")
    ));

    public static GlobalFITMessage DEVELOPER_DATA = new GlobalFITMessage(207, "DEVELOPER_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.BASE_TYPE_BYTE, 16, "developer_id", FieldDefinitionFactory.FIELD.ARRAY, 1, 0),
            new FieldDefinitionPrimitive(1, BaseType.BASE_TYPE_BYTE, 16, "application_id", FieldDefinitionFactory.FIELD.ARRAY, 1, 0),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "manufacturer_id"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "developer_data_index"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "application_version")
    ));

    public static GlobalFITMessage MAGNETOMETER_DATA = new GlobalFITMessage(208, "MAGNETOMETER_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sample_time_offset", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "mag_x", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "mag_y", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "mag_z", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(5, BaseType.FLOAT32, "calibrated_mag_x", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(6, BaseType.FLOAT32, "calibrated_mag_y", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(7, BaseType.FLOAT32, "calibrated_mag_z", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage BAROMETER_DATA = new GlobalFITMessage(209, "BAROMETER_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sample_time_offset", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "baro_pres", FieldDefinitionFactory.FIELD.ARRAY), // Pa
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ONE_D_SENSOR_CALIBRATION = new GlobalFITMessage(210, "ONE_D_SENSOR_CALIBRATION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sensor_type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "calibration_factor"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "calibration_divisor"),
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "level_shift"),
            new FieldDefinitionPrimitive(4, BaseType.SINT32, "offset_cal"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage MONITORING_HR_DATA = new GlobalFITMessage(211, "MONITORING_HR_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "resting_heart_rate"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "current_day_resting_heart_rate"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TIME_IN_ZONE = new GlobalFITMessage(216, "TIME_IN_ZONE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "reference_message"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "reference_index"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "time_in_zone", FieldDefinitionFactory.FIELD.HR_TIME_IN_ZONE), // seconds
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "time_in_speed_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "time_in_cadence_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "time_in_power_zone", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // s
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "hr_zone_high_boundary", FieldDefinitionFactory.FIELD.HR_ZONE_HIGH_BOUNDARY), // bpm
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "speed_zone_high_boundary", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // m/s
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "cadence_zone_high_boundary", FieldDefinitionFactory.FIELD.ARRAY), // rpm
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "power_zone_high_boundary", FieldDefinitionFactory.FIELD.ARRAY), // watt
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "hr_calc_type"), // 1 percent max hr
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "max_heart_rate"),
            new FieldDefinitionPrimitive(12, BaseType.UINT8, "resting_heart_rate"),
            new FieldDefinitionPrimitive(13, BaseType.UINT8, "threshold_heart_rate"),
            new FieldDefinitionPrimitive(14, BaseType.ENUM, "pwr_calc_type"),
            new FieldDefinitionPrimitive(15, BaseType.UINT16, "functional_threshold_power"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ALARM_SETTINGS = new GlobalFITMessage(222, "ALARM_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "time", FieldDefinitionFactory.FIELD.ALARM),
            new FieldDefinitionPrimitive(1, BaseType.UINT32Z, "repeat"), // 31 weekday 96 weekend 126 all except mon 127 daily 128 once
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "enabled"), // 0/1
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "sound"), // 0 none 1 sound 2 vibrate 3 sound+vibrate
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "backlight"), // 1
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "some_timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(7, BaseType.UINT8, "unknown7"), // 0
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "label", FieldDefinitionFactory.FIELD.ALARM_LABEL), // 0 none 2 workout 3 reminder 4 appointment 6 class 7 meditate 8 bedtime
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SET = new GlobalFITMessage(225, "SET", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "duration", 1000, 0), // seconds
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "repetitions"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "weight", 16, 0), // kg
            new FieldDefinitionPrimitive(5, BaseType.UINT8, "set_type"), // 1 active 0 rest
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "start_time", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "category", FieldDefinitionFactory.FIELD.EXERCISE_CATEGORY),
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "category_subtype", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "weight_display_unit"),
            new FieldDefinitionPrimitive(10, BaseType.UINT16, "message_index"),
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "wkt_step_index"),
            new FieldDefinitionPrimitive(254, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));
    public static GlobalFITMessage DIVE_SETTINGS = new GlobalFITMessage(258, "DIVE_SETTINGS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.STRING, "name"),
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "model"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "gf_low"), // %
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "gf_high"), // %
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "water_type"), // %
            new FieldDefinitionPrimitive(5, BaseType.FLOAT32, "water_density"), // kg/m^3
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "po2_warn", 100, 0), // %
            new FieldDefinitionPrimitive(7, BaseType.UINT8, "po2_critical", 100, 0), // %
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "po2_deco", 100, 0), // %
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "safety_stop_enabled"),
            new FieldDefinitionPrimitive(10, BaseType.FLOAT32, "bottom_depth"),
            new FieldDefinitionPrimitive(11, BaseType.UINT32, "bottom_time"),
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "apnea_countdown_enabled"),
            new FieldDefinitionPrimitive(13, BaseType.UINT32, "apnea_countdown_time"),
            new FieldDefinitionPrimitive(14, BaseType.ENUM, "backlight_mode"),
            new FieldDefinitionPrimitive(15, BaseType.UINT8, "backlight_brightness"),
            new FieldDefinitionPrimitive(16, BaseType.UINT8, "backlight_timeout"),
            new FieldDefinitionPrimitive(17, BaseType.UINT16, "repeat_dive_interval"), // s
            new FieldDefinitionPrimitive(18, BaseType.UINT16, "safety_stop_time"), // s
            new FieldDefinitionPrimitive(19, BaseType.ENUM, "heart_rate_source_type"),
            new FieldDefinitionPrimitive(20, BaseType.UINT8, "heart_rate_source"),
            new FieldDefinitionPrimitive(21, BaseType.UINT16, "travel_gas"),
            new FieldDefinitionPrimitive(22, BaseType.ENUM, "ccr_low_setpoint_switch_mode"),
            new FieldDefinitionPrimitive(23, BaseType.UINT8, "ccr_low_setpoint", 100, 0), // %
            new FieldDefinitionPrimitive(24, BaseType.UINT32, "ccr_low_setpoint_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(25, BaseType.ENUM, "ccr_high_setpoint_switch_mode"),
            new FieldDefinitionPrimitive(26, BaseType.UINT8, "ccr_high_setpoint", 100, 0), // %
            new FieldDefinitionPrimitive(27, BaseType.UINT32, "ccr_high_setpoint_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(29, BaseType.ENUM, "gas_consumption_display"),
            new FieldDefinitionPrimitive(30, BaseType.ENUM, "up_key_enabled"),
            new FieldDefinitionPrimitive(35, BaseType.ENUM, "dive_sounds"),
            new FieldDefinitionPrimitive(36, BaseType.UINT8, "last_stop_multiple", 10, 0),
            new FieldDefinitionPrimitive(37, BaseType.ENUM, "no_fly_time_mode"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));
    public static GlobalFITMessage DIVE_GAS = new GlobalFITMessage(259, "DIVE_GAS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "helium_content"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "oxygen_content"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "status"),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "mode"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));
    public static GlobalFITMessage STRESS_LEVEL = new GlobalFITMessage(227, "STRESS_LEVEL", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.SINT16, "stress_level_value"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "stress_level_time", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(3, BaseType.SINT8, "body_energy")
    ));

    public static GlobalFITMessage DIVE_ALARM = new GlobalFITMessage(262, "DIVE_ALARM", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "depth", 1000, 0), // m
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "time"), // s
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "alarm_type"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "sound"),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "dive_types", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "id"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "popup_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "trigger_on_descent", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "trigger_on_ascent", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "repeating", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(11, BaseType.SINT32, "speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage EXERCISE_TITLE = new GlobalFITMessage(264, "EXERCISE_TITLE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "exercise_category"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "exercise_name"),
            new FieldDefinitionPrimitive(2, BaseType.STRING, "wkt_step_name"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SPO2 = new GlobalFITMessage(269, "SPO2", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "reading_spo2"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "reading_confidence"),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "mode"), // 1 manual 3 periodic
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SLEEP_DATA_INFO = new GlobalFITMessage(273, "SLEEP_DATA_INFO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "unk0"), // 2
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sample_length"), // 60, sample time?
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "unk3"), // 1
            new FieldDefinitionPrimitive(4, BaseType.STRING, "version"), // matches ETE in settings
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SLEEP_DATA_RAW = new GlobalFITMessage(274, "SLEEP_DATA_RAW", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.BASE_TYPE_BYTE, "bytes") // arr of 20 bytes per sample
    ));

    public static GlobalFITMessage SLEEP_STAGE = new GlobalFITMessage(275, "SLEEP_STAGE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "sleep_stage", FieldDefinitionFactory.FIELD.SLEEP_STAGE),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage MAX_MET_DATA = new GlobalFITMessage(229, "MAX_MET_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "update_time", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "vo2_max", 10, 0),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "sport"),
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "sub_sport"),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "max_met_category"), // 0 generic
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "calibrated_data"), // 1?
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "hr_source"),
            new FieldDefinitionPrimitive(13, BaseType.ENUM, "speed_source")
    ));

    public static GlobalFITMessage DIVE_SUMMARY = new GlobalFITMessage(268, "DIVE_SUMMARY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "reference_mesg"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "reference_index"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "avg_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "max_depth", 1000, 0), // m
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "surface_interval"), // s
            new FieldDefinitionPrimitive(5, BaseType.UINT8, "start_cns"), // %
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "end_cns"), // %
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "start_n2"), // %
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "end_n2"), // %
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "o2_toxicity"), // OTUs
            new FieldDefinitionPrimitive(10, BaseType.UINT32, "dive_number"),
            new FieldDefinitionPrimitive(11, BaseType.UINT32, "bottom_time", 1000, 0), // s
            new FieldDefinitionPrimitive(12, BaseType.UINT16, "avg_pressure_sac", 100, 0), // bar/min
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "avg_volume_sac", 100, 0), // L/min
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "avg_rmv", 100, 0), // L/min
            new FieldDefinitionPrimitive(15, BaseType.UINT32, "descent_time", 1000, 0), // s
            new FieldDefinitionPrimitive(16, BaseType.UINT32, "ascent_time", 1000, 0), // s
            new FieldDefinitionPrimitive(17, BaseType.SINT32, "avg_ascent_rate", 1000, 0), // m/s
            new FieldDefinitionPrimitive(22, BaseType.UINT32, "avg_descent_rate", 1000, 0), // m/s
            new FieldDefinitionPrimitive(23, BaseType.UINT32, "max_ascent_rate", 1000, 0), // m/s
            new FieldDefinitionPrimitive(24, BaseType.UINT32, "max_descent_rate", 1000, 0), // m/s
            new FieldDefinitionPrimitive(25, BaseType.UINT32, "hang_time", 1000, 0), // s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)

    ));

    public static GlobalFITMessage JUMP = new GlobalFITMessage(285, "JUMP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.FLOAT32, "distance"), // m
            new FieldDefinitionPrimitive(1, BaseType.FLOAT32, "heigh"), // m
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "rotations"),
            new FieldDefinitionPrimitive(3, BaseType.FLOAT32, "hang_time"), // s
            new FieldDefinitionPrimitive(4, BaseType.FLOAT32, "score"), // s
            new FieldDefinitionPrimitive(5, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(6, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(7, BaseType.UINT16, "speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(8, BaseType.UINT32, "enhanced_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage AAD_ACCEL_FEATURES = new GlobalFITMessage(289, "AAD_ACCEL_FEATURES", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "time"), // s
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "energy_total"),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "zero_cross_cnt"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "instance"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "time_above_threshold"), // s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage BEAT_INTERVALS = new GlobalFITMessage(290, "BEAT_INTERVALS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "time", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage RESPIRATION_RATE = new GlobalFITMessage(297, "RESPIRATION_RATE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.SINT16, "respiration_rate", 100, 0), // breaths / min
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_ACCELEROMETER_DATA = new GlobalFITMessage(302, "HSA_ACCELEROMETER_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sampling_interval"),
            new FieldDefinitionPrimitive(2, BaseType.SINT16, "accel_x", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.SINT16, "accel_y", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.SINT16, "accel_z", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "timestamp_32k"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_STEP_DATA = new GlobalFITMessage(304, "HSA_STEP_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "steps", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_SPO2_DATA = new GlobalFITMessage(305, "HSA_SPO2_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "reading_spo2", FieldDefinitionFactory.FIELD.ARRAY), // %
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "confidence", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_STRESS_DATA = new GlobalFITMessage(306, "HSA_STRESS_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.SINT8, "stress_level", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_RESPIRATION_DATA = new GlobalFITMessage(307, "HSA_RESPIRATION_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.SINT16, "respiration_rate", FieldDefinitionFactory.FIELD.ARRAY, 100, 0), // breath / minute
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_HEART_RATE_DATA = new GlobalFITMessage(308, "HSA_HEART_RATE_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "status"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "heart_rate", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SPLIT = new GlobalFITMessage(312, "SPLIT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "split_type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "total_elapsed_time", 1000, 0), // seconds
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "total_timer_time", 1000, 0), // seconds
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "total_distance", 100, 0), // meter
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "avg_speed", 1000, 0), // meter / second
            new FieldDefinitionPrimitive(9, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(13, BaseType.UINT16, "total_ascent"), // meter
            new FieldDefinitionPrimitive(14, BaseType.UINT16, "total_descent"), // meter
            new FieldDefinitionPrimitive(21, BaseType.SINT32, "start_position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(22, BaseType.SINT32, "start_position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(23, BaseType.SINT32, "end_position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(24, BaseType.SINT32, "end_position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(25, BaseType.UINT32, "max_speed", 1000, 0), // meter / second
            new FieldDefinitionPrimitive(26, BaseType.SINT32, "avg_vert_speed", 1000, 0), // meter / second
            new FieldDefinitionPrimitive(27, BaseType.UINT32, "end_time"),
            new FieldDefinitionPrimitive(28, BaseType.UINT32, "total_calories"), // kcal
            new FieldDefinitionPrimitive(74, BaseType.UINT32, "start_elevation", 5, 500), // meter
            new FieldDefinitionPrimitive(110, BaseType.UINT32, "total_moving_time", 1000, 0), // seconds
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage SPLIT_SUMMARY = new GlobalFITMessage(313, "SPLIT_SUMMARY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "split_type"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "num_splits"),
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "total_timer_time", 1000, 0), // s
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "total_distance", 100, 0), // m
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(8, BaseType.UINT16, "total_ascent"), // m
            new FieldDefinitionPrimitive(9, BaseType.UINT16, "total_descent"), // m
            new FieldDefinitionPrimitive(10, BaseType.UINT8, "avg_heart_rate"), // bpm
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "max_heart_rate"), // bpm
            new FieldDefinitionPrimitive(12, BaseType.SINT32, "avg_vert_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(13, BaseType.UINT32,  "total_calories"), // kcal
            new FieldDefinitionPrimitive(77, BaseType.UINT32,  "total_moving_time", 1000, 0), // s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage HSA_BODY_BATTERY_DATA = new GlobalFITMessage(314, "HSA_BODY_BATTERY_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // seconds
            new FieldDefinitionPrimitive(1, BaseType.SINT8, "level", FieldDefinitionFactory.FIELD.ARRAY), // %
            new FieldDefinitionPrimitive(2, BaseType.SINT16, "charged", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.SINT16, "uncharged", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_EVENT = new GlobalFITMessage(315, "HSA_EVENT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "event_id"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage CLIMB_PRO = new GlobalFITMessage(317, "CLIMB_PRO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.SINT32, "position_lat", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "position_long", FieldDefinitionFactory.FIELD.COORDINATE),
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "climb_pro_event"),
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "climb_number"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "climb_category"),
            new FieldDefinitionPrimitive(5, BaseType.FLOAT32, "current_dist"), // m
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TANK_UPDATE = new GlobalFITMessage(319, "TANK_UPDATE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32Z, "sensor"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "pressure", 100, 0), // bar
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TANK_SUMMARY = new GlobalFITMessage(323, "TANK_SUMMARY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32Z, "sensor"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "start_pressure", 100, 0), // bar
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "end_pressure", 100, 0), // bar
            new FieldDefinitionPrimitive(3, BaseType.UINT32, "volume_used", 100, 0), // liter
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage GPS_EVENT = new GlobalFITMessage(326, "GPS_EVENT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "event_type"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "data"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage ECG_SUMMARY = new GlobalFITMessage(336, "ECG_SUMMARY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "unknown_0"), // 10103
            new FieldDefinitionPrimitive(1, BaseType.ENUM, "unknown_1"), // 3
            new FieldDefinitionPrimitive(2, BaseType.FLOAT32, "unknown_2"), // 512
            new FieldDefinitionPrimitive(3, BaseType.FLOAT32, "unknown_3"), // 128
            new FieldDefinitionPrimitive(4, BaseType.UINT32, "ecg_timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "unknown_6"), // 1
            new FieldDefinitionPrimitive(7, BaseType.FLOAT32, "average_heart_rate"), // bpm
            new FieldDefinitionPrimitive(10, BaseType.STRING, "unknown_10"), // ?
            new FieldDefinitionPrimitive(11, BaseType.UINT16, "unknown_11"), // 30
            new FieldDefinitionPrimitive(12, BaseType.UINT8, "unknown_12") // 39
    ));

    public static GlobalFITMessage ECG_RAW_SAMPLE = new GlobalFITMessage(337, "ECG_RAW_SAMPLE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.FLOAT32, "value")
    ));

    public static GlobalFITMessage ECG_SMOOTH_SAMPLE = new GlobalFITMessage(338, "ECG_SMOOTH_SAMPLE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.FLOAT32, "value")
    ));

    public static GlobalFITMessage SLEEP_STATS = new GlobalFITMessage(346, "SLEEP_STATS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "combined_awake_score"),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "awake_time_score"),
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "awakenings_count_score"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "deep_sleep_score"),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "sleep_duration_score"),
            new FieldDefinitionPrimitive(5, BaseType.UINT8, "light_sleep_score"),
            new FieldDefinitionPrimitive(6, BaseType.UINT8, "overall_sleep_score"),
            new FieldDefinitionPrimitive(7, BaseType.UINT8, "sleep_quality_score"),
            new FieldDefinitionPrimitive(8, BaseType.UINT8, "sleep_recovery_score"),
            new FieldDefinitionPrimitive(9, BaseType.UINT8, "rem_sleep_score"),
            new FieldDefinitionPrimitive(10, BaseType.UINT8, "sleep_restlessness_score"),
            new FieldDefinitionPrimitive(11, BaseType.UINT8, "awakenings_count"),
            new FieldDefinitionPrimitive(12, BaseType.ENUM, "unk_12"),
            new FieldDefinitionPrimitive(13, BaseType.ENUM, "unk_13"),
            new FieldDefinitionPrimitive(14, BaseType.UINT8, "interruptions_score"),
            new FieldDefinitionPrimitive(15, BaseType.UINT16, "average_stress_during_sleep", 100, 0),
            new FieldDefinitionPrimitive(16, BaseType.ENUM, "unk_16")
    ));

    public static GlobalFITMessage HRV_SUMMARY = new GlobalFITMessage(370, "HRV_SUMMARY", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "weekly_average", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "last_night_average", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "last_night_5_min_high", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "baseline_low_upper", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "baseline_balanced_lower", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(5, BaseType.UINT16, "baseline_balanced_upper", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "status", FieldDefinitionFactory.FIELD.HRV_STATUS),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HRV_VALUE = new GlobalFITMessage(371, "HRV_VALUE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "value", 128, 0), // milliseconds, scaled by 128
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage RAW_BBI = new GlobalFITMessage(372, "RAW_BBI", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(2, BaseType.UINT16, "time", FieldDefinitionFactory.FIELD.ARRAY), // ms
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "quality", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.UINT8, "gap", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage DEVICE_AUX_BATTERY_INFO = new GlobalFITMessage(375, "DEVICE_AUX_BATTERY_INFO", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT8, "device_index"),
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "battery_voltage", 256, 0), // V
            new FieldDefinitionPrimitive(2, BaseType.UINT8, "battery_status"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "battery_identifier"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_GYROSCOPE_DATA = new GlobalFITMessage(376, "HSA_GYROSCOPE_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "timestamp_ms"), // ms
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "sampling_interval"),
            new FieldDefinitionPrimitive(2, BaseType.SINT16, "gyro_x", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(3, BaseType.SINT16, "gyro_y", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(4, BaseType.SINT16, "gyro_z", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "timestamp_32k"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SKIN_TEMP_RAW = new GlobalFITMessage(397, "SKIN_TEMP_RAW", Arrays.asList(
            new FieldDefinitionPrimitive(1, BaseType.FLOAT32, "deviation"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage TRAINING_LOAD = new GlobalFITMessage(378, "TRAINING_LOAD", Arrays.asList(
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "training_load_acute"),
            new FieldDefinitionPrimitive(4, BaseType.UINT16, "training_load_chronic"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SLEEP_SCHEDULE = new GlobalFITMessage(379, "SLEEP_SCHEDULE", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "bed_time"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "wake_time"),
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage CHRONO_SHOT_SESSION = new GlobalFITMessage(387, "CHRONO_SHOT_SESSION", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "min_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "max_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "avg_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(3, BaseType.UINT16, "shot_count"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "projectile_type"),
            new FieldDefinitionPrimitive(5, BaseType.UINT32, "grain_weight", 10, 0), // g
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "standard_deviation", 1000, 0), // m/s
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage CHRONO_SHOT_DATA = new GlobalFITMessage(388, "CHRONO_SHOT_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "shot_speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "shot_num"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_CONFIGURATION_DATA = new GlobalFITMessage(389, "HSA_CONFIGURATION_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.BASE_TYPE_BYTE, "data", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(1, BaseType.UINT8, "data_size"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage DIVE_APNEA_ALARM = new GlobalFITMessage(393, "DIVE_APNEA_ALARM", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "depth", 1000, 0), // m
            new FieldDefinitionPrimitive(1, BaseType.SINT32, "time"), // s
            new FieldDefinitionPrimitive(2, BaseType.ENUM, "enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(3, BaseType.ENUM, "alarm_type"),
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "sound"),
            new FieldDefinitionPrimitive(5, BaseType.ENUM, "dive_types", FieldDefinitionFactory.FIELD.ARRAY),
            new FieldDefinitionPrimitive(6, BaseType.UINT32, "id"),
            new FieldDefinitionPrimitive(7, BaseType.ENUM, "popup_enabled", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(8, BaseType.ENUM, "trigger_on_descent", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(9, BaseType.ENUM, "trigger_on_ascent", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(10, BaseType.ENUM, "repeating", FieldDefinitionFactory.FIELD.BOOLEAN),
            new FieldDefinitionPrimitive(11, BaseType.SINT32, "speed", 1000, 0), // m/s
            new FieldDefinitionPrimitive(254, BaseType.UINT16, "message_index")
    ));

    public static GlobalFITMessage CPE_STATUS = new GlobalFITMessage(394, "CPE_STATUS", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.ENUM, "status"),
            new FieldDefinitionPrimitive(1, BaseType.UINT32, "start_time"),
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "end_time"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage SKIN_TEMP_OVERNIGHT = new GlobalFITMessage(398, "SKIN_TEMP_OVERNIGHT", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "local_timestamp"), // garmin timestamp, but in user timezone
            new FieldDefinitionPrimitive(1, BaseType.FLOAT32, "average_deviation"),
            new FieldDefinitionPrimitive(2, BaseType.FLOAT32, "average_7_day_deviation"),
            new FieldDefinitionPrimitive(3, BaseType.UINT8, "unk3"),
            new FieldDefinitionPrimitive(4, BaseType.FLOAT32, "nightly_value"),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage HSA_WRIST_TEMPERATURE_DATA = new GlobalFITMessage(409, "HSA_WRIST_TEMPERATURE_DATA", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT16, "processing_interval"), // s
            new FieldDefinitionPrimitive(1, BaseType.UINT16, "value", FieldDefinitionFactory.FIELD.ARRAY, 1000, 0), // °C
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static GlobalFITMessage NAP = new GlobalFITMessage(412, "NAP", Arrays.asList(
            new FieldDefinitionPrimitive(0, BaseType.UINT32, "start_timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(1, BaseType.SINT16, "unknown_1"), // 0
            new FieldDefinitionPrimitive(2, BaseType.UINT32, "end_timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(3, BaseType.SINT16, "unknown_3"), // 0
            new FieldDefinitionPrimitive(4, BaseType.ENUM, "unknown_4"), // 8
            new FieldDefinitionPrimitive(6, BaseType.ENUM, "unknown_6"), // 0
            new FieldDefinitionPrimitive(7, BaseType.UINT32, "timestamp_7", FieldDefinitionFactory.FIELD.TIMESTAMP),
            new FieldDefinitionPrimitive(253, BaseType.UINT32, "timestamp", FieldDefinitionFactory.FIELD.TIMESTAMP)
    ));

    public static Map<Integer, GlobalFITMessage> KNOWN_MESSAGES = new HashMap<>() {{
        put(0, FILE_ID);
        put(1, CAPABILITIES);
        put(2, DEVICE_SETTINGS);
        put(3, USER_PROFILE);
        put(4, HRM_PROFILE);
        put(5, SDM_PROFILE);
        put(6, BIKE_PROFILE);
        put(7, ZONES_TARGET);
        put(8, HR_ZONE);
        put(9, POWER_ZONE);
        put(10, MET_ZONE);
        put(12, SPORT);
        put(13, TRAINING_SETTINGS);
        put(15, GOALS);
        put(18, SESSION);
        put(19, LAP);
        put(20, RECORD);
        put(21, EVENT);
        put(22, DEVICE_USED);
        put(23, DEVICE_INFO);
        put(26, WORKOUT);
        put(27, WORKOUT_STEP);
        put(28, SCHEDULE);
        put(29, LOCATION);
        put(30, WEIGHT_SCALE);
        put(31, COURSE);
        put(32, COURSE_POINT);
        put(33, TOTALS);
        put(34, ACTIVITY);
        put(35, SOFTWARE);
        put(37, FILE_CAPABILITIES);
        put(38, MESG_CAPABILITIES);
        put(39, FIELD_CAPABILITIES);
        put(49, FILE_CREATOR);
        put(51, BLOOD_PRESSURE);
        put(53, SPEED_ZONE);
        put(55, MONITORING);
        put(70, MAP_LAYER);
        put(72, TRAINING_FILE);
        put(78, HRV);
        put(79, USER_METRICS);
        put(80, ANT_RX);
        put(81, ANT_TX);
        put(82, ANT_CHANNEL_ID);
        put(101, LENGTH);
        put(103, MONITORING_INFO);
        put(104, DEVICE_STATUS);
        put(105, PAD);
        put(106, SLAVE_DEVICE);
        put(127, CONNECTIVITY);
        put(128, WEATHER);
        put(129, WEATHER_ALERT);
        put(131, CADENCE_ZONE);
        put(132, HR);
        put(140, PHYSIOLOGICAL_METRICS);
        put(141, EPO_STATUS);
        put(142, SEGMENT_LAP);
        put(145, MEMO_GLOB);
        put(147, SENSOR_SETTINGS);
        put(148, SEGMENT_ID);
        put(149, SEGMENT_LEADERBOARD_ENTRY);
        put(150, SEGMENT_POINT);
        put(151, SEGMENT_FILE);
        put(158, WORKOUT_SESSION);
        put(159, WATCHFACE_SETTINGS);
        put(160, GPS_METADATA);
        put(161, CAMERA_EVENT);
        put(162, TIMESTAMP_CORRELATION);
        put(164, GYROSCOPE_DATA);
        put(165, ACCELEROMETER_DATA);
        put(167, THREE_D_SENSOR_CALIBRATION);
        put(169, VIDEO_FRAME);
        put(174, OBDII_DATA);
        put(177, NMEA_SENTENCE);
        put(178, AVIATION_ATTITUDE);
        put(184, VIDEO);
        put(185, VIDEO_TITLE);
        put(186, VIDEO_DESCRIPTION);
        put(187, VIDEO_CLIP);
        put(188, OHR_SETTINGS);
        put(200, EXD_SCREEN_CONFIGURATION);
        put(201, EXD_DATA_FIELD_CONFIGURATION);
        put(202, EXD_DATA_CONCEPT_CONFIGURATION);
        put(206, FIELD_DESCRIPTION);
        put(207, DEVELOPER_DATA);
        put(208, MAGNETOMETER_DATA);
        put(209, BAROMETER_DATA);
        put(210, ONE_D_SENSOR_CALIBRATION);
        put(211, MONITORING_HR_DATA);
        put(216, TIME_IN_ZONE);
        put(222, ALARM_SETTINGS);
        put(225, SET);
        put(227, STRESS_LEVEL);
        put(229, MAX_MET_DATA);
        put(258, DIVE_SETTINGS);
        put(259, DIVE_GAS);
        put(262, DIVE_ALARM);
        put(264, EXERCISE_TITLE);
        put(269, SPO2);
        put(273, SLEEP_DATA_INFO);
        put(274, SLEEP_DATA_RAW);
        put(268, DIVE_SUMMARY);
        put(275, SLEEP_STAGE);
        put(285, JUMP);
        put(289, AAD_ACCEL_FEATURES);
        put(290, BEAT_INTERVALS);
        put(297, RESPIRATION_RATE);
        put(302, HSA_ACCELEROMETER_DATA);
        put(304, HSA_STEP_DATA);
        put(305, HSA_SPO2_DATA);
        put(306, HSA_STRESS_DATA);
        put(307, HSA_RESPIRATION_DATA);
        put(308, HSA_HEART_RATE_DATA);
        put(312, SPLIT);
        put(313, SPLIT_SUMMARY);
        put(314, HSA_BODY_BATTERY_DATA);
        put(315, HSA_EVENT);
        put(317, CLIMB_PRO);
        put(319, TANK_UPDATE);
        put(323, TANK_SUMMARY);
        put(326, GPS_EVENT);
        put(336, ECG_SUMMARY);
        put(337, ECG_RAW_SAMPLE);
        put(338, ECG_SMOOTH_SAMPLE);
        put(346, SLEEP_STATS);
        put(370, HRV_SUMMARY);
        put(371, HRV_VALUE);
        put(372, RAW_BBI);
        put(375, DEVICE_AUX_BATTERY_INFO);
        put(376, HSA_GYROSCOPE_DATA);
        put(378, TRAINING_LOAD);
        put(379, SLEEP_SCHEDULE);
        put(387, CHRONO_SHOT_SESSION);
        put(388, CHRONO_SHOT_DATA);
        put(389, HSA_CONFIGURATION_DATA);
        put(393, DIVE_APNEA_ALARM);
        put(394, CPE_STATUS);
        put(397, SKIN_TEMP_RAW);
        put(398, SKIN_TEMP_OVERNIGHT);
        put(409, HSA_WRIST_TEMPERATURE_DATA);
        put(412, NAP);
    }};

    private final int number;
    private final String name;

    private final List<FieldDefinitionPrimitive> fieldDefinitionPrimitives;

    GlobalFITMessage(int number, String name, List<FieldDefinitionPrimitive> fieldDefinitionPrimitives) {
        this.number = number;
        this.name = name;
        this.fieldDefinitionPrimitives = fieldDefinitionPrimitives;
    }

    public static GlobalFITMessage fromNumber(final int number) {
        final GlobalFITMessage found = KNOWN_MESSAGES.get(number);
        if (found != null) {
            return found;
        }

        return new GlobalFITMessage(number, "UNK_" + FitDebug.mesgNumLookup(number), null);
    }

    public String name() {
        return this.name;
    }

    public int getNumber() {
        return number;
    }

    public List<FieldDefinitionPrimitive> getFieldDefinitionPrimitives() {
        return fieldDefinitionPrimitives;
    }

    @Nullable
    public List<FieldDefinition> getFieldDefinitions(int... ids) {
        if (null == fieldDefinitionPrimitives)
            return null;
        List<FieldDefinition> subset = new ArrayList<>(ids.length);
        for (int id :
                ids) {
            for (FieldDefinitionPrimitive fieldDefinitionPrimitive :
                    fieldDefinitionPrimitives) {
                if (fieldDefinitionPrimitive.number == id) {
                    subset.add(FieldDefinitionFactory.create(
                            fieldDefinitionPrimitive.number,
                            fieldDefinitionPrimitive.size,
                            fieldDefinitionPrimitive.type,
                            fieldDefinitionPrimitive.baseType,
                            fieldDefinitionPrimitive.name,
                            fieldDefinitionPrimitive.scale,
                            fieldDefinitionPrimitive.offset
                    ));
                }
            }
        }
        return subset;
    }

    public FieldDefinition getFieldDefinition(final String name, final int count) {
        for (FieldDefinitionPrimitive fieldDefinitionPrimitive :
                fieldDefinitionPrimitives) {
            if (name.equals(fieldDefinitionPrimitive.name)) {
                return FieldDefinitionFactory.create(
                        fieldDefinitionPrimitive.number,
                        fieldDefinitionPrimitive.size * count,
                        fieldDefinitionPrimitive.type,
                        fieldDefinitionPrimitive.baseType,
                        fieldDefinitionPrimitive.name,
                        fieldDefinitionPrimitive.scale,
                        fieldDefinitionPrimitive.offset
                );
            }
        }

        throw new IllegalArgumentException("Unknown field name " + name);
    }

    @Nullable
    public FieldDefinition getFieldDefinition(int id, int size) {
        if (null == fieldDefinitionPrimitives)
            return null;
        for (GlobalFITMessage.FieldDefinitionPrimitive fieldDefinitionPrimitive :
                fieldDefinitionPrimitives) {
            if (fieldDefinitionPrimitive.number == id) {
                return FieldDefinitionFactory.create(
                        fieldDefinitionPrimitive.number,
                        size,
                        fieldDefinitionPrimitive.type,
                        fieldDefinitionPrimitive.baseType,
                        fieldDefinitionPrimitive.name,
                        fieldDefinitionPrimitive.scale,
                        fieldDefinitionPrimitive.offset
                );
            }
        }
        return null;
    }

    public static class FieldDefinitionPrimitive {
        private final int number;
        private final BaseType baseType;
        private final String name;
        private final FieldDefinitionFactory.FIELD type;
        private final int scale;
        private final int offset;
        private final int size;

        public FieldDefinitionPrimitive(int number, BaseType baseType, int size, String name, FieldDefinitionFactory.FIELD type, int scale, int offset) {
            this.number = number;
            this.baseType = baseType;
            this.size = size;
            this.name = name;
            this.type = type;
            this.scale = scale;
            this.offset = offset;
        }

        public FieldDefinitionPrimitive(int number, BaseType baseType, String name, FieldDefinitionFactory.FIELD type, int scale, int offset) {
            this(number, baseType, baseType.getSize(), name, type, scale, offset);
        }

        public FieldDefinitionPrimitive(int number, BaseType baseType, String name, FieldDefinitionFactory.FIELD type) {
            this(number, baseType, baseType.getSize(), name, type, 1, 0);
        }

        public FieldDefinitionPrimitive(int number, BaseType baseType, String name) {
            this(number, baseType, baseType.getSize(), name, null, 1, 0);
        }

        public FieldDefinitionPrimitive(int number, BaseType baseType, int size, String name) {
            this(number, baseType, size, name, null, 1, 0);
        }

        public FieldDefinitionPrimitive(int number, BaseType baseType, String name, int scale, int offset) {
            this(number, baseType, baseType.getSize(), name, null, scale, offset);
        }

        public int getNumber() {
            return number;
        }

        public BaseType getBaseType() {
            return baseType;
        }

        public String getName() {
            return name;
        }

        public FieldDefinitionFactory.FIELD getType() {
            return type;
        }

        public int getScale() {
            return scale;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }
    }
}
