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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitDeviceInfo

data class AntGadget(val manufacturer: Int, val product: Int, val name: String) {
    companion object {
        val All: Collection<AntGadget> = listOf(
            AntGadget(1, 255, "OHR"),
            AntGadget(1, 357, "Rally RS200"),
            AntGadget(1, 359, "Varia RTL515"),
            AntGadget(1, 1253, "Chirp"),
            AntGadget(1, 1381, "Vector"),
            AntGadget(1, 1570, "Tempe"),
            AntGadget(1, 1743, "HRM-Tri"),
            AntGadget(1, 1752, "HRM-Run"),
            AntGadget(1, 2161, "Vector 2"),
            AntGadget(1, 2327, "HRM-Run"),
            AntGadget(1, 2567, "Varia UT800"),
            AntGadget(1, 2593, "RD Pod"),
            AntGadget(1, 2641, "Xero A1"),
            AntGadget(1, 2787, "Vector 3S"),
            AntGadget(1, 2954, "Varia RTL510"),
            AntGadget(1, 3192, "Speed Sensor 2"),
            AntGadget(1, 3299, "HRM-Dual"),
            AntGadget(1, 3300, "HRM-Pro"),
            AntGadget(1, 3340, "Varia RVR315"),
            AntGadget(1, 3458, "Xero X1i"),
            AntGadget(1, 3540, "Speed and Cadence 2"),
            AntGadget(1, 3578, "Rally RS200"),
            AntGadget(1, 3592, "Varia RTL515"),
            AntGadget(1, 3808, "Varia RCT715"),
            AntGadget(1, 3811, "Varia Rearview"),
            AntGadget(1, 3889, "Instinct 2S"),
            AntGadget(1, 3905, "Fenix 7S"),
            AntGadget(1, 3968, "inReach Mini 2"),
            AntGadget(1, 4053, "Xero C1 Pro"),
            AntGadget(1, 4130, "HRM-Pro Plus"),
            AntGadget(1, 4446, "HRM-Fit"),
            AntGadget(1, 4525, "Rally X10"),
            AntGadget(1, 4606, "HRM 200"),
            AntGadget(1, 4607, "HRM 600"),
            AntGadget(1, 10007, "SDM4 Pod"),
            AntGadget(1, 65534, "Connect"),
            AntGadget(123, 2, "Polar H10"),
            AntGadget(263, 10, "Assioma Uno"),
            AntGadget(263, 12, "Assioma Duo"),
            AntGadget(268, 1013, "XX1 Eagle AXS"),
            AntGadget(268, 1075, "GX Eagle"),
            AntGadget(268, 1118, "RED AXS"),
            AntGadget(268, 1139, "Rival XPLR AXS"),
        )

        fun FindGadget(manufacturer: Int?, product: Int?): AntGadget? {
            if (manufacturer == null || product == null) {
                return null
            }
            val m: Int = manufacturer
            val p: Int = product
            return All.find { gadget -> gadget.manufacturer == m && gadget.product == p }
        }

        fun NameGadget(deviceInfo: FitDeviceInfo): String {
            val n = deviceInfo.productName
            if (!n.isNullOrEmpty()) {
                return n
            }

            val gadget = FindGadget(deviceInfo.manufacturer, deviceInfo.product)
            if (gadget != null) {
                return gadget.name
            }

            val serial = deviceInfo.serialNumber
            if (serial != null) {
                return serial.toString()
            }

            return deviceInfo.deviceIndex!!.toString()
        }
    }
}