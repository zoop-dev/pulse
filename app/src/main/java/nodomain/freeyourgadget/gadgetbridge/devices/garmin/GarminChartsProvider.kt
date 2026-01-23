package nodomain.freeyourgadget.gadgetbridge.devices.garmin

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DefaultChartsProvider
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler
import nodomain.freeyourgadget.gadgetbridge.devices.GarminSleepRestlessMomentsSampleProvider
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE

class GarminChartsProvider : DefaultChartsProvider() {
    override fun getDailySleepStats(
        context: Context,
        db: DBHandler,
        device: GBDevice,
        tsStart: Int,
        tsEnd: Int
    ): Map<String, ActivitySummarySimpleEntry> {
        val sleepRestlessMomentsSampleProvider = GarminSleepRestlessMomentsSampleProvider(device, db.getDaoSession())
        val restlessMoments = sleepRestlessMomentsSampleProvider.getAllSamples(tsStart * 1000L, tsEnd * 1000L)
        if (restlessMoments.isEmpty()) {
            return emptyMap()
        }
        restlessMoments.sumOf { it.count }
        return mapOf(
            context.getString(R.string.sleep_restless_moments) to ActivitySummarySimpleEntry(
                restlessMoments.sumOf { it.count },
                UNIT_NONE
            )
        )
    }
}
