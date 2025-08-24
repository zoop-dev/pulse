package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts

import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart

object ChartDataRepository {
    var chartData: List<WorkoutChart>? = null

    fun clear() {
        chartData = null
    }
}