/*  Copyright (C) 2020-2025 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.MenuProvider
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesChartFragment
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DurationXLabelFormatter
import nodomain.freeyourgadget.gadgetbridge.activities.fit.FitViewerActivity
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackViewModel.Companion.getActivityPoints
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.ChartDataRepository
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.DefaultWorkoutCharts
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.charts.WorkoutChartsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryEntry
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryGroup
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummarySimpleEntry
import nodomain.freeyourgadget.gadgetbridge.databinding.FragmentWorkoutDetailsBinding
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.entities.Device
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.model.workout.Workout
import nodomain.freeyourgadget.gadgetbridge.model.workout.WorkoutChart
import nodomain.freeyourgadget.gadgetbridge.util.ActivitySummaryUtils
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class WorkoutDetailsFragment : Fragment(), MenuProvider {
    private var workoutId: Long = -1
    private var currentWorkout: Workout? = null
    private var gbDevice: GBDevice? = null

    private lateinit var binding: FragmentWorkoutDetailsBinding

    private var chartFragment: ActivitySummariesChartFragment? = null
    private var gpsFragment: WorkoutGpsFragment? = null

    private lateinit var workoutEditor: WorkoutEditor

    private val workoutValueFormatter = WorkoutValueFormatter()

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutEditor = WorkoutEditor(requireContext())
        arguments?.let {
            workoutId = it.getLong(ARG_WORKOUT_ID, -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWorkoutDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartFragment = ActivitySummariesChartFragment()
        gpsFragment = WorkoutGpsFragment()

        childFragmentManager.beginTransaction()
            .replace(R.id.chartsFragmentHolder, chartFragment!!)
            .replace(R.id.gpsFragmentHolder, gpsFragment!!)
            .commit()

        // Toggle raw data when long pressing the workout image
        binding.itemImage.setOnLongClickListener {
            workoutValueFormatter.toggleRawData()
            currentWorkout?.let { workout ->
                updateWorkoutHeader(workout.summary)
                updateWorkoutDetails(workout)
            }
            false
        }

        loadWorkoutData()
    }

    override fun onResume() {
        super.onResume()

        currentWorkout?.summary?.activityKind?.let {
            val activityKindName = ActivityKind.fromCode(it).getLabel(requireContext())
            // Action bar title
            (activity as? AppCompatActivity)?.supportActionBar?.title = activityKindName
        }
    }

    private fun loadWorkoutData() {
        if (workoutId == -1L) return

        showLoading(true)

        lifecycleScope.launch {
            try {
                currentWorkout = withContext(Dispatchers.IO) {
                    val summary = GBApplication.acquireDB().use { dbHandler ->
                        dbHandler.daoSession.baseActivitySummaryDao.load(workoutId)
                    }
                    gbDevice = getGBDevice(summary.device)
                    val parsedWorkout = try {
                        gbDevice!!.deviceCoordinator.getActivitySummaryParser(gbDevice, requireContext())
                            .parseWorkout(summary, true)
                    } catch (e: Exception) {
                        // Do not break completely - use any previously processed data
                        GB.toast(requireContext(), "Error while loading workout", Toast.LENGTH_SHORT, GB.ERROR, e)
                        Workout(
                            summary,
                            ActivitySummaryData.fromJson(summary.summaryData),
                            mutableListOf()
                        )
                    }
                    if (parsedWorkout.charts.isEmpty()) {
                        try {
                            val trackFile = ActivitySummaryUtils.getTrackFile(parsedWorkout.summary)
                            if (trackFile != null) {
                                val activityPoints = getActivityPoints(trackFile)
                                    .stream()
                                    .collect(Collectors.toList())
                                val defaultCharts = DefaultWorkoutCharts.buildDefaultCharts(
                                    requireContext(),
                                    activityPoints,
                                    ActivityKind.fromCode(parsedWorkout.summary.activityKind)
                                )
                                return@withContext Workout(parsedWorkout.summary, parsedWorkout.data, defaultCharts)
                            }
                        } catch (e: Exception) {
                            LOG.error("Failed to build default charts", e)
                        }
                    }
                    return@withContext parsedWorkout
                }

                requireActivity().addMenuProvider(
                    this@WorkoutDetailsFragment,
                    viewLifecycleOwner,
                    Lifecycle.State.RESUMED
                )

                currentWorkout?.let { workout ->
                    updateWorkoutHeader(workout.summary)
                    updateWorkoutDetails(workout)
                    updateFragments(workout)
                    showLoading(false)
                } ?: run {
                    showError("Workout not found")
                }
            } catch (e: Exception) {
                LOG.error("Error loading workout data", e)
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                showError("Failed to load workout: $sw")
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.errorMessage.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingSpinner.visibility = View.GONE
        binding.scrollView.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun updateWorkoutHeader(summary: BaseActivitySummary) {
        val activityName = summary.name
        val startTime = summary.startTime
        val endTime = summary.endTime
        val durationHms = DateTimeUtils.formatDurationHoursMinutes(
            endTime.time - startTime.time, TimeUnit.MILLISECONDS
        )

        view?.let { view ->
            binding.itemImage.setImageResource(
                ActivityKind.fromCode(summary.activityKind).icon
            )

            // Activity name
            binding.activityname.apply {
                text = activityName
                visibility = if (StringUtils.isBlank(activityName)) View.GONE else View.VISIBLE
            }

            // Date
            binding.activitydate.apply {
                val timeString = if (DateTimeUtils.isSameDay(startTime, endTime)) {
                    context.getString(
                        R.string.date_placeholders__start_time__end_time,
                        DateTimeUtils.formatDateTimeRelative(context, startTime),
                        DateTimeUtils.formatTime(endTime.hours, endTime.minutes)
                    )
                } else {
                    context.getString(
                        R.string.date_placeholders__start_time__end_time,
                        DateTimeUtils.formatDateTimeRelative(context, startTime),
                        DateTimeUtils.formatDateTimeRelative(context, endTime)
                    )
                }
                text = timeString
            }

            // Duration
            binding.activityduration.text = durationHms
        }
    }

    private fun updateWorkoutDetails(workout: Workout) {
        binding.summaryDetails.removeAllViews()

        val groups = ActivitySummaryGroup.buildGroupedList(workout.data)

        for ((groupKey, entries) in groups) {
            val groupCharts = workout.charts.filter { chart -> groupKey == chart.group }
            if (entries.isEmpty() && groupCharts.isEmpty()) {
                continue
            }

            if (ActivitySummaryEntries.GROUP_ACTIVITY != groupKey) {
                addGroupHeader(groupKey)
            }

            if (!entries.isEmpty()) {
                addGroupContent(entries)
            }

            if (!groupCharts.isEmpty() && entries.isEmpty()) {
                // Add the initial separator
                binding.summaryDetails.addView(createSeparator())
            }

            groupCharts.forEach { chart ->
                addChart(binding.summaryDetails, false, chart, workout.charts)
            }
        }
    }

    private fun addGroupHeader(groupKey: String) {
        val labelRow = TableRow(context)
        val labelField = TextView(context).apply {
            id = View.generateViewId()
            textSize = 18f
            gravity = Gravity.CENTER
            setPaddingRelative(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            typeface = Typeface.DEFAULT_BOLD
            text = workoutValueFormatter.getStringResourceByName(groupKey)
        }
        labelRow.addView(labelField)
        binding.summaryDetails.addView(labelRow)
    }

    private fun addGroupContent(entries: List<Pair<String, ActivitySummaryEntry>>) {
        val gridLayout = GridLayout(context).apply {
            setBackgroundColor(resources.getColor(R.color.gauge_line_color))
            columnCount = 2
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val totalCells = entries.sumOf { it.right.columnSpan }.let { it + (it and 1) }
        var cellNumber = 0

        for ((key, entry) in entries) {
            val columnSpan = entry.columnSpan
            if (columnSpan == 2 && cellNumber % 2 != 0) {
                cellNumber++
            }

            val linearLayout = generateLinearLayout(cellNumber, cellNumber + 2 >= totalCells, columnSpan)
            entry.populate(key, linearLayout, workoutValueFormatter)
            gridLayout.addView(linearLayout)
            cellNumber += columnSpan
        }

        if (gridLayout.isNotEmpty()) {
            if (cellNumber % 2 != 0) {
                val emptyLayout = generateLinearLayout(cellNumber, true, 1)
                ActivitySummarySimpleEntry(null, "", "string").populate("", emptyLayout, workoutValueFormatter)
                gridLayout.addView(emptyLayout)
            }
            binding.summaryDetails.addView(gridLayout)
        }
    }

    private fun generateLinearLayout(i: Int, lastRow: Boolean, columnSize: Int): LinearLayout {
        return LinearLayout(context).apply {
            val layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSize, GridLayout.FILL, 1f)
            )
            layoutParams.width = 0
            this.layoutParams = layoutParams
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            setBackgroundColor(GBApplication.getWindowBackgroundColor(context))

            val marginLeft = if (i % 2 == 0) 0 else 1
            val marginRight = if (i % 2 == 0) 1 else 0
            val marginTop = 2
            val marginBottom = if (lastRow) 2 else 0

            layoutParams.setMargins(dpToPx(marginLeft), dpToPx(marginTop), dpToPx(marginRight), dpToPx(marginBottom))
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun updateFragments(workout: Workout) {
        val trackFile = ActivitySummaryUtils.getTrackFile(workout.summary)

        // If there's a device-specific HR chart, prefer it over the default one
        if (workout.charts.any { chart -> chart.group == ActivitySummaryEntries.GROUP_HEART_RATE }) {
            binding.heartRateChartWrapper.visibility = View.GONE
        } else {
            chartFragment?.setDateAndGetData(
                trackFile,
                gbDevice,
                workout.summary.startTime.time / 1000,
                workout.summary.endTime.time / 1000
            )
        }

        binding.dynamicCharts.removeAllViews()
        for (chart in workout.charts) {
            if (chart.group == null) {
                addChart(binding.dynamicCharts, true, chart, workout.charts)
            }
        }

        if (workoutHasGps(workout.summary)) {
            showGpsCanvas()
            gpsFragment?.setTrackData(trackFile)
        } else {
            hideGpsCanvas()
        }
    }

    private fun addChart(
        chartsLayout: LinearLayout,
        includeHeader: Boolean,
        chart: WorkoutChart,
        allChartsData: List<WorkoutChart>
    ) {
        if (includeHeader) {
            val chartTitle = TextView(context).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = chart.title
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)

                val paddingPx = (16 * resources.displayMetrics.density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            }

            chartsLayout.addView(createSeparator())
            chartsLayout.addView(chartTitle)
            chartsLayout.addView(createSeparator())
        }

        val chartsFragmentHolder = FrameLayout(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (300 * resources.displayMetrics.density).toInt()
            )
        }

        val chartTextColor = GBApplication.getSecondaryTextColor(context);
        val lineChart: BarLineChartBase<*> = when (chart.chartData) {
            is ScatterData -> ScatterChart(requireContext())
            else -> LineChart(requireContext())
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            legend.textColor = GBApplication.getTextColor(context)
            isScaleXEnabled = false
            isScaleYEnabled = false
            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = false
            isDragEnabled = false
        }
        lineChart.xAxis.apply {
            setDrawLabels(true)
            setDrawGridLines(false)
            setDrawLimitLinesBehindData(true)
            isEnabled = true
            textColor = chartTextColor
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = DurationXLabelFormatter("mm:ss")
        }
        lineChart.axisLeft.apply {
            setDrawGridLines(false)
            setDrawTopYLabelEntry(true)
            textColor = chartTextColor
            isEnabled = true
            if (chart.chartYLabelFormatter != null) {
                valueFormatter = chart.chartYLabelFormatter;
            }
        }
        lineChart.axisRight.apply {
            isEnabled = false
        }
        chart.lineChart(lineChart);
        when {
            lineChart is LineChart && chart.chartData is LineData -> {
                lineChart.data = chart.chartData
            }
            lineChart is ScatterChart && chart.chartData is ScatterData -> {
                lineChart.data = chart.chartData
            }
        }
        lineChart.description.isEnabled = false;
        lineChart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
            override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
            override fun onChartSingleTapped(me: MotionEvent?) {
                ChartDataRepository.chartData = allChartsData
                val intent = Intent(requireContext(), WorkoutChartsActivity::class.java).apply {
                    putExtra(WorkoutChartsActivity.INIT_CHART_ID, chart.id)
                }
                startActivity(intent)
            }
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
        }
        lineChart.invalidate()
        chartsFragmentHolder.addView(lineChart)

        chartsLayout.addView(chartsFragmentHolder)
        chartsLayout.addView(createSeparator())
    }

    private fun createSeparator(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (2 * resources.displayMetrics.density).toInt()
            )

            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.row_separator, typedValue, true)
            setBackgroundColor(ContextCompat.getColor(context, typedValue.resourceId))
        }
    }

    private fun workoutHasGps(summary: BaseActivitySummary): Boolean {
        summary.gpxTrack?.let { gpxTrack ->
            val existing = FileUtils.tryFixPath(File(gpxTrack))
            if (existing != null && existing.canRead()) {
                return true
            }
        }

        val summaryDataJson = summary.summaryData
        if (summaryDataJson != null && summaryDataJson.contains(ActivitySummaryEntries.INTERNAL_HAS_GPS)) {
            val summaryData = ActivitySummaryData.fromJson(summaryDataJson)
            return summaryData.getBoolean(ActivitySummaryEntries.INTERNAL_HAS_GPS, false)
        }

        return false
    }

    private fun showGpsCanvas() {
        val params = binding.gpsFragmentHolder.layoutParams
        params.height = (300 * requireContext().resources.displayMetrics.density).toInt()
        binding.gpsFragmentHolder.layoutParams = params
    }

    private fun hideGpsCanvas() {
        val params = binding.gpsFragmentHolder.layoutParams
        params.height = 0
        binding.gpsFragmentHolder.layoutParams = params
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.activity_take_screenshot_menu, menu)
        this.menu = menu
        updateMenuItems(menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val workout = currentWorkout ?: return false

        return when (menuItem.itemId) {
            R.id.activity_action_take_screenshot -> {
                takeSharedScreenshot()
                true
            }

            R.id.activity_action_show_gpx -> {
                viewGpxTrack()
                true
            }

            R.id.activity_action_share_gpx -> {
                shareGpxTrack()
                true
            }

            R.id.activity_action_dev_inspect_file -> {
                val intent = Intent(requireContext(), FitViewerActivity::class.java).apply {
                    putExtra(FitViewerActivity.EXTRA_PATH, workout.summary.rawDetailsPath)
                }
                startActivity(intent)
                true
            }

            R.id.activity_action_dev_share_raw_summary -> {
                shareRawSummary(workout)
                true
            }

            R.id.activity_action_dev_share_raw_details -> {
                shareRawDetails(workout)
                true
            }

            R.id.activity_action_dev_share_json_details -> {
                shareJsonDetails(workout)
                true
            }

            R.id.activity_summary_detail_action_edit_name -> {
                currentWorkout?.let {
                    workoutEditor.editWorkoutName(it, object : WorkoutEditor.Callback {
                        override fun onWorkoutUpdated() {
                            notifyWorkoutChanged()
                            updateWorkoutHeader(workout.summary)
                        }
                    })
                }
                true
            }

            R.id.activity_summary_detail_action_edit_gps -> {
                currentWorkout?.let {
                    workoutEditor.editGpsTrack(it, object : WorkoutEditor.Callback {
                        override fun onWorkoutUpdated() {
                            notifyWorkoutChanged()
                            // Reload the entire workout data so that we can refresh the charts
                            loadWorkoutData()
                        }
                    })
                }
                true
            }

            android.R.id.home -> {
                requireActivity().finish()
                true
            }

            else -> false
        }
    }

    private fun notifyWorkoutChanged() {
        val resultIntent = Intent().apply {
            putExtra(ARG_WORKOUT_ID, workoutId)
        }
        requireActivity().setResult(WorkoutDetailsActivity.RESULT_WORKOUT_CHANGED, resultIntent)
    }

    private fun updateMenuItems(menu: Menu) {
        val workout = currentWorkout ?: return

        val hasGpx = workoutHasGps(workout.summary)
        val hasRawSummary = workout.summary.rawSummaryData != null
        val hasRawDetails = workout.summary.rawDetailsPath?.let { File(it).exists() } ?: false

        val overflowMenu = menu.findItem(R.id.activity_detail_overflowMenu)?.subMenu
        if (overflowMenu != null) {
            overflowMenu.findItem(R.id.activity_action_show_gpx)?.isVisible = hasGpx
            overflowMenu.findItem(R.id.activity_action_share_gpx)?.isVisible = hasGpx
            overflowMenu.findItem(R.id.activity_action_dev_inspect_file)?.isVisible =
                hasRawDetails && workout.summary.rawDetailsPath?.lowercase(Locale.ROOT)?.endsWith(".fit") == true
            overflowMenu.findItem(R.id.activity_action_dev_share_raw_summary)?.isVisible = hasRawSummary
            overflowMenu.findItem(R.id.activity_action_dev_share_raw_details)?.isVisible = hasRawDetails

            val devToolsMenu = overflowMenu.findItem(R.id.activity_action_dev_tools)
            val devToolsSubMenu = devToolsMenu?.subMenu
            devToolsMenu?.isVisible = devToolsSubMenu != null && devToolsSubMenu.hasVisibleItems()
        }
    }

    private fun takeSharedScreenshot() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workout = currentWorkout ?: return@launch
                val width = binding.scrollView.getChildAt(0).width
                val height = binding.scrollView.getChildAt(0).height
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)

                withContext(Dispatchers.Main) {
                    canvas.drawColor(GBApplication.getWindowBackgroundColor(requireContext()))
                    binding.scrollView.draw(canvas)
                }

                val fileName = FileUtils.makeValidFileName(
                    "Screenshot-${
                        ActivityKind.fromCode(workout.summary.activityKind).getLabel(requireContext()).lowercase()
                    }-${DateTimeUtils.formatIso8601(workout.summary.startTime)}.png"
                )
                val targetFile = File(FileUtils.getExternalFilesDir(), fileName)
                val fOut = FileOutputStream(targetFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
                fOut.flush()
                fOut.close()

                withContext(Dispatchers.Main) {
                    shareScreenshot(targetFile)
                    GB.toast(requireContext(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO)
                }
            } catch (e: IOException) {
                LOG.error("Error taking screenshot", e)
            }
        }
    }

    private fun shareScreenshot(targetFile: File) {
        val subject = currentWorkout?.summary?.name?.let { "Sports Activity" }
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.screenshot_provider",
            targetFile
        )
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, subject)
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        } catch (e: Exception) {
            LOG.error("Failed to share screenshot", e)
            Toast.makeText(requireContext(), R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show()
        }
    }

    private fun viewGpxTrack() {
        val workout = currentWorkout ?: return
        val gpxFile = ActivitySummaryUtils.getGpxFile(workout.summary)

        if (gpxFile == null) {
            GB.toast(requireContext(), "No GPX track in this activity", Toast.LENGTH_LONG, GB.INFO)
            return
        }

        try {
            AndroidUtils.viewFile(gpxFile.path, "application/gpx+xml", requireContext())
        } catch (e: Exception) {
            GB.toast(
                requireContext(),
                "Unable to display GPX track: ${e.localizedMessage}",
                Toast.LENGTH_LONG,
                GB.ERROR,
                e
            )
        }
    }

    private fun shareGpxTrack() {
        val workout = currentWorkout ?: return
        val gpxFile = ActivitySummaryUtils.getGpxFile(workout.summary)

        if (gpxFile == null) {
            GB.toast(requireContext(), "No GPX track in this activity", Toast.LENGTH_LONG, GB.INFO)
            return
        }

        try {
            AndroidUtils.shareFile(requireContext(), gpxFile)
        } catch (e: Exception) {
            GB.toast(
                requireContext(),
                "Unable to share GPX track: ${e.localizedMessage}",
                Toast.LENGTH_LONG,
                GB.ERROR,
                e
            )
        }
    }

    private fun shareRawSummary(workout: Workout) {
        if (workout.summary.rawSummaryData == null) {
            GB.toast(requireContext(), "No raw summary in this activity", Toast.LENGTH_LONG, GB.WARN)
            return
        }

        val filename =
            FileUtils.makeValidFileName("${DateTimeUtils.formatIso8601(workout.summary.startTime)}_summary.bin")

        try {
            AndroidUtils.shareBytesAsFile(requireContext(), filename, workout.summary.rawSummaryData)
        } catch (e: Exception) {
            GB.toast(
                requireContext(),
                "Unable to share raw summary: ${e.localizedMessage}",
                Toast.LENGTH_LONG,
                GB.ERROR,
                e
            )
        }
    }

    private fun shareRawDetails(workout: Workout) {
        if (workout.summary.rawDetailsPath == null) {
            GB.toast(requireContext(), "No raw details in this activity", Toast.LENGTH_LONG, GB.WARN)
            return
        }

        try {
            AndroidUtils.shareFile(requireContext(), File(workout.summary.rawDetailsPath))
        } catch (e: Exception) {
            GB.toast(
                requireContext(),
                "Unable to share raw details: ${e.localizedMessage}",
                Toast.LENGTH_LONG,
                GB.ERROR,
                e
            )
        }
    }

    private fun shareJsonDetails(workout: Workout) {
        val filename = FileUtils.makeValidFileName("${DateTimeUtils.formatIso8601(workout.summary.startTime)}.json")

        try {
            AndroidUtils.shareBytesAsFile(
                requireContext(),
                filename,
                workout.data.toString().toByteArray(StandardCharsets.UTF_8)
            )
        } catch (e: Exception) {
            GB.toast(
                requireContext(),
                "Unable to share json details: ${e.localizedMessage}",
                Toast.LENGTH_LONG,
                GB.ERROR,
                e
            )
        }
    }

    private fun getGBDevice(device: Device?): GBDevice? {
        return device?.let { findDevice ->
            GBApplication.app().deviceManager.devices
                .firstOrNull { it.address.equals(findDevice.identifier, ignoreCase = true) }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutDetailsFragment::class.java)

        private const val ARG_WORKOUT_ID = "workout_id"

        fun newInstance(workoutId: Long): WorkoutDetailsFragment {
            return WorkoutDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_WORKOUT_ID, workoutId)
                }
            }
        }
    }
}
