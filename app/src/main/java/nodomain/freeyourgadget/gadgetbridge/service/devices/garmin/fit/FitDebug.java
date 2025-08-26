/*  Copyright (C) 2023-2025 Hikmatulloh Hari Mukti, The FIT SDK for Go Authors, Thomas Kuehne

    This file is part of Gadgetbridge.

    Based on https://github.com/muktihari/fit/blob/master/cmd/fitconv/fitcsv/lookup_gen.go

    BSD 3-Clause License

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice, this
       list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation
       and/or other materials provided with the distribution.

    3. Neither the name of the copyright holder nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

public final class FitDebug {
    /// format a global number (FIT message number) for logging
    public static String mesgNumLookup(int globalNumber) {
        return switch (globalNumber) {
            case 0 -> "0_file_id";
            case 1 -> "1_capabilities";
            case 2 -> "2_device_settings";
            case 3 -> "3_user_profile";
            case 4 -> "4_hrm_profile";
            case 5 -> "5_sdm_profile";
            case 6 -> "6_bike_profile";
            case 7 -> "7_zones_target";
            case 8 -> "8_hr_zone";
            case 9 -> "9_power_zone";
            case 10 -> "10_met_zone";
            case 12 -> "12_sport";
            case 13 -> "13_training_settings";
            case 15 -> "15_goal";
            case 18 -> "18_session";
            case 19 -> "19_lap";
            case 20 -> "20_record";
            case 21 -> "21_event";
            case 23 -> "23_device_info";
            case 26 -> "26_workout";
            case 27 -> "27_workout_step";
            case 28 -> "28_schedule";
            case 30 -> "30_weight_scale";
            case 31 -> "31_course";
            case 32 -> "32_course_point";
            case 33 -> "33_totals";
            case 34 -> "34_activity";
            case 35 -> "35_software";
            case 37 -> "37_file_capabilities";
            case 38 -> "38_mesg_capabilities";
            case 39 -> "39_field_capabilities";
            case 49 -> "49_file_creator";
            case 51 -> "51_blood_pressure";
            case 53 -> "53_speed_zone";
            case 55 -> "55_monitoring";
            case 72 -> "72_training_file";
            case 78 -> "78_hrv";
            case 80 -> "80_ant_rx";
            case 81 -> "81_ant_tx";
            case 82 -> "82_ant_channel_id";
            case 101 -> "101_length";
            case 103 -> "103_monitoring_info";
            case 105 -> "105_pad";
            case 106 -> "106_slave_device";
            case 127 -> "127_connectivity";
            case 128 -> "128_weather_conditions";
            case 129 -> "129_weather_alert";
            case 131 -> "131_cadence_zone";
            case 132 -> "132_hr";
            case 142 -> "142_segment_lap";
            case 145 -> "145_memo_glob";
            case 148 -> "148_segment_id";
            case 149 -> "149_segment_leaderboard_entry";
            case 150 -> "150_segment_point";
            case 151 -> "151_segment_file";
            case 158 -> "158_workout_session";
            case 159 -> "159_watchface_settings";
            case 160 -> "160_gps_metadata";
            case 161 -> "161_camera_event";
            case 162 -> "162_timestamp_correlation";
            case 164 -> "164_gyroscope_data";
            case 165 -> "165_accelerometer_data";
            case 167 -> "167_three_d_sensor_calibration";
            case 169 -> "169_video_frame";
            case 174 -> "174_obdii_data";
            case 177 -> "177_nmea_sentence";
            case 178 -> "178_aviation_attitude";
            case 184 -> "184_video";
            case 185 -> "185_video_title";
            case 186 -> "186_video_description";
            case 187 -> "187_video_clip";
            case 188 -> "188_ohr_settings";
            case 200 -> "200_exd_screen_configuration";
            case 201 -> "201_exd_data_field_configuration";
            case 202 -> "202_exd_data_concept_configuration";
            case 206 -> "206_field_description";
            case 207 -> "207_developer_data_id";
            case 208 -> "208_magnetometer_data";
            case 209 -> "209_barometer_data";
            case 210 -> "210_one_d_sensor_calibration";
            case 211 -> "211_monitoring_hr_data";
            case 216 -> "216_time_in_zone";
            case 225 -> "225_set";
            case 227 -> "227_stress_level";
            case 229 -> "229_max_met_data";
            case 258 -> "258_dive_settings";
            case 259 -> "259_dive_gas";
            case 262 -> "262_dive_alarm";
            case 264 -> "264_exercise_title";
            case 268 -> "268_dive_summary";
            case 269 -> "269_spo2_data";
            case 275 -> "275_sleep_level";
            case 285 -> "285_jump";
            case 289 -> "289_aad_accel_features";
            case 290 -> "290_beat_intervals";
            case 297 -> "297_respiration_rate";
            case 302 -> "302_hsa_accelerometer_data";
            case 304 -> "304_hsa_step_data";
            case 305 -> "305_hsa_spo2_data";
            case 306 -> "306_hsa_stress_data";
            case 307 -> "307_hsa_respiration_data";
            case 308 -> "308_hsa_heart_rate_data";
            case 312 -> "312_split";
            case 313 -> "313_split_summary";
            case 314 -> "314_hsa_body_battery_data";
            case 315 -> "315_hsa_event";
            case 317 -> "317_climb_pro";
            case 319 -> "319_tank_update";
            case 323 -> "323_tank_summary";
            case 346 -> "346_sleep_assessment";
            case 370 -> "370_hrv_status_summary";
            case 371 -> "371_hrv_value";
            case 372 -> "372_raw_bbi";
            case 375 -> "375_device_aux_battery_info";
            case 376 -> "376_hsa_gyroscope_data";
            case 387 -> "387_chrono_shot_session";
            case 388 -> "388_chrono_shot_data";
            case 389 -> "389_hsa_configuration_data";
            case 393 -> "393_dive_apnea_alarm";
            case 398 -> "398_skin_temp_overnight";
            case 409 -> "409_hsa_wrist_temperature_data";
            default -> Integer.toString(globalNumber);
        };
    }
}