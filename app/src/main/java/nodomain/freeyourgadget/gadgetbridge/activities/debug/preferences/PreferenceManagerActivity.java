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
package nodomain.freeyourgadget.gadgetbridge.activities.debug.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.backup.JsonBackupPreferences;

public class PreferenceManagerActivity extends AbstractGBActivity implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PreferenceManagerActivity.class);

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_TITLE = "title";

    private SearchView searchView;
    private String sharedPreferencesName;
    private final List<DebugPreference> allPrefs = new ArrayList<>();
    private PreferenceManagerAdapter listAdapter;

    private final ActivityResultLauncher<String> exportFileChooser = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                LOG.info("Got target export file: {}", uri);
                if (uri != null) {
                    exportPreferences(uri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> importFileChooser = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                LOG.info("Got import file: {}", uri);
                if (uri != null) {
                    importPreferences(uri);
                }
            }
    );

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_preferences);
        addMenuProvider(this);

        final RecyclerView preferenceListView = findViewById(R.id.preferenceListView);
        preferenceListView.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferencesName = getIntent().getStringExtra(EXTRA_NAME);
        if (sharedPreferencesName == null) {
            GB.toast("No preferences name!", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        final String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (actionBar != null) {
            actionBar.setTitle(title != null ? title : sharedPreferencesName);
        }

        listAdapter = new PreferenceManagerAdapter(this, allPrefs);
        preferenceListView.setAdapter(listAdapter);

        searchView = findViewById(R.id.preferenceListSearchView);
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchView.setIconifiedByDefault(false);
        searchView.setVisibility(View.GONE);
        searchView.setIconified(false);
        searchView.setQuery("", false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                listAdapter.getFilter().filter(newText);
                return true;
            }
        });

        reloadPreferences();
    }

    @SuppressLint("NotifyDataSetChanged")  // the entire list will be reloaded
    private void reloadPreferences() {
        allPrefs.clear();
        final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> e : sharedPreferences.getAll().entrySet()) {
            allPrefs.add(new DebugPreference(
                    e.getKey(),
                    e.getValue().toString()
            ));
        }
        LOG.debug("Got {} preferences", allPrefs.size());
        allPrefs.sort((a, b) -> a.key().compareToIgnoreCase(b.key()));
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_debug_preferences, menu);
        if (!sharedPreferencesName.startsWith("devicesettings_")) {
            menu.findItem(R.id.debug_preferences_reset).setVisible(false);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
        final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.debug_preferences_search) {
            searchView.setVisibility(View.VISIBLE);
            searchView.requestFocus();
            searchView.setIconified(true);
            searchView.setIconified(false);
            return true;
        } else if (itemId == R.id.debug_preferences_export) {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            final String defaultFilename = String.format(Locale.ROOT, "%s_%s.json", sharedPreferencesName, sdf.format(new Date()))
                    .replace(":", "_")
                    .toLowerCase(Locale.ROOT);
            exportFileChooser.launch(defaultFilename);
            return true;
        } else if (itemId == R.id.debug_preferences_import) {
            importFileChooser.launch(new String[]{"application/json"});
            return true;
        } else if (itemId == R.id.debug_preferences_reset) {
            if (!sharedPreferencesName.startsWith("devicesettings_")) {
                LOG.warn("Preventing non device settings clear");
                return true;
            }
            new MaterialAlertDialogBuilder(this)
                    .setCancelable(true)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.debugactivity_confirm_remove_device_preferences_title)
                    .setMessage(R.string.debugactivity_confirm_remove_device_preferences)
                    .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        sharedPreferences.edit().clear().commit();
                        reloadPreferences();
                    }).show();

            return true;
        }
        return false;
    }

    private void exportPreferences(final Uri uri) {
        try {
            final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
            final JsonBackupPreferences jsonBackupPreferences = JsonBackupPreferences.exportFrom(sharedPreferences);
            final String preferencesJson = jsonBackupPreferences.toJson();

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    GB.toast(this, "Failed to open output file", Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                outputStream.write(preferencesJson.getBytes(StandardCharsets.UTF_8));
                GB.toast(this, getString(R.string.export_success), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException e) {
            LOG.error("Error exporting preferences", e);
            GB.toast(this, "Error exporting preferences: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    private void importPreferences(final Uri uri) {
        try {
            final JsonBackupPreferences jsonBackupPreferences;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                jsonBackupPreferences = JsonBackupPreferences.fromJson(is);
            }

            final SharedPreferences sharedPreferences = getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
            jsonBackupPreferences.importInto(sharedPreferences);

            reloadPreferences();
            GB.toast(this, getString(R.string.import_success), Toast.LENGTH_SHORT, GB.INFO);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Error importing preferences", e);
            GB.toast(this, "Error importing preferences: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
