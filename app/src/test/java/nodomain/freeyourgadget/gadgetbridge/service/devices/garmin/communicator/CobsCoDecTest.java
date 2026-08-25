/*  Copyright (C) 2024-2026 José Rebelo, Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.calcMaxWriteChunk;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CobsCoDecTest {
    final CobsCoDec cobsCoDec = new CobsCoDec();

    @Test
    public void testCobsDecoder() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B800"));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsDecoder2() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(
                new byte[]{0x2b, 0x00, (byte) 0x88, 0x13, (byte) 0xa0, 0x13, 0x00, (byte) 0x96, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, 0x1f, (byte) 0xff, (byte) 0xff, 0x04, 0x6c, 0x61, 0x72, 0x6a, 0x07, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x07, 0x58, 0x51, 0x2d, 0x43, 0x43, 0x37, 0x32, 0x01, (byte) 0xf9, (byte) 0xcf},
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsEncoder2() {
        byte[] result = cobsCoDec.encode(GB.hexStringToByteArray("022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(
                new byte[]{0x00, 0x2d, (byte) 0x02, (byte) 0x2b, (byte) 0x05, (byte) 0x88, (byte) 0x13, (byte) 0xa0, (byte) 0x13, (byte) 0x02, (byte) 0x96, (byte) 0x23, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, (byte) 0x1f, (byte) 0xff, (byte) 0xff, (byte) 0x04, (byte) 0x6c, (byte) 0x61, (byte) 0x72, (byte) 0x6a, (byte) 0x07, (byte) 0x75, (byte) 0x6e, (byte) 0x6b, (byte) 0x6e, (byte) 0x6f, (byte) 0x77, (byte) 0x6e, (byte) 0x07, (byte) 0x58, (byte) 0x51, (byte) 0x2d, (byte) 0x43, (byte) 0x43, (byte) 0x37, (byte) 0x32, (byte) 0x01, (byte) 0xf9, (byte) 0xcf, (byte) 0x01, (byte) 0x00},
                result
        );
    }

    @Test
    public void testCobsDecoderSingleByteAtStart() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B800"));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsDecoderSingleByteAtEnd() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B8"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00"));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsEncoder() {
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253010304B800"),
                cobsCoDec.encode(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"))
        );
    }

    @Test
    public void testLongPayload() {
        //test strings from https://github.com/themarpe/cobs-java/blob/master/tests-java/Tests.java
        final byte[] test_string_0 = new byte[]{0, 0, 0, 0};
        final byte[] test_string_1 = new byte[]{0, '1', '2', '3', '4', '5'};
        final byte[] test_string_2 = new byte[]{0, '1', '2', '3', '4', '5', 0};
        final byte[] test_string_3 = new byte[]{'1', '2', '3', '4', '5', 0, '6', '7', '8', '9'};
        final byte[] test_string_4 = new byte[]{0, '1', '2', '3', '4', '5', 0, '6', '7', '8', '9', 0};
        final byte[] test_string_5 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
        final byte[] test_string_6 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
        final byte[] test_string_7 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1};
        final byte[] test_string_8 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, 0, -5, -4, -3, -2, -1};
        final byte[] test_string_long_nonzero = GB.hexStringToByteArray("49535a73394d73483159456f515352443069546e466c4b32394d3479336f396a6936543263544954794e576b456f4e365769704734444a6b4c6439657a6256386b30676f4e544e587954326e596e617567334f546449376c535471506e724e4444326664774d7a3142725653596c4838794b524b50327a5a3438704961545457384b483571744a6c726948704552364b7a466c54315776337a373954524942467442784c3062486f6c386b786a48377750726f5277766546757a596876533731726e6972344e644f70475a6c6a4c65753371554545396a4f556974703655774b426b34575970754e4f484b6349364f425468334c753532324b66abababab000B");
        //test strings from #5903
        final byte[] test_string_9 = {1, 0, 0};
        final byte[] test_string_10 = {1};

        final byte[][] allTests = new byte[][]{
                test_string_0,
                test_string_1,
                test_string_2,
                test_string_3,
                test_string_4,
                test_string_5,
                test_string_6,
                test_string_7,
                test_string_8,
                test_string_long_nonzero,
                test_string_9,
                test_string_10,
        };

        for (byte[] payload : allTests) {
            byte[] encodedData = cobsCoDec.encode(payload);
            cobsCoDec.receivedBytes(encodedData);
            byte[] decodedData = cobsCoDec.retrieveMessage();

            Assert.assertArrayEquals(payload, decodedData);
        }
    }

    // Real frames captured from a Venu X1 on 2026-08-23 (support dump):
    // multi-chunk GFDI protobuf weather requests (current, forecast/day, forecast/hour)
    // and the status message that followed. Chunk sizes are limited by the BLE MTU
    // (229 bytes per notification). The forecast/hour frame arrived truncated - a middle
    // chunk was lost on the BLE link - which used to wedge the COBS decoder until the
    // app was restarted (no watch data for ~20h). The leading BLE handle byte is already
    // stripped; these are the exact byte streams delivered to receivedBytes().
    private static final String WEATHER_CURRENT_CHUNK1 = // current-weather request, chunk 1 of 2
            "00078C01B313400301010103B6010103780101FF12B3032AB0030A7668747470733A2F2F6170692E6763732E6761726D696E2E636F6D2F776561746865722F76322F63757272656E743F6C61743D363730343035333830266C6F6E3D3434313236313933392674656D70556E69743D43454C53495553267370656564556E69743D4D45544552535F5045525F5345434F4E4418012A150A0F4163636570742D4C616E6775616765120272752A3C0A0D417574686F72697A6174696F6E122B4265617265722064643866653930382D636334652D346437382D396164332D31653732373434";
    private static final String WEATHER_CURRENT_CHUNK2 = // current-weather request, chunk 2 of 2
            "30313232642A170A0F4163636570742D456E636F64696E671204677A69702A210A1C782D6761726D696E2D696E747D65726E616C2D726571756573742D69641201312A1D0A17582D4761726D696E2D557365722D496E6974696174656412023F312A1E0A10582D4761726D696E2D556E69742D4944120A333631323932383336382A270A17582D4761726D696E2D53572D506172742D4E756D626572120C3030362D42343630332D30113F00";
    private static final String WEATHER_CURRENT_DECODED = // current-weather request decoded (396 bytes)
            "8C01B313400300000000B60100007801000012B3032AB0030A7668747470733A2F2F6170692E6763732E6761726D696E2E636F6D2F776561746865722F76322F63757272656E743F6C61743D363730343035333830266C6F6E3D3434313236313933392674656D70556E69743D43454C53495553267370656564556E69743D4D45544552535F5045525F5345434F4E4418012A150A0F4163636570742D4C616E6775616765120272752A3C0A0D417574686F72697A6174696F6E122B4265617265722064643866653930382D636334652D346437382D396164332D3165373237343430313232642A170A0F4163636570742D456E636F64696E671204677A69702A210A1C782D6761726D696E2D696E7465726E616C2D726571756573742D69641201312A1D0A17582D4761726D696E2D557365722D496E6974696174656412023F312A1E0A10582D4761726D696E2D556E69742D4944120A333631323932383336382A270A17582D4761726D696E2D53572D506172742D4E756D626572120C3030362D42343630332D30113F";
    private static final String WEATHER_FORECAST_DAY_CHUNK1 = // forecast/day request, chunk 1 of 2
            "00078C01B313410301010103AA010103780101FF12A7032AA4030A6A68747470733A2F2F6170692E6763732E6761726D696E2E636F6D2F776561746865722F76322F666F7265636173742F6461793F6C61743D363730343035333830266C6F6E3D343431323631393339266475726174696F6E3D352674656D70556E69743D43454C5349555318012A150A0F4163636570742D4C616E6775616765120272752A3C0A0D417574686F72697A6174696F6E122B4265617265722064643866653930382D636334652D346437382D396164332D3165373237343430313232642A170A0F416363";
    private static final String WEATHER_FORECAST_DAY_CHUNK2 = // forecast/day request, chunk 2 of 2
            "6570742D456E636F64696E671204677A69702A210A1C782D6761726D696E2D696E7465726E616C2D7265717565737D742D69641201312A1D0A17582D4761726D696E2D557365722D496E6974696174656412023F312A1E0A10582D4761726D696E2D556E69742D4944120A333631323932383336382A270A17582D4761726D696E2D53572D506172742D4E756D626572120C3030362D42343630332D30302A220A19582D4761726D6919D100";
    private static final String WEATHER_FORECAST_DAY_DECODED = // forecast/day request decoded (396 bytes)
            "8C01B313410300000000AA0100007801000012A7032AA4030A6A68747470733A2F2F6170692E6763732E6761726D696E2E636F6D2F776561746865722F76322F666F7265636173742F6461793F6C61743D363730343035333830266C6F6E3D343431323631393339266475726174696F6E3D352674656D70556E69743D43454C5349555318012A150A0F4163636570742D4C616E6775616765120272752A3C0A0D417574686F72697A6174696F6E122B4265617265722064643866653930382D636334652D346437382D396164332D3165373237343430313232642A170A0F4163636570742D456E636F64696E671204677A69702A210A1C782D6761726D696E2D696E7465726E616C2D726571756573742D69641201312A1D0A17582D4761726D696E2D557365722D496E6974696174656412023F312A1E0A10582D4761726D696E2D556E69742D4944120A333631323932383336382A270A17582D4761726D696E2D53572D506172742D4E756D626572120C3030362D42343630332D30302A220A19582D4761726D6919D1";
    private static final String WEATHER_FORECAST_HOUR_CHUNK1 = // forecast/hour request, chunk 1 (truncated frame)
            "00078C01B3134203010101030E020103780101FF128B042A88040ACD0168747470733A2F2F6170692E6763732E6761726D696E2E636F6D2F776561746865722F76322F666F7265636173742F686F75723F6C61743D363730343035333830266C6F6E3D343431323631393339266475726174696F6E3D3134267370656564556E69743D4D45544552535F5045525F5345434F4E442674656D70556E69743D43454C53495553267072657373757265556E69743D494E434845535F4F465F4D455243555259267669736962696C697479556E69743D4D455445522674696D65734F66496E74";
    private static final String WEATHER_FORECAST_HOUR_CHUNK2 = // forecast/hour request, "final" chunk (middle chunk was lost)
            "00021A05B31344030101010206010102060101066A041A0208037E1900";
    private static final String STATUS_MSG = // protobuf status message, the next frame after the truncated one
            "000211058813B41303370301010101010337E400";
    private static final String STATUS_MSG_DECODED =
            "11008813B41300370300000000000037E4";

    // A truncated multi-chunk frame (a lost BLE notification) must not wedge the
    // decoder - it is discarded and the next complete frame decodes normally.
    @Test
    public void testTruncatedFrameDoesNotWedgeDecoder() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_HOUR_CHUNK1));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        // would previously throw BufferUnderflowException and leave the decoder wedged
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_HOUR_CHUNK2));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        // the next complete frame must decode normally
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(STATUS_MSG));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(STATUS_MSG_DECODED),
                cobsCoDec.retrieveMessage()
        );
    }

    // A stream that starts mid-frame (no leading 0x00) must be discarded, not wedge the
    // decoder: the next complete frame still decodes.
    @Test
    public void testOutOfSyncStreamRecovers() {
        // mid-frame continuation bytes ending with a trailing 0x00: the buffer is
        // out of sync (no leading 0x00) and must be discarded
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_DAY_CHUNK2));
        Assert.assertNull(cobsCoDec.retrieveMessage());

        // mid-frame continuation bytes without a trailing 0x00 stay in the buffer;
        // when the next complete frame arrives, both are discarded as out of sync
        final String midFrame = WEATHER_FORECAST_DAY_CHUNK2.substring(0, WEATHER_FORECAST_DAY_CHUNK2.length() - 2);
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(midFrame));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(STATUS_MSG));
        Assert.assertNull(cobsCoDec.retrieveMessage());

        // the next complete frame decodes normally
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(STATUS_MSG));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(STATUS_MSG_DECODED),
                cobsCoDec.retrieveMessage()
        );
    }

    // Multi-chunk frames decode correctly, a truncated frame is skipped, and decoding
    // continues with the following frame.
    @Test
    public void testMultiChunkFramesAndTruncatedFrameRecovery() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_CURRENT_CHUNK1));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_CURRENT_CHUNK2));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(WEATHER_CURRENT_DECODED),
                cobsCoDec.retrieveMessage()
        );

        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_DAY_CHUNK1));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_DAY_CHUNK2));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(WEATHER_FORECAST_DAY_DECODED),
                cobsCoDec.retrieveMessage()
        );

        // truncated frame - the middle chunk was lost on the BLE link
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_HOUR_CHUNK1));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(WEATHER_FORECAST_HOUR_CHUNK2));
        Assert.assertNull(cobsCoDec.retrieveMessage());

        // decoder recovered - the following frame decodes
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(STATUS_MSG));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(STATUS_MSG_DECODED),
                cobsCoDec.retrieveMessage()
        );
    }

    // A buffer overflow (>10 KB without a frame terminator) must reset the decoder
    // instead of wedging it: the next complete frame still decodes.
    @Test
    public void testBufferOverflowResetsDecoder() {
        final byte[] junk = new byte[10_000];
        Arrays.fill(junk, (byte) 0x55); // no 0x00 - the frame never completes
        cobsCoDec.receivedBytes(junk);             // fills the buffer exactly
        cobsCoDec.receivedBytes(new byte[]{0x55}); // overflows -> decoder reset
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray(STATUS_MSG));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray(STATUS_MSG_DECODED),
                cobsCoDec.retrieveMessage()
        );
    }

    // reproducible fuzzing
    @Test
    public void TestEncodeDecodeRoundTrip() {
        Random random = new Random(192837465L);

        // general fuzzing
        for (int round = 0; round < 100_000; round++) {
            // lower bound due to minimal GFDI overhead
            // upper bound due to capacity of CobsCoDec's byteBuffer
            byte[] payload = new byte[random.nextInt(4, 10_000 / 2 - 2)];

            // "random" payload
            random.nextBytes(payload);

            // minimum MTU
            chunkedEncodeDecode(payload, 23);

            // maximum MTU
            chunkedEncodeDecode(payload, 517);

            // "random" MTU
            int mtu = random.nextInt(23, 517);
            chunkedEncodeDecode(payload, mtu);
        }


        // small payload length
        for (int len = 1; len <= 517; len++) {
            byte[] payload = new byte[len];
            random.nextBytes(payload);
            chunkedEncodeDecode(payload, 517);
        }

        // MTU range
        for (int mtu = 23; mtu <= 517; mtu++) {
            // upper bound due to capacity of CobsCoDec's byteBuffer
            byte[] payload = new byte[random.nextInt(mtu + 1, 10_000 / 2 - 2)];
            random.nextBytes(payload);
            chunkedEncodeDecode(payload, mtu);
        }

        // systematic exploration at the boundaries
        for (int len : new int[]{1, 2, 3, 0xFE, 0xFF, 0x100}) {
            byte[] bytes = new byte[len];
            random.nextBytes(bytes);

            // all combinations for last two values
            for (int x = Byte.MIN_VALUE; x <= Byte.MAX_VALUE; x++) {
                bytes[len - 1] = (byte) x;
                if (len > 1) {
                    for (int y = Byte.MIN_VALUE; y <= Byte.MAX_VALUE; y++) {
                        bytes[len - 2] = (byte) y;
                        chunkedEncodeDecode(bytes, random.nextInt(23, 517));
                    }
                } else {
                    chunkedEncodeDecode(bytes, 23);
                }
            }

            // all the same value
            for (int every = Byte.MIN_VALUE; every <= Byte.MAX_VALUE; every++) {
                Arrays.fill(bytes, (byte) every);
                chunkedEncodeDecode(bytes, 517);
            }
        }
    }

    private void chunkedEncodeDecode(final byte[] payload, final int mtu) {
        int maxChunkSize = calcMaxWriteChunk(mtu);
        byte[] encodedData = CobsCoDec.encode(payload);

        int end;
        for (int start = 0; true; start = end) {
            end = start + maxChunkSize;
            if (end >= encodedData.length) {
                end = encodedData.length;
            }
            if (end <= start) {
                break;
            }
            byte[] chunk = java.util.Arrays.copyOfRange(encodedData, start, end);
            cobsCoDec.receivedBytes(chunk);
        }
        byte[] decodedData = cobsCoDec.retrieveMessage();
        Assert.assertArrayEquals(payload, decodedData);
    }
}
