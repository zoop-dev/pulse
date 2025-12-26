package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.adapter.SimpleIconListAdapter
import nodomain.freeyourgadget.gadgetbridge.model.RunnableListIconItem
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherCacheManager
import nodomain.freeyourgadget.gadgetbridge.util.GB
import java.lang.Boolean
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.apply
import kotlin.plus

class WeatherDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_weather, rootKey)

        onClick(PREF_DEBUG_WEATHER_SEND) {
            if (Weather.getWeatherSpecs().isEmpty()) {
                GB.toast(requireContext(), "No cached weather to send", Toast.LENGTH_SHORT, GB.ERROR)
                return@onClick
            }

            runOnDebugDevices("Send weather to devices") {
                GBApplication.deviceService(it).onSendWeather()
            }
        }

        onClick(PREF_DEBUG_WEATHER_ADD_TEST) {
            val weatherSpec = WeatherSpec.createTestWeather()
            if (!Weather.getWeatherSpecs().isEmpty()) {
                weatherSpec.location += " (${Weather.getWeatherSpecs().size})"
            }
            Weather.setWeatherSpec((Weather.getWeatherSpecs() + weatherSpec).toMutableList())
            reloadCachedWeather()
        }

        findPreference<Preference>(CACHE_WEATHER)!!.setOnPreferenceChangeListener { _: Preference?, newVal: Any? ->
            val doEnable = Boolean.TRUE == newVal
            Weather.initializeCache(WeatherCacheManager(requireContext().cacheDir, doEnable))
            true
        }

        reloadCachedWeather()
    }

    override fun onResume() {
        super.onResume()
        reloadCachedWeather()
    }

    private fun reloadCachedWeather() {
        val cachedWeatherHeader: PreferenceCategory = findPreference(PREF_HEADER_CACHED_WEATHER)!!

        val weatherSpecs: List<WeatherSpec> = Weather.getWeatherSpecs()

        removeDynamicPrefs(cachedWeatherHeader)

        if (weatherSpecs.isEmpty()) {
            addDynamicPref(cachedWeatherHeader, "", "No cached weather")
            return
        }

        for ((i, weatherSpec) in weatherSpecs.withIndex()) {
            addDynamicPref(
                group = cachedWeatherHeader,
                title = weatherSpec.location ?: "Unknown location",
                icon = R.drawable.ic_wb_sunny,
                onClickFunction = { onWeatherClick(weatherSpecs, i) }
            )
        }
    }

    fun <T> List<T>.moveTo(from: Int, to: Int): List<T> {
        val target = to.coerceIn(indices)
        if (from == target) return this

        return toMutableList().apply {
            add(target, removeAt(from))
        }
    }

    private fun onWeatherClick(weatherSpecs: List<WeatherSpec>, i: Int) {
        val weatherSpec = weatherSpecs[i]

        val items: MutableList<RunnableListIconItem?> = ArrayList(4)

        // Show details
        items.add(
            RunnableListIconItem(
                "Show details",
                R.drawable.ic_wb_sunny
            ) {
                goTo(
                    WeatherSpecDebugFragment().apply {
                        arguments = Bundle().apply { putParcelable("weatherSpec", weatherSpec) }
                    }
                )
            }
        )

        // Move up
        if (i > 0) {
            items.add(
                RunnableListIconItem(
                    requireContext().getString(R.string.widget_move_up),
                    R.drawable.ic_arrow_upward
                ) {
                    Weather.setWeatherSpec(Weather.getWeatherSpecs().moveTo(i, i - 1))
                    reloadCachedWeather()
                }
            )
        }

        // Move down
        if (i < weatherSpecs.size - 1) {
            items.add(
                RunnableListIconItem(
                    requireContext().getString(R.string.widget_move_down),
                    R.drawable.ic_arrow_downward
                ) {
                    Weather.setWeatherSpec(Weather.getWeatherSpecs().moveTo(i, i + 1))
                    reloadCachedWeather()
                }
            )
        }

        // Delete
        items.add(
            RunnableListIconItem(
                requireContext().getString(R.string.Delete),
                R.drawable.ic_delete
            ) {
                val newList = Weather.getWeatherSpecs().toMutableList()
                newList.removeAt(i)
                Weather.setWeatherSpec(newList)
                reloadCachedWeather()
            }
        )

        val adapter = SimpleIconListAdapter(context, items)

        MaterialAlertDialogBuilder(requireContext())
            .setAdapter(adapter) { _: DialogInterface?, i1: Int -> items[i1]!!.action.run() }
            .setTitle(weatherSpec.location)
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
            .create()
            .show()
    }

    companion object {
        private const val PREF_HEADER_WEATHER = "pref_header_weather"
        private const val PREF_DEBUG_WEATHER_SEND = "pref_debug_weather_send"
        private const val CACHE_WEATHER = "cache_weather"
        private const val PREF_DEBUG_WEATHER_ADD_TEST = "pref_debug_weather_add_test"
        private const val PREF_HEADER_CACHED_WEATHER = "pref_header_cached_weather"
    }
}
