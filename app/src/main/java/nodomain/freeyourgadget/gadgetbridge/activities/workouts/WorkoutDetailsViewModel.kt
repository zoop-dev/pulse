package nodomain.freeyourgadget.gadgetbridge.activities.workouts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import org.slf4j.LoggerFactory
import java.util.Date

class WorkoutDetailsViewModel : ViewModel() {
    private val _workouts = MutableLiveData<List<BaseActivitySummary>>()
    val workouts: LiveData<List<BaseActivitySummary>> = _workouts

    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> = _currentPosition

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    suspend fun loadSingleWorkout(workoutId: Long) {
        _isLoading.value = true
        _error.value = null

        try {
            val workout = withContext(Dispatchers.IO) {
                GBApplication.acquireDB().use { dbHandler ->
                    val summaryDao = dbHandler.daoSession.baseActivitySummaryDao
                    summaryDao.load(workoutId)
                }
            }

            if (workout != null) {
                _workouts.value = listOf(workout)
                _currentPosition.value = 0
            } else {
                _error.value = "Workout not found"
            }
        } catch (e: Exception) {
            LOG.error("Error loading single workout", e)
            _error.value = "Failed to load workout: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load filtered workouts for paging
     */
    suspend fun loadFilteredWorkouts(
        gbDevice: GBDevice?,
        activityKindFilter: Int,
        dateFromFilter: Long,
        dateToFilter: Long,
        nameContainsFilter: String?,
        deviceFilter: Long,
        itemsFilter: List<Long>?,
        initialPosition: Int
    ) {
        _isLoading.value = true
        _error.value = null

        try {
            val workouts = withContext(Dispatchers.IO) {
                loadWorkoutsFromDatabase(
                    gbDevice,
                    activityKindFilter,
                    dateFromFilter,
                    dateToFilter,
                    nameContainsFilter,
                    deviceFilter,
                    itemsFilter
                )
            }

            _workouts.value = workouts

            // Set initial position, but ensure it's valid
            val validPosition = if (initialPosition > 0 && initialPosition < workouts.size) {
                initialPosition
            } else {
                0
            }
            _currentPosition.value = validPosition

        } catch (e: Exception) {
            LOG.error("Error loading filtered workouts", e)
            _error.value = "Failed to load workouts: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    private fun loadWorkoutsFromDatabase(
        gbDevice: GBDevice?,
        activityKindFilter: Int,
        dateFromFilter: Long,
        dateToFilter: Long,
        nameContainsFilter: String?,
        deviceFilter: Long,
        itemsFilter: List<Long>?
    ): List<BaseActivitySummary> {
        return GBApplication.acquireDB().use { dbHandler ->
            val summaryDao = dbHandler.daoSession.baseActivitySummaryDao
            val dbDevice = gbDevice?.let { DBHelper.findDevice(it, dbHandler.daoSession) }

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

    fun setCurrentPosition(position: Int) {
        _currentPosition.value = position
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkoutDetailsViewModel::class.java)

        const val ALL_DEVICES = 999L
    }
}
