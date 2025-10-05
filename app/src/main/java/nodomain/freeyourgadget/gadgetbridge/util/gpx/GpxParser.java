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
import java.util.Date;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
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
        return ISO8601Utils.parse(text, new ParsePosition(0));
    }

    private double getDouble(String attribute, double defaultDouble) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultDouble;
        }
        return Double.parseDouble(text);
    }

    private float getFloat(String attribute, float defaultFloat) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultFloat;
        }
        return Float.parseFloat(text);
    }

    private int getInt(String attribute, int defaultInt) {
        final String text = parser.getAttributeValue(null, attribute);
        if (text == null || text.length() < 1) {
            return defaultInt;
        }
        return Integer.parseInt(text);
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
                                getFloat("depth", Float.NaN)
                        );
                        segmentBuilder.withTrackPoint(trackPoint);
                        break;
                }
            }

            eventType = parser.next();
        }

        return segmentBuilder.build();
    }
}