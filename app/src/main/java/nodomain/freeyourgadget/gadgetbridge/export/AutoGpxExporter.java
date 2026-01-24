package nodomain.freeyourgadget.gadgetbridge.export;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class AutoGpxExporter {
    private static final Logger LOG = LoggerFactory.getLogger(AutoGpxExporter.class);

    public static void doExport(final Context context,
                                final GBDevice gbDevice,
                                @Nullable final BaseActivitySummary summary,
                                final ActivityTrack activityTrack) {
        final GBPrefs prefs = GBApplication.getPrefs();
        final boolean enabled = prefs.getBoolean(GBPrefs.AUTO_EXPORT_GPX_ENABLED, false);
        if (!enabled) {
            LOG.debug("Auto gpx export is disabled");
            return;
        }

        final Set<String> selectedDevices = prefs.getStringSet(GBPrefs.AUTO_EXPORT_GPX_SELECTED_DEVICES, Collections.emptySet());
        final boolean allDevices = prefs.getBoolean(GBPrefs.AUTO_EXPORT_GPX_ALL_DEVICES, true);
        if (!allDevices && !selectedDevices.contains(gbDevice.getAddress())) {
            LOG.debug("Skipping auto gpx export - not enabled for {}", gbDevice);
            return;
        }

        final List<ActivityPoint> points = activityTrack.getAllPoints();
        final Optional<ActivityPoint> firstValidPoint = points.stream().filter(p -> p.getLocation() != null).findFirst();
        if (firstValidPoint.isEmpty()) {
            LOG.warn("Not auto-exporting gpx, no valid points");
            return;
        }

        final String directory = prefs.getString(GBPrefs.AUTO_EXPORT_GPX_DIRECTORY, "");
        if (directory.isBlank()) {
            LOG.warn("Not auto-exporting gpx, no directory specified");
            return;
        }

        final String trackType;
        if (summary != null) {
            trackType = context.getString(ActivityKind.fromCode(summary.getActivityKind()).getLabel()).toLowerCase(Locale.ROOT);
        } else {
            trackType = "track";
        }

        final String isoDate = DateTimeUtils.formatIso8601(points.get(0).getTime());

        final String fileName = FileUtils.makeValidFileName(isoDate + "-" + trackType + ".gpx");

        try {
            final Uri directoryUri = Uri.parse(directory);
            final DocumentFile documentDir = DocumentFile.fromTreeUri(context, directoryUri);

            if (documentDir == null || !documentDir.exists() || !documentDir.canWrite()) {
                LOG.error("Cannot write to directory: {}", directory);
                // TODO notification?
                return;
            }

            final DocumentFile existingFile = documentDir.findFile(fileName);
            if (existingFile != null) {
                LOG.debug("File already exists, will not overwrite: {}", fileName);
                return;
            }

            final DocumentFile targetFile = documentDir.createFile("application/gpx+xml", fileName);
            if (targetFile == null) {
                LOG.error("Failed to create file: {}", fileName);
                // TODO notification?
                return;
            }

            try (OutputStream outputStream = context.getContentResolver().openOutputStream(targetFile.getUri());
                 BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
                final GPXExporter exporter = new GPXExporter();
                exporter.performExport(activityTrack, bos);
            }

            LOG.info("Auto-exported GPX to: {}", targetFile.getUri());
        } catch (final ActivityTrackExporter.GPXTrackEmptyException e) {
            LOG.debug("Activity does not contain any points");
        } catch (final Exception e) {
            LOG.error("Failed to auto-export GPX", e);
            // TODO notification
        }
    }
}
