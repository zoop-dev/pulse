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
package nodomain.freeyourgadget.gadgetbridge.activities.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.lifecycle.LifecycleKt;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectClientProvider;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectPermissionManager;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectSyncWorker;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils;
import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.PermissionsResultOutcome;
import nodomain.freeyourgadget.gadgetbridge.activities.HealthConnectInitialSyncDialog;


public class HealthConnectPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new HealthConnectPreferencesFragment();
    }

    public static class HealthConnectPreferencesFragment extends AbstractPreferenceFragment implements HealthConnectInitialSyncDialog.InitialSyncDialogListener {
        protected static final Logger LOG = LoggerFactory.getLogger(HealthConnectPreferencesFragment.class);

        public static final String HEALTH_CONNECT_SYNC_WORKER_TAG = "HealthConnectSyncWorker";
        private static final String HEALTH_CONNECT_ONETIME_WORK_NAME = "HealthConnectSyncWorker_OneTime";
        private static final String HC_DEVICE_SELECT_DIALOG_TAG = "HC_DEVICE_SELECT_DIALOG";
        private static final String HC_INITIAL_SYNC_DIALOG_TAG = "HC_INITIAL_SYNC_DIALOG_TAG";

        private HealthConnectUtils healthConnectUtils;
        private ActivityResultLauncher<Set<String>> requestPermissionLauncher;

        private SwitchPreferenceCompat healthConnectEnabledPref;
        private Preference healthConnectSyncStatus;
        private Preference healthConnectDisableNotice;
        private SwitchPreferenceCompat syncOnEventPref;
        private MultiSelectListPreference selectedDevicesPref;
        private Preference healthConnectManualSettings;
        private Preference healthConnectSettings;
        private boolean pendingHealthConnectPermissionRequest = false;
        private Runnable permissionCheckRunnable;
        private final android.os.Handler permissionCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        private String lastDisplayedStatus = null;


        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.health_connect_preferences, rootKey);

            healthConnectUtils = new HealthConnectUtils();

            healthConnectEnabledPref = findPreference(GBPrefs.HEALTH_CONNECT_ENABLED);
            healthConnectSyncStatus = findPreference(GBPrefs.HEALTH_CONNECT_SYNC_STATUS);
            healthConnectDisableNotice = findPreference(GBPrefs.HEALTH_CONNECT_DISABLE_NOTICE);
            syncOnEventPref = findPreference(GBPrefs.HEALTH_CONNECT_SYNC_ON_EVENT);
            selectedDevicesPref = findPreference(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION);
            healthConnectManualSettings = findPreference(GBPrefs.HEALTH_CONNECT_MANUAL_SETTINGS);
            healthConnectSettings = findPreference(GBPrefs.HEALTH_CONNECT_SETTINGS);

            requestPermissionLauncher = registerForActivityResult(
                    PermissionController.createRequestPermissionResultContract(),
                    grantedPermissions -> {
                        PermissionsResultOutcome outcome = HealthConnectPermissionManager.handlePermissionsResult(requireContext(), grantedPermissions);

                        if (outcome.getMessage() != null && getContext() != null) {
                            GB.toast(getContext(), outcome.getMessage(), Toast.LENGTH_LONG, outcome.getMessageType());
                        }

                        GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, outcome.getSuccess()).apply();
                        updateHealthConnectUIState(outcome.getSuccess());

                        if (outcome.getSuccess()) {
                            if (outcome.getStartSync()) {
                                HealthConnectInitialSyncDialog dialog = new HealthConnectInitialSyncDialog();
                                dialog.setTargetFragment(HealthConnectPreferencesFragment.this, 0);
                                dialog.show(getParentFragmentManager(), HC_INITIAL_SYNC_DIALOG_TAG);
                            }
                        } else {
                            WorkManager.getInstance(requireContext()).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
                        }
                    });

            setupHealthConnectSwitch();
            setupManualSettingsLink();
            setupHealthConnectSettingsLink();
            setupDeviceMultiSelectList();

            checkInitialPermissionsAndUpdateUI();
        }

        @Override
        public void onViewCreated(@androidx.annotation.NonNull android.view.View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            WorkManager.getInstance(requireContext()).getWorkInfosByTagLiveData(HEALTH_CONNECT_SYNC_WORKER_TAG).observe(getViewLifecycleOwner(), workInfos -> {
                if (workInfos == null || workInfos.isEmpty()) {
                    updateSyncStatusFromPreferences();
                    return;
                }

                WorkInfo runningWork = findRunningWork(workInfos);
                handleWorkInfoUpdate(runningWork);
            });
        }

        private WorkInfo findRunningWork(List<WorkInfo> workInfos) {
            for (WorkInfo info : workInfos) {
                if (info.getState() == WorkInfo.State.RUNNING) {
                    return info;
                }
            }
            return null;
        }

        private void handleWorkInfoUpdate(WorkInfo runningWork) {
            if (runningWork == null) {
                updateSyncStatusFromPreferences();
                return;
            }

            String storedStatus = GBApplication.getPrefs().getPreferences().getString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, "");
            if (!storedStatus.contains("Finished")) {
                updateSyncStatus(runningWork);
                return;
            }

            String runningProgress = runningWork.getProgress().getString("progress");
            if (runningProgress != null && runningProgress.contains("Finished")) {
                updateSyncStatus(runningWork);
            } else {
                updateSyncStatusFromPreferences();
            }
        }

        @Override
        public void onSyncPeriodSelected() {
            checkIfSyncIsRunningAndStartInitialSync();
        }

        private void checkIfSyncIsRunningAndStartInitialSync() {
            WorkManager workManager = WorkManager.getInstance(requireContext());
            ListenableFuture<List<WorkInfo>> future = workManager.getWorkInfosByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
            future.addListener(() -> {
                try {
                    List<WorkInfo> workInfos = future.get();
                    if (isSyncCurrentlyRunning(workInfos)) {
                        showSyncAlreadyRunningMessage();
                        return;
                    }
                    startInitialSyncWork(workManager);
                } catch (Exception e) {
                    LOG.error("Error checking sync status", e);
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        }

        private boolean isSyncCurrentlyRunning(List<WorkInfo> workInfos) {
            if (workInfos == null) return false;

            for (WorkInfo info : workInfos) {
                if (info.getState() == WorkInfo.State.RUNNING) {
                    return true;
                }
            }
            return false;
        }

        private void showSyncAlreadyRunningMessage() {
            LOG.info("Sync already running, not starting initial sync");
            if (!isAdded() || getActivity() == null || getActivity().isFinishing()) return;

            getActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    GB.toast(requireContext(), getString(R.string.health_connect_sync_already_running), Toast.LENGTH_SHORT, GB.INFO);
                }
            });
        }

        private void startInitialSyncWork(WorkManager workManager) {
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(HealthConnectSyncWorker.class)
                    .setInputData(new Data.Builder().putBoolean("initial_sync", true).build())
                    .addTag(HEALTH_CONNECT_SYNC_WORKER_TAG)
                    .build();
            workManager.enqueueUniqueWork(
                    HEALTH_CONNECT_ONETIME_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    syncRequest
            );
            LOG.info("Initial sync work enqueued with KEEP policy");
        }

        @Override
        public void onResume() {
            super.onResume();

            checkPermissionsAndUpdateUI();
            startPeriodicPermissionCheck();
        }

        @Override
        public void onPause() {
            super.onPause();
            stopPeriodicPermissionCheck();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            stopPeriodicPermissionCheck();
        }

        private void startPeriodicPermissionCheck() {
            if (permissionCheckRunnable == null) {
                permissionCheckRunnable = new Runnable() {
                    @Override
                    public void run() {
                        checkPermissionsAndUpdateUI();
                        permissionCheckHandler.postDelayed(this, 2000);
                    }
                };
            }
            permissionCheckHandler.postDelayed(permissionCheckRunnable, 2000);
        }

        private void stopPeriodicPermissionCheck() {
            if (permissionCheckRunnable != null) {
                permissionCheckHandler.removeCallbacks(permissionCheckRunnable);
            }
        }

        private void checkPermissionsAndUpdateUI() {
            if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
                return;
            }

            BuildersKt.launch(LifecycleKt.getCoroutineScope(getLifecycle()), Dispatchers.getIO(), CoroutineStart.DEFAULT, (scope, continuation) -> {
                Object result = HealthConnectPermissionManager.INSTANCE.checkPermissionChange(requireContext(), continuation);
                if (result == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                    return result;
                }

                updateUIAfterPermissionCheck();
                return Unit.INSTANCE;
            });
        }

        private void updateUIAfterPermissionCheck() {
            if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                handlePermissionCheckResult();
            });
        }

        private void handlePermissionCheckResult() {
            if (HealthConnectPermissionManager.isHealthConnectPermissionResetNeeded(requireContext())) {
                handlePermissionsRevoked();
            } else {
                checkInitialPermissionsAndUpdateUI();
            }
        }

        private void handlePermissionsRevoked() {
            boolean wasEnabled = GBApplication.getPrefs().getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false);
            if (!wasEnabled) {
                HealthConnectPermissionManager.showResetDialogIfNecessary(requireActivity());
                return;
            }

            LOG.info("All HC permissions revoked, disabling HC and cancelling all sync work");
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);

            WorkManager.getInstance(requireContext()).cancelUniqueWork(HEALTH_CONNECT_ONETIME_WORK_NAME);
            WorkManager.getInstance(requireContext()).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
            LOG.info("Cancelled all Health Connect sync work due to permission removal");

            clearSyncStatus();
            HealthConnectPermissionManager.showResetDialogIfNecessary(requireActivity());
        }

        private void clearSyncStatus() {
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary("");
                healthConnectSyncStatus.setVisible(false);
            }
            lastDisplayedStatus = null;
            GBApplication.getPrefs().getPreferences().edit()
                .putString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, "")
                .apply();
        }

        private void updateSyncStatus(WorkInfo workInfo) {
            if (healthConnectSyncStatus == null) return;

            String statusSummary;
            boolean isSyncing;
            switch (workInfo.getState()) {
                case ENQUEUED:
                case RUNNING:
                    statusSummary = workInfo.getProgress().getString("progress");
                    if (statusSummary == null) {
                        statusSummary = getString(R.string.health_connect_syncing);
                        isSyncing = true;
                    } else {
                        isSyncing = !statusSummary.contains("Finished");
                    }
                    break;
                case SUCCEEDED:
                case FAILED:
                case BLOCKED:
                case CANCELLED:
                default:
                    updateSyncStatusFromPreferences();
                    return;
            }

            if (lastDisplayedStatus != null && lastDisplayedStatus.contains("Finished") &&
                !statusSummary.contains("Finished")) {
                return;
            }

            if (statusSummary.equals(lastDisplayedStatus)) {
                return;
            }

            lastDisplayedStatus = statusSummary;
            healthConnectSyncStatus.setSummary(statusSummary);
        }

        private void updateSyncStatusFromPreferences() {
            if (healthConnectSyncStatus == null) return;

            String storedStatus = GBApplication.getPrefs().getPreferences().getString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, "");

            if (storedStatus.equals(lastDisplayedStatus)) {
                return;
            }

            lastDisplayedStatus = storedStatus;
            healthConnectSyncStatus.setSummary(storedStatus);
        }

        private void updateHealthConnectUIState(boolean enabled) {
            if (healthConnectEnabledPref != null) {
                healthConnectEnabledPref.setChecked(enabled);
                healthConnectEnabledPref.setEnabled(true);
            }
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setVisible(enabled);
            }
            if (healthConnectDisableNotice != null) healthConnectDisableNotice.setVisible(enabled);

            // Keep these always enabled to prevent lockout when permission requests are denied
            if (selectedDevicesPref != null) {
                selectedDevicesPref.setEnabled(true);
            }
            if (syncOnEventPref != null) {
                syncOnEventPref.setEnabled(true);
            }
        }


        private void showSdkStatusMessage(int sdkStatus) {
            if (healthConnectSyncStatus != null && getContext() != null) {
                int messageResId = sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
                        ? R.string.health_connect_update_required
                        : R.string.health_connect_unsupported;
                healthConnectSyncStatus.setSummary(getContext().getString(messageResId));
                healthConnectSyncStatus.setVisible(true);
            }
        }

        private void checkInitialPermissionsAndUpdateUI() {
            Context context = getContext();
            if (context == null || healthConnectEnabledPref == null) return;

            boolean hcEnabledInPrefs = GBApplication.getPrefs().getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false);
            healthConnectEnabledPref.setChecked(hcEnabledInPrefs);

            if (!hcEnabledInPrefs) {
                handleHealthConnectDisabledState(context);
                return;
            }

            int sdkStatus = HealthConnectClient.getSdkStatus(context);
            if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                handleSdkUnavailable(context, sdkStatus);
                return;
            }

            HealthConnectClient client = HealthConnectClientProvider.healthConnectInit(context);
            if (client == null) {
                handleClientInitializationFailed(context);
                return;
            }

            fetchAndProcessPermissions(client);
        }

        private void handleHealthConnectDisabledState(Context context) {
            updateHealthConnectUIState(false);
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary("");
                healthConnectSyncStatus.setVisible(false);
            }
            WorkManager.getInstance(context).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
        }

        private void handleSdkUnavailable(Context context, int sdkStatus) {
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);
            showSdkStatusMessage(sdkStatus);
            WorkManager.getInstance(context).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
        }

        private void handleClientInitializationFailed(Context context) {
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary(R.string.health_connect_unavailable_summary);
                healthConnectSyncStatus.setVisible(true);
            }
            WorkManager.getInstance(context).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
        }


        private void fetchAndProcessPermissions(HealthConnectClient client) {
            BuildersKt.launch(LifecycleKt.getCoroutineScope(getLifecycle()), Dispatchers.getMain(), CoroutineStart.DEFAULT, (scope, continuation) -> {
                Object resultFromKotlin = HealthConnectPreferencesLogicKt.getGrantedHealthConnectPermissions(client, (Continuation<? super Set<String>>) continuation);

                if (resultFromKotlin == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                    return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                }

                Set<String> grantedPermissions = (Set<String>) resultFromKotlin;

                try {
                    processPermissionsResult(grantedPermissions);
                } catch (Exception e) {
                    handlePermissionsProcessingError(e);
                }
                return Unit.INSTANCE;
            });
        }

        private void processPermissionsResult(Set<String> grantedPermissions) {
            if (grantedPermissions == null) {
                handlePermissionsCheckError();
                return;
            }

            boolean hasPermissions = !grantedPermissions.isEmpty();

            if (GBApplication.getPrefs().getBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false) != hasPermissions) {
                GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, hasPermissions).apply();
            }

            updateHealthConnectUIState(hasPermissions);
            if (!hasPermissions) {
                WorkManager.getInstance(requireContext()).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
            }
        }

        private void handlePermissionsCheckError() {
            LOG.error("Error checking initial Health Connect permissions: Kotlin helper indicated an error.");
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary(R.string.health_connect_permission_check_error);
                healthConnectSyncStatus.setVisible(true);
            }
            WorkManager.getInstance(requireContext()).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
        }

        private void handlePermissionsProcessingError(Exception e) {
            LOG.error("Error processing Health Connect permissions result or updating UI", e);
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);
            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary(R.string.health_connect_permission_check_error);
                healthConnectSyncStatus.setVisible(true);
            }
            WorkManager.getInstance(requireContext()).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
        }

        private void setupHealthConnectSwitch() {
            if (healthConnectEnabledPref == null || getContext() == null) {
                return;
            }

            healthConnectEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabling = (boolean) newValue;
                Context context = getContext();
                if (context == null) return false;

                if (enabling) {
                    handleHealthConnectEnabling(context);
                    return false;
                } else {
                    return handleHealthConnectDisabling(context);
                }
            });
        }

        private void handleHealthConnectEnabling(Context context) {
            int sdkStatus = HealthConnectClient.getSdkStatus(context);
            if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                showSdkStatusMessage(sdkStatus);
                return;
            }

            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary("");
            }

            if (needsDeviceSelection()) {
                showDeviceSelectionDialog();
                return;
            }

            if (healthConnectUtils != null) {
                requestPermissionLauncher.launch(HealthConnectPermissionManager.getRequiredHealthConnectPermissions());
            }
        }

        private boolean needsDeviceSelection() {
            if (selectedDevicesPref == null || selectedDevicesPref.getKey() == null) {
                return false;
            }
            Set<String> currentValues = selectedDevicesPref.getValues();
            return currentValues == null || currentValues.isEmpty();
        }

        private void showDeviceSelectionDialog() {
            pendingHealthConnectPermissionRequest = true;
            if (getParentFragmentManager().findFragmentByTag(HC_DEVICE_SELECT_DIALOG_TAG) == null) {
                DialogFragment dialog = MultiSelectListPreferenceDialogFragmentCompat.newInstance(selectedDevicesPref.getKey());
                dialog.setTargetFragment(HealthConnectPreferencesFragment.this, 0);
                dialog.show(getParentFragmentManager(), HC_DEVICE_SELECT_DIALOG_TAG);
            }
        }

        private boolean handleHealthConnectDisabling(Context context) {
            LOG.info("User disabled Health Connect, cancelling all sync work");
            pendingHealthConnectPermissionRequest = false;
            GBApplication.getPrefs().getPreferences().edit().putBoolean(GBPrefs.HEALTH_CONNECT_ENABLED, false).apply();
            updateHealthConnectUIState(false);

            WorkManager.getInstance(context).cancelUniqueWork(HEALTH_CONNECT_ONETIME_WORK_NAME);
            WorkManager.getInstance(context).cancelAllWorkByTag(HEALTH_CONNECT_SYNC_WORKER_TAG);
            LOG.info("Cancelled all Health Connect sync work (including running syncs)");


            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary("");
            }
            GBApplication.getPrefs().getPreferences().edit()
                .putString(GBPrefs.HEALTH_CONNECT_SYNC_STATUS, "")
                .apply();

            return true;
        }


        private void setupManualSettingsLink() {
            if (healthConnectManualSettings != null && getContext() != null) {
                healthConnectManualSettings.setOnPreferenceClickListener(preference -> {
                    Context context = getContext();
                    if (context == null) return false;
                    try {
                        Intent healthConnectManageDataIntent = HealthConnectClient.getHealthConnectManageDataIntent(context);
                        healthConnectManageDataIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        startActivity(healthConnectManageDataIntent);
                    } catch (Exception e) {
                        LOG.error("Could not open Health Connect settings", e);
                        if (healthConnectSyncStatus != null) {
                            healthConnectSyncStatus.setSummary(R.string.health_connect_settings_open_error);
                            healthConnectSyncStatus.setVisible(true);
                        }
                    }
                    return true;
                });
            }
        }

        private void setupHealthConnectSettingsLink() {
            if (healthConnectSettings != null && getContext() != null) {
                healthConnectSettings.setOnPreferenceClickListener(preference -> {
                    Context context = getContext();
                    if (context == null) return false;
                    try {
                        final Intent intent;
                        if (android.os.Build.VERSION.SDK_INT < 34) {
                            intent = new Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
                        } else {
                            intent = new Intent(HealthConnectClient.getHealthConnectSettingsAction());
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        startActivity(intent);
                    } catch (Exception e) {
                        LOG.error("Could not open Health Connect settings", e);
                        if (healthConnectSyncStatus != null) {
                            healthConnectSyncStatus.setSummary(R.string.health_connect_settings_open_error);
                            healthConnectSyncStatus.setVisible(true);
                        }
                    }
                    return true;
                });
            }
        }

        private void setupDeviceMultiSelectList() {
            if (selectedDevicesPref != null && getContext() != null) {
                List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
                List<String> deviceMACs = new ArrayList<>();
                List<String> deviceNames = new ArrayList<>();
                for (GBDevice dev : devices) {
                    deviceMACs.add(dev.getAddress());
                    deviceNames.add(dev.getAliasOrName());
                }
                selectedDevicesPref.setEntryValues(deviceMACs.toArray(new String[0]));
                selectedDevicesPref.setEntries(deviceNames.toArray(new String[0]));

                cleanupDeletedDevicesFromSelection(new java.util.HashSet<>(deviceMACs));

                selectedDevicesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    Context context = getContext();
                    if (context == null) {
                        return false;
                    }

                    if (pendingHealthConnectPermissionRequest) {
                        handlePendingPermissionRequest(context, newValue);
                    }
                    return true;
                });
            }
        }

        private void handlePendingPermissionRequest(Context context, Object newValue) {
            pendingHealthConnectPermissionRequest = false;

            int sdkStatus = HealthConnectClient.getSdkStatus(context);
            if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                showSdkStatusMessage(sdkStatus);
                return;
            }

            if (healthConnectSyncStatus != null) {
                healthConnectSyncStatus.setSummary("");
            }

            Set<String> newSelectedValues = (newValue instanceof Set) ? (Set<String>) newValue : null;
            if (newSelectedValues == null || newSelectedValues.isEmpty()) {
                LOG.warn("Device selection listener triggered but no devices selected; not launching permissions.");
                return;
            }

            if (healthConnectUtils != null) {
                requestPermissionLauncher.launch(HealthConnectPermissionManager.getRequiredHealthConnectPermissions());
            } else {
                LOG.error("healthConnectUtils is null, cannot launch permissions.");
            }
        }

        private void cleanupDeletedDevicesFromSelection(Set<String> validDeviceAddresses) {
            Set<String> selectedDevices = GBApplication.getPrefs().getPreferences()
                .getStringSet(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION, null);

            if (selectedDevices == null || selectedDevices.isEmpty()) {
                return;
            }

            Set<String> currentSelection = new HashSet<>(selectedDevices);
            Set<String> cleanedSelection = new HashSet<>();
            int removedCount = 0;

            for (String address : currentSelection) {
                if (validDeviceAddresses.contains(address)) {
                    cleanedSelection.add(address);
                } else {
                    LOG.info("Removing deleted device {} from Health Connect selection", address);
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                LOG.info("Cleaned up {} deleted device(s) from Health Connect device selection", removedCount);
                GBApplication.getPrefs().getPreferences().edit()
                    .putStringSet(GBPrefs.HEALTH_CONNECT_DEVICE_SELECTION, cleanedSelection)
                    .apply();

                if (selectedDevicesPref != null) {
                    selectedDevicesPref.setValues(cleanedSelection);
                }
            }
        }
    }
}
