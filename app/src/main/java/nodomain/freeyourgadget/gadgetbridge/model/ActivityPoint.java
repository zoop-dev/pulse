/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Date;
import java.util.Objects;

import androidx.annotation.Nullable;

// https://www8.garmin.com/xmlschemas/TrackPointExtensionv1.xsd
/*
<trkpt lat="54.8591470" lon="-1.5754310">
        <ele>29.2</ele>
        <time>2015-07-26T07:43:42Z</time>
        <extensions>
            <gpxtpx:TrackPointExtension>
                <gpxtpx:atemp>11</gpxtpx:atemp>
                <gpxtpx:hr>92</gpxtpx:hr>
                <gpxtpx:cad>0</gpxtpx:cad>
                <gpxtpx:speed>0.5</gpxtpx:speed>
            </gpxtpx:TrackPointExtension>
        </extensions>
        </trkpt>
*/
public class ActivityPoint {
    private Date time;
    private GPSCoordinate location;
    private int heartRate;
    private float speed = -1;
    private int strideCm = -1;
    private int cadence = -1;
    private int power = -1;
    private float respiratoryRate = -1;
    private double depth = -1;
    private double temperature = -273;

    // e.g. to describe a pause during the activity
    private @Nullable String description;

    public ActivityPoint() {
    }

    public ActivityPoint(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public GPSCoordinate getLocation() {
        return location;
    }

    public void setLocation(GPSCoordinate location) {
        this.location = location;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getStrideCm() {
        return strideCm;
    }

    public void setStrideCm(int strideCm) {
        this.strideCm = strideCm;
    }

    public int getCadence() {
        return cadence;
    }

    public void setCadence(final int cadence) {
        this.cadence = cadence;
    }

    public int getPower() {
        return power;
    }

    public void setPower(final int power) {
        this.power = power;
    }

    public float getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(final float respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public double getTemperature() {return temperature; }
    public void setTemperature(final double temperature) { this.temperature = temperature; }
    public double getDepth() {return depth; }
    public void setDepth(final double depth) { this.depth = depth; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActivityPoint that)) return false;
        return heartRate == that.heartRate &&
                Float.compare(speed, that.speed) == 0 &&
                strideCm == that.strideCm &&
                cadence == that.cadence &&
                power == that.power &&
                Float.compare(respiratoryRate, that.respiratoryRate) == 0 &&
                Double.compare(depth, that.depth) == 0 &&
                Double.compare(temperature, that.temperature) == 0 &&
                Objects.equals(time, that.time) &&
                Objects.equals(location, that.location) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, location, heartRate, speed, strideCm, cadence, power, respiratoryRate, depth, temperature, description);
    }

    public static class Builder {
        private long timeMillis;

        private double latitude;
        private double longitude;
        private double altitude = GPSCoordinate.UNKNOWN_ALTITUDE;

        private int heartRate;
        private float speed = -1;
        private int strideCm = -1;
        private int cadence = -1;
        private int power = -1;
        private float respiratoryRate = -1;
        private double depth = -1;
        private double temperature = -273;

        public long getTime() {
            return timeMillis;
        }

        public void setTime(final long timeMillis) {
            this.timeMillis = timeMillis;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(final double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(final double longitude) {
            this.longitude = longitude;
        }

        public double getAltitude() {
            return altitude;
        }

        public void setAltitude(final double altitude) {
            this.altitude = altitude;
        }

        public int getHeartRate() {
            return heartRate;
        }

        public void setHeartRate(final int heartRate) {
            this.heartRate = heartRate;
        }

        public float getSpeed() {
            return speed;
        }

        public void setSpeed(final float speed) {
            this.speed = speed;
        }

        public int getStrideCm() {
            return strideCm;
        }

        public void setStrideCm(int strideCm) {
            this.strideCm = strideCm;
        }

        public int getCadence() {
            return cadence;
        }

        public void setCadence(final int cadence) {
            this.cadence = cadence;
        }

        public int getPower() {
            return power;
        }

        public void setPower(final int power) {
            this.power = power;
        }

        public float getRespiratoryRate() {
            return respiratoryRate;
        }

        public void setRespiratoryRate(final float respiratoryRate) {
            this.respiratoryRate = respiratoryRate;
        }

        public double getDepth() {
            return depth;
        }

        public void setDepth(final double depth) {
            this.depth = depth;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(final double temperature) {
            this.temperature = temperature;
        }

        public ActivityPoint build() {
            final ActivityPoint activityPoint = new ActivityPoint();
            activityPoint.setTime(new Date(timeMillis));
            if (latitude != 0 && longitude != 0) {
                activityPoint.setLocation(new GPSCoordinate(longitude, latitude, altitude));
            }
            activityPoint.setHeartRate(heartRate);
            activityPoint.setSpeed(speed);
            activityPoint.setCadence(cadence);
            activityPoint.setStrideCm(strideCm);
            activityPoint.setPower(power);
            activityPoint.setRespiratoryRate(respiratoryRate);
            activityPoint.setDepth(depth);
            activityPoint.setTemperature(temperature);
            return activityPoint;
        }
    }
}
