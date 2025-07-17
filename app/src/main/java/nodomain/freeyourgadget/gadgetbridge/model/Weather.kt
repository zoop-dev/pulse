package nodomain.freeyourgadget.gadgetbridge.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

object Weather {
    private val LOG = LoggerFactory.getLogger("Weather")

    private val weatherSpecs = CopyOnWriteArrayList<WeatherSpec>()
    private var reconstructedOWMForecast: JSONObject? = null

    private var cacheManager: WeatherCacheManager? = null

    fun setWeatherSpec(newSpecs: Collection<WeatherSpec>) {
        weatherSpecs.apply {
            clear()
            addAll(newSpecs)
        }
        cacheManager?.save(weatherSpecs)
    }

    fun getWeatherSpec(): WeatherSpec? = weatherSpecs.firstOrNull()

    fun getWeatherSpecs(): List<WeatherSpec> = weatherSpecs

    fun createReconstructedOWMWeatherReply(): JSONObject? {
        val spec = getWeatherSpec() ?: return null

        return try {
            JSONObject().apply {
                put("weather", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", spec.currentConditionCode)
                        put("main", spec.currentCondition)
                        put("description", spec.currentCondition)
                        put("icon", WeatherMapper.mapToOpenWeatherMapIcon(spec.currentConditionCode))
                    })
                })

                put("main", JSONObject().apply {
                    put("temp", spec.currentTemp)
                    put("humidity", spec.currentHumidity)
                    put("temp_min", spec.todayMinTemp)
                    put("temp_max", spec.todayMaxTemp)
                })

                put("wind", JSONObject().apply {
                    put("speed", spec.windSpeed / 3.6f)
                    put("deg", spec.windDirection)
                })

                put("name", spec.location)
            }.also {
                LOG.debug("Weather JSON for WEBVIEW: {}", it)
            }
        } catch (e: JSONException) {
            LOG.error("Error while reconstructing OWM weather reply", e)
            null
        }
    }

    fun getReconstructedOWMForecast(): JSONObject? = reconstructedOWMForecast

    fun setReconstructedOWMForecast(value: JSONObject?) {
        reconstructedOWMForecast = value
    }

    fun initializeCache(cacheManager: WeatherCacheManager) {
        this.cacheManager = cacheManager

        cacheManager.load { loadedSpecs ->
            weatherSpecs.clear()
            weatherSpecs.addAll(loadedSpecs)
        }
    }
}
