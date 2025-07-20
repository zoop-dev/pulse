package nodomain.freeyourgadget.gadgetbridge.model.weather

import java.util.concurrent.CopyOnWriteArrayList

object Weather {
    private val weatherSpecs = CopyOnWriteArrayList<WeatherSpec>()

    private var cacheManager: WeatherCacheManager? = null

    @JvmStatic
    fun setWeatherSpec(newSpecs: Collection<WeatherSpec>) {
        weatherSpecs.apply {
            clear()
            addAll(newSpecs)
        }
        cacheManager?.save(weatherSpecs)
    }

    @JvmStatic
    fun getWeatherSpec(): WeatherSpec? = weatherSpecs.firstOrNull()

    @JvmStatic
    fun getWeatherSpecs(): List<WeatherSpec> = weatherSpecs

    @JvmStatic
    fun initializeCache(cacheManager: WeatherCacheManager) {
        Weather.cacheManager = cacheManager

        cacheManager.load { loadedSpecs ->
            weatherSpecs.clear()
            weatherSpecs.addAll(loadedSpecs)
        }
    }
}
