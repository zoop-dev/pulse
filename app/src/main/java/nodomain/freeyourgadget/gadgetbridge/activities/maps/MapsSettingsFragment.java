/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.maps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager;

public class MapsSettingsFragment extends AbstractPreferenceFragment {
    static final String FRAGMENT_TAG = "MAP_SETTINGS_FRAGMENT";

    public static final String ACTION_SETTING_CHANGE = "nodomain.freeyourgadget.gadgetbridge.maps.setting_change";

    @Override
    public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
        setPreferencesFromResource(R.xml.map_settings, rootKey);

        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        if (prefs == null) {
            requireActivity().finish();
            return;
        }

        final Preference prefDownload = Objects.requireNonNull(findPreference("maps_download"));
        prefDownload.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/")
            ));
            return true;
        });

        final Preference prefFolder = Objects.requireNonNull(findPreference(MapsManager.PREF_MAPS_FOLDER));
        final ActivityResultLauncher<Uri> mapsFolderChooser = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                localUri -> {
                    LOG.info("Maps folder: {}", localUri);
                    if (localUri != null) {
                        requireContext().getContentResolver().takePersistableUriPermission(localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        prefs.edit()
                                .putString(MapsManager.PREF_MAPS_FOLDER, localUri.toString())
                                .apply();
                        prefFolder.setSummary(localUri.toString());
                        broadcastPreferenceChange();
                    }
                }
        );
        final String currentFolder = prefs.getString(MapsManager.PREF_MAPS_FOLDER, "");
        prefFolder.setSummary(currentFolder);
        prefFolder.setOnPreferenceClickListener(preference -> {
            mapsFolderChooser.launch(null);
            return true;
        });

        final Preference prefTrackColor = Objects.requireNonNull(findPreference(MapsManager.PREF_TRACK_COLOR));
        prefTrackColor.setOnPreferenceChangeListener((preference, newValue) -> {
            broadcastPreferenceChange();
            return true;
        });
    }

    private void broadcastPreferenceChange() {
        final Intent intent = new Intent();
        intent.setAction(ACTION_SETTING_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
    }
}
