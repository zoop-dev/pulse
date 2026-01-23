package nodomain.freeyourgadget.gadgetbridge.activities.charts

import android.content.Context
import androidx.fragment.app.Fragment
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity.UnknownFragment
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.Locale

open class DefaultChartsProvider : DeviceChartsProvider {
    companion object {
        @JvmField
        val INSTANCE = DefaultChartsProvider()
    }

    override fun getSupportedCharts(device: GBDevice): List<String> {
        val coordinator = device.deviceCoordinator
        val supportedCharts = mutableListOf<String>()

        if (coordinator.supportsCyclingData(device)) {
            supportedCharts.add("cycling")
        }
        if (coordinator.supportsActivityTracking(device)) {
            supportedCharts.add("activity")
            supportedCharts.add("activitylist")
        }
        if (coordinator.supportsSleepMeasurement(device)) {
            supportedCharts.add("sleep")
        }
        if (coordinator.supportsHrvMeasurement(device)) {
            supportedCharts.add("hrvstatus")
        }
        if (coordinator.supportsBodyEnergy(device)) {
            supportedCharts.add("bodyenergy")
        }
        if (coordinator.supportsVO2Max(device)) {
            supportedCharts.add("vo2max")
        }
        if (coordinator.supportsTrainingLoad(device)) {
            supportedCharts.add("load")
        }
        if (coordinator.supportsHeartRateMeasurement(device)) {
            supportedCharts.add("heartrate")
        }
        if (coordinator.supportsStepCounter(device)) {
            supportedCharts.add("stepsweek")
        }
        if (coordinator.supportsStressMeasurement(device)) {
            supportedCharts.add("stress")
        }
        if (coordinator.supportsPai(device)) {
            supportedCharts.add("pai")
        }
        if (coordinator.supportsSpeedzones(device)) {
            supportedCharts.add("speedzones")
        }
        if (coordinator.supportsRealtimeData(device)) {
            supportedCharts.add("livestats")
        }
        if (coordinator.supportsSpo2(device)) {
            supportedCharts.add("spo2")
        }
        if (coordinator.supportsTemperatureMeasurement(device)) {
            supportedCharts.add("temperature")
        }
        if (coordinator.supportsWeightMeasurement(device)) {
            supportedCharts.add("weight")
        }
        if (coordinator.supportsActiveCalories(device)) {
            supportedCharts.add("calories")
        }
        if (coordinator.supportsRespiratoryRate(device)) {
            supportedCharts.add("respiratoryrate")
        }

        return supportedCharts
    }

    override fun getEnabledCharts(device: GBDevice): List<String> {
        val prefs = GBApplication.getDevicePrefs(device)
        val enabledCharts = prefs.getString(DeviceSettingsPreferenceConst.PREFS_DEVICE_CHARTS_TABS, null)
        return if (!enabledCharts.isNullOrBlank()) {
            enabledCharts.split(",").intersect(getSupportedCharts(device).toSet()).toList()
        } else {
            getSupportedCharts(device)
        }
    }

    override fun getChartLabel(
        context: Context,
        device: GBDevice,
        chartName: String
    ): String {
        return when (chartName) {
            "activity" -> context.getString(R.string.activity_sleepchart_activity_and_sleep)
            "activitylist" -> context.getString(R.string.charts_activity_list)
            "sleep" -> context.getString(R.string.sleepchart_your_sleep)
            "heartrate" -> context.getString(R.string.menuitem_hr)
            "hrvstatus" -> context.getString(R.string.pref_header_hrv_status)
            "bodyenergy" -> context.getString(R.string.body_energy)
            "vo2max" -> context.getString(R.string.menuitem_vo2_max)
            "stress" -> context.getString(R.string.menuitem_stress)
            "pai" -> context.getString(device.deviceCoordinator.getPaiName())
            "stepsweek" -> context.getString(R.string.steps)
            "speedzones" -> context.getString(R.string.stats_title)
            "livestats" -> context.getString(R.string.liveactivity_live_activity)
            "spo2" -> context.getString(R.string.pref_header_spo2)
            "temperature" -> context.getString(R.string.menuitem_temperature)
            "cycling" -> context.getString(R.string.title_cycling)
            "weight" -> context.getString(R.string.menuitem_weight)
            "calories" -> context.getString(R.string.calories)
            "respiratoryrate" -> context.getString(R.string.respiratoryrate)
            "load" -> context.getString(R.string.pref_header_training_load)
            else -> String.format(Locale.getDefault(), "Unknown %s", chartName)
        }
    }

    override fun getChartFragment(
        device: GBDevice,
        chartName: String,
        allowSwipe: Boolean
    ): Fragment {
        return when (chartName) {
            "activity" -> ActivitySleepChartFragment()
            "activitylist" -> ActivityListingChartFragment()
            "sleep" -> SleepCollectionFragment.newInstance(allowSwipe)
            "heartrate" -> HeartRateCollectionFragment.newInstance(allowSwipe)
            "hrvstatus" -> HRVStatusFragment()
            "bodyenergy" -> BodyEnergyCollectionFragment.newInstance(allowSwipe)
            "vo2max" -> VO2MaxFragment()
            "load" -> LoadFragment()
            "stress" -> StressCollectionFragment.newInstance(allowSwipe)
            "pai" -> PaiChartFragment()
            "stepsweek" -> StepsCollectionFragment.newInstance(allowSwipe)
            "speedzones" -> SpeedZonesFragment()
            "livestats" -> LiveActivityFragment()
            "spo2" -> Spo2CollectionFragment.newInstance(allowSwipe)
            "temperature" -> {
                if (device.deviceCoordinator.supportsContinuousTemperature(device))
                    TemperatureDailyFragment() else TemperatureChartFragment()
            }

            "cycling" -> CyclingChartFragment()
            "weight" -> WeightChartFragment()
            "calories" -> CaloriesCollectionFragment.newInstance(allowSwipe)
            "respiratoryrate" -> RespiratoryRateCollectionFragment.newInstance(allowSwipe)
            else -> UnknownFragment()
        }
    }

    override fun getDailySleepStats(
        context: Context,
        db: DBHandler,
        device: GBDevice,
        tsStart: Int,
        tsEnd: Int
    ): Map<String, ActivitySummarySimpleEntry> {
        return emptyMap()
    }
}
