package nodomain.freeyourgadget.gadgetbridge.model.weather

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.R

object WeatherMapper {

    @JvmStatic
    fun mapToOpenWeatherMapIcon(code: Int): String = when {
        //see https://openweathermap.org/weather-conditions
        code in 200..299 -> "11d"
        code in 300..499 -> "09d"
        code in 500..509 -> "10d"
        code in 511..599 -> "09d"
        code in 600..699 -> "13d"
        code in 700..799 -> "50d"
        code == 800 -> "01d" //TODO: night?
        code == 801 -> "02d" //TODO: night?
        code == 802 -> "03d" //TODO: night?
        code == 803 || code == 804 -> "04d" //TODO: night?
        else -> "02d" // fallback
    }

    @JvmStatic
    fun mapToOpenWeatherMapCondition(yahooCondition: Int): Int {
        //yahoo weather conditions:
//https://developer.yahoo.com/weather/documentation.html
        return when (yahooCondition) {
            0 -> 900
            1 -> 901
            2 -> 962
            3 -> 212
            4 -> 211
            5, 6 -> 616
            7 -> 600
            8, 9 -> 301
            10 -> 511
            11, 12 -> 521
            13, 14 -> 620
            15, 41, 42, 43, 46 -> 602
            16 -> 601
            17, 35 -> 906
            18 -> 611
            19 -> 761
            20 -> 741
            21 -> 721
            22 -> 711
            23, 24 -> 905
            25 -> 903
            26, 27, 28 -> 804
            29, 30 -> 801
            31, 32 -> 800
            33, 34 -> 801
            36 -> 904
            37, 38, 39 -> 210
            40 -> 520
            44 -> 801
            45, 47 -> 211
            3200 -> -1
            else -> -1
        }
    }

    @JvmStatic
    fun getConditionString(context: Context, code: Int): String = when (code) {
        200 -> context.getString(R.string.weather_condition_thunderstorm_with_light_rain)
        201 -> context.getString(R.string.weather_condition_thunderstorm_with_rain)
        202 -> context.getString(R.string.weather_condition_thunderstorm_with_heavy_rain)
        210 -> context.getString(R.string.weather_condition_light_thunderstorm)
        211 -> context.getString(R.string.weather_condition_thunderstorm)
        230 -> context.getString(R.string.weather_condition_thunderstorm_with_light_drizzle)
        231 -> context.getString(R.string.weather_condition_thunderstorm_with_drizzle)
        232 -> context.getString(R.string.weather_condition_thunderstorm_with_heavy_drizzle)
        212 -> context.getString(R.string.weather_condition_heavy_thunderstorm)
        221 -> context.getString(R.string.weather_condition_ragged_thunderstorm)
        // Group 3xx: Drizzle
        300 -> context.getString(R.string.weather_condition_light_intensity_drizzle)
        301 -> context.getString(R.string.weather_condition_drizzle)
        302 -> context.getString(R.string.weather_condition_heavy_intensity_drizzle)
        310 -> context.getString(R.string.weather_condition_light_intensity_drizzle_rain)
        311 -> context.getString(R.string.weather_condition_drizzle_rain)
        312 -> context.getString(R.string.weather_condition_heavy_intensity_drizzle_rain)
        313 -> context.getString(R.string.weather_condition_shower_rain_and_drizzle)
        314 -> context.getString(R.string.weather_condition_heavy_shower_rain_and_drizzle)
        321 -> context.getString(R.string.weather_condition_shower_drizzle)
        // Group 5xx: Rain
        500 -> context.getString(R.string.weather_condition_light_rain)
        501 -> context.getString(R.string.weather_condition_moderate_rain)
        502 -> context.getString(R.string.weather_condition_heavy_intensity_rain)
        503 -> context.getString(R.string.weather_condition_very_heavy_rain)
        504 -> context.getString(R.string.weather_condition_extreme_rain)
        511 -> context.getString(R.string.weather_condition_freezing_rain)
        520 -> context.getString(R.string.weather_condition_light_intensity_shower_rain)
        521 -> context.getString(R.string.weather_condition_shower_rain)
        522 -> context.getString(R.string.weather_condition_heavy_intensity_shower_rain)
        531 -> context.getString(R.string.weather_condition_ragged_shower_rain)
        // Group 6xx: Snow
        600 -> context.getString(R.string.weather_condition_light_snow)
        601 -> context.getString(R.string.weather_condition_snow)
        602 -> context.getString(R.string.weather_condition_heavy_snow)
        611 -> context.getString(R.string.weather_condition_sleet)
        612 -> context.getString(R.string.weather_condition_shower_sleet)
        615 -> context.getString(R.string.weather_condition_light_rain_and_snow)
        616 -> context.getString(R.string.weather_condition_rain_and_snow)
        620 -> context.getString(R.string.weather_condition_light_shower_snow)
        621 -> context.getString(R.string.weather_condition_shower_snow)
        622 -> context.getString(R.string.weather_condition_heavy_shower_snow)
        // Group 7xx: Atmosphere
        701 -> context.getString(R.string.weather_condition_mist)
        711 -> context.getString(R.string.weather_condition_smoke)
        721 -> context.getString(R.string.weather_condition_haze)
        731 -> context.getString(R.string.weather_condition_sandcase_dust_whirls)
        741 -> context.getString(R.string.weather_condition_fog)
        751 -> context.getString(R.string.weather_condition_sand)
        761 -> context.getString(R.string.weather_condition_dust)
        762 -> context.getString(R.string.weather_condition_volcanic_ash)
        771 -> context.getString(R.string.weather_condition_squalls)
        781, 900 -> context.getString(R.string.weather_condition_tornado)
        // Group 80x: Clouds
        800 -> context.getString(R.string.weather_condition_clear_sky)
        801 -> context.getString(R.string.weather_condition_few_clouds)
        802 -> context.getString(R.string.weather_condition_scattered_clouds)
        803 -> context.getString(R.string.weather_condition_broken_clouds)
        804 -> context.getString(R.string.weather_condition_overcast_clouds)
        // Group 90x: Extreme
        901 -> context.getString(R.string.weather_condition_tropical_storm)
        902, 962 -> context.getString(R.string.weather_condition_hurricane)
        903 -> context.getString(R.string.weather_condition_cold)
        904 -> context.getString(R.string.weather_condition_hot)
        905 -> context.getString(R.string.weather_condition_windy)
        906 -> context.getString(R.string.weather_condition_hail)
        // Group 9xx: Additional
        951 -> context.getString(R.string.weather_condition_calm)
        952 -> context.getString(R.string.weather_condition_light_breeze)
        953 -> context.getString(R.string.weather_condition_gentle_breeze)
        954 -> context.getString(R.string.weather_condition_moderate_breeze)
        955 -> context.getString(R.string.weather_condition_fresh_breeze)
        956 -> context.getString(R.string.weather_condition_strong_breeze)
        957 -> context.getString(R.string.weather_condition_high_windcase_near_gale)
        958 -> context.getString(R.string.weather_condition_gale)
        959 -> context.getString(R.string.weather_condition_severe_gale)
        960 -> context.getString(R.string.weather_condition_storm)
        961 -> context.getString(R.string.weather_condition_violent_storm)

        else -> ""
    }

    @JvmStatic
    fun getAqiLevelString(context: Context, aqi: Int): String = when {
        // Uses the [2023 Plume index](https://plumelabs.files.wordpress.com/2023/06/plume_aqi_2023.pdf) as a reference
        aqi < 0   -> context.getString(R.string.aqi_level_unknown)
        aqi < 20  -> context.getString(R.string.aqi_level_excellent)
        aqi < 50  -> context.getString(R.string.aqi_level_fair)
        aqi < 100 -> context.getString(R.string.aqi_level_poor)
        aqi < 150 -> context.getString(R.string.aqi_level_unhealthy)
        aqi < 250 -> context.getString(R.string.aqi_level_very_unhealthy)
        else      -> context.getString(R.string.aqi_level_dangerous)
    }

    @JvmStatic
    fun mapToPebbleCondition(openWeatherMapCondition: Int): Byte {
        /* deducted values:
    0 = sun + cloud
    1 = clouds
    2 = some snow
    3 = some rain
    4 = heavy rain
    5 = heavy snow
    6 = sun + cloud + rain (default icon?)
    7 = sun
    8 = rain + snow
    9 = 6
    10, 11, ... = empty icon
 */
        return when (openWeatherMapCondition) {
            200, 201, 202, 210, 211, 230, 231, 232, 212, 221 -> 4
            300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501 -> 3
            502, 503, 504, 511, 520, 521, 522, 531 -> 4
            600, 601, 620 -> 2
            602, 611, 612, 621, 622 -> 5
            615, 616 -> 8
            701, 711, 721, 731, 741, 751, 761, 762, 771, 781, 900 -> 6
            800 -> 7
            801, 802, 803, 804 -> 0
            901, 903, 904, 905, 906, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 902, 962 -> 6

            else -> 6

        }
    }

    @JvmStatic
    fun mapToYahooCondition(openWeatherMapCondition: Int): Int {
        // openweathermap.org conditions:
        // http://openweathermap.org/weather-conditions
        return when (openWeatherMapCondition) {
            200, 201, 202, 210, 211, 230, 231, 232 -> 4
            212, 221 -> 3
            300, 301, 302, 310, 311, 312 -> 9
            313, 314, 321 -> 11
            500, 501, 502, 503, 504, 511 -> 10
            520 -> 40
            521, 522, 531 -> 12
            600 -> 7
            601 -> 16
            602 -> 15
            611, 612 -> 18
            615, 616 -> 5
            620 -> 14
            621 -> 46
            622, 701, 711 -> 22
            721 -> 21
            731 -> 3200
            741 -> 20
            751, 761 -> 19
            762, 771 -> 3200
            781, 900 -> 0
            800 -> 32
            801, 802 -> 34
            803, 804 -> 44
            901 -> 1
            903 -> 25
            904 -> 36
            905 -> 24
            906 -> 17
            951, 952, 953, 954, 955 -> 34
            956, 957 -> 24
            958, 959, 960, 961 -> 3200
            902, 962 -> 2
            else -> 3200

        }
    }

    @JvmStatic
    fun mapToZeTimeConditionOld(openWeatherMapCondition: Int): Byte {
        /* deducted values:
    0 = partly cloudy
    1 = cloudy
    2 = sunny
    3 = windy/gale
    4 = heavy rain
    5 = snowy
    6 = storm
 */
        return when (openWeatherMapCondition) {
            200, 201, 202, 210, 211, 230, 231, 232, 212, 221, 771, 781, 900, 901, 960, 961, 902, 962 -> 6
            300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 511, 520, 521, 522, 531, 906 -> 4
            600, 601, 620, 602, 611, 612, 621, 622, 615, 616, 903 -> 5
            701, 711, 721, 731, 741, 751, 761, 762 -> 1
            800, 904 -> 2
            801, 802, 803, 804 -> 0

            905, 951, 952, 953, 954, 955, 956, 957, 958, 959 -> 3
            else -> 0

        }
    }

    @JvmStatic
    fun mapToZeTimeCondition(openWeatherMapCondition: Int): Byte {
        /* deducted values:
    0 = tornado
    1 = typhoon
    2 = hurricane
    3 = thunderstorm
    4 = rain and snow
    5 = unavailable
    6 = freezing rain
    7 = drizzle
    8 = showers
    9 = snow flurries
    10 = blowing snow
    11 = snow
    12 = sleet
    13 = foggy
    14 = windy
    15 = cloudy
    16 = partly cloudy (night)
    17 = partly cloudy (day)
    18 = clear night
    19 = sunny
    20 = thundershower
    21 = hot
    22 = scattered thunders
    23 = snow showers
    24 = heavy snow
 */
        return when (openWeatherMapCondition) {
            210 -> 22

            200, 201, 202, 230, 231, 232 -> 20

            211, 212, 221 -> 3

            781, 900 -> 0

            901 -> 1

            771, 960, 961, 902, 962 -> 2

            300, 301, 302, 310, 311, 312, 313, 314, 321 -> 7

            500, 501, 502, 503, 504, 520, 521, 522, 531, 906 -> 8

            511 -> 6

            620, 621, 622 -> 23

            615, 616 -> 4

            611, 612 -> 12

            600, 601 -> 11
            602 -> 24

            701, 711, 721, 731, 741, 751, 761, 762 -> 13

            800 -> 19

            904 -> 21

            801, 802, 803 -> 17

            804 -> 15

            905, 951, 952, 953, 954, 955, 956, 957, 958, 959 -> 14

            903 -> 5
            else -> 5
        }
    }

    @JvmStatic
    fun mapToCmfCondition(openWeatherMapCondition: Int): Byte {
        /* deducted values:
    1 = sunny
    2 = cloudy
    3 = overcast
    4 = showers
    5 = snow showers
    6 = fog

    9 = thunder showers

    14 = sleet

    19 = hot (extreme)
    20 = cold (extreme)

    21 = strong wind
    22 = (night) sunny - with moon
    23 = (night) sunny with stars
    24 = (night) cloudy - with moon
    25 = sun with haze
    26 = cloudy (sun with cloud)
 */
        return when (openWeatherMapCondition) {
            210, 200, 201, 202, 230, 231, 232, 211, 212, 221 -> 9

            901, 781, 900, 771, 960, 961, 902, 962 -> 21

            300, 301, 302, 310, 311, 312, 313, 314, 321, 500, 501, 502, 503, 504, 520, 521, 522, 531 -> 4

            906, 615, 616, 511 -> 14

            611, 612, 600, 601, 602, 620, 621, 622 -> 5


            701, 711, 721, 731, 741, 751, 761, 762 -> 6

            800 -> 1

            904 -> 19

            801, 802 -> 26
            803 -> 2

            804 -> 3

            905, 951, 952, 953, 954, 955, 956, 957, 958, 959 -> 21

            903 -> 20
            else -> 20
        }
    }

    @JvmStatic
    fun mapToFitProCondition(openWeatherMapCondition: Int): Byte {
        return when (openWeatherMapCondition) {
            100 -> 1
            104 -> 2
            101, 102, 103 -> 3
            305, 309 -> 4
            306, 314, 399 -> 5
            307, 308, 310, 311, 312, 315, 316, 317, 318 -> 6
            300, 301, 302, 303 -> 7
            400, 407 -> 8
            401, 408, 499 -> 9
            402, 403, 409, 410 -> 10
            404, 405, 406 -> 11
            500, 501, 502, 509, 510, 511, 512, 513, 514, 515 -> 12
            304, 313 -> 13
            503, 504, 507, 508 -> 14
            200, 201, 202, 203, 204 -> 15
            205, 206, 207, 208 -> 16
            209, 210, 211 -> 17
            212 -> 18
            231 -> 19
            else -> 3
        }
    }
}
