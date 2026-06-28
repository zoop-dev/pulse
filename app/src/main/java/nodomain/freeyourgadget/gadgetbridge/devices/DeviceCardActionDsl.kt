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
package nodomain.freeyourgadget.gadgetbridge.devices

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

class DeviceCardActionBuilder {
    var icon: ((GBDevice) -> Int)? = null
    var description: ((GBDevice, Context) -> String)? = null
    var label: ((GBDevice, Context) -> String?)? = null
    var isVisible: ((GBDevice) -> Boolean)? = null
    var onClick: ((GBDevice, Context) -> Unit)? = null

    fun build(): DeviceCardAction {
        val iconFn = requireNotNull(icon) { "icon must be set" }
        val descriptionFn = requireNotNull(description) { "description must be set" }
        val onClickFn = requireNotNull(onClick) { "onClick must be set" }
        return object : DeviceCardAction {
            override fun getIcon(device: GBDevice) = iconFn(device)
            override fun getDescription(device: GBDevice, context: Context) = descriptionFn(device, context)
            override fun getLabel(device: GBDevice, context: Context) = label?.invoke(device, context)
            override fun isVisible(device: GBDevice) = isVisible?.invoke(device) ?: super.isVisible(device)
            override fun onClick(device: GBDevice, context: Context) = onClickFn(device, context)
        }
    }
}

fun deviceCardAction(block: DeviceCardActionBuilder.() -> Unit): DeviceCardAction =
    DeviceCardActionBuilder().apply(block).build()
