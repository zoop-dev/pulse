package nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.children
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineScatterCandleRadarDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DurationXLabelFormatter
import nodomain.freeyourgadget.gadgetbridge.activities.charts.marker.ValueMarker
import nodomain.freeyourgadget.gadgetbridge.databinding.WorkoutChartsBinding
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart

class WorkoutChartsActivity : AbstractGBActivity(), MenuProvider {

    private var context: Context = GBApplication.getContext()
    private lateinit var binding: WorkoutChartsBinding
    private var chartData: List<WorkoutChart>? = null
    val selectedCharts = mutableListOf<Any>()

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WorkoutChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addMenuProvider(this)
        chartData = ChartDataRepository.chartData

        if (chartData == null) {
            Toast.makeText(this, "No charts data found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val chartTextColor = GBApplication.getSecondaryTextColor(context)
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
        binding.workoutDataChart.description.isEnabled = false
        binding.workoutDataChart.legend.textColor = GBApplication.getTextColor(context)

        val initChartId = intent.getStringExtra(INIT_CHART_ID) ?: "none"
        selectedCharts.add(0, initChartId)
        setupChipGroup(binding.workoutDataChartChipGroup, initChartId)
        refreshChart()
    }

    override fun onDestroy() {
        ChartDataRepository.clear()
        super.onDestroy()
    }

    fun setupChipGroup(chipGroup: ChipGroup, initChartId: String) {
        for (chart in chartData!!) {
            val chip = Chip(this).apply {
                text = chart.title
                isCheckable = true
                isClickable = true
                tag = chart.id
                isChecked = chart.id == initChartId
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                val tag = chip.tag
                val checkedCount = binding.workoutDataChartChipGroup.children
                    .filterIsInstance<Chip>()
                    .count { it.isChecked }
                if (isChecked) {
                    if (checkedCount > 2) {
                        chip.isChecked = false
                        Toast.makeText(this, context.getString(R.string.charts_two_items_only), Toast.LENGTH_SHORT).show()
                    } else {
                        selectedCharts.add(tag)
                        refreshChart()
                    }
                } else {
                    if (checkedCount == 0) {
                        chip.isChecked = true
                        Toast.makeText(this, context.getString(R.string.charts_at_least_one_item), Toast.LENGTH_SHORT).show()
                    } else {
                        selectedCharts.remove(tag)
                        refreshChart()
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    fun chipUpdate(tag: String, checked: Boolean) {
        val chip = binding.workoutDataChartChipGroup.children
            .filterIsInstance<Chip>()
            .firstOrNull { it.tag == tag }
        chip?.isChecked = checked
    }

    fun refreshChart() {
        val combinedData = CombinedData()
        val lineData = LineData()
        val scatterData = ScatterData()
        val lineDataSetsMarkerFormatters = mutableListOf<ValueFormatter?>()
        val lineDataSetsMarkerUnits = mutableListOf<String?>()
        var leftY = true
        selectedCharts.forEach { selectedChart ->
            val workoutChart = chartData?.find { it.id == selectedChart } ?: return@forEach
            val dataSet = workoutChart.chartData.getDataSetByIndex(0) as? LineScatterCandleRadarDataSet<Entry> ?: return@forEach
            dataSet.highLightColor = ContextCompat.getColor(context, R.color.chart_highline_dolor)
            dataSet.highlightLineWidth = 1f
            dataSet.axisDependency = if(leftY) YAxis.AxisDependency.LEFT else YAxis.AxisDependency.RIGHT
            when (dataSet) {
                is LineDataSet -> {
                    lineData.addDataSet(dataSet)
                }
                is ScatterDataSet -> {
                    scatterData.addDataSet(dataSet)
                }
                else -> {}
            }
            lineDataSetsMarkerFormatters.add(workoutChart.chartYLabelFormatter)
            lineDataSetsMarkerUnits.add(workoutChart.unitString)
            val axis = if (leftY) binding.workoutDataChart.axisLeft else binding.workoutDataChart.axisRight
            axis.valueFormatter = workoutChart.chartYLabelFormatter ?: DefaultAxisValueFormatter(0)
            leftY = false
        }
        if (selectedCharts.size == 1) {
            val selectedChartId = selectedCharts.first()
            val workoutChart = chartData?.find { it.id == selectedChartId } ?: return
            binding.workoutDataChart.axisRight.valueFormatter = workoutChart.chartYLabelFormatter ?: DefaultAxisValueFormatter(0)
        }
        combinedData.setData(lineData)
        combinedData.setData(scatterData)
        binding.workoutDataChart.data = combinedData
        binding.workoutDataChart.marker = ValueMarker(this, combinedData, lineDataSetsMarkerFormatters, lineDataSetsMarkerUnits)
        binding.workoutDataChart.highlightValues(null)
        binding.workoutDataChart.invalidate()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        this.menu = menu
        getMenuInflater().inflate(R.menu.workout_charts_menu, menu)
        setMenuFilterItemVisibility(getResources().configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    fun setMenuFilterItemVisibility(visibility: Boolean) {
        menu?.findItem(R.id.action_filter_charts)?.isVisible = visibility
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_rotate_screen -> {
                val currentOrientation = getResources().configuration.orientation
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    setMenuFilterItemVisibility(true)
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                    setMenuFilterItemVisibility(false)
                }
                true
            }
            R.id.action_filter_charts -> {
                showFiltersDialog()
                true
            }

            android.R.id.home -> {
                // back button
                finish()
                true
            }

            else -> false
        }
    }

    private fun showFiltersDialog() {
        val items = chartData!!.map { it.title }.toTypedArray()
        val ids = chartData!!.map { it.id }
        val checkedItems = BooleanArray(items.size) { index ->
            selectedCharts.contains(ids[index])
        }
        val selectedIndices = mutableSetOf<Int>()
        checkedItems.forEachIndexed { index, isChecked ->
            if (isChecked) selectedIndices.add(index)
        }
        val builder = MaterialAlertDialogBuilder(this)
            .setCancelable(true)
            .setMultiChoiceItems(items, checkedItems, null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            val listView = dialog.listView
            listView.setOnItemClickListener { _, _, which, _ ->
                val isChecked = listView.isItemChecked(which)
                if (isChecked) {
                    if (selectedIndices.size == 2) {
                        listView.setItemChecked(which, false)
                        Toast.makeText(this, context.getString(R.string.charts_two_items_only), Toast.LENGTH_SHORT).show()
                    } else {
                        selectedIndices.add(which)
                        chipUpdate(ids[which], true)
                    }
                } else {
                    if (selectedIndices.size == 1) {
                        listView.setItemChecked(which, true)
                        Toast.makeText(this, context.getString(R.string.charts_at_least_one_item), Toast.LENGTH_SHORT).show()
                    } else {
                        selectedIndices.remove(which)
                        chipUpdate(ids[which], false)
                    }
                }
            }
        }
        dialog.show()
    }

    companion object {
        const val INIT_CHART_ID = "INIT_CHART_ID"
    }
}