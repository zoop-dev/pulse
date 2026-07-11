/*  Copyright (C) 2025 LLan, Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.GlucoseSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.entities.HealthConnectSleepSession
import nodomain.freeyourgadget.gadgetbridge.entities.HealthConnectSleepSessionDao
import nodomain.freeyourgadget.gadgetbridge.entities.HealthConnectSyncState
import nodomain.freeyourgadget.gadgetbridge.entities.HealthConnectSyncStateDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectPermissionManager.PREF_KEY_LAST_GRANTED_HC_PERMISSIONS
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.BiConsumer
import kotlin.math.pow

data class SyncStatistics(
    val success: Boolean,
    val dataTypesProcessed: Int,
    val dataTypesSkipped: Int,
    val recordsSyncedByType: Map<String, Int>, // data type name -> count of records synced
    val dataTypesWithErrors: Set<String> = emptySet() // data types that had errors
)

class HealthConnectUtils {
    private val LOG = LoggerFactory.getLogger(HealthConnectUtils::class.java)

    private fun addTimestamp(message: String): String {
        return TIME_FORMATTER.format(ZonedDateTime.now()) + ": " + message
    }

    private fun updateSyncStatus(
        message: String,
        inProgress: Boolean,
        summaryCallback: BiConsumer<String, Boolean>?,
        mainHandler: Handler
    ) {
        val summary = addTimestamp(message)
        GBApplication.getPrefs().preferences.edit {
            putString(
                GBPrefs.HEALTH_CONNECT_SYNC_STATUS,
                summary
            )
        }

        summaryCallback?.let {
            mainHandler.post { it.accept(summary, inProgress) }
        }
    }

    private fun buildSyncCompletionMessage(context: Context, stats: SyncStatistics): String {
        if (!stats.success) {
            val errorDetails = if (stats.dataTypesWithErrors.isNotEmpty()) {
                " (${stats.dataTypesWithErrors.joinToString(", ")})"
            } else {
                ""
            }
            return context.getString(R.string.health_connect_finished_with_errors) + errorDetails
        }

        if (stats.dataTypesProcessed == 0) {
            return context.getString(R.string.health_connect_finished_no_data)
        }

        val details = if (stats.recordsSyncedByType.isNotEmpty()) {
            stats.recordsSyncedByType.entries
                .sortedByDescending { it.value }
                .joinToString(", ") { "${it.key}: ${it.value}" }
        } else {
            ""
        }

        return if (stats.dataTypesWithErrors.isNotEmpty()) {
            val errorList = stats.dataTypesWithErrors.joinToString(", ")
            if (details.isNotEmpty()) {
                context.getString(R.string.health_connect_finished_with_stats_and_errors, details, errorList)
            } else {
                context.getString(R.string.health_connect_finished_with_errors)
            }
        } else {
            if (details.isNotEmpty()) {
                context.getString(R.string.health_connect_finished_with_stats, details)
            } else {
                context.getString(R.string.health_connect_finished)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun healthConnectDataSync(
        context: Context,
        healthConnectClient: HealthConnectClient,
        summaryCallback: BiConsumer<String, Boolean>?,
        onFinished: Runnable?,
        worker: androidx.work.CoroutineWorker? = null,  // Optional worker to check for cancellation
        deviceAddress: String? = null  // Optional specific device address to sync
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        updateSyncStatus(context.getString(R.string.health_connect_syncing), true, summaryCallback, mainHandler)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val prefs = GBApplication.getPrefs()
                val grantedPermissions = prefs.preferences.getStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, emptySet()) ?: emptySet()
                val hcDevices = prefs.getStringSet(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION, emptySet())

                // If a specific device address is provided, sync only that device
                // Otherwise, sync all selected devices from preferences
                val selectedDevices = if (deviceAddress != null && deviceAddress.isNotEmpty()) {
                    if (hcDevices.contains(deviceAddress.uppercase())) {
                        setOf(deviceAddress)
                    } else {
                        LOG.error("Attempting to sync {}, which is not configured for HC - refusing", deviceAddress)
                        emptySet()
                    }
                } else {
                    prefs.getStringSet(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION, emptySet())
                }

                if (selectedDevices.isNullOrEmpty()) {
                    updateSyncStatus(
                        context.getString(R.string.health_connect_no_devices_selected),
                        false,
                        summaryCallback,
                        mainHandler
                    )
                } else if (grantedPermissions.isEmpty()) {
                    updateSyncStatus(
                        context.getString(R.string.health_connect_no_permissions),
                        false,
                        summaryCallback,
                        mainHandler
                    )
                } else {
                    try {
                        val stats = healthConnectDataSyncImp(
                            context,
                            selectedDevices,
                            healthConnectClient,
                            summaryCallback,
                            mainHandler,
                            grantedPermissions,
                            worker
                        )

                        val finalMessage = buildSyncCompletionMessage(context, stats)
                        LOG.info("$HC_SYNC_TAG Final sync status: {}", finalMessage)
                        updateSyncStatus(
                            finalMessage,
                            false,
                            summaryCallback,
                            mainHandler
                        )
                    } catch (e: SyncException) {
                        LOG.error("Sync failed: ${e.message}", e)

                        // Check if this is a permission error
                        if (e.cause is SecurityException) {
                            LOG.warn("Sync failed due to missing permissions. Triggering permission check.")
                            // Trigger permission check which will set the reset dialog flag if needed
                            HealthConnectPermissionManager.checkPermissionChange(context)
                        }

                        updateSyncStatus(
                            e.message ?: context.getString(R.string.health_connect_sync_failed),
                            false,
                            summaryCallback,
                            mainHandler
                        )
                    }
                }
            } catch (t: Throwable) {
                LOG.error("Critical error during healthConnectDataSync for Health Connect", t)
                updateSyncStatus(
                    context.getString(R.string.health_connect_exception) + t.localizedMessage,
                    false,
                    summaryCallback,
                    mainHandler
                )
                 // The overall process is considered failed if an exception occurs here
            } finally {
                onFinished?.run()
            }
        }
    }

    private suspend fun healthConnectDataSyncImp(
        context: Context,
        selectedDevices: Set<String>,
        healthConnectClient: HealthConnectClient,
        summaryCallback: BiConsumer<String, Boolean>?,
        mainHandler: Handler,
        grantedPermissions: Set<String>,
        worker: androidx.work.CoroutineWorker? = null  // Optional worker to check for cancellation
    ): SyncStatistics {
        var totalDataTypesProcessed = 0
        var totalDataTypesSkipped = 0
        val recordsSyncedByType = mutableMapOf<String, Int>() // Track records per data type
        val dataTypesWithErrors = mutableSetOf<String>() // Track which data types had errors

        val zoneId = TimeZone.getDefault().toZoneId()
        val manager = GBApplication.app().deviceManager
        val syncIntervalInSeconds: Long = 24 * 60 * 60 // 1 day slices
        val lookBackInSeconds: Long = 24 * 60 * 60
        val sleepLookForwardSeconds: Long = 12 * 60 * 60

        for (targetAddress in selectedDevices) {
            // Check if worker has been cancelled
            if (worker?.isStopped == true) {
                LOG.info("$HC_SYNC_TAG Worker has been cancelled, aborting sync")
                break
            }

            val gbDevice = manager.getDeviceByAddress(targetAddress)
            if (gbDevice == null) {
                LOG.error("$HC_SYNC_TAG Device for address {} not found during sync", targetAddress)
                continue
            }

            LOG.info("$HC_SYNC_TAG Starting sync for device: {}", gbDevice.aliasOrName)

            val deviceCoordinator = gbDevice.deviceCoordinator
            val manufacturer = deviceCoordinator.manufacturer
            var deviceName = context.getString(deviceCoordinator.deviceNameResource)
            if (deviceName.startsWith(manufacturer) && deviceName != manufacturer) {
                deviceName = deviceName.replace(manufacturer, "").trim();
            }
            val device = Device(
                type = when (deviceCoordinator.getDeviceKind(gbDevice)) {
                    DeviceCoordinator.DeviceKind.WATCH -> Device.TYPE_WATCH
                    DeviceCoordinator.DeviceKind.PHONE -> Device.TYPE_PHONE
                    DeviceCoordinator.DeviceKind.SCALE -> Device.TYPE_SCALE
                    DeviceCoordinator.DeviceKind.RING -> Device.TYPE_RING
                    DeviceCoordinator.DeviceKind.HEAD_MOUNTED -> Device.TYPE_HEAD_MOUNTED
                    DeviceCoordinator.DeviceKind.FITNESS_BAND -> Device.TYPE_FITNESS_BAND
                    DeviceCoordinator.DeviceKind.CHEST_STRAP -> Device.TYPE_CHEST_STRAP
                    DeviceCoordinator.DeviceKind.SMART_DISPLAY -> Device.TYPE_SMART_DISPLAY
                    else -> Device.TYPE_UNKNOWN
                },
                manufacturer = manufacturer,
                model = deviceName
            )

            dataTypeLoop@ for (dataType in HealthConnectPermissionManager.HealthConnectDataType.entries) {
                // Check if worker has been canceled
                if (worker?.isStopped == true) {
                    LOG.info("$HC_SYNC_TAG Worker has been cancelled, aborting sync")
                    break@dataTypeLoop
                }

                LOG.debug("$HC_SYNC_TAG Checking data type {} for device {}", dataType.name, gbDevice.aliasOrName)
                val permsNeededForThisDataType = HealthConnectPermissionManager.getRequiredPermissionsForDataType(dataType)

                if (permsNeededForThisDataType.none { it in grantedPermissions }) {
                    LOG.info(
                        "$HC_SYNC_TAG Skipping sync for HealthConnectDataType '{}' on device {}: None of the required Health Connect permissions are granted. Needed: {}, Have: {}",
                        dataType.name,
                        gbDevice.aliasOrName,
                        permsNeededForThisDataType,
                        grantedPermissions.intersect(permsNeededForThisDataType)
                    )
                    totalDataTypesSkipped++
                    continue@dataTypeLoop
                }

                val timestampRange = try {
                    getSyncTimestampRange(context, gbDevice, deviceCoordinator, dataType)
                } catch (e: Exception) {
                    LOG.error("$HC_SYNC_TAG Error during initial DBAccess for Health Connect for device {} and data type {}", gbDevice.aliasOrName, dataType, e)
                    dataTypesWithErrors.add(dataType.name)
                    totalDataTypesSkipped++
                    continue@dataTypeLoop
                }

                if (timestampRange == null) {
                    LOG.info("$HC_SYNC_TAG Data sync range (start/end) not determined for {} on device {}. Skipping.", dataType.name, gbDevice.aliasOrName)
                    totalDataTypesSkipped++
                    continue@dataTypeLoop
                }
                
                var timestampToPersistForThisDataType = timestampRange.first
                val currentDataTypeStartTsFromDb = timestampRange.first
                val currentDataTypeEndTsFromDb = timestampRange.second

                if (currentDataTypeEndTsFromDb.isBefore(currentDataTypeStartTsFromDb) || currentDataTypeEndTsFromDb == currentDataTypeStartTsFromDb) {
                    LOG.info("$HC_SYNC_TAG No new data to sync for device {} and data type {}. Start: {}, End: {}", gbDevice.aliasOrName, dataType.name, currentDataTypeStartTsFromDb, currentDataTypeEndTsFromDb)
                    totalDataTypesSkipped++
                    continue@dataTypeLoop
                }

                LOG.info("$HC_SYNC_TAG Proceeding to sync for {}. Overall sync range for {}({}): {} to {}", dataType.name, gbDevice.aliasOrName, dataType.name, currentDataTypeStartTsFromDb, currentDataTypeEndTsFromDb)
                totalDataTypesProcessed++


                val metadata = when (dataType) {
                    HealthConnectPermissionManager.HealthConnectDataType.ACTIVITY -> Metadata.activelyRecorded(device)
                    else -> Metadata.autoRecorded(device)
                }

                var currentSliceStartTs = currentDataTypeStartTsFromDb

                // Sleep carries a per-night registry (frozen clientRecordId + grown span) across
                // slices, persisted once at the end, to decouple HC record identity from the
                // unstable session start.
                val isSleep = dataType == HealthConnectPermissionManager.HealthConnectDataType.SLEEP
                var sleepRows: List<SleepSessionRow> =
                    if (isSleep) loadSleepRows(gbDevice) else emptyList()

                while (currentSliceStartTs.isBefore(currentDataTypeEndTsFromDb)) {
                    var currentSliceEndTs = currentSliceStartTs.plusSeconds(syncIntervalInSeconds)
                    if (currentSliceEndTs.isAfter(currentDataTypeEndTsFromDb)) {
                        currentSliceEndTs = currentDataTypeEndTsFromDb
                    }
                    if (!currentSliceEndTs.isAfter(currentSliceStartTs)) {
                        LOG.warn("$HC_SYNC_TAG Skipping sync slice for {}({}) as start and end are not distinct: {} to {}", gbDevice.aliasOrName, dataType.name, currentSliceStartTs, currentSliceEndTs)
                        break
                    }

                    // Check if worker has been cancelled before processing each slice
                    if (worker?.isStopped == true) {
                        LOG.info("$HC_SYNC_TAG Worker has been cancelled, aborting sync")
                        break
                    }

                    LOG.info("$HC_SYNC_TAG Processing slice for {}({}): {} to {}", gbDevice.aliasOrName, dataType.name, currentSliceStartTs, currentSliceEndTs)
                    val startTimeFormatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(currentSliceStartTs)
                    val endTimeFormatted = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(currentSliceEndTs)
                    val summary = context.getString(
                        R.string.health_connect_syncing_device_datatype,
                        gbDevice.aliasOrName,
                        dataType.name,
                        startTimeFormatted,
                        endTimeFormatted
                    )

                    updateSyncStatus(summary, true, summaryCallback, mainHandler)

                    val queryStartTs = currentSliceStartTs.minusSeconds(lookBackInSeconds)
                    val queryEndTs = if (dataType == HealthConnectPermissionManager.HealthConnectDataType.SLEEP) {
                        currentSliceEndTs.plusSeconds(sleepLookForwardSeconds)
                    } else {
                        currentSliceEndTs
                    }
                    LOG.info("$HC_SYNC_TAG Querying Gadgetbridge DB for {}({}) from {} to {}", gbDevice.aliasOrName, dataType.name, queryStartTs, queryEndTs)

                    // Fetch activityBasedSamples under their own lock if needed for the current dataType
                    val activityBasedSamples: List<ActivitySample>? = 
                        if (dataType == HealthConnectPermissionManager.HealthConnectDataType.ACTIVITY || 
                            dataType == HealthConnectPermissionManager.HealthConnectDataType.SLEEP) {
                            GBApplication.acquireDbReadOnly().use { db ->
                                getActivitySamples(db, gbDevice, queryStartTs.epochSecond.toInt(), queryEndTs.epochSecond.toInt())
                            }
                        } else {
                            null
                        }

                    try {
                        val sliceStats: List<SyncerStatistics>
                        if (isSleep) {
                            if (activityBasedSamples.isNullOrEmpty()) {
                                sliceStats = emptyList()
                            } else {
                                val result = SleepSyncer.sync(
                                    healthConnectClient, gbDevice, metadata, zoneId,
                                    grantedPermissions, activityBasedSamples, context,
                                    sleepRows
                                )
                                sleepRows = result.rows
                                sliceStats = listOf(result.statistics)
                            }
                        } else {
                            sliceStats = syncDataTypeSlice(
                                dataType = dataType,
                                healthConnectClient = healthConnectClient,
                                gbDevice = gbDevice,
                                metadata = metadata,
                                offset = zoneId,
                                currentSliceStartTs = currentSliceStartTs,
                                currentSliceEndTs = currentSliceEndTs,
                                grantedPermissions = grantedPermissions,
                                activityBasedSamples = activityBasedSamples,
                                context = context
                            )
                        }

                        for (stat in sliceStats) {
                            val key = stat.recordType
                            val currentSynced = recordsSyncedByType.getOrDefault(key, 0)
                            recordsSyncedByType[key] = currentSynced + stat.recordsSynced
                        }

                        val sliceTotalSynced = sliceStats.sumOf { it.recordsSynced }

                        val latestRecordTs = sliceStats.mapNotNull { it.latestRecordTimestamp }.maxOrNull()

                        if (latestRecordTs != null && latestRecordTs.isAfter(timestampToPersistForThisDataType)) {
                            timestampToPersistForThisDataType = latestRecordTs
                        } else if (sliceTotalSynced == 0) {
                            LOG.debug(
                                "$HC_SYNC_TAG Holding sync state for {}({}) at {} (no records synced in slice {} to {})",
                                gbDevice.aliasOrName, dataType.name, timestampToPersistForThisDataType,
                                currentSliceStartTs, currentSliceEndTs
                            )
                        }

                        currentSliceStartTs = currentSliceEndTs
                    } catch (e: SyncException) {
                        LOG.warn(
                            "Failed to process slice for {}({}) from {} to {}. Will not process further slices for this data type in this run. Reason: {}",
                            gbDevice.aliasOrName,
                            dataType.name,
                            currentSliceStartTs,
                            currentSliceEndTs,
                            e.message
                        )
                        dataTypesWithErrors.add(dataType.name)
                        updateSyncStatus(
                            context.getString(
                                R.string.health_connect_sync_error_for_device,
                                gbDevice.aliasOrName,
                                dataType.name,
                                e.message ?: ""
                            ),
                            false,
                            summaryCallback,
                            mainHandler
                        )
                        break
                    }
                } // End while loop for slices

                try {
                    GBApplication.acquireDB().use { db -> // This lock remains
                        val deviceFromDb = DBHelper.getDevice(gbDevice, db.daoSession)
                        val syncStateDao = db.daoSession.healthConnectSyncStateDao
                        val syncState = HealthConnectSyncState(
                            deviceFromDb.id!!,
                            dataType.name,
                            timestampToPersistForThisDataType.epochSecond
                        )

                        LOG.info("$HC_SYNC_TAG Updating Health Connect sync state for device {}, data type {} to timestamp: {}", gbDevice.aliasOrName, dataType.name, timestampToPersistForThisDataType)
                        syncStateDao.insertOrReplace(syncState)
                    }
                } catch (e: Exception) {
                    LOG.error("$HC_SYNC_TAG Error updating Health Connect sync state for device {}, data type {}: {}", gbDevice.aliasOrName, dataType.name, e.localizedMessage, e)
                    dataTypesWithErrors.add(dataType.name)
                }

                if (isSleep) {
                    // Prune rows unreachable by the next run's scan (end < cursor - lookBack), persist.
                    val pruneBefore = timestampToPersistForThisDataType.minusSeconds(lookBackInSeconds)
                    val prunedRows = SleepSyncer.pruneSleepRows(sleepRows, pruneBefore)
                    try {
                        persistSleepRows(gbDevice, prunedRows)
                    } catch (e: Exception) {
                        LOG.error("$HC_SYNC_TAG Error persisting Health Connect sleep sessions for device {}: {}", gbDevice.aliasOrName, e.localizedMessage, e)
                        dataTypesWithErrors.add(dataType.name)
                    }
                }
            } // End dataTypeLoop
        } // End device loop

        // Log summary
        if (totalDataTypesProcessed == 0 && totalDataTypesSkipped > 0) {
            LOG.info("$HC_SYNC_TAG Health Connect sync completed. No data was available to sync. {} data types were skipped (no data in Gadgetbridge database or no permission).", totalDataTypesSkipped)
        } else {
            LOG.info("$HC_SYNC_TAG Health Connect sync completed. Processed: {}, Skipped: {}", totalDataTypesProcessed, totalDataTypesSkipped)
        }

        // Consider sync successful if at least some data types were processed
        val syncSuccess = totalDataTypesProcessed > 0 || (totalDataTypesProcessed == 0 && totalDataTypesSkipped > 0)

        LOG.info("$HC_SYNC_TAG Sync statistics - Success: {}, Processed: {}, Skipped: {}, Errors: {}, Records by type: {}",
            syncSuccess, totalDataTypesProcessed, totalDataTypesSkipped, dataTypesWithErrors, recordsSyncedByType)

        return SyncStatistics(
            success = syncSuccess,
            dataTypesProcessed = totalDataTypesProcessed,
            dataTypesSkipped = totalDataTypesSkipped,
            recordsSyncedByType = recordsSyncedByType,
            dataTypesWithErrors = dataTypesWithErrors
        )
    }

    companion object {
        private val CompanionLogger = LoggerFactory.getLogger(HealthConnectUtils::class.java)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
        internal const val CHUNK_SIZE = 200
        internal const val MAX_SAMPLES_PER_HEART_RATE_RECORD = 1000
        private const val MAX_RETRIES = 5
        private const val INITIAL_DELAY_MS = 1000L
        internal const val HC_SYNC_TAG = "[HC_SYNC]"

        // Floor the sync-start at 2015 (Gadgetbridge predates it). A bogus near-epoch sample
        // timestamp otherwise resolves the start to ~1970 and triggers a full historical resync.
        private const val MIN_VALID_SAMPLE_SECONDS = 1420070400L // 2015-01-01T00:00:00Z
        private const val MIN_VALID_SAMPLE_MILLIS = MIN_VALID_SAMPLE_SECONDS * 1000

        private fun getSyncTimestampRange(
            context: Context,
            gbDevice: GBDevice,
            deviceCoordinator: DeviceCoordinator,
            dataType: HealthConnectPermissionManager.HealthConnectDataType
        ): Pair<Instant, Instant>? {
            return GBApplication.acquireDbReadOnly().use { db ->
                val deviceFromDb = DBHelper.getDevice(gbDevice, db.daoSession)
                if (deviceFromDb == null) {
                    CompanionLogger.error("$HC_SYNC_TAG Device not found in database for address: {}", gbDevice.address)
                    return@use null
                }

                val syncStateDao = db.daoSession.healthConnectSyncStateDao
                if (syncStateDao == null) {
                    CompanionLogger.error("$HC_SYNC_TAG HealthConnectSyncStateDao is null for device {}", gbDevice.aliasOrName)
                    return@use null
                }

                val syncState = syncStateDao.queryBuilder()
                    .where(
                        HealthConnectSyncStateDao.Properties.DeviceId.eq(deviceFromDb.id),
                        HealthConnectSyncStateDao.Properties.DataType.eq(dataType.name)
                    )
                    .unique()

                val startTs = getStartTimestamp(context, gbDevice, deviceCoordinator, db, dataType, syncState)
                if (startTs == null) {
                    CompanionLogger.info("$HC_SYNC_TAG No starting point for sync for {} on device {}. Skipping.", dataType.name, gbDevice.aliasOrName)
                    return@use null
                }

                val endTs = getLastSampleTimestamp(deviceCoordinator, gbDevice, db, dataType)
                if (endTs == null) {
                    CompanionLogger.info("$HC_SYNC_TAG No ending point (latest sample) for {} on device {}. Skipping.", dataType.name, gbDevice.aliasOrName)
                    return@use null
                }

                CompanionLogger.info("$HC_SYNC_TAG Determined sync range for {}({}): {} to {}", gbDevice.aliasOrName, dataType.name, startTs, endTs)
                Pair(startTs, endTs)
            }
        }

        private fun getStartTimestamp(
            context: Context,
            gbDevice: GBDevice,
            deviceCoordinator: DeviceCoordinator,
            db: DBHandler,
            dataType: HealthConnectPermissionManager.HealthConnectDataType,
            syncState: HealthConnectSyncState?
        ): Instant? {
            if (syncState != null) {
                CompanionLogger.info("$HC_SYNC_TAG Found existing sync state for {}({}), last sync was at: {}", gbDevice.aliasOrName, dataType.name, Instant.ofEpochSecond(syncState.lastSyncTimestamp))
                return Instant.ofEpochSecond(syncState.lastSyncTimestamp)
            }

            val initialSyncPrefs = context.getSharedPreferences(GBPrefs.HEALTH_CONNECT_SETTINGS, Context.MODE_PRIVATE)
            val initialSyncStartTs = initialSyncPrefs.getLong(GBPrefs.HEALTH_CONNECT_INITIAL_SYNC_START_TS, -1L)

            if (initialSyncStartTs != -1L) {
                CompanionLogger.info("$HC_SYNC_TAG Using initial sync start timestamp for {}({}): {} ({})", gbDevice.aliasOrName, dataType.name, initialSyncStartTs, Instant.ofEpochSecond(initialSyncStartTs))
                return Instant.ofEpochSecond(initialSyncStartTs)
            }

            val firstTs = getFirstSampleTimestamp(deviceCoordinator, gbDevice, db, dataType)
            if (firstTs != null) {
                CompanionLogger.info("$HC_SYNC_TAG Using first sample timestamp for {}({}): {}", gbDevice.aliasOrName, dataType.name, firstTs)
                return firstTs
            }

            return null
        }

        private suspend fun syncDataTypeSlice(
            dataType: HealthConnectPermissionManager.HealthConnectDataType,
            healthConnectClient: HealthConnectClient,
            gbDevice: GBDevice,
            metadata: Metadata,
            offset: java.time.ZoneId,
            currentSliceStartTs: Instant,
            currentSliceEndTs: Instant,
            grantedPermissions: Set<String>,
            activityBasedSamples: List<ActivitySample>?,
            context: Context
        ): List<SyncerStatistics> {
            val sliceStats = mutableListOf<SyncerStatistics>()

            when (dataType) {
                HealthConnectPermissionManager.HealthConnectDataType.ACTIVITY -> {
                    // Sync activity samples (steps, heart rate) if available
                    if (!activityBasedSamples.isNullOrEmpty()) {
                        sliceStats.add(StepsSyncer.sync(
                            healthConnectClient, gbDevice, metadata, offset,
                            currentSliceStartTs, currentSliceEndTs, grantedPermissions, activityBasedSamples
                        ))
                        sliceStats.add(HeartRateSyncer.sync(
                            healthConnectClient, gbDevice, metadata, offset,
                            currentSliceStartTs, currentSliceEndTs, grantedPermissions, activityBasedSamples
                        ))
                        if (gbDevice.deviceCoordinator.supportsActiveCalories(gbDevice)) {
                            sliceStats.add(ActiveCaloriesSyncer.sync(
                                healthConnectClient, gbDevice, metadata, offset,
                                currentSliceStartTs, currentSliceEndTs, grantedPermissions, activityBasedSamples
                            ))
                        }
                        if (gbDevice.deviceCoordinator.supportsActivityDistance(gbDevice)) {
                            sliceStats.add(DistanceSyncer.sync(
                                healthConnectClient, gbDevice, metadata, offset,
                                currentSliceStartTs, currentSliceEndTs, grantedPermissions, activityBasedSamples
                            ))
                        }
                    }
                }
                HealthConnectPermissionManager.HealthConnectDataType.SLEEP -> {
                    // Handled statefully in the slice loop; identity carried across slices.
                }
                HealthConnectPermissionManager.HealthConnectDataType.VO2MAX -> sliceStats.add(Vo2MaxSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.HRV -> sliceStats.add(HrvSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.WEIGHT -> sliceStats.add(WeightSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.SPO2 -> sliceStats.add(Spo2Syncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.TEMPERATURE -> sliceStats.add(TemperatureSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.RESPIRATORY_RATE -> sliceStats.add(RespiratoryRateSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.RESTING_HEART_RATE -> sliceStats.add(RestingHeartRateSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.BLOOD_GLUCOSE -> sliceStats.add(BloodGlucoseSyncer.sync(
                    healthConnectClient, gbDevice, metadata, offset,
                    currentSliceStartTs, currentSliceEndTs, grantedPermissions
                ))
                HealthConnectPermissionManager.HealthConnectDataType.WORKOUTS -> {
                    // Sync explicitly recorded workouts from BaseActivitySummary
                    val coordinator = gbDevice.deviceCoordinator
                    if (coordinator.supportsRecordedActivities(gbDevice)) {
                        sliceStats.add(RecordedWorkoutSyncer.sync(
                            healthConnectClient, gbDevice, metadata, offset,
                            currentSliceStartTs, currentSliceEndTs, grantedPermissions, context
                        ))
                    }
                }
            }

            return sliceStats
        }


        internal fun getActivitySamples(db: DBHandler, device: GBDevice, tsFrom: Int, tsTo: Int): List<ActivitySample> {
            val provider = device.deviceCoordinator.getSampleProvider(device, db.daoSession)
            if (provider == null) { 
                CompanionLogger.error("getSampleProvider for ACTIVITY/SLEEP returned null for device {}", device.name)
                return emptyList() // Return empty list on error
            }
            return provider.getAllActivitySamples(tsFrom, tsTo)
        }

        private fun loadSleepRows(gbDevice: GBDevice): List<SleepSessionRow> {
            return GBApplication.acquireDbReadOnly().use { db ->
                val deviceFromDb = DBHelper.getDevice(gbDevice, db.daoSession) ?: return@use emptyList()
                db.daoSession.healthConnectSleepSessionDao.queryBuilder()
                    .where(HealthConnectSleepSessionDao.Properties.DeviceId.eq(deviceFromDb.id))
                    .list()
                    .map {
                        SleepSessionRow(
                            clientRecordId = it.clientRecordId,
                            startTime = Instant.ofEpochSecond(it.startTime),
                            endTime = Instant.ofEpochSecond(it.endTime)
                        )
                    }
            }
        }

        private fun persistSleepRows(gbDevice: GBDevice, rows: List<SleepSessionRow>) {
            GBApplication.acquireDB().use { db ->
                val deviceFromDb = DBHelper.getDevice(gbDevice, db.daoSession) ?: return@use
                val dao = db.daoSession.healthConnectSleepSessionDao
                val entities = rows.map {
                    HealthConnectSleepSession(
                        null,
                        deviceFromDb.id,
                        it.clientRecordId,
                        it.startTime.epochSecond,
                        it.endTime.epochSecond
                    )
                }
                // Atomic replace: a failed insert must not leave the delete committed, else ids
                // re-mint next run and duplicate HC records.
                db.daoSession.runInTx {
                    dao.queryBuilder()
                        .where(HealthConnectSleepSessionDao.Properties.DeviceId.eq(deviceFromDb.id))
                        .buildDelete()
                        .executeDeleteWithoutDetachingEntities()
                    dao.insertInTx(entities)
                }
            }
        }

        private fun getFirstSampleTimestamp(
            deviceCoordinator: DeviceCoordinator,
            device: GBDevice,
            db: DBHandler,
            dataType: HealthConnectPermissionManager.HealthConnectDataType
        ): Instant? {
            return when (val provider = getProviderForDataType(deviceCoordinator, device, db, dataType)) {
                is TimeSampleProvider<*> -> {
                    provider.firstSample?.timestamp?.takeIf { it > MIN_VALID_SAMPLE_MILLIS }?.let { Instant.ofEpochMilli(it) }
                }
                is SampleProvider<*> -> { // For ActivitySample based providers
                    provider.getFirstActivitySample(MIN_VALID_SAMPLE_SECONDS.toInt())?.timestamp?.takeIf { it > MIN_VALID_SAMPLE_SECONDS }?.let { Instant.ofEpochSecond(it.toLong()) }
                }
                is BaseActivitySummaryDao -> {
                    val deviceEntity = DBHelper.getDevice(device, db.daoSession) ?: return null
                    db.daoSession.baseActivitySummaryDao?.queryBuilder()
                        ?.where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceEntity.id))
                        ?.orderAsc(BaseActivitySummaryDao.Properties.StartTime)
                        ?.limit(1)
                        ?.list()
                        ?.firstOrNull()
                        ?.startTime?.toInstant()
                }
                else -> {
                    CompanionLogger.error("No suitable provider found or provider is null for getFirstSampleTimestamp, dataType: {}, device: {}", dataType, device.name)
                    null
                }
            }
        }

        private fun getLastSampleTimestamp(
            deviceCoordinator: DeviceCoordinator,
            device: GBDevice,
            db: DBHandler,
            dataType: HealthConnectPermissionManager.HealthConnectDataType
        ): Instant? {
            return when (val provider = getProviderForDataType(deviceCoordinator, device, db, dataType)) {
                is TimeSampleProvider<*> -> {
                    provider.latestSample?.timestamp?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
                }
                is SampleProvider<*> -> { // For ActivitySample based providers
                    provider.latestActivitySample?.timestamp?.takeIf { it > 0 }?.let { Instant.ofEpochSecond(it.toLong()) }
                }
                is BaseActivitySummaryDao -> {
                    val deviceEntity = DBHelper.getDevice(device, db.daoSession) ?: return null
                    return db.daoSession.baseActivitySummaryDao?.queryBuilder()
                        ?.where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceEntity.id))
                        ?.orderDesc(BaseActivitySummaryDao.Properties.EndTime)
                        ?.limit(1)
                        ?.list()
                        ?.firstOrNull()
                        ?.endTime?.toInstant()
                }
                else -> {
                    CompanionLogger.error("No suitable provider found or provider is null for getLastSampleTimestamp, dataType: {}, device: {}", dataType, device.name)
                    null
                }
            }
        }

        private fun getProviderForDataType(
            coordinator: DeviceCoordinator,
            device: GBDevice,
            db: DBHandler,
            dataType: HealthConnectPermissionManager.HealthConnectDataType
        ): Any? { // Return type is Any? as providers have different base types
            return when (dataType) {
                HealthConnectPermissionManager.HealthConnectDataType.ACTIVITY, HealthConnectPermissionManager.HealthConnectDataType.SLEEP -> coordinator.getSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.VO2MAX -> coordinator.getVo2MaxSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.HRV -> coordinator.getHrvValueSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.RESPIRATORY_RATE -> coordinator.getRespiratoryRateSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.RESTING_HEART_RATE -> coordinator.getHeartRateRestingSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.BLOOD_GLUCOSE -> GlucoseSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.WEIGHT -> coordinator.getWeightSampleProvider(device, db.daoSession)
                // For SpO2 and Temperature, there might be a specific provider or fallback to general sample provider
                HealthConnectPermissionManager.HealthConnectDataType.SPO2 -> coordinator.getSpo2SampleProvider(device, db.daoSession) // Potentially add fallback if needed: ?: coordinator.getSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.TEMPERATURE -> coordinator.getTemperatureSampleProvider(device, db.daoSession) // Potentially add fallback: ?: coordinator.getSampleProvider(device, db.daoSession)
                HealthConnectPermissionManager.HealthConnectDataType.WORKOUTS -> db.daoSession.baseActivitySummaryDao
            }
        }

        @Throws(SyncException::class)
        internal suspend fun insertRecords(records: List<Record>, healthConnectClient: HealthConnectClient) {
            if (records.isEmpty()) {
                return
            }

            var currentRecords = records
            var lastException: Exception? = null
            for (i in 0..MAX_RETRIES) {
                try {
                    healthConnectClient.insertRecords(currentRecords)
                    CompanionLogger.debug("Successfully inserted {} records into Health Connect.", currentRecords.size)
                    return // Success
                } catch (e: SecurityException) {
                    // Permission error - don't retry, abort immediately
                    CompanionLogger.error("Permission error while inserting records. Aborting sync.", e)
                    throw SyncException(
                        GBApplication.getContext().getString(
                            R.string.health_connect_permission_denied,
                            e.localizedMessage ?: ""
                        ),
                        e
                    )
                } catch (e: Exception) {
                    lastException = e
                    // The HC platform enforces a 1MB per-record limit (RateLimiter) not exposed by
                    // any API. An oversized record fails deterministically, so plain retries just
                    // burn all attempts. The only variable-size record we build is the GPS route
                    // embedded in ExerciseSessionRecord; shrink it by the limit/was ratio and retry.
                    val shrunk = shrinkOversizedRoute(currentRecords, e)
                    if (shrunk != null) {
                        currentRecords = shrunk
                        continue
                    }
                    if (i < MAX_RETRIES) {
                        val delayMillis = INITIAL_DELAY_MS * 2.0.pow(i).toLong()
                        CompanionLogger.warn(
                            "Caught exception while inserting records. Retrying in ${delayMillis}ms... (Attempt ${i + 1}/$MAX_RETRIES)",
                            e
                        )
                        delay(delayMillis)
                    }
                }
            }
            // If the loop completes, all retries have failed
            throw SyncException(
                GBApplication.getContext().getString(
                    R.string.health_connect_sync_failed_retries,
                    lastException?.localizedMessage ?: ""
                ),
                lastException
            )
        }

        // Matches "...single record size limit: 1000000, was: 1700644" from the HC platform.
        private val RECORD_SIZE_REGEX =
            Regex("single record size limit:\\s*(\\d+),\\s*was:\\s*(\\d+)")

        /**
         * If [e] is a "record size exceeded" error and [records] contains a downsizable
         * ExerciseSessionRecord route, returns a copy of the list with every route decimated by
         * the limit/was ratio (with margin). Returns null when the error is unrelated or nothing
         * can be shrunk, so the caller falls back to normal retry/abort.
         */
        internal fun shrinkOversizedRoute(records: List<Record>, e: Exception): List<Record>? {
            val match = RECORD_SIZE_REGEX.find(e.message ?: "") ?: return null
            val limit = match.groupValues[1].toLongOrNull() ?: return null
            val was = match.groupValues[2].toLongOrNull() ?: return null
            if (limit <= 0 || was <= limit) {
                return null
            }

            // Aim for 90% of the limit to leave room for per-point overhead we don't model.
            val keepRatio = (limit.toDouble() / was.toDouble()) * 0.9
            var shrankAny = false
            val result = records.map { record ->
                if (record !is ExerciseSessionRecord) {
                    return@map record
                }
                val route = (record.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute
                    ?: return@map record
                val points = route.route
                val target = (points.size * keepRatio).toInt()
                if (points.size < 2 || target >= points.size) {
                    return@map record
                }
                shrankAny = true
                val decimated = decimateRoute(points, target.coerceAtLeast(2))
                CompanionLogger.warn(
                    "$HC_SYNC_TAG ExerciseSessionRecord route too large ({} bytes > {} limit); decimated route from {} to {} points and retrying.",
                    was, limit, points.size, decimated.size
                )
                ExerciseSessionRecord(
                    startTime = record.startTime,
                    startZoneOffset = record.startZoneOffset,
                    endTime = record.endTime,
                    endZoneOffset = record.endZoneOffset,
                    exerciseType = record.exerciseType,
                    title = record.title,
                    notes = record.notes,
                    segments = record.segments,
                    laps = record.laps,
                    exerciseRoute = ExerciseRoute(decimated),
                    metadata = record.metadata
                )
            }
            return if (shrankAny) result else null
        }

        /** Uniformly decimates [points] down to [target] points, preserving first and last. */
        private fun decimateRoute(
            points: List<ExerciseRoute.Location>,
            target: Int
        ): List<ExerciseRoute.Location> {
            val kept = ArrayList<ExerciseRoute.Location>(target)
            val lastIndex = points.size - 1
            val step = lastIndex.toDouble() / (target - 1).toDouble()
            var idx = 0.0
            repeat(target - 1) {
                // Clamp below lastIndex so the explicit last point is never duplicated
                // (HC rejects routes with duplicate timestamps).
                kept.add(points[idx.toInt().coerceAtMost(lastIndex - 1)])
                idx += step
            }
            kept.add(points.last())
            return kept
        }
    }
}
