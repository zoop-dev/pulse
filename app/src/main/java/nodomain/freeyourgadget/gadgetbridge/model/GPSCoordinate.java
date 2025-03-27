/*  Copyright (C) 2017-2024 Carsten Pfeiffer, José Rebelo, Petr Vaněk

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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

public class GPSCoordinate implements Parcelable {
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private double hdop;
    private double vdop;
    private double pdop;

    public static final double UNKNOWN_ALTITUDE = -20000d;
    public static final double UNKNOWN_DOP = -1d;

    public static final int GPS_DECIMAL_DEGREES_SCALE = 6; // precise to 111.132mm at equator: https://en.wikipedia.org/wiki/Decimal_degrees

    public GPSCoordinate(double longitude, double latitude, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.hdop = UNKNOWN_DOP;
        this.vdop = UNKNOWN_DOP;
        this.pdop = UNKNOWN_DOP;
    }

    public GPSCoordinate(double longitude, double latitude) {
        this(longitude, latitude, UNKNOWN_ALTITUDE);
    }

    protected GPSCoordinate(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        altitude = in.readDouble();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setHdop(double hdop) {
        this.hdop = hdop;
    }

    public boolean hasHdop() {
        return (Double.compare(hdop, UNKNOWN_DOP) > 0);
    }

    public double getHdop() { return hdop; }

    public void setVdop(double vdop) {
        this.vdop = vdop;
    }

    public boolean hasVdop() {
        return (Double.compare(vdop, UNKNOWN_DOP) > 0);
    }

    public double getVdop() { return vdop; }

    public void setPdop(double pdop) {
        this.pdop = pdop;
    }

    public boolean hasPdop() {
        return (Double.compare(pdop, UNKNOWN_DOP) > 0);
    }

    public double getPdop() { return pdop; }

    public double getDistance(GPSCoordinate source) {
        final Location end = new Location("end");
        end.setLatitude(this.getLatitude());
        end.setLongitude(this.getLongitude());

        final Location start = new Location("start");
        start.setLatitude(source.getLatitude());
        start.setLongitude(source.getLongitude());

        return end.distanceTo(start);
    }

    public double getAltitudeDifference(GPSCoordinate source) {
        if (this.getAltitude() == UNKNOWN_ALTITUDE)
            return 0;
        if (source.getAltitude() == UNKNOWN_ALTITUDE)
            return 0;
        return this.getAltitude() - source.getAltitude();
    }

    public double getAscent(GPSCoordinate source) {
        return Math.max(0, this.getAltitudeDifference(source));
    }

    public double getDescent(GPSCoordinate source) {
        return Math.max(0, -this.getAltitudeDifference(source));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GPSCoordinate that = (GPSCoordinate) o;

        if (Double.compare(that.getLatitude(), getLatitude()) != 0) return false;
        if (Double.compare(that.getLongitude(), getLongitude()) != 0) return false;
        return Double.compare(that.getAltitude(), getAltitude()) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getLatitude());
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getLongitude());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getAltitude());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    private String formatLocation(double value) {
        return new BigDecimal(value).setScale(8, RoundingMode.HALF_UP).toPlainString();
    }

    @NonNull
    @Override
    public String toString() {
        return "lon: " + formatLocation(longitude) + ", lat: " + formatLocation(latitude) + ", alt: " + formatLocation(altitude) + "m";
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(altitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GPSCoordinate> CREATOR = new Creator<GPSCoordinate>() {
        @Override
        public GPSCoordinate createFromParcel(Parcel in) {
            return new GPSCoordinate(in);
        }

        @Override
        public GPSCoordinate[] newArray(int size) {
            return new GPSCoordinate[size];
        }
    };

    public static class compareLatitude implements Comparator<GPSCoordinate> {
        @Override
        public int compare(GPSCoordinate trkPt1, GPSCoordinate trkPt2) {
            return Double.compare(trkPt1.getLatitude(), trkPt2.getLatitude());
        }
    }

    public static class compareLongitude implements Comparator<GPSCoordinate> {
        @Override
        public int compare(GPSCoordinate trkPt1, GPSCoordinate trkPt2) {
            return Double.compare(trkPt1.getLongitude(), trkPt2.getLongitude());
        }
    }

    public static class compareElevation implements Comparator<GPSCoordinate> {
        @Override
        public int compare(GPSCoordinate trkPt1, GPSCoordinate trkPt2) {
            return Double.compare(trkPt1.getAltitude(), trkPt2.getAltitude());
        }
    }

}
