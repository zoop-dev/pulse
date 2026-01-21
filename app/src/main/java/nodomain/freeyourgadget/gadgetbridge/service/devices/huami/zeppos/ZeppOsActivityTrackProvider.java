package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GpxActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class ZeppOsActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsActivityTrackProvider.class);

    /**
     * If parsing the raw file fails, we fallback to gpx, which we may have previously written
     */
    @Nullable
    @Override
    public ActivityTrack getActivityTrack(@NonNull final BaseActivitySummary summary) {
        final File inputFile = FileUtils.tryFixPath(summary.getRawDetailsPath());
        if (inputFile == null) {
            LOG.warn("Raw file for details not found: {}", summary.getRawDetailsPath());
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        LOG.debug("Loading raw details from {}", inputFile);

        final byte[] detailsBytes;
        try {
            detailsBytes = FileUtils.readAll(inputFile);
        } catch (final IOException e) {
            LOG.error("Failed to read {}", inputFile, e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        try {
            return new ZeppOsActivityDetailsParser(summary).parse(detailsBytes);
        } catch (GBException e) {
            LOG.error("Failed to parse bytes from {}", inputFile, e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }
    }
}
