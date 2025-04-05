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
package nodomain.freeyourgadget.gadgetbridge.activities.audiorecordings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.database.repository.AudioRecordingsRepository;
import nodomain.freeyourgadget.gadgetbridge.entities.AudioRecording;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AudioRecordingsActivity extends AbstractGBActivity implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AudioRecordingsActivity.class);

    public static final String ACTION_FETCH_FINISH = "audio_recordings_fetch_finish";

    private GBDevice device;
    private SearchView searchView;
    private SwipeRefreshLayout refreshLayout;

    private List<AudioRecording> recordings;
    private AudioRecordingsAdapter adapter;

    private final BroadcastReceiver fetchStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (!ACTION_FETCH_FINISH.equals(action)) {
                LOG.error("Got unknown action {}", action);
                return;
            }

            refreshLayout.setRefreshing(false);
            recordings.clear();
            recordings.addAll(AudioRecordingsRepository.listAll(device));
            // FIXME insert only new records
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            GB.toast(this, "Device is null", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        setContentView(R.layout.activity_audio_recordings);
        addMenuProvider(this);

        final RecyclerView audioRecordingsView = findViewById(R.id.audioRecordingsListView);
        audioRecordingsView.setLayoutManager(new LinearLayoutManager(this));

        recordings = AudioRecordingsRepository.listAll(device);
        adapter = new AudioRecordingsAdapter(this, recordings);

        audioRecordingsView.setAdapter(adapter);

        searchView = findViewById(R.id.audioRecordingsListSearchView);
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
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        refreshLayout = findViewById(R.id.audioRecordingsRefreshLayout);
        refreshLayout.setOnRefreshListener(this::triggerRefresh);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_FETCH_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(fetchStatusReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchStatusReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_audio_recording_activity, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemId == R.id.audio_recording_search) {
            searchView.setVisibility(View.VISIBLE);
            searchView.requestFocus();
            searchView.setIconified(true);
            searchView.setIconified(false);
            return true;
        }

        if (itemId == R.id.audio_recording_refresh) {
            triggerRefresh();
            return true;
        }

        return false;
    }

    private void triggerRefresh() {
        if (!device.isConnected()) {
            Toast.makeText(this, R.string.device_not_connected, Toast.LENGTH_LONG).show();
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_AUDIO_REC);
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
