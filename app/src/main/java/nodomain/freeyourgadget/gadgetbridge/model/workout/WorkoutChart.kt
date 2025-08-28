package nodomain.freeyourgadget.gadgetbridge.model.workout

import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IDataSet

data class WorkoutChart @JvmOverloads constructor(
    val id: String,
    val title: String,
    val group: String,
    val chartData: ChartData<out IDataSet<out Entry>>,
    var chartYLabelFormatter: ValueFormatter? = null,
    var unitString: String? = null,
    val lineChart: (BarLineChartBase<*>) -> Unit = {}
)
