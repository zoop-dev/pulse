package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiGpsParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.model.GpxActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

@SuppressWarnings("ClassCanBeRecord")
public class HuaweiActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiActivityTrackProvider.class);

    private final GBDevice gbDevice;

    public HuaweiActivityTrackProvider(final GBDevice gbDevice) {
        this.gbDevice = gbDevice;
    }

    @Nullable
    @Override
    public ActivityTrack getActivityTrack(@NonNull final BaseActivitySummary summary) {
        // Find the existing HuaweiWorkoutSummarySample and its per-sample data samples.
        final HuaweiWorkoutSummarySample huaweiWorkoutSummarySample;
        final List<HuaweiWorkoutDataSample> dataSamples;
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            QueryBuilder<HuaweiWorkoutSummarySample> qb = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp.eq(summary.getStartTime().getTime() / 1000));
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(device.getId()));
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(user.getId()));
            final List<HuaweiWorkoutSummarySample> huaweiSummaries = qb.build().list();
            if (huaweiSummaries.isEmpty()) {
                LOG.warn("Failed to find huawei summary for {}", summary.getId());
                return new GpxActivityTrackProvider().getActivityTrack(summary);
            }

            huaweiWorkoutSummarySample = huaweiSummaries.get(0);

            final QueryBuilder<HuaweiWorkoutDataSample> qbData = session.getHuaweiWorkoutDataSampleDao().queryBuilder();
            qbData.where(HuaweiWorkoutDataSampleDao.Properties.WorkoutId.eq(huaweiWorkoutSummarySample.getWorkoutId()));
            dataSamples = qbData.build().list();
        } catch (Exception e) {
            LOG.error("Failed to get huawei summary", e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        // Rich per-sample points (speed / HR / cadence / altitude) — the same decode the
        // in-app charts use. These carry the metrics the Health Connect detailed sync and
        // GPX/FIT exporters read from ActivityPoint, which the GPS file alone does not have.
        final List<ActivityPoint> detailPoints = (dataSamples == null || dataSamples.isEmpty())
                ? Collections.emptyList()
                : HuaweiWorkoutGbParser.buildActivityPointsFromSamples(huaweiWorkoutSummarySample, dataSamples);

        // Parse the GPS track file if present (gives location, and altitude on some models).
        HuaweiGpsParser.GpsPoint[] gpsParsed = null;
        final String rawGpsFileLocation = huaweiWorkoutSummarySample.getRawGpsFileLocation();
        if (rawGpsFileLocation != null) {
            final File rawGpsFile = FileUtils.tryFixPath(rawGpsFileLocation);
            if (rawGpsFile == null) {
                LOG.debug("Raw gps file not found: {}", rawGpsFileLocation);
            } else {
                LOG.debug("Loading gps points from {}", rawGpsFileLocation);
                try {
                    final byte[] rawGpsBytes = FileUtils.readAll(rawGpsFile);
                    gpsParsed = HuaweiGpsParser.parseHuaweiGps(rawGpsBytes);
                } catch (final IOException e) {
                    LOG.error("Failed to read raw gps bytes from {}", rawGpsFile, e);
                } catch (final Exception e) {
                    LOG.error("Failed to parse Huawei GpsPoints", e);
                }
            }
        }

        final ActivityTrack activityTrack = new ActivityTrack();
        activityTrack.setName(summary.getName());

        if (gpsParsed != null && gpsParsed.length > 0) {
            // GPS workout (e.g. outdoor hike/ride): merge the rich metrics onto the located
            // points by timestamp, then append any unmatched detail samples (location-less).
            for (final HuaweiGpsParser.GpsPoint gp : gpsParsed) {
                activityTrack.addTrackPoint(gp.toActivityPoint());
            }
            mergeDetailSamples(activityTrack, detailPoints);
            return activityTrack;
        }

        if (!detailPoints.isEmpty()) {
            // No GPS (indoor/treadmill): expose the rich per-sample stream on its own so the
            // detailed sync still exports per-sample speed and heart rate.
            activityTrack.addTrackPoints(detailPoints);
            activityTrack.sortPointsByTime();
            return activityTrack;
        }

        // Nothing device-specific available — fall back to a stored GPX if any.
        return new GpxActivityTrackProvider().getActivityTrack(summary);
    }

    /**
     * Merge per-sample detail metrics onto an existing (GPS) track by unix-second timestamp.
     * Located points gain speed/HR/cadence (and altitude when the GPS file has none); detail
     * samples with no matching GPS timestamp (e.g. a mid-trip GPS fix gap) are appended as
     * location-less points so the metric stream stays continuous, and the track is re-sorted by
     * time only when such points were appended. Mirrors Xiaomi's
     * {@code WorkoutDetailsParser.mergeOntoTrack}.
     */
    static void mergeDetailSamples(final ActivityTrack track,
                                   final List<ActivityPoint> detailPoints) {
        if (track == null || detailPoints == null || detailPoints.isEmpty()) {
            return;
        }
        final Map<Long, ActivityPoint> bySecond = new HashMap<>(detailPoints.size());
        for (final ActivityPoint dp : detailPoints) {
            if (dp.getTime() != null) {
                bySecond.put(dp.getTime().getTime() / 1000L, dp);
            }
        }
        for (final ActivityPoint gp : track.getAllPoints()) {
            if (gp.getTime() == null) {
                continue;
            }
            final ActivityPoint dp = bySecond.remove(gp.getTime().getTime() / 1000L);
            if (dp == null) {
                continue;
            }
            if (dp.getSpeed() >= 0) {
                gp.setSpeed(dp.getSpeed());
            }
            if (dp.getHeartRate() > 0) {
                gp.setHeartRate(dp.getHeartRate());
            }
            if (dp.getCadence() >= 0) {
                gp.setCadence(dp.getCadence());
            }
            if (gp.getAltitude() <= GPSCoordinate.UNKNOWN_ALTITUDE
                    && dp.getAltitude() > GPSCoordinate.UNKNOWN_ALTITUDE) {
                gp.setAltitude(dp.getAltitude());
            }
        }
        if (bySecond.isEmpty()) {
            // GPS points are already in file (time) order; nothing appended, so no re-sort.
            return;
        }
        // Leftover detail samples without a matching GPS timestamp keep the metric stream continuous.
        for (final ActivityPoint leftover : bySecond.values()) {
            track.addTrackPoint(leftover);
        }
        track.sortPointsByTime();
    }
}
