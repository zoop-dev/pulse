/*  Copyright (C) 2021-2024 Andreas Shimokawa, Arjan Schrijver, Gordon Williams

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
package nodomain.freeyourgadget.gadgetbridge.model

import nodomain.freeyourgadget.gadgetbridge.util.GBToStringBuilder

class NavigationInfoSpec {
    // Next instruction
    var instruction: String? = null

    // Distance to turn (as a string, e.g. "100m")
    var distanceToTurn: String? = null
        set(value) {
            field = value
            distanceToTurnMeters = parseToMeters(value)
        }

    // Normalized distance in meters, derived from distanceToTurn.
    // Read-only from the outside: it's always kept in sync with distanceToTurn, never set it directly.
    // We lose some precision by converting to an integer, but it's acceptable
    var distanceToTurnMeters: Int? = null
        private set

    // One of the ACTION_ constants
    var nextAction: Int = 0

    // Estimated time of arrival
    var ETA: String? = null

    // Distance to target (as a string, e.g. "100m")
    var distanceToTarget: String? = null
        set(value) {
            field = value
            distanceToTargetMeters = parseToMeters(value)
        }

    // Normalized distance in meters, derived from distanceToTarget.
    // Read-only from the outside: it's always kept in sync with distanceToTarget, never set it directly.
    // We lose some precision by converting to an integer, but it's acceptable
    var distanceToTargetMeters: Int? = null
        private set

    // Completion percent
    var completionPercent: Int = 0

    override fun toString(): String {
        val tsb = GBToStringBuilder(this)
        tsb.append("instruction", instruction)
        tsb.append("distanceToTurn", distanceToTurn)
        tsb.append("distanceToTurnMeters", distanceToTurnMeters)
        tsb.append("distanceToTarget", distanceToTarget)
        tsb.append("distanceToTargetMeters", distanceToTargetMeters)
        tsb.append("nextAction", nextAction)
        tsb.append("ETA", ETA)
        tsb.append("completionPercent", completionPercent)
        return tsb.toString()
    }

    companion object {
        const val ACTION_CONTINUE: Int = 1
        const val ACTION_TURN_LEFT: Int = 2
        const val ACTION_TURN_LEFT_SLIGHTLY: Int = 3
        const val ACTION_TURN_LEFT_SHARPLY: Int = 4
        const val ACTION_TURN_RIGHT: Int = 5
        const val ACTION_TURN_RIGHT_SLIGHTLY: Int = 6
        const val ACTION_TURN_RIGHT_SHARPLY: Int = 7
        const val ACTION_KEEP_LEFT: Int = 8
        const val ACTION_KEEP_RIGHT: Int = 9
        const val ACTION_UTURN_LEFT: Int = 10
        const val ACTION_UTURN_RIGHT: Int = 11
        const val ACTION_OFFROUTE: Int = 12
        const val ACTION_ROUNDABOUT_RIGHT: Int = 13
        const val ACTION_ROUNDABOUT_LEFT: Int = 14
        const val ACTION_ROUNDABOUT_STRAIGHT: Int = 15
        const val ACTION_ROUNDABOUT_UTURN: Int = 16
        const val ACTION_FINISH: Int = 17
        const val ACTION_MERGE: Int = 18

        // Conversion factors to meters, per recognized unit suffix.
        private val UNIT_TO_METERS: Map<String, Double> = mapOf(
            "m" to 1.0,
            "km" to 1000.0,
            "yd" to 0.9144,
            "ft" to 0.3048,
            "mi" to 1609.344
        )

        private val DISTANCE_REGEX = Regex("""^\s*([0-9]+(?:[.,][0-9]+)?)\s*([a-zA-Z]+)\s*$""")

        private fun parseToMeters(raw: String?): Int? {
            if (raw.isNullOrBlank()) return null
            val match = DISTANCE_REGEX.matchEntire(raw) ?: return null
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            val factor = UNIT_TO_METERS[unit] ?: return null
            return (value * factor).toInt()
        }
    }
}
