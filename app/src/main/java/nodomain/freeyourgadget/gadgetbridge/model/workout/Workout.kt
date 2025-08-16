package nodomain.freeyourgadget.gadgetbridge.model.workout

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData

data class Workout @JvmOverloads constructor(
    val summary: BaseActivitySummary,
    val data: ActivitySummaryData,
    val charts: List<WorkoutChart> = emptyList()
)
