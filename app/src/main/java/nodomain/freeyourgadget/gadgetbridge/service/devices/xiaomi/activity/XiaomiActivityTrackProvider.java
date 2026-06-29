package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivityFile;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivityFileDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.model.GpxActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutGpsParser;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

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
        final long ts = summary.getStartTime() != null
                ? summary.getStartTime().getTime() / 1000L
                : 0L;
        if (ts <= 0) {
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        XiaomiActivityFile gpsFile = null;
        XiaomiActivityFile detailsFile = null;
        try (DBHandler dbh = GBApplication.acquireDbReadOnly()) {
            final DaoSession session = dbh.getDaoSession();
            final Device dbDevice = DBHelper.getDevice(device, session);
            final List<XiaomiActivityFile> files = session.getXiaomiActivityFileDao().queryBuilder()
                    .where(XiaomiActivityFileDao.Properties.DeviceId.eq(dbDevice.getId()),
                            XiaomiActivityFileDao.Properties.Timestamp.eq(ts))
                    .list();
            for (final XiaomiActivityFile f : files) {
                if (f.getDetailType() == XiaomiActivityFileId.DetailType.GPS_TRACK.getCode()) {
                    gpsFile = f;
                } else if (f.getDetailType() == XiaomiActivityFileId.DetailType.DETAILS.getCode()) {
                    detailsFile = f;
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed XiaomiActivityFile lookup for ts={}", ts, e);
        }

        if (gpsFile == null && detailsFile == null) {
            // Backward compat: fall back to BaseActivitySummary.rawDetailsPath.
            final ActivityTrack track = legacyTrackFromRawDetailsPath(summary);
            if (track != null) return track;
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        if (gpsFile != null) {
            final ActivityTrack track = parseGps(gpsFile);
            if (track != null && hasAnyNonNullIslandLocation(track)) {
                if (detailsFile != null) {
                    mergeDetails(track, detailsFile);
                }
                return track;
            }
            // Empty or all-Null-Island GPS track (e.g. indoor session that produced a
            // 13-byte placeholder, or a workout that never acquired a fix) — fall through
            // to the DETAILS-only path so we still surface HR / cadence samples.
        }

        if (detailsFile != null) {
            return parseDetailsAsTrack(detailsFile);
        }
        return new GpxActivityTrackProvider().getActivityTrack(summary);
    }

    /**
     * Merge the DETAILS per-record stream (HR / cadence / speed) onto an already-parsed GPS
     * {@link ActivityTrack}. For the auto-export path ({@code WorkoutGpsParser.parse}), which
     * builds the GPS track itself and emits GPX/FIT immediately instead of going through
     * {@link #getActivityTrack} — without this the auto-exported files carry GPS + speed but no
     * HR. No-op when the workout has no DETAILS file or the file yields no samples. Relies on
     * the DETAILS file already being persisted, guaranteed by the fetch order (DETAILS is
     * fetched before GPS_TRACK — see {@link XiaomiActivityFileId.DetailType#getFetchOrder}).
     */
    public static void mergeDetailsForExport(@NonNull final GBDevice device,
                                             @NonNull final ActivityTrack track,
                                             final long tsSeconds) {
        if (tsSeconds <= 0) {
            return;
        }
        XiaomiActivityFile detailsFile = null;
        try (DBHandler dbh = GBApplication.acquireDbReadOnly()) {
            final DaoSession session = dbh.getDaoSession();
            final Device dbDevice = DBHelper.getDevice(device, session);
            final List<XiaomiActivityFile> files = session.getXiaomiActivityFileDao().queryBuilder()
                    .where(XiaomiActivityFileDao.Properties.DeviceId.eq(dbDevice.getId()),
                            XiaomiActivityFileDao.Properties.Timestamp.eq(tsSeconds))
                    .list();
            for (final XiaomiActivityFile f : files) {
                if (f.getDetailType() == XiaomiActivityFileId.DetailType.DETAILS.getCode()) {
                    detailsFile = f;
                    break;
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed DETAILS lookup for auto-export ts={}", tsSeconds, e);
            return;
        }
        if (detailsFile != null) {
            mergeDetails(track, detailsFile);
        }
    }

    public static boolean hasAnyNonNullIslandLocation(final ActivityTrack track) {
        for (final ActivityPoint p : track.getAllPoints()) {
            if (hasNonNullIslandLocation(p)) {
                return true;
            }
        }
        return false;
    }

    // Some indoor activities record all points with fake Null Island (0°N 0°E) position.
    public static boolean hasNonNullIslandLocation(final ActivityPoint point) {
        if (point != null) {
            final GPSCoordinate location = point.getLocation();
            if (location != null) {
                final double lat = location.getLatitude();
                final double lon = location.getLongitude();
                return (!Double.isNaN(lat) && lat != 0.0) || (!Double.isNaN(lon) && lon != 0.0);
            }
        }
        return false;
    }

    @Nullable
    private static ActivityTrack parseGps(final XiaomiActivityFile gpsFile) {
        final byte[] bytes = readBytes(gpsFile);
        if (bytes == null) return null;
        try {
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bytes);
            final byte[] fixedBytes = XiaomiActivityParser.fixAndWrap(bytes).array();
            return new WorkoutGpsParser().getActivityTrack(fileId, fixedBytes);
        } catch (final Exception e) {
            LOG.error("Failed to parse GPS bytes from {}", gpsFile.getFilePath(), e);
            return null;
        }
    }

    @Nullable
    private static ActivityTrack parseDetailsAsTrack(final XiaomiActivityFile detailsFile) {
        final byte[] bytes = readBytes(detailsFile);
        if (bytes == null) return null;
        try {
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bytes);
            final byte[] fixedBytes = XiaomiActivityParser.fixAndWrap(bytes).array();
            return new WorkoutDetailsParser().getActivityTrack(fileId, fixedBytes);
        } catch (final Exception e) {
            LOG.error("Failed to parse DETAILS bytes from {}", detailsFile.getFilePath(), e);
            return null;
        }
    }

    private static void mergeDetails(final ActivityTrack track, final XiaomiActivityFile detailsFile) {
        final byte[] bytes = readBytes(detailsFile);
        if (bytes == null) return;
        try {
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bytes);
            final byte[] fixedBytes = XiaomiActivityParser.fixAndWrap(bytes).array();
            WorkoutDetailsParser.mergeOntoTrack(track, fileId, fixedBytes);
        } catch (final Exception e) {
            LOG.warn("Failed to merge DETAILS into GPS track from {}", detailsFile.getFilePath(), e);
        }
    }

    @Nullable
    private static byte[] readBytes(final XiaomiActivityFile entry) {
        final File file = FileUtils.tryFixPath(entry.getFilePath());
        if (file == null) {
            LOG.warn("Raw file missing: {}", entry.getFilePath());
            return null;
        }
        try {
            return FileUtils.readAll(file);
        } catch (final IOException e) {
            LOG.error("Failed to read {}", file, e);
            return null;
        }
    }

    /** Legacy fallback for summaries created before XIAOMI_ACTIVITY_FILE existed. */
    @Nullable
    private ActivityTrack legacyTrackFromRawDetailsPath(final BaseActivitySummary summary) {
        final File inputFile = FileUtils.tryFixPath(summary.getRawDetailsPath());
        if (inputFile == null) return null;
        final byte[] bytes;
        try {
            bytes = FileUtils.readAll(inputFile);
        } catch (final IOException e) {
            LOG.error("Failed to read legacy {}", inputFile, e);
            return null;
        }
        try {
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bytes);
            final byte[] fixedBytes = XiaomiActivityParser.fixAndWrap(bytes).array();
            if (fileId.getDetailType() == XiaomiActivityFileId.DetailType.GPS_TRACK) {
                return new WorkoutGpsParser().getActivityTrack(fileId, fixedBytes);
            }
            if (fileId.getDetailType() == XiaomiActivityFileId.DetailType.DETAILS) {
                return new WorkoutDetailsParser().getActivityTrack(fileId, fixedBytes);
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse legacy bytes from {}", inputFile, e);
        }
        return null;
    }
}
