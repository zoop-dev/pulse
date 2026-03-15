/*  Copyright (C) 2026 Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest.readBinaryResource;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;

public class GpxRouteFileConverterTest extends TestBase {

    @Test
    public void TestSampleTrack() throws IOException {
        GpxToFitCourse("/gpx-exporter-test-SampleTrack.gpx", "/gpx-exporter-test-SampleTrack.course.fit");
    }

    @Test
    public void TestMultipleSegments() throws IOException {
        GpxToFitCourse("/gpx-parser-test-multiple-segments.gpx", "/gpx-parser-test-multiple-segments.course.fit");
    }

    @Test
    public void TestImport() throws IOException {
        GpxToFitCourse("/TestGpxImport.gpx", "/TestGpxImport.course.fit");
    }

    private void GpxToFitCourse(String gpxResource, String expectedFitResource) throws IOException {
        final byte[] gpxRaw = readBinaryResource(gpxResource);
        final GpxFile gpx = GpxParser.parseGpx(gpxRaw);
        final GpxRouteFileConverter converter = new GpxRouteFileConverter(gpx, gpx.getName(), gpx.getTime());
        final FitFile fit = converter.getConvertedFile();
        final byte[] generatedFit = fit.getOutgoingMessage();
        //Files.write(Path.of("a.fit"), generatedFit);
        final byte[] expectedFit = readBinaryResource(expectedFitResource);
        Assert.assertArrayEquals(expectedFit, generatedFit);
    }
}
