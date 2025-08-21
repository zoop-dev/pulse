package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser
import org.slf4j.LoggerFactory
import java.util.Date


class WorkoutListViewModel : ViewModel() {
    private val _summaries = MutableLiveData<List<BaseActivitySummary>>()
    val summaries: LiveData<List<BaseActivitySummary>> = _summaries

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _dashboardStats = MutableLiveData<DashboardStats?>()
    val dashboardStats: LiveData<DashboardStats?> = _dashboardStats

    private val _isDashboardLoading = MutableLiveData<Boolean>()
    val isDashboardLoading: LiveData<Boolean> = _isDashboardLoading

    fun loadSummaries(
        gbDevice: GBDevice,
        activityKindFilter: Int,
        dateFromFilter: Long,
        dateToFilter: Long,
        nameContainsFilter: String?,
        deviceFilter: Long,
        itemsFilter: List<Long>?
    ) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val summaries = withContext(Dispatchers.IO) {
                    loadSummariesFromDatabase(
                        gbDevice,
                        activityKindFilter,
                        dateFromFilter,
                        dateToFilter,
                        nameContainsFilter,
                        deviceFilter,
                        itemsFilter
                    )
                }

                val allSummaries: MutableList<BaseActivitySummary> = mutableListOf()

                val dashboardSummary = BaseActivitySummary()
                allSummaries.add(dashboardSummary) // dashboard
                allSummaries.addAll(summaries)
                allSummaries.add(BaseActivitySummary()) // empty

                _summaries.value = allSummaries

                loadDashboardStats(gbDevice, summaries)
            } catch (e: Exception) {
                LOG.error("Error loading summaries", e)
                _error.value = "Error loading summaries: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadDashboardStats(gbDevice: GBDevice, summaries: List<BaseActivitySummary>) {
        _isDashboardLoading.value = true

        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.Default) {
                    val parser = gbDevice.deviceCoordinator.getActivitySummaryParser(gbDevice, GBApplication.app())
                    DashboardStats.from(parser, summaries)
                }
                _dashboardStats.value = stats
            } catch (e: Exception) {
                LOG.error("Error loading dashboard stats", e)
                _error.value = "Error loading dashboard stats: ${e.localizedMessage}"
            } finally {
                _isDashboardLoading.value = false
            }
        }
    }

    private fun loadSummariesFromDatabase(
        gbDevice: GBDevice,
        activityKindFilter: Int,
        dateFromFilter: Long,
        dateToFilter: Long,
        nameContainsFilter: String?,
        deviceFilter: Long,
        itemsFilter: List<Long>?
    ): List<BaseActivitySummary> {
        return GBApplication.acquireDB().use { dbHandler ->
            val summaryDao = dbHandler.daoSession.baseActivitySummaryDao
            val dbDevice = DBHelper.findDevice(gbDevice, dbHandler.daoSession)

            val queryBuilder = summaryDao.queryBuilder()

            // Apply device filter
            when {
                deviceFilter != 0L && deviceFilter != ALL_DEVICES -> {
                    queryBuilder.where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceFilter))
                }
                dbDevice != null -> {
                    queryBuilder.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.id))
                }
            }
            queryBuilder.orderDesc(BaseActivitySummaryDao.Properties.StartTime)

            if (activityKindFilter != 0) {
                queryBuilder.where(BaseActivitySummaryDao.Properties.ActivityKind.eq(activityKindFilter))
            }

            if (dateFromFilter != 0L) {
                queryBuilder.where(BaseActivitySummaryDao.Properties.StartTime.gt(Date(dateFromFilter)))
            }
            if (dateToFilter != 0L) {
                queryBuilder.where(BaseActivitySummaryDao.Properties.EndTime.lt(Date(dateToFilter)))
            }

            if (!nameContainsFilter.isNullOrEmpty()) {
                queryBuilder.where(BaseActivitySummaryDao.Properties.Name.like("%${nameContainsFilter}%"))
            }

            if (!itemsFilter.isNullOrEmpty()) {
                queryBuilder.where(BaseActivitySummaryDao.Properties.Id.`in`(itemsFilter))
            }

            queryBuilder.list()
        }
    }

    data class DashboardStats(
        val durationSum: Long,
        val caloriesBurntSum: Double,
        val distanceSum: Double,
        val activeSecondsSum: Long,
        val firstItemDate: Long,
        val lastItemDate: Long,
        val activityIcon: Int
    ) {
        companion object {
            fun from(summaryParser: ActivitySummaryParser, activities: List<BaseActivitySummary>): DashboardStats {
                var durationSum = 0L
                var caloriesBurntSum = 0.0
                var distanceSum = 0.0
                var activeSecondsSum = 0L
                var firstItemDate = 0L
                var lastItemDate = 0L
                var activityIcon = 0
                var activitySame = true

                for (summary in activities) {
                    if (summary.startTime == null) continue  // first item is empty, for dashboard

                    if (firstItemDate == 0L) firstItemDate = summary.startTime.time
                    lastItemDate = summary.endTime.time
                    durationSum += (summary.endTime.time - summary.startTime.time)

                    if (activityIcon == 0) {
                        activityIcon = summary.activityKind
                    } else if (activityIcon != summary.activityKind) {
                        activitySame = false
                    }

                    val activitySummaryJsonSummary = ActivitySummaryJsonSummary(summaryParser, summary)
                    val summarySubData = activitySummaryJsonSummary.getSummaryData(false)

                    if (summarySubData != null) {
                        if (summarySubData.has("caloriesBurnt")) {
                            caloriesBurntSum += summarySubData.getNumber("caloriesBurnt", 0).toDouble()
                        }
                        if (summarySubData.has("distanceMeters")) {
                            distanceSum += summarySubData.getNumber("distanceMeters", 0).toDouble()
                        }
                        if (summarySubData.has("activeSeconds")) {
                            activeSecondsSum += summarySubData.getNumber("activeSeconds", 0).toLong()
                        }
                    }
                }

                return DashboardStats(
                    durationSum = durationSum,
                    caloriesBurntSum = caloriesBurntSum,
                    distanceSum = distanceSum,
                    activeSecondsSum = activeSecondsSum,
                    firstItemDate = firstItemDate,
                    lastItemDate = lastItemDate,
                    activityIcon = if (activitySame) activityIcon else 0
                )
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutListViewModel::class.java)
        const val ALL_DEVICES = 999L
    }
}
