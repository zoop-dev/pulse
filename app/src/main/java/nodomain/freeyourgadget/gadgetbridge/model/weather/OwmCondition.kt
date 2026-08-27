package nodomain.freeyourgadget.gadgetbridge.model.weather

import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.R

/// https://openweathermap.org/api/weather-conditions
enum class OwmCondition(
    val code: Int,
    @StringRes val labelRes: Int
) {
    THUNDERSTORM_WITH_LIGHT_RAIN(200, R.string.weather_condition_thunderstorm_with_light_rain),
    THUNDERSTORM_WITH_RAIN(201, R.string.weather_condition_thunderstorm_with_rain),
    THUNDERSTORM_WITH_HEAVY_RAIN(202, R.string.weather_condition_thunderstorm_with_heavy_rain),
    LIGHT_THUNDERSTORM(210, R.string.weather_condition_light_thunderstorm),
    THUNDERSTORM(211, R.string.weather_condition_thunderstorm),
    HEAVY_THUNDERSTORM(212, R.string.weather_condition_heavy_thunderstorm),
    RAGGED_THUNDERSTORM(221, R.string.weather_condition_ragged_thunderstorm),
    THUNDERSTORM_WITH_LIGHT_DRIZZLE(230, R.string.weather_condition_thunderstorm_with_light_drizzle),
    THUNDERSTORM_WITH_DRIZZLE(231, R.string.weather_condition_thunderstorm_with_drizzle),
    THUNDERSTORM_WITH_HEAVY_DRIZZLE(232, R.string.weather_condition_thunderstorm_with_heavy_drizzle),

    // Group 3xx: Drizzle
    LIGHT_INTENSITY_DRIZZLE(300, R.string.weather_condition_light_intensity_drizzle),
    DRIZZLE(301, R.string.weather_condition_drizzle),
    HEAVY_INTENSITY_DRIZZLE(302, R.string.weather_condition_heavy_intensity_drizzle),
    LIGHT_INTENSITY_DRIZZLE_RAIN(310, R.string.weather_condition_light_intensity_drizzle_rain),
    DRIZZLE_RAIN(311, R.string.weather_condition_drizzle_rain),
    HEAVY_INTENSITY_DRIZZLE_RAIN(312, R.string.weather_condition_heavy_intensity_drizzle_rain),
    SHOWER_RAIN_AND_DRIZZLE(313, R.string.weather_condition_shower_rain_and_drizzle),
    HEAVY_SHOWER_RAIN_AND_DRIZZLE(314, R.string.weather_condition_heavy_shower_rain_and_drizzle),
    SHOWER_DRIZZLE(321, R.string.weather_condition_shower_drizzle),

    // Group 5xx: Rain
    LIGHT_RAIN(500, R.string.weather_condition_light_rain),
    MODERATE_RAIN(501, R.string.weather_condition_moderate_rain),
    HEAVY_INTENSITY_RAIN(502, R.string.weather_condition_heavy_intensity_rain),
    VERY_HEAVY_RAIN(503, R.string.weather_condition_very_heavy_rain),
    EXTREME_RAIN(504, R.string.weather_condition_extreme_rain),
    FREEZING_RAIN(511, R.string.weather_condition_freezing_rain),
    LIGHT_INTENSITY_SHOWER_RAIN(520, R.string.weather_condition_light_intensity_shower_rain),
    SHOWER_RAIN(521, R.string.weather_condition_shower_rain),
    HEAVY_INTENSITY_SHOWER_RAIN(522, R.string.weather_condition_heavy_intensity_shower_rain),
    RAGGED_SHOWER_RAIN(531, R.string.weather_condition_ragged_shower_rain),

    // Group 6xx: Snow
    LIGHT_SNOW(600, R.string.weather_condition_light_snow),
    SNOW(601, R.string.weather_condition_snow),
    HEAVY_SNOW(602, R.string.weather_condition_heavy_snow),
    SLEET(611, R.string.weather_condition_sleet),
    LIGHT_SHOWER_SLEET(612, R.string.weather_condition_light_shower_sleet),
    SHOWER_SLEET(613, R.string.weather_condition_shower_sleet),
    LIGHT_RAIN_AND_SNOW(615, R.string.weather_condition_light_rain_and_snow),
    RAIN_AND_SNOW(616, R.string.weather_condition_rain_and_snow),
    LIGHT_SHOWER_SNOW(620, R.string.weather_condition_light_shower_snow),
    SHOWER_SNOW(621, R.string.weather_condition_shower_snow),
    HEAVY_SHOWER_SNOW(622, R.string.weather_condition_heavy_shower_snow),

    // Group 7xx: Atmosphere
    MIST(701, R.string.weather_condition_mist),
    SMOKE(711, R.string.weather_condition_smoke),
    HAZE(721, R.string.weather_condition_haze),
    SAND_OR_DUST_WHIRLS(731, R.string.weather_condition_sandcase_dust_whirls),
    FOG(741, R.string.weather_condition_fog),
    SAND(751, R.string.weather_condition_sand),
    DUST(761, R.string.weather_condition_dust),
    VOLCANIC_ASH(762, R.string.weather_condition_volcanic_ash),
    SQUALLS(771, R.string.weather_condition_squalls),
    TORNADO(781, R.string.weather_condition_tornado),

    // Group 800: Clear
    CLEAR_SKY(800, R.string.weather_condition_clear_sky),

    // Group 80x: Clouds
    FEW_CLOUDS(801, R.string.weather_condition_few_clouds),
    SCATTERED_CLOUDS(802, R.string.weather_condition_scattered_clouds),
    BROKEN_CLOUDS(803, R.string.weather_condition_broken_clouds),
    OVERCAST_CLOUDS(804, R.string.weather_condition_overcast_clouds),

    // Group 90x: Extreme (not in the API anymore?)
    TORNADO_900(900, R.string.weather_condition_tornado),
    TROPICAL_STORM(901, R.string.weather_condition_tropical_storm),
    HURRICANE_902(902, R.string.weather_condition_hurricane),
    COLD(903, R.string.weather_condition_cold),
    HOT(904, R.string.weather_condition_hot),
    WINDY(905, R.string.weather_condition_windy),
    HAIL(906, R.string.weather_condition_hail),

    // Group 9xx: Additional (not in the API anymore?)
    CALM(951, R.string.weather_condition_calm),
    LIGHT_BREEZE(952, R.string.weather_condition_light_breeze),
    GENTLE_BREEZE(953, R.string.weather_condition_gentle_breeze),
    MODERATE_BREEZE(954, R.string.weather_condition_moderate_breeze),
    FRESH_BREEZE(955, R.string.weather_condition_fresh_breeze),
    STRONG_BREEZE(956, R.string.weather_condition_strong_breeze),
    HIGH_WINDCASE_NEAR_GALE(957, R.string.weather_condition_high_windcase_near_gale),
    GALE(958, R.string.weather_condition_gale),
    SEVERE_GALE(959, R.string.weather_condition_severe_gale),
    STORM(960, R.string.weather_condition_storm),
    VIOLENT_STORM(961, R.string.weather_condition_violent_storm),
    HURRICANE_962(962, R.string.weather_condition_hurricane),
    ;

    companion object {
        fun fromCode(code: Int): OwmCondition? {
            return entries.find { it.code == code }
        }
    }
}
