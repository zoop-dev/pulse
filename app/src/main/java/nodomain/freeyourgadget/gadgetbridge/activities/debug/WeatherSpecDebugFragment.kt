package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeatherSpecDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_empty, rootKey)

        @Suppress("DEPRECATION")
        val weatherSpec = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("weatherSpec", WeatherSpec::class.java)!!
        } else {
            arguments?.getParcelable<WeatherSpec>("weatherSpec")!!
        }
        val day = arguments?.getBoolean("days", false)
        val hour = arguments?.getBoolean("hours", false)

        preferenceScreen?.title = weatherSpec.location

        if (day == true) {
            showDays(weatherSpec)
        } else if (hour == true) {
            showHours(weatherSpec)
        } else {
            showMainWeather(weatherSpec)
        }
    }

    private fun addPreference(title: String, summary: Any?, onClickFunction: (() -> Unit)? = null) {
        addDynamicPref(preferenceScreen, title, summary?.toString() ?: "<null>") {
            onClickFunction?.invoke()
        }
    }

    private fun showMainWeather(weatherSpec: WeatherSpec) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT)

        addPreference("Location", weatherSpec.location)
        addPreference("Timestamp", String.format(
            "%s (%s)",
            sdf.format(Date(weatherSpec.timestamp * 1000L)),
            DateTimeUtils.formatDurationHoursMinutes(System.currentTimeMillis() - weatherSpec.timestamp * 1000L, TimeUnit.MILLISECONDS)
        ))
        addPreference("Current Temp", "${weatherSpec.currentTemp} K (${weatherSpec.currentTemp - 273} °C)")
        addPreference("Max Temp", "${weatherSpec.todayMaxTemp} K (${weatherSpec.todayMaxTemp - 273} °C)")
        addPreference("Min Temp", "${weatherSpec.todayMinTemp} K (${weatherSpec.todayMinTemp - 273} °C)")
        addPreference("Condition", weatherSpec.currentCondition)
        addPreference("Condition Code", weatherSpec.currentConditionCode)
        addPreference("Humidity", weatherSpec.currentHumidity)
        addPreference("Wind Speed", "${weatherSpec.windSpeed} kmph")
        addPreference("Wind Direction", "${weatherSpec.windDirection} deg")
        addPreference("UV Index", weatherSpec.uvIndex)
        addPreference("Precip Probability", "${weatherSpec.precipProbability} %")
        addPreference("Dew Point", "${weatherSpec.dewPoint} K (${weatherSpec.dewPoint - 273} °C)")
        addPreference("Pressure", "${weatherSpec.pressure} mb")
        addPreference("Cloud Cover", "${weatherSpec.cloudCover} %")
        addPreference("Visibility", "${weatherSpec.visibility} m")
        addPreference("Sun Rise", sdf.format(Date(weatherSpec.sunRise * 1000L)))
        addPreference("Sun Set", sdf.format(Date(weatherSpec.sunSet * 1000L)))
        addPreference("Moon Rise", sdf.format(Date(weatherSpec.moonRise * 1000L)))
        addPreference("Moon Set", sdf.format(Date(weatherSpec.moonSet * 1000L)))
        addPreference("Moon Phase", "${weatherSpec.moonPhase} deg")
        addPreference("Latitude", weatherSpec.latitude)
        addPreference("Longitude", weatherSpec.longitude)
        addPreference("Feels Like Temp", "${weatherSpec.feelsLikeTemp} K (${weatherSpec.feelsLikeTemp - 273} °C)")
        addPreference("Is Current Location", weatherSpec.getIsCurrentLocation())

        if (weatherSpec.airQuality != null) {
            addPreference("Air Quality aqi", weatherSpec.airQuality!!.aqi)
            addPreference("Air Quality co", weatherSpec.airQuality!!.co)
            addPreference("Air Quality no2", weatherSpec.airQuality!!.no2)
            addPreference("Air Quality o3", weatherSpec.airQuality!!.o3)
            addPreference("Air Quality pm10", weatherSpec.airQuality!!.pm10)
            addPreference("Air Quality pm25", weatherSpec.airQuality!!.pm25)
            addPreference("Air Quality so2", weatherSpec.airQuality!!.so2)
            addPreference("Air Quality coAqi", weatherSpec.airQuality!!.coAqi)
            addPreference("Air Quality no2Aqi", weatherSpec.airQuality!!.no2Aqi)
            addPreference("Air Quality o3Aqi", weatherSpec.airQuality!!.o3Aqi)
            addPreference("Air Quality pm10Aqi", weatherSpec.airQuality!!.pm10Aqi)
            addPreference("Air Quality pm25Aqi", weatherSpec.airQuality!!.pm25Aqi)
            addPreference("Air Quality so2Aqi", weatherSpec.airQuality!!.so2Aqi)
        } else {
            addPreference("Air Quality", null)
        }

        addPreference("Daily Forecasts", "${weatherSpec.forecasts.size} entries") {
            if (weatherSpec.forecasts.isNotEmpty()) {
                goTo(
                    WeatherSpecDebugFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("weatherSpec", weatherSpec)
                            putBoolean("days", true)
                        }
                    }
                )
            }
        }

        addPreference("Hourly Forecasts", "${weatherSpec.hourly.size} entries") {
            if (weatherSpec.hourly.isNotEmpty()) {
                goTo(
                    WeatherSpecDebugFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("weatherSpec", weatherSpec)
                            putBoolean("hours", true)
                        }
                    }
                )
            }
        }
    }

    private fun showDays(weatherSpec: WeatherSpec) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT)

        for ((i, daily) in weatherSpec.forecasts.withIndex()) {
            addDynamicCategory("Day $i")
            addPreference("Max Temp", "${daily.maxTemp} K (${daily.maxTemp - 273} °C)")
            addPreference("Min Temp", "${daily.minTemp} K (${daily.minTemp - 273} °C)")
            addPreference("Condition Code", daily.conditionCode)
            addPreference("Humidity", daily.humidity)
            addPreference("Wind Speed", "${daily.windSpeed} kmph")
            addPreference("Wind Direction", "${daily.windDirection} deg")
            addPreference("UV Index", daily.uvIndex)
            addPreference("Precip Probability", "${daily.precipProbability} %")
            addPreference("Sun Rise", sdf.format(Date(daily.sunRise * 1000L)))
            addPreference("Sun Set", sdf.format(Date(daily.sunSet * 1000L)))
            addPreference("Moon Rise", sdf.format(Date(daily.moonRise * 1000L)))
            addPreference("Moon Set", sdf.format(Date(daily.moonSet * 1000L)))
            addPreference("Moon Phase", "${daily.moonPhase} deg")

            if (daily.airQuality != null) {
                addPreference("Air Quality aqi", daily.airQuality!!.aqi)
                addPreference("Air Quality co", daily.airQuality!!.co)
                addPreference("Air Quality no2", daily.airQuality!!.no2)
                addPreference("Air Quality o3", daily.airQuality!!.o3)
                addPreference("Air Quality pm10", daily.airQuality!!.pm10)
                addPreference("Air Quality pm25", daily.airQuality!!.pm25)
                addPreference("Air Quality so2", daily.airQuality!!.so2)
                addPreference("Air Quality coAqi", daily.airQuality!!.coAqi)
                addPreference("Air Quality no2Aqi", daily.airQuality!!.no2Aqi)
                addPreference("Air Quality o3Aqi", daily.airQuality!!.o3Aqi)
                addPreference("Air Quality pm10Aqi", daily.airQuality!!.pm10Aqi)
                addPreference("Air Quality pm25Aqi", daily.airQuality!!.pm25Aqi)
                addPreference("Air Quality so2Aqi", daily.airQuality!!.so2Aqi)
            } else {
                addPreference("Air Quality", null)
            }
        }
    }

    private fun showHours(weatherSpec: WeatherSpec) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT)
        for ((i, hourly) in weatherSpec.hourly.withIndex()) {
            addDynamicCategory("Hour $i - ${sdf.format(Date(hourly.timestamp * 1000L))}")
            addPreference("Max Temp", "${hourly.temp} K (${hourly.temp - 273} °C)")
            addPreference("Condition Code", hourly.conditionCode)
            addPreference("Humidity", hourly.humidity)
            addPreference("Wind Speed", "${hourly.windSpeed} kmph")
            addPreference("Wind Direction", "${hourly.windDirection} deg")
            addPreference("UV Index", hourly.uvIndex)
            addPreference("Precip Probability", "${hourly.precipProbability} %")
        }
    }
}
