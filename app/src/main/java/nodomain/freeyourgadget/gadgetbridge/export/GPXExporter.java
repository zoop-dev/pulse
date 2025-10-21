/*  Copyright (C) 2017-2025 Andreas Shimokawa, AndrewH, Carsten Pfeiffer,
    Daniele Gobbetti, Dikay900, José Rebelo, Nick Spacek, Petr Vaněk, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.export;

import android.util.Xml;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class GPXExporter implements ActivityTrackExporter {
    private static final String NS_GPX_URI = "http://www.topografix.com/GPX/1/1";
    private static final String NS_GPX_PREFIX = "";
    private static final String NS_TRACKPOINT_EXTENSION = "gpxtpx";
    private static final String NS_TRACKPOINT_EXTENSION_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v2";
    private static final String NS_XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String TRACKPOINT_EXTENSION_XSD = "http://www.garmin.com/xmlschemas/TrackPointExtensionv2.xsd";
    private static final String TOPOGRAFIX_NAMESPACE_XSD = "http://www.topografix.com/GPX/1/1/gpx.xsd";
    private static final String OPENTRACKS_PREFIX = "opentracks";
    private static final String OPENTRACKS_NAMESPACE_URI = "http://opentracksapp.com/xmlschemas/v1";
    private static final String OPENTRACKS_XSD = "https://raw.githubusercontent.com/OpenTracksApp/OpenTracks/main/doc/opentracks-schema-1.0.xsd";

    private String creator;
    private Date date;
    private boolean includeHeartRate = true;
    private boolean includeHeartRateOfNearestSample = true;
    private UUID uuid;

    private final DecimalFormat doubleFormat;
    private final DecimalFormat locationFormat;

    public GPXExporter(){
        doubleFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        doubleFormat.setMaximumFractionDigits(15);
        doubleFormat.setRoundingMode(RoundingMode.HALF_UP);

        locationFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        locationFormat.setMinimumFractionDigits(GPSCoordinate.GPS_DECIMAL_DEGREES_SCALE);
        locationFormat.setMaximumFractionDigits(GPSCoordinate.GPS_DECIMAL_DEGREES_SCALE);
        locationFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Override
    public void performExport(ActivityTrack track, File targetFile) throws IOException, GPXTrackEmptyException {
        String encoding = StandardCharsets.UTF_8.name();
        XmlSerializer ser = Xml.newSerializer();
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            ser.setOutput(outputStream, encoding);
            ser.startDocument(encoding, Boolean.TRUE);
            ser.setPrefix("xsi", NS_XSI_URI);
            ser.setPrefix(NS_TRACKPOINT_EXTENSION, NS_TRACKPOINT_EXTENSION_URI);
            ser.setPrefix(NS_GPX_PREFIX, NS_GPX_URI);
            ser.setPrefix(OPENTRACKS_PREFIX, OPENTRACKS_NAMESPACE_URI);

            ser.startTag(NS_GPX_URI, "gpx");
            ser.attribute(null, "version", "1.1");
            if (creator != null) {
                ser.attribute(null, "creator", creator);
            } else {
                ser.attribute(null, "creator", GBApplication.app().getNameAndVersion());
            }
            ser.attribute(NS_XSI_URI, "schemaLocation",NS_GPX_URI + " " + TOPOGRAFIX_NAMESPACE_XSD
                    + " " + NS_TRACKPOINT_EXTENSION_URI + " " + TRACKPOINT_EXTENSION_XSD
                    + " " + OPENTRACKS_NAMESPACE_URI + " " + OPENTRACKS_XSD);

            exportMetadata(ser, track);
            exportTrack(ser, track);

            ser.endTag(NS_GPX_URI, "gpx");
            ser.endDocument();
            ser.flush();
        }
    }

    private void exportMetadata(XmlSerializer ser, ActivityTrack track) throws IOException {
        ser.startTag(NS_GPX_URI, "metadata");
        if (track.getName() != null) {
            ser.startTag(NS_GPX_URI, "name").text(track.getName()).endTag(NS_GPX_URI, "name");
        }

        final User user = track.getUser();
        if (user != null) {
            ser.startTag(NS_GPX_URI, "author");
            ser.startTag(NS_GPX_URI, "name").text(user.getName()).endTag(NS_GPX_URI, "name");
            ser.endTag(NS_GPX_URI, "author");
        }

        Date time = date;
        if (time == null){
            time = new Date();
        }
        ser.startTag(NS_GPX_URI, "time").text(formatTime(time)).endTag(NS_GPX_URI, "time");

        ser.endTag(NS_GPX_URI, "metadata");
    }

    private String formatTime(Date date) {
        return DateTimeUtils.formatIso8601UTC(date);
    }

    private void exportTrack(XmlSerializer ser, ActivityTrack track) throws IOException, GPXTrackEmptyException {
        String uuid = ((this.uuid != null) ? this.uuid : UUID.randomUUID()).toString();
        ser.startTag(NS_GPX_URI, "trk");
        ser.startTag(NS_GPX_URI, "extensions");
        ser.startTag(NS_GPX_URI, OPENTRACKS_PREFIX + ":trackid").text(uuid).endTag(NS_GPX_URI, OPENTRACKS_PREFIX + ":trackid");
        ser.endTag(NS_GPX_URI, "extensions");

        List<List<ActivityPoint>> segments = track.getSegments();
        boolean atLeastOnePointExported = false;
        for (List<ActivityPoint> segment : segments) {
            if (segment.isEmpty()) {
                // Skip empty segments
                continue;
            }

            ser.startTag(NS_GPX_URI, "trkseg");
            for (ActivityPoint point : segment) {
                atLeastOnePointExported |= exportTrackPoint(ser, point, segment);
            }
            ser.endTag(NS_GPX_URI, "trkseg");
        }

        if (!atLeastOnePointExported) {
            throw new GPXTrackEmptyException();
        }

        ser.endTag(NS_GPX_URI, "trk");
    }

    private boolean exportTrackPoint(XmlSerializer ser, ActivityPoint point, Iterable<ActivityPoint> trackPoints) throws IOException {
        GPSCoordinate location = point.getLocation();
        if (location == null) {
            return false; // skip invalid points, that just contain hr data, for example
        }
        ser.startTag(NS_GPX_URI, "trkpt");
        // lon and lat attributes do not have an explicit namespace
        ser.attribute(null, "lon", formatLocation(location.getLongitude()));
        ser.attribute(null, "lat", formatLocation(location.getLatitude()));
        if (location.hasAltitude()) {
            ser.startTag(NS_GPX_URI, "ele").text(formatDouble(location.getAltitude())).endTag(NS_GPX_URI, "ele");
        }
        Date time = point.getTime();
        if (time != null) {
            ser.startTag(NS_GPX_URI, "time").text(formatTime(time)).endTag(NS_GPX_URI, "time");
        }
        String description = point.getDescription();
        if (description != null) {
            ser.startTag(NS_GPX_URI, "desc").text(description).endTag(NS_GPX_URI, "desc");
        }
        //ser.startTag(NS_GPX_URI, "src").text(source).endTag(NS_GPX_URI, "src");
        if (location.hasHdop()) {
            ser.startTag(NS_GPX_URI, "hdop").text(formatDouble(location.getHdop())).endTag(NS_GPX_URI, "hdop");
        }
        if (location.hasVdop()) {
            ser.startTag(NS_GPX_URI, "vdop").text(formatDouble(location.getVdop())).endTag(NS_GPX_URI, "vdop");
        }
        if (location.hasPdop()) {
            ser.startTag(NS_GPX_URI, "pdop").text(formatDouble(location.getPdop())).endTag(NS_GPX_URI, "pdop");
        }

        exportTrackpointExtensions(ser, point, trackPoints);

        ser.endTag(NS_GPX_URI, "trkpt");

        return true;
    }

    private void exportTrackpointExtensions(XmlSerializer ser, ActivityPoint point, Iterable<ActivityPoint> trackPoints) throws IOException {
        if (!includeHeartRate) {
            return;
        }

        double temperature = point.getTemperature();
        double depth = point.getDepth();
        float speed = point.getSpeed();
        int cadence = point.getCadence();
        int hr = point.getHeartRate();
        if (!HeartRateUtils.getInstance().isValidHeartRateValue(hr) && includeHeartRateOfNearestSample) {

            ActivityPoint closestPointItem = findClosestSensibleActivityPoint(point.getTime(), trackPoints);
            if (closestPointItem != null) {
                hr = closestPointItem.getHeartRate();
            }

        }

        boolean exportHr = HeartRateUtils.getInstance().isValidHeartRateValue(hr) && includeHeartRate;
        boolean exportCadence = cadence >= 0;
        boolean exportSpeed = speed >= 0.0f;
        boolean exportTemperature = temperature > -273.0;
        boolean exportDepth = !Double.isNaN(depth) && depth != -1.0;

        if (!(exportHr || exportCadence || exportSpeed || exportTemperature || exportDepth)) {
            // No valid data to export in extensions
            return;
        }

        ser.startTag(NS_GPX_URI, "extensions");
        ser.setPrefix(NS_TRACKPOINT_EXTENSION, NS_TRACKPOINT_EXTENSION_URI);
        ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "TrackPointExtension");
        if (exportTemperature) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "atemp").text(formatDouble(temperature)).endTag(NS_TRACKPOINT_EXTENSION_URI, "atemp");
        }
        if (exportDepth) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "depth").text(formatDouble(depth)).endTag(NS_TRACKPOINT_EXTENSION_URI, "depth");
        }
        if (exportHr) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "hr").text(formatLong(hr)).endTag(NS_TRACKPOINT_EXTENSION_URI, "hr");
        }
        if (exportCadence) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "cad").text(formatLong(cadence)).endTag(NS_TRACKPOINT_EXTENSION_URI, "cad");
        }
        if (exportSpeed) {
            ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "speed").text(formatDouble(speed)).endTag(NS_TRACKPOINT_EXTENSION_URI, "speed");
        }
        ser.endTag(NS_TRACKPOINT_EXTENSION_URI, "TrackPointExtension");
        ser.endTag(NS_GPX_URI, "extensions");
    }

    private @Nullable ActivityPoint findClosestSensibleActivityPoint(Date time, Iterable<ActivityPoint> trackPoints) {
        if (time == null) {
            return null;
        }
        ActivityPoint closestPointItem = null;
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        long lowestDifference = 60 * 2 * 1000; // minimum distance is 2min
        for (ActivityPoint pointItem : trackPoints) {
            int hrItem = pointItem.getHeartRate();
            if (heartRateUtilsInstance.isValidHeartRateValue(hrItem)) {
                Date timeItem = pointItem.getTime();
                if (timeItem == null) {
                    continue;
                }
                if (timeItem.after(time) || timeItem.equals(time)) {
                    break; // we assume that the given trackPoints are sorted in time ascending order (oldest first)
                }
                long difference = time.getTime() - timeItem.getTime();
                if (difference < lowestDifference) {
                    lowestDifference = difference;
                    closestPointItem = pointItem;
                }
            }
        }
        return closestPointItem;
    }

    private String formatLocation(double value) {
        return locationFormat.format(value);
    }

    private String formatDouble(double value) {
        return doubleFormat.format(value);
    }

    private String formatLong(long value) {
        return NumberFormat.getNumberInstance(Locale.ROOT).format(value);
    }

    public String getCreator() {
        return creator; // TODO: move to some kind of BrandingInfo class
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setIncludeHeartRate(boolean includeHeartRate) {
        this.includeHeartRate = includeHeartRate;
    }

    public boolean isIncludeHeartRate() {
        return includeHeartRate;
    }

    @TestOnly
    public void setUuid(@Nullable UUID id) {
        uuid = id;
    }
}
