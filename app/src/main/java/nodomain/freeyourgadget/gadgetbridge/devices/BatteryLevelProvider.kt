/*  Copyright (C) 2026 Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices

import android.content.Context
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevel
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevelDao
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BatteryLevelProvider(val device: GBDevice, val session: DaoSession) :
    PersistenceProvider<BatteryLevel> {

    /**
     * Returns the battery level samples for the given battery index and time range, ordered
     * by ascending timestamp. The time range is given in Unix epoch milliseconds, even though
     * [BatteryLevel.timestamp] is stored in seconds.
     */
    fun getAllSamples(batteryIndex: Int,
                      timestampFromMillis: Long,
                      timestampToMillis: Long): List<BatteryLevel> {
        val dbDevice = DBHelper.findDevice(device, session) ?: return emptyList()

        val qb = session.batteryLevelDao.queryBuilder()
        qb.where(BatteryLevelDao.Properties.DeviceId.eq(dbDevice.id))
            .where(BatteryLevelDao.Properties.BatteryIndex.eq(batteryIndex))
            .where(BatteryLevelDao.Properties.Timestamp.ge((timestampFromMillis / 1000L).toInt()))
            .where(BatteryLevelDao.Properties.Timestamp.le((timestampToMillis / 1000L).toInt()))
            .orderAsc(BatteryLevelDao.Properties.Timestamp)
        val samples = qb.build().list()
        session.batteryLevelDao.detachAll()
        return samples
    }

    /**
     * The most recently recorded battery level sample for the given battery index, regardless
     * of any time range, or null if none was ever recorded.
     */
    fun getLatestSample(batteryIndex: Int): BatteryLevel? {
        val dbDevice = DBHelper.findDevice(device, session) ?: return null

        val qb = session.batteryLevelDao.queryBuilder()
        qb.where(BatteryLevelDao.Properties.DeviceId.eq(dbDevice.id))
            .where(BatteryLevelDao.Properties.BatteryIndex.eq(batteryIndex))
            .orderDesc(BatteryLevelDao.Properties.Timestamp)
            .limit(1)
        val samples = qb.build().list()
        session.batteryLevelDao.detachAll()
        return samples.firstOrNull()
    }

    /**
     * Whether any battery level sample was ever recorded for the given battery index.
     */
    fun hasSamples(batteryIndex: Int): Boolean {
        val dbDevice = DBHelper.findDevice(device, session) ?: return false

        val qb = session.batteryLevelDao.queryBuilder()
        qb.where(BatteryLevelDao.Properties.DeviceId.eq(dbDevice.id))
            .where(BatteryLevelDao.Properties.BatteryIndex.eq(batteryIndex))
            .limit(1)
        val hasSamples = !qb.build().list().isEmpty()
        session.batteryLevelDao.detachAll()
        return hasSamples
    }

    override fun persistSamples(
        samples: List<BatteryLevel>, context: Context?
    ): Boolean {
        if (samples.isEmpty()) {
            return true
        }

        LOG.debug(
            "Will persist {} {} samples",
            samples.size,
            javaClass.getSimpleName().replace("Provider", "")
        )

        try {
            val session = this.session

            val gbDevice = this.device
            val device = DBHelper.findDevice(gbDevice, session)
            if (device == null) {
                LOG.warn("Device not found in database for '{}'", gbDevice.getAliasOrName())
                return false
            }
            val deviceId = device.id!!

            for (sample in samples) {
                sample.deviceId = deviceId
            }

            val dao = session.batteryLevelDao
            dao.insertOrReplaceInTx(samples)
        } catch (e: Exception) {
            LOG.error("Error saving samples", e)
            val ctx = context ?: GBApplication.getContext()
            val message: String =
                ctx.getString(R.string.persisting_samples_failed, e.localizedMessage)
            GB.toast(ctx, message, Toast.LENGTH_LONG, GB.ERROR, e)
            return false
        }
        return true
    }

    companion object {
        internal val LOG: Logger by lazy { LoggerFactory.getLogger(BatteryLevelProvider::class.java) }
    }
}
