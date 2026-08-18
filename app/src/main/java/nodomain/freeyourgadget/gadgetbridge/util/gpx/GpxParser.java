/*  Copyright (C) 2023-2026 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx;

import androidx.annotation.Nullable;

import com.google.gson.internal.bind.util.ISO8601Utils;

import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxNavigationPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackSegment;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxWaypoint;

public class GpxParser {
    private static final Logger LOG = LoggerFactory.getLogger(GpxParser.class);

    public static final byte[][] XML_HEADER = {
            {'<', '?', 'x', 'm', 'l'},
            {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<', '?', 'x', 'm', 'l'}, // UTF-8 with BOM
            {'<', 0, '?', 0, 'x', 0, 'm', 0, 'l', 0},  // UTF-16 LE
            {(byte) 0xFF, (byte) 0xFE, '<', 0, '?', 0, 'x', 0, 'm', 0, 'l', 0}, // UTF-16 LE with BOM
            {0, '<', 0, '?', 0, 'x', 0, 'm', 0, 'l'}, // UTF-16 BE
            {(byte) 0xFE, (byte) 0xFF, 0, '<', 0, '?', 0, 'x', 0, 'm', 0, 'l'} // UTF-16 BE with BOM
    };

    // Some gpx files start with "<gpx" directly...
    public static final byte[][] GPX_START = {
            {'<', 'g', 'p', 'x'},
            {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<', 'g', 'p', 'x'}, // UTF-8 with BOM
            {'<', 0, 'g', 0, 'p', 0, 'x', 0}, // UTF-16 LE
            {(byte) 0xFF, (byte) 0xFE, '<', 0, 'g', 0, 'p', 0, 'x', 0}, // UTF-16 LE with BOM
            {0, '<', 0, 'g', 0, 'p', 0, 'x'}, // UTF-16 BE
            {(byte) 0xFE, (byte) 0xFF, 0, '<', 0, 'g', 0, 'p', 0, 'x'} // UTF-16 BE with BOM
    };

    private final XmlPullParser parser;
    private int eventType;

    private final GpxFile.Builder fileBuilder;


    @Nullable
    public static GpxFile parseGpx(final byte[] xmlBytes) {
        if (!isGpxFile(xmlBytes)) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
            final GpxParser gpxParser = new GpxParser(bais);
            return gpxParser.getGpxFile();
        } catch (final IOException e) {
            LOG.error("Failed to read xml", e);
        } catch (final GpxParseException e) {
            LOG.error("Failed to parse gpx", e);
        }

        return null;
    }

    /// simplify the GPX for parsing
    @Nullable
    @TestOnly
    public static byte[] transformGpx(InputStream input){
        try {
            Source xmlSource = new StreamSource(input);
            try (final InputStream xsltStream = GBApplication.getContext().getResources().openRawResource(R.raw.gpx_xslt)) {
                Source xsltSource = new StreamSource(xsltStream);

                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer(xsltSource);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final StreamResult result = new StreamResult(outputStream);
                transformer.transform(xmlSource, result);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            LOG.error("transformGpx ", e);
            return null;
        }
    }

    public static boolean isGpxFile(final byte[] data) {
        for(byte[] header : XML_HEADER){
            if(ArrayUtils.equals(data, header, 0)){
                return true;
            }
        }

        for(byte[] start : GPX_START){
            if(ArrayUtils.equals(data, start, 0)){
                return true;
            }
        }
        return false;
    }

    public GpxParser(final InputStream stream) throws GpxParseException {
        this.fileBuilder = new GpxFile.Builder();

        try (InputStream simple = new ByteArrayInputStream(transformGpx(stream))){
            parser = createXmlParser(simple);
            parseGpx();
        } catch (final Exception e) {
            throw new GpxParseException("Failed to parse gpx", e);
        }
    }

    public GpxFile getGpxFile() {
        return fileBuilder.build();
    }

    private static XmlPullParser createXmlParser(InputStream stream) throws XmlPullParserException {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(stream, null);
        return parser;
    }

    private Date getDate(String attribute, Date defaultDate) throws Exception {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultDate;
        }
        // If no timezone indicator is present, assume UTC
        if (!text.endsWith("Z") && !text.contains("+") && !(text.length() > 19 && text.charAt(19) == '-')) {
            return ISO8601Utils.parse(text + "Z", new ParsePosition(0));
        }
        return ISO8601Utils.parse(text, new ParsePosition(0));
    }

    private double getDouble(String attribute, double defaultDouble) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultDouble;
        }
        // some GPX generators write NaN instead of an empty value
        final double value = Double.parseDouble(text);
        return Double.isNaN(value) ? defaultDouble : value;
    }

    private float getFloat(String attribute, float defaultFloat) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultFloat;
        }
        // some GPX generators write NaN instead of an empty value
        final float value = Float.parseFloat(text);
        return Float.isNaN(value) ? defaultFloat : value;
    }

    private int getInt(String attribute, int defaultInt) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultInt;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // some GPX generators write malformed cadence and HR values: "6.0" instead of "6"
            return Math.round(getFloat(attribute, defaultInt));
        }
    }

    private boolean getBoolean(String attribute, boolean defaultBoolean) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultBoolean;
        }

        return "true".equalsIgnoreCase(text);
    }

    private void parseGpx() throws Exception {
        for (eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                switch (name) {
                    case "gpx":
                        fileBuilder.withName(parser.getAttributeValue(null, "name"));
                        fileBuilder.withAuthor(parser.getAttributeValue(null, "author"));
                        fileBuilder.withTime(getDate("time", null));
                        break;
                    case "trk":
                        final GpxTrack track = parseTrack();
                        if (!track.isEmpty()) {
                            fileBuilder.withTrack(track);
                        }
                        break;
                    case "wpt":
                        final GpxWaypoint waypoint = new GpxWaypoint(
                                getDouble("lon", 0.0),
                                getDouble("lat", 0.0),
                                getDouble("ele", Double.NaN),
                                getDate("time", null),
                                parser.getAttributeValue(null, "name"),
                                parser.getAttributeValue(null, "desc"),
                                parser.getAttributeValue(null, "sym"),
                                getDouble("hdop", Double.NaN),
                                getDouble("vdop", Double.NaN),
                                getDouble("pdop", Double.NaN),
                                getFloat("temperature", Float.NaN),
                                getFloat("depth", Float.NaN)
                        );
                        fileBuilder.withWaypoints(waypoint);
                        break;
                }
            }
        }
    }

    private GpxTrack parseTrack() throws Exception {
        final GpxTrack.Builder trackBuilder = new GpxTrack.Builder();

        trackBuilder.withName(parser.getAttributeValue(null, "name"));
        trackBuilder.withType(parser.getAttributeValue(null, "type"));
        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("trk")) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "trkseg":
                        final GpxTrackSegment segment = parseTrackSegment();
                        if (!segment.getTrackPoints().isEmpty()) {
                            trackBuilder.withTrackSegment(segment);
                        }
                        continue;
                }
            }

            eventType = parser.next();
        }

        return trackBuilder.build();
    }

    private GpxTrackSegment parseTrackSegment() throws Exception {
        final GpxTrackSegment.Builder segmentBuilder = new GpxTrackSegment.Builder();

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("trkseg")) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "trkpt":
                        final GpxTrackPoint trackPoint = new GpxTrackPoint(
                                getDouble("lon", 0.0),
                                getDouble("lat", 0.0),
                                getDouble("ele", Double.NaN),
                                getDate("time", null),
                                parser.getAttributeValue(null, "name"),
                                parser.getAttributeValue(null, "desc"),
                                parser.getAttributeValue(null, "sym"),
                                getDouble("hdop", Double.NaN),
                                getDouble("vdop", Double.NaN),
                                getDouble("pdop", Double.NaN),
                                getInt("hr", -1),
                                getFloat("speed", -1),
                                getInt("cad", -1),
                                getFloat("temperature", Float.NaN),
                                getFloat("depth", Float.NaN),
                                getInt("power", -1)
                        );
                        segmentBuilder.withTrackPoint(trackPoint);
                        break;
                    case "extensions":
                        parseTrackSegmentExtensions(segmentBuilder);
                        break;
                }
            }

            eventType = parser.next();
        }

        return segmentBuilder.build();
    }

    private void parseTrackSegmentExtensions(GpxTrackSegment.Builder trackSegmentBuilder) throws Exception {
        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("extensions")) {
            if (eventType == XmlPullParser.START_TAG) {
                switch(parser.getName()) {
                    case "osmand_instructions":
                        parseExtensionsOsmAndInstructions(trackSegmentBuilder);
                        break;
                }
            }

            eventType = parser.next();
        }
    }

    private void parseExtensionsOsmAndInstructions(GpxTrackSegment.Builder trackSegmentBuilder) throws Exception {
        GpxTrackPoint startPoint = trackSegmentBuilder.getTrackPointAtIndex(0);
        GpxTrackPoint prevPoint = startPoint;

        List<Double> distancesCumulative = new ArrayList<>();
        double totalDistance = 0.0;
        for (GpxTrackPoint point : trackSegmentBuilder.getTrackPoints()) {
            totalDistance += point.getDistance(prevPoint);
            distancesCumulative.add(totalDistance);
            prevPoint = point;
        }

        Date referenceTime = (startPoint.getTime() != null) ? startPoint.getTime() : new Date();

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("osmand_instructions")) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("segment")) {
                final int trackPointIndex = getInt("startTrkptIdx", -1);
                final String turnType = parser.getAttributeValue(null, "turnType");
                final GpxTrackPoint referencePoint = trackSegmentBuilder.getTrackPointAtIndex(trackPointIndex);
                referenceTime = Date.from(referenceTime.toInstant().plusSeconds((int) getDouble("segmentTime", -1)));

                if (!getBoolean("skipTurn", false) && turnType != null) {
                    int parsedTurnType = switch (turnType) {
                        case "C" -> NavigationInfoSpec.ACTION_CONTINUE;
                        case "TL" -> NavigationInfoSpec.ACTION_TURN_LEFT;
                        case "TSLL" -> NavigationInfoSpec.ACTION_TURN_LEFT_SLIGHTLY;
                        case "TSHL" -> NavigationInfoSpec.ACTION_TURN_LEFT_SHARPLY;
                        case "TR" -> NavigationInfoSpec.ACTION_TURN_RIGHT;
                        case "TSLR" -> NavigationInfoSpec.ACTION_TURN_RIGHT_SLIGHTLY;
                        case "TSHR" -> NavigationInfoSpec.ACTION_TURN_RIGHT_SHARPLY;
                        case "KL" -> NavigationInfoSpec.ACTION_KEEP_LEFT;
                        case "KR" -> NavigationInfoSpec.ACTION_KEEP_RIGHT;
                        case "TU" -> NavigationInfoSpec.ACTION_UTURN_LEFT;
                        case "TRU" -> NavigationInfoSpec.ACTION_UTURN_RIGHT;
                        case "OFFR" -> NavigationInfoSpec.ACTION_OFFROUTE;
                        default -> -1;
                    };

                    // osmand includes the exit to take at the end of the string
                    String roundaboutInstruction = null;
                    int exitNumber = -1;
                    if (turnType.startsWith("RNLB")) {
                        parsedTurnType = NavigationInfoSpec.ACTION_ROUNDABOUT_LEFT;
                        exitNumber = Integer.parseInt(turnType.substring(4));
                        roundaboutInstruction = GBApplication.getContext().getString(
                                R.string.gpx_roundabout_exit,
                                exitNumber
                        );
                    } else if (turnType.startsWith("RNDB")) {
                        parsedTurnType = NavigationInfoSpec.ACTION_ROUNDABOUT_RIGHT;
                        exitNumber = Integer.parseInt(turnType.substring(4));
                        roundaboutInstruction = GBApplication.getContext().getString(
                                R.string.gpx_roundabout_exit,
                                exitNumber
                        );
                    }

                    if (parsedTurnType == -1) {
                        LOG.debug("Unknown OsmAnd navigation instruction found: {}", turnType);
                    }

                    GpxNavigationPoint navigationPoint = new GpxNavigationPoint(
                            referencePoint.getLongitude(),
                            referencePoint.getLatitude(),
                            distancesCumulative.get(trackPointIndex),
                            referenceTime, parsedTurnType,
                            roundaboutInstruction,
                            exitNumber
                    );

                    trackSegmentBuilder.withNavigationPoint(navigationPoint);
                }
            }

            eventType = parser.next();
        }
    }
}