/*  Copyright (C) 2026 Jeff Laflamme

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
package nodomain.freeyourgadget.gadgetbridge.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// official test vectors, see src/test/resources/openlocationcode/README.md
public class OpenLocationCodeTest {
    private static final double PRECISION = 1e-10;

    private static List<String[]> readVectors(String name) throws Exception {
        final List<String[]> vectors = new ArrayList<>();
        try (InputStream in = OpenLocationCodeTest.class.getResourceAsStream("/openlocationcode/" + name)) {
            Assert.assertNotNull(name, in);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    vectors.add(line.split(","));
                }
            }
        }
        Assert.assertFalse(name, vectors.isEmpty());
        return vectors;
    }

    @Test
    public void TestValidity() throws Exception {
        for (String[] v : readVectors("validityTests.csv")) {
            final String code = v[0];
            Assert.assertEquals("isValid " + code, Boolean.parseBoolean(v[1]), OpenLocationCode.isValid(code));
            Assert.assertEquals("isShort " + code, Boolean.parseBoolean(v[2]), OpenLocationCode.isShort(code));
            Assert.assertEquals("isFull " + code, Boolean.parseBoolean(v[3]), OpenLocationCode.isFull(code));
        }
    }

    @Test
    public void TestDecode() throws Exception {
        for (String[] v : readVectors("decoding.csv")) {
            final String code = v[0];
            final OpenLocationCode.CodeArea area = OpenLocationCode.decode(code);
            Assert.assertEquals("length " + code, Integer.parseInt(v[1]), area.getCodeLength());
            Assert.assertEquals("latLo " + code, Double.parseDouble(v[2]), area.getLatitudeLo(), PRECISION);
            Assert.assertEquals("lngLo " + code, Double.parseDouble(v[3]), area.getLongitudeLo(), PRECISION);
            Assert.assertEquals("latHi " + code, Double.parseDouble(v[4]), area.getLatitudeHi(), PRECISION);
            Assert.assertEquals("lngHi " + code, Double.parseDouble(v[5]), area.getLongitudeHi(), PRECISION);
        }
    }

    @Test
    public void TestEncode() throws Exception {
        for (String[] v : readVectors("encoding.csv")) {
            final double lat = Double.parseDouble(v[0]);
            final double lng = Double.parseDouble(v[1]);
            final int length = Integer.parseInt(v[4]);
            Assert.assertEquals(v[0] + "," + v[1] + " @" + length, v[5], OpenLocationCode.encode(lat, lng, length));
        }
    }

    @Test
    public void TestRecoverNearest() throws Exception {
        for (String[] v : readVectors("shortCodeTests.csv")) {
            if ("S".equals(v[4])) {
                continue; // shortening is not implemented
            }
            final String expected = v[0];
            final double lat = Double.parseDouble(v[1]);
            final double lng = Double.parseDouble(v[2]);
            Assert.assertEquals(v[3] + " near " + v[1] + "," + v[2], expected, OpenLocationCode.recoverNearest(v[3], lat, lng));
        }
    }

    @Test
    public void TestEncodeLength() {
        for (final int length : new int[]{-1, 0, 1, 3, 9}) {
            Assert.assertThrows("length " + length, IllegalArgumentException.class, () -> OpenLocationCode.encode(47.0, 8.0, length));
        }
        Assert.assertEquals("8F000000+", OpenLocationCode.encode(47.0, 8.0, 2));
        Assert.assertEquals("8FVC2222+222", OpenLocationCode.encode(47.0, 8.0, 11));
    }
}
