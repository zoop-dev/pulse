package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractListActivity
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesFilter
import nodomain.freeyourgadget.gadgetbridge.adapter.WorkoutSummariesAdapter
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import nodomain.freeyourgadget.gadgetbridge.util.ActivitySummaryUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory
import java.io.File
import java.util.BitSet
import java.util.Calendar

class WorkoutListActivity : AbstractListActivity<BaseActivitySummary>() {
    private val viewModel: WorkoutListViewModel by viewModels()

    private var activityKindMap = HashMap<String, ActivityKind>(0)
    private var activityFilter = 0
    private var dateFromFilter = 0L
    private var dateToFilter = 0L
    private var deviceFilter = 0L
    private var itemsFilter: List<Long>? = null
    private var nameContainsFilter: String? = null
    private var gbDevice: GBDevice? = null
    private var selectedItems: BitSet? = null
    private lateinit var swipeLayout: SwipeRefreshLayout
    private var actionMode: ActionMode? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != GBDevice.ACTION_DEVICE_CHANGED) {
                LOG.warn("Got unexpected action {}", intent.action)
                return
            }
            val device = intent.getParcelableExtra<GBDevice>(GBDevice.EXTRA_DEVICE)
            if (device == null) {
                LOG.error("Got device changed without device")
                return
            }
            if (device != gbDevice) {
                return
            }
            if (device.isBusy) {
                swipeLayout.isRefreshing = true
            } else {
                val wasBusy = swipeLayout.isRefreshing
                swipeLayout.isRefreshing = false
                if (wasBusy) {
                    refresh()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.activity_action_manage_timestamp -> {
                resetFetchTimestampToChosenDate()
                true
            }
            R.id.activity_action_filter -> {
                runFilterActivity()
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when (requestCode) {
            ACTIVITY_FILTER -> {
                resultData?.extras?.let { bundle ->
                    activityFilter = bundle.getInt("activityFilter", 0)
                    dateFromFilter = bundle.getLong("dateFromFilter", 0)
                    dateToFilter = bundle.getLong("dateToFilter", 0)
                    deviceFilter = bundle.getLong("deviceFilter", 0)
                    nameContainsFilter = bundle.getString("nameContainsFilter")
                    @Suppress("UNCHECKED_CAST")
                    itemsFilter = bundle.getSerializable("itemsFilter") as? List<Long>
                    refresh()
                }
            }
            ACTIVITY_DETAIL -> {
                if (resultCode == WorkoutDetailsActivity.RESULT_WORKOUT_CHANGED) {
                    // FIXME we could try to only update the workouts that changed
                    refresh()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
            ?: throw IllegalArgumentException("Must provide a device when invoking this activity")
        deviceFilter = getDeviceId(gbDevice!!)

        val filterLocal = IntentFilter(GBDevice.ACTION_DEVICE_CHANGED)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filterLocal)

        super.onCreate(savedInstanceState)

        setupViews()
        setupViewModel()
    }

    private fun setupViews() {
        val workoutSummariesAdapter = WorkoutSummariesAdapter(
            this,
            gbDevice!!,
            activityFilter,
            dateFromFilter,
            dateToFilter,
            nameContainsFilter,
            deviceFilter,
            itemsFilter
        )
        selectedItems = workoutSummariesAdapter.selectedItems

        workoutSummariesAdapter.setOnItemClickListener { position ->
            if (!selectedItems!!.isEmpty) {
                selectedItems!!.set(position, !selectedItems!!.get(position))
                workoutSummariesAdapter.notifyItemChanged(position)
                if (!selectedItems!!.isEmpty) {
                    startActionMode()
                } else {
                    stopActionMode()
                }
                return@setOnItemClickListener
            }
            if (position == 0) return@setOnItemClickListener // item 0 is empty for dashboard
            val summary = workoutSummariesAdapter.getItem(position)
            if (summary != null) {
                try {
                    showActivityDetail(position - 1)
                } catch (e: Exception) {
                    GB.toast(
                        applicationContext,
                        "Unable to display Activity Detail: ${e.localizedMessage}",
                        Toast.LENGTH_LONG,
                        GB.ERROR,
                        e
                    )
                }
            }
        }

        workoutSummariesAdapter.setOnItemLongClickListener { position ->
            selectedItems!!.set(position, !selectedItems!!.get(position))
            workoutSummariesAdapter.notifyItemChanged(position)

            if (!selectedItems!!.isEmpty) {
                startActionMode()
            } else {
                stopActionMode()
            }
        }

        setItemAdapter(workoutSummariesAdapter)

        swipeLayout = findViewById(R.id.list_activity_swipe_layout)
        swipeLayout.setOnRefreshListener {
            if (GBApplication.getPrefs().refreshOnSwipe()) {
                fetchTrackData()
            } else {
                swipeLayout.isRefreshing = false
            }
        }
        swipeLayout.setEnabled(GBApplication.getPrefs().refreshOnSwipe())

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { fetchTrackData() }
    }

    private fun setupViewModel() {
        viewModel.summaries.observe(this) { summaries ->
            itemAdapter?.setItems(summaries, true)
            activityKindMap = fillKindMap()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            swipeLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                GB.toast(this, it, Toast.LENGTH_LONG, GB.ERROR)
            }
        }

        viewModel.isDashboardLoading.observe(this) { isLoading ->
            (itemAdapter as WorkoutSummariesAdapter).isDashboardLoading = isLoading
            itemAdapter?.notifyItemChanged(0)
        }

        viewModel.dashboardStats.observe(this) { stats ->
            (itemAdapter as WorkoutSummariesAdapter).dashboardStats = stats
            itemAdapter?.notifyItemChanged(0)
        }

        refresh()
    }

    private fun stopActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    private fun startActionMode() {
        val numSelected = selectedItems!!.cardinality()

        if (actionMode != null) {
            // already in action mode
            actionMode?.title = getString(R.string.number_selected_items, numSelected)
            return
        }

        actionMode = startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.title = getString(R.string.number_selected_items, numSelected)
                menuInflater.inflate(R.menu.activity_list_context_menu, menu)
                findViewById<View>(R.id.fab).visibility = View.INVISIBLE
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

            override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.activity_action_delete -> {
                        val toDelete = ArrayList<BaseActivitySummary>()
                        for (i in 0 until selectedItems!!.length()) {
                            if (selectedItems!!.get(i)) {
                                itemAdapter?.getItem(i)?.let { toDelete.add(it) }
                            }
                        }

                        MaterialAlertDialogBuilder(this@WorkoutListActivity)
                            .setTitle(getString(R.string.sports_activity_confirm_delete_title, toDelete.size))
                            .setMessage(getString(R.string.sports_activity_confirm_delete_description, toDelete.size))
                            .setIcon(R.drawable.ic_delete_forever)
                            .setPositiveButton(android.R.string.yes) { _, _ -> deleteItems(toDelete) }
                            .setNegativeButton(android.R.string.no, null)
                            .show()

                        return true
                    }
                    R.id.activity_action_export -> {
                        val paths = ArrayList<String>()
                        for (i in 0 until selectedItems!!.length()) {
                            if (selectedItems!!.get(i)) {
                                itemAdapter?.getItem(i)?.let { summary ->
                                    ActivitySummaryUtils.getGpxFile(summary)?.let { file ->
                                        paths.add(file.path)
                                    }
                                }
                            }
                        }
                        shareMultiple(paths)
                        return true
                    }
                    R.id.activity_action_select_all -> {
                        for (i in 1 until itemAdapter!!.itemCount - 1) {
                            if (!selectedItems!!.get(i)) {
                                selectedItems!!.set(i, true)
                                itemAdapter!!.notifyItemChanged(i)
                            }
                        }
                        mode.title = getString(R.string.number_selected_items, selectedItems!!.cardinality())
                        return true
                    }
                    R.id.activity_action_addto_filter -> {
                        val toFilter = ArrayList<Long>()
                        for (i in 0 until selectedItems!!.length()) {
                            if (selectedItems!!.get(i)) {
                                itemAdapter?.getItem(i)?.id?.let { id ->
                                    toFilter.add(id)
                                }
                            }
                        }
                        itemsFilter = toFilter
                        refresh()
                        return true
                    }
                }
                mode.finish()
                actionMode = null
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                for (i in 0 until selectedItems!!.length()) {
                    if (selectedItems!!.get(i)) {
                        selectedItems!!.set(i, false)
                        itemAdapter?.notifyItemChanged(i)
                    }
                }
                findViewById<View>(R.id.fab).visibility = View.VISIBLE
            }
        })
    }

    private fun fillKindMap(): HashMap<String, ActivityKind> {
        val newMap = HashMap<String, ActivityKind>(0)
        newMap[getString(R.string.activity_summaries_all_activities)] = ActivityKind.UNKNOWN

        itemAdapter?.items?.forEach { item ->
            val activityName = ActivityKind.fromCode(item.activityKind).getLabel(this)
            if (!newMap.containsKey(activityName) && item.activityKind != 0) {
                newMap[activityName] = ActivityKind.fromCode(item.activityKind)
            }
        }
        return newMap
    }

    private fun resetFetchTimestampToChosenDate() {
        val currentDate = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }
                val timestamp = date.timeInMillis - 1000
                GBApplication.getDeviceSpecificSharedPrefs(gbDevice!!.address).edit().apply {
                    remove("lastSportsActivityTimeMillis")
                    putLong("lastSportsActivityTimeMillis", timestamp)
                    apply()
                }
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DATE)
        ).show()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun deleteItems(items: List<BaseActivitySummary>) {
        items.forEach { item ->
            try {
                item.delete()
            } catch (e: Exception) {
                // pass delete error
            }
        }
        refresh()
    }

    private fun fetchTrackData() {
        gbDevice?.let { device ->
            if (device.isInitialized && !device.isBusy) {
                swipeLayout.isRefreshing = true
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_GPS_TRACKS)
            } else {
                swipeLayout.isRefreshing = false
                if (!device.isInitialized) {
                    GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR)
                }
            }
        }
    }

    private fun shareMultiple(paths: List<String>) {
        val uris = paths.map { path ->
            val file = File(path)
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.screenshot_provider",
                file
            )
        }

        if (uris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/gpx+xml"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            startActivity(Intent.createChooser(intent, "SHARE"))
        } else {
            GB.toast(this, "No selected activity contains a GPX track to share", Toast.LENGTH_SHORT, GB.ERROR)
        }
    }

    private fun showActivityDetail(position: Int) {
        val intent = WorkoutDetailsActivity.createFilteredListIntent(
            this,
            gbDevice!!,
            position,
            activityFilter,
            dateFromFilter,
            dateToFilter,
            nameContainsFilter,
            deviceFilter,
            itemsFilter
        )
        startActivityForResult(intent, ACTIVITY_DETAIL)
    }

    private fun runFilterActivity() {
        val filterIntent = Intent(this, ActivitySummariesFilter::class.java).apply {
            putExtra("activityKindMap", activityKindMap)
            putExtra("itemsFilter", ArrayList(itemsFilter ?: emptyList()))
            putExtra("activityFilter", activityFilter)
            putExtra("dateFromFilter", dateFromFilter)
            putExtra("dateToFilter", dateToFilter)
            putExtra("deviceFilter", deviceFilter)
            putExtra("initial_deviceFilter", getDeviceId(gbDevice!!))
            putExtra("nameContainsFilter", nameContainsFilter)
        }
        startActivityForResult(filterIntent, ACTIVITY_FILTER)
    }

    private fun getDeviceId(device: GBDevice): Long {
        return try {
            GBApplication.acquireDB().use { handler ->
                DBHelper.findDevice(device, handler.daoSession)?.id ?: 0L
            }
        } catch (e: Exception) {
            LOG.error("Failed to get device id", e)
            0L
        }
    }

    override fun refresh() {
        gbDevice?.let { device ->
            viewModel.loadSummaries(
                device,
                activityFilter,
                dateFromFilter,
                dateToFilter,
                nameContainsFilter,
                deviceFilter,
                itemsFilter
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutListActivity::class.java)
        const val ACTIVITY_FILTER = 1
        const val ACTIVITY_DETAIL = 11
    }
}
