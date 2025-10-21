/*  Copyright (C) 2023-2025 José Rebelo, Thomas Kuehne

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
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxTrackPoint extends GPSCoordinate {
    @Nullable
    private final String name;
    @Nullable
    private final String symbol;
    @Nullable
    private final String description;
    private final Date time;
    private final int heartRate;
    private final float speed;
    private final int cadence;
    private final float temperature;
    private final float depth;

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time) {
        this(longitude, latitude, altitude, time, -1);
    }

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude, final Date time, final int heartRate) {
        this(longitude, latitude, altitude, time, null, null, null, Double.NaN, Double.NaN, Double.NaN, heartRate, -1, -1, Float.NaN, Float.NaN);
    }

    public GpxTrackPoint(final double longitude, final double latitude, final double altitude,
                         final Date time, final String name, final String description,
                         final String symbol, double hdop, double vdop, double pdop,
                         final int heartRate, final float speed, final int cadence,
                         final float temperature, final float depth) {
        super(longitude, latitude, altitude, hdop, vdop, pdop);
        this.name = name;
        this.symbol = symbol;
        this.description = description;
        this.time = time;
        this.heartRate = heartRate;
        this.speed = speed;
        this.cadence = cadence;
        this.temperature = temperature;
        this.depth = depth;
    }

    @Nullable
    public String getName() {return name;}

    @Nullable
    public String getSymbol() {return symbol;}

    @Nullable
    public String getDescription() {return description;}

    @Nullable
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
    public float getTemperature() {
        return temperature;
    }
    public float getDepth() {
        return depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GpxTrackPoint that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(time, that.time) &&
                Objects.equals(heartRate, that.heartRate) &&
                Objects.equals(cadence, that.cadence) &&
                Objects.equals(speed, that.speed) &&
                Objects.equals(name, that.name) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(description, that.description) &&
                Objects.equals(temperature, that.temperature) &&
                Objects.equals(depth, that.depth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), time, heartRate, speed, cadence, name, symbol, description, temperature, depth);
    }

    @NonNull
    @Override
    public String toString() {
        return "ts: " + (time == null ? null : time.getTime()) + ", " + super.toString() + ", heartRate: " + heartRate + ", speed: " + speed + ", cadence: " + cadence;
    }

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(time);
        activityPoint.setLocation(this);
        activityPoint.setHeartRate(heartRate);
        if (!Float.isNaN(speed)) {
            activityPoint.setSpeed(speed);
        }
        activityPoint.setCadence(cadence);
        if (!Double.isNaN(depth)) {
            activityPoint.setDepth(depth);
        }
        if (!Double.isNaN(temperature)) {
            activityPoint.setTemperature(temperature);
        }
        if (description != null && description.length() > 0) {
            activityPoint.setDescription(description);
        }

        return activityPoint;
    }
}
