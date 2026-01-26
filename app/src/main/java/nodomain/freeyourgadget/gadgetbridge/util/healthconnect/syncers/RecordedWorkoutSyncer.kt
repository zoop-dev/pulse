/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_EXERCISE_ROUTE
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackViewModel
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

private val LOG = LoggerFactory.getLogger("RecordedWorkoutSyncer")

/**
 * Syncs workouts from BaseActivitySummary for devices that support activity tracks.
 * This provides accurate workout data with GPS, detailed metrics, etc.
 *
 * Use this syncer for devices where coordinator.supportsActivityTracks() returns true.
 */
@SuppressLint("RestrictedApi")
internal object RecordedWorkoutSyncer {
    suspend fun sync(
        healthConnectClient: HealthConnectClient,
        gbDevice: GBDevice,
        metadata: Metadata,
        offset: ZoneOffset,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant,
        grantedPermissions: Set<String>,
        context: Context
    ): SyncerStatistics {

        val deviceName = gbDevice.aliasOrName

        val prefs = Prefs(GBApplication.getPrefs().preferences)
        val useDetailedSync = prefs.getBoolean(GBPrefs.HEALTH_CONNECT_DETAILED_WORKOUT_SYNC, true)

        // Permission Check for ExerciseSessionRecord
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in grantedPermissions) {
            LOG.info("Skipping Recorded Workout sync for device '$deviceName'; ExerciseSessionRecord permission not granted.")
            return SyncerStatistics(recordType = "Workout")
        }

        // Query BaseActivitySummary from database
        val workouts = queryWorkoutsFromDatabase(gbDevice, sliceStartBoundary, sliceEndBoundary)

        if (workouts.isEmpty()) {
            LOG.info("No workouts found in database for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Workout")
        }

        LOG.info("Found ${workouts.size} workout(s) from BaseActivitySummary for device '$deviceName' in slice: $sliceStartBoundary to $sliceEndBoundary.")

        parseWorkoutSummaries(workouts, gbDevice, context, deviceName)

        var workoutsProcessedInThisSlice = 0
        val workoutRecordList = mutableListOf<Record>()

        for (workout in workouts) {
            try {
                val workoutStartInstant = workout.startTime.toInstant()
                val workoutEndInstant = workout.endTime.toInstant()

                if (workoutEndInstant.isBefore(workoutStartInstant)) {
                    LOG.warn("Skipping invalid workout for device '$deviceName' (Type: ${workout.activityKind}): End time $workoutEndInstant is before start time $workoutStartInstant.")
                    continue
                }

                workoutsProcessedInThisSlice++
                LOG.info("Processing workout for device '$deviceName' (Type: ${workout.activityKind}, Start: $workoutStartInstant, End: $workoutEndInstant).")

                val recordsToInsert = mutableListOf<Record>()
                val activityKind = ActivityKind.fromCode(workout.activityKind)
                val exerciseType = WorkoutSyncerUtils.mapActivityKindToExerciseType(activityKind)

                // Skip non-exercise activities (but allow UNKNOWN since it's in BaseActivitySummary - was explicitly recorded)
                if (activityKind == ActivityKind.NOT_MEASURED ||
                        activityKind == ActivityKind.NOT_WORN ||
                        ActivityKind.isSleep(activityKind)) {
                    LOG.debug("Skipping non-exercising or sleep-related ActivityKind {} for device '{}'.", activityKind, deviceName)
                    workoutsProcessedInThisSlice--
                    continue
                }

                var activityPoints: List<ActivityPoint>? = null

                if (useDetailedSync) {
                    activityPoints = loadActivityPoints(workout, deviceName)
                }

                if (activityPoints != null && activityPoints.isNotEmpty()) {
                    LOG.info("Using detailed sync with ${activityPoints.size} activity points for workout (Type: ${activityKind}, Start: $workoutStartInstant).")
                    processDetailedWorkout(
                        workout,
                        activityPoints,
                        workoutStartInstant,
                        workoutEndInstant,
                        offset,
                        metadata,
                        grantedPermissions,
                        recordsToInsert,
                        gbDevice,
                        activityKind,
                        exerciseType,
                        context
                    )
                } else {
                    if (useDetailedSync) {
                        if (activityPoints == null) {
                            LOG.info("No track file available for workout, falling back to aggregate data.")
                        } else {
                            LOG.info("Track file contains no activity points, falling back to aggregate data.")
                        }
                    }
                    processAggregateWorkout(
                        gbDevice,
                        workout,
                        workoutStartInstant,
                        workoutEndInstant,
                        offset,
                        metadata,
                        grantedPermissions,
                        recordsToInsert,
                        deviceName,
                        activityKind,
                        exerciseType,
                        context
                    )
                }

                if (recordsToInsert.isEmpty()) {
                    LOG.warn("No records were created for workout (Type: ${activityKind}, Start: $workoutStartInstant) for device '$deviceName'. This should not happen.")
                    continue
                }

                HealthConnectUtils.insertRecords(recordsToInsert, healthConnectClient)
                LOG.info("Successfully inserted ${recordsToInsert.size} record(s) for workout (Type: ${activityKind}, Start: $workoutStartInstant) for device '$deviceName'.")
                workoutRecordList.addAll(recordsToInsert)
            } catch (e: Exception) {
                LOG.error("Error processing workout for device '$deviceName'", e)
                // Continue with next workout instead of failing entire sync
            }
        }

        if (workoutsProcessedInThisSlice == 0 && workouts.isNotEmpty()) {
            LOG.info("No workouts were processed for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary (e.g., all invalid).")
        } else if (workoutsProcessedInThisSlice > 0) {
            LOG.info("Finished processing $workoutsProcessedInThisSlice workout(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        }

        if (workoutRecordList.isEmpty()) {
            LOG.info("No valid ExerciseSessionRecord(s) created for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
            return SyncerStatistics(recordType = "Workout")
        }

        LOG.info("Successfully inserted ${workoutRecordList.size} ExerciseSessionRecord(s) for device '$deviceName' for slice $sliceStartBoundary to $sliceEndBoundary.")
        return SyncerStatistics(recordsSynced = workoutRecordList.size, recordType = "Workout")
    }


    private fun queryWorkoutsFromDatabase(
        gbDevice: GBDevice,
        sliceStartBoundary: Instant,
        sliceEndBoundary: Instant
    ): List<BaseActivitySummary> {
        val db: DBHandler = GBApplication.acquireDB()
        try {
            val device = DBHelper.getDevice(gbDevice, db.daoSession)
            if (device == null) {
                LOG.warn("Device not found in database for '{}'", gbDevice.aliasOrName)
                return emptyList()
            }

            val startDate = Date.from(sliceStartBoundary)
            val endDate = Date.from(sliceEndBoundary)

            return db.daoSession.baseActivitySummaryDao.queryBuilder()
                .where(
                    BaseActivitySummaryDao.Properties.DeviceId.eq(device.id),
                    BaseActivitySummaryDao.Properties.StartTime.ge(startDate),
                    BaseActivitySummaryDao.Properties.StartTime.lt(endDate)
                )
                .build()
                .list()
        } catch (e: Exception) {
            LOG.error("Error querying workouts from database for device '{}'", gbDevice.aliasOrName, e)
            return emptyList()
        } finally {
            GBApplication.releaseDB()
        }
    }

    private fun parseWorkoutSummaries(
        workouts: List<BaseActivitySummary>,
        gbDevice: GBDevice,
        context: Context,
        deviceName: String
    ) {
        val coordinator = gbDevice.deviceCoordinator
        val activitySummaryParser = coordinator.getActivitySummaryParser(gbDevice, context)

        if (activitySummaryParser == null) {
            LOG.warn("No ActivitySummaryParser available for device '{}'", deviceName)
            return
        }

        for (workout in workouts) {
            try {
                activitySummaryParser.parseWorkout(workout, true)
                LOG.debug("Parsed workout summary for device '{}' at {}", deviceName, workout.startTime)
            } catch (e: Exception) {
                LOG.error("Error parsing workout summary for device '{}' at {}", deviceName, workout.startTime, e)
            }
        }
    }

    private fun loadActivityPoints(workout: BaseActivitySummary, deviceName: String): List<ActivityPoint>? {
        val trackFilePath = workout.rawDetailsPath
        if (trackFilePath.isNullOrBlank()) {
            LOG.debug("No track file path available for workout on device '$deviceName'.")
            return null
        }

        val trackFile = FileUtils.tryFixPath(File(trackFilePath))
        if (trackFile == null || (!trackFile.exists() || !trackFile.canRead())) {
            LOG.warn("Track file does not exist or cannot be read: $trackFilePath")
            return null
        }

        return try {
            val points = MapsTrackViewModel.getActivityPoints(trackFile)
            if (points.isEmpty()) {
                LOG.debug("Track file contains no activity points: $trackFilePath")
                null
            } else {
                points
            }
        } catch (e: Exception) {
            LOG.error("Error loading activity points from track file: $trackFilePath", e)
            null
        }
    }

    private fun processDetailedWorkout(
        workout: BaseActivitySummary,
        activityPoints: List<ActivityPoint>,
        workoutStartInstant: Instant,
        workoutEndInstant: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        device: GBDevice,
        activityKind: ActivityKind,
        exerciseType: Int,
        context: Context
    ) {
        val deviceName = device.aliasOrName

        // Build GPS route if location data is available and permission is granted
        val exerciseRoute = if (PERMISSION_WRITE_EXERCISE_ROUTE in grantedPermissions) {
            val locationPoints = activityPoints
                .filter { it.location != null && it.time != null }
                .mapNotNull { point ->
                    val pointInstant = point.time.toInstant()
                    if (pointInstant.isBefore(workoutStartInstant) || pointInstant.isAfter(workoutEndInstant)) {
                        return@mapNotNull null
                    }
                    val location = point.location ?: return@mapNotNull null
                    ExerciseRoute.Location(
                        time = pointInstant,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) Length.meters(location.altitude) else null,
                        horizontalAccuracy = if (location.hasHdop()) Length.meters(location.hdop) else null,
                        verticalAccuracy = if (location.hasVdop()) Length.meters(location.vdop) else null
                    )
                }

            if (locationPoints.isNotEmpty()) {
                LOG.info("Adding GPS route with ${locationPoints.size} location points for workout on device '$deviceName'.")
                ExerciseRoute(locationPoints)
            } else {
                null
            }
        } else {
            null
        }

        recordsToInsert.add(
            ExerciseSessionRecord(
                startTime = workoutStartInstant,
                startZoneOffset = offset,
                endTime = workoutEndInstant,
                endZoneOffset = offset,
                exerciseType = exerciseType,
                title = workout.name ?: activityKind.getLabel(context),
                exerciseRoute = exerciseRoute,
                metadata = metadata
            )
        )

        addDetailedHeartRateRecords(activityPoints, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
        addDetailedSpeedRecords(activityPoints, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
        addDetailedPowerRecords(activityPoints, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)

        val summaryData = parseSummaryData(workout.summaryData)
        if (summaryData != null) {
            addDistanceRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            // Active calories are synced more granularly for these devices in ActiveCaloriesSyncer
            if (!device.deviceCoordinator.supportsActiveCalories(device)) {
                addCaloriesRecords(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            }
            addElevationGainedRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addStepsRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addCadenceRecords(summaryData, activityKind, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
        }
    }

    private fun processAggregateWorkout(
        gbDevice: GBDevice,
        workout: BaseActivitySummary,
        workoutStartInstant: Instant,
        workoutEndInstant: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String,
        activityKind: ActivityKind,
        exerciseType: Int,
        context: Context
    ) {
        recordsToInsert.add(
            ExerciseSessionRecord(
                startTime = workoutStartInstant,
                startZoneOffset = offset,
                endTime = workoutEndInstant,
                endZoneOffset = offset,
                exerciseType = exerciseType,
                title = workout.name ?: activityKind.getLabel(context),
                metadata = metadata
            )
        )

        val summaryData = parseSummaryData(workout.summaryData)
        if (summaryData != null) {
            addDistanceRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addHeartRateRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addSpeedRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addCaloriesRecords(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addElevationGainedRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addStepsRecord(summaryData, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
            addCadenceRecords(summaryData, activityKind, workoutStartInstant, workoutEndInstant, offset, metadata, grantedPermissions, recordsToInsert, deviceName)
        } else {
            LOG.warn("No summary data available for workout on device '{}' at {}", deviceName, workoutStartInstant)
        }
    }

    private fun addDetailedHeartRateRecords(
        activityPoints: List<ActivityPoint>,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val hrPermission = HealthPermission.getWritePermission(HeartRateRecord::class)
        if (hrPermission !in grantedPermissions) {
            return
        }

        val hrSamples = activityPoints
            .filter { it.heartRate > 0 && it.time != null }
            .map { point ->
                val pointInstant = point.time.toInstant()
                if (pointInstant.isBefore(startTime) || pointInstant.isAfter(endTime)) {
                    return@map null
                }
                HeartRateRecord.Sample(
                    time = pointInstant,
                    beatsPerMinute = point.heartRate.toLong()
                )
            }
            .filterNotNull()

        if (hrSamples.isNotEmpty()) {
            recordsToInsert.add(
                HeartRateRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    samples = hrSamples,
                    metadata = metadata
                )
            )
            LOG.debug("Added detailed HeartRateRecord with ${hrSamples.size} samples for workout on device '$deviceName'.")
        }
    }

    private fun addDetailedSpeedRecords(
        activityPoints: List<ActivityPoint>,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val speedPermission = HealthPermission.getWritePermission(SpeedRecord::class)
        if (speedPermission !in grantedPermissions) {
            return
        }

        val speedSamples = activityPoints
            .filter { it.speed > 0 && it.time != null }
            .map { point ->
                val pointInstant = point.time.toInstant()
                if (pointInstant.isBefore(startTime) || pointInstant.isAfter(endTime)) {
                    return@map null
                }
                SpeedRecord.Sample(
                    time = pointInstant,
                    speed = Velocity.metersPerSecond(point.speed.toDouble())
                )
            }
            .filterNotNull()

        if (speedSamples.isNotEmpty()) {
            recordsToInsert.add(
                SpeedRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    samples = speedSamples,
                    metadata = metadata
                )
            )
            LOG.debug("Added detailed SpeedRecord with ${speedSamples.size} samples for workout on device '$deviceName'.")
        }
    }

    private fun addDetailedPowerRecords(
        activityPoints: List<ActivityPoint>,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val powerPermission = HealthPermission.getWritePermission(PowerRecord::class)
        if (powerPermission !in grantedPermissions) {
            return
        }

        val powerSamples = activityPoints
            .filter { it.power > 0 && it.time != null }
            .map { point ->
                val pointInstant = point.time.toInstant()
                if (pointInstant.isBefore(startTime) || pointInstant.isAfter(endTime)) {
                    return@map null
                }
                PowerRecord.Sample(
                    time = pointInstant,
                    power = Power.watts(point.power.toDouble())
                )
            }
            .filterNotNull()

        if (powerSamples.isNotEmpty()) {
            recordsToInsert.add(
                PowerRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    samples = powerSamples,
                    metadata = metadata
                )
            )
            LOG.debug("Added detailed PowerRecord with ${powerSamples.size} samples for workout on device '$deviceName'.")
        }
    }

    private fun parseSummaryData(summaryDataJson: String?): ActivitySummaryData? {
        if (summaryDataJson.isNullOrBlank()) {
            return null
        }
        return try {
            ActivitySummaryData.fromJson(summaryDataJson)
        } catch (e: Exception) {
            LOG.warn("Failed to parse summaryData JSON", e)
            null
        }
    }

    private fun addDistanceRecord(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val distancePermission = HealthPermission.getWritePermission(DistanceRecord::class)

        if (distancePermission !in grantedPermissions) {
            return
        }

        val distanceMeters = summaryData.getNumber(ActivitySummaryEntries.DISTANCE_METERS, 0.0)
        if (distanceMeters.toDouble() > 0) {
            recordsToInsert.add(
                DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    distance = Length.meters(distanceMeters.toDouble()),
                    metadata = metadata
                )
            )
            LOG.debug("Added DistanceRecord ({} meters) for workout at {} for device '{}'.", distanceMeters, startTime, deviceName)
        }
    }

    private fun addHeartRateRecord(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val hrPermission = HealthPermission.getWritePermission(HeartRateRecord::class)

        if (hrPermission !in grantedPermissions) {
            return
        }

        val hrAvg = summaryData.getNumber(ActivitySummaryEntries.HR_AVG, 0.0)
        if (hrAvg.toDouble() > 0) {
            val midTime = startTime.plusSeconds((endTime.epochSecond - startTime.epochSecond) / 2)
            recordsToInsert.add(
                HeartRateRecord(
                    startTime = midTime,
                    startZoneOffset = offset,
                    endTime = midTime,
                    endZoneOffset = offset,
                    samples = listOf(
                        HeartRateRecord.Sample(
                            time = midTime,
                            beatsPerMinute = hrAvg.toLong()
                        )
                    ),
                    metadata = metadata
                )
            )
            LOG.debug("Added HeartRateRecord (avg: {} bpm) for workout at {} for device '{}'.", hrAvg, startTime, deviceName)
        }
    }

    private fun addSpeedRecord(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val speedPermission = HealthPermission.getWritePermission(SpeedRecord::class)

        if (speedPermission !in grantedPermissions) {
            return
        }

        val speedAvg = summaryData.getNumber(ActivitySummaryEntries.SPEED_AVG, 0.0)
        if (speedAvg.toDouble() > 0) {
            val midTime = startTime.plusSeconds((endTime.epochSecond - startTime.epochSecond) / 2)
            recordsToInsert.add(
                SpeedRecord(
                    startTime = midTime,
                    startZoneOffset = offset,
                    endTime = midTime,
                    endZoneOffset = offset,
                    samples = listOf(
                        SpeedRecord.Sample(
                            time = midTime,
                            speed = Velocity.metersPerSecond(speedAvg.toDouble())
                        )
                    ),
                    metadata = metadata
                )
            )
            LOG.debug("Added SpeedRecord (avg: {} m/s) for workout at {} for device '{}'.", speedAvg, startTime, deviceName)
        }
    }

    private fun addCaloriesRecords(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        // Most fitness trackers report "caloriesBurnt" as active calories (exercise calories)
        // Try dedicated active_calories field first, fall back to caloriesBurnt
        val activeCaloriesPermission = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
        if (activeCaloriesPermission in grantedPermissions) {
            var activeCalories = summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, 0.0).toDouble()

            if (activeCalories > 0) {
                recordsToInsert.add(
                    ActiveCaloriesBurnedRecord(
                        startTime = startTime,
                        startZoneOffset = offset,
                        endTime = endTime,
                        endZoneOffset = offset,
                        energy = Energy.kilocalories(activeCalories),
                        metadata = metadata
                    )
                )
                LOG.debug("Added ActiveCaloriesBurnedRecord ({} kcal) for workout at {} for device '{}'.", activeCalories, startTime, deviceName)
            } else {
                LOG.debug("No active calories data in workout summary for device '{}' at {}.", deviceName, startTime)
            }
        } else {
            LOG.debug("Permission for ActiveCaloriesBurnedRecord not granted for device '{}'.", deviceName)
        }

        // Only sync TotalCaloriesBurnedRecord if we have both active AND resting calories
        // Otherwise, syncing the same value as both active and total would be misleading
        val totalCaloriesPermission = HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
        if (totalCaloriesPermission in grantedPermissions) {
            val activeCalories = summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, 0.0).toDouble()
            val restingCalories = summaryData.getNumber(ActivitySummaryEntries.CALORIES_RESTING, 0.0).toDouble()

            if (activeCalories > 0 && restingCalories > 0) {
                val totalCalories = activeCalories + restingCalories
                recordsToInsert.add(
                    TotalCaloriesBurnedRecord(
                        startTime = startTime,
                        startZoneOffset = offset,
                        endTime = endTime,
                        endZoneOffset = offset,
                        energy = Energy.kilocalories(totalCalories),
                        metadata = metadata
                    )
                )
                LOG.debug("Added TotalCaloriesBurnedRecord ({} kcal = {} active + {} resting) for workout at {} for device '{}'.",
                    totalCalories, activeCalories, restingCalories, startTime, deviceName)
            } else {
                LOG.debug("Not syncing TotalCaloriesBurnedRecord - need both active and resting calories (have: active={}, resting={}) for device '{}'",
                    activeCalories, restingCalories, deviceName)
            }
        }
    }

    private fun addElevationGainedRecord(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val elevationPermission = HealthPermission.getWritePermission(ElevationGainedRecord::class)

        if (elevationPermission !in grantedPermissions) {
            return
        }

        var elevationGain = summaryData.getNumber(ActivitySummaryEntries.ELEVATION_GAIN, 0.0).toDouble()
        if (elevationGain == 0.0) {
            elevationGain = summaryData.getNumber(ActivitySummaryEntries.TOTAL_ASCENT, 0.0).toDouble()
        }
        if (elevationGain == 0.0) {
            elevationGain = summaryData.getNumber(ActivitySummaryEntries.ASCENT_METERS, 0.0).toDouble()
        }

        if (elevationGain > 0) {
            recordsToInsert.add(
                ElevationGainedRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    elevation = Length.meters(elevationGain),
                    metadata = metadata
                )
            )
            LOG.debug("Added ElevationGainedRecord ({} m) for workout at {} for device '{}'.", elevationGain, startTime, deviceName)
        }
    }

    private fun addStepsRecord(
        summaryData: ActivitySummaryData,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val stepsPermission = HealthPermission.getWritePermission(StepsRecord::class)
        if (stepsPermission !in grantedPermissions) {
            return
        }

        val steps = summaryData.getNumber(ActivitySummaryEntries.STEPS, 0.0).toLong()
        if (steps > 0) {
            recordsToInsert.add(
                StepsRecord(
                    startTime = startTime,
                    startZoneOffset = offset,
                    endTime = endTime,
                    endZoneOffset = offset,
                    count = steps,
                    metadata = metadata
                )
            )
            LOG.debug("Added StepsRecord ({} steps) for workout at {} for device '{}'.", steps, startTime, deviceName)
        }
    }

    private fun addCadenceRecords(
        summaryData: ActivitySummaryData,
        activityKind: ActivityKind,
        startTime: Instant,
        endTime: Instant,
        offset: ZoneOffset,
        metadata: Metadata,
        grantedPermissions: Set<String>,
        recordsToInsert: MutableList<Record>,
        deviceName: String
    ) {
        val cadenceAvg = summaryData.getNumber(ActivitySummaryEntries.CADENCE_AVG, 0.0)
        if (cadenceAvg.toDouble() <= 0) {
            return
        }

        // Check the cycle unit to determine if cadence syncing is appropriate
        val cycleUnit = ActivityKind.getCycleUnit(activityKind)

        when (cycleUnit) {
            ActivityKind.CycleUnit.STEPS -> {
                // Sync as StepsCadenceRecord for step-based activities (walking, running, hiking)
                val stepsCadencePermission = HealthPermission.getWritePermission(StepsCadenceRecord::class)
                if (stepsCadencePermission in grantedPermissions) {
                    val midTime = startTime.plusSeconds((endTime.epochSecond - startTime.epochSecond) / 2)
                    recordsToInsert.add(
                        StepsCadenceRecord(
                            startTime = midTime,
                            startZoneOffset = offset,
                            endTime = midTime,
                            endZoneOffset = offset,
                            samples = listOf(
                                StepsCadenceRecord.Sample(
                                    time = midTime,
                                    rate = cadenceAvg.toDouble()
                                )
                            ),
                            metadata = metadata
                        )
                    )
                    LOG.debug("Added StepsCadenceRecord (avg: {} steps/min) for workout at {} for device '{}'.", cadenceAvg, startTime, deviceName)
                }
            }
            ActivityKind.CycleUnit.REVOLUTIONS -> {
                // Sync as CyclingPedalingCadenceRecord for cycling activities
                val cyclingCadencePermission = HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class)
                if (cyclingCadencePermission in grantedPermissions) {
                    val midTime = startTime.plusSeconds((endTime.epochSecond - startTime.epochSecond) / 2)
                    recordsToInsert.add(
                        CyclingPedalingCadenceRecord(
                            startTime = midTime,
                            startZoneOffset = offset,
                            endTime = midTime,
                            endZoneOffset = offset,
                            samples = listOf(
                                CyclingPedalingCadenceRecord.Sample(
                                    time = midTime,
                                    revolutionsPerMinute = cadenceAvg.toDouble()
                                )
                            ),
                            metadata = metadata
                        )
                    )
                    LOG.debug("Added CyclingPedalingCadenceRecord (avg: {} rpm) for workout at {} for device '{}'.", cadenceAvg, startTime, deviceName)
                }
            }
            else -> {
                // Skip cadence syncing for other cycle units (strokes, jumps, reps, swings, none)
                // Health Connect doesn't have dedicated cadence records for these activity types
                LOG.debug("Skipping cadence sync for {} activity (cycle unit: {}) - no appropriate Health Connect record type.", activityKind, cycleUnit)
            }
        }
    }
}

