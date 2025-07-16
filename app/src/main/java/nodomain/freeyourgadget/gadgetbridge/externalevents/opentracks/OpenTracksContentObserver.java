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
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;


public class OpenTracksContentObserver extends ContentObserver {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTracksContentObserver.class);

    private Context mContext;
    private Uri tracksUri;
    private Uri trackpointsUri;
    private int protocolVersion;
    private int totalTimeMillis;
    private float totalDistanceMeter;
    private final List<ActivityPoint> activityPoints;

    private long previousTimeMillis = 0;
    private float previousDistanceMeter = 0;
    private long lastTrackPointId;

    public int getTotalTimeMillis() {
        return totalTimeMillis;
    }
    public float getTotalDistanceMeter() {
        return totalDistanceMeter;
    }
    public List<ActivityPoint> getActivityPoints() {
        return activityPoints;
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
        this.activityPoints = new ArrayList<>();

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
            final TrackPointsBySegments trackPointsBySegments = TrackPoint.readTrackPointsBySegments(mContext.getContentResolver(), trackpointsUri, lastTrackPointId, protocolVersion);
            if (!trackPointsBySegments.isEmpty()) {
                for (List<TrackPoint> segment : trackPointsBySegments.segments()) {
                    for (TrackPoint trackPoint : segment) {
                        lastTrackPointId = trackPoint.getTrackPointId();
                        ActivityPoint activityPoint = new ActivityPoint();
                        activityPoint.setLocation(new GPSCoordinate(trackPoint.getLongitude(), trackPoint.getLatitude()));
                        activityPoint.setTime(new Date());
                        activityPoints.add(activityPoint);
                        LOG.debug("Trackpoint received from OpenTracks: {}/{}", trackPoint.getLatitude(), trackPoint.getLongitude());
                    }
                }
            }
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
}

