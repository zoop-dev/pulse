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

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.FitDevice
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitDeviceInfo
import java.util.Locale

data class AntGadget(val manufacturer: Int, val product: Int, val name: String) {
    companion object {
        fun FindGadget(manufacturer: Int?, product: Int?): FitDevice? {
            if (manufacturer == null || product == null) {
                return null
            }
            val m: Int = manufacturer
            val p: Int = product

            return FitDevice.entries.find { gadget -> gadget.manufacturer == m && gadget.product == p }
        }

        fun NameGadget(deviceInfo: FitDeviceInfo): String {
            val descriptor = deviceInfo.descriptor
            if (!descriptor.isNullOrEmpty()) {
                return descriptor
            }

            var productName = deviceInfo.productName
            if (productName.isNullOrEmpty()) {
                val gadget = FindGadget(deviceInfo.manufacturer, deviceInfo.product)
                if (gadget != null) {
                    productName = gadget.name
                } else {
                    productName = null
                }
            }

            // by default most newer Garmin UIs append ":" and the extended ANT device number
            val antId = deviceInfo.antId
            if (antId != null) {
                val deviceId = getExtendedAntDeviceId(antId)
                if (productName != null) {
                    return "$productName:$deviceId"
                } else {
                    return deviceId.toString()
                }
            }

            // for BLE / Bluetooth devices Garmin UIs append ":" and the lower 20 bits of the serial number
            val serial = deviceInfo.serialNumber
            if (serial != null) {
                if (productName != null) {
                    return productName + ":" + (serial and 0xFFFFF).toString()
                } else {
                    return (serial and 0xFFFFF).toString()
                }
            } else if (productName != null) {
                return productName
            }

            val index = deviceInfo.deviceIndex
            if (index != null) {
                return index.toString()
            }

            // fallback of the fallback -> very unlikely to get here -> no localization implemented
            return "<???>"
        }

        // format ANT id to the common text representation (e.g. 1234567890 to "4-9-96-02D2")
        // actual type is BaseType.UINT32Z thus only 32 bits are relevant
        fun formatAntID(id: Long?): String? {
            if (id == null) {
                return null
            }
            return String.format(
                Locale.ROOT,
                "%X-%X-%02X-%04X",
                (id ushr 28) and 0x0F,
                (id ushr 24) and 0x0F,
                (id ushr 16) and 0xFF,
                id and 0xFFFF
            )
        }

        // extract extended ANT device ID from the ANT ID:
        // top nibble of the transmission type followed by botton 2 bytes
        // (0xF000_FFFF -> 0xF_FFFF) - shown in decimal format by many Garmin UIs
        // see also Sensor Settings (record 147) / Name (field 2) and ANT ID (field 0)
        fun getExtendedAntDeviceId(ant: Long): Int {
            return (((ant ushr 12) and 0xF0000L) or (ant and 0xFFFFL)).toInt()
        }
    }
}