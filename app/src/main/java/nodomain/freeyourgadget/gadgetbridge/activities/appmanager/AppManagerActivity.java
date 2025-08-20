/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class AppManagerActivity extends AbstractGBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(AppManagerActivity.class);
    private static final int READ_REQUEST_CODE = 42;

    private GBDevice mGBDevice = null;

    private List<String> enabledTabsList;

    public GBDevice getGBDevice() {
        return mGBDevice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragmentappmanager);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        if (mGBDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        final DeviceCoordinator coordinator = mGBDevice.getDeviceCoordinator();

        enabledTabsList = new ArrayList<>();

        if (coordinator.supportsCachedAppManagement(mGBDevice)) {
            enabledTabsList.add("cache");
        }
        if (coordinator.supportsInstalledAppManagement(mGBDevice)) {
            enabledTabsList.add("apps");
        }
        if (coordinator.supportsWatchfaceManagement(mGBDevice)) {
            enabledTabsList.add("watchfaces");
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        if (coordinator.supportsFlashing(mGBDevice)) {
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // Set up the ViewPager with the sections adapter.
        ViewPager2 viewPager = findViewById(R.id.appmanager_pager);
        if (viewPager != null) {
            viewPager.setAdapter(new SectionsStateAdapter(this));
        }

        TabLayout tabLayout = findViewById(R.id.charts_pagerTabStrip);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(getPageTitle(position));
                }).attach();
    }

    public static synchronized void deleteFromAppOrderFile(String filename, UUID uuid) {
        ArrayList<UUID> uuids = getUuidsFromFile(filename);
        uuids.remove(uuid);
        rewriteAppOrderFile(filename, uuids);
    }

    public class SectionsStateAdapter extends FragmentStateAdapter {
        SectionsStateAdapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (enabledTabsList.get(position)) {
                case "cache" -> new AppManagerFragmentCache();
                case "apps" -> new AppManagerFragmentInstalledApps();
                case "watchfaces" -> new AppManagerFragmentInstalledWatchfaces();
                default -> throw new IllegalArgumentException("Unknown tab position " + position);
            };
        }

        @Override
        public int getItemCount() {
            return enabledTabsList.size();
        }
    }

    public CharSequence getPageTitle(int position) {
        return switch (enabledTabsList.get(position)) {
            case "cache" -> getString(R.string.appmanager_cached_watchapps_watchfaces);
            case "apps" -> getString(R.string.appmanager_installed_watchapps);
            case "watchfaces" -> getString(R.string.appmanager_installed_watchfaces);
            default -> "";
        };
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    static synchronized void rewriteAppOrderFile(String filename, Iterable<UUID> uuids) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(FileUtils.getExternalFilesDir() + "/" + filename))) {
            for (UUID uuid : uuids) {
                out.write(uuid.toString());
                out.newLine();
            }
        } catch (IOException e) {
            LOG.warn("can't write app order to file!");
        }
    }

    synchronized public static void addToAppOrderFile(String filename, UUID uuid) {
        ArrayList<UUID> uuids = getUuidsFromFile(filename);
        if (!uuids.contains(uuid)) {
            uuids.add(uuid);
            rewriteAppOrderFile(filename, uuids);
        }
    }

    static synchronized ArrayList<UUID> getUuidsFromFile(String filename) {
        ArrayList<UUID> uuids = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(FileUtils.getExternalFilesDir() + "/" + filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                uuids.add(UUID.fromString(line));
            }
        } catch (IOException e) {
            LOG.warn("could not read sort file");
        }
        return uuids;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final Uri uri = resultData.getData();
            final InstallHandler installHandler = mGBDevice.getDeviceCoordinator().findInstallHandler(uri, this);
            if (installHandler == null) {
                GB.toast(getString(R.string.fwinstaller_file_not_compatible_to_device), Toast.LENGTH_LONG, GB.INFO);
                return;
            }
            final Intent startIntent = new Intent(AppManagerActivity.this, installHandler.getInstallActivity());
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
            startIntent.setAction(Intent.ACTION_VIEW);
            startIntent.setDataAndType(uri, null);
            startActivity(startIntent);
        }
    }
}
