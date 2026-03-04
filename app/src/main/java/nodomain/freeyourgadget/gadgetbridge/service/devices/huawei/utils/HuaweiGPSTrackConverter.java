package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.utils;

import android.text.TextUtils;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiState;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;

public class HuaweiGPSTrackConverter {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiGPSTrackConverter.class);

    public static String generateTrackId(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            instance.update(str.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(instance.digest(), 2);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static HuaweiRouteTrack getTrack(final HuaweiState huaweiState, final GpxFile gpxFile, final String trackName) {
        if(gpxFile == null) {
            LOG.error("Gpx is null");
            return null;
        }
        if (gpxFile.getTracks().isEmpty()) {
            LOG.error("Gpx file contains no Tracks.");
            return null;
        }

        // GPX files may contain multiple tracks, we use only the first one
        final List<GpxTrackPoint> gpxTrackPointList = gpxFile.getTracks().get(0)
                .getTrackSegments().stream()
                .flatMap(segment -> segment.getTrackPoints().stream())
                .collect(Collectors.toList());

        if (gpxTrackPointList.isEmpty()) {
            LOG.error("Gpx track contains no points");
            return null;
        }

        List<HuaweiRouteTrack.TrackPoint> new_point1 = new ArrayList<>();

        boolean hasAltitude = gpxTrackPointList.get(0).hasAltitude();
        // Set altitude to 0 if it is not supported on the track
        double alt = hasAltitude?gpxTrackPointList.get(0).getAltitude():0;
        for (GpxTrackPoint point : gpxTrackPointList) {
            if (hasAltitude && point.hasAltitude()) {
                alt = point.getAltitude();
            }
            new_point1.add(new HuaweiRouteTrack.TrackPoint(point.getLatitude(), point.getLongitude(), alt));
        }

        int totalTime = 0;

        String trackId = generateTrackId(trackName);
        HuaweiRouteTrack track = new HuaweiRouteTrack();
        track.setTrackName(trackName);
        track.setTrackId(trackId);
        track.setHasAltitude(true);
        track.setVersion(2);
        if(!huaweiState.supportsRouteV2()) {
            track.setHasAltitude(false);
            track.setVersion(1);
        }
        track.setTotalTime(totalTime);

        track.setPoints(new_point1);

        return track;
    }
}
