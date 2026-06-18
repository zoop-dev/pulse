package nodomain.freeyourgadget.gadgetbridge.devices.soundbrenner;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Settings customizer for the Soundbrenner Core.
 *
 * Shows/hides per-beat accent SeekBars depending on the selected time-signature
 * numerator. Only beats 1..numerator are visible; the rest are hidden.
 *
 * All preference keys are referenced via {@link SoundbrennerConstants}.
 */
public class SoundbrennerSettingsCustomizer implements DeviceSpecificSettingsCustomizer {

    // -------------------------------------------------------------------------
    // Parcelable boilerplate
    // -------------------------------------------------------------------------

    public static final Creator<SoundbrennerSettingsCustomizer> CREATOR =
            new Creator<SoundbrennerSettingsCustomizer>() {
                @Override
                public SoundbrennerSettingsCustomizer createFromParcel(final Parcel in) {
                    return new SoundbrennerSettingsCustomizer(in);
                }

                @Override
                public SoundbrennerSettingsCustomizer[] newArray(final int size) {
                    return new SoundbrennerSettingsCustomizer[size];
                }
            };

    public SoundbrennerSettingsCustomizer() {}

    protected SoundbrennerSettingsCustomizer(final Parcel in) {}

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {}

    // -------------------------------------------------------------------------
    // DeviceSpecificSettingsCustomizer
    // -------------------------------------------------------------------------

    /**
     * Called after any preference in this screen changes and has been persisted.
     * Re-evaluates beat-bar visibility when the time signature is updated.
     *
     * The new value is read directly from the ListPreference object because
     * DeviceSpecificSettingsHandler does not expose a getPrefs() accessor.
     */
     @Override
         public void onPreferenceChange(final Preference preference,
                                        final DeviceSpecificSettingsHandler handler) {
             if (SoundbrennerConstants.PREF_TIME_SIGNATURE.equals(preference.getKey())
                     && preference instanceof ListPreference) {
                 final String timeSig = ((ListPreference) preference).getValue();
                 updateBeatAccentVisibility(handler, parseNumerator(timeSig));
             }

             // Trigger onSendConfiguration() in SoundbrennerSupport so the BLE
             // config packet is written to the device whenever any preference changes.
             handler.notifyPreferenceChanged(preference.getKey());
    }

    /**
     * Called once when the settings screen is first built.
     * Registers the change handler and sets initial beat-bar visibility from
     * the {@code prefs} snapshot that Gadgetbridge passes in.
     */
    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler,
                                  final Prefs prefs,
                                  final String rootKey) {
        // Register so onPreferenceChange fires for ALL Soundbrenner preferences.
        handler.addPreferenceHandlerFor(SoundbrennerConstants.PREF_TIME_SIGNATURE);
        handler.addPreferenceHandlerFor(SoundbrennerConstants.PREF_BPM);
        handler.addPreferenceHandlerFor(SoundbrennerConstants.PREF_SUBDIVISION);
        for (final String beatKey : SoundbrennerConstants.PREF_BEATS) {
            handler.addPreferenceHandlerFor(beatKey);
        }

        // Apply initial visibility from the persisted time-signature value.
        final String timeSig = prefs.getString(
                SoundbrennerConstants.PREF_TIME_SIGNATURE,
                SoundbrennerConstants.DEFAULT_TIME_SIG);
        updateBeatAccentVisibility(handler, parseNumerator(timeSig));

        // Connect Start Stop Button
        Preference togglePref = handler.findPreference("soundbrenner_metronome_toggle");
        if (togglePref != null) {
            togglePref.setOnPreferenceClickListener(preference -> {
                // Kommando via GBDeviceEventConfigurationUpdate an den Support schicken
                GBApplication.deviceService(handler.getDevice())
                        .onSendConfiguration(SoundbrennerConstants.PREF_METRONOME_RUNNING + "_toggle");
                return true;
            });
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Show SeekBars for beats 1..numerator, hide beats (numerator+1)..8.
     */
    private void updateBeatAccentVisibility(final DeviceSpecificSettingsHandler handler,
                                            final int numerator) {
        for (int i = 0; i < SoundbrennerConstants.PREF_BEATS.length; i++) {
            final Preference pref = handler.findPreference(SoundbrennerConstants.PREF_BEATS[i]);
            if (pref != null) {
                pref.setVisible(i < numerator);
            }
        }
    }

    /**
     * Parse the numerator from a string like "4/4" or "3/8".
     * Returns 4 on any parse error.
     */
    private int parseNumerator(final String timeSig) {
        try {
            if (timeSig != null && timeSig.contains("/")) {
                return Integer.parseInt(timeSig.split("/")[0]);
            }
        } catch (final NumberFormatException ignored) {}
        return 4;
    }
}
