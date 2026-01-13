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
import android.content.Context;
import android.net.Uri;

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.v2.CommunicatorV2;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.IncomingFitDefinitionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
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
        byte[] weatherDataMessage = GB.hexStringToByteArray("5901941306003f3672953f3672951a0e1b0200b4000a7a1a2b211bba1707fe6f1d426f6c7a616e6f2c204974616c790009013f367a1018020144037e0a360e3ecac083ff09013f36882017020144037e0a3b0e00000000ff09013f36963015020144037e0a3f0e00000000ff09013f36a44013010015037e0a430d00000000ff09013f36b25012010015037e00470d00000000ff09013f36c06012010015037e004a0d00000000ff09013f36ce7011010017037e004d0d00000000ff09013f36dc8010010017037e00500d00000000ff09013f36ea9010010017037e00510d00000000ff09013f36f8a00f000029037e00530c00000000ff09013f3706b00e000029037e00540c00000000ff09013f3714c00f000029037e00500c00000000ff0a023f3672950e1b0200030a023f3672950e1d0000040a023f3672950f1e0114050a023f36729511200114060a023f367295122100140052E3");
//        byte[] weatherDataMessage = GB.hexStringToByteArray("59019413060040684550406845500A060A010095640254095200000000000000004D79204C6F636174696F6E000000000901406848000A160087012A62547F3E99999AFF0901406856100A010168025460577F3DCCCCCDFF0901406864200901000504A85E557F00000000FF09014068723009010027025458517F00000000FF09014068804009010015037E53517F00000000FF090140688E5009010161037E4D4E7F00000000FF090140689C6008010162037E334E7F00000000FF09014068AA700701015A012A1A4F7F00000000FF09014068B8800701008D0254004E7F00000000FF09014068C690070100AC037E004D7F00000000FF09014068D4A007010099012A004E7F00000000FF09014068E2B0061600B4012A004F7F00000000FF0A0240684550060A0164040A0240684550060E1064050A02406845500A121061060A02406845500A0D0361000A0240684550090F106401C8DA");
//        byte[] weatherDataMessage = GB.hexStringToByteArray("590194130600405D7050405D70501007130000C20001920E3000000000000000004D79204C6F636174696F6E000000000901405D75B0100100D30245002D0040933333000901405D83C0120100B40245002A004099999A000901405D91D0120100A0048A002A0040900000000901405D9FE01301009E0649002B00406CCCCD000901405DADF013010096056A002F0040133333000901405DBC001201008703510036003FA00000000901405DCA101001003B048A003B003F000000000901405DD8200D01003B02450041003D4CCCCD000901405DE6300D16005A019200410000000000000901405DF4400C16007501BF00450000000000000901405E02500C16005A045E00450000000000000901405E10600B01007204B700490000000000000A02405D705007130000030A02405D70500814163A040A02405D705008141605050A02405D70500611035E060A02405D7050050C1640006324");
        FitDataMessage fitDataMessage = (FitDataMessage) GFDIMessage.parseIncoming(weatherDataMessage);

        byte[] weatherDefinitionMessage = GB.hexStringToByteArray("7b00931346000100800f000100fd04860904860101010e01010d01010201000302840501020402840601010701020a04850b0485080f0749000100800b000100fd04860101010201000302840402840501020701020f01011004881101004a0001008007000100fd04860e01010d01010201000501020c01003ae3");
        FitDefinitionMessage fitDefinitionMessage = (FitDefinitionMessage) GFDIMessage.parseIncoming(weatherDefinitionMessage);

        List<RecordData>  recordDataList = fitDataMessage.applyDefinitions(fitDefinitionMessage.getRecordDefinitions());

        List<RecordData> weatherData = new ArrayList<>();

        for (RecordData rec : recordDataList) {


            RecordData out = new RecordData(rec.getRecordDefinition(), rec.getRecordDefinition().getRecordHeader());

            for (int fieldNumber :
                    rec.getFieldsNumbers()) {
                Object o = rec.getFieldByNumber(fieldNumber);
                out.setFieldByNumber(fieldNumber, o);
                Assert.assertEquals(o, out.getFieldByNumber(fieldNumber));
            }
            weatherData.add(out);
            System.out.println(rec.toString());
            System.out.println(out.toString());

            Assert.assertEquals(rec.toString(), out.toString());

        }

        FitDataMessage recostructed = new FitDataMessage(weatherData);

        byte[] output = recostructed.getOutgoingMessage();

        Assert.assertArrayEquals(weatherDataMessage, output);


    }

    @Test
    public void incomingFitDefinition() {
        GarminSupport support = createSupport();

        final CobsCoDec cobsCoDec = new CobsCoDec();
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00021703AA13082088236905011303021F03033F0507045BC600"));//notificationcontrol message from log
        final byte[] deCobs = cobsCoDec.retrieveMessage();

        GFDIMessage pippo = GFDIMessage.parseIncoming(GB.hexStringToByteArray("1700AA13002088236905011300021F00033F0007045BC6"));//notificationcontrol message from log

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

        //RecordData{CAPABILITIES, sports=7, connectivity_supported=352360138, unknown_26(UINT32Z/4)=7}
        //RecordData{CONNECTIVITY, bluetooth_le_enabled=0, name=Forerunner 35, live_tracking_enabled=1, weather_conditions_enabled=1, auto_activity_upload_enabled=1, gps_ephemeris_download_enabled=1}

        //forse va in conflitto con ``private void enableWeather() {``???



//        final FitLocalMessageBuilder pippo = new FitLocalMessageBuilder(((IncomingFitDefinitionDeviceEvent) events.getFirst()).getRecordDefinitions());
//        pippo.getDefinitions();

    }
    @Test
    public void testBaseFields() {

        RecordDefinition recordDefinition = new RecordDefinition(new RecordHeader((byte) 6), ByteOrder.LITTLE_ENDIAN, GlobalFITMessage.WEATHER, null, null); //just some random data
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
    public void TestFitFileSettings2() throws FitParseException {

        byte[] fileContents = GB.hexStringToByteArray("0e101405b90600002e464954b18b40000000000603048c04048601028402" +
                "028405028400010000ed2adce7ffffffff01001906ffff02410000310002" +
                "000284010102015401ff4200000200160104860204860001020301000401" +
                "000501010a01000b01000c01000d01020e01020f01021001001101001201" +
                "001501001601001a01001b01001d01003401003501000200000000000000" +
                "0000000000030002000032ffffff0100fe00000001430000030013000807" +
                "0402840101000201020301020501000601000701000801020a01020b0102" +
                "0c01000d01000e0100100100110100120100150100180102036564676535" +
                "3130000c030129b70000003cb9b901000001a80200ff440000040004fe02" +
                "8401028b00010203010a04000058c3010145000006002400040703048627" +
                "040a290c0afe028404028b05028b06028b07028b0802840902840a02840b" +
                "02842a028b0101000201000c01020d01020e01020f010210010211010212" +
                "010213010214010215010a16010a17010a18010a23030224010025010226" +
                "010a28010a2b010a2c01000545564f00849eb90227350000171513121110" +
                "0f0e0d0c0b00000000000000000001ba300800005000f4010000ffff0101" +
                "0000000001fe01000000050032ff04ff020b000046000006002400050703" +
                "048627040a290c0afe028404028b05028b06028b07028b0802840902840a" +
                "02840b02842a028b0101000201000c01020d01020e01020f010210010211" +
                "010212010213010214010215010a16010a17010a18010a23030224010025" +
                "010226010a28010a2b010a2c0100065032534c0000000000273500001715" +
                "131211100f0e0d0c0b000100000000000000316e300800005a00f4010000" +
                "ffff01010000000001fe01000000050076be04ff020b0000470000060024" +
                "00090703048627040a290c0afe028404028b05028b06028b07028b080284" +
                "0902840a02840b02842a028b0101000201000c01020d01020e01020f0102" +
                "10010211010212010213010214010215010a16010a17010a18010a230302" +
                "24010025010226010a28010a2b010a2c0100074c414e47535445520013cc" +
                "1200273500001715131211100f0e0d0c0b000200000000000000632a3008" +
                "00005f00f4010000ffff010100000000010001000000050032ff04ff020b" +
                "000048000006002400020703048627040a290c0afe028404028b05028b06" +
                "028b07028b0802840902840a02840b02842a028b0101000201000c01020d" +
                "01020e01020f010210010211010212010213010214010215010a16010a17" +
                "010a18010a23030224010025010226010a28010a2b010a2c0100084d0000" +
                "000000352700001715131211100f0e0d0c0b000300000000000000697a30" +
                "0800005f00f4010000ffff010100000000010001000000050032ff04ff02" +
                "0b000049000006002400070703048627040a290c0afe028404028b05028b" +
                "06028b07028b0802840902840a02840b02842a028b0101000201000c0102" +
                "0d01020e01020f010210010211010212010213010214010215010a16010a" +
                "17010a18010a23030224010025010226010a28010a2b010a2c0100094269" +
                "6b6520350000000000273500001715131211100f0e0d0c0b000400000000" +
                "0000000000300800005f00f4010000ffff01010000000000fe0000000000" +
                "0032ff04ff020b00000942696b6520360000000000273500001715131211" +
                "100f0e0d0c0b0005000000000000000000300800005f00f4010000ffff01" +
                "010000000000fe00000000000032ff04ff020b00000942696b6520370000" +
                "000000273500001715131211100f0e0d0c0b000600000000000000000030" +
                "0800005f00f4010000ffff01010000000000fe00000000000032ff04ff02" +
                "0b00000942696b6520380000000000273500001715131211100f0e0d0c0b" +
                "0007000000000000000000300800005f00f4010000ffff01010000000000" +
                "fe00000000000032ff04ff020b00000942696b6520390000000000273500" +
                "001715131211100f0e0d0c0b0008000000000000000000300800005f00f4" +
                "010000ffff01010000000000fe00000000000032ff04ff020b00004a0000" +
                "06002400080703048627040a290c0afe028404028b05028b06028b07028b" +
                "0802840902840a02840b02842a028b0101000201000c01020d01020e0102" +
                "0f010210010211010212010213010214010215010a16010a17010a18010a" +
                "23030224010025010226010a28010a2b010a2c01000a42696b6520313000" +
                "00000000273500001715131211100f0e0d0c0b0009000000000000000000" +
                "300800005f00f4010000ffff01010000000000fe00000000000032ff04ff" +
                "020b00004b00007f00090309070001000401000501000601000701000801" +
                "000901000a01000b45646765203531300000ffffffffffffff09ef");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/Settings2.fit

        String expectedOutput = "[" +
                "FitFileId{type=SETTINGS, manufacturer=1, product=1561, serial_number=3889965805}, FitFileCreator{software_version=340}, " +
                "FitDeviceSettings{active_time_zone=0, utc_offset=0, time_offset=0, unknown_3(ENUM/1)=0, time_mode=0, time_zone_offset=0, unknown_10(ENUM/1)=3, unknown_11(ENUM/1)=0, backlight_mode=2, unknown_13(UINT8/1)=0, unknown_14(UINT8/1)=0, unknown_15(UINT8/1)=50, unknown_21(ENUM/1)=1, unknown_22(ENUM/1)=0, unknown_26(ENUM/1)=254, unknown_27(ENUM/1)=0, unknown_29(ENUM/1)=0, unknown_52(ENUM/1)=0, unknown_53(ENUM/1)=1}, " +
                "FitUserProfile{friendly_name=edge510, gender=1, age=41, height=183, weight=78.0, language=english, elev_setting=metric, weight_setting=metric, resting_heart_rate=60, default_max_biking_heart_rate=185, default_max_heart_rate=185, hr_setting=1, speed_setting=metric, dist_setting=metric, power_setting=1, activity_class=168, position_setting=2, temperature_setting=metric}, " +
                "FitHrmProfile{enabled=true, hrm_ant_id=50008, hrm_ant_id_trans_type=1, message_index=0}, " +
                "FitBikeProfile{name=EVO, odometer=457191.72, bike_power_ant_id=47617, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=8.0, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=true, crank_length=227.0, enabled=true, bike_power_ant_id_trans_type=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=0}, " +
                "FitBikeProfile{name=P2SL, odometer=0.0, bike_power_ant_id=28209, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.0, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=true, crank_length=227.0, enabled=true, bike_power_ant_id_trans_type=5, unknown_35(UINT8/3)=[0,118,190], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=1}, " +
                "FitBikeProfile{name=LANGSTER, odometer=12318.91, bike_power_ant_id=10851, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=true, crank_length=100.0, enabled=true, bike_power_ant_id_trans_type=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=2}, " +
                "FitBikeProfile{name=M, odometer=0.0, bike_power_ant_id=31337, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=true, crank_length=100.0, enabled=true, bike_power_ant_id_trans_type=5, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[53,39,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=3}, " +
                "FitBikeProfile{name=Bike 5, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=4}, " +
                "FitBikeProfile{name=Bike 6, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=5}, " +
                "FitBikeProfile{name=Bike 7, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=6}, " +
                "FitBikeProfile{name=Bike 8, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=7}, " +
                "FitBikeProfile{name=Bike 9, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=8}, " +
                "FitBikeProfile{name=Bike 10, odometer=0.0, custom_wheelsize=2.096, auto_wheelsize=0.0, bike_weight=9.5, power_cal_factor=50.0, auto_wheel_cal=true, auto_power_zero=true, id=0, spd_enabled=false, cad_enabled=false, spdcad_enabled=false, power_enabled=false, crank_length=227.0, enabled=false, unknown_35(UINT8/3)=[0,50,], unknown_36(ENUM/1)=4, front_gear_num=2, front_gear=[39,53,,], rear_gear_num=11, rear_gear=[23,21,19,18,17,16,15,14,13,12,11,], shimano_di2_enabled=false, message_index=9}, " +
                "FitConnectivity{bluetooth_enabled=0, name=Edge 510}" +
                "]";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());
        getAllFitFieldValues(fitFile);
    }

    @Test
    public void TestFitFileDevelopersField() throws FitParseException {
        byte[] fileContents = GB.hexStringToByteArray("0e206806a20000002e464954bed040000100000401028400010002028403048c00000f042329000006a540000100cf0201100d030102000101020305080d1522375990e97962db0040000100ce05000102010102020102031107080a0700000001646f7567686e7574735f6561726e656400646f7567686e7574730060000100140403010204010205048606028401000100008c580000c738b98001008f5a00032c808e400200905c0005a9388a1003d39e");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/DeveloperData.fit

        String expectedOutput = "[" +
                "FitFileId{type=ACTIVITY, manufacturer=15, product=9001, serial_number=1701}, " +
                "FitDeveloperData{application_id=[1,1,2,3,5,8,13,21,34,55,89,144,233,121,98,219], developer_data_index=0}, " +
                "FitFieldDescription{developer_data_index=0, field_definition_number=0, fit_base_type_id=1, field_name=doughnuts_earned, units=doughnuts}, " +
                "FitRecord{doughnuts_earned=1, heart_rate=140, cadence=88, distance=510.0, speed=47.488}, " +
                "FitRecord{doughnuts_earned=2, heart_rate=143, cadence=90, distance=2080.0, speed=36.416}, " +
                "FitRecord{doughnuts_earned=3, heart_rate=144, cadence=92, distance=3710.0, speed=35.344}" +
                "]";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());
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