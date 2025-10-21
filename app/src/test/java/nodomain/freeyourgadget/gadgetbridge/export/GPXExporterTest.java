/*  Copyright (C) 2019-2025 Nick Spacek, Thomas Kuehne

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

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest.readBinaryResource;

import com.google.gson.internal.bind.util.ISO8601Utils;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter.GPXTrackEmptyException;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.test.GBTestApplication;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;

public class GPXExporterTest extends TestBase {
    @Test
    public void shouldCreateValidGpxFromSimulatedData() throws IOException, ParseException, GPXTrackEmptyException, SAXException {
        final List<ActivityPoint> points = readActivityPoints("/GPXExporterTest-SampleTracks.csv");

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.setCreator("Gadgetbridge Test");
        final ActivityTrack track = createTestTrack(points, new Date());

        final File tempFile = File.createTempFile("gpx-exporter-test-track", ".gpx");
        tempFile.deleteOnExit();

        gpxExporter.performExport(track, tempFile);
        validateGpxFile(tempFile);
    }

    @Test
    public void shouldCreateValidGpxFromSimulatedDataWithHeartrate() throws IOException, ParseException, GPXTrackEmptyException, ParserConfigurationException, SAXException {
        final List<ActivityPoint> points = readActivityPoints("/GPXExporterTest-SampleTracksHR.csv");

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.setCreator("Gadgetbridge Test");
        final ActivityTrack track = createTestTrack(points, new Date());

        final File tempFile = File.createTempFile("gpx-exporter-test-track", ".gpx");
        tempFile.deleteOnExit();

        gpxExporter.performExport(track, tempFile);
        validateGpxFile(tempFile);
    }

    private ActivityTrack createTestTrack(List<ActivityPoint> points, Date time) {
        final User user = new User();
        user.setName("Test User");

        Device device = new Device();
        device.setName("Test Device");

        final ActivityTrack track = new ActivityTrack();
        track.setName("Test Track");
        track.setBaseTime(time);
        track.setUser(user);
        track.setDevice(device);

        for (final ActivityPoint point : points) {
            track.addTrackPoint(point);
        }
        return track;
    }

    private List<ActivityPoint> readActivityPoints(String resourcePath) throws IOException, ParseException {
        final List<ActivityPoint> points = new ArrayList<>();
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String nextLine = reader.readLine();
                while (nextLine != null) {
                    final String[] pieces = nextLine.split("\\s+");
                    final ActivityPoint point = new ActivityPoint();
                    point.setLocation(new GPSCoordinate(
                            Double.parseDouble(pieces[0]),
                            Double.parseDouble(pieces[1]),
                            Double.parseDouble(pieces[2]))
                    );

                    final int dateIndex;
                    if (pieces.length == 5) {
                        point.setHeartRate(Integer.parseInt(pieces[3]));
                        dateIndex = 4;
                    } else {
                        dateIndex = 3;
                    }
                    // Not sure about this parser but seemed safe to use
                    point.setTime(ISO8601Utils.parse(pieces[dateIndex], new ParsePosition(0)));

                    points.add(point);
                    nextLine = reader.readLine();
                }
            }
        }
        return points;
    }

    private void validateGpxFile(File tempFile) throws SAXException, IOException {
        final Source xmlFile = new StreamSource(tempFile);
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final StreamSource[] sources = {
                new StreamSource(getClass().getResourceAsStream("/gpx.xsd")),
                new StreamSource(getClass().getResourceAsStream("/opentracks-schema-1.0.xsd")),
                new StreamSource(getClass().getResourceAsStream("/TrackPointExtensionv2.xsd"))
        };
        final Schema schema = schemaFactory.newSchema(sources);
        final Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

    /// GPX (import -> export) with different locales to ensure parsing and formating work correctly
    @Test
    public void TestImportExport() throws Exception {
        try {
            GBTestApplication.setLanguage("bn");
            Assert.assertEquals("-১২,৩৫৬.৬৫৪৩২১", String.format("%,f", -12356.654321));
            ImportExport();
            GBTestApplication.setLanguage("dz");
            Assert.assertEquals("-༡༢,༣༥༦.༦༥༤༣༢༡", String.format("%,f", -12356.654321));
            ImportExport();
            GBTestApplication.setLanguage("my");
            Assert.assertEquals("-၁၂,၃၅၆.၆၅၄၃၂၁", String.format("%,f", -12356.654321));
            ImportExport();
            GBTestApplication.setLanguage("sa");
            Assert.assertEquals("-१२,३५६.६५४३२१", String.format("%,f", -12356.654321));
            ImportExport();
        }finally {
            GBTestApplication.setLanguage("en");
            Assert.assertEquals("-12,356.654321", String.format("%,f", -12356.654321));
        }
    }

    public void ImportExport() throws IOException, GpxParseException, GPXTrackEmptyException {
        final GpxFile imported;
        try (final InputStream gpx = getClass().getResourceAsStream("/TestGpxImport.gpx")) {
            imported = new GpxParser(gpx).getGpxFile();
        }

        final List<ActivityPoint> points = imported.getActivityPoints();

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.setCreator("Gadgetbridge Test");
        gpxExporter.setDate(Date.from(Instant.parse("2025-10-17T21:00:00-00:00")));
        gpxExporter.setUuid(UUID.fromString("c5185301-d578-4e52-bb9f-c2a7afa044e2"));
        final ActivityTrack track = createTestTrack(points, imported.getTime());

        final File tempFile = File.createTempFile("gpx-exporter-test-import-export", ".gpx");
        tempFile.deleteOnExit();

        gpxExporter.performExport(track, tempFile);

        byte[] exported = Files.readAllBytes(tempFile.toPath());
        byte[] expected = readBinaryResource("/TestGpxExport.gpx");
        Assert.assertArrayEquals(expected, exported);
    }
}