package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfWorkoutGpsSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfWorkoutGpsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

@SuppressWarnings("ClassCanBeRecord")
public class CmfActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CmfActivityTrackProvider.class);

    private final GBDevice gbDevice;

    public CmfActivityTrackProvider(final GBDevice gbDevice) {
        this.gbDevice = gbDevice;
    }

    @Nullable
    @Override
    public ActivityTrack getActivityTrack(@NonNull final BaseActivitySummary summary) {
        final ActivityTrack track = new ActivityTrack();
        track.setUser(summary.getUser());
        track.setDevice(summary.getDevice());
        track.setName(createActivityName(summary));

        final List<CmfWorkoutGpsSample> gpsSamples;
        final List<CmfHeartRateSample> hrSamples;
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final CmfWorkoutGpsSampleProvider gpsSampleProvider = new CmfWorkoutGpsSampleProvider(gbDevice, session);
            gpsSamples = gpsSampleProvider.getAllSamples(summary.getStartTime().getTime(), summary.getEndTime().getTime());

            final CmfHeartRateSampleProvider hrSampleProvider = new CmfHeartRateSampleProvider(gbDevice, session);
            hrSamples = new ArrayList<>(hrSampleProvider.getAllSamples(summary.getStartTime().getTime(), summary.getEndTime().getTime()));
        } catch (final Exception e) {
            LOG.error("Error while building activity track", e);
            return null;
        }

        Collections.sort(hrSamples, Comparator.comparingLong(CmfHeartRateSample::getTimestamp));

        for (final CmfWorkoutGpsSample gpsSample : gpsSamples) {
            final ActivityPoint ap = new ActivityPoint(new Date(gpsSample.getTimestamp()));
            final GPSCoordinate coordinate = new GPSCoordinate(
                    gpsSample.getLongitude() / 10000000d,
                    gpsSample.getLatitude() / 10000000d
            );
            ap.setLocation(coordinate);

            final CmfHeartRateSample hrSample = findNearestSample(hrSamples, gpsSample.getTimestamp());
            if (hrSample != null) {
                ap.setHeartRate(hrSample.getHeartRate());
            }

            track.addTrackPoint(ap);
        }

        return track;
    }

    @Nullable
    private CmfHeartRateSample findNearestSample(final List<CmfHeartRateSample> samples, final long timestamp) {
        if (samples.isEmpty()) {
            return null;
        }

        if (timestamp < samples.get(0).getTimestamp()) {
            return samples.get(0);
        }

        if (timestamp > samples.get(samples.size() - 1).getTimestamp()) {
            return samples.get(samples.size() - 1);
        }

        int start = 0;
        int end = samples.size() - 1;

        while (start <= end) {
            final int mid = (start + end) / 2;

            if (timestamp < samples.get(mid).getTimestamp()) {
                end = mid - 1;
            } else if (timestamp > samples.get(mid).getTimestamp()) {
                start = mid + 1;
            } else {
                return samples.get(mid);
            }
        }

        // FIXME return null if too far?

        if (samples.get(start).getTimestamp() - timestamp < timestamp - samples.get(end).getTimestamp()) {
            return samples.get(start);
        }

        return samples.get(end);
    }

    protected static String createActivityName(final BaseActivitySummary summary) {
        String name = summary.getName();
        String nameText = "";
        Long id = summary.getId();
        if (name != null) {
            nameText = name + " - ";
        }
        return nameText + id;
    }
}
