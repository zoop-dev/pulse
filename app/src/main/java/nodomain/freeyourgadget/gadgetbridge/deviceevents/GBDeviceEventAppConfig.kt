/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.parcelize.Parcelize
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfigFragment
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import java.util.UUID

@Parcelize
class GBDeviceEventAppConfig(
    val uuid: UUID,
    val event: Event,
    val configs: ArrayList<DynamicAppConfig> = ArrayList()
) : GBDeviceEvent(), Parcelable {
    override fun toString(): String {
        val sb = StringBuilder(super.toString())
        sb.append("event: $event")
        if (!configs.isEmpty()) {
            sb.append(", configs: ")
            for (pref in configs) {
                sb.append(pref.key).append(", ")
            }
            sb.setLength(sb.length - 2)
        }

        return sb.toString()
    }

    override fun evaluate(context: Context, device: GBDevice) {
        val intent = Intent(DynamicAppConfigFragment.ACTION_APP_CONFIG_EVENT)
        intent.putExtra(GBDevice.EXTRA_DEVICE, device)
        intent.putExtra(EXTRA_EVENT, this)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    enum class Event {
        APP_CONFIG_GET_SUCCESS,
        APP_CONFIG_GET_FAILED,
        APP_CONFIG_SET_SUCCESS,
        APP_CONFIG_SET_FAILED,
    }

    companion object {
        const val EXTRA_EVENT = "app_config_event"
    }
}
