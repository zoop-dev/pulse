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
package nodomain.freeyourgadget.gadgetbridge.activities.fit;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FitViewerActivity extends AbstractGBActivity implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FitViewerActivity.class);

    public static final String EXTRA_PATH = "path";

    private FitRecordAdapter fitRecordAdapter;
    private FitFile fitFile;
    private final Set<GlobalFITMessage> filter = new HashSet<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_viewer);
        addMenuProvider(this);

        if (!getIntent().hasExtra(EXTRA_PATH)) {
            GB.toast("Missing path", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        final RecyclerView fileListView = findViewById(R.id.fitRecordView);
        fileListView.setLayoutManager(new LinearLayoutManager(this));

        final File fitPath = new File(Objects.requireNonNull(getIntent().getStringExtra(EXTRA_PATH)));
        if (!fitPath.isFile() || !fitPath.canRead()) {
            GB.toast("Unable to read fit file", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(fitPath.getName());
        }

        try {
            fitFile = FitFile.parseIncoming(fitPath);
        } catch (final IOException | FitParseException e) {
            GB.toast("Failed to parse fit file", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Failed to parse fit file", e);
            finish();
            return;
        }

        fitRecordAdapter = new FitRecordAdapter(this, fitFile);

        fileListView.setAdapter(fitRecordAdapter);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_fit_viewer, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.fit_viewer_filter) {
            final GlobalFITMessage[] globals = fitFile.getRecords().stream()
                    .map(RecordData::getGlobalFITMessage)
                    .distinct()
                    .sorted((a, b) -> {
                        if (a.name().startsWith("UNK_") && b.name().startsWith("UNK_")) {
                            return Integer.compare(a.getNumber(), b.getNumber());
                        } else {
                            return a.name().compareToIgnoreCase(b.name());
                        }
                    })
                    .toArray(GlobalFITMessage[]::new);

            final boolean[] checked = new boolean[globals.length];
            for (int i = 0; i < globals.length; i++) {
                if (filter.contains(globals[i])) {
                    checked[i] = true;
                }
            }

            final CharSequence[] mEntries = Arrays.stream(globals)
                    .map(GlobalFITMessage::name)
                    .toArray(CharSequence[]::new);

            new MaterialAlertDialogBuilder(this)
                    .setCancelable(true)
                    .setTitle(R.string.filter_mode)
                    .setMultiChoiceItems(mEntries, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        filter.clear();
                        for (int i = 0; i < globals.length; i++) {
                            if (checked[i]) {
                                filter.add(globals[i]);
                            }
                        }
                        fitRecordAdapter.updateFilter(filter);
                        fitRecordAdapter.notifyDataSetChanged();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    })
                    .setNeutralButton(R.string.reset, (dialog, which) -> {
                        filter.clear();
                        fitRecordAdapter.updateFilter(filter);
                        fitRecordAdapter.notifyDataSetChanged();
                    })
                    .show();
            return true;
        }
        return false;
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
