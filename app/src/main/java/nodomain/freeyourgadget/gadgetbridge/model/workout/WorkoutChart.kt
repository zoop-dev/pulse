package nodomain.freeyourgadget.gadgetbridge.model.workout

import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.formatter.ValueFormatter

data class WorkoutChart @JvmOverloads constructor(
    val id: String,
    val title: String,
    val group: String,
    val chartData: ChartData<*>,
    var chartYLabelFormatter: ValueFormatter? = null,
    var unitString: String? = null,
)
