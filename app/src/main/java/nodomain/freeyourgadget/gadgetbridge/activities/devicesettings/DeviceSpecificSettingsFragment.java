/*  Copyright (C) 2019-2025 akasaka / Genjitsu Labs, Alicia Hormann, Andreas
    Böhler, Andreas Shimokawa, Arjan Schrijver, Cre3per, Damien Gaignon, Daniel
    Dakhno, Daniele Gobbetti, Davis Mosenkovs, foxstidious, José Rebelo, mamucho,
    NekoBox, opavlov, Petr Vaněk, Yoran Vulker, Yukai Li, Zhong Jianxin, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_CONTROL_CENTER_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_EXPOSE_HR_THIRDPARTY;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_SHORTCUTS_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_NIGHT_MODE_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_SWIPE_UNLOCK;
import static nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants.PREF_MOYOUNG_DEVICE_VERSION;
import static nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants.PREF_MOYOUNG_WATCH_FACE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.CalBlacklistActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureContacts;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureWorldClocks;
import nodomain.freeyourgadget.gadgetbridge.activities.NotificationsAppIconUploadActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications.AppSpecificNotificationSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.audiorecordings.AudioRecordingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst;
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.musicmanager.MusicManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.widgets.WidgetScreensListActivity;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.GBSimpleSummaryProvider;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.PreferenceCategoryMultiline;

public class DeviceSpecificSettingsFragment extends AbstractPreferenceFragment implements DeviceSpecificSettingsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceSpecificSettingsFragment.class);

    private DeviceSpecificSettings deviceSpecificSettings;
    private DeviceSpecificSettingsCustomizer deviceSpecificSettingsCustomizer;

    private GBDevice device;

    private void setSettingsFileSuffix(String settingsFileSuffix) {
        Bundle args = new Bundle();
        args.putString("settingsFileSuffix", settingsFileSuffix);
        setArguments(args);
    }

    private void setDeviceSpecificSettings(final DeviceSpecificSettings deviceSpecificSettings) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("deviceSpecificSettings", deviceSpecificSettings);
        setArguments(args);
    }

    private void setDeviceSpecificSettingsCustomizer(final DeviceSpecificSettingsCustomizer customizer) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("deviceSpecificSettingsCustomizer", customizer);
        setArguments(args);
    }

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }

    private final BroadcastReceiver mDeviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                final GBDevice changedDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (changedDevice != null && changedDevice.equals(device)) {
                    device = changedDevice; // update the state
                    if (deviceSpecificSettingsCustomizer != null) {
                        LOG.debug("{} changed, notifying customizer", changedDevice);
                        deviceSpecificSettingsCustomizer.onDeviceChanged(DeviceSpecificSettingsFragment.this);
                    }
                    reloadEnabledPreferences();
                }
            }
        }
    };

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        final IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mDeviceUpdateReceiver, commandFilter);

        return view;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mDeviceUpdateReceiver);
        super.onDestroyView();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        String settingsFileSuffix = arguments.getString("settingsFileSuffix", null);
        this.deviceSpecificSettings = arguments.getParcelable("deviceSpecificSettings");
        this.deviceSpecificSettingsCustomizer = arguments.getParcelable("deviceSpecificSettingsCustomizer");
        this.device = arguments.getParcelable("device");

        if (settingsFileSuffix == null || deviceSpecificSettings == null) {
            return;
        }

        getPreferenceManager().setSharedPreferencesName("devicesettings_" + settingsFileSuffix);

        LOG.debug("onCreatePreferences: {}", rootKey);

        if (rootKey == null) {
            // we are the main preference screen
            boolean first = true;
            for (int setting : deviceSpecificSettings.getRootScreens()) {
                if (first) {
                    setPreferencesFromResource(setting, null);
                    first = false;
                } else {
                    addPreferencesFromResource(setting);
                }
            }
        } else {
            // First attempt to find a known screen for this key
            final List<Integer> screenSettings = deviceSpecificSettings.getScreen(rootKey);
            if (screenSettings != null) {
                boolean first = true;
                for (int setting : screenSettings) {
                    if (first) {
                        // Use the root key here to set the root screen, so that the actionbar title gets updated
                        setPreferencesFromResource(setting, rootKey);
                        first = false;
                        addDynamicSettings(rootKey);
                    } else {
                        addPreferencesFromResource(setting);
                    }
                }
            } else {
                // Now, this is ugly: search all the xml files for the rootKey
                // This means that this device is using the deprecated getSupportedDeviceSpecificSettings,
                // or that we're on a sub-screen
                final List<Integer> allScreens = deviceSpecificSettings.getAllScreens();
                for (int setting : allScreens) {
                    try {
                        setPreferencesFromResource(setting, rootKey);
                    } catch (Exception ignore) {
                        continue;
                    }
                    break;
                }
            }
        }

        // Since all root preference screens are empty, clicking them will not do anything
        // add on-click listeners
        for (final DeviceSpecificSettingsScreen value : DeviceSpecificSettingsScreen.values()) {
            final PreferenceScreen prefScreen = findPreference(value.getKey());
            if (prefScreen != null) {
                prefScreen.setOnPreferenceClickListener(p -> {
                    onNavigateToScreen(prefScreen);
                    return true;
                });
            }
        }

        setChangeListener(rootKey);

        reloadEnabledPreferences();
    }

    private void reloadEnabledPreferences() {
        if (deviceSpecificSettings != null) {
            for (String connectedPreference : deviceSpecificSettings.getConnectedPreferences()) {
                final Preference pref = findPreference(connectedPreference);
                if (pref != null) {
                    pref.setEnabled(device.isInitialized());
                }
            }
        }
    }

    private void addDynamicSettings(final String rootKey) {
        if (rootKey.equals(DeviceSpecificSettingsScreen.BATTERY.getKey())) {
            addBatterySettings();
        }
    }

    private void addBatterySettings() {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final PreferenceScreen batteryScreen = getPreferenceScreen();
        if (batteryScreen == null) {
            return;
        }
        final BatteryConfig[] batteryConfigs = coordinator.getBatteryConfig(device);
        for (final BatteryConfig batteryConfig : batteryConfigs) {
            if (batteryConfigs.length > 1) {
                final Preference prefHeader = new PreferenceCategory(requireContext());
                prefHeader.setKey("pref_battery_header_" + batteryConfig.getBatteryIndex());
                prefHeader.setIconSpaceReserved(false);
                if (batteryConfig.getBatteryLabel() != GBDevice.BATTERY_LABEL_DEFAULT) {
                    prefHeader.setTitle(batteryConfig.getBatteryLabel());
                } else {
                    prefHeader.setTitle(requireContext().getString(R.string.battery_i, batteryConfig.getBatteryIndex()));
                }
                batteryScreen.addPreference(prefHeader);
            }

            final SwitchPreferenceCompat showInNotification = new SwitchPreferenceCompat(requireContext());
            showInNotification.setLayoutResource(R.layout.preference_checkbox);
            showInNotification.setKey(PREF_BATTERY_SHOW_IN_NOTIFICATION + batteryConfig.getBatteryIndex());
            showInNotification.setTitle(R.string.show_in_notification);
            showInNotification.setIconSpaceReserved(false);
            showInNotification.setDefaultValue(true);
            batteryScreen.addPreference(showInNotification);

            final SwitchPreferenceCompat notifyLowEnabled = new SwitchPreferenceCompat(requireContext());
            notifyLowEnabled.setLayoutResource(R.layout.preference_checkbox);
            notifyLowEnabled.setKey(PREF_BATTERY_NOTIFY_LOW_ENABLED + batteryConfig.getBatteryIndex());
            notifyLowEnabled.setTitle(R.string.battery_low_notify_enabled);
            notifyLowEnabled.setDefaultValue(true);
            notifyLowEnabled.setIconSpaceReserved(false);
            batteryScreen.addPreference(notifyLowEnabled);

            final EditTextPreference notifyLowThreshold = new EditTextPreference(requireContext());
            notifyLowThreshold.setKey(PREF_BATTERY_NOTIFY_LOW_THRESHOLD + batteryConfig.getBatteryIndex());
            notifyLowThreshold.setTitle(R.string.battery_low_threshold);
            notifyLowThreshold.setDialogTitle(R.string.battery_low_threshold);
            notifyLowThreshold.setIconSpaceReserved(false);
            notifyLowThreshold.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 100, true));
                editText.setSelection(editText.getText().length());
            });
            notifyLowThreshold.setSummaryProvider(new GBSimpleSummaryProvider(
                    requireContext().getString(R.string.default_percentage, batteryConfig.getDefaultLowThreshold()),
                    R.string.battery_percentage_str
            ));

            batteryScreen.addPreference(notifyLowThreshold);

            final SwitchPreferenceCompat notifyFullEnabled = new SwitchPreferenceCompat(requireContext());
            notifyFullEnabled.setLayoutResource(R.layout.preference_checkbox);
            notifyFullEnabled.setKey(PREF_BATTERY_NOTIFY_FULL_ENABLED + batteryConfig.getBatteryIndex());
            notifyFullEnabled.setTitle(R.string.battery_full_notify_enabled);
            notifyFullEnabled.setDefaultValue(true);
            notifyFullEnabled.setIconSpaceReserved(false);
            batteryScreen.addPreference(notifyFullEnabled);

            final EditTextPreference notifyFullThreshold = new EditTextPreference(requireContext());
            notifyFullThreshold.setKey(PREF_BATTERY_NOTIFY_FULL_THRESHOLD + batteryConfig.getBatteryIndex());
            notifyFullThreshold.setTitle(R.string.battery_full_threshold);
            notifyFullThreshold.setDialogTitle(R.string.battery_full_threshold);
            notifyFullThreshold.setIconSpaceReserved(false);
            notifyFullThreshold.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 100, true));
                editText.setSelection(editText.getText().length());
            });
            notifyFullThreshold.setSummaryProvider(new GBSimpleSummaryProvider(
                    requireContext().getString(R.string.default_percentage, batteryConfig.getDefaultFullThreshold()),
                    R.string.battery_percentage_str
            ));
            batteryScreen.addPreference(notifyFullThreshold);
        }

        if (coordinator.addBatteryPollingSettings()) {
            final Preference prefHeader = new PreferenceCategoryMultiline(requireContext());
            prefHeader.setKey("pref_battery_polling_header");
            prefHeader.setIconSpaceReserved(false);
            prefHeader.setTitle(R.string.pref_battery_polling_configuration);
            prefHeader.setSummary(R.string.pref_battery_polling_summary);
            batteryScreen.addPreference(prefHeader);

            final SwitchPreferenceCompat pollingToggle = new SwitchPreferenceCompat(requireContext());
            pollingToggle.setLayoutResource(R.layout.preference_checkbox);
            pollingToggle.setKey(PREF_BATTERY_POLLING_ENABLE);
            pollingToggle.setTitle(R.string.pref_battery_polling_enable);
            pollingToggle.setDefaultValue(true);
            pollingToggle.setIconSpaceReserved(false);
            batteryScreen.addPreference(pollingToggle);

            final EditTextPreference pollingInterval = new EditTextPreference(requireContext());
            pollingInterval.setKey(PREF_BATTERY_POLLING_INTERVAL);
            pollingInterval.setTitle(R.string.pref_battery_polling_interval);
            pollingInterval.setDialogTitle(R.string.pref_battery_polling_interval);
            pollingInterval.setIconSpaceReserved(false);
            pollingInterval.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                // Max is set to 8 days, which should be more than enough
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 11520, true));
                editText.setSelection(editText.getText().length());
            });
            pollingInterval.setSummaryProvider(new GBSimpleSummaryProvider(
                    getString(R.string.interval_fifteen_minutes),
                    R.string.pref_battery_polling_interval_format
            ));
            batteryScreen.addPreference(pollingInterval);
        }
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    @Override
    public void notifyPreferenceChanged(final String preferenceKey) {
        invokeLater(new Runnable() {
            @Override
            public void run() {
                GBApplication.deviceService(device).onSendConfiguration(preferenceKey);
            }
        });
    }

    private void setChangeListener(final String rootKey) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final Prefs prefs = new Prefs(getPreferenceManager().getSharedPreferences());

        final ListPreference languageListPreference = findPreference("language");
        if (languageListPreference != null) {
            final String[] supportedLanguages = coordinator.getSupportedLanguageSettings(device);
            if (supportedLanguages != null) {
                CharSequence[] entries = languageListPreference.getEntries();
                CharSequence[] values = languageListPreference.getEntryValues();
                for (int i = entries.length - 1; i >= 0; i--) {
                    if (!ArrayUtils.contains(supportedLanguages, values[i])) {
                        entries = ArrayUtils.remove(entries, i);
                        values = ArrayUtils.remove(values, i);
                    }
                }
                languageListPreference.setEntries(entries);
                languageListPreference.setEntryValues(values);
            }
            DeviceSettingsUtils.sortListPreference(
                    languageListPreference,
                    (supportedLanguages != null) && supportedLanguages.length > 0 && "auto".equals(supportedLanguages[0])
            );
        }

        final ListPreference transliterationPreference = findPreference(DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_LANGUAGES);
        if (transliterationPreference != null) {
            DeviceSettingsUtils.sortListPreference(transliterationPreference, false);
        }

        final ListPreference weightScaleUnitPreference = findPreference(PREF_WEIGHT_SCALE_UNIT);
        if (weightScaleUnitPreference != null) {
            DeviceSettingsUtils.sortListPreference(weightScaleUnitPreference, false);
        }

        String disconnectNotificationState = prefs.getString(PREF_DISCONNECT_NOTIFICATION, PREF_DO_NOT_DISTURB_OFF);
        boolean disconnectNotificationScheduled = disconnectNotificationState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference disconnectNotificationStart = findPreference(PREF_DISCONNECT_NOTIFICATION_START);
        if (disconnectNotificationStart != null) {
            disconnectNotificationStart.setEnabled(disconnectNotificationScheduled);
            disconnectNotificationStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION_START);
                    return true;
                }
            });
        }

        final Preference disconnectNotificationEnd = findPreference(PREF_DISCONNECT_NOTIFICATION_END);
        if (disconnectNotificationEnd != null) {
            disconnectNotificationEnd.setEnabled(disconnectNotificationScheduled);
            disconnectNotificationEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION_END);
                    return true;
                }
            });
        }

        final Preference disconnectNotification = findPreference(PREF_DISCONNECT_NOTIFICATION);
        if (disconnectNotification != null) {
            disconnectNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(disconnectNotificationStart).setEnabled(scheduled);
                    Objects.requireNonNull(disconnectNotificationEnd).setEnabled(scheduled);
                    notifyPreferenceChanged(PREF_DISCONNECT_NOTIFICATION);
                    return true;
                }
            });

        }

        String nightModeState = prefs.getString(MiBandConst.PREF_NIGHT_MODE, PREF_NIGHT_MODE_OFF);
        boolean nightModeScheduled = nightModeState.equals(PREF_NIGHT_MODE_SCHEDULED);

        final Preference nightModeStart = findPreference(PREF_NIGHT_MODE_START);
        if (nightModeStart != null) {
            nightModeStart.setEnabled(nightModeScheduled);
            nightModeStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_NIGHT_MODE_START);
                    return true;
                }
            });
        }

        final Preference nightModeEnd = findPreference(PREF_NIGHT_MODE_END);
        if (nightModeEnd != null) {
            nightModeEnd.setEnabled(nightModeScheduled);
            nightModeEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_NIGHT_MODE_END);
                    return true;
                }
            });
        }

        final Preference nightMode = findPreference(PREF_NIGHT_MODE);
        if (nightMode != null) {

            nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_NIGHT_MODE_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(nightModeStart).setEnabled(scheduled);
                    Objects.requireNonNull(nightModeEnd).setEnabled(scheduled);

                    notifyPreferenceChanged(PREF_NIGHT_MODE);
                    return true;
                }
            });
        }


        String doNotDisturbState = prefs.getString(PREF_DO_NOT_DISTURB, PREF_DO_NOT_DISTURB_OFF);
        boolean doNotDisturbScheduled = doNotDisturbState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference doNotDisturbStart = findPreference(PREF_DO_NOT_DISTURB_START);
        if (doNotDisturbStart != null) {
            doNotDisturbStart.setEnabled(doNotDisturbScheduled);
            doNotDisturbStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB_START);
                    return true;
                }
            });
        }

        final Preference doNotDisturbEnd = findPreference(PREF_DO_NOT_DISTURB_END);
        if (doNotDisturbEnd != null) {
            doNotDisturbEnd.setEnabled(doNotDisturbScheduled);
            doNotDisturbEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB_END);
                    return true;
                }
            });
        }

        final Preference doNotDisturb = findPreference(PREF_DO_NOT_DISTURB);
        if (doNotDisturb != null) {
            doNotDisturb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());

                    Objects.requireNonNull(doNotDisturbStart).setEnabled(scheduled);
                    Objects.requireNonNull(doNotDisturbEnd).setEnabled(scheduled);

                    notifyPreferenceChanged(PREF_DO_NOT_DISTURB);
                    return true;
                }
            });
        }

        addPreferenceHandlerFor(PREF_SEND_APP_NOTIFICATIONS);
        addPreferenceHandlerFor(PREF_SWIPE_UNLOCK);
        addPreferenceHandlerFor(PREF_MI2_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DATEFORMAT);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS);
        addPreferenceHandlerFor(PREF_DISPLAY_ITEMS_SORTABLE);
        addPreferenceHandlerFor(PREF_WORKOUT_ACTIVITY_TYPES_SORTABLE);
        addPreferenceHandlerFor(PREF_SHORTCUTS);
        addPreferenceHandlerFor(PREF_SHORTCUTS_SORTABLE);
        addPreferenceHandlerFor(PREF_CONTROL_CENTER_SORTABLE);
        addPreferenceHandlerFor(PREF_LANGUAGE);
        addPreferenceHandlerFor(PREF_EXPOSE_HR_THIRDPARTY);
        addPreferenceHandlerFor(PREF_BT_CONNECTED_ADVERTISEMENT);
        addPreferenceHandlerFor(PREF_WEARLOCATION);
        addPreferenceHandlerFor(PREF_VIBRATION_ENABLE);
        addPreferenceHandlerFor(PREF_NOTIFICATION_ENABLE);
        addPreferenceHandlerFor(PREF_DEVICE_NAME);
        addPreferenceHandlerFor(PREF_SCREEN_BRIGHTNESS);
        addPreferenceHandlerFor(PREF_SCREEN_AUTO_BRIGHTNESS);
        addPreferenceHandlerFor(PREF_SCREEN_ORIENTATION);
        addPreferenceHandlerFor(PREF_SCREEN_TIMEOUT);
        addPreferenceHandlerFor(PREF_TIMEFORMAT);
        addPreferenceHandlerFor(PREF_UPPER_BUTTON_LONG_PRESS);
        addPreferenceHandlerFor(PREF_LOWER_BUTTON_SHORT_PRESS);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_SHORT);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_LONG);
        addPreferenceHandlerFor(PREF_BUTTON_1_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_BUTTON_2_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_BUTTON_3_FUNCTION_DOUBLE);
        addPreferenceHandlerFor(PREF_VIBRATION_STRENGH_PERCENTAGE);
        addPreferenceHandlerFor(PREF_POWER_MODE);
        addPreferenceHandlerFor(PREF_CONNECTION_DURATION);
        addPreferenceHandlerFor(PREF_LIFTWRIST_NOSHED);
        addPreferenceHandlerFor(PREF_DISCONNECTNOTIF_NOSHED);
        addPreferenceHandlerFor(PREF_BUTTON_BP_CALIBRATE);
        addPreferenceHandlerFor(PREF_ALTITUDE_CALIBRATE);
        addPreferenceHandlerFor(PREF_INACTIVITY_ENABLE);
        addPreferenceHandlerFor(PREF_INACTIVITY_START);
        addPreferenceHandlerFor(PREF_INACTIVITY_END);
        addPreferenceHandlerFor(PREF_INACTIVITY_THRESHOLD);
        addPreferenceHandlerFor(PREF_INACTIVITY_THRESHOLD_EXTENDED);
        addPreferenceHandlerFor(PREF_INACTIVITY_MO);
        addPreferenceHandlerFor(PREF_INACTIVITY_TU);
        addPreferenceHandlerFor(PREF_INACTIVITY_WE);
        addPreferenceHandlerFor(PREF_INACTIVITY_TH);
        addPreferenceHandlerFor(PREF_INACTIVITY_FR);
        addPreferenceHandlerFor(PREF_INACTIVITY_SA);
        addPreferenceHandlerFor(PREF_INACTIVITY_SU);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND_START);
        addPreferenceHandlerFor(PREF_INACTIVITY_DND_END);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SWITCH);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_SLEEP);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_INTERVAL);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_START);
        addPreferenceHandlerFor(PREF_AUTOHEARTRATE_END);
        addPreferenceHandlerFor(PREF_HEARTRATE_ACTIVITY_MONITORING);
        addPreferenceHandlerFor(PREF_HEARTRATE_ALERT_ENABLED);
        addPreferenceHandlerFor(PREF_HEARTRATE_STRESS_MONITORING);
        addPreferenceHandlerFor(PREF_HEARTRATE_STRESS_RELAXATION_REMINDER);
        addPreferenceHandlerFor(PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING);
        addPreferenceHandlerFor(PREF_SPO2_ALL_DAY_MONITORING);
        addPreferenceHandlerFor(PREF_SPO2_MEASUREMENT_INTERVAL);
        addPreferenceHandlerFor(PREF_SPO2_MEASUREMENT_TIME);
        addPreferenceHandlerFor(PREF_SPO2_MEASUREMENT_START);
        addPreferenceHandlerFor(PREF_SPO2_MEASUREMENT_END);
        addPreferenceHandlerFor(PREF_SPO2_LOW_ALERT_THRESHOLD);
        addPreferenceHandlerFor(PREF_HRV_ALL_DAY_MONITORING);
        addPreferenceHandlerFor(PREF_TEMPERATURE_ALL_DAY_MONITORING);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_START);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOAUTO_END);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_START);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_END);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_MO);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_TU);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_WE);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_TH);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_FR);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_SA);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_SU);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_LIFT_WRIST);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_NOT_WEAR);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_BOOL);
        addPreferenceHandlerFor(PREF_DO_NOT_DISTURB_FOLLOW_PHONE);
        addPreferenceHandlerFor(PREF_FIND_PHONE);
        addPreferenceHandlerFor(PREF_FIND_PHONE_DURATION);
        addPreferenceHandlerFor(PREF_AUTOLIGHT);
        addPreferenceHandlerFor(PREF_LIGHT_DURATION_LONGER);
        addPreferenceHandlerFor(PREF_AUTOREMOVE_MESSAGE);
        addPreferenceHandlerFor(PREF_AUTOREMOVE_NOTIFICATIONS);
        addPreferenceHandlerFor(PREF_PREVIEW_MESSAGE_IN_TITLE);
        addPreferenceHandlerFor(PREF_CASIO_ALERT_EMAIL);
        addPreferenceHandlerFor(PREF_CASIO_ALERT_SMS);
        addPreferenceHandlerFor(PREF_CASIO_ALERT_CALL);
        addPreferenceHandlerFor(PREF_CASIO_ALERT_CALENDAR);
        addPreferenceHandlerFor(PREF_CASIO_ALERT_OTHER);
        addPreferenceHandlerFor(PREF_SCREEN_ON_ON_NOTIFICATIONS);
        addPreferenceHandlerFor(PREF_SCREEN_ON_ON_NOTIFICATIONS_TIMEOUT);
        addPreferenceHandlerFor(PREF_WORKOUT_KEEP_SCREEN_ON);
        addPreferenceHandlerFor(PREF_KEY_VIBRATION);
        addPreferenceHandlerFor(PREF_OPERATING_SOUNDS);
        addPreferenceHandlerFor(PREF_FAKE_RING_DURATION);
        addPreferenceHandlerFor(PREF_ANTILOST_ENABLED);
        addPreferenceHandlerFor(PREF_HYDRATION_SWITCH);
        addPreferenceHandlerFor(PREF_HYDRATION_PERIOD);
        addPreferenceHandlerFor(PREF_HYDRATION_DND);
        addPreferenceHandlerFor(PREF_HYDRATION_DND_START);
        addPreferenceHandlerFor(PREF_HYDRATION_DND_END);
        addPreferenceHandlerFor(PREF_AMPM_ENABLED);
        addPreferenceHandlerFor(PREF_SOUNDS);
        addPreferenceHandlerFor(PREF_CAMERA_REMOTE);
        addPreferenceHandlerFor(PREF_SCREEN_LIFT_WRIST);
        addPreferenceHandlerFor(PREF_SYNC_CALENDAR);
        addPreferenceHandlerFor(PREF_CALENDAR_LOOKAHEAD_DAYS);

        addPreferenceHandlerFor(PREF_BATTERY_POLLING_ENABLE);
        addPreferenceHandlerFor(PREF_BATTERY_POLLING_INTERVAL);
        addPreferenceHandlerFor(PREF_TIME_SYNC);

        addPreferenceHandlerFor(PREF_BLUETOOTH_CALLS_ENABLED);
        addPreferenceHandlerFor(PREF_DISPLAY_CALLER);
        addPreferenceHandlerFor(PREF_NOTIFICATION_DELAY_CALLS);
        addPreferenceHandlerFor(PREF_CALL_REJECT_METHOD);
        addPreferenceHandlerFor(PREF_AUTO_REPLY_INCOMING_CALL);
        addPreferenceHandlerFor(PREF_ENABLE_CALL_REJECT);
        addPreferenceHandlerFor(PREF_ENABLE_SMS_QUICK_REPLY);

        addPreferenceHandlerFor(PREF_SLEEP_MODE_SLEEP_SCREEN);
        addPreferenceHandlerFor(PREF_SLEEP_MODE_SMART_ENABLE);

        addPreferenceHandlerFor(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE);
        addPreferenceHandlerFor(PREF_NOISE_CONTROL_SELECTOR);
        addPreferenceHandlerFor(PREF_WEAR_SENSOR_TOGGLE);
        addPreferenceHandlerFor(PREF_BANDW_PSERIES_GUI_VPT_LEVEL);

        addPreferenceHandlerFor(PREF_EVEN_REALITIES_SCREEN_HEIGHT);
        addPreferenceHandlerFor(PREF_EVEN_REALITIES_SCREEN_DEPTH);
        addPreferenceHandlerFor(PREF_EVEN_REALITIES_SCREEN_ACTIVATION_ANGLE);

        addPreferenceHandlerFor(PREF_HYBRID_HR_DRAW_WIDGET_CIRCLES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_FORCE_WHITE_COLOR);
        addPreferenceHandlerFor(PREF_HYBRID_HR_SAVE_RAW_ACTIVITY_FILES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_DANGEROUS_EXTERNAL_INTENTS);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ENABLED);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_ASK_FIRST);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_RUNNING_MINUTES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ENABLED);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_ASK_FIRST);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_BIKING_MINUTES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ENABLED);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_ASK_FIRST);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_WALKING_MINUTES);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ENABLED);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_ASK_FIRST);
        addPreferenceHandlerFor(PREF_HYBRID_HR_ACTIVITY_RECOGNITION_ROWING_MINUTES);

        addPreferenceHandlerFor(PREF_SONYSWR12_STAMINA);
        addPreferenceHandlerFor(PREF_SONYSWR12_LOW_VIBRATION);
        addPreferenceHandlerFor(PREF_SONYSWR12_SMART_INTERVAL);

        addPreferenceHandlerFor(PREF_NOTHING_EAR1_INEAR);
        addPreferenceHandlerFor(PREF_NOTHING_EAR1_AUDIOMODE);

        addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_INEAR);
        addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_AUDIOMODE);
        addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_ANC_MODE);
        addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_VOICE_BOOST);
        addPreferenceHandlerFor(PREF_HUAWEI_FREEBUDS_BETTER_AUDIO_QUALITY);


        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_VOLUME);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_LOCK_TOUCH);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_GAME_MODE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER_DOLBY);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_EQUALIZER_MODE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_LEFT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_RIGHT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_LIVE_ANC);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRESSURE_RELIEF);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_ANC_LEVEL);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_SOUND);
        addPreferenceHandlerFor(PREF_GALAXY_PRO_DOUBLE_TAP_EDGE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_IN_EAR_DETECTION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_VOICE_DETECT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_VOICE_DETECT_DURATION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_BALANCE);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_READ_NOTIFICATIONS_OUTLOUD);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_MODE_DURING_CALL);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_RIGHT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_LEFT);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_PRO_AMBIENT_SOUND_TONE);
        addPreferenceHandlerFor(PREFS_NOISE_CONTROL_WITH_ONE_EARBUD);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_AMBIENT_MODE);
        addPreferenceHandlerFor(PREFS_GALAXY_BUDS_SEAMLESS_CONNECTION);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH);
        addPreferenceHandlerFor(PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH);

        addPreferenceHandlerFor(MORNING_UPDATES_ENABLED);
        addPreferenceHandlerFor(MORNING_UPDATES_CATEGORIES_SORTABLE);

        addPreferenceHandlerFor(SHORTCUT_CARDS_SORTABLE);

        addPreferenceHandlerFor(PREF_WATCHFACE);

        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_NOISE_CANCELLING_STRENGTH);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_TRANSPARENCY_STRENGTH);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_ADAPTIVE_NOISE_CANCELLING);
//        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_PERSONALIZED_NOISE_CANCELLING);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_LEFT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_RIGHT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_LEFT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_RIGHT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_LEFT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_RIGHT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_LEFT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_RIGHT);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_WEARING_DETECTION);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_AUTO_REPLY_PHONECALL);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION);
//        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_SURROUND_SOUND);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND);
//        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_SURROUND_SOUND_MODE);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k);
        addPreferenceHandlerFor(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k);

        addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL);
        addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE);
        addPreferenceHandlerFor(PREF_SONY_FOCUS_VOICE);
        addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_LEVEL);
        addPreferenceHandlerFor(PREF_SONY_SOUND_POSITION);
        addPreferenceHandlerFor(PREF_SONY_SURROUND_MODE);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_MODE);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_400);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_1000);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_2500);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_6300);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BAND_16000);
        addPreferenceHandlerFor(PREF_SONY_EQUALIZER_BASS);
        addPreferenceHandlerFor(PREF_SONY_AUDIO_UPSAMPLING);
        addPreferenceHandlerFor(PREF_SONY_TOUCH_SENSOR);
        addPreferenceHandlerFor(PREF_SONY_PAUSE_WHEN_TAKEN_OFF);
        addPreferenceHandlerFor(PREF_SONY_BUTTON_MODE_LEFT);
        addPreferenceHandlerFor(PREF_SONY_BUTTON_MODE_RIGHT);
        addPreferenceHandlerFor(PREF_SONY_QUICK_ACCESS_DOUBLE_TAP);
        addPreferenceHandlerFor(PREF_SONY_QUICK_ACCESS_TRIPLE_TAP);
        addPreferenceHandlerFor(PREF_SONY_AUTOMATIC_POWER_OFF);
        addPreferenceHandlerFor(PREF_SONY_NOTIFICATION_VOICE_GUIDE);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE);
        addPreferenceHandlerFor(PREF_SONY_SPEAK_TO_CHAT_TIMEOUT);
        addPreferenceHandlerFor(PREF_SONY_CONNECT_TWO_DEVICES);
        addPreferenceHandlerFor(PREF_SONY_ADAPTIVE_VOLUME_CONTROL);
        addPreferenceHandlerFor(PREF_SONY_WIDE_AREA_TAP);

        addPreferenceHandlerFor(PREF_MEDIA_SOURCE);
        addPreferenceHandlerFor(PREF_MEDIA_PLAYBACK_MODE);
        addPreferenceHandlerFor(PREF_SHOKZ_EQUALIZER_BLUETOOTH);
        addPreferenceHandlerFor(PREF_SHOKZ_EQUALIZER_MP3);

        addPreferenceHandlerFor(PREF_SHOKZ_CONTROLS_LONG_PRESS_MULTI_FUNCTION);
        addPreferenceHandlerFor(PREF_SHOKZ_CONTROLS_SIMULTANEOUS_VOLUME_UP_DOWN);

        addPreferenceHandlerFor(PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL);
        addPreferenceHandlerFor(PREF_SOUNDCORE_WIND_NOISE_REDUCTION);
        addPreferenceHandlerFor(PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING);
        addPreferenceHandlerFor(PREF_SOUNDCORE_ANC_MODE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_TOUCH_TONE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_WEARING_TONE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_BATTERY_LOW_TONE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_WEARING_DETECTION);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_TOUCH_DISABLED);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT);
        addPreferenceHandlerFor(PREF_SOUNDCORE_VOICE_PROMPTS);
        addPreferenceHandlerFor(PREF_SOUNDCORE_BUTTON_BRIGHTNESS);
        addPreferenceHandlerFor(PREF_SOUNDCORE_AUTO_POWER_OFF);
        addPreferenceHandlerFor(PREF_SOUNDCORE_LDAC_MODE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_GAMING_MODE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_ADAPTIVE_DIRECTION);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_PRESET);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_DIRECTION);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND1_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND1_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND2_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND2_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND3_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND3_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND4_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND4_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND5_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND5_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND6_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND6_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND7_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND7_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND8_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND8_VALUE);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND9_FREQ);
        addPreferenceHandlerFor(PREF_SOUNDCORE_EQUALIZER_BAND9_VALUE);

        addPreferenceHandlerFor(PREF_MOONDROP_EQUALIZER_PRESET);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_PLAY_PAUSE_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_PLAY_PAUSE_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_MEDIA_PREV_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_MEDIA_PREV_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_MEDIA_NEXT_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_MEDIA_NEXT_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_CALL_PICK_HANG_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_CALL_PICK_HANG_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_CALL_START_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_CALL_START_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_ASSISTANT_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_ASSISTANT_TRIGGER);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_ANC_MODE_EARBUD);
        addPreferenceHandlerFor(PREF_MOONDROP_TOUCH_ANC_MODE_TRIGGER);

        addPreferenceHandlerFor(PREF_MISCALE_WEIGHT_UNIT);
        addPreferenceHandlerFor(PREF_MISCALE_SMALL_OBJECTS);

        addPreferenceHandlerFor(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER);
        addPreferenceHandlerFor(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER);
        addPreferenceHandlerFor(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER);
        addPreferenceHandlerFor(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER);

        addPreferenceHandlerFor(PREF_FEMOMETER_MEASUREMENT_MODE);

        addPreferenceHandlerFor(PREF_MOYOUNG_WATCH_FACE);
        addPreferenceHandlerFor(PREF_MOYOUNG_DEVICE_VERSION);

        addPreferenceHandlerFor(PREF_QC35_NOISE_CANCELLING_LEVEL);

        addPreferenceHandlerFor(PREF_DUAL_DEVICE_SUPPORT);

        addPreferenceHandlerFor(PREF_DEVICE_LOGS_TOGGLE);

        addPreferenceHandlerFor(PREF_USER_FITNESS_GOAL);
        addPreferenceHandlerFor(PREF_USER_FITNESS_GOAL_NOTIFICATION);
        addPreferenceHandlerFor(PREF_USER_FITNESS_GOAL_SECONDARY);

        addPreferenceHandlerFor(PREF_VITALITY_SCORE_7_DAY);
        addPreferenceHandlerFor(PREF_VITALITY_SCORE_DAILY);

        addPreferenceHandlerFor(PREF_UM25_SHOW_THRESHOLD_NOTIFICATION);
        addPreferenceHandlerFor(PREF_UM25_SHOW_THRESHOLD);
        addPreferenceHandlerFor(PREF_HOURLY_CHIME_ENABLE);
        addPreferenceHandlerFor(PREF_HOURLY_CHIME_START);
        addPreferenceHandlerFor(PREF_HOURLY_CHIME_END);

        addPreferenceHandlerFor(PREF_WORKOUT_DETECTION_CATEGORIES);
        addPreferenceHandlerFor(PREF_WORKOUT_DETECTION_ALERT);
        addPreferenceHandlerFor(PREF_WORKOUT_DETECTION_SENSITIVITY);

        addPreferenceHandlerFor(PREF_GPS_MODE_PRESET);
        addPreferenceHandlerFor(PREF_GPS_BAND);
        addPreferenceHandlerFor(PREF_GPS_COMBINATION);
        addPreferenceHandlerFor(PREF_GPS_SATELLITE_SEARCH);
        addPreferenceHandlerFor(PREF_AGPS_EXPIRY_REMINDER_ENABLED);
        addPreferenceHandlerFor(PREF_AGPS_EXPIRY_REMINDER_TIME);
        addPreferenceHandlerFor(PREF_ALWAYS_ON_DISPLAY_FOLLOW_WATCHFACE);
        addPreferenceHandlerFor(PREF_ALWAYS_ON_DISPLAY_STYLE);
        addPreferenceHandlerFor(PREF_WEARDIRECTION);
        addPreferenceHandlerFor(PREF_WEARMODE);

        addPreferenceHandlerFor(PREF_VOLUME);
        addPreferenceHandlerFor(PREF_CROWN_VIBRATION);
        addPreferenceHandlerFor(PREF_ALERT_TONE);
        addPreferenceHandlerFor(PREF_COVER_TO_MUTE);
        addPreferenceHandlerFor(PREF_VIBRATE_FOR_ALERT);
        addPreferenceHandlerFor(PREF_VIBRATION_INTENSITY);
        addPreferenceHandlerFor(PREF_TEXT_TO_SPEECH);

        addPreferenceHandlerFor(PREF_OFFLINE_VOICE_RESPOND_TURN_WRIST);
        addPreferenceHandlerFor(PREF_OFFLINE_VOICE_RESPOND_SCREEN_ON);
        addPreferenceHandlerFor(PREF_OFFLINE_VOICE_RESPONSE_DURING_SCREEN_LIGHTING);
        addPreferenceHandlerFor(PREF_OFFLINE_VOICE_LANGUAGE);

        addPreferenceHandlerFor(PREF_VOICE_SERVICE_LANGUAGE);

        addPreferenceHandlerFor(PREF_TEMPERATURE_SCALE_CF);

        addPreferenceHandlerFor(PREF_PREFIX_NOTIFICATION_WITH_APP);

        addPreferenceHandlerFor(PREF_SLEEP_MODE_SCHEDULE_ENABLED);
        addPreferenceHandlerFor(PREF_SLEEP_MODE_SCHEDULE_START);
        addPreferenceHandlerFor(PREF_SLEEP_MODE_SCHEDULE_END);

        addPreferenceHandlerFor(PREF_CLAP_HANDS_TO_WAKEUP_DEVICE);
        addPreferenceHandlerFor(PREF_POWER_SAVING);

        addPreferenceHandlerFor(PREF_HEARTRATE_AUTOMATIC_ENABLE);
        addPreferenceHandlerFor(PREF_SPO_AUTOMATIC_ENABLE);

        addPreferenceHandlerFor(PREF_CYCLING_SENSOR_PERSISTENCE_INTERVAL);
        addPreferenceHandlerFor(PREF_CYCLING_SENSOR_WHEEL_DIAMETER);

        addPreferenceHandlerFor(PREF_SPEAK_NOTIFICATIONS_FOCUS_EXCLUSIVE);

        addPreferenceHandlerFor(PREFS_KEY_DEVICE_BLE_API_DEVICE_STATE);
        addPreferenceHandlerFor(PREFS_KEY_DEVICE_BLE_API_DEVICE_READ_WRITE);
        addPreferenceHandlerFor(PREFS_KEY_DEVICE_BLE_API_DEVICE_NOTIFY);
        addPreferenceHandlerFor(PREFS_KEY_DEVICE_BLE_API_CHARACTERISTIC);
        addPreferenceHandlerFor(PREFS_KEY_DEVICE_BLE_API_PACKAGE);

        addPreferenceHandlerFor("lock");

        addPreferenceHandlerFor(PREF_BATTERY_MINIMUM_CHARGE);
        addPreferenceHandlerFor(PREF_BATTERY_ALLOW_PASS_THROUGH);

        addPreferenceHandlerFor(PREF_DISPLAY_ENABLED);
        addPreferenceHandlerFor(PREF_DISPLAY_ENABLED_ALL_DAY);
        addPreferenceHandlerFor(PREF_DISPLAY_ON_START);
        addPreferenceHandlerFor(PREF_DISPLAY_ON_END);

        addPreferenceHandlerFor(PREF_CALENDAR_SYNC_EVENTS_AMOUNT);
        addPreferenceHandlerFor(PREF_CALENDAR_MAX_TITLE_LENGTH);
        addPreferenceHandlerFor(PREF_CALENDAR_MAX_DESC_LENGTH);
        addPreferenceHandlerFor(PREF_CALENDAR_TARGET_APP);

        addPreferenceHandlerFor(PREF_ATC_BLE_OEPL_MODEL);
        addPreferenceHandlerFor(PREF_ATC_BLE_OEPL_BLE_ADV_INTERVAL);
        addPreferenceHandlerFor(PREF_ATC_BLE_OEPL_OEPL_PROTOCOL_ENABLE);

        final Preference dischargeIntervalsSet = findPreference(PREF_BATTERY_DISCHARGE_INTERVALS_SET);
        if (dischargeIntervalsSet != null) {
            dischargeIntervalsSet.setOnPreferenceClickListener(preference -> {
                notifyPreferenceChanged(PREF_BATTERY_DISCHARGE_INTERVALS_SET);
                return true;
            });
        }

        String sleepTimeState = prefs.getString(PREF_SLEEP_TIME, PREF_DO_NOT_DISTURB_OFF);
        boolean sleepTimeScheduled = sleepTimeState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);

        final Preference sleepTimeInfo = findPreference(PREF_SLEEP_TIME);
        if (sleepTimeInfo != null) {
            //sleepTimeInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(sleepTimeInfo));
            sleepTimeInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME);
                    return true;
                }
            });
        }

        final Preference sleepTimeStart = findPreference(PREF_SLEEP_TIME_START);
        if (sleepTimeStart != null) {
            sleepTimeStart.setEnabled(sleepTimeScheduled);
            sleepTimeStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME_START);
                    return true;
                }
            });
        }

        final Preference sleepTimeEnd = findPreference(PREF_SLEEP_TIME_END);
        if (sleepTimeEnd != null) {
            sleepTimeEnd.setEnabled(sleepTimeScheduled);
            sleepTimeEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_SLEEP_TIME_END);
                    return true;
                }
            });
        }

        final Preference sleepTime = findPreference(PREF_SLEEP_TIME);
        if (sleepTime != null) {
            sleepTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
                    Objects.requireNonNull(sleepTimeStart).setEnabled(scheduled);
                    Objects.requireNonNull(sleepTimeEnd).setEnabled(scheduled);
                    if (sleepTimeInfo != null) {
                        //sleepTimeInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(newVal.toString()));
                    }
                    notifyPreferenceChanged(PREF_SLEEP_TIME);
                    return true;
                }
            });
        }
        String displayOnLiftState = prefs.getString(PREF_ACTIVATE_DISPLAY_ON_LIFT, PREF_DO_NOT_DISTURB_OFF);
        boolean displayOnLiftScheduled = displayOnLiftState.equals(PREF_DO_NOT_DISTURB_SCHEDULED);
        boolean displayOnLiftOff = displayOnLiftState.equals(PREF_DO_NOT_DISTURB_OFF);

        final Preference rotateWristCycleInfo = findPreference(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
        if (rotateWristCycleInfo != null) {
            rotateWristCycleInfo.setEnabled(!PREF_DO_NOT_DISTURB_OFF.equals(displayOnLiftState));
            rotateWristCycleInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
                    return true;
                }
            });
        }

        final Preference phoneSilentMode = findPreference(PREF_PHONE_SILENT_MODE);
        if (phoneSilentMode != null) {
            phoneSilentMode.setOnPreferenceChangeListener((preference, newVal) -> {
                final AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
                GBApplication.deviceService(device).onChangePhoneSilentMode(audioManager.getRingerMode());
                return true;
            });
        }

        final String alwaysOnDisplayState = prefs.getString(PREF_ALWAYS_ON_DISPLAY_MODE, PREF_ALWAYS_ON_DISPLAY_OFF);
        boolean alwaysOnDisplayScheduled = alwaysOnDisplayState.equals(PREF_ALWAYS_ON_DISPLAY_SCHEDULED);
        boolean alwaysOnDisplayOff = alwaysOnDisplayState.equals(PREF_ALWAYS_ON_DISPLAY_OFF);

        final Preference alwaysOnDisplayStart = findPreference(PREF_ALWAYS_ON_DISPLAY_START);
        if (alwaysOnDisplayStart != null) {
            alwaysOnDisplayStart.setEnabled(alwaysOnDisplayScheduled);
            alwaysOnDisplayStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_ALWAYS_ON_DISPLAY_START);
                    return true;
                }
            });
        }

        final Preference alwaysOnDisplayEnd = findPreference(PREF_ALWAYS_ON_DISPLAY_END);
        if (alwaysOnDisplayEnd != null) {
            alwaysOnDisplayEnd.setEnabled(alwaysOnDisplayScheduled);
            alwaysOnDisplayEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_ALWAYS_ON_DISPLAY_END);
                    return true;
                }
            });
        }

        final Preference alwaysOnDisplayMode = findPreference(PREF_ALWAYS_ON_DISPLAY_MODE);
        if (alwaysOnDisplayMode != null) {
            alwaysOnDisplayMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_ALWAYS_ON_DISPLAY_SCHEDULED.equals(newVal.toString());
                    final boolean off = PREF_ALWAYS_ON_DISPLAY_OFF.equals(newVal.toString());
                    alwaysOnDisplayStart.setEnabled(scheduled);
                    alwaysOnDisplayEnd.setEnabled(scheduled);
                    notifyPreferenceChanged(PREF_ALWAYS_ON_DISPLAY_MODE);
                    return true;
                }
            });
        }

        final Preference displayOnLiftStart = findPreference(PREF_DISPLAY_ON_LIFT_START);
        if (displayOnLiftStart != null) {
            displayOnLiftStart.setEnabled(displayOnLiftScheduled);
            displayOnLiftStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_START);
                    return true;
                }
            });
        }

        final Preference displayOnLiftEnd = findPreference(PREF_DISPLAY_ON_LIFT_END);
        if (displayOnLiftEnd != null) {
            displayOnLiftEnd.setEnabled(displayOnLiftScheduled);
            displayOnLiftEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_END);
                    return true;
                }
            });
        }

        final Preference displayOnLiftSensitivity = findPreference(PREF_DISPLAY_ON_LIFT_SENSITIVITY);
        if (displayOnLiftSensitivity != null) {
            displayOnLiftSensitivity.setEnabled(!displayOnLiftOff);
            displayOnLiftSensitivity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(PREF_DISPLAY_ON_LIFT_SENSITIVITY);
                    return true;
                }
            });
        }

        final Preference displayOnLift = findPreference(PREF_ACTIVATE_DISPLAY_ON_LIFT);
        if (displayOnLift != null) {
            displayOnLift.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final boolean scheduled = PREF_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
                    final boolean off = PREF_DO_NOT_DISTURB_OFF.equals(newVal.toString());
                    Objects.requireNonNull(displayOnLiftStart).setEnabled(scheduled);
                    Objects.requireNonNull(displayOnLiftEnd).setEnabled(scheduled);
                    if (rotateWristCycleInfo != null) {
                        rotateWristCycleInfo.setEnabled(!off);
                    }
                    if (displayOnLiftSensitivity != null) {
                        displayOnLiftSensitivity.setEnabled(!off);
                    }
                    notifyPreferenceChanged(PREF_ACTIVATE_DISPLAY_ON_LIFT);
                    return true;
                }
            });
        }

        final Preference worldClocks = findPreference(PREF_WORLD_CLOCKS);
        if (worldClocks != null) {
            worldClocks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getContext(), ConfigureWorldClocks.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference contacts = findPreference(PREF_CONTACTS);
        if (contacts != null) {
            contacts.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getContext(), ConfigureContacts.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference music_management = findPreference(PREF_MUSIC_MANAGEMENT);
        if (music_management != null) {
            music_management.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getContext(), MusicManagerActivity.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference notifications_app_icon_upload = findPreference(PREF_UPLOAD_NOTIFICATIONS_APP_ICON);
        if (notifications_app_icon_upload != null) {
            notifications_app_icon_upload.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getContext(), NotificationsAppIconUploadActivity.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final Preference widgets = findPreference(PREF_WIDGETS);
        if (widgets != null) {
            widgets.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getContext(), WidgetScreensListActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                startActivity(intent);
                return true;
            });
        }

        final Preference calendarBlacklist = findPreference("blacklist_calendars");
        if (calendarBlacklist != null) {
            calendarBlacklist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), CalBlacklistActivity.class);
                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }

        final int cannedRepliesSlotCount = coordinator.getCannedRepliesSlotCount(device);

        final Preference cannedMessagesDismissCall = findPreference("canned_messages_dismisscall_send");
        if (cannedMessagesDismissCall != null) {
            cannedMessagesDismissCall.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    ArrayList<String> messages = new ArrayList<>();
                    for (int i = 1; i <= cannedRepliesSlotCount; i++) {
                        String message = prefs.getString("canned_message_dismisscall_" + i, null);
                        if (message != null && !message.equals("")) {
                            messages.add(message);
                        }
                    }
                    CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                    cannedMessagesSpec.type = CannedMessagesSpec.TYPE_REJECTEDCALLS;
                    cannedMessagesSpec.cannedMessages = messages.toArray(new String[0]);
                    GBApplication.deviceService(device).onSetCannedMessages(cannedMessagesSpec);
                    return true;
                }
            });

            // TODO we could use this to auto-create preferences for watches with > 16 slots
            for (int i = cannedRepliesSlotCount + 1; i <= 16; i++) {
                final Preference cannedReplyPref = findPreference("canned_message_dismisscall_" + i);
                if (cannedReplyPref != null) {
                    cannedReplyPref.setVisible(false);
                }
            }
        }

        final Preference cannedMessagesGeneric = findPreference("canned_messages_generic_send");
        if (cannedMessagesGeneric != null) {

            cannedMessagesGeneric.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    final ArrayList<String> messages = new ArrayList<>();
                    for (int i = 1; i <= cannedRepliesSlotCount; i++) {
                        String message = prefs.getString("canned_reply_" + i, null);
                        if (message != null && !message.equals("")) {
                            messages.add(message);
                        }
                    }
                    final CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                    cannedMessagesSpec.type = CannedMessagesSpec.TYPE_GENERIC;
                    cannedMessagesSpec.cannedMessages = messages.toArray(new String[0]);
                    GBApplication.deviceService().onSetCannedMessages(cannedMessagesSpec);
                    return true;
                }
            });

            // TODO we could use this to auto-create preferences for watches with > 16 slots
            for (int i = cannedRepliesSlotCount + 1; i <= 16; i++) {
                final Preference cannedReplyPref = findPreference("canned_reply_" + i);
                if (cannedReplyPref != null) {
                    cannedReplyPref.setVisible(false);
                }
            }
        }

        final EditTextPreference calendarLookahead = findPreference(DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS);
        if (calendarLookahead != null) {
            setInputTypeFor(DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS, InputType.TYPE_CLASS_NUMBER);
            calendarLookahead.setSummaryProvider(new GBSimpleSummaryProvider(
                    requireContext().getString(R.string.pref_summary_calendar_lookahead, "7"),
                    R.string.pref_summary_calendar_lookahead
            ));
        }

        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_BROADCAST_DELAY, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_PRESS_MAX_INTERVAL, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(HuamiConst.PREF_BUTTON_ACTION_PRESS_COUNT, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(MiBandConst.PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_RESERVE_REMINDERS_CALENDAR, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_DEVICE_GPS_UPDATE_INTERVAL, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_BANGLEJS_TEXT_BITMAP_SIZE, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(DeviceSettingsPreferenceConst.PREF_AUTO_REPLY_INCOMING_CALL_DELAY, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor("hplus_screentime", InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(PREF_CALENDAR_SYNC_EVENTS_AMOUNT, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(PREF_CALENDAR_MAX_TITLE_LENGTH, InputType.TYPE_CLASS_NUMBER);
        setInputTypeFor(PREF_CALENDAR_MAX_DESC_LENGTH, InputType.TYPE_CLASS_NUMBER);
        setNumericInputTypeWithRangeFor(DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVAL1_WATT, 80, 800, false);
        setNumericInputTypeWithRangeFor(DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVAL2_WATT, 80, 800, false);
        setNumericInputTypeWithRangeFor(DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVAL3_WATT, 80, 800, false);
        setNumericInputTypeWithRangeFor(DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVAL4_WATT, 80, 800, false);
        setNumericInputTypeWithRangeFor(DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVAL5_WATT, 80, 800, false);
        setNumericInputTypeWithRangeFor(PREF_BATTERY_MINIMUM_CHARGE, 0, 100, false);

        new PasswordCapabilityImpl().registerPreferences(getContext(), coordinator.getPasswordCapability(), this);
        new HeartRateCapability().registerPreferences(getContext(), coordinator.getHeartRateMeasurementIntervals(), this);

        Set<String> deviceActionsFellSleepSelection = prefs.getStringSet(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS, Collections.emptySet());
        final Preference deviceActionsFellSleep = findPreference(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS);
        final Preference deviceActionsFellSleepBroadcastAction = findPreference(PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST_ACTION);
        final Preference deviceActionsFellSleepBroadcastPackage = findPreference(PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST_PACKAGE);
        boolean deviceActionsFellSleepSelectionBroadcast = deviceActionsFellSleepSelection.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsFellSleep != null) {
            deviceActionsFellSleep.setOnPreferenceChangeListener((preference, newVal) -> {
                final Set<String> newValSet = (Set<String>) newVal;
                final boolean broadcast = newValSet.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
                Objects.requireNonNull(deviceActionsFellSleepBroadcastAction).setEnabled(broadcast);
                Objects.requireNonNull(deviceActionsFellSleepBroadcastPackage).setEnabled(broadcast);
                return true;
            });
        }
        if (deviceActionsFellSleepBroadcastAction != null) {
            deviceActionsFellSleepBroadcastAction.setEnabled(deviceActionsFellSleepSelectionBroadcast);
        }
        if (deviceActionsFellSleepBroadcastPackage != null) {
            deviceActionsFellSleepBroadcastPackage.setEnabled(deviceActionsFellSleepSelectionBroadcast);
        }

        Set<String> deviceActionsWokeUpSelection = prefs.getStringSet(PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS, Collections.emptySet());
        final Preference deviceActionsWokeUp = findPreference(PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS);
        final Preference deviceActionsWokeUpBroadcastAction = findPreference(PREF_DEVICE_ACTION_WOKE_UP_BROADCAST_ACTION);
        final Preference deviceActionsWokeUpBroadcastPackage = findPreference(PREF_DEVICE_ACTION_WOKE_UP_BROADCAST_PACKAGE);
        boolean deviceActionsWokeUpSelectionBroadcast = deviceActionsWokeUpSelection.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsWokeUp != null) {
            deviceActionsWokeUp.setOnPreferenceChangeListener((preference, newVal) -> {
                final Set<String> newValSet = (Set<String>) newVal;
                final boolean broadcast = newValSet.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
                Objects.requireNonNull(deviceActionsWokeUpBroadcastAction).setEnabled(broadcast);
                Objects.requireNonNull(deviceActionsWokeUpBroadcastPackage).setEnabled(broadcast);
                return true;
            });
        }
        if (deviceActionsWokeUpBroadcastAction != null) {
            deviceActionsWokeUpBroadcastAction.setEnabled(deviceActionsWokeUpSelectionBroadcast);
        }
        if (deviceActionsWokeUpBroadcastPackage != null) {
            deviceActionsWokeUpBroadcastPackage.setEnabled(deviceActionsWokeUpSelectionBroadcast);
        }

        Set<String> deviceActionsStartNonWearSelection = prefs.getStringSet(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS, Collections.emptySet());
        final Preference deviceActionsStartNonWear = findPreference(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS);
        final Preference deviceActionsStartNonWearBroadcastAction = findPreference(PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST_ACTION);
        final Preference deviceActionsStartNonWearBroadcastPackage = findPreference(PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST_PACKAGE);
        boolean deviceActionsStartNonWearSelectionBroadcast = deviceActionsStartNonWearSelection.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
        if (deviceActionsStartNonWear != null) {
            deviceActionsStartNonWear.setOnPreferenceChangeListener((preference, newVal) -> {
                final Set<String> newValSet = (Set<String>) newVal;
                final boolean broadcast = newValSet.contains(PREF_DEVICE_ACTION_SELECTION_BROADCAST);
                Objects.requireNonNull(deviceActionsStartNonWearBroadcastAction).setEnabled(broadcast);
                Objects.requireNonNull(deviceActionsStartNonWearBroadcastPackage).setEnabled(broadcast);
                return true;
            });
        }
        if (deviceActionsStartNonWearBroadcastAction != null) {
            deviceActionsStartNonWearBroadcastAction.setEnabled(deviceActionsStartNonWearSelectionBroadcast);
        }
        if (deviceActionsStartNonWearBroadcastPackage != null) {
            deviceActionsStartNonWearBroadcastPackage.setEnabled(deviceActionsStartNonWearSelectionBroadcast);
        }

        // this is to ensure that Control Center device cards are refreshed on preference changes
        final Preference activityInDeviceCard = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD);
        final Preference activityInDeviceSteps = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS);
        final Preference activityInDeviceSleep = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP);
        final Preference activityInDeviceDistance = findPreference(PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE);
        final Preference chartsTabsOrderSelection = findPreference(PREFS_DEVICE_CHARTS_TABS);

        Preference.OnPreferenceClickListener sendIntentRefreshDeviceListListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(refreshIntent);
                return true;
            }
        };

        Preference[] preferencesInControlCenter = {
                activityInDeviceCard,
                activityInDeviceSteps,
                activityInDeviceSleep,
                activityInDeviceDistance,
                chartsTabsOrderSelection,
        };

        for (Preference preferenceInControlCenter : preferencesInControlCenter) {
            if (preferenceInControlCenter != null) {
                preferenceInControlCenter.setOnPreferenceClickListener(sendIntentRefreshDeviceListListener);
            }
        }

        // Replace the PAI with the device-specific name
        if (chartsTabsOrderSelection != null) {
            final ListPreference chartsTabsListPref = (ListPreference) chartsTabsOrderSelection;
            final CharSequence[] entries = chartsTabsListPref.getEntries();
            final CharSequence[] entryValues = chartsTabsListPref.getEntryValues();
            for (int i = 0; i < entries.length; i++) {
                if ("pai".equals(entryValues[i].toString())) {
                    entries[i] = getString(coordinator.getPaiName());
                    break;
                }
            }
            chartsTabsListPref.setEntries(entries);
        }

        final Preference loyaltyCards = findPreference(LoyaltyCardsSettingsConst.PREF_KEY_LOYALTY_CARDS);
        if (loyaltyCards != null) {
            loyaltyCards.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getContext(), LoyaltyCardsSettingsActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
                startActivity(intent);
                return true;
            });
        }

        final Preference multipointPref = findPreference(PREF_MULTIPOINT);
        if (multipointPref != null) {
            multipointPref.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getContext(), MultipointPairingActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
                startActivity(intent);
                return true;
            });
        }

        final Preference audioRecordings = findPreference("pref_key_audio_recordings");
        if (audioRecordings != null) {
            audioRecordings.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getContext(), AudioRecordingsActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
                startActivity(intent);
                return true;
            });
        }

        final Preference notificationSettings = findPreference(PREFS_PER_APP_NOTIFICATION_SETTINGS);
        if (notificationSettings != null) {
            notificationSettings.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getContext(), AppSpecificNotificationSettingsActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
                startActivity(intent);
                return true;
            });
        }

        if (deviceSpecificSettingsCustomizer != null) {
            deviceSpecificSettingsCustomizer.customizeSettings(this, prefs, rootKey);
        }
    }

    static DeviceSpecificSettingsFragment newInstance(GBDevice device, DeviceSettingsActivity.MENU_ENTRY_POINTS applicationSpecificSettings) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (applicationSpecificSettings.equals(DeviceSettingsActivity.MENU_ENTRY_POINTS.AUTH_SETTINGS)) { //auth settings screen
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_pairingkey_explanation);
            for (final int s : coordinator.getSupportedDeviceSpecificAuthenticationSettings()) {
                deviceSpecificSettings.addRootScreen(s);
            }
        } else { //device/application settings
            if (coordinator.getSupportedLanguageSettings(device) != null) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_language_generic);
            }
            DeviceSpecificSettings coordinatorDeviceSettings = coordinator.getDeviceSpecificSettings(device);
            if (coordinatorDeviceSettings != null) {
                deviceSpecificSettings.mergeFrom(coordinatorDeviceSettings);
            }
            final int[] supportedAuthSettings = coordinator.getSupportedDeviceSpecificAuthenticationSettings();
            if (supportedAuthSettings != null && supportedAuthSettings.length > 0) {
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.AUTHENTICATION,
                        supportedAuthSettings
                );
            }

            deviceSpecificSettings.addRootScreen(
                    DeviceSpecificSettingsScreen.CONNECTION,
                    coordinator.getSupportedDeviceSpecificConnectionSettings()
            );

            if (coordinator.getBatteryCount(device) > 0) {
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.BATTERY
                );
            }

            if (coordinator.supportsActivityTracking(device)) {
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.ACTIVITY_INFO,
                        R.xml.devicesettings_chartstabs,
                        R.xml.devicesettings_device_card_activity_card_preferences
                );
            }

            deviceSpecificSettings.addRootScreen(
                    DeviceSpecificSettingsScreen.DEVELOPER,
                    R.xml.devicesettings_device_support_can_reconnect
            );

            deviceSpecificSettings.addRootScreen(
                    DeviceSpecificSettingsScreen.DEVELOPER,
                    R.xml.devicesettings_header_intent_api,
                    R.xml.devicesettings_settings_third_party_apps
            );
            if (coordinator.getConnectionType().usesBluetoothLE()) {
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.DEVELOPER,
                        R.xml.devicesettings_ble_api
                );
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.DEVELOPER,
                        R.xml.devicesettings_gatt_synchronous_writes
                );
                if (GBApplication.isRunningOreoOrLater()) {
                    deviceSpecificSettings.addRootScreen(
                            DeviceSpecificSettingsScreen.DEVELOPER,
                            R.xml.devicesettings_connection_force_legacy_gatt
                    );
                }
            }
            if(BuildConfig.DEBUG) {
                deviceSpecificSettings.addRootScreen(
                        DeviceSpecificSettingsScreen.DEVELOPER,
                        R.xml.devicesettings_stress_test
                );
            }
        }

        final DeviceSpecificSettingsCustomizer deviceSpecificSettingsCustomizer = coordinator.getDeviceSpecificSettingsCustomizer(device);
        final String settingsFileSuffix = device.getAddress();
        final DeviceSpecificSettingsFragment fragment = new DeviceSpecificSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);
        fragment.setDeviceSpecificSettings(deviceSpecificSettings);
        fragment.setDeviceSpecificSettingsCustomizer(deviceSpecificSettingsCustomizer);
        fragment.setDevice(device);

        return fragment;
    }

    @Override
    public void addPreferenceHandlerFor(final String preferenceKey) {
        addPreferenceHandlerFor(preferenceKey, null);
    }

    @Override
    public void addPreferenceHandlerFor(final String preferenceKey, final Preference.OnPreferenceChangeListener extraListener) {
        Preference pref = findPreference(preferenceKey);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    notifyPreferenceChanged(preferenceKey);

                    if (extraListener != null) {
                        return extraListener.onPreferenceChange(preference, newVal);
                    }

                    return true;
                }
            });
        }
    }

    @Override
    public GBDevice getDevice() {
        return device;
    }

    @Override
    protected Set<String> getPreferenceKeysWithSummary() {
        final Set<String> keysWithSummary = new HashSet<>();

        keysWithSummary.add(PREF_INACTIVITY_THRESHOLD);
        keysWithSummary.add(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS);
        keysWithSummary.add(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS);
        keysWithSummary.add(PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS);

        if (deviceSpecificSettingsCustomizer != null) {
            keysWithSummary.addAll(deviceSpecificSettingsCustomizer.getPreferenceKeysWithSummary());
        }

        return keysWithSummary;
    }

    @Override
    protected void onSharedPreferenceChanged(Preference preference) {
        if (deviceSpecificSettingsCustomizer != null) {
            deviceSpecificSettingsCustomizer.onPreferenceChange(preference, DeviceSpecificSettingsFragment.this);
        }
    }
}
