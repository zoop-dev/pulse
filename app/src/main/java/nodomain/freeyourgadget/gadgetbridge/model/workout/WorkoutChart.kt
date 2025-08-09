package nodomain.freeyourgadget.gadgetbridge.model.workout

import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.formatter.ValueFormatter

data class WorkoutChart @JvmOverloads constructor(
    val title: String,
    val chartData: ChartData<*>,
    var chartYLabelFormatter: ValueFormatter? = null,
)
