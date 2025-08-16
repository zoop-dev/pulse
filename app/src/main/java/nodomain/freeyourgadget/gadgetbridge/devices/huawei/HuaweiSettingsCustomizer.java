/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Set;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.ui.HuaweiStressCalibrationActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_ACTIVITY_REMINDER_GOAL_REACHED;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_ACTIVITY_REMINDER_PROGRESS;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_ACTIVITY_REMINDER_STAND;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_DEBUG_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_CONTINUOUS_SKIN_TEMPERATURE_MEASUREMENT;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_HEART_RATE_HIGH_ALERT;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_HEART_RATE_LOW_ALERT;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_HEART_RATE_REALTIME_MODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_SLEEP_BREATH;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_SPO_LOW_ALERT;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_STRESS_CALIBRATE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_STRESS_SWITCH;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_TRUSLEEP;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_WORKMODE;

public class HuaweiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;
    final HuaweiCoordinator coordinator;

    public HuaweiSettingsCustomizer(final GBDevice device) {
        this.device = device;
        this.coordinator = ((HuaweiCoordinatorSupplier) this.device.getDeviceCoordinator()).getHuaweiCoordinator();
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        if (preference.getKey().equals(PREF_DO_NOT_DISTURB)) {
            final String dndState = ((ListPreference) preference).getValue();
            final XTimePreference dndStart = handler.findPreference(PREF_DO_NOT_DISTURB_START);
            final XTimePreference dndEnd = handler.findPreference(PREF_DO_NOT_DISTURB_END);
            final SwitchPreferenceCompat dndLifWrist = handler.findPreference(PREF_DO_NOT_DISTURB_LIFT_WRIST);
            final SwitchPreferenceCompat dndNotWear = handler.findPreference(PREF_DO_NOT_DISTURB_NOT_WEAR);
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean statusLiftWrist = sharedPrefs.getBoolean(PREF_LIFTWRIST_NOSHED, false);

            dndStart.setEnabled(dndState.equals("scheduled"));
            dndEnd.setEnabled(dndState.equals("scheduled"));
            dndLifWrist.setEnabled(statusLiftWrist && !dndState.equals("off"));
            dndNotWear.setEnabled(dndState.equals("off"));
        }
        if (preference.getKey().equals("huawei_reparse_workout_data")) {
            if (((SwitchPreferenceCompat) preference).isChecked()) {
                GB.toast("Starting workout reparse", Toast.LENGTH_SHORT, 0);
                new HuaweiWorkoutGbParser(handler.getDevice(), handler.getContext()).parseAllWorkouts();
                GB.toast("Workout reparse is complete", Toast.LENGTH_SHORT, 0);

                ((SwitchPreferenceCompat) preference).setChecked(false);
            }
        }
        if (preference.getKey().equals(PREF_FORCE_OPTIONS)) {
            final Preference dnd = handler.findPreference("screen_do_not_disturb");
            if (dnd != null)
                dnd.setVisible(this.coordinator.supportsDoNotDisturb(handler.getDevice()));
            final ListPreference wearLocation = handler.findPreference(PREF_WEARLOCATION);
            if (wearLocation != null)
                wearLocation.setVisible(this.coordinator.supportsWearLocation(handler.getDevice()));
            final ListPreference heartRate = handler.findPreference(PREF_HEARTRATE_AUTOMATIC_ENABLE);
            if (heartRate != null)
                heartRate.setVisible(this.coordinator.supportsHeartRate(handler.getDevice()));
            final ListPreference spo2 = handler.findPreference(PREF_SPO_AUTOMATIC_ENABLE);
            if (spo2 != null)
                spo2.setVisible(this.coordinator.supportsSPo2(handler.getDevice()));
        }
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs, final String rootKey) {

        handler.addPreferenceHandlerFor(PREF_FORCE_OPTIONS);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_SMART_ALARM);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_WEAR_LOCATION);
        handler.addPreferenceHandlerFor(PREF_FORCE_DND_SUPPORT);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_HEARTRATE_SUPPORT);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_SPO2_SUPPORT);

        handler.addPreferenceHandlerFor(PREF_HUAWEI_WORKMODE);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_TRUSLEEP);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_SLEEP_BREATH);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_DEBUG_REQUEST);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_CONTINUOUS_SKIN_TEMPERATURE_MEASUREMENT);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_HEART_RATE_REALTIME_MODE);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_HEART_RATE_LOW_ALERT);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_HEART_RATE_HIGH_ALERT);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_SPO_LOW_ALERT);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_STRESS_SWITCH);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_STRESS_CALIBRATE);

        handler.addPreferenceHandlerFor(PREF_HUAWEI_ACTIVITY_REMINDER_STAND);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_ACTIVITY_REMINDER_PROGRESS);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_ACTIVITY_REMINDER_GOAL_REACHED);


        final Preference forceOptions = handler.findPreference(PREF_FORCE_OPTIONS);
        if (forceOptions != null) {
            boolean supportsSmartAlarm = this.coordinator.supportsSmartAlarm();
            boolean supportsWearLocation = this.coordinator.supportsWearLocation();
            boolean supportsHeartRate = this.coordinator.supportsHeartRate();
            boolean supportsSpO2 = this.coordinator.supportsSPo2();
            forceOptions.setVisible(!supportsSmartAlarm || !supportsWearLocation || !supportsHeartRate || !supportsSpO2);
            final SwitchPreferenceCompat forceSmartAlarm = handler.findPreference(PREF_FORCE_ENABLE_SMART_ALARM);
            forceSmartAlarm.setVisible(!supportsSmartAlarm);
            final SwitchPreferenceCompat forceWearLocation = handler.findPreference(PREF_FORCE_ENABLE_WEAR_LOCATION);
            forceWearLocation.setVisible(!supportsWearLocation);
            final SwitchPreferenceCompat forceHeartRate = handler.findPreference(PREF_FORCE_ENABLE_HEARTRATE_SUPPORT);
            forceHeartRate.setVisible(!supportsHeartRate);
            final SwitchPreferenceCompat forceSpO2 = handler.findPreference(PREF_FORCE_ENABLE_SPO2_SUPPORT);
            forceSpO2.setVisible(!supportsSpO2);
        }

        final SwitchPreferenceCompat sleepBreath = handler.findPreference(PREF_HUAWEI_SLEEP_BREATH);
        if(sleepBreath != null && !coordinator.supportsSleepBreath()) {
            sleepBreath.setVisible(false);
        }


        final SwitchPreferenceCompat reparseWorkout = handler.findPreference("huawei_reparse_workout_data");
        if (reparseWorkout != null) {
            reparseWorkout.setVisible(false);
            if (this.coordinator.supportsWorkouts())
                reparseWorkout.setVisible(true);
        }

        final Preference stressCalibrate = handler.findPreference("pref_huawei_stress_perform_calibrate");
        if (stressCalibrate != null) {
            stressCalibrate.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(handler.getContext(), HuaweiStressCalibrationActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, handler.getDevice());
                handler.getContext().startActivity(intent);
                return true;
            });
        }

        // Huawei devices do not support lookahead > 7 days
        final Preference calendarLookahead = handler.findPreference(DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS);
        if (calendarLookahead != null) {
            calendarLookahead.setVisible(false);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }


    public static final Creator<HuaweiSettingsCustomizer> CREATOR= new Creator<HuaweiSettingsCustomizer>() {

        @Override
        public HuaweiSettingsCustomizer createFromParcel(Parcel parcel) {
            final GBDevice device = parcel.readParcelable(HuaweiSettingsCustomizer.class.getClassLoader());
            return new HuaweiSettingsCustomizer(device);
        }

        @Override
        public HuaweiSettingsCustomizer[] newArray(int i) {
            return new HuaweiSettingsCustomizer[0];
        }
    };
}
