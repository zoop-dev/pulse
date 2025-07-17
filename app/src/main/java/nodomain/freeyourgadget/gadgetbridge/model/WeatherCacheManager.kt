package nodomain.freeyourgadget.gadgetbridge.model

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.*

class WeatherCacheManager(
    cacheDir: File,
    private val useCache: Boolean
) {
    private val LOG = LoggerFactory.getLogger("WeatherCacheManager")
    private val cacheFile = File(cacheDir, "weatherCache.bin")


    fun load(onLoaded: (List<WeatherSpec>) -> Unit) {
        if (!useCache || !cacheFile.exists()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ObjectInputStream(FileInputStream(cacheFile)).use { input ->
                    val specs = input.readObject() as? ArrayList<WeatherSpec>
                    specs?.let { onLoaded(it) }
                    LOG.info("Loaded ${specs?.size ?: 0} weather specs from cache")
                }
            } catch (e: Exception) {
                LOG.error("Failed to load weather cache", e)
            }
        }
    }

    fun save(specs: List<WeatherSpec>) {
        if (!useCache || specs.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ObjectOutputStream(FileOutputStream(cacheFile)).use { output ->
                    output.writeObject(ArrayList(specs))
                }
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
