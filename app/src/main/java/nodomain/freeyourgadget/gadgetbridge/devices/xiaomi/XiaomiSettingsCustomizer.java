/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.hidePrefIfNoneVisible;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.populateOrHideListPreference;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSettingsCustomizer.class);

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference reprocessActivityPref = handler.findPreference("reprocess_activity_files");
        if (reprocessActivityPref != null) {
            reprocessActivityPref.setOnPreferenceClickListener(preference -> {
                parseAllActivityFilesFromStorage(handler.getContext(), handler.getDevice());
                return true;
            });
        }

        final Preference activityMonitoringPref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ACTIVITY_MONITORING);
        if (activityMonitoringPref != null) {
            activityMonitoringPref.setVisible(false);
        }

        final Preference hrAlertActivePref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD);
        if (hrAlertActivePref != null) {
            hrAlertActivePref.setVisible(false);
        }

        populateOrHideListPreference(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, handler, prefs);

        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_DISPLAY, Arrays.asList(
                HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE,
                DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD
        ));
        hidePrefIfNoneVisible(handler, "pref_header_other", Arrays.asList(
                "pref_contacts",
                "camera_remote",
                "screen_events_forwarding",
                "phone_silent_mode"
        ));
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<XiaomiSettingsCustomizer> CREATOR = new Creator<>() {
        @Override
        public XiaomiSettingsCustomizer createFromParcel(final Parcel in) {
            return new XiaomiSettingsCustomizer();
        }

        @Override
        public XiaomiSettingsCustomizer[] newArray(final int size) {
            return new XiaomiSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
    }

    private static final AtomicBoolean PARSING_FROM_STORAGE = new AtomicBoolean(false);

    private static void parseAllActivityFilesFromStorage(final Context context, final GBDevice device) {
        if (!PARSING_FROM_STORAGE.compareAndSet(false, true)) {
            GB.toast(context, "Already parsing!", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        LOG.info("Parsing all activity files from storage");

        final List<File> activityFiles;
        try {
            final File externalFilesDir = device.getDeviceCoordinator().getWritableExportDirectory(device, true);
            final File exportDir = new File(externalFilesDir, "rawFetchOperations");

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                GB.toast(context, "export directory " + exportDir + " not found", Toast.LENGTH_LONG, GB.ERROR);
                return;
            }

            activityFiles = FileUtils.listRecursive(exportDir, (dir, name) -> name.endsWith(".bin"));
            if (activityFiles.isEmpty()) {
                LOG.error("No activity files found in {}", exportDir);
                GB.toast(context, "No activity files found in " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return;
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            GB.toast(context, "Failed to parse from storage", Toast.LENGTH_LONG, GB.ERROR, e);
            return;
        }

        LOG.debug("Will parse {} files", activityFiles.size());

        GB.toast(context, "Check notification for progress", Toast.LENGTH_LONG, GB.INFO);
        GB.updateTransferNotification("Parsing activity files", "...", true, 0, context);
        final long[] lastNotificationUpdateTs = new long[]{System.currentTimeMillis()};

        final Handler handler = new Handler(context.getMainLooper());
        new Thread(() -> {
            try {
                int[] i = new int[]{0};
                for (final File activityFile : activityFiles) {
                    i[0]++;

                    LOG.debug("Parsing {}", activityFile);

                    final long now = System.currentTimeMillis();
                    if (now - lastNotificationUpdateTs[0] > 1500L) {
                        lastNotificationUpdateTs[0] = now;
                        handler.post(() -> {
                            GB.updateTransferNotification(
                                    "Parsing activity files", "File " + i[0] + " of " + activityFiles.size(),
                                    true,
                                    (i[0] * 100) / activityFiles.size(), context
                            );
                        });
                    }

                    // The logic below just replicates XiaomiActivityFileFetcher

                    final byte[] data;
                    try (InputStream in = new FileInputStream(activityFile)) {
                        data = FileUtils.readAll(in, 999999);
                    } catch (final IOException ioe) {
                        LOG.error("Failed to read {}", activityFile, ioe);
                        continue;
                    }

                    final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
                    final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

                    final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
                    if (activityParser == null) {
                        LOG.warn("Failed to find parser for {}", fileId);
                        continue;
                    }

                    try {
                        if (activityParser.parse(context, device, fileId, data)) {
                            LOG.info("Successfully parsed {}", fileId);
                        } else {
                            LOG.warn("Failed to parse {}", fileId);
                        }
                    } catch (final Exception ex) {
                        LOG.error("Exception while parsing {}", fileId, ex);
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to parse from storage", e);
            }

            handler.post(() -> {
                PARSING_FROM_STORAGE.set(false);
                GB.updateTransferNotification("", "", false, 100, context);
                GB.signalActivityDataFinish(device);
            });
        }, "XiaomiReprocessThread").start();
    }
}
