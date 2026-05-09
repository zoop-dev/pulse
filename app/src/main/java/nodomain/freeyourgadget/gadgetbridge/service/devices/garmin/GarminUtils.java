package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.location.Location;
import android.os.Build;

import androidx.annotation.Nullable;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;

public final class GarminUtils {
    private GarminUtils() {
        // utility class
    }

    /** Watch lat/lon resolution: 180° spans the signed-32-bit range, so 1 semicircle = 180/2^31 degrees. */
    public static final double SEMICIRCLE_DEGREES = 180.0D / 0x80000000L;

    /** Sortable, locale-independent timestamp embedded in Garmin file/GPX names. */
    public static final DateTimeFormatter FILENAME_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
                    .withZone(ZoneId.systemDefault());

    /** Year subdirectory used in the device's writable export tree. */
    public static final DateTimeFormatter FILENAME_YEAR_FORMAT =
            DateTimeFormatter.ofPattern("yyyy", Locale.ROOT)
                    .withZone(ZoneId.systemDefault());

    /**
     * Build the standard Garmin export path:
     * {@code [TYPE]/[yyyy]/[TYPE]_[yyyy-MM-dd_HH-mm-ss]_[suffix].[extension]}.
     * Both the year subfolder and the timestamp body are skipped when
     * {@code date} is null, yielding the missing-date fallback shape
     * {@code [TYPE]/[TYPE]_[suffix].[extension]}.
     * Suffix is opaque — callers pass the FIT file index, our UUID
     * hex prefix, etc.; pass {@code ""} to omit the segment entirely.
     */
    public static String buildExportPath(@Nullable final FileType.FILETYPE type, @Nullable final Date date,
                                         final String suffix, final String extension) {
        // Type can be null when a FIT file's file_id.type field is missing
        // or unrecognised; fall back to a fixed placeholder so both the
        // directory and filename segments stay consistent.
        final String typeName = type != null ? type.name() : "NULL";
        final StringBuilder sb = new StringBuilder();
        sb.append(typeName).append(File.separator);
        if (date != null) {
            sb.append(FILENAME_YEAR_FORMAT.format(date.toInstant())).append(File.separator);
        }
        sb.append(typeName);
        if (date != null) {
            sb.append('_').append(FILENAME_TIMESTAMP_FORMAT.format(date.toInstant()));
        }
        if (!suffix.isEmpty()) {
            sb.append('_').append(suffix);
        }
        sb.append('.').append(extension);
        return sb.toString();
    }

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
