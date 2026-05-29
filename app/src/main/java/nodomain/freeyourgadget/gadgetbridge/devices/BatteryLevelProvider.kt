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
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BatteryLevelProvider(val device: GBDevice, val session: DaoSession) :
    PersistanceProvider<BatteryLevel> {

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
            val deviceId = device.id

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