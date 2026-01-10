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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_EXERCISE_ROUTE
import androidx.health.connect.client.records.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.HealthConnectResetDialogFragment
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory

data class PermissionsResultOutcome(
    val success: Boolean,
    val healthConnectClient: HealthConnectClient?,
    val message: String?, // For toasts/UI feedback
    val messageType: Int = GB.INFO,
    val requiresUiRefresh: Boolean = false, // If UI needs to update based on permission change
    val startSync: Boolean = false, // If a sync should be initiated
    val promptForFullDaoReset: Boolean = false // True if UI should prompt user for full DAO reset
)

object HealthConnectPermissionManager {

    private val LOG = LoggerFactory.getLogger(HealthConnectPermissionManager::class.java)

    const val PREF_KEY_LAST_GRANTED_HC_PERMISSIONS = "health_connect_last_granted_permissions"
    const val PREF_KEY_HC_PROMPT_FOR_FULL_DAO_RESET = "health_connect_prompt_for_full_dao_reset"

    enum class HealthConnectDataType {
        ACTIVITY,
        SLEEP,
        VO2MAX,
        HRV,
        WEIGHT,
        SPO2,
        TEMPERATURE,
        RESPIRATORY_RATE,
        RESTING_HEART_RATE,
        BLOOD_GLUCOSE,
        WORKOUTS
    }

    @JvmStatic
    fun getRequiredPermissionsForDataType(dataType: HealthConnectDataType): Set<String> {
        return when (dataType) {
            HealthConnectDataType.ACTIVITY -> setOf(
                HealthPermission.getWritePermission(StepsRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
                HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getWritePermission(DistanceRecord::class)
            )
            HealthConnectDataType.SLEEP -> setOf(HealthPermission.getWritePermission(SleepSessionRecord::class))
            HealthConnectDataType.VO2MAX -> setOf(HealthPermission.getWritePermission(Vo2MaxRecord::class))
            HealthConnectDataType.HRV -> setOf(HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class))
            HealthConnectDataType.WEIGHT -> setOf(HealthPermission.getWritePermission(WeightRecord::class))
            HealthConnectDataType.SPO2 -> setOf(HealthPermission.getWritePermission(OxygenSaturationRecord::class))
            HealthConnectDataType.RESPIRATORY_RATE -> setOf(HealthPermission.getWritePermission(RespiratoryRateRecord::class))
            HealthConnectDataType.RESTING_HEART_RATE -> setOf(HealthPermission.getWritePermission(RestingHeartRateRecord::class))
            HealthConnectDataType.BLOOD_GLUCOSE -> setOf(HealthPermission.getWritePermission(BloodGlucoseRecord::class))
            HealthConnectDataType.TEMPERATURE -> setOf(
                HealthPermission.getWritePermission(BodyTemperatureRecord::class),
                HealthPermission.getWritePermission(SkinTemperatureRecord::class)
            )
            HealthConnectDataType.WORKOUTS -> setOf(
                HealthPermission.getWritePermission(ExerciseSessionRecord::class),
                HealthPermission.getWritePermission(DistanceRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
                HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getWritePermission(ElevationGainedRecord::class),
                HealthPermission.getWritePermission(SpeedRecord::class),
                HealthPermission.getWritePermission(PowerRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class),
                HealthPermission.getWritePermission(StepsCadenceRecord::class),
                HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class),
                PERMISSION_WRITE_EXERCISE_ROUTE
            )
        }
    }

    @JvmStatic
    fun getRequiredHealthConnectPermissions(): Set<String> {
        return HealthConnectDataType.entries
            .flatMap { getRequiredPermissionsForDataType(it) }
            .toSet()
    }

    // Helper data class for internal processing
    private data class PermissionChangeAnalysis(
        val finalMessage: String?,
        val finalMessageType: Int,
        val shouldPromptForDaoReset: Boolean,
        val shouldStartSync: Boolean,
        val permissionsActuallyChanged: Boolean
    )

    private fun analyzePermissionChange(
        context: Context,
        newRelevantPermissions: Set<String>,
        oldRelevantPermissions: Set<String>
    ): PermissionChangeAnalysis {
        val permissionsActuallyChanged = newRelevantPermissions != oldRelevantPermissions

        val shouldPromptForDaoreset = newRelevantPermissions.isEmpty() && oldRelevantPermissions.isNotEmpty()
        if (shouldPromptForDaoreset) {
            LOG.info("All relevant Health Connect permissions removed, will prompt for DAO reset.")
        }

        val shouldStartSync = oldRelevantPermissions.isEmpty() && newRelevantPermissions.isNotEmpty()
        if (shouldStartSync) {
            LOG.info("Health Connect permissions newly granted, will start sync.")
        }

        var msg: String? = null
        var msgType = GB.INFO

        if (permissionsActuallyChanged) {
            if (newRelevantPermissions.isEmpty()) {
                msg = context.getString(R.string.health_connect_all_denied)
                msgType = GB.WARN
            } else if (oldRelevantPermissions.isEmpty()) {
                msg = context.getString(R.string.health_connect_all_granted)
            } else {
                msg = context.getString(R.string.health_connect_permission_changed)
            }
        }

        return PermissionChangeAnalysis(
            finalMessage = msg,
            finalMessageType = msgType,
            shouldPromptForDaoReset = shouldPromptForDaoreset,
            shouldStartSync = shouldStartSync,
            permissionsActuallyChanged = permissionsActuallyChanged
        )
    }

    @JvmStatic
    fun isHealthConnectPermissionResetNeeded(context: Context): Boolean {
        val prefs = GBApplication.getPrefs().preferences
        return prefs.getBoolean(PREF_KEY_HC_PROMPT_FOR_FULL_DAO_RESET, false)
    }

    @JvmStatic
    fun setHealthConnectPermissionResetNeeded(context: Context, needed: Boolean) {
        val prefs = GBApplication.getPrefs().preferences
        prefs.edit {
            putBoolean(PREF_KEY_HC_PROMPT_FOR_FULL_DAO_RESET, needed)
        }
        LOG.info("Set PREF_KEY_HC_PROMPT_FOR_FULL_DAO_RESET to: {}", needed)
    }

    @JvmStatic
    fun handlePermissionsResult(context: Context, grantedPermissionsFromFlow: Set<String>): PermissionsResultOutcome {
        val prefs = GBApplication.getPrefs().preferences
        val healthConnectClient = HealthConnectClientProvider.healthConnectInit(context)

        if (healthConnectClient == null) {
            LOG.warn("HealthConnectClient init failed during permission handling.")
            return PermissionsResultOutcome(
                success = false,
                healthConnectClient = null,
                message = context.getString(R.string.health_connect_failed_init),
                messageType = GB.ERROR,
                requiresUiRefresh = true,
                startSync = false,
                promptForFullDaoReset = false
            )
        }

        val oldRelevantPermissions = prefs.getStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, emptySet()) ?: emptySet()
        val newRelevantPermissions = grantedPermissionsFromFlow.intersect(getRequiredHealthConnectPermissions())

        val analysis = analyzePermissionChange(context, newRelevantPermissions, oldRelevantPermissions)

        if (analysis.permissionsActuallyChanged) {
            prefs.edit { putStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, newRelevantPermissions) }
            LOG.debug("Updated stored Health Connect permissions. New count: {}", newRelevantPermissions.size)
        }

        if (analysis.shouldPromptForDaoReset) {
            setHealthConnectPermissionResetNeeded(context, true)
        }

        return PermissionsResultOutcome(
            success = newRelevantPermissions.isNotEmpty(),
            healthConnectClient = healthConnectClient,
            message = analysis.finalMessage,
            messageType = analysis.finalMessageType,
            requiresUiRefresh = analysis.permissionsActuallyChanged,
            startSync = analysis.shouldStartSync,
            promptForFullDaoReset = analysis.shouldPromptForDaoReset
        )
    }

    @JvmStatic
    fun isHealthConnectEnabled(context: Context): Boolean {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            return false
        }
        val prefs = GBApplication.getPrefs()
        if (!prefs.getBoolean(nodomain.freeyourgadget.gadgetbridge.util.GBPrefs.HEALTH_CONNECT_ENABLED, false)) {
            return false
        }
        val lastGranted = prefs.preferences.getStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, null)
        return lastGranted != null && lastGranted.isNotEmpty()
    }

    suspend fun checkPermissionChange(context: Context) {
        if (!isHealthConnectEnabled(context)) return

        val client = HealthConnectClientProvider.healthConnectInit(context) ?: return
        val currentPermissions = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            LOG.error("Failed to get current HC permissions", e)
            return
        }

        val oldPermissions = GBApplication.getPrefs().preferences.getStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, emptySet()) ?: emptySet()
        val newPermissions = currentPermissions.intersect(getRequiredHealthConnectPermissions())

        if (newPermissions != oldPermissions) {
            LOG.info("Health Connect permissions changed outside of app flow. Old: ${oldPermissions.size}, New: ${newPermissions.size}")
            GBApplication.getPrefs().preferences.edit {
                putStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, newPermissions)
            }
            if (newPermissions.isEmpty() && oldPermissions.isNotEmpty()) {
                LOG.info("All Health Connect permissions have been revoked.")
                setHealthConnectPermissionResetNeeded(context, true)
            }
        }
    }


    @JvmStatic
    fun checkAndRectifyPermissions(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            LOG.info("Proactive Health Connect permission check starting.")
            val prefs = GBApplication.getPrefs().preferences // Keep for PREF_KEY_LAST_GRANTED_HC_PERMISSIONS
            val healthConnectClient = HealthConnectClientProvider.healthConnectInit(context)

            if (healthConnectClient == null) {
                LOG.warn("Proactive Check: HealthConnectClient init failed.")
                return@launch
            }

            try {
                val oldRelevantPermissions = prefs.getStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, emptySet()) ?: emptySet()
                val systemGrantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                val newRelevantSystemPermissions = systemGrantedPermissions.intersect(getRequiredHealthConnectPermissions())

                val analysis = analyzePermissionChange(context, newRelevantSystemPermissions, oldRelevantPermissions)

                if (analysis.permissionsActuallyChanged) {
                    prefs.edit { putStringSet(PREF_KEY_LAST_GRANTED_HC_PERMISSIONS, newRelevantSystemPermissions) }
                    LOG.debug("Proactive check: Updated stored Health Connect permissions. New count: {}", newRelevantSystemPermissions.size)
                }

                if (analysis.shouldPromptForDaoReset) {
                    setHealthConnectPermissionResetNeeded(context, true)
                } else {
                    // If prompt not needed, ensure flag is cleared if it was somehow true
                    if (isHealthConnectPermissionResetNeeded(context)) {
                        setHealthConnectPermissionResetNeeded(context, false)
                        LOG.debug("Proactive check: DAO reset prompt not needed, flag cleared.")
                    }
                }

            } catch (e: Exception) {
                LOG.error("Proactive Check: Failed to get or process system granted permissions.", e)
            }
        }
    }

    @JvmStatic
    fun clearAllSyncStates(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            LOG.info("Clearing ALL HealthConnect sync states from DAO.")
            try {
                GBApplication.acquireDB().use { db ->
                    db.daoSession.healthConnectSyncStateDao.deleteAll()
                    db.daoSession.clear()
                    LOG.info("Successfully cleared all HealthConnect sync states and session cache.")
                }
                setHealthConnectPermissionResetNeeded(context, false) // Use new setter
                // LOG.info("Cleared PREF_KEY_HC_PROMPT_FOR_FULL_DAO_RESET after DAO reset.") // Log is now part of setter

            } catch (e: Exception) {
                LOG.error("Failed to clear all HealthConnect sync states.", e)
            }
        }
    }

    @JvmStatic
    fun showResetDialogIfNecessary(activity: FragmentActivity) {
        if (isHealthConnectPermissionResetNeeded(activity)) { // Use new getter, activity is a Context
            if (activity.supportFragmentManager.findFragmentByTag(HealthConnectResetDialogFragment.TAG) == null) {
                val dialogFragment = HealthConnectResetDialogFragment()
                dialogFragment.show(activity.supportFragmentManager, HealthConnectResetDialogFragment.TAG)
            } else {
                LOG.debug("HealthConnectResetDialogFragment is already showing or an attempt to show it again was made.")
            }
        }
    }
}
