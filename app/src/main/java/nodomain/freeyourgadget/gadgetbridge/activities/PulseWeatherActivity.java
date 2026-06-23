/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWeather;

/** Pulse: weather settings — location source + manual city + manual refresh. */
public class PulseWeatherActivity extends AbstractSettingsActivityV2 {
    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new PulseWeatherFragment();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static class PulseWeatherFragment extends AbstractPreferenceFragment {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.pulse_weather_preferences, rootKey);

            final EditTextPreference city = findPreference("pulse_weather_city");
            if (city != null) {
                city.setOnPreferenceChangeListener((preference, newValue) -> {
                    geocodeCity(String.valueOf(newValue));
                    return true;
                });
            }

            final Preference refresh = findPreference("pulse_weather_refresh");
            if (refresh != null) {
                refresh.setOnPreferenceClickListener(p -> {
                    new Thread(() -> PulseWeather.fetchAndSend(requireContext().getApplicationContext()),
                            "pulse-weather-manual").start();
                    GB.toast(requireContext(), getString(R.string.pulse_weather_refreshing), Toast.LENGTH_SHORT, GB.INFO);
                    return true;
                });
            }
        }

        /** Resolve a typed city name into stored lat/lon for the fetcher to use. */
        private void geocodeCity(final String cityName) {
            if (cityName == null || cityName.trim().isEmpty()) {
                return;
            }
            new Thread(() -> {
                try {
                    final Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    final List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        final Address a = addresses.get(0);
                        GBApplication.getPrefs().getPreferences().edit()
                                .putString("pulse_weather_lat", String.valueOf(a.getLatitude()))
                                .putString("pulse_weather_lon", String.valueOf(a.getLongitude()))
                                .apply();
                        PulseWeather.fetchAndSend(requireContext().getApplicationContext());
                    }
                } catch (final Exception ignored) {
                }
            }, "pulse-weather-geocode").start();
        }
    }
}
