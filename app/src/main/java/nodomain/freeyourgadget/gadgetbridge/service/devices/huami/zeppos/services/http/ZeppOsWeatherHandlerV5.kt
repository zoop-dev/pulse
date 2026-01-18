package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.http

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather.getWeatherSpec
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsWeatherHandler
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.gson.OffsetDateTimeAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date
import java.util.GregorianCalendar
import kotlin.math.roundToInt

@Suppress("unused")
object ZeppOsWeatherHandlerV5 {
    private val LOG: Logger = LoggerFactory.getLogger(ZeppOsWeatherHandlerV5::class.java)

    private val GSON: Gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeAdapter())
        .create()

    @JvmStatic
    fun handleHttpRequest(path: String, query: MutableMap<String, String>): String? {
        val weatherSpec = getWeatherSpec()

        if (weatherSpec == null) {
            LOG.error("No weather in weather instance")
            return null
        }

        // /weather/v5/<LOCALE>/<LONGITUDE>/<LATITUDE>/
        if (!Regex("^/weather/v5/[^/]+/[^/]+/[^/]+/?$").matches(path)) {
            LOG.error("Unknown path: {}", path)
            return null
        }

        val pathParts = path.trim('/').split("/")
        val locale = pathParts[2]
        val longitude = pathParts[3]
        val latitude = pathParts[4]

        LOG.debug("Weather request: locale={}, lon={}, lat={}", locale, longitude, latitude)

        val datasetsParam = query["datasets"]
        if (datasetsParam.isNullOrBlank()) {
            LOG.error("No datasets parameter provided")
            return null
        }

        val datasets = datasetsParam.split(",").map { it.trim() }

        val response = JsonObject()

        for (dataset in datasets) {
            val datasetObject = when (dataset) {
                "place" -> createPlace(weatherSpec)
                "hourlyWeather" -> createHourlyWeather(weatherSpec)
                "hourlyAirQuality" -> createHourlyAirQuality(weatherSpec)
                "dailyIndices" -> createDailyIndices(weatherSpec)
                "dailyWeather" -> createDailyWeather(weatherSpec)
                "dailyTide" -> createDailyTide(weatherSpec)
                // TODO "dailyAirQuality" -> createDailyAirQuality(weatherSpec)
                else -> null
            }

            if (datasetObject != null) {
                response.add(dataset, GSON.toJsonTree(datasetObject))
            } else {
                LOG.warn("Failed to compute dataset object for {}", dataset)
            }
        }

        return GSON.toJson(response)
    }

    private fun createMetadata(weatherSpec: WeatherSpec) = Metadata(
        reportedTime = toOffsetDateTime(Date(weatherSpec.timestamp * 1000L)),
        units = "m",
        version = 1,
    )

    private fun createPlace(weatherSpec: WeatherSpec) = Place(
        locationKey = "accu:123456",
        longitude = weatherSpec.longitude.toString(),
        latitude = weatherSpec.latitude.toString(),
        affiliation = weatherSpec.location,
        name = weatherSpec.location,
        countryCode = null,
    )

    private fun createHourlyWeather(weatherSpec: WeatherSpec): HourlyWeather {
        return HourlyWeather(
            metadata = createMetadata(weatherSpec),
            hours = weatherSpec.hourly
                .filter { it.timestamp != 0 }
                .map {
                    HourlyWeatherHour(
                        forecastStart = toOffsetDateTime(Date(it.timestamp * 1000L)),
                        conditionCode = ZeppOsWeatherHandler.mapToZeppOsWeatherCode(it.conditionCode).toString(),
                        humidity = it.humidity / 100.0f,
                        pressure = weatherSpec.pressure, // TODO WeatherSpec does not support hourly pressure
                        temperature = it.temp - 273.15f,
                        uvIndex = it.uvIndex.roundToInt(),
                        visibility = weatherSpec.visibility, // TODO WeatherSpec does not support hourly visibility
                        windDirection = it.windDirection,
                        windSpeed = it.windSpeed,
                        windScale = it.windSpeedAsBeaufort(),
                    )
                }.toList()
        )
    }

    private fun createHourlyAirQuality(weatherSpec: WeatherSpec): HourlyAirQuality {
        // TODO WeatherSpec does not support hourly air quality
        return HourlyAirQuality(
            metadata = createMetadata(weatherSpec),
            hours = emptyList()
        )
    }

    private fun createDailyIndices(weatherSpec: WeatherSpec): DailyIndices {
        return DailyIndices(
            metadata = createMetadata(weatherSpec),
            days = (listOf(weatherSpec.todayAsDaily()) + weatherSpec.forecasts).mapIndexed { i, day ->
                val dayTimestamp = weatherSpec.timestamp * 1000L + i * 86400_000L
                return@mapIndexed DailyIndicesDay(
                    forecastStart = toOffsetDateTime(DateTimeUtils.dayStart(Date(dayTimestamp))),
                    forecastEnd = toOffsetDateTime(DateTimeUtils.dayEnd(Date(dayTimestamp))),
                    outdoorSportIndex = null,
                    uvLevel = day.uvIndex.roundToInt().toString(),
                    carWashingIndex = null,
                    fishingIndex = null,
                    allergyIndex = null
                )
            }.toList()
        )
    }

    private fun createDailyWeather(weatherSpec: WeatherSpec): DailyWeather {
        return DailyWeather(
            metadata = createMetadata(weatherSpec),
            days = (listOf(weatherSpec.todayAsDaily()) + weatherSpec.forecasts).mapIndexed { i, day ->
                val dayTimestamp = weatherSpec.timestamp * 1000L + i * 86400_000L
                val calendar = GregorianCalendar()
                calendar.setTime(Date(dayTimestamp))

                val sunrise = WeatherSpec.sunriseComputed(day.sunRise, calendar, weatherSpec.getLocationObject())
                val sunset = WeatherSpec.sunsetComputed(day.sunRise, calendar, weatherSpec.getLocationObject())

                DailyWeatherDay(
                    forecastStart = toOffsetDateTime(DateTimeUtils.dayStart(Date(dayTimestamp))),
                    forecastEnd = toOffsetDateTime(DateTimeUtils.dayEnd(Date(dayTimestamp))),
                    conditionCode = ZeppOsWeatherHandler.mapToZeppOsWeatherCode(day.conditionCode).toString(),
                    maxUvIndex = day.uvIndex.roundToInt(),
                    moonPhaseLunarDay = day.lunarDay().toString(),
                    moonPhase = getMoonPhaseString(day.lunarDay()),
                    moonrise = if (day.moonRise > 0) toOffsetDateTime(Date(day.moonRise * 1000L)) else null,
                    moonset = if (day.moonSet > 0) toOffsetDateTime(Date(day.moonSet * 1000L)) else null,
                    sunrise = sunrise?.let { toOffsetDateTime(it) },
                    sunset = sunset?.let { toOffsetDateTime(it) },
                    temperatureMax = day.maxTemp - 273.15f,
                    temperatureMin = day.minTemp - 273.15f,
                    daytimeForecast = null,
                    overnightForecast = null,
                    //daytimeForecast = DayPartForecast(
                    //    forecastStart = sunrise,
                    //    forecastEnd = sunset,
                    //    conditionCode = ZeppOsWeatherHandler.mapToZeppOsWeatherCode(day.conditionCode).toString(),
                    //    humidity = day.humidity / 100.0f,
                    //    windDirection = day.windDirection,
                    //    windSpeed = day.windSpeed,
                    //    windScale = day.windSpeedAsBeaufort(),
                    //),
                    //overnightForecast = DayPartForecast(
                    //    forecastStart = sunset,
                    //    forecastEnd = Date(sunrise.time + 86400_000L),
                    //    conditionCode = ZeppOsWeatherHandler.mapToZeppOsWeatherCode(day.conditionCode).toString(),
                    //    humidity = day.humidity / 100.0f,
                    //    windDirection = day.windDirection,
                    //    windSpeed = day.windSpeed,
                    //    windScale = day.windSpeedAsBeaufort(),
                    //),
                )
            }
        )
    }

    private fun toOffsetDateTime(date: Date): OffsetDateTime {
        return OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(date.time),
            ZoneId.systemDefault()
        )
    }

    private fun createDailyTide(weatherSpec: WeatherSpec) = DailyTide(
        // We do not support tide data
        metadata = createMetadata(weatherSpec),
        days = emptyList()
    )

    private fun getMoonPhaseString(moonPhase: Int): String = when (moonPhase) {
        // TODO only seen waxingCrescent
        0 -> "new"
        in 1..6 -> "waxingCrescent"
        7 -> "firstQuarter"
        in 8..13 -> "waxingGibbous"
        14 -> "full"
        in 15..20 -> "waningGibbous"
        21 -> "lastQuarter"
        in 22..27 -> "waningCrescent"
        else -> "new"
    }

    data class Metadata(
        val reportedTime: OffsetDateTime,
        val units: String,
        val version: Int,
    )

    data class Place(
        val locationKey: String?,
        val longitude: String?,
        val latitude: String?,
        val affiliation: String?,
        val name: String?,
        val countryCode: String?,
    )

    data class HourlyWeatherHour(
        val forecastStart: OffsetDateTime,
        val conditionCode: String,
        val humidity: Float,
        val pressure: Float,
        val temperature: Float,
        val uvIndex: Int,
        val visibility: Float,
        val windDirection: Int,
        val windSpeed: Float,
        val windScale: Int,
    )

    data class HourlyWeather(
        val metadata: Metadata,
        val hours: List<HourlyWeatherHour>,
    )

    data class HourlyAirQualityHour(
        val forecastStart: OffsetDateTime,
        val aqi: String,
        val co: String,
        val no2: String,
        val o3: String,
        val pm10: String,
        val pm25: String,
        val so2: String,
    )

    data class HourlyAirQuality(
        val metadata: Metadata,
        val hours: List<HourlyAirQualityHour>,
    )

    data class DailyIndicesDay(
        val forecastStart: OffsetDateTime,
        val forecastEnd: OffsetDateTime,
        val outdoorSportIndex: String?,
        val uvLevel: String?,
        val carWashingIndex: String?,
        val fishingIndex: String?,
        val allergyIndex: String?,
    )

    data class DailyIndices(
        val metadata: Metadata,
        val days: List<DailyIndicesDay>,
    )

    data class DayPartForecast(
        val forecastStart: OffsetDateTime,
        val forecastEnd: OffsetDateTime,
        val conditionCode: String,
        val humidity: Float,
        val windDirection: Int,
        val windSpeed: Float,
        val windScale: Int,
    )

    data class DailyWeatherDay(
        val forecastStart: OffsetDateTime,
        val forecastEnd: OffsetDateTime,
        val conditionCode: String,
        val maxUvIndex: Int,
        val moonPhaseLunarDay: String?,
        val moonPhase: String?,
        val moonrise: OffsetDateTime?,
        val moonset: OffsetDateTime?,
        val sunrise: OffsetDateTime?,
        val sunset: OffsetDateTime?,
        val temperatureMax: Float,
        val temperatureMin: Float,
        val daytimeForecast: DayPartForecast?,
        val overnightForecast: DayPartForecast?,
    )

    data class DailyWeather(
        val metadata: Metadata,
        val days: List<DailyWeatherDay>,
    )

    data class TideTableEntry(
        val forecastTime: OffsetDateTime,
        val height: String,
        val type: String,
    )

    data class TideHourlyEntry(
        val forecastStart: OffsetDateTime,
        val height: String,
    )

    data class DailyTideDay(
        val forecastStart: OffsetDateTime,
        val forecastEnd: OffsetDateTime,
        val poiName: String,
        val poiKey: String,
        val tideTable: List<TideTableEntry>,
        val tideHourly: List<TideHourlyEntry>,
    )

    data class DailyTide(
        val metadata: Metadata,
        val days: List<DailyTideDay>,
    )
}
