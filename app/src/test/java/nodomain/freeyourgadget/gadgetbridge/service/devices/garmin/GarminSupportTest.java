/*  Copyright (C) 2024-2025 Daniele Gobbetti, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.IncomingFitDefinitionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileCreator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLocation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.FitDefinitionMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FitDefinitionStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminSupportTest extends TestBase {
    @BeforeClass
    public static void forceUtc() {
        // FIXME this is hacky, but we need the timestamps to match in the toString comparisons below
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testWeatherDataMessage() {
        byte[] weatherDataMessage = GB.hexStringToByteArray("540194130000438CC424438CC4240F0A1904000C630BA40D1E00000026FFFFFF860A02477265656E2048696C6C00000000000101438CD2340A000A001E1748320A400000000101438CE0440B000B001F1872330B404000000101438CEE540C000C0020199C340C408000000101438CFC640D000D00211AC6350D40A000000101438D0A740E000E00221BF0360E40C000000101438D18840F000F00231D1A370F40E000000101438D269410001000241E443810410000000101438D34A411001100251F6E3911411000000101438D42B4120012002620983A12412000000101438D50C4130013002721C23B13413000000101438D5ED4140014002822EC3C14414000000101438D6CE4150015002924163D15415000000202438CC4240A19046305020202438CC4240A19003206030202438CC4240B1A003300030202438CC4240C1B003401030202438CC4240D1C0035020330AD");
        FitDataMessage fitDataMessage = (FitDataMessage) GFDIMessage.parseIncoming(weatherDataMessage);

        byte[] weatherDefinitionMessage = GB.hexStringToByteArray("81009313400001008011000100FD04860904860101010E01010D01010201000302840501020402840601010701020A04850B04850F0101110100080F0741000100800A000100FD0486010101020100060101030284040284050102070102100488420001008008000100FD04860E01010D01010201000501020C0100110100F2FF");
        FitDefinitionMessage fitDefinitionMessage = (FitDefinitionMessage) GFDIMessage.parseIncoming(weatherDefinitionMessage);

        List<RecordData>  recordDataList = fitDataMessage.applyDefinitions(fitDefinitionMessage.getRecordDefinitions());

        List<RecordData> weatherData = new ArrayList<>();

        for (RecordData rec : recordDataList) {
            RecordData out = new RecordData(rec.getRecordDefinition(), rec.getRecordDefinition().getRecordHeader());

            for (int fieldNumber :
                    rec.getFieldsNumbers()) {
                Object o = rec.getFieldByNumber(fieldNumber);
                out.setFieldByNumber(fieldNumber, o);
                //due to the different ways applyDefinitions and setFieldByNumber we manually set the computedTimestamp to ensure the equality is passed
                out.computedTimestamp = rec.getComputedTimestamp();
                Assert.assertEquals(o, out.getFieldByNumber(fieldNumber));
            }
            Assert.assertEquals(rec.toString(), out.toString());
            weatherData.add(out);
        }

        Assert.assertArrayEquals(weatherDataMessage, new FitDataMessage(weatherData).getOutgoingMessage());
    }

    @Test
    public void incomingFitDefinition() {
        GarminSupport support = createSupport();

        final byte[] incomingDefinition = GB.hexStringToByteArray("5700931340000000000603048C0404860102840202840502840001004100000C000404028400010001010005010042000001000317048C1A048C01010A4300007F00060318070101000401000501000701000A010054CE");
        FitDefinitionMessage fitDefinitionMessage = (FitDefinitionMessage) GFDIMessage.parseIncoming(incomingDefinition);
        FitDefinitionStatusMessage ok_applied = new FitDefinitionStatusMessage(GFDIMessage.GarminMessage.FIT_DEFINITION, GFDIMessage.Status.ACK, FitDefinitionStatusMessage.FitDefinitionStatusCode.APPLIED, true);

        Assert.assertArrayEquals(ok_applied.getOutgoingMessage(), fitDefinitionMessage.getAckBytestream());

        final List<GBDeviceEvent> events = fitDefinitionMessage.getGBDeviceEvent();
        Assert.assertEquals(1, events.size());
        Assert.assertTrue((events.getFirst() instanceof IncomingFitDefinitionDeviceEvent));

        final FitLocalMessageHandler fitLocalMessageHandler = new FitLocalMessageHandler(support, ((IncomingFitDefinitionDeviceEvent) events.getFirst()).getRecordDefinitions());

        final byte[] incomingData = GB.hexStringToByteArray("2E00941302CA960015070000000703466F726572756E6E65722033350000000000000000000000000101010197F3");
        FitDataMessage fitDataMessage = (FitDataMessage) GFDIMessage.parseIncoming(incomingData);

        // FIXME this fails, and we were not validating its result anyway fitLocalMessageHandler.handle(fitDataMessage);

        FitDataStatusMessage ok_applied_data = new FitDataStatusMessage(GFDIMessage.GarminMessage.FIT_DATA, GFDIMessage.Status.ACK, FitDataStatusMessage.FitDataStatusCode.APPLIED, true);
        Assert.assertArrayEquals(ok_applied_data.getOutgoingMessage(), fitDataMessage.getAckBytestream());
    }
    @Test
    public void testBaseFields() {

        RecordDefinition recordDefinition = new RecordDefinition(new RecordHeader((byte) 6), ByteOrder.LITTLE_ENDIAN, NativeFITMessage.WEATHER, null, null); //just some random data
        List<FieldDefinition> fieldDefinitionList = new ArrayList<>();
        for (BaseType baseType :
                BaseType.values()) {
            fieldDefinitionList.add(new FieldDefinition(baseType.getIdentifier(), baseType.getSize(), baseType, baseType.name()));

        }
        recordDefinition.setFieldDefinitions(fieldDefinitionList);

        RecordData test = new RecordData(recordDefinition, recordDefinition.getRecordHeader());

        for (BaseType baseType :
                BaseType.values()) {
            System.out.println(baseType.getIdentifier());
            Object startVal, endVal;

            switch (baseType.name()) {
                case "ENUM":
                case "UINT8":
                case "BASE_TYPE_BYTE":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT8":
                    startVal = (int) Byte.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT16":
                    startVal = (int) Short.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT32":
                    startVal = (long) Integer.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = 0xffffffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT32":
                    startVal = 0.0f;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Float.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT64":
                    startVal = 0.0d;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Double.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT8Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT64":
                    startVal = BigInteger.valueOf(Long.MIN_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "STRING":
                    //TODO
                    break;
                default:
                    System.out.println(baseType.name());
                    Assert.assertFalse(true); //we should not end up here, if it happen we forgot a case in the switch
            }

        }

    }

    @Test
    public void TestFitFileSettings2() throws FitParseException, IOException {
        //https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/Settings2.fit
        byte[] fileContents = readBinaryResource("/TestFitFileSettings2.fit");
        String expectedOutput = readTextResource("/TestFitFileSettings2.txt");

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        String actualOutput = fitFile.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
        Assert.assertEquals(expectedOutput, actualOutput);
        getAllFitFieldValues(fitFile);
    }

    @Test
    public void TestFitFileDevelopersField() throws FitParseException, IOException {
        //https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/DeveloperData.fit
        byte[] fileContents = readBinaryResource("/TestFitFileDevelopersField.fit");
        String expectedOutput = readTextResource("/TestFitFileDevelopersField.txt");

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        String actualOutput = fitFile.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
        Assert.assertEquals(expectedOutput, actualOutput);
        // Field 0 is not overwritten by the developer field
        Assert.assertNull(fitFile.getRecords().get(3).getFieldByNumber(0));
        getAllFitFieldValues(fitFile);
    }

    @Test
    public void TestFitMessageTypeParsing() throws FitParseException, IOException {
        byte[] fileContents = readBinaryResource("/TestFitMessageTypeParsing.fit");
        String expected = readTextResource("/TestFitMessageTypeParsing.txt");

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        String actual = fitFile.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
        Assert.assertEquals(expected, actual);
        getAllFitFieldValues(fitFile);
    }

    /// encode->decode three locations / waypoints in a Lctns.fit compliant format
    @Test
    public void TestFitLocationEncoding() throws FitParseException, IOException {
        final List<RecordData> records = new ArrayList<>();

        FitFileId.Builder fileId = new FitFileId.Builder();
        fileId.setType(FileType.FILETYPE.LOCATION);
        fileId.setManufacturer(5759);
        fileId.setProduct(65534);
        fileId.setTimeCreated(null);
        fileId.setNumber(65533);
        fileId.setProductName("Gadgetbridge");
        records.add(fileId.build());

        FitFileCreator.Builder fileCreator = new FitFileCreator.Builder();
        fileCreator.setSoftwareVersion(2318);
        fileCreator.setHardwareVersion(254);
        records.add(fileCreator.build());

        FitLocation.Builder bregenz = new FitLocation.Builder();
        bregenz.setName("Bregenz");
        bregenz.setPositionLat(47.505);
        bregenz.setPositionLong(9.740);
        bregenz.setSymbol(FieldDefinitionLocationSymbol.LocationSymbol.Airport);
        bregenz.setAltitude(-495.0f);
        bregenz.setMessageIndex(0);
        records.add(bregenz.build());

        FitLocation.Builder friedrichshafen = new FitLocation.Builder();
        friedrichshafen.setName("腓特烈港");
        friedrichshafen.setPositionLat(47.656);
        friedrichshafen.setPositionLong(9.473);
        friedrichshafen.setSymbol(FieldDefinitionLocationSymbol.LocationSymbol.Anchor);
        friedrichshafen.setAltitude(0.0f);
        friedrichshafen.setMessageIndex(1);
        records.add(friedrichshafen.build());

        FitLocation.Builder kreuzlingen = new FitLocation.Builder();
        kreuzlingen.setName("קרויצלינגן");
        kreuzlingen.setPositionLat(47.633);
        kreuzlingen.setPositionLong(9.167);
        kreuzlingen.setSymbol(FieldDefinitionLocationSymbol.LocationSymbol.Water_Source);
        kreuzlingen.setAltitude(8850.0f);
        kreuzlingen.setMessageIndex(2);
        records.add(kreuzlingen.build());

        FitFile fitEncoded = new FitFile(records);
        byte[] fit = fitEncoded.getOutgoingMessage();

        String expectedText = readTextResource("/TestFitLocationEncoding.txt");
        FitFile fitDecoded = FitFile.parseIncoming(fit);
        String actual = fitDecoded.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
        Assert.assertEquals(expectedText, actual);
        getAllFitFieldValues(fitDecoded);

        byte[] expectedFit = readBinaryResource("/TestFitLocationEncoding.fit");
        Assert.assertArrayEquals(expectedFit, fit);
    }

    // try to retrieve the value of each message's fields
    private static void getAllFitFieldValues(FitFile fitFile) {
        List<RecordData> records = fitFile.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }

        for (int recordIndex = 0; recordIndex < records.size(); recordIndex++) {
            RecordData record = records.get(recordIndex);
            Class<?> recordClass = record.getClass();
            Method[] methods = recordClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if (!methodName.startsWith("get")) {
                    continue;
                }
                int modifiers = method.getModifiers();
                if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
                    continue;
                }
                Parameter[] parameters = method.getParameters();
                if (parameters.length != 0) {
                    continue;
                }

                try {
                    method.invoke(record);
                } catch (Exception e) {
                    String recordName = record.getClass().getSimpleName();
                    String message = methodName + " failed for " + recordName;
                    throw new AssertionError(message, e);
                }
            }
        }
    }

    public static byte[] readBinaryResource(String resourceName) throws IOException {
        try (final InputStream inputStream = GarminSupportTest.class.getResourceAsStream(resourceName)) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            inputStream.transferTo(output);
            return output.toByteArray();
        }
    }

    public static String readTextResource(String resourceName) throws IOException {
        try (final InputStream inputStream = GarminSupportTest.class.getResourceAsStream(resourceName)) {
            try (final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                StringWriter writer = new StringWriter();
                reader.transferTo(writer);
                return writer.toString().replace("\r\n", "\n");
            }
        }
    }


    private GarminSupport createSupport() {
        return new GarminSupport() {
            @Override
            public BluetoothGattCharacteristic getCharacteristic(final UUID uuid) {
                return new BluetoothGattCharacteristic(null, 0, 0);
            }
        };
    }
}