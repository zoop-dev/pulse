/*  Copyright (C) 2025 Me7c7

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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.adapter.NotificationsAppIconAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class NotificationsAppIconUploadActivity extends AbstractGBActivity {

    protected GBDevice mGBDevice = null;

    private NotificationsAppIconAdapter appsAdapter = null;

    private RecyclerView appsListView;

    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notifications_app_icon_upload);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        if (mGBDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        Button uploadToDevice = findViewById(R.id.notifications_app_icon_send_to_device_button);

        loadingView = findViewById(R.id.notifications_app_icon_apps_loading);

        appsListView = findViewById(R.id.notifications_app_icon_apps_list);
        appsListView.setLayoutManager(new LinearLayoutManager(this));

        loadingView.setVisibility(View.VISIBLE);

        loadAppsList();

        uploadToDevice.setOnClickListener(view -> {
            List<String> selected = appsAdapter.getSelectedItems();
            if(selected.isEmpty()) {
                Snackbar.make(appsListView, getString(R.string.notifications_app_icon_nothing_selected), Snackbar.LENGTH_LONG).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.notifications_app_icon_upload_to_device)
                    .setMessage(this.getString(R.string.notifications_app_icon_uploading_confirm_description, selected.size()))
                    .setIcon(R.drawable.ic_info)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        HashSet<String> iconsToUpload = new HashSet<>(appsAdapter.getSelectedItems());
                        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(mGBDevice.getAddress());
                        SharedPreferences.Editor editor = prefs.edit();
                        Prefs.putStringSet(editor, DeviceSettingsPreferenceConst.PREF_UPLOAD_NOTIFICATIONS_APP_ICON, iconsToUpload);
                        editor.apply();
                        GBApplication.deviceService(mGBDevice).onSendConfiguration(DeviceSettingsPreferenceConst.PREF_UPLOAD_NOTIFICATIONS_APP_ICON);
                        Snackbar.make(appsListView, getString(R.string.notifications_app_icon_uploading), Snackbar.LENGTH_LONG).show();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });

        final SearchView searchView = findViewById(R.id.notifications_app_icon_send_to_device_search_view);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                if(appsAdapter != null) {
                    appsAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });

    }

    void loadAppsList() {
        new Thread(() -> {
            final Map<String,String> appNames = new HashMap<>();
            final List<String> apps = NotificationUtils.getAllApplications(GBApplication.getContext());
            for(String packageName: apps) {
                String appName = NotificationUtils.getApplicationLabel(GBApplication.getContext(), packageName);
                if(!TextUtils.isEmpty(appName)) {
                    appNames.put(packageName, appName);
                }
            }
            Collections.sort(apps, (i1, i2) -> {
                final String s1 = appNames.get(i1);
                final String s2 = appNames.get(i2);
                if(s1 != null && s2 != null)
                    return s1.compareToIgnoreCase(s2);
                return 0;
            });
            runOnUiThread(() -> {
                SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(mGBDevice.getAddress());
                HashSet<String> iconsToUpload = (HashSet<String>) prefs.getStringSet(DeviceSettingsPreferenceConst.PREF_UPLOAD_NOTIFICATIONS_APP_ICON, null);
                if (iconsToUpload == null) {
                    iconsToUpload = new HashSet<>();
                }
                appsAdapter = new NotificationsAppIconAdapter(getApplicationContext(), apps, new ArrayList<>(iconsToUpload));
                appsListView.setAdapter(appsAdapter);
                loadingView.setVisibility(View.GONE);
            });
        }).start();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // Simulate a back press, so that we don't actually exit the activity when
            // in a nested PreferenceScreen
            this.getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
