package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Parcel;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class QHybridSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private Preference timeOffsetPref;
    private Preference timeZoneOffsetPref;

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        handler.addPreferenceHandlerFor("use_activity_hand_as_notification_counter");

        final Preference pref = handler.findPreference("pref_key_qhybrid_legacy");
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(handler.getContext(), QHybridConfigActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, handler.getDevice());
                handler.getContext().startActivity(intent);
                return true;
            });
        }

        timeOffsetPref = handler.findPreference("time_offset");
        if (timeOffsetPref != null) {
            timeOffsetPref.setOnPreferenceClickListener(preference -> {
                int timeOffset = prefs.getInt("QHYBRID_TIME_OFFSET", 0);
                LinearLayout layout2 = new LinearLayout(handler.getContext());
                layout2.setOrientation(LinearLayout.HORIZONTAL);

                final NumberPicker hourPicker = new NumberPicker(handler.getContext());
                hourPicker.setMinValue(0);
                hourPicker.setMaxValue(23);
                hourPicker.setValue(timeOffset / 60);

                final NumberPicker minPicker = new NumberPicker(handler.getContext());
                minPicker.setMinValue(0);
                minPicker.setMaxValue(59);
                minPicker.setValue(timeOffset % 60);

                layout2.addView(hourPicker);
                TextView tw = new TextView(handler.getContext());
                tw.setText(":");
                layout2.addView(tw);
                layout2.addView(minPicker);

                layout2.setGravity(Gravity.CENTER);

                new MaterialAlertDialogBuilder(handler.getContext())
                        .setTitle(handler.getContext().getString(R.string.qhybrid_offset_time_by))
                        .setView(layout2)
                        .setPositiveButton(handler.getContext().getString(R.string.ok), (dialogInterface, i) -> {
                            int value = hourPicker.getValue() * 60 + minPicker.getValue();
                            prefs.getPreferences().edit().putInt("QHYBRID_TIME_OFFSET", value).apply();
                            updateTimeOffsetSummary(value);
                            LocalBroadcastManager.getInstance(handler.getContext()).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE));
                            GB.toast(handler.getContext().getString(R.string.qhybrid_changes_delay_prompt), Toast.LENGTH_SHORT, GB.INFO);
                        })
                        .setNegativeButton(handler.getContext().getString(R.string.fossil_hr_new_action_cancel), null)
                        .show();
                return true;
            });
            updateTimeOffsetSummary(prefs.getInt("QHYBRID_TIME_OFFSET", 0));
        }

        timeZoneOffsetPref = handler.findPreference("second_tz_offset");
        if (timeZoneOffsetPref != null) {
            timeZoneOffsetPref.setOnPreferenceClickListener(preference -> {
                int timeOffset = prefs.getInt("QHYBRID_TIMEZONE_OFFSET", 0);
                LinearLayout layout2 = new LinearLayout(handler.getContext());
                layout2.setOrientation(LinearLayout.HORIZONTAL);

                final NumberPicker hourPicker = new NumberPicker(handler.getContext());
                hourPicker.setMinValue(0);
                hourPicker.setMaxValue(23);
                hourPicker.setValue(timeOffset / 60);

                final NumberPicker minPicker = new NumberPicker(handler.getContext());
                minPicker.setMinValue(0);
                minPicker.setMaxValue(59);
                minPicker.setValue(timeOffset % 60);

                layout2.addView(hourPicker);
                TextView tw = new TextView(handler.getContext());
                tw.setText(":");
                layout2.addView(tw);
                layout2.addView(minPicker);

                layout2.setGravity(Gravity.CENTER);

                new MaterialAlertDialogBuilder(handler.getContext())
                        .setTitle(handler.getContext().getString(R.string.qhybrid_offset_timezone))
                        .setView(layout2)
                        .setPositiveButton(handler.getContext().getString(R.string.ok), (dialogInterface, i) -> {
                            int value = hourPicker.getValue() * 60 + minPicker.getValue();
                            prefs.getPreferences().edit().putInt("QHYBRID_TIMEZONE_OFFSET", value).apply();
                            updateTimezoneOffsetSummary(value);
                            LocalBroadcastManager.getInstance(handler.getContext()).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE_TIMEZONE));
                            GB.toast(handler.getContext().getString(R.string.qhybrid_changes_delay_prompt), Toast.LENGTH_SHORT, GB.INFO);
                        })
                        .setNegativeButton(handler.getContext().getString(R.string.fossil_hr_new_action_cancel), null)
                        .show();
                return true;
            });
            updateTimezoneOffsetSummary(prefs.getInt("QHYBRID_TIMEZONE_OFFSET", 0));
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<QHybridSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public QHybridSettingsCustomizer createFromParcel(final Parcel in) {
            return new QHybridSettingsCustomizer();
        }

        @Override
        public QHybridSettingsCustomizer[] newArray(final int size) {
            return new QHybridSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
    }

    private void updateTimeOffsetSummary(int timeOffset) {
        DecimalFormat format = new DecimalFormat("00");
        timeOffsetPref.setSummary(
                format.format(timeOffset / 60) + ":" +
                        format.format(timeOffset % 60)
        );
    }

    private void updateTimezoneOffsetSummary(int timeZoneOffset) {
        DecimalFormat format = new DecimalFormat("00");
        timeZoneOffsetPref.setSummary(
                format.format(timeZoneOffset / 60) + ":" +
                        format.format(timeZoneOffset % 60)
        );
    }
}
