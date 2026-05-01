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
import java.util.Locale

data class AntGadget(val manufacturer: Int, val product: Int, val name: String) {
    companion object {
        val All: Collection<AntGadget> = listOf(
            AntGadget(1, 255, "OHR"),
            AntGadget(1, 357, "Rally RS200"),
            AntGadget(1, 359, "Varia RTL515"),
            AntGadget(1, 717, "Forerunner 405"),
            AntGadget(1, 782, "Forerunner 50"),
            AntGadget(1, 988, "Forerunner 60"),
            AntGadget(1, 1018, "Forerunner 310XT"),
            AntGadget(1, 1036, "Edge 500"),
            AntGadget(1, 1124, "Forerunner 110"),
            AntGadget(1, 1169, "Edge 800"),
            AntGadget(1, 1253, "Chirp"),
            AntGadget(1, 1325, "Edge 200"),
            AntGadget(1, 1328, "Forerunner 910XT"),
            AntGadget(1, 1345, "Forerunner 610"),
            AntGadget(1, 1381, "Vector"),
            AntGadget(1, 1405, "Approach G10"),
            AntGadget(1, 1436, "Forerunner 70"),
            AntGadget(1, 1482, "Forerunner 10"),
            AntGadget(1, 1499, "Garmin Swim"),
            AntGadget(1, 1551, "Fenix"),
            AntGadget(1, 1561, "Edge 510"),
            AntGadget(1, 1567, "Edge 810"),
            AntGadget(1, 1570, "Tempe"),
            AntGadget(1, 1623, "Forerunner 620"),
            AntGadget(1, 1632, "Forerunner 220"),
            AntGadget(1, 1736, "Edge Touring"),
            AntGadget(1, 1743, "HRM-Tri"), // also HRM-Swim
            AntGadget(1, 1752, "HRM-Run"),
            AntGadget(1, 1765, "Forerunner 920XT"),
            AntGadget(1, 1836, "Edge 1000"),
            AntGadget(1, 1837, "Vivo Fit"),
            AntGadget(1, 1903, "Forerunner 15"),
            AntGadget(1, 1907, "Vivoactive"),
            AntGadget(1, 1932, "Forerunner 15"),
            AntGadget(1, 1936, "Approach S6"),
            AntGadget(1, 1967, "Fenix 2"),
            AntGadget(1, 2050, "Fenix 3"),
            AntGadget(1, 2067, "Edge 520"),
            AntGadget(1, 2130, "Forerunner 920XT"),
            AntGadget(1, 2147, "Edge 25"),
            AntGadget(1, 2148, "Forerunner 25"),
            AntGadget(1, 2153, "Forerunner 225"),
            AntGadget(1, 2156, "Forerunner 630"),
            AntGadget(1, 2157, "Forerunner 230"),
            AntGadget(1, 2158, "Forerunner 735XT"),
            AntGadget(1, 2161, "Vector 2"),
            AntGadget(1, 2204, "Edge Explore 1000"),
            AntGadget(1, 2238, "Edge 20"),
            AntGadget(1, 2266, "Approach S20"),
            AntGadget(1, 2288, "Edge 25"),
            AntGadget(1, 2292, "Approach X40"),
            AntGadget(1, 2327, "HRM-Run"),
            AntGadget(1, 2337, "Vivoactive HR"),
            AntGadget(1, 2348, "Vivosmart HR"),
            AntGadget(1, 2413, "Fenix 3 HR"),
            AntGadget(1, 2413, "Fenix 3"),
            AntGadget(1, 2431, "Forerunner 235"),
            AntGadget(1, 2432, "Fenix 3 Chronos"),
            AntGadget(1, 2497, "Vivoactive HR"),
            AntGadget(1, 2503, "Forerunner 35"),
            AntGadget(1, 2512, "Oregon 700"),
            AntGadget(1, 2530, "Edge 820"),
            AntGadget(1, 2531, "Edge Explore 820"),
            AntGadget(1, 2544, "Fenix 5S"),
            AntGadget(1, 2567, "Varia UT800"),
            AntGadget(1, 2593, "RD Pod"),
            AntGadget(1, 2604, "Fenix 5X"),
            AntGadget(1, 2623, "Vivosport"),
            AntGadget(1, 2641, "Xero A1"),
            AntGadget(1, 2656, "Approach S60"),
            AntGadget(1, 2691, "Forerunner 935"),
            AntGadget(1, 2697, "Fenix 5"),
            AntGadget(1, 2700, "Vivoactive 3"),
            AntGadget(1, 2713, "Edge 1030"),
            AntGadget(1, 2772, "Vivomove HR"),
            AntGadget(1, 2787, "Vector 3"),
            AntGadget(1, 2798, "Fenix 5X Asia"),
            AntGadget(1, 2806, "Approach Z80"),
            AntGadget(1, 2859, "Descent Mk1"),
            AntGadget(1, 2886, "Forerunner 645"),
            AntGadget(1, 2888, "Forerunner 645 Music"),
            AntGadget(1, 2891, "Forerunner 30"),
            AntGadget(1, 2900, "Fenix 5S Plus"),
            AntGadget(1, 2909, "Edge 130"),
            AntGadget(1, 2927, "Vivosmart 4"),
            AntGadget(1, 2954, "Varia RTL510"),
            AntGadget(1, 2962, "Approach X10"),
            AntGadget(1, 3011, "Edge Explore"),
            AntGadget(1, 3028, "GPSMAP 66s"),
            AntGadget(1, 3049, "Approach S10"),
            AntGadget(1, 3076, "Forerunner 245"),
            AntGadget(1, 3077, "Forerunner 245 Music"),
            AntGadget(1, 3085, "Approach G80"),
            AntGadget(1, 3095, "Edge 1030"),
            AntGadget(1, 3110, "Fenix 5 Plus"),
            AntGadget(1, 3111, "Fenix 5X Plus"),
            AntGadget(1, 3112, "Edge 520 Plus"),
            AntGadget(1, 3113, "Forerunner 945"),
            AntGadget(1, 3121, "Edge 530"),
            AntGadget(1, 3122, "Edge 830"),
            AntGadget(1, 3126, "Instinct"),
            AntGadget(1, 3143, "Descent T1"),
            AntGadget(1, 3192, "Speed Sensor 2"),
            AntGadget(1, 3224, "Vivoactive 4S"),
            AntGadget(1, 3225, "Vivoactive 4"),
            AntGadget(1, 3251, "MARQ Athlete"),
            AntGadget(1, 3258, "Descent Mk2"),
            AntGadget(1, 3282, "Forerunner 45"),
            AntGadget(1, 3284, "GPSMAP 66i"),
            AntGadget(1, 3287, "Fenix 6S Sport"),
            AntGadget(1, 3288, "Fenix 6S"),
            AntGadget(1, 3289, "Fenix 6 Sport"),
            AntGadget(1, 3290, "Fenix 6"),
            AntGadget(1, 3291, "Fenix 6X"),
            AntGadget(1, 3299, "HRM-Dual"),
            AntGadget(1, 3300, "HRM-Pro"),
            AntGadget(1, 3314, "Approach S40"),
            AntGadget(1, 3340, "Varia RVR315"),
            AntGadget(1, 3405, "Garmin Swim 2"),
            AntGadget(1, 3458, "Xero X1i"),
            AntGadget(1, 3466, "Instinct Solar"),
            AntGadget(1, 3540, "Speed and Cadence 2"),
            AntGadget(1, 3542, "Descent Mk2S"),
            AntGadget(1, 3558, "Edge 130 Plus"),
            AntGadget(1, 3570, "Edge 1030 Plus"),
            AntGadget(1, 3578, "Rally RS200"),
            AntGadget(1, 3589, "Forerunner 745"),
            AntGadget(1, 3592, "Varia RTL515"),
            AntGadget(1, 3638, "Enduro"),
            AntGadget(1, 3652, "Forerunner 945 LTE"),
            AntGadget(1, 3694, "GPSMAP 66sr"),
            AntGadget(1, 3704, "Venu 2S"),
            AntGadget(1, 3808, "Varia RCT715"),
            AntGadget(1, 3811, "Varia Rearview"),
            AntGadget(1, 3823, "Approach S12"),
            AntGadget(1, 3843, "Edge 1040"),
            AntGadget(1, 3869, "Forerunner 55"),
            AntGadget(1, 3888, "Instinct 2"),
            AntGadget(1, 3889, "Instinct 2S"),
            AntGadget(1, 3905, "Fenix 7S"),
            AntGadget(1, 3906, "Fenix 7"),
            AntGadget(1, 3907, "Fenix 7X"),
            AntGadget(1, 3927, "Approach G12"),
            AntGadget(1, 3934, "Approach S42"),
            AntGadget(1, 3943, "Epix (Gen 2)"),
            AntGadget(1, 3968, "inReach Mini 2"),
            AntGadget(1, 3990, "Forerunner 255 Music"),
            AntGadget(1, 3991, "Forerunner 255S Music"),
            AntGadget(1, 3992, "Forerunner 255"),
            AntGadget(1, 4005, "Descent G1"),
            AntGadget(1, 4024, "Forerunner 955"),
            AntGadget(1, 4053, "Xero C1 Pro"),
            AntGadget(1, 4061, "Edge 540"),
            AntGadget(1, 4062, "Edge 840"),
            AntGadget(1, 4105, "MARQ Athlete (Gen 2)"),
            AntGadget(1, 4130, "HRMPro+"),
            AntGadget(1, 4132, "Descent G1 Asia"),
            AntGadget(1, 4145, "inReach Mini 2"),
            AntGadget(1, 4155, "Instinct Crossover"),
            AntGadget(1, 4169, "Edge Explore 2"),
            AntGadget(1, 4222, "Descent Mk3"),
            AntGadget(1, 4223, "Descent Mk3i"),
            AntGadget(1, 4233, "Approach S70"),
            AntGadget(1, 4257, "Forerunner 265"),
            AntGadget(1, 4260, "Venu 3"),
            AntGadget(1, 4313, "Epix Pro (Gen 2)"),
            AntGadget(1, 4314, "epix Pro (Gen 2)"),
            AntGadget(1, 4315, "Forerunner 965"),
            AntGadget(1, 4336, "GPSMAP 67"),
            AntGadget(1, 4341, "Enduro 2"),
            AntGadget(1, 4374, "Fenix 7S Pro"),
            AntGadget(1, 4375, "fenix 7 Pro"),
            AntGadget(1, 4376, "Fenix 7X Pro"),
            AntGadget(1, 4394, "Instinct 2X"),
            AntGadget(1, 4432, "Forerunner 165"),
            AntGadget(1, 4433, "Forerunner 165 Music"),
            AntGadget(1, 4440, "Edge 1050"),
            AntGadget(1, 4442, "Descent T2"),
            AntGadget(1, 4446, "HRM-Fit"),
            AntGadget(1, 4518, "Descent X50i"),
            AntGadget(1, 4525, "Rallyx10"),
            AntGadget(1, 4532, "Fenix 8 Solar"),
            AntGadget(1, 4533, "Fenix 8 Solar Large"),
            AntGadget(1, 4534, "Fenix 8 Small"),
            AntGadget(1, 4536, "Fenix 8"),
            AntGadget(1, 4565, "Forerunner 970"),
            AntGadget(1, 4575, "Enduro 3"),
            AntGadget(1, 4583, "Instinct E"),
            AntGadget(1, 4584, "Instinct E"),
            AntGadget(1, 4585, "Instinct 3 Solar"),
            AntGadget(1, 4586, "Instinct 3 Amoled"),
            AntGadget(1, 4587, "Instinct 3"),
            AntGadget(1, 4588, "Descent G2"),
            AntGadget(1, 4603, "Venu X1"),
            AntGadget(1, 4606, "HRM200"),
            AntGadget(1, 4607, "HRM600"),
            AntGadget(1, 4631, "Fenix 8 Pro"),
            AntGadget(1, 4633, "Edge 550"),
            AntGadget(1, 4634, "Edge 850"),
            AntGadget(1, 4647, "Approach S44"),
            AntGadget(1, 4655, "Edge MTB"),
            AntGadget(1, 4656, "Approach S50"),
            AntGadget(1, 4666, "Fenix E"),
            AntGadget(1, 4678, "Instinct Crossover"),
            AntGadget(1, 4759, "Instinct 3 Solar"),
            AntGadget(1, 4825, "Approach J1"),
            AntGadget(1, 10007, "SDM4 Pod"),
            AntGadget(1, 20119, "Training Center"),
            AntGadget(1, 30025, "Golf iOS"),
            AntGadget(1, 65534, "Connect"),
            AntGadget(16, 87, "Timex Ironman GPS"),
            AntGadget(23, 1, "Suunto x9"),
            AntGadget(23, 2, "Suunto x10"),
            AntGadget(23, 3, "Suunto x6"),
            AntGadget(23, 4, "Suunto Memory Belt"),
            AntGadget(23, 5, "Suunto Smart Belt"),
            AntGadget(23, 6, "Suunto t6"),
            AntGadget(23, 7, "Suunto t6c"),
            AntGadget(23, 8, "Suunto t6d"),
            AntGadget(23, 9, "Suunto t4"),
            AntGadget(23, 10, "Suunto t4c"),
            AntGadget(23, 11, "Suunto t4d"),
            AntGadget(23, 12, "Suunto t3"),
            AntGadget(23, 13, "Suunto t3c"),
            AntGadget(23, 14, "Suunto t3d"),
            AntGadget(23, 15, "Suunto m4"),
            AntGadget(23, 16, "Suunto m5"),
            AntGadget(23, 17, "Suunto Quest"),
            AntGadget(23, 18, "Suunto Ambit"),
            AntGadget(23, 19, "Suunto Ambit2"),
            AntGadget(23, 20, "Suunto Ambit2 S"),
            AntGadget(23, 21, "Suunto Ambit2 R"),
            AntGadget(23, 22, "Suunto Ambit3 Peak"),
            AntGadget(23, 23, "Suunto Ambit3 Sport"),
            AntGadget(23, 24, "Suunto Ambit3 Run"),
            AntGadget(23, 25, "Suunto Ambit3 Vertical"),
            AntGadget(23, 26, "Suunto Traverse"),
            AntGadget(23, 27, "Suunto Traverse Alpha"),
            AntGadget(23, 28, "Suunto Spartan Sport"),
            AntGadget(23, 29, "Suunto Spartan Ultra"),
            AntGadget(23, 30, "Suunto Spartan Sport Wrist HR"),
            AntGadget(23, 31, "Suunto Spartan Trainer Wrist HR"),
            AntGadget(23, 32, "Suunto Spartan Sport Wrist HR Baro"),
            AntGadget(23, 33, "Suunto 3"),
            AntGadget(23, 34, "Suunto 9 Baro"),
            AntGadget(23, 35, "Suunto 9"),
            AntGadget(23, 36, "Suunto 5"),
            AntGadget(23, 37, "Suunto EON Core"),
            AntGadget(23, 38, "Suunto EON Steel"),
            AntGadget(23, 39, "Suunto DS"),
            AntGadget(23, 40, "Suunto 7"),
            AntGadget(23, 41, "Suunto EON Steel Black"),
            AntGadget(23, 42, "Suunto 9 Peak"),
            AntGadget(23, 43, "Suunto Cobra3"),
            AntGadget(23, 44, "Suunto D4"),
            AntGadget(23, 45, "Suunto D4f"),
            AntGadget(23, 46, "Suunto D4i"),
            AntGadget(23, 47, "Suunto D6"),
            AntGadget(23, 48, "Suunto D6i"),
            AntGadget(23, 49, "Suunto D9"),
            AntGadget(23, 50, "Suunto D9tx"),
            AntGadget(23, 51, "Suunto DX"),
            AntGadget(23, 52, "Suunto Vyper Novo"),
            AntGadget(23, 53, "Suunto Zoop Novo"),
            AntGadget(23, 54, "Suunto Zoop Novo Rental"),
            AntGadget(23, 55, "Suunto GPS Track POD"),
            AntGadget(23, 56, "Suunto 5 Peak"),
            AntGadget(23, 58, "Suunto 9 Peak Pro"),
            AntGadget(23, 59, "Suunto Vertical"),
            AntGadget(23, 62, "Suunto Ocean"),
            AntGadget(23, 66, "Suunto Race 2"),
            AntGadget(23, 67, "Suunto Vertical 2"),
            AntGadget(32, 8, "TICKRX"),
            AntGadget(32, 33, "ELEMNT RIVAL"),
            AntGadget(32, 35, "TICKR FIT"),
            AntGadget(32, 37, "ELEMNT ROAM"),
            AntGadget(32, 40, "Kickr Core"),
            AntGadget(32, 47, "ELEMNT ROAM 3"),
            AntGadget(32, 57, "ELEMNT ACE"),
            AntGadget(32, 338, "TRACKR RADAR"),
            AntGadget(69, 1, "Stages DashL50"),
            AntGadget(69, 3, "Stages M200"),
            AntGadget(123, 2, "Polar H10"),
            AntGadget(123, 3, "Polar H9"),
            AntGadget(123, 4, "Polar Verity Sense"),
            AntGadget(123, 261, "Polar Pacer Pro"),
            AntGadget(132, 3, "Cycplus C3"),
            AntGadget(258, 4, "Lezyne Super GPS"),
            AntGadget(263, 10, "Assioma Uno"),
            AntGadget(263, 12, "Assioma Duo"),
            AntGadget(267, 1902, "Rider 750"),
            AntGadget(268, 1013, "XX1 Eagle AXS"),
            AntGadget(268, 1075, "GX Eagle"),
            AntGadget(268, 1118, "RED AXS"),
            AntGadget(268, 1139, "Rival XPLR AXS"),
            AntGadget(289, 1, "Karoo"),
            AntGadget(289, 2, "Karoo 2"),
            AntGadget(289, 3, "Karoo 3"),
            AntGadget(294, 802, "COROS PACE 2"),
            AntGadget(294, 804, "COROS PACE 3"),
            AntGadget(294, 805, "COROS PACE Pro"),
            AntGadget(294, 811, "COROS APEX 42mm"),
            AntGadget(294, 812, "COROS APEX 2"),
            AntGadget(294, 821, "COROS APEX"),
            AntGadget(294, 822, "COROS APEX 2 Pro"),
            AntGadget(294, 831, "COROS VERTIX"),
            AntGadget(294, 832, "COROS VERTIX 2"),
            AntGadget(294, 841, "COROS APEX Pro"),
            AntGadget(294, 851, "COROS DURA"),
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
            val descriptor = deviceInfo.descriptor
            if (!descriptor.isNullOrEmpty()) {
                return descriptor
            }

            val name = deviceInfo.productName
            if (!name.isNullOrEmpty()) {
                return name
            }

            val gadget = FindGadget(deviceInfo.manufacturer, deviceInfo.product)
            // by default most newer Garmin UIs append ":" and the compressed serial number (first 20 bits)
            // The ANT ID is only available for ANT devices while the serial number is also available
            // for Bluetooth devices, so use the compressed serial number instead of the compressed
            // ANT ID. For ANT devices both result in the same compressed value.

            val serial = deviceInfo.serialNumber
            if (serial != null) {
                if (gadget != null) {
                    return gadget.name + ":" + (serial and 0xFFFFF).toString()
                } else {
                    return (serial and 0xFFFFF).toString()
                }
            } else if (gadget != null) {
                return gadget.name
            }

            // instead of full hexadecimal ANT IDs shown by some older Garmin UIs, most modern UIs
            // display a compressed decimal ANT ID
            val ant = deviceInfo.antId
            if (ant != null) {
                return compressAntID(ant).toString()
            }

            val index = deviceInfo.deviceIndex
            if (index != null){
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

        // convert ANT id to the compressed ANT ID format - top 4 + bottom 16 bits
        // (0xF000_FFFF -> 0xF_FFFF) - shown in decimal format by many Garmin UIs
        // see also Sensor Settings (record 147) / Name (field 2) and ANT ID (field 0)
        fun compressAntID(ant: Long): Int {
            return (((ant ushr 12) and 0xF0000L) or (ant and 0xFFFFL)).toInt()
        }
    }
}