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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackSegment;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxWaypoint;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest.readBinaryResource;

public class GPXParserTest extends TestBase {

    @Test
    public void shouldReadGPXCorrectly() throws IOException, GpxParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-exporter-test-SampleTrack.gpx")) {
            GpxParser gpxParser = new GpxParser(inputStream);
            List<GpxTrackPoint> trackPoints = gpxParser.getGpxFile().getPoints();
            Assert.assertEquals(trackPoints.size(), 14);
            DecimalFormat df = new DecimalFormat("###.##");
            for (GPSCoordinate tp : trackPoints) {
                Assert.assertEquals(df.format(tp.getLongitude()), "-68.2");
                Assert.assertEquals(df.format(tp.getLatitude()), "44.15");
                Assert.assertThat(df.format(tp.getAltitude()), anyOf(is("40"), is("46")));
            }
            Assert.assertEquals(
                    new GpxTrackPoint(-68.200293, 44.152462, 40, new Date(1546300800000L)),
                    trackPoints.get(0)
            );
        }
    }

    @Test
    public void shouldParseMultipleSegments() throws IOException, GpxParseException, ParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-parser-test-multiple-segments.gpx")) {
            final GpxParser gpxParser = new GpxParser(inputStream);
            final GpxFile gpxFile = gpxParser.getGpxFile();
            Assert.assertEquals(1, gpxFile.getTracks().size());
            Assert.assertEquals(2, gpxFile.getTracks().get(0).getTrackSegments().size());

            final List<GpxTrackPoint> segment1 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 790.0, new Date(1680969788000L), 123));
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 296.0, new Date(1680970639000L), 56));
            }};

            final List<GpxTrackPoint> segment2 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 205.0, new Date(1680971684000L), 85));
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 209.0, new Date(1680973017000L), 150));
            }};

            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(0).getTrackPoints(), segment1);
            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(1).getTrackPoints(), segment2);
        }
    }

    @Test
    public void shouldParseOutOfOrder() throws IOException, GpxParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-parser-test-order.gpx")) {
            final GpxParser gpxParser = new GpxParser(inputStream);
            final GpxFile gpxFile = gpxParser.getGpxFile();
            Assert.assertEquals(1, gpxFile.getTracks().size());
            Assert.assertEquals(1, gpxFile.getTracks().get(0).getTrackSegments().size());

            final List<GpxTrackPoint> segment1 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 790.0, new Date(1680969788000L), 123));
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 296.0, new Date(1680970639000L), 56));
            }};

            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(0).getTrackPoints(), segment1);
        }
    }

    @Test
    public void TestGpxImport() throws Exception {
        final byte[] actual;
        try (final InputStream gpx = getClass().getResourceAsStream("/TestGpxImport.gpx")) {
            actual = GpxParser.transformGpx(gpx);
        }
        byte[] expected = readBinaryResource("/TestGpxImport.xml");
        Assert.assertArrayEquals(expected, actual);

        final GpxFile file1;
        try (final InputStream gpx = getClass().getResourceAsStream("/TestGpxImport.gpx")) {
            file1 = new GpxParser(gpx).getGpxFile();
        }
        Assert.assertNotNull(file1);
        Assert.assertEquals("Gadgetbridge's TestGpxImport.gpx", file1.getName());
        Assert.assertEquals("Gadgetbridge test author", file1.getAuthor());
        Assert.assertEquals(Date.from(Instant.parse("2025-10-03T18:20:45Z")), file1.getTime());
        Assert.assertEquals(3, file1.getWaypoints().size());
        Assert.assertEquals(1, file1.getTracks().size());
        Assert.assertEquals(4, file1.getPoints().size());

        GpxWaypoint wp0 = file1.getWaypoints().get(0);
        Assert.assertEquals(9.7393798828125, wp0.getLongitude(), 0.0001);
        Assert.assertEquals(47.5048828125, wp0.getLatitude(), 0.0001);
        Assert.assertEquals(-470.1, wp0.getAltitude(), 0.0001);
        Assert.assertEquals(Date.from(Instant.parse("2025-10-03T14:10:59Z")), wp0.getTime());
        Assert.assertEquals("Bregenz", wp0.getName());
        Assert.assertEquals("Airport", wp0.getSymbol());
        Assert.assertEquals("city in Österreich", wp0.getDescription());
        Assert.assertEquals(1.4, wp0.getHdop(), 0.0001);
        Assert.assertEquals(3.2, wp0.getVdop(),0.0001);
        Assert.assertEquals(1.5, wp0.getPdop(), 0.0001);
        Assert.assertEquals(9.1, wp0.getTemperature(), 0.0001);
        Assert.assertEquals(6.1, wp0.getDepth(), 0.0001);

        Assert.assertEquals("腓特烈港", file1.getWaypoints().get(1).getName());

        GpxTrack trk0 = file1.getTracks().get(0);
        Assert.assertEquals("Gadgetbridge import test track", trk0.getName());
        Assert.assertEquals(1, trk0.getTrackSegments().size());

        GpxTrackSegment trkseg0 = trk0.getTrackSegments().get(0);
        Assert.assertEquals(4, trkseg0.getTrackPoints().size());

        GpxTrackPoint trkpt0 = trkseg0.getTrackPoints().get(0);
        Assert.assertEquals(47.5048828125, trkpt0.getLatitude(), 0.0001);
        Assert.assertEquals(9.7393798828125, trkpt0.getLongitude(), 0.0001);
        Assert.assertEquals(-440.2, trkpt0.getAltitude(), 0.0001);
        Assert.assertEquals(Date.from(Instant.parse("2025-10-03T15:10:59Z")), trkpt0.getTime());
        Assert.assertEquals("Tp1", trkpt0.getName());
        Assert.assertEquals("Summit", trkpt0.getSymbol());
        Assert.assertEquals("a description", trkpt0.getDescription());
        Assert.assertEquals(1.4, trkpt0.getHdop(), 0.0001);
        Assert.assertEquals(3.2, trkpt0.getVdop(), 0.0001);
        Assert.assertEquals(1.5, trkpt0.getPdop(), 0.0001);
        Assert.assertEquals(123, trkpt0.getHeartRate());
        Assert.assertEquals(22, trkpt0.getCadence());
        Assert.assertEquals(23.3f, trkpt0.getSpeed(), 0.0001f);
        Assert.assertEquals(28.3f, trkpt0.getTemperature(), 0.0001f);
        Assert.assertEquals(26.3f, trkpt0.getDepth(), 0.0001f);

        GpxTrackPoint trkpt1 = trkseg0.getTrackPoints().get(1);
        Assert.assertEquals(Date.from(Instant.parse("2025-10-03T21:06:07+05:45")), trkpt1.getTime());

        byte[] raw = readBinaryResource("/TestGpxImport.gpx");
        GpxFile file2 = GpxParser.parseGpx(raw);
        Assert.assertNotNull(file2);
        Assert.assertEquals(3, file2.getWaypoints().size());
        Assert.assertEquals(4, file2.getPoints().size());
        Assert.assertEquals(1, file2.getTracks().size());
        Assert.assertEquals(trkpt0, file2.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().get(0));
    }

    @Test
    public void TestToActivityPoint() throws Exception {
        final GpxFile file;
        try (final InputStream gpx = getClass().getResourceAsStream("/TestGpxImport.gpx")) {
            file = new GpxParser(gpx).getGpxFile();
        }

        GpxTrackPoint trackpoint = file.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().get(0);
        ActivityPoint activityPoint = trackpoint.toActivityPoint();
        Assert.assertNotNull(activityPoint);

        Assert.assertEquals(trackpoint.getAltitude(), activityPoint.getLocation().getAltitude(), 0.0);
        Assert.assertEquals(trackpoint.getCadence(), activityPoint.getCadence());
        Assert.assertEquals(trackpoint.getDepth(), activityPoint.getDepth(), 0.0);
        Assert.assertEquals(trackpoint.getDescription(), activityPoint.getDescription());
        Assert.assertEquals(trackpoint.getHdop(), activityPoint.getLocation().getHdop(), 0.0);
        Assert.assertEquals(trackpoint.getHeartRate(), activityPoint.getHeartRate());
        Assert.assertEquals(trackpoint.getLatitude(), activityPoint.getLocation().getLatitude(), 0.0);
        Assert.assertEquals(trackpoint.getLongitude(), activityPoint.getLocation().getLongitude(), 0.0);
        Assert.assertEquals(trackpoint.getPdop(), activityPoint.getLocation().getPdop(), 0.0);
        Assert.assertEquals(trackpoint.getSpeed(), activityPoint.getSpeed(), 0.0);
        Assert.assertEquals(trackpoint.getTemperature(), activityPoint.getTemperature(), 0.0);
        Assert.assertEquals(trackpoint.getTime(), activityPoint.getTime());
        Assert.assertEquals(trackpoint.getVdop(), activityPoint.getLocation().getVdop(), 0.0);
    }
}
