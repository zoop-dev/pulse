/*  Copyright (C) 2026 Dany Mestas

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
package nodomain.freeyourgadget.gadgetbridge.export;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class AutoFitExporter {
    private static final Logger LOG = LoggerFactory.getLogger(AutoFitExporter.class);

    public static boolean isExportEnabled(@NonNull final GBDevice gbDevice) {
        return getExportDirectory(gbDevice) != null;
    }

    @Nullable
    public static String getExportDirectory(@NonNull final GBDevice gbDevice) {
        final GBPrefs prefs = GBApplication.getPrefs();
        final boolean enabled = prefs.getBoolean(GBPrefs.AUTO_EXPORT_FIT_ENABLED, false);
        if (!enabled) {
            LOG.debug("Auto fit export is disabled");
            return null;
        }

        final Set<String> selectedDevices = prefs.getStringSet(GBPrefs.AUTO_EXPORT_FIT_SELECTED_DEVICES, Collections.emptySet());
        final boolean allDevices = prefs.getBoolean(GBPrefs.AUTO_EXPORT_FIT_ALL_DEVICES, true);
        if (!allDevices && !selectedDevices.contains(gbDevice.getAddress())) {
            LOG.debug("Auto fit export is not enabled for {}", gbDevice);
            return null;
        }

        final String directory = prefs.getString(GBPrefs.AUTO_EXPORT_FIT_DIRECTORY, "");
        if (directory.isBlank()) {
            LOG.warn("No auto fit export directory specified");
            return null;
        }

        return directory;
    }

    public static void doExport(final Context context,
                                final GBDevice gbDevice,
                                @NonNull final BaseActivitySummary summary,
                                @Nullable final ActivityTrack activityTrack) {
        final String directory = getExportDirectory(gbDevice);
        if (directory == null) {
            return;
        }

        final String kindLabel = context.getString(
                ActivityKind.fromCode(summary.getActivityKind()).getLabel()).toLowerCase(Locale.ROOT);
        final String isoDate = DateTimeUtils.formatIso8601(summary.getStartTime());
        final String fileName = FileUtils.makeValidFileName(isoDate + "-" + kindLabel + ".fit");

        final ActivitySummaryData summaryData;
        final String summaryJson = summary.getSummaryData();
        if (summaryJson != null) {
            summaryData = ActivitySummaryData.fromJson(summaryJson);
        } else {
            summaryData = null;
        }

        try {
            final Uri directoryUri = Uri.parse(directory);
            final DocumentFile documentDir = DocumentFile.fromTreeUri(context, directoryUri);
            if (documentDir == null || !documentDir.exists() || !documentDir.canWrite()) {
                LOG.error("Cannot write to directory: {}", directory);
                return;
            }

            final DocumentFile existingFile = documentDir.findFile(fileName);
            if (existingFile != null) {
                LOG.debug("File already exists, will not overwrite: {}", fileName);
                return;
            }

            final DocumentFile targetFile = documentDir.createFile("application/octet-stream", fileName);
            if (targetFile == null) {
                LOG.error("Failed to create file: {}", fileName);
                return;
            }

            // FIT-native devices (Garmin, iGPSPORT) keep the original .fit at
            // rawDetailsPath — export it verbatim instead of regenerating.
            final File rawFit = FitExporter.resolveRawFitFile(summary);
            try (OutputStream out = context.getContentResolver().openOutputStream(targetFile.getUri())) {
                if (out == null) {
                    LOG.error("Failed to open output stream for {}", targetFile.getUri());
                    return;
                }
                if (rawFit != null) {
                    LOG.debug("Auto-export: using original FIT {}", rawFit);
                    try (FileInputStream in = new FileInputStream(rawFit)) {
                        final byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                        }
                    }
                } else {
                    new FitExporter().performExport(activityTrack, summary, summaryData, out);
                }
            }

            LOG.info("Auto-exported FIT to: {}", targetFile.getUri());
        } catch (final Exception e) {
            LOG.error("Failed to auto-export FIT", e);
        }
    }
}
