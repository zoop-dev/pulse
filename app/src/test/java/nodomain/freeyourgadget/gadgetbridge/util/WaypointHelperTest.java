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
package nodomain.freeyourgadget.gadgetbridge.util;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest.readBinaryResource;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest.readTextResource;

import android.net.Uri;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class WaypointHelperTest extends TestBase {
    private static final WaypointHelper ACONCAGUA = new WaypointHelper(
            "Aconcagua",
            -(32.0 + 39.0 / 60.0 + 11.0 / 3600.0),
            -(70.0 + 00.0 / 60.0 + 42.0 / 3600.0),
            6967.15
    );

    private static final WaypointHelper DEAD_SEA = new WaypointHelper(
            "ים המלח",
            31.0 + 30.0 / 60.0,
            35.0 + 30.0 / 60.0,
            -439.78
    );
    private static final WaypointHelper EVEREST = new WaypointHelper(
            "सगरमाथा",
            27.0 + 59.0 / 60.0 + 18.0 / 3600.0,
            86.0 + 55.0 / 60.0 + 31.0 / 3600.0,
            8848.86
    );
    private static final WaypointHelper SANTA_CLAUS = new WaypointHelper(
            "Wihnächtsmann",
            89.0 + 59.0 / 60.0 + 59.0 / 3600.0,
            -(179.0 + 59.0 / 60.0 + 59.0 / 3600.0),
            null
    );

    private static final WaypointHelper ROLAS = new WaypointHelper(
            "Ilhéu das Rolas",
            -(0.0 + 0.0 / 60.0 + 9.8 / 3600.0),
            (6.0 + 31.0 / 60.0 + 0.2 / 3600.0),
            0.0
    );

    @BeforeClass
    public static void forceUtc() {
        // FIXME this is hacky, but we need the timestamps to match in the toString comparisons below
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private static void ToStringRoundTrip(WaypointHelper input, String expected) {
        String actual = input.toString();
        Assert.assertEquals(expected, actual);
        WaypointHelper reprocessed = WaypointHelper.Companion.fromText(actual);
        Assert.assertNotNull(reprocessed);
        Assert.assertEquals(expected, reprocessed.toString());
    }

    @Test
    public void TestFromGeoUri() {
        String[][] vectors = {
                // https://datatracker.ietf.org/doc/html/rfc5870
                {"geo:13.4125,103.8667", "geo:13.412500,103.866700?q=13.412500,103.866700"},
                {"geo:48.2010,16.3695,-18.3", "geo:48.201000,16.369500,-18.300000?q=48.201000,16.369500"},
                {"geo:-48.198634,16.371648;crs=wgs84;u=40", "geo:-48.198634,16.371648?q=-48.198634,16.371648"},
                {"geo:90,-22.43;crs=WGS84", "geo:90.000000,-22.430000?q=90.000000,-22.430000"},
                {"geo:90,46", "geo:90.000000,46.000000?q=90.000000,46.000000"},
                {"geo:22.300,-118.44", "geo:22.300000,-118.440000?q=22.300000,-118.440000"},
                {"geo:22.3,-118.4400", "geo:22.300000,-118.440000?q=22.300000,-118.440000"},
                {"geo:66,30;u=6.500;FOo=this%2dthat", "geo:66.000000,30.000000?q=66.000000,30.000000"},
                // https://developer.android.com/guide/components/intents-common
                {"geo:47.6,-122.3", "geo:47.600000,-122.300000?q=47.600000,-122.300000"},
                {"geo:47.6,-122.3?z=11", "geo:47.600000,-122.300000?q=47.600000,-122.300000"},
                {"geo:0,0?q=34.99,-106.61(Treasure)", "geo:34.990000,-106.610000?q=34.990000,-106.610000(Treasure)"},
                {"geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA", "geo:0,0?q=1600%20Amphitheatre%20Parkway%2C%20CA"},
                // https://developer.android.com/guide/components/google-maps-intents
                {"geo:0,0?q=restaurants", "geo:0,0?q=restaurants"},
                {"geo:37.7749,-122.4194?z=10&q=restaurants", "geo:37.774900,-122.419400?q=37.774900,-122.419400(restaurants)"},
                // https://en.wikipedia.org/wiki/Geo_URI_scheme
                {"geo:37.78918,-122.40335?z=14&(Wikimedia+Foundation)", "geo:37.789180,-122.403350?q=37.789180,-122.403350(Wikimedia%2BFoundation)"},
                {"geo:37.78918,-122.40335(Wikimedia+Foundation)", "geo:37.789180,-122.403350?q=37.789180,-122.403350(Wikimedia%2BFoundation)"},
                {"geo:0,0?q=37.78918,-122.40335(Wikimedia+Foundation)", "geo:37.789180,-122.403350?q=37.789180,-122.403350(Wikimedia%20Foundation)"},
                {"geo:0,0?q=37.78918,-122.40335", "geo:37.789180,-122.403350?q=37.789180,-122.403350"},
                {"geo:37.78918,-122.40335", "geo:37.789180,-122.403350?q=37.789180,-122.403350"}
        };

        for (String[] vector : vectors) {
            final String input = vector[0];
            final String expected = vector[1];
            final Uri uri = Uri.parse(input);

            final WaypointHelper helper = WaypointHelper.Companion.fromGeoUri(uri);
            Assert.assertNotNull(input, helper);

            final String actual = helper.toString();
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void TestFromText() {
        final String BUENOS_AIRES = "geo:-34.603889,-58.381389?q=-34.603889,-58.381389";
        final String Q1 = "geo:0.002500,0.005000?q=0.002500,0.005000";
        final String Q2 = "geo:-0.002500,0.005000?q=-0.002500,0.005000";
        final String Q3 = "geo:-0.002500,-0.005000?q=-0.002500,-0.005000";
        final String Q4 = "geo:0.002500,-0.005000?q=0.002500,-0.005000";

        final String[][] vectors = {
                // URI round trips:
                {BUENOS_AIRES, BUENOS_AIRES},
                {Q1, Q1},
                {Q2, Q2},
                {Q3, Q3},
                {Q4, Q4},
                // basic DMS handling:
                {"0°00’09″N 0°00’18″E", Q1},
                {"0°00’09″S 0°00’18″E", Q2},
                {"0°00’09″S 0°00’18″W", Q3},
                {"0°00’09″N 0°00’18″W", Q4},
                {"N0°00’09″ E0°00’18″", Q1},
                {"S0°00’09″ E0°00’18″", Q2},
                {"S0°00’09″ W0°00’18″", Q3},
                {"N0°00’09″ W0°00’18″", Q4},
                {"+0°00’09″ +0°00’18″", Q1},
                {"-0°00’09″ +0°00’18″", Q2},
                {"-0°00’09″ -0°00’18″", Q3},
                {"+0°00’09″ -0°00’18″", Q4},
                // basic DM handling:
                {"0°00.15’N 0°00.30’E", Q1},
                {"0°00.15’S 0°00.30’E", Q2},
                {"0°00.15’S 0°00.30’W", Q3},
                {"0°00.15’N 0°00.30’W", Q4},
                {"N0°00.15’ E0°00.30’", Q1},
                {"S0°00.15’ E0°00.30’", Q2},
                {"S0°00.15’ W0°00.30’", Q3},
                {"N0°00.15’ W0°00.30’", Q4},
                {"+0°00.15’ +0°00.30’", Q1},
                {"-0°00.15’ +0°00.30’", Q2},
                {"-0°00.15’ -0°00.30’", Q3},
                {"+0°00.15’ -0°00.30’", Q4},
                // basic D handling:
                {"0.0025°N 0.0050°E", Q1},
                {"0.0025°S 0.0050°E", Q2},
                {"0.0025°S 0.0050°W", Q3},
                {"0.0025°N 0.0050°W", Q4},
                {"N0.0025° E0.0050°", Q1},
                {"S0.0025° E0.0050°", Q2},
                {"S0.0025° W0.0050°", Q3},
                {"N0.0025° W0.0050°", Q4},
                {"+0.0025° +0.0050°", Q1},
                {"-0.0025° +0.0050°", Q2},
                {"-0.0025° -0.0050°", Q3},
                {"+0.0025° -0.0050°", Q4},
                // Everybody uses a different format:
                {"-34 36 14, -58 22 53", BUENOS_AIRES},
                {"-34°36’14\" -58°22’53\"", BUENOS_AIRES},
                {"-34°36’14″ -58°22’53″", BUENOS_AIRES},
                {"34:36:14S 58:22:53W", BUENOS_AIRES},
                {"34d 36’ 14\" S 58d 22’ 53\" W", BUENOS_AIRES},
                {"34° 36' 14.0\" S 58° 22' 53.0\" W", BUENOS_AIRES},
                {"34° 36′ 14.000″ S, 58° 22′ 53.000″ W", BUENOS_AIRES},
                {"34° 36′ 14.00″ S, 58° 22′ 53.00″ W", BUENOS_AIRES},
                {"34° 36′ 14.0″ S, 58° 22′ 53.0″ W", BUENOS_AIRES},
                {"34° 36′ 14″ S 58° 22′ 53″ W", BUENOS_AIRES},
                {"34° 36′ 14″ S ; 58° 22′ 53″ W", BUENOS_AIRES},
                {"34° 36′ 14″ S, 58° 22′ 53″ W", BUENOS_AIRES},
                {"34°36'14.0\"S 58°22'53.0\"W", BUENOS_AIRES},
                {"34°36’14\"S 58°22’53\"W", BUENOS_AIRES},
                {"34°36’14\"S, 58°22’53\"W", BUENOS_AIRES},
                {"34°36′14″S 58°22′53″W", BUENOS_AIRES},
                {"34°36′14″S,58°22′53″W", BUENOS_AIRES},
                {"34°36′14″S;58°22′53″W", BUENOS_AIRES},
                {"-34° 36.23333, -58° 22.88333", BUENOS_AIRES},
                {"-34° 36.23333’; -58° 22.88333’", BUENOS_AIRES},
                {"-34° 36.23333' -58° 22.88333'", BUENOS_AIRES},
                {"34° 36.23333S , 58° 22.883333W", BUENOS_AIRES},
                {"34° 36.23333’S , 58° 22.883333’W", BUENOS_AIRES},
                {"34° 36.23333'S , 58° 22.883333'W", BUENOS_AIRES},
                {"34° 36.23333S , 58° 22.883333 W", BUENOS_AIRES},
                {"34° 36.23333’ S 58° 22.883333’ W", BUENOS_AIRES},
                {"34° 36.23333' S 58° 22.883333' W", BUENOS_AIRES},
                {"-34.603889, -58.381389", BUENOS_AIRES},
                {"-34.603889 , -58.381389", BUENOS_AIRES},
                {"-34.603889 -58.381389", BUENOS_AIRES},
                {"-34.603889; -58.381389", BUENOS_AIRES},
                {"34.6038890° S 58.3813890° W", BUENOS_AIRES},
                {"-34.603889°,-58.381389°", BUENOS_AIRES},
                {"34.6038890S58.3813890W", BUENOS_AIRES},
                {"34.6038890S 58.3813890W", BUENOS_AIRES},
                {"34/36/14.0S 58/22/53.0W", BUENOS_AIRES},
                {"34/36.23333S  58/22.883333W", BUENOS_AIRES},
                {"34.603889S   58.3813890W", BUENOS_AIRES},
                {"34.6038890S/58.3813890W", BUENOS_AIRES},
                {"-34.6038890/-58.3813890", BUENOS_AIRES},
                {"34.6038890ºS , 58.3813890ºW", BUENOS_AIRES},
                {"34.6038890°S ; 58.3813890°W", BUENOS_AIRES},
                {"34.6038890°S / 58.3813890°W", BUENOS_AIRES},

                // pasted geo:
                {"Y geo:-34.603889,-58.381389?z=16 Z", BUENOS_AIRES},
                {"X\nY geo:-34.603889,-58.381389?z=16 Z", BUENOS_AIRES},
                {"X\nY geo:-34.603889,-58.381389?z=16\nZ", BUENOS_AIRES},
                {"X\r\nY geo:-34.603889,-58.381389?z=16\r\nZ", BUENOS_AIRES},

                // some URLs
                {"https://osmand.net/map?pin=-34.603889,-58.381389#16/-34.603889/-58.381389", BUENOS_AIRES},
                {"https://osmand.net/map/#16/-34.603889/-58.381389", BUENOS_AIRES},
                {"https://www.google.com/maps/@-34.603889,-58.381389,15z?", BUENOS_AIRES},
                {"https://www.google.com/maps/place/XYZ/@-34.603889,-58.381389,15z/data=", BUENOS_AIRES},
                {"https://www.google.com/maps/search/?api=1&query=-34.603889%2C-58.381389&a=1", BUENOS_AIRES},
                {"https://www.google.com/maps/search/?api=1&query=-34.603889,-58.381389#a", BUENOS_AIRES},
                {"https://www.google.com/maps/@?api=1&map_action=map&center=-34.603889%2C-58.381389&zoom=12&basemap=terrain", BUENOS_AIRES},
                {"https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=-34.603889%2C-58.381389&heading=-45&pitch=38&fov=80", BUENOS_AIRES},
                {"https://maps.google.com/?q=@-34.603889,-58.381389", BUENOS_AIRES}
        };

        for (String[] vector : vectors) {
            final String input = vector[0];
            final String expected = vector[1];

            final WaypointHelper helper = WaypointHelper.Companion.fromText(input);
            Assert.assertNotNull(input, helper);
            final String actual = helper.toString();
            Assert.assertEquals(expected, actual);
        }
    }

    private static void assertPosition(String input, WaypointHelper actual, String name, double latitude, double longitude) {
        Assert.assertNotNull(input, actual);
        Assert.assertEquals(input, name, actual.getName());
        Assert.assertNotNull(input, actual.getLatitude());
        Assert.assertNotNull(input, actual.getLongitude());
        Assert.assertEquals(input, latitude, actual.getLatitude(), 1e-9);
        Assert.assertEquals(input, longitude, actual.getLongitude(), 1e-9);
    }

    @Test
    public void TestPlusCodes() {
        // center of 8FVC2222+22
        final double zLat = 47.0000625;
        final double zLong = 8.0000625;
        final String[] full = {
                "8FVC2222+22",
                "8fvc2222+22",
                " 8FVC2222+22 ",
                "Meet at 8FVC2222+22, gate 3",
                "https://plus.codes/8FVC2222+22",
                "https://plus.codes/8FVC2222%2B22",
                "https://www.google.com/maps/search/?api=1&query=8FVC2222%2B22",
        };
        for (String input : full) {
            assertPosition(input, WaypointHelper.Companion.fromText(input), null, zLat, zLong);
        }

        // short codes need a reference position: QM9X+83 near Ho Chi Minh City is 7P28QM9X+83
        final WaypointHelper saigon = new WaypointHelper(null, 10.77, 106.69, null);
        final double sLat = 10.7683125;
        final double sLong = 106.6976875;
        final String[] shortCodes = {
                "QM9X+83",
                "QM9X+83 Ho Chi Minh City, Vietnam",
                "https://plus.codes/QM9X%2B83",
                "https://www.google.com/maps/search/?api=1&query=QM9X%2B83+Ho+Chi+Minh+City",
        };
        for (String input : shortCodes) {
            assertPosition(input, WaypointHelper.Companion.fromText(input, saigon), null, sLat, sLong);
            Assert.assertNull(input, WaypointHelper.Companion.fromText(input));
            Assert.assertTrue(input, WaypointHelper.Companion.containsShortPlusCode(input));
        }
        Assert.assertFalse(WaypointHelper.Companion.containsShortPlusCode("8FVC2222+22"));
        Assert.assertFalse(WaypointHelper.Companion.containsShortPlusCode("-34.603889, -58.381389"));

        // not Plus Codes
        // a code needs digits before the "+": "+855" alone is a valid short code, but here a phone prefix
        final String[] invalid = {"8FVC2222+2", "8FVC2222+22+", "CAFE+22", "1600+Amphitheatre+Parkway", "QM9X+83Z", "Call +855 23 999 999"};
        for (String input : invalid) {
            Assert.assertNull(input, WaypointHelper.Companion.fromText(input, saigon));
        }
    }

    @Test
    public void TestPlusCodeGeoUri() {
        final WaypointHelper saigon = new WaypointHelper(null, 10.77, 106.69, null);
        final double sLat = 10.7683125;
        final double sLong = 106.6976875;

        String input = "geo:0,0?q=8FVC2222+22";
        assertPosition(input, WaypointHelper.Companion.fromGeoUri(Uri.parse(input)), null, 47.0000625, 8.0000625);

        input = "geo:0,0?q=8FVC2222%2B22(Camp)";
        assertPosition(input, WaypointHelper.Companion.fromGeoUri(Uri.parse(input)), "Camp", 47.0000625, 8.0000625);

        input = "geo:0,0?q=QM9X+83+Ho+Chi+Minh+City";
        assertPosition(input, WaypointHelper.Companion.fromGeoUri(Uri.parse(input), saigon), "Ho Chi Minh City", sLat, sLong);

        // the geo: position is the reference, no other position needed
        input = "geo:10.77,106.69?q=QM9X+83";
        assertPosition(input, WaypointHelper.Companion.fromGeoUri(Uri.parse(input)), null, sLat, sLong);

        // short code, no reference: nothing to place
        input = "geo:0,0?q=QM9X+83";
        Assert.assertNull(input, WaypointHelper.Companion.fromGeoUri(Uri.parse(input)));
        Assert.assertNull(input, WaypointHelper.Companion.fromText(input));
        assertPosition(input, WaypointHelper.Companion.fromText(input, saigon), null, sLat, sLong);

        // short code with a label but no reference keeps the label
        input = "geo:0,0?q=QM9X+83(Camp)";
        final WaypointHelper labelOnly = WaypointHelper.Companion.fromGeoUri(Uri.parse(input));
        Assert.assertNotNull(input, labelOnly);
        Assert.assertEquals("Camp", labelOnly.getName());
        Assert.assertNull(labelOnly.getLatitude());
        Assert.assertNull(labelOnly.getLongitude());
    }

    @Test
    public void TestToString() {
        ToStringRoundTrip(ACONCAGUA, "geo:-32.653056,-70.011667,6967.150000?q=-32.653056,-70.011667(Aconcagua)");
        ToStringRoundTrip(DEAD_SEA, "geo:31.500000,35.500000,-439.780000?q=31.500000,35.500000(%D7%99%D7%9D%20%D7%94%D7%9E%D7%9C%D7%97)");
        ToStringRoundTrip(EVEREST, "geo:27.988333,86.925278,8848.860000?q=27.988333,86.925278(%E0%A4%B8%E0%A4%97%E0%A4%B0%E0%A4%AE%E0%A4%BE%E0%A4%A5%E0%A4%BE)");
        ToStringRoundTrip(SANTA_CLAUS, "geo:89.999722,-179.999722?q=89.999722,-179.999722(Wihn%C3%A4chtsmann)");
        ToStringRoundTrip(ROLAS, "geo:-0.002722,6.516722,0.000000?q=-0.002722,6.516722(Ilh%C3%A9u%20das%20Rolas)");
    }

    /// encode->decode waypoints in a Lctns.fit compliant format
    @Test
    public void TestFitLocationEncoding() throws Exception {
        // waypoints -> fit
        WaypointHelper[] waypoints = {ACONCAGUA, DEAD_SEA, EVEREST, SANTA_CLAUS, ROLAS};
        FitFile generatedFit = WaypointHelper.Companion.generateFitLocationFile(waypoints, 1774688353L, 246);
        byte[] fitGenerated = generatedFit.getOutgoingMessage();
        byte[] fitExpected = readBinaryResource("/TestFitLocationEncoding.fit");
        Assert.assertArrayEquals(fitExpected, fitGenerated);

        // fit -> text
        FitFile decodedFit = FitFile.parseIncoming(fitGenerated);
        String decodedActual = decodedFit.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
        String decodedExpected = readTextResource("/TestFitLocationEncoding.txt");
        Assert.assertEquals(decodedExpected, decodedActual);
    }

    @Test
    public void TestFormatPosition() {
        String[] aconcaguaActual = WaypointHelper.Companion.formatPosition(ACONCAGUA.getLatitude(), ACONCAGUA.getLongitude());
        String[] aconcaguaExpected = {
                "32° 39′ 11.0″ S, 70° 00′ 42.0″ W",
                "32° 39.183′ S, 70° 00.700′ W",
                "32.653056°S 70.011667°W",
                "-32.653056; -70.011667",
        };
        Assert.assertArrayEquals(aconcaguaExpected, aconcaguaActual);

        String[] deadSeaActual = WaypointHelper.Companion.formatPosition(DEAD_SEA.getLatitude(), DEAD_SEA.getLongitude());
        String[] deadSeaExpected = {
                "31° 30′ 00.0″ N, 35° 30′ 00.0″ E",
                "31° 30.000′ N, 35° 30.000′ E",
                "31.500000°N 35.500000°E",
                "31.500000; 35.500000",
        };
        Assert.assertArrayEquals(deadSeaExpected, deadSeaActual);

        String[] everestActual = WaypointHelper.Companion.formatPosition(EVEREST.getLatitude(), EVEREST.getLongitude());
        String[] everestExpected = {
                "27° 59′ 18.0″ N, 86° 55′ 31.0″ E",
                "27° 59.300′ N, 86° 55.517′ E",
                "27.988333°N 86.925278°E",
                "27.988333; 86.925278",
        };
        Assert.assertArrayEquals(everestExpected, everestActual);

        String[] santaClausActual = WaypointHelper.Companion.formatPosition(SANTA_CLAUS.getLatitude(), SANTA_CLAUS.getLongitude());
        String[] santaClausExpected = {
                "89° 59′ 59.0″ N, 179° 59′ 59.0″ W",
                "89° 59.983′ N, 179° 59.983′ W",
                "89.999722°N 179.999722°W",
                "89.999722; -179.999722",
        };
        Assert.assertArrayEquals(santaClausExpected, santaClausActual);

        String[] rolasActual = WaypointHelper.Companion.formatPosition(ROLAS.getLatitude(), ROLAS.getLongitude());
        String[] rolasExpected = {
                "0° 00′ 09.8″ S, 6° 31′ 00.2″ E",
                "0° 00.163′ S, 6° 31.003′ E",
                "0.002722°S 6.516722°E",
                "-0.002722; 6.516722",
        };
        Assert.assertArrayEquals(rolasExpected, rolasActual);
    }
}
