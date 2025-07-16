/*  Copyright (C) 2025 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class was copied and modified from
 * https://github.com/OpenTracksApp/OSMDashboard/blob/v4.3.0/src/main/java/de/storchp/opentracks/osmplugin/dashboardapi/TrackPoint.java
 */
public class TrackPoint {
    private static final Logger LOG = LoggerFactory.getLogger(TrackPoint.class);

    public static final String _ID = "_id";
    public static final String TRACKID = "trackid";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String TIME = "time";
    public static final String TYPE = "type";
    public static final String SPEED = "speed";
    public static final double PAUSE_LATITUDE = 100.0;
    public static final double LAT_LON_FACTOR = 1E6;

    public static final String[] PROJECTION_V1 = {
            _ID,
            TRACKID,
            LATITUDE,
            LONGITUDE,
            TIME,
            SPEED
    };

    public static final String[] PROJECTION_V2 = {
            _ID,
            TRACKID,
            LATITUDE,
            LONGITUDE,
            TIME,
            TYPE,
            SPEED
    };

    private final long trackPointId;
    private final long trackId;
    private final double latitude;
    private final double longitude;
    private final boolean pause;
    private final double speed;

    public TrackPoint(long trackId, long trackPointId, double latitude, double longitude, Integer type, double speed) {
        this.trackId = trackId;
        this.trackPointId = trackPointId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pause = type != null ? type == 3 : latitude == PAUSE_LATITUDE;
        this.speed = speed;
    }

    public boolean hasValidLocation() {
        return latitude != 0 || longitude != 0;
    }

    public boolean isPause() {
        return pause;
    }

    /**
     * Reads the TrackPoints from the Content Uri and split by segments.
     * Pause TrackPoints and different Track IDs split the segments.
     */
    public static TrackPointsBySegments readTrackPointsBySegments(ContentResolver resolver, Uri data, long lastTrackPointId, int protocolVersion) {
        var segments = new ArrayList<List<TrackPoint>>();
        var projection = PROJECTION_V2;
        var typeQuery = " AND " + TrackPoint.TYPE + " IN (-2, -1, 0, 1, 3)";
        if (protocolVersion < 2) { // fallback to old Dashboard API
            projection = PROJECTION_V1;
            typeQuery = "";
        }
        try (Cursor cursor = resolver.query(data, projection, TrackPoint._ID + "> ?" + typeQuery, new String[]{Long.toString(lastTrackPointId)}, null)) {
            TrackPoint lastTrackPoint = null;
            List<TrackPoint> segment = null;
            while (cursor.moveToNext()) {
                var trackPointId = cursor.getLong(cursor.getColumnIndexOrThrow(TrackPoint._ID));
                var trackId = cursor.getLong(cursor.getColumnIndexOrThrow(TrackPoint.TRACKID));
                var latitude = cursor.getInt(cursor.getColumnIndexOrThrow(TrackPoint.LATITUDE)) / LAT_LON_FACTOR;
                var longitude = cursor.getInt(cursor.getColumnIndexOrThrow(TrackPoint.LONGITUDE)) / LAT_LON_FACTOR;
                var typeIndex = cursor.getColumnIndex(TrackPoint.TYPE);
                var speed = cursor.getDouble(cursor.getColumnIndexOrThrow(TrackPoint.SPEED));

                Integer type = null;
                if (typeIndex > -1) {
                    type = cursor.getInt(typeIndex);
                }

                if (lastTrackPoint == null || lastTrackPoint.trackId != trackId) {
                    segment = new ArrayList<>();
                    segments.add(segment);
                }

                lastTrackPoint = new TrackPoint(trackId, trackPointId, latitude, longitude, type, speed);
                if (lastTrackPoint.hasValidLocation()) {
                    segment.add(lastTrackPoint);
                } else if (!lastTrackPoint.isPause()) {
                    // invalid trackpoint
                }
                if (lastTrackPoint.isPause()) {
                    if (!lastTrackPoint.hasValidLocation()) {
                        if (!segment.isEmpty()) {
                            var previousTrackpoint = segment.get(segment.size() - 1);
                            if (previousTrackpoint.hasValidLocation()) {
                                segment.add(new TrackPoint(trackId, trackPointId, previousTrackpoint.getLatitude(), previousTrackpoint.getLongitude(), type, speed));
                            }
                        }
                    }
                    lastTrackPoint = null;
                }
            }
        } catch (Exception e) {
            LOG.error("Couldn't read trackpoints from OpenTracks URI", e);
        }
        return new TrackPointsBySegments(segments);
    }

    public long getTrackPointId() {
        return trackPointId;
    }

    public long getTrackId() {
        return trackId;
    }

    public double getSpeed() {
        return speed;
    }

    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
}
