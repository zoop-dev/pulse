package nodomain.freeyourgadget.gadgetbridge.model.workout

import com.github.mikephil.charting.data.ChartData

data class WorkoutChart(
    val title: String,
    val chartData: ChartData<*>
)
