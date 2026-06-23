/*  Copyright (C) 2015-2024 0nse, Andreas Shimokawa, Anemograph, Arjan
    Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Felix Konstantin
    Maurer, José Rebelo, Martin, Normano64, Pavel Elagin, Petr Vaněk, Sebastian
    Kranz, Taavi Eomäe

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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryPairingPreferenceActivity;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SettingsActivity extends AbstractSettingsActivityV2 {
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_UNIT_WEIGHT = "unit_weight";
    public static final String PREF_UNIT_TEMPERATURE = "unit_temperature";
    public static final String PREF_UNIT_DISTANCE = "unit_distance";

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new SettingsFragment();
    }

    @Override
    public void onSearchResultClicked(final SearchPreferenceResult result) {
        if (result.getResourceFile() == R.xml.dashboard_preferences) {
            open(DashboardPreferencesActivity.class, result);
        } else if (result.getResourceFile() == R.xml.about_user) {
            open(AboutUserPreferencesActivity.class, result);
        } else if (result.getResourceFile() == R.xml.charts_preferences) {
            open(ChartsPreferencesActivity.class, result);
        } else if (result.getResourceFile() == R.xml.discovery_pairing_preferences) {
            open(DiscoveryPairingPreferenceActivity.class, result);
        } else if (result.getResourceFile() == R.xml.notifications_preferences) {
            open(NotificationManagementActivity.class, result);
        } else {
            super.onSearchResultClicked(result);
        }
    }

    public static class SettingsFragment extends AbstractPreferenceFragment {
        private static final Logger LOG = LoggerFactory.getLogger(SettingsActivity.class);

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            index(R.xml.preferences);
            index(R.xml.dashboard_preferences, R.string.bottom_nav_dashboard);
            index(R.xml.about_user, R.string.activity_prefs_about_you);
            index(R.xml.charts_preferences, R.string.activity_prefs_charts);
            index(R.xml.discovery_pairing_preferences, R.string.activity_prefs_discovery_pairing);
            index(R.xml.notifications_preferences, R.string.pref_header_notifications);

            setInputTypeFor("location_latitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            setInputTypeFor("location_longitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED  | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            Prefs prefs = GBApplication.getPrefs();
            Preference pref = findPreference("pref_category_activity_personal");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), AboutUserPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_pulse_goals");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), PulseGoalsActivity.class));
                    return true;
                });
            }

            pref = findPreference("pref_charts");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), ChartsPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("datetime_synconconnect");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    if (Boolean.TRUE.equals(newVal)) {
                        TimeChangeReceiver.scheduleNextDstChangeOrPeriodicSync(requireContext());
                        GBApplication.deviceService().onSetTime();
                    }
                    return true;
                });
            }

            final SwitchPreferenceCompat logToFilePreference = findPreference("log_to_file");
            if (logToFilePreference != null) {
                logToFilePreference.setOnPreferenceChangeListener((preference, newVal) -> {
                    boolean doEnable = Boolean.TRUE.equals(newVal);
                    try {
                        if (doEnable) {
                            FileUtils.getExternalFilesDir(); // ensures that it is created
                        }
                        Logging.getInstance().setFileLoggingEnabled(doEnable);
                    } catch (IOException ex) {
                        GB.toast(requireContext().getApplicationContext(),
                                getString(R.string.error_creating_directory_for_logfiles, ex.getLocalizedMessage()),
                                Toast.LENGTH_LONG,
                                GB.ERROR,
                                ex);
                    }
                    return true;
                });

                // If we didn't manage to initialize file logging, disable the preference and show the button to initialize again
                if (!Logging.getInstance().isFileLoggerInitialized()) {
                    logToFilePreference.setEnabled(false);
                    logToFilePreference.setSummary(R.string.pref_write_logfiles_not_available);
                    final Preference logRestart = findPreference("log_restart");
                    if (logRestart != null) {
                        logRestart.setVisible(true);
                        logRestart.setOnPreferenceClickListener(preference -> {
                            Logging.getInstance().setFileLoggingEnabled(logToFilePreference.isChecked());
                            if (Logging.getInstance().isFileLoggerInitialized()) {
                                logToFilePreference.setEnabled(true);
                                logToFilePreference.setSummary(null);
                                logRestart.setVisible(false);

                            }
                            return true;
                        });
                    }
                }

                final SwitchPreferenceCompat logLevelTrace = findPreference("log_level_trace");
                logLevelTrace.setOnPreferenceChangeListener((preference, newVal) -> {
                    final boolean traceEnabled = Boolean.TRUE.equals(newVal);
                    Logging.getInstance().setTraceLogging(traceEnabled);
                    return true;
                });
            }

            pref = findPreference(PREF_LANGUAGE);
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    String newLang = newVal.toString();
                    try {
                        GBApplication.setLanguage(newLang);
                        requireActivity().recreate();
                        invokeLater(() -> GBApplication.deviceService().onSendConfiguration(PREF_LANGUAGE));
                    } catch (Exception ex) {
                        GB.toast(requireContext().getApplicationContext(),
                                "Error setting language: " + ex.getLocalizedMessage(),
                                Toast.LENGTH_LONG,
                                GB.ERROR,
                                ex);
                    }
                    return true;
                });
            }

            pref = findPreference("display_add_device_fab");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    sendThemeChangeIntent();
                    return true;
                });
            }
            pref = findPreference("display_bottom_navigation_bar");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    sendThemeChangeIntent();
                    return true;
                });
            }

            final Preference unitDistance = findPreference(PREF_UNIT_DISTANCE);
            if (unitDistance != null) {
                unitDistance.setOnPreferenceChangeListener((preference, newVal) -> {
                    invokeLater(() -> GBApplication.deviceService().onSendConfiguration(PREF_UNIT_DISTANCE));
                    return true;
                });
            }
            final Preference unitTemperature = findPreference(PREF_UNIT_TEMPERATURE);
            if (unitTemperature != null) {
                unitTemperature.setOnPreferenceChangeListener((preference, newVal) -> {
                    invokeLater(() -> GBApplication.deviceService().onSendConfiguration(PREF_UNIT_TEMPERATURE));
                    return true;
                });
            }
            final Preference unitWeight = findPreference(PREF_UNIT_WEIGHT);
            if (unitWeight != null) {
                unitWeight.setOnPreferenceChangeListener((preference, newVal) -> {
                    invokeLater(() -> GBApplication.deviceService().onSendConfiguration(PREF_UNIT_WEIGHT));
                    return true;
                });
            }

            pref = findPreference("location_aquire");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    if (ActivityCompat.checkSelfPermission(requireContext().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    }

                    LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    String provider = locationManager.getBestProvider(criteria, false);
                    if (provider != null) {
                        Location location = locationManager.getLastKnownLocation(provider);
                        if (location != null) {
                            setLocationPreferences(location);
                        } else {
                            locationManager.requestSingleUpdate(provider, new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {
                                    setLocationPreferences(location);
                                }

                                @Override
                                public void onStatusChanged(String provider, int status, Bundle extras) {
                                    LOG.info("provider status changed to " + status + " (" + provider + ")");
                                }

                                @Override
                                public void onProviderEnabled(String provider) {
                                    LOG.info("provider enabled (" + provider + ")");
                                }

                                @Override
                                public void onProviderDisabled(String provider) {
                                    LOG.info("provider disabled (" + provider + ")");
                                    GB.toast(requireContext(), getString(R.string.toast_enable_networklocationprovider), 3000, 0);
                                }
                            }, null);
                        }
                    } else {
                        LOG.warn("No location provider found, did you deny location permission?");
                    }
                    return true;
                });
            }

            pref = findPreference("pref_pulse_weather");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), PulseWeatherActivity.class));
                    return true;
                });
            }

            pref = findPreference("pref_category_dashboard");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), DashboardPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_category_notifications");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), NotificationManagementActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            final Preference theme = findPreference("pref_key_theme");
            final Preference amoled_black = findPreference("pref_key_theme_amoled_black");

            if (amoled_black != null) {
                String selectedTheme = prefs.getString("pref_key_theme", requireContext().getString(R.string.pref_theme_value_system));
                if (selectedTheme.equals("light"))
                    amoled_black.setEnabled(false);
                else
                    amoled_black.setEnabled(true);
                amoled_black.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        sendThemeChangeIntent();
                        return true;
                    }
                });
            }

            if (theme != null) {
                theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        final String val = newVal.toString();
                        if (amoled_black != null) {
                            if (val.equals("light"))
                                amoled_black.setEnabled(false);
                            else
                                amoled_black.setEnabled(true);
                        }
                        // Warn user if dynamic colors are not available
                        if (val.equals(requireContext().getString(R.string.pref_theme_value_dynamic)) && !DynamicColors.isDynamicColorAvailable()) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.pref_theme_dynamic_colors_not_available_warning)
                                    .setIcon(R.drawable.ic_warning)
                                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                                        sendThemeChangeIntent();
                                    })
                                    .show();
                        } else {
                            sendThemeChangeIntent();
                        }
                        return true;
                    }
                });
            }

            pref = findPreference("pulse_accent");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    // Re-apply the accent overlay across the app and refresh this screen.
                    sendThemeChangeIntent();
                    requireActivity().recreate();
                    return true;
                });
            }

            pref = findPreference("pref_discovery_pairing");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), DiscoveryPairingPreferenceActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }
        }

        /*
         * delayed execution so that the preferences are applied first
         */
        private void invokeLater(Runnable runnable) {
            getListView().post(runnable);
        }

        private void setLocationPreferences(Location location) {
            String latitude = String.format(Locale.US, "%.6g", location.getLatitude());
            String longitude = String.format(Locale.US, "%.6g", location.getLongitude());
            LOG.info("got location. Lat: {} Lng: {}", latitude, longitude);
            GB.toast(requireContext(), getString(R.string.toast_aqurired_networklocation), 2000, 0);
            GBApplication.getPrefs().getPreferences()
                    .edit()
                    .putString("location_latitude", latitude)
                    .putString("location_longitude", longitude)
                    .apply();
        }

        /**
         * Signal running activities that the theme has changed
         */
        private void sendThemeChangeIntent() {
            // Pulse: re-apply day/night so the palette flips immediately on theme change.
            GBApplication.applyPulseNightMode();
            Intent intent = new Intent();
            intent.setAction(GBApplication.ACTION_THEME_CHANGE);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        }
    }
}
