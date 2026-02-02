package nodomain.freeyourgadget.gadgetbridge.activities.charts

import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser

object VO2MaxRanges {
    // Percentile boundaries: superior (95-100), excellent (80-95), good (60-80), fair (40-60), poor (0-40)
    val PERCENTILES = floatArrayOf(0.95f, 0.80f, 0.60f, 0.40f, 0.0f)

    fun calculateVO2MaxPercentile(vo2MaxValue: Float, age: Int, gender: Int): Float {
        val ranges = getVO2MaxRanges(age, gender)

        // Determine which band the value falls into (0=superior, 1=excellent, 2=good, 3=fair, 4=poor)
        val bandIndex = when {
            vo2MaxValue >= ranges[0] -> 0
            vo2MaxValue >= ranges[1] -> 1
            vo2MaxValue >= ranges[2] -> 2
            vo2MaxValue >= ranges[3] -> 3
            else -> 4
        }

        // Get the upper and lower bounds for this band
        val upperBound = if (bandIndex == 0) {
            // Superior band. Highest recorded VO2 Max
            97.5f
        } else {
            ranges[bandIndex - 1]
        }
        val lowerBound = ranges[bandIndex]

        // Scale the percentile within the band
        val positionInBand = if (upperBound > lowerBound) {
            ((vo2MaxValue - lowerBound) / (upperBound - lowerBound)).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        val upperPercentile = if (bandIndex == 0) {
            1.0f  // Top of superior band is 100th percentile
        } else {
            PERCENTILES[bandIndex - 1]
        }

        return PERCENTILES[bandIndex] + (positionInBand * (upperPercentile - PERCENTILES[bandIndex]))
    }

    /**
     * Returns a 5-element float array with VO2 max lower bounds (inclusive) for each level. The levels are ordered from highest to
     * lowest: superior, excellent, good, fair, poor.
     */
    fun getVO2MaxRanges(age: Int, gender: Int): FloatArray {
        return when (gender) {
            ActivityUser.GENDER_MALE -> when {
                age <= 29 -> floatArrayOf(55.4f, 51.1f, 45.4f, 41.7f, 0f)
                age in 30..39 -> floatArrayOf(54f, 48.3f, 44f, 40.5f, 0f)
                age in 40..49 -> floatArrayOf(52.5f, 46.4f, 42.4f, 38.5f, 0f)
                age in 50..59 -> floatArrayOf(48.9f, 43.4f, 39.2f, 35.6f, 0f)
                age in 60..69 -> floatArrayOf(45.7f, 39.5f, 35.5f, 32.3f, 0f)
                else -> floatArrayOf(42.1f, 36.7f, 32.3f, 29.4f, 0f)
            }

            ActivityUser.GENDER_FEMALE -> when {
                age <= 29 -> floatArrayOf(49.6f, 43.9f, 39.5f, 36.1f, 0f)
                age in 30..39 -> floatArrayOf(47.4f, 42.4f, 37.8f, 34.4f, 0f)
                age in 40..49 -> floatArrayOf(45.3f, 39.7f, 36.3f, 33f, 0f)
                age in 50..59 -> floatArrayOf(41.1f, 36.7f, 33f, 30.1f, 0f)
                age in 60..69 -> floatArrayOf(37.8f, 33f, 30f, 27.5f, 0f)
                else -> floatArrayOf(36.7f, 30.9f, 28.1f, 25.9f, 0f)
            }

            else -> {
                // Average them (?)
                val rangesMale = getVO2MaxRanges(age, ActivityUser.GENDER_MALE)
                val rangesFemale = getVO2MaxRanges(age, ActivityUser.GENDER_FEMALE)
                floatArrayOf(
                    (rangesMale[0] + rangesFemale[0]) / 2f,
                    (rangesMale[1] + rangesFemale[1]) / 2f,
                    (rangesMale[2] + rangesFemale[2]) / 2f,
                    (rangesMale[3] + rangesFemale[3]) / 2f,
                    (rangesMale[4] + rangesFemale[4]) / 2f,
                )
            }
        }
    }
}
