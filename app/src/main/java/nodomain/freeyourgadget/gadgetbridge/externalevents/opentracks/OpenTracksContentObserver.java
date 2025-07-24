/*  Copyright (C) 2022-2024 José Rebelo

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

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;


public class OpenTracksContentObserver extends ContentObserver {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTracksContentObserver.class);

    private Context mContext;
    private Uri tracksUri;
    private Uri trackpointsUri;
    private int protocolVersion;
    private int totalTimeMillis;
    private float totalDistanceMeter;
    private final ActivityTrack activityTrack = new ActivityTrack();

    private long previousTimeMillis = 0;
    private float previousDistanceMeter = 0;
    private long lastTrackId;
    private long lastTrackPointId;
    private ActivityPoint previousActivityPoint = null;

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }
    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }
    public ActivityTrack getActivityTrack() {
        return activityTrack;
    }

    public long getTimeMillisChange() {
        /**
         * We don't use the timeMillis received from OpenTracks here, because those updates do not
         * come in very regularly when GPS reception is bad
         */
        long timeMillisDelta = System.currentTimeMillis() - previousTimeMillis;
        previousTimeMillis = System.currentTimeMillis();
        return timeMillisDelta;
    }

    public float getDistanceMeterChange() {
        float distanceMeterDelta = totalDistanceMeter - previousDistanceMeter;
        previousDistanceMeter = totalDistanceMeter;
        return distanceMeterDelta;
    }

    public OpenTracksContentObserver(Context context, final Uri tracksUri, final Uri trackpointsUri, final int protocolVersion) {
        super(new Handler());
        this.mContext = context;
        this.tracksUri = tracksUri;
        this.trackpointsUri = trackpointsUri;
        this.protocolVersion = protocolVersion;
        this.previousTimeMillis = System.currentTimeMillis();

        LOG.debug("Initializing OpenTracksContentObserver...");
    }

    @Override
    public void onChange(final boolean selfChange, final Uri uri) {
        if (uri == null) {
            return; // nothing can be done without an uri
        }
        if (tracksUri.toString().startsWith(uri.toString())) {
            final List<Track> tracks = Track.readTracks(mContext.getContentResolver(), tracksUri, protocolVersion);
            if (!tracks.isEmpty()) {
                final TrackStatistics statistics = new TrackStatistics(tracks);
                totalTimeMillis = statistics.getTotalTimeMillis();
                totalDistanceMeter = statistics.getTotalDistanceMeter();
            }
        }
        if (trackpointsUri.toString().startsWith(uri.toString())) {
            readTrackPointsBySegments(trackpointsUri);
        }
    }

    public void unregister() {
        if (mContext != null) {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public void finish() {
        unregister();
        if (mContext != null) {
            ((Activity) mContext).finish();
            mContext = null;
        }
    }

    /**
     * The constants and logic below were copied and modified from
     * https://github.com/OpenTracksApp/OSMDashboard/blob/v4.3.0/src/main/java/de/storchp/opentracks/osmplugin/dashboardapi/TrackPoint.java
     */
    public static final String _ID = "_id";
    public static final String TRACKID = "trackid";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String TIME = "time";
    public static final String TYPE = "type";
    public static final String SPEED = "speed";
    public static final String ELEVATION = "elevation";
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
            SPEED,
            ELEVATION
    };
    public void readTrackPointsBySegments(Uri data) {
        String[] projection = PROJECTION_V2;
        String typeQuery = " AND " + TYPE + " IN (-2, -1, 0, 1, 3)";
        if (protocolVersion < 2) { // fallback to old Dashboard API
            projection = PROJECTION_V1;
            typeQuery = "";
        }
        try (Cursor cursor = mContext.getContentResolver().query(data, projection, _ID + "> ?" + typeQuery, new String[]{Long.toString(lastTrackPointId)}, null)) {
            while (cursor.moveToNext()) {
                lastTrackPointId = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
                long trackId = cursor.getLong(cursor.getColumnIndexOrThrow(TRACKID));
                double latitude = cursor.getInt(cursor.getColumnIndexOrThrow(LATITUDE)) / LAT_LON_FACTOR;
                double longitude = cursor.getInt(cursor.getColumnIndexOrThrow(LONGITUDE)) / LAT_LON_FACTOR;
                int typeIndex = cursor.getColumnIndex(TYPE);
                double speed = cursor.getDouble(cursor.getColumnIndexOrThrow(SPEED));
                double elevation = cursor.getDouble(cursor.getColumnIndexOrThrow(ELEVATION));
                Date time = Date.from(Instant.ofEpochMilli(cursor.getLong(cursor.getColumnIndexOrThrow(TIME))));

                int type = 0;
                if (typeIndex > -1) {
                    type = cursor.getInt(typeIndex);
                }

                if (lastTrackId != trackId) {
                    activityTrack.startNewSegment();
                    lastTrackId = trackId;
                }

                LOG.debug("Trackpoint received from OpenTracks: {}/{} type={} speed={} elev={} time={}", latitude, longitude, type, speed, elevation, time);

                ActivityPoint activityPoint = new ActivityPoint();
                activityPoint.setTime(time);
                activityPoint.setSpeed((float) speed);
                if ((latitude != 0 || longitude != 0) && latitude != PAUSE_LATITUDE) {
                    activityPoint.setLocation(new GPSCoordinate(longitude, latitude, elevation));
                } else if (previousActivityPoint != null && previousActivityPoint.getLocation() != null && (type == 3 || latitude == PAUSE_LATITUDE)) {
                    activityPoint.setLocation(previousActivityPoint.getLocation());
                }
                if (activityPoint.getLocation() != null) {
                    activityTrack.addTrackPoint(activityPoint);
                }
                previousActivityPoint = activityPoint;
            }
        } catch (Exception e) {
            LOG.error("Couldn't read trackpoints from OpenTracks URI", e);
        }
    }
}

