package nodomain.freeyourgadget.gadgetbridge.model.weather

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec
import org.slf4j.LoggerFactory
import java.io.File

class WeatherCacheManager(
    cacheDir: File,
    private val useCache: Boolean
) {
    private val LOG = LoggerFactory.getLogger(WeatherCacheManager::class.java)
    private val cacheFile = File(cacheDir, "weatherCache.bin")
    private val listType = object : TypeToken<List<WeatherSpec>>() {}.type
    private val gson: Gson = Gson()

    fun load(onLoaded: (List<WeatherSpec>) -> Unit) {
        if (!useCache || !cacheFile.exists()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = cacheFile.readText()
                val specs: List<WeatherSpec> = gson.fromJson(json, listType)
                LOG.info("Loaded ${specs.size} weather specs from cache")
                onLoaded(specs)
            } catch (e: Exception) {
                LOG.error("Failed to read weather cache file", e)
            }
        }
    }

    fun save(specs: List<WeatherSpec>) {
        if (!useCache || specs.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = gson.toJson(specs)
                cacheFile.writeText(json)
                LOG.info("Saved weather specs to cache: ${cacheFile.path}")
            } catch (e: Exception) {
                LOG.error("Failed to save weather cache", e)
            }
        }
    }

    fun clear() {
        if (cacheFile.exists()) {
            try {
                if (cacheFile.delete()) {
                    LOG.info("Deleted cache file: ${cacheFile.path}")
                } else {
                    LOG.warn("Failed to delete cache file: ${cacheFile.path}")
                }
            } catch (e: Exception) {
                LOG.error("Error deleting cache file", e)
            }
        }
    }
}
