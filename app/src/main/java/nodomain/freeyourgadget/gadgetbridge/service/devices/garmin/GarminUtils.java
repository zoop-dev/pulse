package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.location.Location;
import android.os.Build;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;

public final class GarminUtils {
    private GarminUtils() {
        // utility class
    }

    /** Watch lat/lon resolution: 180° spans the signed-32-bit range, so 1 semicircle = 180/2^31 degrees. */
    public static final double SEMICIRCLE_DEGREES = 180.0D / 0x80000000L;

    public static double semicirclesToDegrees(final int semicircles) {
        return semicircles * SEMICIRCLE_DEGREES;
    }

    public static int degreesToSemicircles(final double degrees) {
        return (int) Math.round(degrees / SEMICIRCLE_DEGREES);
    }

    public static GdiCore.CoreService.LocationData toLocationData(final Location location, final GdiCore.CoreService.DataType dataType) {
        final GdiCore.CoreService.LatLon positionForWatch = GdiCore.CoreService.LatLon.newBuilder()
                .setLat(degreesToSemicircles(location.getLatitude()))
                .setLon(degreesToSemicircles(location.getLongitude()))
                .build();

        float vAccuracy = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vAccuracy = location.getVerticalAccuracyMeters();
        }

        return GdiCore.CoreService.LocationData.newBuilder()
                .setPosition(positionForWatch)
                .setAltitude((float) location.getAltitude())
                .setTimestamp(GarminTimeUtils.javaMillisToGarminTimestamp(location.getTime()))
                .setHAccuracy(location.getAccuracy())
                .setVAccuracy(vAccuracy)
                .setPositionType(dataType)
                .setBearing(location.getBearing())
                .setSpeed(location.getSpeed())
                .build();
    }
}
