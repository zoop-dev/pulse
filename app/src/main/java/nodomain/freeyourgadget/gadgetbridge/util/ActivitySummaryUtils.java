package nodomain.freeyourgadget.gadgetbridge.util;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GpxActivityTrackProvider;

public final class ActivitySummaryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryUtils.class);

    private ActivitySummaryUtils() {
        // utility class
    }

    @Nullable
    public static File getShareableGpxFile(final ActivityTrackProvider activityTrackProvider, final BaseActivitySummary summary) {
        if (activityTrackProvider == null) {
            return null;
        }

        if (activityTrackProvider instanceof GpxActivityTrackProvider) {
            // Avoid re-processing what already is a gpx file
            final String gpxTrack = summary.getGpxTrack();
            if (gpxTrack != null) {
                return FileUtils.tryFixPath(new File(gpxTrack));
            }
        }

        ActivityTrack activityTrack = activityTrackProvider.getActivityTrack(summary);
        if (activityTrack == null) {
            // Attempt to fallback to existing gpx file
            if (!(activityTrackProvider instanceof GpxActivityTrackProvider)) {
                activityTrack = new GpxActivityTrackProvider().getActivityTrack(summary);
            }
            if (activityTrack == null) {
                return null;
            }
        }

        try {
            return writeToTmpGpx(activityTrack, summary);
        } catch (final Exception e) {
            LOG.error("Failed to get gpx track", e);
        }

        return null;
    }

    private static File writeToTmpGpx(final ActivityTrack activityTrack,
                                      final BaseActivitySummary summary) throws IOException, ActivityTrackExporter.GPXTrackEmptyException {
        final String summaryDate = DateTimeUtils.formatIso8601(summary.getStartTime());
        final String gpxFileName;
        if (activityTrack.getName() != null) {
            gpxFileName = FileUtils.makeValidFileName(activityTrack.getName() + "_" + summaryDate + ".gpx");
        } else {
            gpxFileName = FileUtils.makeValidFileName("gadgetbridge-" + summaryDate + ".gpx");
        }

        final File cacheDir = GBApplication.getContext().getCacheDir();
        final File rawCacheDir = new File(cacheDir, "gpx");
        //noinspection ResultOfMethodCallIgnored
        rawCacheDir.mkdir();
        final File gpxFile = new File(rawCacheDir, gpxFileName);

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.performExport(activityTrack, gpxFile);

        return gpxFile;
    }
}
