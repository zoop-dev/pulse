/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.aawireless;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.adapter.SimpleIconListAdapter;
import nodomain.freeyourgadget.gadgetbridge.model.RunnableListIconItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.CountryCodeTextWatcher;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MinMaxTextWatcher;

public class AAWirelessSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private static final String PREF_DUMMY_PHONE = "pref_aawireless_dummy_phone_";

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {

    }

    @Override
    public void onDeviceChanged(final DeviceSpecificSettingsHandler handler) {
        final PreferenceCategory pairedPhonesHeader = handler.findPreference(AAWirelessPrefs.PREF_HEADER_PAIRED_PHONES);
        if (pairedPhonesHeader != null) {
            setupPhoneManagement(handler, new AAWirelessPrefs(GBApplication.getDevicePrefs(handler.getDevice()).getPreferences(), handler.getDevice()));
        }

        setEnabledPreferences(handler);
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs genericDevicePrefs, final String rootKey) {
        final AAWirelessPrefs prefs = new AAWirelessPrefs(genericDevicePrefs.getPreferences(), handler.getDevice());

        // Paired phone management
        if (AAWirelessPrefs.PREF_SCREEN_PAIRED_PHONES.equals(rootKey)) {
            setupPhoneManagement(handler, prefs);
            return;
        }

        // Country
        handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_COUNTRY);
        final EditTextPreference country = handler.findPreference(DeviceSettingsPreferenceConst.PREF_COUNTRY);
        if (country != null) {
            country.setOnBindEditTextListener(editText -> {
                editText.addTextChangedListener(new CountryCodeTextWatcher(editText));
                editText.setSelection(editText.getText().length());
            });
        }

        // Auto-standby
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_AUTO_STANDBY_ENABLED);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_AUTO_STANDBY_DEVICE);

        // Button modes
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK);
        appendKnownPhones(handler.findPreference(AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK), handler.getContext(), prefs);
        appendKnownPhones(handler.findPreference(AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK), handler.getContext(), prefs);

        // Wi-Fi frequency and channel
        final String currentFrequency = prefs.getString(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY, "5");
        final ListPreference wifiChannel24 = handler.findPreference(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4);
        if (wifiChannel24 != null) {
            wifiChannel24.setVisible("2.4".equals(currentFrequency));
        }
        final ListPreference wifiChannel5 = handler.findPreference(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5);
        if (wifiChannel5 != null) {
            wifiChannel5.setVisible("5".equals(currentFrequency));
        }
        handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY, (preference, newValue) -> {
            if (wifiChannel24 != null) {
                wifiChannel24.setVisible("2.4".equals(newValue));
            }
            if (wifiChannel5 != null) {
                wifiChannel5.setVisible("5".equals(newValue));
            }
            return true;
        });
        handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4);
        handler.addPreferenceHandlerFor(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5);

        // Advanced settings
        DeviceSettingsUtils.addConfirmablePreferenceHandlerFor(
                handler,
                AAWirelessPrefs.PREF_DONGLE_MODE,
                R.string.pref_aawireless_dongle_confirmation
        );
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_PASSTHROUGH);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_AUDIO_STUTTER_FIX);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_DPI);
        final EditTextPreference dpi = handler.findPreference(AAWirelessPrefs.PREF_DPI);
        if (dpi != null) {
            dpi.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new MinMaxTextWatcher(editText, 0, 300, false));
                editText.setSelection(editText.getText().length());
            });
        }
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_DISABLE_MEDIA_SINK);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_DISABLE_TTS_SINK);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_REMOVE_TAP_RESTRICTION);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_VAG_CRASH_FIX);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_START_FIX);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_DEVELOPER_MODE);
        handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_AUTO_VIDEO_FOCUS);

        setEnabledPreferences(handler);
    }

    private static void setEnabledPreferences(final DeviceSpecificSettingsHandler handler) {
        final boolean initialized = handler.getDevice().isInitialized();

        setEnabled(handler.findPreference(DeviceSettingsPreferenceConst.PREF_COUNTRY), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_AUTO_STANDBY_ENABLED), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_AUTO_STANDBY_DEVICE), initialized);
        setEnabled(handler.findPreference(DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY), initialized);
        setEnabled(handler.findPreference(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4), initialized);
        setEnabled(handler.findPreference(DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_DONGLE_MODE), initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_PASSTHROUGH), initialized);
        final SwitchPreferenceCompat passthroughPref = handler.findPreference(AAWirelessPrefs.PREF_PASSTHROUGH);
        final boolean passthrough = passthroughPref != null && passthroughPref.isChecked();
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_AUDIO_STUTTER_FIX), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_DPI), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_DISABLE_MEDIA_SINK), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_DISABLE_TTS_SINK), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_REMOVE_TAP_RESTRICTION), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_VAG_CRASH_FIX), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_START_FIX), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_DEVELOPER_MODE), !passthrough && initialized);
        setEnabled(handler.findPreference(AAWirelessPrefs.PREF_AUTO_VIDEO_FOCUS), !passthrough && initialized);
    }

    private static void setEnabled(final Preference pref, final boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled);
        }
    }

    private static void setupPhoneManagement(final DeviceSpecificSettingsHandler handler, final AAWirelessPrefs prefs) {
        final Context context = handler.getContext();
        final int phonesCount = prefs.getPairedPhoneCount();

        final Preference preferLastConnected = handler.findPreference(AAWirelessPrefs.PREF_PREFER_LAST_CONNECTED);
        if (preferLastConnected != null) {
            handler.addPreferenceHandlerFor(AAWirelessPrefs.PREF_PREFER_LAST_CONNECTED);
            preferLastConnected.setEnabled(handler.getDevice().isInitialized());
            preferLastConnected.setVisible(!prefs.enableDongleMode());
        }
        final PreferenceCategory pairedPhonesHeader = handler.findPreference(AAWirelessPrefs.PREF_HEADER_PAIRED_PHONES);
        final Preference noPairedPhones = handler.findPreference(AAWirelessPrefs.PREF_NO_PAIRED_PHONES);
        if (noPairedPhones != null) {
            noPairedPhones.setVisible(phonesCount == 0);
        }

        for (int i = 0; i < pairedPhonesHeader.getPreferenceCount(); i++) {
            final Preference preference = pairedPhonesHeader.getPreference(i);
            if (preference.getKey().startsWith(PREF_DUMMY_PHONE)) {
                pairedPhonesHeader.removePreference(preference);
                i--;
            }
        }

        for (int i = 0; i < phonesCount; i++) {
            final int phonePosition = i;
            final Preference prefPhone = new Preference(context);
            final String phoneName = prefs.getPairedPhoneName(i);
            final String phoneMac = prefs.getPairedPhoneMac(i);
            prefPhone.setOnPreferenceClickListener(preference -> {
                final List<RunnableListIconItem> items = new ArrayList<>(4);

                if (!prefs.enableDongleMode()) {
                    items.add(new RunnableListIconItem(context.getString(R.string.switch_to_phone, phoneName), R.drawable.ic_switch_left, () -> {
                        final Intent intent = new Intent(AAWirelessPrefs.ACTION_PHONE_SWITCH);
                        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, phoneMac);
                        intent.setPackage(BuildConfig.APPLICATION_ID);
                        context.sendBroadcast(intent);
                    }));
                }

                if (phonePosition > 0) {
                    items.add(new RunnableListIconItem(context.getString(R.string.widget_move_up), R.drawable.ic_arrow_upward, () -> {
                        final Intent intent = new Intent(AAWirelessPrefs.ACTION_PHONE_SORT);
                        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, phoneMac);
                        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_NEW_POSITION, phonePosition - 1);
                        intent.setPackage(BuildConfig.APPLICATION_ID);
                        context.sendBroadcast(intent);
                    }));
                }

                if (phonePosition < phonesCount - 1) {
                    items.add(new RunnableListIconItem(context.getString(R.string.widget_move_down), R.drawable.ic_arrow_downward, () -> {
                        final Intent intent = new Intent(AAWirelessPrefs.ACTION_PHONE_SORT);
                        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, phoneMac);
                        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_NEW_POSITION, phonePosition + 1);
                        intent.setPackage(BuildConfig.APPLICATION_ID);
                        context.sendBroadcast(intent);
                    }));
                }

                items.add(new RunnableListIconItem(context.getString(R.string.Delete), R.drawable.ic_delete, () -> {
                    final Intent intent = new Intent(AAWirelessPrefs.ACTION_PHONE_DELETE);
                    intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, phoneMac);
                    intent.setPackage(BuildConfig.APPLICATION_ID);
                    context.sendBroadcast(intent);
                }));

                final SimpleIconListAdapter adapter = new SimpleIconListAdapter(context, items);

                new MaterialAlertDialogBuilder(context)
                        .setAdapter(adapter, (dialog, i1) -> items.get(i1).getAction().run())
                        .setTitle(phoneName)
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i1) -> {
                        })
                        .create()
                        .show();
                return true;
            });

            prefPhone.setKey(PREF_DUMMY_PHONE + i);
            prefPhone.setIcon(R.drawable.ic_smartphone);
            prefPhone.setTitle(phoneName);
            prefPhone.setSummary(phoneMac);
            prefPhone.setEnabled(handler.getDevice().isInitialized());

            pairedPhonesHeader.addPreference(prefPhone);
        }
    }

    private static void appendKnownPhones(final ListPreference listPreference, final Context context, final AAWirelessPrefs prefs) {
        if (listPreference == null) {
            return;
        }

        final int phonesCount = prefs.getPairedPhoneCount();
        if (phonesCount <= 0) {
            return;
        }

        final CharSequence[] entries = listPreference.getEntries();
        final CharSequence[] entryValues = listPreference.getEntryValues();
        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            return;
        }

        final CharSequence[] newEntries = new CharSequence[entries.length + phonesCount];
        final CharSequence[] newEntryValues = new CharSequence[entries.length + phonesCount];

        for (int i = 0; i < entries.length; i++) {
            newEntries[i] = entries[i];
            newEntryValues[i] = entryValues[i];
        }
        for (int i = 0; i < phonesCount; i++) {
            final String mac = prefs.getPairedPhoneMac(i);
            final String name = prefs.getPairedPhoneName(i);
            newEntries[entries.length + i] = context.getString(R.string.switch_to_phone, name);
            newEntryValues[entries.length + i] = mac;
        }

        listPreference.setEntries(newEntries);
        listPreference.setEntryValues(newEntryValues);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<AAWirelessSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public AAWirelessSettingsCustomizer createFromParcel(final Parcel in) {
            return new AAWirelessSettingsCustomizer();
        }

        @Override
        public AAWirelessSettingsCustomizer[] newArray(final int size) {
            return new AAWirelessSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel parcel, final int i) {

    }
}
