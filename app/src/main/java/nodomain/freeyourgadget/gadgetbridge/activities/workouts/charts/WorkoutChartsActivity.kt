package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts

import android.os.Bundle
import android.widget.Toast
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.chip.Chip
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DurationXLabelFormatter
import nodomain.freeyourgadget.gadgetbridge.activities.charts.marker.ValueMarker
import nodomain.freeyourgadget.gadgetbridge.databinding.WorkoutChartsBinding
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart
import kotlin.math.max

class WorkoutChartsActivity : AbstractGBActivity() {
    private lateinit var binding: WorkoutChartsBinding
    private var chartData: List<WorkoutChart>? = null
    val selectedCharts = mutableListOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WorkoutChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        chartData = ChartDataRepository.chartData

        if (chartData == null) {
            Toast.makeText(this, "No charts data found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        ChartDataRepository.clear()

        val chartTextColor = GBApplication.getSecondaryTextColor(baseContext);
        binding.workoutDataChart.xAxis.apply {
            setDrawLabels(true)
            setDrawGridLines(false)
            setDrawLimitLinesBehindData(true)
            isEnabled = true
            textColor = chartTextColor
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = DurationXLabelFormatter("mm:ss")
        }
        binding.workoutDataChart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawTopYLabelEntry(true)
            textColor = chartTextColor
            isEnabled = true
        }
        binding.workoutDataChart.axisRight.apply {
            setDrawGridLines(false)
            setDrawTopYLabelEntry(true)
            textColor = chartTextColor
            isEnabled = true
        }
        binding.workoutDataChart.description.isEnabled = false;
        binding.workoutDataChart.legend.textColor = GBApplication.getTextColor(baseContext);

        val initChartId = intent.getStringExtra(INIT_CHART_ID) ?: "none"
        selectedCharts.add(0, initChartId);
        setupChipGroup(initChartId);
        refreshChart()
    }

    fun setupChipGroup(initChartId: String) {
        for (chart in chartData!!) {
            val chip = Chip(this).apply {
                text = chart.title
                isCheckable = true
                isClickable = true
                tag = chart.id
                isChecked = chart.id.equals(initChartId)
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                val tag = chip.tag
                if (isChecked) {
                    if (selectedCharts.size >= 2) {
                        chip.isChecked = false
                        Toast.makeText(this, "You can only compare two items at a time.", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedCharts.add(tag)
                        refreshChart()
                    }
                } else {
                    if (selectedCharts.size == 1 && selectedCharts.contains(tag)) {
                        chip.isChecked = true
                        Toast.makeText(this, "There should be at least one item selected.", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedCharts.remove(tag)
                        refreshChart()
                    }
                }
            }
            binding.workoutDataChartChipGroup.addView(chip)
        }
    }

    fun refreshChart() {
        val lineDataSets = mutableListOf<ILineDataSet>()
        val lineDataSetsMarkerFormatters = mutableListOf<ValueFormatter?>()
        val lineDataSetsMarkerUnits = mutableListOf<String?>()
        var leftY = true
        selectedCharts.forEach { selectedChart ->
            val workoutChart = chartData?.find { it.id == selectedChart } ?: return@forEach
            val dataSet = workoutChart.chartData.getDataSetByIndex(0) as? LineDataSet ?: return@forEach
            dataSet.highLightColor = this.getColor(R.color.chart_highline_dolor)
            dataSet.highlightLineWidth = 1f;
            dataSet.axisDependency = if(leftY) YAxis.AxisDependency.LEFT else YAxis.AxisDependency.RIGHT;
            lineDataSets.add(dataSet)
            lineDataSetsMarkerFormatters.add(workoutChart.chartYLabelFormatter);
            lineDataSetsMarkerUnits.add(workoutChart.unitString);
            val axis = if (leftY) binding.workoutDataChart.axisLeft else binding.workoutDataChart.axisRight
            axis.valueFormatter = workoutChart.chartYLabelFormatter ?: DefaultAxisValueFormatter(0)
            leftY = false
        }
        if (selectedCharts.size == 1) {
            val selectedChartId = selectedCharts.first()
            val workoutChart = chartData?.find { it.id == selectedChartId } ?: return
            binding.workoutDataChart.axisRight.valueFormatter = workoutChart?.chartYLabelFormatter ?: DefaultAxisValueFormatter(0)
        }
        val lineData = LineData(lineDataSets)
        binding.workoutDataChart.data = lineData;
        binding.workoutDataChart.marker = ValueMarker(this, lineData, lineDataSetsMarkerFormatters, lineDataSetsMarkerUnits);
        binding.workoutDataChart.highlightValues(null)
        binding.workoutDataChart.invalidate()
    }

    companion object {
        const val INIT_CHART_ID = "INIT_CHART_ID"
    }
}