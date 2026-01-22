package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GpxActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutGpsParser;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

@SuppressWarnings("ClassCanBeRecord")
public class XiaomiActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiActivityTrackProvider.class);

    private final GBDevice device;
    private final Context context;

    public XiaomiActivityTrackProvider(final GBDevice device, final Context context) {
        this.device = device;
        this.context = context;
    }

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
            final WorkoutGpsParser workoutGpsParser = new WorkoutGpsParser();
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(detailsBytes);
            return workoutGpsParser.getActivityTrack(fileId, detailsBytes);
        } catch (final Exception e) {
            LOG.error("Failed to parse bytes from {}", inputFile, e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }
    }
}
