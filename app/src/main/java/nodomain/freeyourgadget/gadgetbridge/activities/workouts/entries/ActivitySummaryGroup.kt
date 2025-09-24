package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries

import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import org.apache.commons.lang3.tuple.Pair

object ActivitySummaryGroup {
    fun buildGroupedList(activitySummaryData: ActivitySummaryData): Map<String, List<Pair<String, ActivitySummaryEntry>>> {
        // Initialize activeGroups with the initial expected order and empty arrays
        val activeGroups = DEFAULT_GROUPS.keys
            .associateWith { mutableListOf<Pair<String, ActivitySummaryEntry>>() }
            .toMutableMap()

        activitySummaryData.keys
            .filterNot { it.startsWith("internal") }
            .forEach { key ->
                val item = activitySummaryData[key]
                // Use the group if specified in the entry, otherwise fallback to the default mapping from getDefaultGroup
                val groups: List<String> = item.group?.let { listOf(it) } ?: getDefaultGroups(key)

                for (groupName in groups) {
                    // If the group is not defined the default groups, it will be added to the end
                    val group = activeGroups.getOrPut(groupName) { mutableListOf() }
                    group.add(Pair.of<String, ActivitySummaryEntry>(key, item))
                }
            }

        // activeGroups is already ordered
        return activeGroups
    }

    /**
     * Find the default group key for a given entry. Defaults to Activity if not found.
     */
    private fun getDefaultGroups(searchItem: String): List<String> {
        return DEFAULT_GROUPS.entries
            .filter { (_, items) -> items.contains(searchItem) }
            .map { (key, _) -> key }
            .ifEmpty { listOf(ActivitySummaryEntries.GROUP_OTHER) }
    }

    /**
     * A map of group key to list of entries that should be in that group by default.
     */
    val DEFAULT_GROUPS: Map<String, List<String>> = object : LinkedHashMap<String, List<String>>() {
        init {
            // NB: Default group Activity must be present in this definition, otherwise it wouldn't
            // be shown.
            put(
                ActivitySummaryEntries.GROUP_ACTIVITY, listOf<String>(
                    ActivitySummaryEntries.ACTIVE_SECONDS,
                    ActivitySummaryEntries.DISTANCE_METERS,
                    ActivitySummaryEntries.CALORIES_ACTIVE,
                    ActivitySummaryEntries.HR_AVG,
                    ActivitySummaryEntries.STEPS,
                    ActivitySummaryEntries.STRIDE_TOTAL,
                    ActivitySummaryEntries.STEP_RATE_SUM,
                    ActivitySummaryEntries.STANDING_TIME,
                    ActivitySummaryEntries.STANDING_COUNT,
                    ActivitySummaryEntries.PACE_AVG_SECONDS_KM,
                    ActivitySummaryEntries.SPEED_AVG,
                    ActivitySummaryEntries.SPEED_AVG,
                    ActivitySummaryEntries.ELEVATION_GAIN,
                    ActivitySummaryEntries.ASCENT_METERS,
                )
            )

            // Sets
            put(ActivitySummaryEntries.SETS, listOf<String>())

            // Intervals
            put(ActivitySummaryEntries.GROUP_INTERVALS, listOf<String>())

            // Heart rate
            put(
                ActivitySummaryEntries.GROUP_HEART_RATE, listOf<String>(
                    ActivitySummaryEntries.HR_AVG,
                    ActivitySummaryEntries.HR_MAX,
                    ActivitySummaryEntries.HR_MIN,
                )
            )

            // Heart rate zones
            put(
                ActivitySummaryEntries.GROUP_HEART_RATE_ZONES, listOf<String>(
                    ActivitySummaryEntries.HR_ZONE_NA,
                    ActivitySummaryEntries.HR_ZONE_WARM_UP,
                    ActivitySummaryEntries.HR_ZONE_FAT_BURN,
                    ActivitySummaryEntries.HR_ZONE_EASY,
                    ActivitySummaryEntries.HR_ZONE_AEROBIC,
                    ActivitySummaryEntries.HR_ZONE_ANAEROBIC,
                    ActivitySummaryEntries.HR_ZONE_THRESHOLD,
                    ActivitySummaryEntries.HR_ZONE_EXTREME,
                    ActivitySummaryEntries.HR_ZONE_MAXIMUM,
                )
            )

            // Speed
            put(
                ActivitySummaryEntries.GROUP_SPEED, listOf<String>(
                    ActivitySummaryEntries.SPEED_AVG,
                    ActivitySummaryEntries.SPEED_MAX,
                    ActivitySummaryEntries.SPEED_MIN,
                    ActivitySummaryEntries.PACE_AVG_SECONDS_KM,
                    ActivitySummaryEntries.PACE_MIN,
                    ActivitySummaryEntries.PACE_MAX,
                    "averageSpeed2",
                )
            )

            // Cadence
            put(
                ActivitySummaryEntries.GROUP_CADENCE, listOf<String>(
                    ActivitySummaryEntries.CADENCE_AVG,
                    ActivitySummaryEntries.CADENCE_MAX,
                    ActivitySummaryEntries.CADENCE_MIN,
                )
            )

            // Elevation
            put(
                ActivitySummaryEntries.GROUP_ELEVATION, listOf<String>(
                    ActivitySummaryEntries.TOTAL_ASCENT,
                    ActivitySummaryEntries.TOTAL_DESCENT,
                    ActivitySummaryEntries.ASCENT_METERS,
                    ActivitySummaryEntries.DESCENT_METERS,
                    ActivitySummaryEntries.ALTITUDE_MAX,
                    ActivitySummaryEntries.ALTITUDE_MIN,
                    ActivitySummaryEntries.ALTITUDE_AVG,
                    ActivitySummaryEntries.ALTITUDE_BASE,
                    ActivitySummaryEntries.ASCENT_SECONDS,
                    ActivitySummaryEntries.DESCENT_SECONDS,
                    ActivitySummaryEntries.FLAT_SECONDS,
                    ActivitySummaryEntries.ASCENT_DISTANCE,
                    ActivitySummaryEntries.DESCENT_DISTANCE,
                    ActivitySummaryEntries.FLAT_DISTANCE,
                    ActivitySummaryEntries.ELEVATION_GAIN,
                    ActivitySummaryEntries.ELEVATION_LOSS,
                )
            )

            // Power
            put(
                ActivitySummaryEntries.GROUP_POWER, listOf<String>(
                    ActivitySummaryEntries.CYCLING_POWER_AVERAGE,
                    ActivitySummaryEntries.CYCLING_POWER_MIN,
                    ActivitySummaryEntries.CYCLING_POWER_MAX,
                    ActivitySummaryEntries.AVG_POWER,
                    ActivitySummaryEntries.MAX_POWER,
                    ActivitySummaryEntries.MAX_POWER_SEATING,
                    ActivitySummaryEntries.MAX_POWER_STANDING,
                    ActivitySummaryEntries.NORMALIZED_POWER,
                    ActivitySummaryEntries.AVG_LEFT_POWER_PHASE,
                    ActivitySummaryEntries.AVG_LEFT_POWER_PHASE_PEAK,
                    ActivitySummaryEntries.AVG_RIGHT_POWER_PHASE,
                    ActivitySummaryEntries.AVG_RIGHT_POWER_PHASE_PEAK,
                    ActivitySummaryEntries.AVG_POWER_SEATING,
                    ActivitySummaryEntries.AVG_POWER_STANDING,
                )
            )

            // Strokes
            put(
                ActivitySummaryEntries.GROUP_STROKES, listOf<String>(
                    ActivitySummaryEntries.STROKE_DISTANCE_AVG,
                    ActivitySummaryEntries.STROKE_AVG_PER_SECOND,
                    ActivitySummaryEntries.STROKES,
                    ActivitySummaryEntries.STROKE_RATE_AVG,
                    ActivitySummaryEntries.STROKE_RATE_MAX,
                )
            )

            // Jumps
            put(
                ActivitySummaryEntries.GROUP_JUMPS, listOf<String>(
                    ActivitySummaryEntries.JUMPS,
                    ActivitySummaryEntries.JUMP_RATE_AVG,
                    ActivitySummaryEntries.JUMP_RATE_MAX,
                    ActivitySummaryEntries.JUMP_ROPE_LONGEST_STREAK,
                    ActivitySummaryEntries.JUMP_ROPE_INTERRUPTIONS,
                )
            )

            // Swimming
            put(
                ActivitySummaryEntries.GROUP_SWIMMING, listOf<String>(
                    ActivitySummaryEntries.POOL_LENGTH,
                    ActivitySummaryEntries.SWIM_AVG_CADENCE,
                    ActivitySummaryEntries.SWOLF_INDEX,
                    ActivitySummaryEntries.SWOLF_AVG,
                    ActivitySummaryEntries.SWOLF_MAX,
                    ActivitySummaryEntries.SWOLF_MIN,
                    ActivitySummaryEntries.SWIM_STYLE,
                )
            )

            // Cycling
            put(
                ActivitySummaryEntries.GROUP_CYCLING, listOf<String>(
                    ActivitySummaryEntries.LEFT_RIGHT_BALANCE,
                    ActivitySummaryEntries.AVG_PEDAL_SMOOTHNESS,
                    ActivitySummaryEntries.AVG_TORQUE_EFFECTIVENESS,
                    ActivitySummaryEntries.AVG_LEFT_PCO,
                    ActivitySummaryEntries.AVG_LEFT_POWER_PHASE,
                    ActivitySummaryEntries.AVG_LEFT_POWER_PHASE_PEAK,
                    ActivitySummaryEntries.AVG_RIGHT_PCO,
                    ActivitySummaryEntries.AVG_RIGHT_POWER_PHASE,
                    ActivitySummaryEntries.AVG_RIGHT_POWER_PHASE_PEAK,
                    ActivitySummaryEntries.AVG_POWER_STANDING,
                    ActivitySummaryEntries.MAX_POWER_STANDING,
                    ActivitySummaryEntries.AVG_POWER_SEATING,
                    ActivitySummaryEntries.MAX_POWER_SEATING,
                    ActivitySummaryEntries.AVG_CADENCE_STANDING,
                    ActivitySummaryEntries.AVG_CADENCE_SEATING,
                    ActivitySummaryEntries.MAX_CADENCE_STANDING,
                    ActivitySummaryEntries.MAX_CADENCE_SEATING,
                    ActivitySummaryEntries.FRONT_GEAR_SHIFTS,
                    ActivitySummaryEntries.REAR_GEAR_SHIFTS,
                )
            )

            // Training effect
            put(
                ActivitySummaryEntries.GROUP_TRAINING_EFFECT, listOf<String>(
                    ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC,
                    ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC,
                    ActivitySummaryEntries.WORKOUT_LOAD,
                    ActivitySummaryEntries.TRAINING_LOAD,
                    ActivitySummaryEntries.INTENSITY_FACTOR,
                    ActivitySummaryEntries.TRAINING_STRESS_SCORE,
                    ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE,
                    ActivitySummaryEntries.RECOVERY_TIME,
                    ActivitySummaryEntries.LACTATE_THRESHOLD_HR,
                )
            )

            // Laps
            put(
                ActivitySummaryEntries.GROUP_LAPS, listOf<String>(
                    ActivitySummaryEntries.LAP_PACE_AVERAGE,
                    ActivitySummaryEntries.LAPS,
                    ActivitySummaryEntries.LANE_LENGTH,
                )
            )

            // Pace
            put(ActivitySummaryEntries.GROUP_PACE, listOf<String>())

            // Running form
            put(
                ActivitySummaryEntries.GROUP_RUNNING_FORM, listOf<String>(
                    ActivitySummaryEntries.GROUND_CONTACT_TIME_AVG,
                    ActivitySummaryEntries.IMPACT_AVG,
                    ActivitySummaryEntries.IMPACT_MAX,
                    ActivitySummaryEntries.SWING_ANGLE_AVG,
                    ActivitySummaryEntries.FORE_FOOT_LANDINGS,
                    ActivitySummaryEntries.MID_FOOT_LANDINGS,
                    ActivitySummaryEntries.BACK_FOOT_LANDINGS,
                    ActivitySummaryEntries.EVERSION_ANGLE_AVG,
                    ActivitySummaryEntries.EVERSION_ANGLE_MAX,
                )
            )

            // Diving
            put(
                ActivitySummaryEntries.GROUP_DIVING, listOf<String>(
                    ActivitySummaryEntries.AVG_DEPTH,
                    ActivitySummaryEntries.MAX_DEPTH,
                    ActivitySummaryEntries.START_CNS,
                    ActivitySummaryEntries.END_CNS,
                    ActivitySummaryEntries.START_N2,
                    ActivitySummaryEntries.END_N2,
                    ActivitySummaryEntries.DIVE_NUMBER,
                    ActivitySummaryEntries.BOTTOM_TIME,
                )
            )

            // Recovery Heart Rate
            put(ActivitySummaryEntries.GROUP_RECOVERY_HEART_RATE, listOf<String>())

            // Respiratory Rate
            put(
                ActivitySummaryEntries.GROUP_RESPIRATORY_RATE, listOf<String>(
                    ActivitySummaryEntries.RESPIRATION_MIN,
                    ActivitySummaryEntries.RESPIRATION_MAX,
                    ActivitySummaryEntries.RESPIRATION_AVG,
                )
            )

            // Other
            put(
                ActivitySummaryEntries.GROUP_OTHER, listOf<String>(
                    ActivitySummaryEntries.ESTIMATED_SWEAT_LOSS,
                    ActivitySummaryEntries.CALORIES_BURNT,
                    ActivitySummaryEntries.CALORIES_ACTIVE,
                    ActivitySummaryEntries.CALORIES_RESTING,
                    ActivitySummaryEntries.STRIDE_AVG,
                    ActivitySummaryEntries.STRIDE_MAX,
                    ActivitySummaryEntries.STRIDE_MIN,
                    ActivitySummaryEntries.STEP_LENGTH_AVG,
                    ActivitySummaryEntries.CADENCE_AVG,
                    ActivitySummaryEntries.CADENCE_MAX,
                    ActivitySummaryEntries.CADENCE_MIN,
                    ActivitySummaryEntries.STEP_RATE_AVG,
                    ActivitySummaryEntries.STEP_RATE_MAX,
                )
            )
        }
    }
}
