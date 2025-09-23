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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminSupportTest extends TestBase {
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
        getAllFitFieldValues(fitFile);
    }

    @Test
    public void TestFitMessageTypeParsing() throws FitParseException {
        byte[] fileContents = GB.hexStringToByteArray(
                "0E20B352812200002E464954455040000000000700010001028402028403048C" +
                        "040486050284080D070004FF00FEFF010000007F4533432A0047616467657442" +
                        "72696467650040000001000400030A01030A15048C17048C0001FFFE01FFFE01" +
                        "00000001000000400000020018000102010486020C860403000503010C010024" +
                        "01002704862806842E01002F01003701003801003906843A02843B0284500100" +
                        "5601005901005A04865E01025F0100860100AE010000010100000001000000FF" +
                        "FFFFFFFEFFFFFF01FF00047F1101017F4533430100FFFFFEFF010101010100FF" +
                        "FFFEFF01000100010101010000000101010140000003001DFE02840002070101" +
                        "000201020301020402840501000601000701000801020901020A01020B01020C" +
                        "01000D01000E010010010011010012010015010016028417030D1C04861D0486" +
                        "1E01001F02842002842F010031048600010061000101640A0001010101010101" +
                        "010101017F0101010001FFFE010000000100000001E803E80301010000004000" +
                        "00040005FE028400010001028B02010003010A00010001010001014000000500" +
                        "08FE028400010001028B02028403048604010005010A0701020001000101000A" +
                        "006400000001010140000006002028010A29030A2C0100FE0284000207010100" +
                        "02010003048604028B05028B06028B07028B0802840902840A02840B02840C01" +
                        "000D01000E01020F010010010011010012010013010214010015010A16010A17" +
                        "010A18010A25010226010A27030A000101FFFE01010061000101640000000100" +
                        "010001000100E803E8030A000A0001010101010101FF0101010101010101FFFE" +
                        "4000000700050101020201020302840501000701000001010100010140000008" +
                        "0003FE0284010102020207000100016100400000090003FE0284010284020207" +
                        "000100010061004000000A0004FE0284010102020284030102000100010A000A" +
                        "4000000C000300010001010003020700010161004000000D00041F0486200284" +
                        "2104869904860064000000E8030100000040420F004000000F000DFE02840001" +
                        "000101000204860304860401000504860601000704860801000902840A01000B" +
                        "010000010001017F4533437F4533430101000000010100000001010001014000" +
                        "0012009CBA0488BB0488C00102C10102C20102C30102C50102C60102C70102C8" +
                        "0102D00284D10284D202847D04867E04867F0486800486810284820284830102" +
                        "8402848502848602848901028B02848C04868D04868E04868F01029001029102" +
                        "849202849301029401029501029601019B02849C0486A80485A90284AA0284B4" +
                        "0284B50488B60488B70284600684610684620684630684640684650102660102" +
                        "6701026801026901026E02076F01027004867102847201017301017403027503" +
                        "027603027703027806847906847A03027B03027C04863B04863C02833D02833E" +
                        "02833F0283400102410C86420C86430C86440C86450486460284470284520284" +
                        "5302845402075506845606845702845802845902845A02845B02845C01025D01" +
                        "025E01025F06841A02841B01021C01001D04851E04851F048520048521028422" +
                        "02842302842402842502842604852704852904862A02842B01002C02842D0284" +
                        "2E01002F02843004863102843202843301023402833502833602833702833802" +
                        "833901013A0101FE0284FD048600010001010002048603048504048505010006" +
                        "01000704860804860904860A04860B02840D02840E02840F0284100102110102" +
                        "120102130102140284150284160284170284180102190284000000803F000080" +
                        "3F0101010101016464640064006400E8030000C9090000C9090000C909000001" +
                        "00010002640064000A000AE803E8030000E80300000100000001010100010001" +
                        "010101010001000000000001006400640064000000803F0000803F01006400FF" +
                        "FFA4016400FFFFA4010A00FFFF2A000A00FFFF2A000A00FFFF2A000202020202" +
                        "610001E80300000100010101FF0301FF0301FF0301FF030100FFFFFEFF0100FF" +
                        "FFFEFF01FFFE01FFFEE8030000E8030000E803E803E803E80301E8030000FFFF" +
                        "FFFF68100000E8030000FFFFFFFF68100000E8030000FFFFFFFF68100000E803" +
                        "0000FFFFFFFF68100000E80300000100C9090100010061000100FFFFFEFF0100" +
                        "FFFFFEFF640064000A0064000A008080806400FFFFA401010001010100000001" +
                        "0000000100000001000000010001000A00E803010001000000010000000A0000" +
                        "006400016400010001010001000000C909C90901640064006400640064000101" +
                        "01007F45334303017F45334301000000010000000101E8030000E80300006400" +
                        "00000100000001000100E803E8030101010101000100010001000A0100400000" +
                        "13007B9504889604889702849904889A04889C01029D01029E02849F0284A002" +
                        "846501016603026703026803026903026A06846B06846C03026D03026E04866F" +
                        "0486700486710486720486730284740284750102760284770284780284790284" +
                        "7A04867B04867C01018802848902849301029401023D02843E02843F01024702" +
                        "844A02844B06844C06844D02844E02844F028450010251010252010253028454" +
                        "06845506845606845706845806845906845B01025C01025D01025E01025F0102" +
                        "6204866302846401011901001A01022002842102842202842302842502842601" +
                        "002701002802842904862A02842B02842C01022D02832E02832F028330028331" +
                        "0283320101330101340486350283360283370283380283390C863A0C863B0C86" +
                        "3C0C86FE0284FD04860001000101000204860304850404850504850604850704" +
                        "860804860904860A04860B02840C02840D02840E02840F010210010211010212" +
                        "0102130284140284150284160284170100180100000000803F0000803F010000" +
                        "00803F0000803F64646400640064000101FF0301FF0301FF0301FF030100FFFF" +
                        "FEFF0100FFFFFEFF01FFFE01FFFEE8030000E8030000C9090000C9090000C909" +
                        "00000100010002640064000A00E803E8030000E8030000016400640001010100" +
                        "C90901010001000100FFFFFEFF0100FFFFFEFF0A0064000A0080808001006400" +
                        "FFFFA4016400FFFFA4016400FFFFA4010A00FFFF2A000A00FFFF2A000A00FFFF" +
                        "2A000202020202E8030000010001010101000100010001006400010101000100" +
                        "0000C909C90901640064006400640064000101E8030000E803E803E803E803E8" +
                        "030000FFFFFFFF68100000E8030000FFFFFFFF68100000E8030000FFFFFFFF68" +
                        "100000E8030000FFFFFFFF6810000001007F45334303017F4533430100000001" +
                        "0000000100000001000000E8030000E8030000640000000100000001000100E8" +
                        "03E80301010101010001000100010001014000001400545B04865C04865D0486" +
                        "5E04865F04866004866101026202846301026C02847204887304887402847502" +
                        "847601027701027801027B04867C02847D02847E02847F04858101028B02842D" +
                        "01022E01022F0102300102310100320102330284340284350102360284370284" +
                        "3802843902843A02843B02843E01024301014401014503024603024703024803" +
                        "024904864E0486510102520284530284540284550284570284FD048600048501" +
                        "048502028403010204010205048606028407028408030D0902830A01020B0485" +
                        "0C01020D01011103021201021304861C02841D04861E01021F01022002832102" +
                        "842702842802842902842A01002B01022C01020001000000E8030000E8030000" +
                        "0100000001000000010000000101000164000000803F0000803F640001000101" +
                        "0101000000640064006400E80300006464000202028001016400000180640064" +
                        "0064000A000A000A0001010101FF0301FF0301FF0301FF03E8030000C9090000" +
                        "020100640064000A0064007F4533430100000001000000C909010164000000E8" +
                        "03010001FFFE640001E8030000640110FF4301010000000100010000000101E8" +
                        "0301000A0064000A0001020240000015001309010A0A010A0B010A0C010A0D01" +
                        "020E01000F0486150100160102170102180102FD048600010001010002028403" +
                        "0486040102070284080284000101010101017F45334301010A0A7F4533430301" +
                        "0100010000000101000100400000170013FD048600010201010202028403048C" +
                        "0402840502840601020704860A02840B010212010013020714010A15028B1601" +
                        "001901001B0207200102007F4533430101010001000000010064000101000000" +
                        "00010101610001010001016100014000001A0009FE028404010005048C060284" +
                        "0802070B01000E02840F01001102070001000101000000010061000164000161" +
                        "004000001B00130604860701000802070901000A02840B02840C02840D028413" +
                        "0100140486150486160486FE0284000207010100020486030100040486050486" +
                        "0001000000016100010100010064000100010100000001000000010000000100" +
                        "610001640000000101000000010000004000001C000700028401028402048C03" +
                        "04860401000501000604860001000100010000007F4533430101010000004000" +
                        "001E000EFD048600028401028402028403028404028405028407028408010209" +
                        "02840A01020B01020C02840D0284007F45334364006400640064006400640004" +
                        "00010400010101000A004000001F000404010005020706048C07010000016100" +
                        "0100000001400000200008FE0284010486020485030485040486050100060207" +
                        "0801000001007F4533430100000001000000640000000161000140000021000A" +
                        "FE0284FD04860004860104860204860301000404860502840604860901020001" +
                        "007F453343010000000100000001000000010100000001000100000001400000" +
                        "220008FD0486000486010284020100030100040100050486060102007F453343" +
                        "E803000001000103010100000001400000230003FE0284030284050207000100" +
                        "64006100400000250006FE028400010001010A02020703028404048600010002" +
                        "016100010001000000400000260005FE02840001000102840201000302840001" +
                        "00020100010100400000270005FE028400010001028402010203028400010002" +
                        "01000101004000003100020002840101020001000140000033000BFD04860002" +
                        "84010284020284030284040284050284060102070100080100090284007F4533" +
                        "430100010001000100010001000101010100400000350003FE02840002840102" +
                        "07000100E803610040000037001D220284FD0486000102010284020486030486" +
                        "0404860501000601000701000802840902840A02840B04860C02830E02830F02" +
                        "8310068413028418010D1901021A02841B01021C01021D02841E04861F048620" +
                        "04862102840001007F4533430101006400000002000000E80300000101010100" +
                        "01000100010000006400640064000100FFFFFEFF010001010100010A01000100" +
                        "0000E8030000E80300000100400000480006FD04860001000102840202840304" +
                        "8C040486007F4533430201000100010000007F4533434000004E000100068400" +
                        "E803FFFF6810400000500006FD048600028401010D02030D03010204030D007F" +
                        "45334300800101FFFE0101FFFE400000510006FD048600028401010D02030D03" +
                        "010204030D007F45334300800101FFFE0101FFFE40000052000500010201010A" +
                        "02028B03010A04010200010101000101400000650016FE0284FD048600010001" +
                        "01000204860304860404860502840602840701000901020A01020B02840C0100" +
                        "1202841302841406841506841602841702841801021901020001007F45334303" +
                        "017F453343E8030000E80300000100E803010101010001010001000100FFFFFE" +
                        "FF0100FFFFFEFF640064000101400000670006FD048600048601030003068404" +
                        "0684050284007F4533430100000001FF008813FFFF08528813FFFF0852010040" +
                        "0000690000004000006A000200028401028400010001004000007F000D000100" +
                        "0101000201000302070401000501000601000701000801000901000A01000B01" +
                        "000C0100000101016100010101010101010101400000800010FD048600010001" +
                        "01010201000302840402840501020601010701020802070904860A04850B0485" +
                        "0C01000D01010E0101007F4533430101010100E80301010161007F4533430100" +
                        "000001000000010101400000810006FD04860002070104860204860301000401" +
                        "00007F45334361007F4533437F4533430101400000830003FE02840001020102" +
                        "07000100016100400000840006FD0486000284010102060302090C860A030D00" +
                        "7F4533430080FF01FFFE00040000FFFFFFFFCD10000001FFFE4000008E005F54" +
                        "04885504885604885704885901025A01025B04865C04865D04863902843A0100" +
                        "3B01023C01023D01023E01023F01024001004102074201024301024401024502" +
                        "844602844704864802844901014A01014B03024C03024D03024E03024F068450" +
                        "06845103025203025302841C04851D02071E02841F0284200100210486220284" +
                        "2302842401022502832602832702832802832902832A01012B01012C04862D02" +
                        "832E02832F0283300283310C86320C86330C86340C8635028436028437010238" +
                        "0486FE0284FD0486000100010100020486030485040485050485060485070486" +
                        "0804860904860A04860B02840C02840D02840E02840F01021001021101021201" +
                        "021302841402841502841602841701001801021904851A04851B048500000080" +
                        "3F0000803F0000803F0000803F6464C9090000C9090000C90900000100010202" +
                        "02020201610080808001000100E80300000100010101FF0301FF0301FF0301FF" +
                        "030100FFFFFEFF0100FFFFFEFF01FFFE01FFFE01000100000061000100010001" +
                        "01000000C909C90901640064006400640064000101E8030000E803E803E803E8" +
                        "03E8030000FFFFFFFF68100000E8030000FFFFFFFF68100000E8030000FFFFFF" +
                        "FF68100000E8030000FFFFFFFF681000000100C90901E803000001007F453343" +
                        "03017F45334301000000010000000100000001000000E8030000E80300006400" +
                        "00000100000001000100E803E803010101010100010001000100010101000000" +
                        "0100000001000000400000910006FA048600030D01028402028403010204030A" +
                        "000100000001FFFE010001000101FFFE40000094000900020701020702010003" +
                        "0100040486050486060102070100080100006100610001010100000001000000" +
                        "010101400000950007FE02840002070101000204860304860404860502070001" +
                        "006100010100000001000000E80300006100400000960007FE02840104850204" +
                        "85030486040284050C86060486000100010000000100000064000000C909E803" +
                        "0000FFFFFFFF68100000C9090000400000970009FE0284010207030100040486" +
                        "070300080C86090C860A06070B01020001006100010100000001FF0001000000" +
                        "FFFFFFFFFEFFFFFF01000000FFFFFFFFFEFFFFFF6100007A7A00014000009E00" +
                        "07FE028400010001010002028403028404028405010000010001010100010064" +
                        "00014000009F0003FE028400010001010D0001000101400000A00009FD048600" +
                        "0284010485020485030486040486050284060486070683007F45334301000100" +
                        "000001000000C9090000E803000064007F4533436400FF7FA401400000A10005" +
                        "FD0486000284010100020207030100007F453343010001610001400000A20007" +
                        "FD0486000284010486020284030486040284050284007F45334300807F453343" +
                        "00800100000001000100400000A40009FD048600028401068402068403068404" +
                        "0684050C88060C88070C88007F45334301000100FFFFFEFF0100FFFFFEFF0100" +
                        "FFFFFEFF0100FFFFFEFF0000803FFFFFFFFF666686400000803FFFFFFFFF6666" +
                        "86400000803FFFFFFFFF66668640400000A5000CFD0486000284010684020684" +
                        "030684040684050C88060C88070C880806830906830A0683007F453343010001" +
                        "00FFFFFEFF0100FFFFFEFF0100FFFFFEFF0100FFFFFEFF0000803FFFFFFFFF66" +
                        "6686400000803FFFFFFFFF666686400000803FFFFFFFFF666686400100FF7FFE" +
                        "7F0100FF7FFE7F0100FF7FFE7F400000A70007FD048600010001048602048603" +
                        "0486040C85050C85007F4533430101000000010000000100000001000000FFFF" +
                        "FF7FFEFFFF7FFFFF0000FFFFFF7F2F330400400000A90003FD04860002840104" +
                        "86007F453343010001000000400000AE0009FD048600028401068402010D0303" +
                        "0D040302050C86060486070284007F45334301000100FFFFFEFF0101FFFE01FF" +
                        "FE01000000FFFFFFFFFEFFFFFF7F4533430100400000B10003FD048600028401" +
                        "0207007F45334301006100400000B2000CFD0486000284010C86020683030683" +
                        "0406830506830606830703000803020906840A0684007F453343010001000000" +
                        "FFFFFFFFFEFFFFFFBE28FF7FFF7FBE28FF7FFF7F6400FF7FA4016400FF7FA401" +
                        "0004FF7FCD1001FF0001FFFEBE28FFFF20AB0100FFFFFEFF400000B800030002" +
                        "07010207020486006100610001000000400000B90003FE028400028401020700" +
                        "010001006100400000BA0003FE028400028401020700010001006100400000BB" +
                        "00070002840104860202840304860402840604860704860001007F4533430100" +
                        "7F45334301000100000001000000400000BC0002FD0486000100007F45334301" +
                        "400000C800040001020101020201000301000001010101400000C90006000102" +
                        "01010D0201020301020401000506070001010101016100007A7A00400000CA00" +
                        "0B00010201010D0201020301020401020501020601020801000901000A01000B" +
                        "0100000101010101010101010101400000CE000E000102010102020102030607" +
                        "0401020502070601020701010806070902070A02070D02840E02840F01020001" +
                        "01016100007A7A0001610001016100007A7A00610061000100010001400000CF" +
                        "000500030D01030D0202840301020404860001FFFE01FFFE0100010100000040" +
                        "0000D00009FD0486000284010684020684030684040684050C88060C88070C88" +
                        "007F45334301000100FFFFFEFF0100FFFFFEFF0100FFFFFEFF0100FFFFFEFF00" +
                        "00803FFFFFFFFF666686400000803FFFFFFFFF666686400000803FFFFFFFFF66" +
                        "668640400000D10004FD0486000284010684020C86007F45334301000100FFFF" +
                        "FEFF01000000FFFFFFFFFEFFFFFF400000D20006FD0486000100010486020486" +
                        "030486040485007F4533430101000000010000000100000001000000400000D3" +
                        "0003FD0486000102010102007F4533430101400000D80011FD04860002840102" +
                        "84020C86030C86040C86050C860603020706840803020906840A01000B01020C" +
                        "01020D01020E01000F0284007F45334301000100E8030000FFFFFFFF68100000" +
                        "E8030000FFFFFFFF68100000E8030000FFFFFFFF68100000E8030000FFFFFFFF" +
                        "6810000001FFFEE803FFFF681001FFFE0100FFFFFEFF01010101010100400000" +
                        "E1000BFE04860004860302840402840501020604860706840806840902840A02" +
                        "840B0284007F453343E803000001001000017F4533430100FFFFFEFF0100FFFF" +
                        "FEFF010001000100400000E300020002830104860001007F453343400000E500" +
                        "080004860202840501000601000801000901000C01000D0100007F4533430A00" +
                        "0101010101014000000201231A01021B04861D01001E01002301002401022501" +
                        "00FD0486FE028400020701010002010203010204010005048806010207010208" +
                        "01020901000A04880B04860C01000D04860E01000F0102100102110284120284" +
                        "1301001401021502841601001701021804861901000064E80300000101010A01" +
                        "7F45334301006100FF0101010000803F646464010000803F0100000001010000" +
                        "0001010101000100010101000164E803000001400000030105FE028400010201" +
                        "01020201000301000001000101010140000006010DFE02840004860104850201" +
                        "000301000401000503000604860701000801000901000A01000B0485000100E8" +
                        "0300000100000001010101FF000100000001010101E8030000400000080104FE" +
                        "0284000284010284020607000100010001006100007A7A004000000C0117FD04" +
                        "860002840102840204860304860404860501020601020702840802840902840A" +
                        "04860B04860C02840D02840E02840F0486100486110485160486170486180486" +
                        "190486007F45334301000100E8030000E8030000010000000101010001000100" +
                        "01000000E8030000640064006400E8030000E8030000E8030000E8030000E803" +
                        "0000E8030000E80300004000000D0104FD0486000102010102020100007F4533" +
                        "43010101400000130102FD0486000100007F453343014000001D010AFD048600" +
                        "0488010488020102030488040488050485060485070284080486007F45334300" +
                        "00803F0000803F010000803F0000803F0100000001000000E803E80300004000" +
                        "00210106FD0486000284010486020284030102040284007F4533430100010000" +
                        "000100011900400000220103FD0486000284010684007F45334301000100FFFF" +
                        "FEFF400000290102FD0486000283007F45334364004000002E0107FD04860002" +
                        "84010284020683030683040683050486007F453343010001000100FF7F040001" +
                        "00FF7F04000100FF7F040001000000400000300103FD0486000284010C86007F" +
                        "453343010001000000FFFFFFFFFEFFFFFF400000310104FD0486000284010302" +
                        "020302007F453343010001FFFE01FFFE400000320103FD048600028401030100" +
                        "7F4533430100017F7E400000330103FD0486000284010683007F453343010064" +
                        "00FF7FA401400000340104FD0486000284010102020302007F45334301000101" +
                        "FFFE400000380113FE02840001000104860204860304860404860904860D0284" +
                        "0E02841504851604851704851804851904861A04851B04861C04864A04866E04" +
                        "8600010002E8030000E803000064000000E80300007F45334301000100010000" +
                        "00010000000100000001000000E8030000E80300007F45334301000000C90900" +
                        "00E803000040000039010EFE0284000100030284040486050486060486070486" +
                        "0802840902840A01020B01020C04850D04864D0486000100020100E803000064" +
                        "000000E8030000E8030000010001000101E803000001000000E8030000400000" +
                        "3A0105FD0486000284010301020683030683007F4533430100017F7E0100FF7F" +
                        "FE7F0100FF7FFE7F4000003B0102FD0486000102007F453343014000003D0107" +
                        "FD0486000485010485020100030284040102050488007F453343010000000100" +
                        "0000010100010000803F4000003F0103FD048600048C010284007F4533430100" +
                        "00006400400000430105FD048600048C010284020284030486007F4533430100" +
                        "000064006400640000004000005A010E00010201010202010203010204010205" +
                        "01020601020701020801020901020A01020B01020E01020F0284000101010101" +
                        "01010101010101016400400000720108FD048600028401028402028403028404" +
                        "0284050284060100007F45334380008000800080008000800001400000730102" +
                        "FD0486000284007F4533438000400000740106FD048600028401068402068403" +
                        "0302040302007F45334301000100FFFFFEFF0100FFFFFEFF01FFFE01FFFE4000" +
                        "00770105FD0486000102010284020102030102007F4533430100010101400000" +
                        "780107FD0486000284010284020683030683040683050486007F453343010001" +
                        "001D00FF7F78001D00FF7F78001D00FF7F780001000000400000830108FD0486" +
                        "000486010486020486030284040100050486060486007F453343E8030000E803" +
                        "0000E80300000100010A000000E8030000400000840103FD0486000486010284" +
                        "007F453343E80300000100400000850103FD048600030D010102007F45334301" +
                        "FFFE0140000089010DFE02840004860104850201000301000401000503000604" +
                        "860701000801000901000A01000B0485000100E80300000100000001010101FF" +
                        "000100000001010101E80300004000008E0105FD048600048601048802048804" +
                        "0488007F453343010000000000803F0000803F0000803F400000990103FD0486" +
                        "000284010684007F4533430100E803FFFF681040000013000C00010001010075" +
                        "0102FE0284020486FD04863904863A08863B0C86540284550484560684000B03" +
                        "2802007F4533437F453343E8030000E8030000D0070000E803000000000000B8" +
                        "0B000064006400C800640000002C01A3E8"
        );
        String expectedOutput =
                "[FitFileId{type=ACTIVITY, manufacturer=255, product=65534, serial_number=1, time_created=1758499199, number=42, product_name=GadgetBridge}, " +
                        "FitCapabilities{languages=[1,255,254], sports=[1,255,254], workouts_supported=1, connectivity_supported=1}, " +
                        "FitDeviceSettings{active_time_zone=1, utc_offset=1, time_offset=[1,,4294967294], time_mode=[1,,0], time_zone_offset=[4,,17], backlight_mode=1, activity_tracker_enabled=1, clock_time=1127433599, pages_enabled=[1,,65534], move_alert_enabled=1, date_mode=1, display_orientation=1, mounting_side=1, default_page=[1,,65534], autosync_min_steps=1, autosync_min_time=1, lactate_threshold_autodetect_enabled=1, ble_auto_upload_enabled=1, auto_sync_frequency=1, auto_activity_detect=1, number_of_screens=1, smart_notification_display_orientation=1, tap_interface=1, tap_sensitivity=1}, " +
                        "FitUserProfile{friendly_name=a, gender=1, age=1, height=100, weight=1.0, language=french, elev_setting=imperial, weight_setting=imperial, resting_heart_rate=1, default_max_running_heart_rate=1, default_max_biking_heart_rate=1, default_max_heart_rate=1, hr_setting=1, speed_setting=imperial, dist_setting=imperial, power_setting=1, activity_class=127, position_setting=1, temperature_setting=imperial, local_id=1, global_id=[1,,254], wake_time=1, sleep_time=1, height_setting=imperial, user_running_step_length=1000, user_walking_step_length=1000, depth_setting=imperial, dive_count=1, message_index=1}, " +
                        "FitHrmProfile{enabled=true, hrm_ant_id=1, log_hrv=1, hrm_ant_id_trans_type=1, message_index=1}, " +
                        "FitSdmProfile{enabled=true, sdm_ant_id=1, sdm_cal_factor=1.0, odometer=1.0, speed_source=1, sdm_ant_id_trans_type=1, odometer_rollover=1, message_index=1}, " +
                        "FitBikeProfile{name=a, sport=1, sub_sport=1, odometer=1.0, bike_spd_ant_id=1, bike_cad_ant_id=1, bike_spdcad_ant_id=1, bike_power_ant_id=1, custom_wheelsize=1.0, auto_wheelsize=1.0, bike_weight=1.0, power_cal_factor=1.0, auto_wheel_cal=true, auto_power_zero=true, id=1, spd_enabled=true, cad_enabled=true, spdcad_enabled=true, power_enabled=true, enabled=true, bike_spd_ant_id_trans_type=1, bike_cad_ant_id_trans_type=1, bike_spdcad_ant_id_trans_type=1, bike_power_ant_id_trans_type=1, odometer_rollover=1, front_gear_num=1, front_gear=[1,255,254], rear_gear_num=1, rear_gear=[1,255,254], shimano_di2_enabled=true, message_index=1}, " +
                        "FitZonesTarget{max_heart_rate=1, threshold_heart_rate=1, functional_threshold_power=1, hr_calc_type=1, pwr_calc_type=1}, " +
                        "FitHrZone{high_bpm=1, name=a, message_index=1}, " +
                        "FitPowerZone{high_value=1, name=a, message_index=1}, " +
                        "FitMetZone{high_bpm=1, calories=1.0, fat_calories=1.0, message_index=1}, " +
                        "FitSport{sport=1, sub_sport=1, name=a}, " +
                        "FitTrainingSettings{target_distance=1.0, target_speed=1.0, target_time=1, precise_target_speed=1.0}, " +
                        "FitGoals{sport=1, sub_sport=1, start_date=1127433599, end_date=1127433599, type=distance, value=1, repeat=1, target_value=1, recurrence=1, recurrence_value=1, enabled=1, source=community, message_index=1}, " +
                        "FitSession{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_latitude=8.381903171539307E-8, start_longitude=8.381903171539307E-8, sport=1, sub_sport=1, total_elapsed_time=1000, total_timer_time=1000, total_distance=100, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, average_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, total_training_effect=1.0, first_lap_index=1, num_laps=1, event_group=1, trigger=1, nec_latitude=8.381903171539307E-8, nec_longitude=8.381903171539307E-8, swc_latitude=8.381903171539307E-8, swc_longitude=8.381903171539307E-8, num_lengths=1, normalized_power=1, training_stress_score=1.0, intensity_factor=1.0, left_right_balance=1, end_latitude=8.381903171539307E-8, end_longitude=8.381903171539307E-8, avg_stroke_count=10, avg_stroke_distance=1.0, swim_stroke=1, pool_length=1.0, threshold_power=1, pool_length_unit=1, num_active_lengths=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, min_heart_rate=1, time_in_hr_zone=[1.0,,4.2], time_in_speed_zone=[1.0,,4.2], time_in_cadence_zone=[1.0,,4.2], time_in_power_zone=[1.0,,4.2], avg_lap_time=1.0, best_lap_index=1, min_altitude=1.0, player_score=1, opponent_score=1, opponent_name=a, stroke_count=[1,,65534], zone_count=[1,,65534], max_ball_speed=1.0, avg_ball_speed=1.0, avg_vertical_oscillation=1.0, avg_stance_time_percent=1.0, avg_stance_time=1.0, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, avg_total_hemoglobin_conc=[1.0,,4.2], min_total_hemoglobin_conc=[1.0,,4.2], max_total_hemoglobin_conc=[1.0,,4.2], avg_saturated_hemoglobin_percent=[1.0,,4.2], min_saturated_hemoglobin_percent=[1.0,,4.2], max_saturated_hemoglobin_percent=[1.0,,4.2], avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, sport_profile_name=a, sport_index=1, stand_time=1000, stand_count=1, avg_left_pco=1, avg_right_pco=1, avg_left_power_phase=[1,,3], avg_left_power_phase_peak=[1,,3], avg_right_power_phase=[1,,3], avg_right_power_phase_peak=[1,,3], avg_power_position=[1,,65534], max_power_position=[1,,65534], avg_cadence_position=[1,,254], max_cadence_position=[1,,254], enhanced_avg_speed=1.0, enhanced_max_speed=1.0, enhanced_avg_altitude=1.0, enhanced_min_altitude=1.0, enhanced_max_altitude=1.0, avg_lev_motor_power=1, max_lev_motor_power=1, lev_battery_consumption=1.0, avg_vertical_ratio=1.0, avg_stance_time_balance=1.0, avg_step_length=1.0, total_anaerobic_training_effect=1.0, avg_vam=1.0, avg_depth=1.0, max_depth=1.0, surface_interval=1, start_cns=1, end_cns=1, start_n2=1, end_n2=1, avg_respiration_rate=1, max_respiration_rate=1, min_respiration_rate=1, min_temperature=1, o2_toxicity=1, dive_number=1, training_load_peak=1.0, enhanced_avg_respiration_rate=1.0, enhanced_max_respiration_rate=1.0, enhanced_min_respiration_rate=1.0, total_grit=1.0, total_flow=1.0, jump_count=1, avg_grit=1.0, avg_flow=1.0, workout_feel=1, workout_rpe=1, avg_spo2=1, avg_stress=1, hrv_sdrr=1, hrv_rmssd=1, total_fractional_ascent=1.0, total_fractional_descent=1.0, avg_core_temperature=1.0, min_core_temperature=1.0, max_core_temperature=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitLap{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_lat=8.381903171539307E-8, start_long=8.381903171539307E-8, end_lat=8.381903171539307E-8, end_long=8.381903171539307E-8, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, avg_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, intensity=1, lap_trigger=1, sport=1, event_group=1, num_lengths=1, normalized_power=1, left_right_balance=1, first_length_index=1, avg_stroke_distance=100, swim_style=BACKSTROKE, sub_sport=1, num_active_lengths=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, time_in_hr_zone=[1.0,,4.2], time_in_speed_zone=[1.0,,4.2], time_in_cadence_zone=[1.0,,4.2], time_in_power_zone=[1.0,,4.2], repetition_num=1, min_altitude=1.0, min_heart_rate=1, wkt_step_index=1, opponent_score=1, stroke_count=[1,,65534], zone_count=[1,,65534], avg_vertical_oscillation=1.0, avg_stance_time_percent=1.0, avg_stance_time=1.0, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, player_score=1, avg_total_hemoglobin_conc=[1.0,,4.2], min_total_hemoglobin_conc=[1.0,,4.2], max_total_hemoglobin_conc=[1.0,,4.2], avg_saturated_hemoglobin_percent=[1.0,,4.2], min_saturated_hemoglobin_percent=[1.0,,4.2], max_saturated_hemoglobin_percent=[1.0,,4.2], avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, time_standing=1.0, stand_count=1, avg_left_pco=1, avg_right_pco=1, avg_left_power_phase=[1,,3], avg_left_power_phase_peak=[1,,3], avg_right_power_phase=[1,,3], avg_right_power_phase_peak=[1,,3], avg_power_position=[1,,65534], max_power_position=[1,,65534], avg_cadence_position=[1,,254], max_cadence_position=[1,,254], enhanced_avg_speed=10.0, enhanced_max_speed=10.0, enhanced_avg_altitude=1.0, enhanced_min_altitude=1.0, enhanced_max_altitude=1.0, avg_lev_motor_power=1, max_lev_motor_power=1, lev_battery_consumption=1.0, avg_vertical_ratio=1.0, avg_stance_time_balance=1.0, avg_step_length=1.0, avg_vam=1.0, avg_depth=1.0, max_depth=1.0, min_temperature=1, enhanced_avg_respiration_rate=1.0, enhanced_max_respiration_rate=1.0, avg_respiration_rate=1, max_respiration_rate=1, total_grit=1.0, total_flow=1.0, jump_count=1, avg_grit=1.0, avg_flow=1.0, total_fractional_ascent=1.0, total_fractional_descent=1.0, avg_core_temperature=1.0, min_core_temperature=1.0, max_core_temperature=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitRecord{2025-09-22 01:59:59.000, latitude=8.381903171539307E-8, longitude=8.381903171539307E-8, altitude=1.0, heart_rate=1, cadence=1, distance=1.0, speed=1.0, power=1, compressed_speed_distance=[1,,254], grade=1.0, resistance=1, time_from_course=1.0, cycle_length=1.0, temperature=1, speed_1s=[1.0,,4.1875], cycles=1, total_cycles=1, compressed_accumulated_power=1, accumulated_power=1, left_right_balance=1, gps_accuracy=1, vertical_speed=1.0, calories=1, oscillation=1.0, stance_time_percent=1.0, stance_time=1.0, activity=1, left_torque_effectiveness=1.0, right_torque_effectiveness=1.0, left_pedal_smoothness=1.0, right_pedal_smoothness=1.0, combined_pedal_smoothness=1.0, time128=1.0, stroke_type=1, zone=1, ball_speed=1.0, cadence256=1.0, fractional_cadence=1.0, avg_total_hemoglobin_conc=1.0, min_total_hemoglobin_conc=1.0, max_total_hemoglobin_conc=1.0, avg_saturated_hemoglobin_percent=1.0, min_saturated_hemoglobin_percent=1.0, max_saturated_hemoglobin_percent=1.0, device_index=1, left_pco=1, right_pco=1, left_power_phase=[1,,3], left_power_phase_peak=[1,,3], right_power_phase=[1,,3], right_power_phase_peak=[1,,3], enhanced_speed=1.0, enhanced_altitude=1.0, battery_soc=1.0, motor_power=1, vertical_ratio=1.0, stance_time_balance=1.0, step_length=1.0, cycle_length16=1.0, absolute_pressure=1, depth=1.0, next_stop_depth=1.0, next_stop_time=1, time_to_surface=1, ndl_time=1, cns_load=1, n2_load=1, respiration_rate=1, enhanced_respiration_rate=100, grit=1.0, flow=1.0, current_stress=1.0, ebike_travel_rang=1, ebike_battery_level=1, ebike_assist_mode=1, ebike_assist_level_percent=1, air_time_remaining=1, pressure_sac=1.0, volume_sac=1.0, rmv=1.0, ascent_rate=1.0, po2=1.0, core_temperature=1.0, timestamp=1758499199}, " +
                        "FitEvent{2025-09-22 01:59:59.000, event=3, event_type=1, data16=1, data=1, event_group=1, score=1, opponent_score=1, front_gear_num=1, front_gear=1, rear_gear_num=1, rear_gear=1, device_index=1, activity_type=1, start_timestamp=1127433599, radar_threat_level_max=1, radar_threat_count=1, radar_threat_avg_approach_speed=1.0, radar_threat_max_approach_speed=1.0, timestamp=1758499199}, " +
                        "FitDeviceInfo{2025-09-22 01:59:59.000, device_index=1, device_type=1, manufacturer=1, serial_number=1, product=1, software_version=100, hardware_version=1, cum_operating_time=1, battery_voltage=1.0, battery_status=1, sensor_position=1, descriptor=a, ant_transmission_type=1, ant_device_number=1, ant_network=1, source_type=1, product_name=a, battery_level=1, timestamp=1758499199}, " +
                        "FitWorkout{2025-09-22 01:59:59.000, sport=1, capabilities=1, num_valid_steps=1, name=a, sub_sport=1, pool_length=1.0, pool_length_unit=1, notes=a, message_index=1}, " +
                        "FitWorkoutStep{2025-09-22 01:59:59.000, wkt_step_name=a, duration_type=1, duration_value=100, target_type=1, target_value=1, custom_target_value_low=1, custom_target_value_high=1, intensity=1, notes=a, equipment=1, exercise_category=1, exercise_name=1, exercise_weight=1.0, weight_display_unit=1, secondary_target_type=1, secondary_target_value=1, secondary_custom_target_value_low=1, secondary_custom_target_value_high=1, message_index=1}, " +
                        "FitSchedule{2025-09-22 01:59:59.000, manufacturer=1, product=1, serial_number=1, time_created=1127433599, completed=true, type=1, scheduled_time=1}, " +
                        "FitWeightScale{2025-09-22 01:59:59.000, weight=1.0, percent_fat=1.0, percent_hydration=1.0, visceral_fat_mass=1.0, bone_mass=1.0, muscle_mass=1.0, basal_met=1.0, physique_rating=1, active_met=1.0, metabolic_age=1, visceral_fat_rating=1, user_profile_index=1, bmi=1.0, timestamp=1758499199}, " +
                        "FitCourse{2025-09-22 01:59:59.000, sport=1, name=a, capabilities=1, sub_sport=1}, " +
                        "FitCoursePoint{2025-09-22 01:59:59.000, timestamp=1758499199, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, distance=1.0, type=1, name=a, favorite=1, message_index=1}, " +
                        "FitTotals{2025-09-22 01:59:59.000, timer_time=1, distance=1, calories=1, sport=1, elapsed_time=1, sessions=1, active_time=1, sport_index=1, timestamp=1758499199, message_index=1}, " +
                        "FitActivity{2025-09-22 01:59:59.000, total_timer_time=1000, num_sessions=1, type=1, event=3, event_type=1, local_timestamp=1, event_group=1, timestamp=1758499199}, " +
                        "FitSoftware{2025-09-22 01:59:59.000, version=1.0, part_number=a, message_index=1}, " +
                        "FitFileCapabilities{2025-09-22 01:59:59.000, type=2, flags=1, directory=a, max_count=1, max_size=1, message_index=1}, " +
                        "FitMesgCapabilities{2025-09-22 01:59:59.000, file=2, mesg_num=1, count_type=1, max_count=1, message_index=1}, " +
                        "FitFieldCapabilities{2025-09-22 01:59:59.000, file=2, mesg_num=1, field_num=1, count=1, message_index=1}, " +
                        "FitFileCreator{2025-09-22 01:59:59.000, software_version=1, hardware_version=1}, " +
                        "FitBloodPressure{2025-09-22 01:59:59.000, systolic_pressure=1, diastolic_pressure=1, mean_arterial_pressure=1, map_3_sample_mean=1, map_morning_values=1, map_evening_values=1, heart_rate=1, heart_rate_type=1, status=1, user_profile_index=1, timestamp=1758499199}, " +
                        "FitSpeedZone{2025-09-22 01:59:59.000, high_value=1.0, name=a, message_index=1}, " +
                        "FitMonitoring{2025-09-22 01:59:59.000, device_index=1, calories=1, distance=100, cycles=2, active_time=1000, activity_type=1, activity_subtype=1, activity_level=1, distance_16=1, cycles_16=1, active_time_16=1, local_timestamp=1, temperature=1.0, temperature_min=1.0, temperature_max=1.0, activity_time=[1,,65534], active_calories=1, current_activity_type_intensity=1, timestamp_min_8=1, timestamp_16=1, heart_rate=1, intensity=1.0, duration_min=1, duration=1, ascent=1.0, descent=1.0, moderate_activity_minutes=1, vigorous_activity_minutes=1, timestamp=1758499199}, " +
                        "FitTrainingFile{2025-09-22 01:59:59.000, type=2, manufacturer=1, product=1, serial_number=1, time_created=1127433599, timestamp=1758499199}, " +
                        "FitHrv{2025-09-22 01:59:59.000, time=[1000,,4200]}, " +
                        "FitAntRx{2025-09-22 01:59:59.000, fractional_timestamp=1.0, mesg_id=1, mesg_data=[1,,254], channel_number=1, data=[1,,254], timestamp=1758499199}, " +
                        "FitAntTx{2025-09-22 01:59:59.000, fractional_timestamp=1.0, mesg_id=1, mesg_data=[1,,254], channel_number=1, data=[1,,254], timestamp=1758499199}, " +
                        "FitAntChannelId{2025-09-22 01:59:59.000, channel_number=1, device_type=1, device_number=1, transmission_type=1, device_index=1}, " +
                        "FitLength{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, total_elapsed_time=1.0, total_timer_time=1.0, total_strokes=1, avg_speed=1.0, swim_stroke=1, avg_swimming_cadence=1, event_group=1, total_calories=1, length_type=1, player_score=1, opponent_score=1, stroke_count=[1,,65534], zone_count=[1,,65534], enhanced_avg_respiration_rate=0.1, enhanced_max_respiration_rate=0.1, avg_respiration_rate=1, max_respiration_rate=1, timestamp=1758499199, message_index=1}, " +
                        "FitMonitoringInfo{2025-09-22 01:59:59.000, local_timestamp=1, activity_type=[1,,0], steps_to_distance=[1.0,,4.2], steps_to_calories=[1.0,,4.2], resting_metabolic_rate=1, timestamp=1758499199}, " +
                        "FitPad{2025-09-22 01:59:59.000}, " +
                        "FitSlaveDevice{2025-09-22 01:59:59.000, manufacturer=1, product=1}, " +
                        "FitConnectivity{2025-09-22 01:59:59.000, bluetooth_enabled=1, bluetooth_le_enabled=1, ant_enabled=1, name=a, live_tracking_enabled=1, weather_conditions_enabled=1, weather_alerts_enabled=1, auto_activity_upload_enabled=1, course_download_enabled=1, workout_download_enabled=1, gps_ephemeris_download_enabled=1, incident_detection_enabled=1, grouptrack_enabled=1}, " +
                        "FitWeather{2025-09-22 01:59:59.000, weather_report=1, temperature=274, condition=PARTLY_CLOUDY, wind_direction=1, wind_speed=3.3557048, precipitation_probability=1, temperature_feels_like=274, relative_humidity=1, location=a, observed_at_time=1758499199, observed_location_lat=1, observed_location_long=1, day_of_week=MONDAY, high_temperature=274, low_temperature=274, timestamp=1758499199}, " +
                        "FitWeatherAlert{2025-09-22 01:59:59.000, report_id=a, issue_time=1127433599, expire_time=1127433599, severity=1, type=1, timestamp=1758499199}, " +
                        "FitCadenceZone{2025-09-22 01:59:59.000, high_value=1, name=a, message_index=1}, " +
                        "FitHr{2025-09-22 01:59:59.000, fractional_timestamp=1.0, filtered_bpm=[1,,254], event_timestamp=[1.0,,4.2001953125], event_timestamp_12=[1,,254], timestamp=1758499199}, " +
                        "FitSegmentLap{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_position_lat=8.381903171539307E-8, start_position_long=8.381903171539307E-8, end_position_lat=8.381903171539307E-8, end_position_long=8.381903171539307E-8, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, avg_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, sport=1, event_group=1, nec_lat=8.381903171539307E-8, nec_long=8.381903171539307E-8, swc_lat=8.381903171539307E-8, swc_long=8.381903171539307E-8, name=a, normalized_power=1, left_right_balance=1, sub_sport=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, time_in_hr_zone=[1.0,,4.2], time_in_speed_zone=[1.0,,4.2], time_in_cadence_zone=[1.0,,4.2], time_in_power_zone=[1.0,,4.2], repetition_num=1, min_altitude=1.0, min_heart_rate=1, active_time=1.0, wkt_step_index=1, sport_event=1, avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, status=1, uuid=a, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, front_gear_shift_count=1, rear_gear_shift_count=1, time_standing=1.0, stand_count=1, avg_left_pco=1, avg_right_pco=1, avg_left_power_phase=[1,,3], avg_left_power_phase_peak=[1,,3], avg_right_power_phase=[1,,3], avg_right_power_phase_peak=[1,,3], avg_power_position=[1,,65534], max_power_position=[1,,65534], avg_cadence_position=[1,,254], max_cadence_position=[1,,254], manufacturer=1, total_grit=1.0, total_flow=1.0, avg_grit=1.0, avg_flow=1.0, total_fractional_ascent=1.0, total_fractional_descent=1.0, enhanced_avg_altitude=1.0, enhanced_max_altitude=1.0, enhanced_min_altitude=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitMemoGlob{2025-09-22 01:59:59.000, memo=[1,,254], mesg_num=1, parent_index=1, field_num=1, data=[1,255,254], part_index=1}, " +
                        "FitSegmentId{2025-09-22 01:59:59.000, name=a, uuid=a, sport=1, enabled=1, user_profile_primary_key=1, device_id=1, default_race_leader=1, delete_status=1, selection_type=1}, " +
                        "FitSegmentLeaderboardEntry{2025-09-22 01:59:59.000, name=a, type=1, group_primary_key=1, activity_id=1, segment_time=1.0, activity_id_string=a, message_index=1}, " +
                        "FitSegmentPoint{2025-09-22 01:59:59.000, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, distance=1.0, altitude=1.0, leader_time=[1.0,,4.2], enhanced_altitude=1.0, message_index=1}, " +
                        "FitSegmentFile{2025-09-22 01:59:59.000, file_uuid=a, enabled=1, user_profile_primary_key=1, leader_type=[1,,0], leader_group_primary_key=[1,,4294967294], leader_activity_id=[1,,4294967294], leader_activity_id_string=a, default_race_leader=1, message_index=1}, " +
                        "FitWorkoutSession{2025-09-22 01:59:59.000, sport=1, sub_sport=1, num_valid_steps=1, first_step_index=1, pool_length=1.0, pool_length_unit=1, message_index=1}, " +
                        "FitWatchfaceSettings{2025-09-22 01:59:59.000, mode=1, layout=1, message_index=1}, " +
                        "FitGpsMetadata{2025-09-22 01:59:59.000, timestamp_ms=1, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, enhanced_altitude=1.0, enhanced_speed=1.0, heading=1.0, utc_timestamp=1127433599, velocity=[1.0,,4.2], timestamp=1758499199}, " +
                        "FitCameraEvent{2025-09-22 01:59:59.000, timestamp_ms=1, camera_event_type=1, camera_file_uuid=a, camera_orientation=1, timestamp=1758499199}, " +
                        "FitTimestampCorrelation{2025-09-22 01:59:59.000, fractional_timestamp=1.0, system_timestamp=1127433599, fractional_system_timestamp=1.0, local_timestamp=1, timestamp_ms=1, system_timestamp_ms=1, timestamp=1758499199}, " +
                        "RecordData{UNK_164_gyroscope_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/6)=[1,,65534], unknown_2(UINT16/6)=[1,,65534], unknown_3(UINT16/6)=[1,,65534], unknown_4(UINT16/6)=[1,,65534], unknown_5(FLOAT32/12)=[1.0,,4.2], unknown_6(FLOAT32/12)=[1.0,,4.2], unknown_7(FLOAT32/12)=[1.0,,4.2], 253_timestamp=1758499199}, " +
                        "RecordData{UNK_165_accelerometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/6)=[1,,65534], unknown_2(UINT16/6)=[1,,65534], unknown_3(UINT16/6)=[1,,65534], unknown_4(UINT16/6)=[1,,65534], unknown_5(FLOAT32/12)=[1.0,,4.2], unknown_6(FLOAT32/12)=[1.0,,4.2], unknown_7(FLOAT32/12)=[1.0,,4.2], unknown_8(SINT16/6)=[1,,32766], unknown_9(SINT16/6)=[1,,32766], unknown_10(SINT16/6)=[1,,32766], 253_timestamp=1758499199}, " +
                        "RecordData{UNK_167_three_d_sensor_calibration, 2025-09-22 01:59:59.000, unknown_0(ENUM/1)=1, unknown_1(UINT32/4)=1, unknown_2(UINT32/4)=1, unknown_3(UINT32/4)=1, unknown_4(SINT32/12)=[1,,2147483646], unknown_5(SINT32/12)=[65535,,275247], 253_timestamp=1758499199}, " +
                        "FitVideoFrame{2025-09-22 01:59:59.000, timestamp_ms=1, frame_number=1, timestamp=1758499199}, " +
                        "FitObdiiData{2025-09-22 01:59:59.000, timestamp_ms=1, time_offset=[1,,65534], pid=1, raw_data=[1,,254], pid_data_size=[1,,254], system_time=[1,,4294967294], start_timestamp=1127433599, start_timestamp_ms=1, timestamp=1758499199}, " +
                        "FitNmeaSentence{2025-09-22 01:59:59.000, timestamp_ms=1, sentence=a, timestamp=1758499199}, " +
                        "RecordData{UNK_178_aviation_attitude, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT32/12)=[1,,4294967294], unknown_2(SINT16/6)=[10430,,], unknown_3(SINT16/6)=[10430,,], unknown_4(SINT16/6)=[100,,420], unknown_5(SINT16/6)=[100,,420], unknown_6(SINT16/6)=[1024,,4301], unknown_7(ENUM/3)=[1,,0], unknown_8(UINT8/3)=[1,,254], unknown_9(UINT16/6)=[10430,,43808], unknown_10(UINT16/6)=[1,,65534], 253_timestamp=1758499199}, " +
                        "FitVideo{2025-09-22 01:59:59.000, url=a, hosting_provider=a, duration=1}, " +
                        "FitVideoTitle{2025-09-22 01:59:59.000, message_count=1, text=a, message_index=1}, " +
                        "FitVideoDescription{2025-09-22 01:59:59.000, message_count=1, text=a, message_index=1}, " +
                        "FitVideoClip{2025-09-22 01:59:59.000, clip_number=1, start_timestamp=1127433599, start_timestamp_ms=1, end_timestamp=1127433599, end_timestamp_ms=1, clip_start=1, clip_end=1}, " +
                        "FitOhrSettings{2025-09-22 01:59:59.000, enabled=1, timestamp=1758499199}, " +
                        "RecordData{UNK_200_exd_screen_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(UINT8/1)=1, unknown_2(ENUM/1)=1, unknown_3(ENUM/1)=1}, " +
                        "RecordData{UNK_201_exd_data_field_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(BASE_TYPE_BYTE/1)=1, unknown_2(UINT8/1)=1, unknown_3(UINT8/1)=1, unknown_4(ENUM/1)=1, unknown_5(STRING/6)=a}, " +
                        "RecordData{UNK_202_exd_data_concept_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(BASE_TYPE_BYTE/1)=1, unknown_2(UINT8/1)=1, unknown_3(UINT8/1)=1, unknown_4(UINT8/1)=1, unknown_5(UINT8/1)=1, unknown_6(UINT8/1)=1, unknown_8(ENUM/1)=1, unknown_9(ENUM/1)=1, unknown_10(ENUM/1)=1, unknown_11(ENUM/1)=1}, " +
                        "FitFieldDescription{2025-09-22 01:59:59.000, developer_data_index=1, field_definition_number=1, fit_base_type_id=1, field_name=a, array=1, components=a, scale=1, offset=1, units=a, bits=a, accumulate=a, fit_base_unit_id=1, native_mesg_num=1, native_field_num=1}, " +
                        "FitDeveloperData{2025-09-22 01:59:59.000, developer_id=[1,,254], application_id=[1,,254], manufacturer_id=1, developer_data_index=1, application_version=1}, " +
                        "RecordData{UNK_208_magnetometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/6)=[1,,65534], unknown_2(UINT16/6)=[1,,65534], unknown_3(UINT16/6)=[1,,65534], unknown_4(UINT16/6)=[1,,65534], unknown_5(FLOAT32/12)=[1.0,,4.2], unknown_6(FLOAT32/12)=[1.0,,4.2], unknown_7(FLOAT32/12)=[1.0,,4.2], 253_timestamp=1758499199}, " +
                        "FitBarometerData{2025-09-22 01:59:59.000, timestamp_ms=1, sample_time_offset=[1,,65534], baro_pres=[1,,4294967294], timestamp=1758499199}, " +
                        "RecordData{UNK_210_one_d_sensor_calibration, 2025-09-22 01:59:59.000, unknown_0(ENUM/1)=1, unknown_1(UINT32/4)=1, unknown_2(UINT32/4)=1, unknown_3(UINT32/4)=1, unknown_4(SINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitMonitoringHrData{2025-09-22 01:59:59.000, resting_heart_rate=1, current_day_resting_heart_rate=1, timestamp=1758499199}, " +
                        "FitTimeInZone{2025-09-22 01:59:59.000, reference_message=1, reference_index=1, time_in_zone=[1.0,,4.2], time_in_speed_zone=[1.0,,4.2], time_in_cadence_zone=[1.0,,4.2], time_in_power_zone=[1.0,,4.2], hr_zone_high_boundary=[1,,254], speed_zone_high_boundary=[1.0,,4.2], cadence_zone_high_boundary=[1,,254], power_zone_high_boundary=[1,,65534], hr_calc_type=1, max_heart_rate=1, resting_heart_rate=1, threshold_heart_rate=1, pwr_calc_type=1, functional_threshold_power=1, timestamp=1758499199}, " +
                        "FitSet{2025-09-22 01:59:59.000, duration=1.0, repetitions=1, weight=1.0, set_type=1, start_time=1758499199, category=[CATEGORY_CALF_RAISE,,CATEGORY_UNKNOWN], category_subtype=[1,,65534], weight_display_unit=1, message_index=1, wkt_step_index=1, timestamp=1758499199}, " +
                        "FitStressLevel{2025-09-22 01:59:59.000, stress_level_value=1, stress_level_time=1758499199}, " +
                        "FitMaxMetData{2025-09-22 01:59:59.000, update_time=1758499199, vo2_max=1.0, sport=1, sub_sport=1, max_met_category=1, calibrated_data=1, hr_source=1, speed_source=1}, " +
                        "FitDiveSettings{2025-09-22 01:59:59.000, name=a, gf_low=1, gf_high=1, water_type=1, water_density=1.0, po2_warn=1.0, po2_critical=1.0, po2_deco=1.0, safety_stop_enabled=1, bottom_depth=1.0, bottom_time=1, apnea_countdown_enabled=1, apnea_countdown_time=1, backlight_mode=1, backlight_brightness=1, backlight_timeout=1, repeat_dive_interval=1, safety_stop_time=1, heart_rate_source_type=1, heart_rate_source=1, travel_gas=1, ccr_low_setpoint_switch_mode=1, ccr_low_setpoint=1.0, ccr_low_setpoint_depth=1.0, ccr_high_setpoint_switch_mode=1, ccr_high_setpoint=1.0, ccr_high_setpoint_depth=1.0, gas_consumption_display=1, up_key_enabled=1, dive_sounds=1, last_stop_multiple=1.0, no_fly_time_mode=1, timestamp=1758499199, message_index=1}, " +
                        "FitDiveGas{2025-09-22 01:59:59.000, helium_content=1, oxygen_content=1, status=1, mode=1, message_index=1}, " +
                        "FitDiveAlarm{2025-09-22 01:59:59.000, depth=1.0, time=1, enabled=true, alarm_type=1, sound=1, dive_types=[1,,0], id=1, popup_enabled=true, trigger_on_descent=true, trigger_on_ascent=true, repeating=true, speed=1.0, message_index=1}, " +
                        "FitExerciseTitle{2025-09-22 01:59:59.000, exercise_category=1, exercise_name=1, wkt_step_name=a, message_index=1}, " +
                        "FitDiveSummary{2025-09-22 01:59:59.000, reference_mesg=1, reference_index=1, avg_depth=1.0, max_depth=1.0, surface_interval=1, start_cns=1, end_cns=1, start_n2=1, end_n2=1, o2_toxicity=1, dive_number=1, bottom_time=1.0, avg_pressure_sac=1.0, avg_volume_sac=1.0, avg_rmv=1.0, descent_time=1.0, ascent_time=1.0, avg_ascent_rate=1.0, avg_descent_rate=1.0, max_ascent_rate=1.0, max_descent_rate=1.0, hang_time=1.0, timestamp=1758499199}, " +
                        "FitSpo2{2025-09-22 01:59:59.000, reading_spo2=1, reading_confidence=1, mode=1, timestamp=1758499199}, " +
                        "FitSleepStage{2025-09-22 01:59:59.000, sleep_stage=AWAKE, timestamp=1758499199}, " +
                        "FitJump{2025-09-22 01:59:59.000, distance=1.0, heigh=1.0, rotations=1, hang_time=1.0, score=1.0, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, speed=1.0, enhanced_speed=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_289_aad_accel_features, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT32/4)=1, unknown_2(UINT16/2)=1, unknown_3(UINT8/1)=1, unknown_4(UINT16/2)=25, 253_timestamp=1758499199}, " +
                        "FitBeatIntervals{2025-09-22 01:59:59.000, timestamp_ms=1, time=[1,,65534], timestamp=1758499199}, " +
                        "FitRespirationRate{2025-09-22 01:59:59.000, respiration_rate=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_302_hsa_accelerometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/2)=1, unknown_2(SINT16/6)=[1,,4], unknown_3(SINT16/6)=[1,,4], unknown_4(SINT16/6)=[1,,4], unknown_5(UINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitHsaStepData{2025-09-22 01:59:59.000, processing_interval=1, steps=[1,,4294967294], timestamp=1758499199}, " +
                        "FitHsaSpo2Data{2025-09-22 01:59:59.000, processing_interval=1, reading_spo2=[1,,254], confidence=[1,,254], timestamp=1758499199}, " +
                        "FitHsaStressData{2025-09-22 01:59:59.000, processing_interval=1, stress_level=[1,,126], timestamp=1758499199}, " +
                        "FitHsaRespirationData{2025-09-22 01:59:59.000, processing_interval=1, respiration_rate=[1.0,,4.2], timestamp=1758499199}, " +
                        "FitHsaHeartRateData{2025-09-22 01:59:59.000, processing_interval=1, status=1, heart_rate=[1,,254], timestamp=1758499199}, " +
                        "FitSplit{2025-09-22 01:59:59.000, split_type=2, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, avg_speed=1.0, start_time=1127433599, total_ascent=1, total_descent=1, start_position_lat=8.381903171539307E-8, start_position_long=8.381903171539307E-8, end_position_lat=8.381903171539307E-8, end_position_long=8.381903171539307E-8, max_speed=1.0, avg_vert_speed=1.0, end_time=1127433599, total_calories=1, start_elevation=1.0, total_moving_time=1.0, message_index=1}, " +
                        "FitSplitSummary{2025-09-22 01:59:59.000, split_type=2, num_splits=1, total_timer_time=1.0, total_distance=1.0, avg_speed=1.0, max_speed=1.0, total_ascent=1, total_descent=1, avg_heart_rate=1, max_heart_rate=1, avg_vert_speed=1.0, total_calories=1, total_moving_time=1.0, message_index=1}, " +
                        "FitHsaBodyBatteryData{2025-09-22 01:59:59.000, processing_interval=1, level=[1,,126], charged=[1,,32766], uncharged=[1,,32766], timestamp=1758499199}, " +
                        "FitHsaEvent{2025-09-22 01:59:59.000, event_id=1, timestamp=1758499199}, " +
                        "FitClimbPro{2025-09-22 01:59:59.000, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, climb_pro_event=1, climb_number=1, climb_category=1, current_dist=1.0, timestamp=1758499199}, " +
                        "FitTankUpdate{2025-09-22 01:59:59.000, sensor=1, pressure=1.0, timestamp=1758499199}, " +
                        "FitTankSummary{2025-09-22 01:59:59.000, sensor=1, start_pressure=1.0, end_pressure=1.0, volume_used=1.0, timestamp=1758499199}, " +
                        "FitSleepStats{2025-09-22 01:59:59.000, combined_awake_score=1, awake_time_score=1, awakenings_count_score=1, deep_sleep_score=1, sleep_duration_score=1, light_sleep_score=1, overall_sleep_score=1, sleep_quality_score=1, sleep_recovery_score=1, rem_sleep_score=1, sleep_restlessness_score=1, awakenings_count=1, interruptions_score=1, average_stress_during_sleep=1.0}, " +
                        "FitHrvSummary{2025-09-22 01:59:59.000, weekly_average=1.0, last_night_average=1.0, last_night_5_min_high=1.0, baseline_low_upper=1.0, baseline_balanced_lower=1.0, baseline_balanced_upper=1.0, status=POOR, timestamp=1758499199}, " +
                        "FitHrvValue{2025-09-22 01:59:59.000, value=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_372_raw_bbi, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/6)=[1,,65534], unknown_2(UINT16/6)=[1,,65534], unknown_3(UINT8/3)=[1,,254], unknown_4(UINT8/3)=[1,,254], 253_timestamp=1758499199}, " +
                        "FitDeviceAuxBatteryInfo{2025-09-22 01:59:59.000, device_index=1, battery_voltage=1.0, battery_status=1, battery_identifier=1, timestamp=1758499199}, " +
                        "RecordData{UNK_376_hsa_gyroscope_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/2)=1, unknown_2(SINT16/6)=[29,,120], unknown_3(SINT16/6)=[29,,120], unknown_4(SINT16/6)=[29,,120], unknown_5(UINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitChronoShotSession{2025-09-22 01:59:59.000, min_speed=1.0, max_speed=1.0, avg_speed=1.0, shot_count=1, projectile_type=1, grain_weight=1.0, standard_deviation=1.0, timestamp=1758499199}, " +
                        "FitChronoShotData{2025-09-22 01:59:59.000, shot_speed=1.0, shot_num=1, timestamp=1758499199}, " +
                        "FitHsaConfigurationData{2025-09-22 01:59:59.000, data=[1,,254], data_size=1, timestamp=1758499199}, " +
                        "FitDiveApneaAlarm{2025-09-22 01:59:59.000, depth=1.0, time=1, enabled=true, alarm_type=1, sound=1, dive_types=[1,,0], id=1, popup_enabled=true, trigger_on_descent=true, trigger_on_ascent=true, repeating=true, speed=1.0, message_index=1}, " +
                        "FitSkinTempOvernight{2025-09-22 01:59:59.000, local_timestamp=1, average_deviation=1.0, average_7_day_deviation=1.0, nightly_value=1.0, timestamp=1758499199}, " +
                        "FitHsaWristTemperatureData{2025-09-22 01:59:59.000, processing_interval=1, value=[1.0,,4.2], timestamp=1758499199}, " +
                        "FitLap{2025-09-22 01:59:59.000, event=11, event_type=3, start_time=1127433599, time_in_hr_zone=1.0, time_in_speed_zone=[1.0,2.0], time_in_cadence_zone=[1.0,0.0,3.0], avg_total_hemoglobin_conc=1.0, min_total_hemoglobin_conc=[1.0,2.0], max_total_hemoglobin_conc=[1.0,0.0,3.0], lev_battery_consumption=20.0, timestamp=1758499199, message_index=2}]";

        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone testTimeZone = TimeZone.getTimeZone("Europe/Zurich");
        TimeZone.setDefault(testTimeZone);
        try {
            FitFile fitFile = FitFile.parseIncoming(fileContents);
            String expected = expectedOutput.replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
            String actual = fitFile.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");
            assertThat(actual, is(expected));
            getAllFitFieldValues(fitFile);
        } finally {
            TimeZone.setDefault(defaultTimeZone);
        }
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
                    if ("FitRecord".equals(recordName) && "getLatitude".equals(methodName)) {
                        // TODO GarminSupportTest.TestFitFileDevelopersField -> FitRecord / getLatitude
                        // FIXME java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Double
                        continue;
                    }
                    if ("FitMonitoring".equals(recordName) && "getActivityTime".equals(methodName)) {
                        // TODO GarminSupportTest.TestFitMessageTypeParsing -> sample fit file is likely broken
                        // FIXME sample FIT should use a plain value and not an array
                        continue;
                    }
                    throw new AssertionError(message, e);
                }
            }
        }
    }
}