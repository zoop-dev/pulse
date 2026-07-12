package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class HuaweiActivityTrackMergeTest {

    private static ActivityPoint gpsPoint(final long epochSec, final double lat, final double lng) {
        final ActivityPoint p = new ActivityPoint(new Date(epochSec * 1000L));
        p.setLocation(new GPSCoordinate(lng, lat, GPSCoordinate.UNKNOWN_ALTITUDE));
        return p;
    }

    private static ActivityPoint detailPoint(final long epochSec, final float speed, final int hr,
                                             final int cadence, final Double altitude) {
        final ActivityPoint p = new ActivityPoint(new Date(epochSec * 1000L));
        if (speed >= 0) {
            p.setSpeed(speed);
        }
        if (hr > 0) {
            p.setHeartRate(hr);
        }
        if (cadence >= 0) {
            p.setCadence(cadence);
        }
        if (altitude != null) {
            p.setAltitude(altitude);
        }
        return p;
    }

    private static ActivityTrack trackOf(final ActivityPoint... points) {
        final ActivityTrack track = new ActivityTrack();
        track.addTrackPoints(Arrays.asList(points));
        return track;
    }

    @Test
    public void metricsMergedOntoMatchingGpsPoint() {
        final ActivityTrack track = trackOf(gpsPoint(1000, 52.5, 13.4));
        final List<ActivityPoint> details = Collections.singletonList(detailPoint(1000, 2.5f, 130, 80, 210.0));

        HuaweiActivityTrackProvider.mergeDetailSamples(track, details);

        assertEquals(1, track.getAllPoints().size());
        final ActivityPoint p = track.getAllPoints().get(0);
        assertEquals(2.5f, p.getSpeed(), 1e-6);
        assertEquals(130, p.getHeartRate());
        assertEquals(80, p.getCadence());
        // GPS point had no altitude -> filled from the detail sample.
        assertEquals(210.0, p.getAltitude(), 1e-6);
    }

    @Test
    public void unmatchedDetailAppendedAsLocationlessPoint() {
        final ActivityTrack track = trackOf(gpsPoint(1000, 52.5, 13.4));
        final List<ActivityPoint> details = Arrays.asList(
                detailPoint(1000, 2.5f, 130, 80, null),
                detailPoint(1005, 3.0f, 140, 82, null)); // no GPS point at 1005

        HuaweiActivityTrackProvider.mergeDetailSamples(track, details);

        final List<ActivityPoint> points = track.getAllPoints();
        assertEquals(2, points.size());
        // Time-sorted; the appended sample has no location.
        final ActivityPoint appended = points.get(1);
        assertNull(appended.getLocation());
        assertEquals(3.0f, appended.getSpeed(), 1e-6);
        assertEquals(140, appended.getHeartRate());
    }

    @Test
    public void existingGpsAltitudeNotOverwritten() {
        final ActivityPoint g = new ActivityPoint(new Date(1000L * 1000L));
        g.setLocation(new GPSCoordinate(13.4, 52.5, 500.0)); // GPS already carries altitude
        final ActivityTrack track = trackOf(g);
        final List<ActivityPoint> details = Collections.singletonList(detailPoint(1000, 2.5f, 130, 80, 210.0));

        HuaweiActivityTrackProvider.mergeDetailSamples(track, details);

        assertEquals(500.0, track.getAllPoints().get(0).getAltitude(), 1e-6);
    }

    @Test
    public void emptyDetails_leavesTrackUnchanged() {
        final ActivityTrack track = trackOf(gpsPoint(1000, 52.5, 13.4));

        HuaweiActivityTrackProvider.mergeDetailSamples(track, Collections.emptyList());

        assertEquals(1, track.getAllPoints().size());
        assertEquals(-1f, track.getAllPoints().get(0).getSpeed(), 1e-6); // still the unset sentinel
    }

    @Test
    public void nullDetails_noOp() {
        final ActivityTrack track = trackOf(gpsPoint(1000, 52.5, 13.4));

        HuaweiActivityTrackProvider.mergeDetailSamples(track, null);

        assertEquals(1, track.getAllPoints().size());
    }

    @Test
    public void nullTrack_noOp() {
        // Must not throw when the track is absent.
        HuaweiActivityTrackProvider.mergeDetailSamples(null,
                Collections.singletonList(detailPoint(1000, 2.5f, 130, 80, null)));
    }

    @Test
    public void gpsPointWithNullTime_skipped() {
        final ActivityPoint noTime = new ActivityPoint();
        noTime.setLocation(new GPSCoordinate(13.4, 52.5, GPSCoordinate.UNKNOWN_ALTITUDE));
        final ActivityTrack track = new ActivityTrack();
        track.addTrackPoints(new ArrayList<>(Arrays.asList(noTime, gpsPoint(1000, 52.5, 13.4))));
        final List<ActivityPoint> details = Collections.singletonList(detailPoint(1000, 2.5f, 130, 80, null));

        HuaweiActivityTrackProvider.mergeDetailSamples(track, details);

        // The null-time point is untouched; the 1000s point receives the metrics.
        assertEquals(-1f, noTime.getSpeed(), 1e-6);
        assertEquals(2, track.getAllPoints().size());
    }
}
