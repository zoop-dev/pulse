package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxNavigationPoint extends GPSCoordinate {
    private final Double distanceFromStart;
    private final Date time;

    private final int navigationInstruction;
    @Nullable
    private final String extraNavigationInstruction;

    private final int roundaboutExitNumber;

    public GpxNavigationPoint(final double longitude, final double latitude, final Double distanceFromStart, final Date time, final int navigationInstruction, @Nullable final String extraNavigationInstruction, final int roundaboutExitNumber) {
        super(longitude, latitude);
        this.distanceFromStart = distanceFromStart;
        this.time = time;
        this.navigationInstruction = navigationInstruction;
        this.extraNavigationInstruction = extraNavigationInstruction;
        this.roundaboutExitNumber = roundaboutExitNumber;
    }

    public Double getDistanceFromStart() {
        return distanceFromStart;
    }

    public Date getTime() {
        return time;
    }

    public int getNavigationInstruction() {
        return navigationInstruction;
    }

    @Nullable
    public String getExtraNavigationInstruction() {
        return extraNavigationInstruction;
    }

    public int getRoundaboutExitNumber() {
        return roundaboutExitNumber;
    }

    @NonNull
    @Override
    public String toString() {
        return "lon: " + formatLocation(getLongitude()) + ", lat: " + formatLocation(getLatitude()) + ", distance: " + distanceFromStart + "m, time: " + time.toString() + ", instruction type: " + navigationInstruction;
    }

    private String formatLocation(double value) {
        return new BigDecimal(value).setScale(8, RoundingMode.HALF_UP).toPlainString();
    }
}
