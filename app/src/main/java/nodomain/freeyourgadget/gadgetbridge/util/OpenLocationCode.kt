/*  Copyright (C) 2026 Jeff Laflamme

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
package nodomain.freeyourgadget.gadgetbridge.util

import java.util.Locale
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Offline Open Location Code ("Plus Code") support, following the open specification:
 * https://github.com/google/open-location-code/blob/main/Documentation/Specification/specification.md
 *
 * Full codes (7P2QQM9X+83) decode on their own. Short codes (QM9X+83) drop leading digits
 * and need a reference position nearby, see [recoverNearest].
 */
object OpenLocationCode {
    private const val ALPHABET = "23456789CFGHJMPQRVWX"
    private const val SEPARATOR = '+'
    private const val PADDING = '0'
    private const val SEPARATOR_POSITION = 8
    private const val PAIR_CODE_LENGTH = 10
    private const val MAX_DIGIT_COUNT = 15
    private const val GRID_COLUMNS = 4
    private const val GRID_ROWS = 5
    private const val PAIR_FIRST_PLACE_VALUE = 160000L // 20^4
    private const val PAIR_PRECISION = 8000L // 20^3
    private const val GRID_LAT_FIRST_PLACE_VALUE = 625L // GRID_ROWS^4
    private const val GRID_LNG_FIRST_PLACE_VALUE = 256L // GRID_COLUMNS^4
    private const val FINAL_LAT_PRECISION = PAIR_PRECISION * 3125L // GRID_ROWS^5
    private const val FINAL_LNG_PRECISION = PAIR_PRECISION * 1024L // GRID_COLUMNS^5

    data class CodeArea(
        val latitudeLo: Double,
        val longitudeLo: Double,
        val latitudeHi: Double,
        val longitudeHi: Double,
        val codeLength: Int
    ) {
        val centerLatitude: Double get() = min(90.0, (latitudeLo + latitudeHi) / 2.0)
        val centerLongitude: Double get() = min(180.0, (longitudeLo + longitudeHi) / 2.0)
    }

    @JvmStatic
    fun isValid(code: String?): Boolean {
        if (code.isNullOrEmpty()) {
            return false
        }
        val c = code.uppercase(Locale.ROOT)
        val sep = c.indexOf(SEPARATOR)
        if (sep == -1 || sep != c.lastIndexOf(SEPARATOR) || sep > SEPARATOR_POSITION || sep % 2 == 1) {
            return false
        }
        if (c.contains(PADDING)) {
            // padding only in full codes, never first, an even count directly before the separator
            if (sep < SEPARATOR_POSITION || c[0] == PADDING || c.length > sep + 1) {
                return false
            }
            val padStart = c.indexOf(PADDING)
            if (sep - padStart != c.count { it == PADDING } || (sep - padStart) % 2 == 1) {
                return false
            }
        }
        if (c.length - sep - 1 == 1 || (sep == 0 && c.length < 3)) {
            // a single character after the separator, or a bare separator
            return false
        }
        return c.all { it == SEPARATOR || it == PADDING || ALPHABET.indexOf(it) >= 0 }
    }

    @JvmStatic
    fun isFull(code: String?): Boolean {
        if (!isValid(code) || code!!.indexOf(SEPARATOR) != SEPARATOR_POSITION) {
            return false
        }
        val c = code.uppercase(Locale.ROOT)
        return ALPHABET.indexOf(c[0]) * 20 < 180 && ALPHABET.indexOf(c[1]) * 20 < 360
    }

    @JvmStatic
    fun isShort(code: String?): Boolean {
        return isValid(code) && code!!.indexOf(SEPARATOR) < SEPARATOR_POSITION
    }

    @JvmStatic
    fun decode(code: String): CodeArea {
        require(isFull(code)) { "not a full Plus Code: $code" }
        val clean = code.uppercase(Locale.ROOT)
            .replace(SEPARATOR.toString(), "")
            .replace(PADDING.toString(), "")
            .take(MAX_DIGIT_COUNT)

        var lat = -90L * PAIR_PRECISION
        var lng = -180L * PAIR_PRECISION
        var place = PAIR_FIRST_PLACE_VALUE
        val pairDigits = min(clean.length, PAIR_CODE_LENGTH)
        for (i in 0 until pairDigits step 2) {
            lat += ALPHABET.indexOf(clean[i]) * place
            lng += ALPHABET.indexOf(clean[i + 1]) * place
            if (i < pairDigits - 2) {
                place /= 20
            }
        }
        var latStep = place.toDouble() / PAIR_PRECISION
        var lngStep = place.toDouble() / PAIR_PRECISION

        var gridLat = 0L
        var gridLng = 0L
        if (clean.length > PAIR_CODE_LENGTH) {
            var rowPlace = GRID_LAT_FIRST_PLACE_VALUE
            var colPlace = GRID_LNG_FIRST_PLACE_VALUE
            for (i in PAIR_CODE_LENGTH until clean.length) {
                val value = ALPHABET.indexOf(clean[i])
                gridLat += (value / GRID_COLUMNS) * rowPlace
                gridLng += (value % GRID_COLUMNS) * colPlace
                if (i < clean.length - 1) {
                    rowPlace /= GRID_ROWS
                    colPlace /= GRID_COLUMNS
                }
            }
            latStep = rowPlace.toDouble() / FINAL_LAT_PRECISION
            lngStep = colPlace.toDouble() / FINAL_LNG_PRECISION
        }

        val latLo = lat.toDouble() / PAIR_PRECISION + gridLat.toDouble() / FINAL_LAT_PRECISION
        val lngLo = lng.toDouble() / PAIR_PRECISION + gridLng.toDouble() / FINAL_LNG_PRECISION
        return CodeArea(latLo, lngLo, latLo + latStep, lngLo + lngStep, clean.length)
    }

    @JvmStatic
    @JvmOverloads
    fun encode(latitude: Double, longitude: Double, codeLength: Int = PAIR_CODE_LENGTH): String {
        // the specification allows 2 to 10 digits in pairs, then any length
        require(codeLength >= 2 && (codeLength >= PAIR_CODE_LENGTH || codeLength % 2 == 0)) {
            "Illegal code length $codeLength"
        }
        val length = min(codeLength, MAX_DIGIT_COUNT)
        var lat = latitude.coerceIn(-90.0, 90.0)
        val lng = normalizeLongitude(longitude)
        if (lat == 90.0) {
            lat -= latitudePrecision(length)
        }
        // round away floating point noise first, as the reference implementations do
        var latVal = floor(((lat + 90.0) * FINAL_LAT_PRECISION * 1e6).roundToLong() / 1e6).toLong()
        var lngVal = floor(((lng + 180.0) * FINAL_LNG_PRECISION * 1e6).roundToLong() / 1e6).toLong()

        val code = StringBuilder()
        if (length > PAIR_CODE_LENGTH) {
            for (i in 0 until MAX_DIGIT_COUNT - PAIR_CODE_LENGTH) {
                code.insert(0, ALPHABET[((latVal % GRID_ROWS) * GRID_COLUMNS + lngVal % GRID_COLUMNS).toInt()])
                latVal /= GRID_ROWS
                lngVal /= GRID_COLUMNS
            }
        } else {
            latVal /= 3125L
            lngVal /= 1024L
        }
        for (i in 0 until PAIR_CODE_LENGTH / 2) {
            code.insert(0, ALPHABET[(lngVal % 20).toInt()])
            code.insert(0, ALPHABET[(latVal % 20).toInt()])
            latVal /= 20
            lngVal /= 20
        }
        code.insert(SEPARATOR_POSITION, SEPARATOR)
        if (length >= SEPARATOR_POSITION) {
            return code.substring(0, length + 1)
        }
        return code.substring(0, length) + PADDING.toString().repeat(SEPARATOR_POSITION - length) + SEPARATOR
    }

    /**
     * Full code nearest to the reference position for a short code.
     * A full code is returned unchanged (upper case).
     */
    @JvmStatic
    fun recoverNearest(shortCode: String, referenceLatitude: Double, referenceLongitude: Double): String {
        if (isFull(shortCode)) {
            return shortCode.uppercase(Locale.ROOT)
        }
        require(isShort(shortCode)) { "not a valid Plus Code: $shortCode" }
        val refLat = referenceLatitude.coerceIn(-90.0, 90.0)
        val refLng = normalizeLongitude(referenceLongitude)
        val code = shortCode.uppercase(Locale.ROOT)

        val paddingLength = SEPARATOR_POSITION - code.indexOf(SEPARATOR)
        val resolution = 20.0.pow(2 - paddingLength / 2)
        val half = resolution / 2.0

        val area = decode(encode(refLat, refLng).substring(0, paddingLength) + code)
        var lat = area.centerLatitude
        var lng = area.centerLongitude
        if (refLat + half < lat && lat - resolution >= -90.0) {
            lat -= resolution
        } else if (refLat - half > lat && lat + resolution <= 90.0) {
            lat += resolution
        }
        if (refLng + half < lng) {
            lng -= resolution
        } else if (refLng - half > lng) {
            lng += resolution
        }
        return encode(lat, lng, area.codeLength)
    }

    private fun normalizeLongitude(longitude: Double): Double {
        return (longitude + 180.0).mod(360.0) - 180.0
    }

    private fun latitudePrecision(codeLength: Int): Double {
        if (codeLength <= PAIR_CODE_LENGTH) {
            return 20.0.pow(Math.floorDiv(codeLength, -2) + 2)
        }
        return 20.0.pow(-3) / GRID_ROWS.toDouble().pow(codeLength - PAIR_CODE_LENGTH)
    }
}
