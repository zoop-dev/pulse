/*  Copyright (C) 2024-2025 Marcel Alexandru Nitan, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.devices.SleepAsAndroidFeature;

public class SleepAsAndroidPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new SleepAsAndroidPreferencesFragment();
    }

    public static class SleepAsAndroidPreferencesFragment extends AbstractPreferenceFragment {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.sleepasandroid_preferences, rootKey);

            final ListPreference sleepAsAndroidSlots = findPreference("sleepasandroid_alarm_slot");
            if (sleepAsAndroidSlots != null)
            {
                loadAlarmSlots(sleepAsAndroidSlots);
            }

            final ListPreference sleepAsAndroidDevices = findPreference("sleepasandroid_device");
            if (sleepAsAndroidDevices != null) {
                loadDevicesList(sleepAsAndroidDevices);
                sleepAsAndroidDevices.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                        GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(newValue.toString());
                        if (device != null) {

                            GBApplication.getPrefs().getPreferences().edit().putString("sleepasandroid_device", device.getAddress()).apply();

                            Set<SleepAsAndroidFeature> supportedFeatures = device.getDeviceCoordinator().getSleepAsAndroidFeatures(device);
                            findPreference("sleepasandroid_alarm_slot").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                            findPreference("pref_key_sleepasandroid_feat_alarms").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                            findPreference("pref_key_sleepasandroid_feat_notifications").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.NOTIFICATIONS));
                            findPreference("pref_key_sleepasandroid_feat_movement").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ACCELEROMETER));
                            findPreference("pref_key_sleepasandroid_feat_hr").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.HEART_RATE));
                            findPreference("pref_key_sleepasandroid_feat_rr_intervals").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.RR_INTERVALS));
                            findPreference("pref_key_sleepasandroid_feat_oximetry").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.OXIMETRY));
                            final boolean spo2Supported = supportedFeatures.contains(SleepAsAndroidFeature.SPO2);
                            final boolean spo2AutofetchSupported = supportedFeatures.contains(SleepAsAndroidFeature.SPO2_AUTOFETCH);
                            final SwitchPreferenceCompat spo2Pref = findPreference("pref_key_sleepasandroid_feat_spo2");
                            spo2Pref.setEnabled(spo2Supported);
                            spo2Pref.setSummary(spo2AutofetchSupported ? getString(R.string.pref_sleepasandroid_feat_spo2_autofetch_only_summary) : null);
                            findPreference("pref_key_sleepasandroid_spo2_autofetch_interval").setEnabled(spo2AutofetchSupported && spo2Supported && spo2Pref.isChecked());

                            ListPreference alarmSlots = findPreference("sleepasandroid_alarm_slot");
                            if (alarmSlots != null)
                            {
                                loadAlarmSlots(alarmSlots);
                                if (alarmSlots.getEntries().length > 0)
                                {
                                    alarmSlots.setValueIndex(0);
                                    GB.toast(getString(R.string.alarm_slot_reset), Toast.LENGTH_SHORT, GB.WARN);
                                }
                            }
                        }
                        return false;
                    }
                });

            }

            String defaultDeviceAddr = GBApplication.getPrefs().getString("sleepasandroid_device", "");
            if (!defaultDeviceAddr.isEmpty()) {
                GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(defaultDeviceAddr);
                if (device != null) {

                    Set<SleepAsAndroidFeature> supportedFeatures = device.getDeviceCoordinator().getSleepAsAndroidFeatures(device);
                    findPreference("sleepasandroid_alarm_slot").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                    findPreference("pref_key_sleepasandroid_feat_alarms").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ALARMS));
                    findPreference("pref_key_sleepasandroid_feat_notifications").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.NOTIFICATIONS));
                    findPreference("pref_key_sleepasandroid_feat_movement").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.ACCELEROMETER));
                    findPreference("pref_key_sleepasandroid_feat_hr").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.HEART_RATE));
                    findPreference("pref_key_sleepasandroid_feat_rr_intervals").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.RR_INTERVALS));
                    findPreference("pref_key_sleepasandroid_feat_oximetry").setEnabled(supportedFeatures.contains(SleepAsAndroidFeature.OXIMETRY));
                    final boolean spo2Supported = supportedFeatures.contains(SleepAsAndroidFeature.SPO2);
                    final boolean spo2AutofetchSupported = supportedFeatures.contains(SleepAsAndroidFeature.SPO2_AUTOFETCH);
                    final SwitchPreferenceCompat spo2Pref = findPreference("pref_key_sleepasandroid_feat_spo2");
                    spo2Pref.setEnabled(spo2Supported);
                    spo2Pref.setSummary(spo2AutofetchSupported ? getString(R.string.pref_sleepasandroid_feat_spo2_autofetch_only_summary) : null);
                    findPreference("pref_key_sleepasandroid_spo2_autofetch_interval").setEnabled(spo2AutofetchSupported && spo2Supported && spo2Pref.isChecked());
                }
            }
        }
    }

    private static void loadAlarmSlots(ListPreference sleepAsAndroidSlots) {
        if (sleepAsAndroidSlots != null) {
            String defaultDeviceAddr = GBApplication.getPrefs().getString("sleepasandroid_device", "");
            if (!defaultDeviceAddr.isEmpty()) {
                GBDevice device = GBApplication.app().getDeviceManager().getDeviceByAddress(defaultDeviceAddr);
                if (device != null) {
                    int maxAlarmSlots = device.getDeviceCoordinator().getAlarmSlotCount(device);
                    if (maxAlarmSlots > 0) {
                        List<String> alarmSlots = new ArrayList<>();
                        int reservedAlarmSlots = GBApplication.getPrefs().getInt(DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR, 0);
                        for (int i = reservedAlarmSlots + 1;i < maxAlarmSlots; i++) {
                            alarmSlots.add(String.valueOf(i));
                        }
                        sleepAsAndroidSlots.setEntryValues(alarmSlots.toArray(new String[0]));
                        sleepAsAndroidSlots.setEntries(alarmSlots.toArray(new String[0]));
                    }
                }
            }
        }
    }

    private static void loadDevicesList(ListPreference sleepAsAndroidDevices) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices()
                .stream()
                .sorted(Comparator.comparing(GBDevice::getAliasOrName))
                .collect(Collectors.toList());

        List<String> deviceMACs = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        for (GBDevice dev : devices) {
            if (dev.getDeviceCoordinator().supportsSleepAsAndroid(dev)) {
                deviceMACs.add(dev.getAddress());
                deviceNames.add(dev.getAliasOrName());
            }
        }

        sleepAsAndroidDevices.setEntryValues(deviceMACs.toArray(new String[0]));
        sleepAsAndroidDevices.setEntries(deviceNames.toArray(new String[0]));
    }
}
