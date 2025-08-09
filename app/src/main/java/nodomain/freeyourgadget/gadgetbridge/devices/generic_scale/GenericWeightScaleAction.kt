/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.generic_scale

import android.content.Context
import android.content.Intent
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale.WeightScaleProfile

class GenericWeightScaleAction : DeviceCardAction {
    override fun getIcon(device: GBDevice?): Int {
        return R.drawable.ic_balance
    }

    override fun getDescription(
        device: GBDevice?, context: Context?
    ): String? {
        return context?.getString(R.string.weight_scale_show_measurement)
    }

    override fun onClick(
        device: GBDevice?, context: Context?
    ) {
        val intent = Intent(context, GenericWeightScaleMeasurementActivity::class.java)
        intent.putExtra(WeightScaleProfile.EXTRA_ADDRESS, device?.address)
        context?.startActivity(intent)
    }
}