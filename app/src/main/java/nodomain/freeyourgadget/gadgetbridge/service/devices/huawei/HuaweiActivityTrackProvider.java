package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiGpsParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
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
        // Find the existing HuaweiWorkoutSummarySample
        final HuaweiWorkoutSummarySample huaweiWorkoutSummarySample;
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
        } catch (Exception e) {
            LOG.error("Failed to get huawei summary");
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        final String rawGpsFileLocation = huaweiWorkoutSummarySample.getRawGpsFileLocation();
        final File rawGpsFile = FileUtils.tryFixPath(rawGpsFileLocation);
        if (rawGpsFile == null) {
            LOG.debug("Raw gps file not found: {}", rawGpsFileLocation);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }

        LOG.debug("Loading gps points from {}", rawGpsFileLocation);

        final byte[] rawGpsBytes;
        try {
            rawGpsBytes = FileUtils.readAll(rawGpsFile);
        } catch (final IOException e) {
            LOG.error("Failed to read raw gps bytes from {}", rawGpsFile, e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }
        try {
            final HuaweiGpsParser.GpsPoint[] gpsPoints = HuaweiGpsParser.parseHuaweiGps(rawGpsBytes);
            final List<ActivityPoint> activityPoints = Arrays.stream(gpsPoints)
                    .map(HuaweiGpsParser.GpsPoint::toActivityPoint).collect(Collectors.toList());

            final ActivityTrack activityTrack = new ActivityTrack();
            activityTrack.setName(summary.getName());
            activityTrack.addTrackPoints(activityPoints);
            return activityTrack;
        } catch (final Exception e) {
            LOG.error("Failed to parse Huawei GpsPoints", e);
            return new GpxActivityTrackProvider().getActivityTrack(summary);
        }
    }
}
