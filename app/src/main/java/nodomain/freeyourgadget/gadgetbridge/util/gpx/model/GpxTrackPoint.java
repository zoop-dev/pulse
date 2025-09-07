/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxTrackPoint extends GPSCoordinate {
    private final Date time;
    private final int heartRate;
    private final float speed;
    private final int cadence;

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time) {
        this(longitude, latitude, altitude, time, -1);
    }

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time, final int heartRate) {
        this(longitude, latitude, altitude, time, heartRate, -1, -1);
    }

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time, final int heartRate, final float speed, final int cadence) {
        super(longitude, latitude, altitude);
        this.time = time;
        this.heartRate = heartRate;
        this.speed = speed;
        this.cadence = cadence;
    }

    public Date getTime() {
        return time;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public float getSpeed() {
        return speed;
    }

    public int getCadence() {
        return cadence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GpxTrackPoint that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(time, that.time) &&
                Objects.equals(heartRate, that.heartRate) &&
                Objects.equals(cadence, that.cadence) &&
                Objects.equals(speed, that.speed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), time, heartRate, speed, cadence);
    }

    @NonNull
    @Override
    public String toString() {
        return "ts: " + time.getTime() + ", " + super.toString() + ", heartRate: " + heartRate + ", speed: " + speed + ", cadence: " + cadence;
    }

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(time);
        activityPoint.setLocation(this);
        activityPoint.setHeartRate(heartRate);
        activityPoint.setSpeed(speed);
        activityPoint.setCadence(cadence);

        return activityPoint;
    }

    public static class Builder {
        private double longitude;
        private double latitude;
        private double altitude;
        private Date time;
        private int heartRate = -1;
        private float speed = -1;
        private int cadence = -1;

        public Builder withLongitude(final double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder withLatitude(final double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder withAltitude(final double altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder withTime(final Date time) {
            this.time = time;
            return this;
        }

        public Builder withHeartRate(final int heartRate) {
            this.heartRate = heartRate;
            return this;
        }

        public Builder withSpeed(final float speed) {
            this.speed = speed;
            return this;
        }

        public Builder withCadence(final int cadence) {
            this.cadence = cadence;
            return this;
        }

        public GpxTrackPoint build() {
            return new GpxTrackPoint(longitude, latitude, altitude, time, heartRate, speed, cadence);
        }
    }
}
