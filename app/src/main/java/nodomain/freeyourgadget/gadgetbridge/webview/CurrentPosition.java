/*  Copyright (C) 2017-2024 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.webview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CurrentPosition {

    private static final Logger LOG = LoggerFactory.getLogger(CurrentPosition.class);

    private Location lastKnownLocation;
    private float latitude, longitude;

    long timestamp;
    double altitude;
    float accuracy, speed;

    float getLatitude() {
        return latitude;
    }

    float getLongitude() {
        return longitude;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    /**
     * Whether the position came from the static location preferences, as opposed to an
     * actual location from a location provider.
     */
    public boolean isFromPreferences() {
        return "preferences".equals(lastKnownLocation.getProvider());
    }

    public CurrentPosition() {
        Prefs prefs = GBApplication.getPrefs();
        this.latitude = prefs.getFloat("location_latitude", 0);
        this.longitude = prefs.getFloat("location_longitude", 0);

        lastKnownLocation = new Location("preferences");
        lastKnownLocation.setLatitude(this.latitude);
        lastKnownLocation.setLongitude(this.longitude);

        this.timestamp = System.currentTimeMillis() - 86400000; //let accessor know this value is really old

        if (prefs.getBoolean("use_updated_location_if_available", false)) {
            if (ActivityCompat.checkSelfPermission(GBApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager locationManager = (LocationManager) GBApplication.getContext().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = null;
                if (locationManager != null) {
                    provider = locationManager.getBestProvider(criteria, false);
                }
                if (provider != null) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                    if (lastKnownLocation != null) {
                        this.lastKnownLocation = lastKnownLocation;
                        this.timestamp = lastKnownLocation.getTime();
                        this.timestamp = System.currentTimeMillis() - 1000; //TODO: request updating the location and don't fake its age

                        this.latitude = (float) lastKnownLocation.getLatitude();
                        this.longitude = (float) lastKnownLocation.getLongitude();
                        this.accuracy = lastKnownLocation.getAccuracy();
                        this.altitude = (float) lastKnownLocation.getAltitude();
                        this.speed = lastKnownLocation.getSpeed();
                    }
                }
            } else {
                LOG.warn("use_updated_location_if_available is enabled, but location permission is not granted - falling back to the static location from preferences");
            }
        }

        if (isFromPreferences()) {
            LOG.info("got latitude/longitude from preferences: {}/{}", latitude, longitude);
        } else {
            LOG.info("got latitude/longitude from last known location: {}/{}", latitude, longitude);
        }
    }
}
