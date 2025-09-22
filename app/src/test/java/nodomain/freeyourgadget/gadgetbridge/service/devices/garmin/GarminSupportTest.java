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
                "0E20B352F21B00002E464954" +
                        "AB6240000000000700010001" +
                        "028402028403048C04048605" +
                        "0284080D070004FF00FEFF01" +
                        "0000007F4533432A00476164" +
                        "676574427269646765004000" +
                        "0001000215048C17048C0001" +
                        "000000010000004000000200" +
                        "130001020104860C01002401" +
                        "002704862E01002F01003701" +
                        "003801003A02843B02845001" +
                        "005601005901005A04865E01" +
                        "025F0100860100AE01000001" +
                        "0100000001017F4533430101" +
                        "010101000100010101010000" +
                        "000101010140000003001CFE" +
                        "028400020701010002010203" +
                        "010204028405010006010007" +
                        "01000801020901020A01020B" +
                        "01020C01000D01000E010010" +
                        "010011010012010015010016" +
                        "02841C04861D04861E01001F" +
                        "02842002842F010031048600" +
                        "010061000101640A00010101" +
                        "01010101010101017F010101" +
                        "00010000000100000001E803" +
                        "E80301010000004000000400" +
                        "05FE028400010001028B0201" +
                        "0003010A0001000101000101" +
                        "400000050008FE0284000100" +
                        "01028B020284030486040100" +
                        "05010A070102000100010100" +
                        "0A0064000000010101400000" +
                        "06001E28010A2C0100FE0284" +
                        "000207010100020100030486" +
                        "04028B05028B06028B07028B" +
                        "0802840902840A02840B0284" +
                        "0C01000D01000E01020F0100" +
                        "100100110100120100130102" +
                        "14010015010A16010A17010A" +
                        "18010A25010226010A000101" +
                        "010061000101640000000100" +
                        "010001000100E803E8030A00" +
                        "0A0001010101010101FF0101" +
                        "010101010140000007000501" +
                        "010202010203028405010007" +
                        "010000010101000101400000" +
                        "080003FE0284010102020207" +
                        "000100016100400000090003" +
                        "FE0284010284020207000100" +
                        "010061004000000A0004FE02" +
                        "840101020202840301020001" +
                        "00010A000A4000000C000300" +
                        "010001010003020700010161" +
                        "004000000D00041F04862002" +
                        "842104869904860064000000" +
                        "E8030100000040420F004000" +
                        "000F000DFE02840001000101" +
                        "000204860304860401000504" +
                        "860601000704860801000902" +
                        "840A01000B01000001000101" +
                        "7F4533437F45334301010000" +
                        "000101000000010100010140" +
                        "0000120088BA0488BB0488C0" +
                        "0102C10102C20102C30102C5" +
                        "0102C60102C70102C80102D0" +
                        "0284D10284D202847D04867E" +
                        "04867F048680048681028482" +
                        "028483010284028485028486" +
                        "02848901028B02848C04868D" +
                        "04868E04868F010290010291" +
                        "028492028493010294010295" +
                        "01029601019B02849C0486A8" +
                        "0485A90284AA0284B40284B5" +
                        "0488B60488B7028465010266" +
                        "01026701026801026901026E" +
                        "02076F010270048671028472" +
                        "01017301017C04863B04863C" +
                        "02833D02833E02833F028340" +
                        "010245048646028447028452" +
                        "028453028454020757028458" +
                        "02845902845A02845B02845C" +
                        "01025D01025E01021A02841B" +
                        "01021C01001D04851E04851F" +
                        "048520048521028422028423" +
                        "028424028425028426048527" +
                        "04852904862A02842B01002C" +
                        "02842D02842E01002F028430" +
                        "048631028432028433010234" +
                        "028335028336028337028338" +
                        "02833901013A0101FE0284FD" +
                        "048600010001010002048603" +
                        "048504048505010006010007" +
                        "04860804860904860A04860B" +
                        "02840D02840E02840F028410" +
                        "010211010212010213010214" +
                        "028415028416028417028418" +
                        "0102190284000000803F0000" +
                        "803F01010101010164646400" +
                        "64006400E8030000C9090000" +
                        "C9090000C909000001000100" +
                        "02640064000A000AE803E803" +
                        "0000E8030000010000000101" +
                        "010001000101010101000100" +
                        "000000000100640064006400" +
                        "0000803F0000803F01000202" +
                        "020202610001E80300000100" +
                        "0101E8030000E8030000E803" +
                        "E803E803E80301E803000001" +
                        "00C909010001006100640064" +
                        "000A0064000A008080800100" +
                        "010101000000010000000100" +
                        "000001000000010001000A00" +
                        "E80301000100000001000000" +
                        "0A0000006400016400010001" +
                        "010001000000C909C9090164" +
                        "006400640064006400010101" +
                        "007F45334303017F45334301" +
                        "000000010000000101E80300" +
                        "00E803000064000000010000" +
                        "0001000100E803E803010101" +
                        "0101000100010001000A0100" +
                        "400000130067950488960488" +
                        "9702849904889A04889C0102" +
                        "9D01029E02849F0284A00284" +
                        "6501016E04866F0486700486" +
                        "710486720486730284740284" +
                        "750102760284770284780284" +
                        "7902847A04867B04867C0101" +
                        "880284890284930102940102" +
                        "3D02843E02843F0102470284" +
                        "4A02844D02844E02844F0284" +
                        "500102510102520102530284" +
                        "5B01025C01025D01025E0102" +
                        "5F0102620486630284640101" +
                        "1901001A0102200284210284" +
                        "220284230284250284260100" +
                        "2701002802842904862A0284" +
                        "2B02842C01022D02832E0283" +
                        "2F0283300283310283320101" +
                        "330101340486350283360283" +
                        "370283380283FE0284FD0486" +
                        "000100010100020486030485" +
                        "040485050485060485070486" +
                        "0804860904860A04860B0284" +
                        "0C02840D02840E02840F0102" +
                        "100102110102120102130284" +
                        "140284150284160284170100" +
                        "180100000000803F0000803F" +
                        "01000000803F0000803F6464" +
                        "64006400640001E8030000E8" +
                        "030000C9090000C9090000C9" +
                        "090000010001000264006400" +
                        "0A00E803E8030000E8030000" +
                        "016400640001010100C90901" +
                        "010001000A0064000A008080" +
                        "8001000202020202E8030000" +
                        "010001010101000100010001" +
                        "0064000101010001000000C9" +
                        "09C909016400640064006400" +
                        "64000101E8030000E803E803" +
                        "E803E80301007F4533430301" +
                        "7F4533430100000001000000" +
                        "0100000001000000E8030000" +
                        "E80300006400000001000000" +
                        "01000100E803E80301010101" +
                        "010001000100010001014000" +
                        "0014004E5B04865C04865D04" +
                        "865E04865F04866004866101" +
                        "026202846301026C02847204" +
                        "887304887402847502847601" +
                        "027701027801027B04867C02" +
                        "847D02847E02847F04858101" +
                        "028B02842D01022E01022F01" +
                        "023001023101003201023302" +
                        "843402843501023602843702" +
                        "843802843902843A02843B02" +
                        "843E01024301014401014904" +
                        "864E04865101025202845302" +
                        "84540284550284570284FD04" +
                        "860004850104850202840301" +
                        "020401020504860602840702" +
                        "840902830A01020B04850C01" +
                        "020D01011201021304861C02" +
                        "841D04861E01021F01022002" +
                        "832102842702842802842902" +
                        "842A01002B01022C01020001" +
                        "000000E8030000E803000001" +
                        "000000010000000100000001" +
                        "01000164000000803F000080" +
                        "3F6400010001010101000000" +
                        "640064006400E80300006464" +
                        "000202028001016400000180" +
                        "6400640064000A000A000A00" +
                        "010101E8030000C909000002" +
                        "0100640064000A0064007F45" +
                        "33430100000001000000C909" +
                        "010164000000E80301006400" +
                        "01E803000064010101000000" +
                        "0100010000000101E8030100" +
                        "0A0064000A00010202400000" +
                        "15001309010A0A010A0B010A" +
                        "0C010A0D01020E01000F0486" +
                        "150100160102170102180102" +
                        "FD0486000100010100020284" +
                        "030486040102070284080284" +
                        "000101010101017F45334301" +
                        "010A0A7F4533430301010001" +
                        "000000010100010040000017" +
                        "0013FD048600010201010202" +
                        "028403048C04028405028406" +
                        "01020704860A02840B010212" +
                        "010013020714010A15028B16" +
                        "01001901001B020720010200" +
                        "7F4533430101010001000000" +
                        "010064000101000000000101" +
                        "016100010100010161000140" +
                        "00001A0009FE028404010005" +
                        "048C0602840802070B01000E" +
                        "02840F010011020700010001" +
                        "010000000100610001640001" +
                        "61004000001B001306048607" +
                        "01000802070901000A02840B" +
                        "02840C02840D028413010014" +
                        "0486150486160486FE028400" +
                        "020701010002048603010004" +
                        "048605048600010000000161" +
                        "000101000100640001000101" +
                        "000000010000000100000001" +
                        "006100016400000001010000" +
                        "00010000004000001C000700" +
                        "028401028402048C03048604" +
                        "010005010006048600010001" +
                        "00010000007F453343010101" +
                        "0000004000001E000EFD0486" +
                        "000284010284020284030284" +
                        "040284050284070284080102" +
                        "0902840A01020B01020C0284" +
                        "0D0284007F45334364006400" +
                        "640064006400640004000104" +
                        "00010101000A004000001F00" +
                        "0404010005020706048C0701" +
                        "000001610001000000014000" +
                        "00200008FE02840104860204" +
                        "850304850404860501000602" +
                        "070801000001007F45334301" +
                        "000000010000006400000001" +
                        "61000140000021000AFE0284" +
                        "FD0486000486010486020486" +
                        "030100040486050284060486" +
                        "0901020001007F4533430100" +
                        "000001000000010000000101" +
                        "000000010001000000014000" +
                        "00220008FD04860004860102" +
                        "840201000301000401000504" +
                        "86060102007F453343E80300" +
                        "000100010301010000000140" +
                        "0000230003FE028403028405" +
                        "020700010064006100400000" +
                        "250006FE028400010001010A" +
                        "020207030284040486000100" +
                        "020161000100010000004000" +
                        "00260005FE02840001000102" +
                        "840201000302840001000201" +
                        "00010100400000270005FE02" +
                        "840001000102840201020302" +
                        "840001000201000101004000" +
                        "003100020002840101020001" +
                        "000140000033000BFD048600" +
                        "028401028402028403028404" +
                        "028405028406010207010008" +
                        "0100090284007F4533430100" +
                        "010001000100010001000101" +
                        "010100400000350003FE0284" +
                        "000284010207000100E80361" +
                        "0040000037001C220284FD04" +
                        "860001020102840204860304" +
                        "860404860501000601000701" +
                        "000802840902840A02840B04" +
                        "860C02830E02830F02831302" +
                        "8418010D1901021A02841B01" +
                        "021C01021D02841E04861F04" +
                        "862004862102840001007F45" +
                        "334301010064000000020000" +
                        "00E803000001010101000100" +
                        "010001000000640064006400" +
                        "010001010100010A01000100" +
                        "0000E8030000E80300000100" +
                        "400000480006FD0486000100" +
                        "01028402028403048C040486" +
                        "007F45334302010001000100" +
                        "00007F4533434000004E0000" +
                        "00400000500004FD04860002" +
                        "8401010D030102007F453343" +
                        "00800101400000510004FD04" +
                        "8600028401010D030102007F" +
                        "453343008001014000005200" +
                        "0500010201010A02028B0301" +
                        "0A0401020001010100010140" +
                        "0000650014FE0284FD048600" +
                        "010001010002048603048604" +
                        "048605028406028407010009" +
                        "01020A01020B02840C010012" +
                        "028413028416028417028418" +
                        "01021901020001007F453343" +
                        "03017F453343E8030000E803" +
                        "00000100E803010101010001" +
                        "010001006400640001014000" +
                        "00670003FD04860004860502" +
                        "84007F453343010000000100" +
                        "400000690000004000006A00" +
                        "020002840102840001000100" +
                        "4000007F000D000100010100" +
                        "020100030207040100050100" +
                        "060100070100080100090100" +
                        "0A01000B01000C0100000101" +
                        "016100010101010101010101" +
                        "400000800010FD0486000100" +
                        "010101020100030284040284" +
                        "050102060101070102080207" +
                        "0904860A04850B04850C0100" +
                        "0D01010E0101007F45334301" +
                        "01010100E80301010161007F" +
                        "453343010000000100000001" +
                        "0101400000810006FD048600" +
                        "020701048602048603010004" +
                        "0100007F45334361007F4533" +
                        "437F45334301014000008300" +
                        "03FE02840001020102070001" +
                        "00016100400000840003FD04" +
                        "86000284010102007F453343" +
                        "0080FF4000008E0053540488" +
                        "550488560488570488590102" +
                        "5A01025B04865C04865D0486" +
                        "3902843A01003B01023C0102" +
                        "3D01023E01023F0102400100" +
                        "410207420102430102440102" +
                        "450284460284470486480284" +
                        "4901014A01015302841C0485" +
                        "1D02071E02841F0284200100" +
                        "210486220284230284240102" +
                        "250283260283270283280283" +
                        "2902832A01012B01012C0486" +
                        "2D02832E02832F0283300283" +
                        "350284360284370102380486" +
                        "FE0284FD0486000100010100" +
                        "020486030485040485050485" +
                        "060485070486080486090486" +
                        "0A04860B02840C02840D0284" +
                        "0E02840F0102100102110102" +
                        "120102130284140284150284" +
                        "160284170100180102190485" +
                        "1A04851B0485000000803F00" +
                        "00803F0000803F0000803F64" +
                        "64C9090000C9090000C90900" +
                        "000100010202020202016100" +
                        "80808001000100E803000001" +
                        "000101010001000000610001" +
                        "0001000101000000C909C909" +
                        "016400640064006400640001" +
                        "01E8030000E803E803E803E8" +
                        "030100C90901E80300000100" +
                        "7F45334303017F4533430100" +
                        "000001000000010000000100" +
                        "0000E8030000E80300006400" +
                        "00000100000001000100E803" +
                        "E80301010101010001000100" +
                        "010001010100000001000000" +
                        "01000000400000910004FA04" +
                        "860102840202840301020001" +
                        "000000010001000140000094" +
                        "000900020701020702010003" +
                        "010004048605048606010207" +
                        "010008010000610061000101" +
                        "010000000100000001010140" +
                        "0000950007FE028400020701" +
                        "010002048603048604048605" +
                        "020700010061000101000000" +
                        "01000000E803000061004000" +
                        "00960006FE02840104850204" +
                        "850304860402840604860001" +
                        "000100000001000000640000" +
                        "00C909C90900004000009700" +
                        "05FE02840102070301000404" +
                        "860B01020001006100010100" +
                        "0000014000009E0007FE0284" +
                        "000100010100020284030284" +
                        "040284050100000100010101" +
                        "0001006400014000009F0003" +
                        "FE028400010001010D000100" +
                        "0101400000A00008FD048600" +
                        "028401048502048503048604" +
                        "0486050284060486007F4533" +
                        "4301000100000001000000C9" +
                        "090000E803000064007F4533" +
                        "43400000A10005FD04860002" +
                        "84010100020207030100007F" +
                        "453343010001610001400000" +
                        "A20007FD0486000284010486" +
                        "020284030486040284050284" +
                        "007F45334300807F45334300" +
                        "800100000001000100400000" +
                        "A40002FD0486000284007F45" +
                        "33430100400000A50002FD04" +
                        "86000284007F453343010040" +
                        "0000A70005FD048600010001" +
                        "0486020486030486007F4533" +
                        "430101000000010000000100" +
                        "0000400000A90003FD048600" +
                        "0284010486007F4533430100" +
                        "01000000400000AE0005FD04" +
                        "8600028402010D0604860702" +
                        "84007F4533430100017F4533" +
                        "430100400000B10003FD0486" +
                        "000284010207007F45334301" +
                        "006100400000B20002FD0486" +
                        "000284007F45334301004000" +
                        "00B800030002070102070204" +
                        "860061006100010000004000" +
                        "00B90003FE02840002840102" +
                        "0700010001006100400000BA" +
                        "0003FE028400028401020700" +
                        "010001006100400000BB0007" +
                        "000284010486020284030486" +
                        "040284060486070486000100" +
                        "7F45334301007F4533430100" +
                        "0100000001000000400000BC" +
                        "0002FD0486000100007F4533" +
                        "4301400000C8000400010201" +
                        "010202010003010000010101" +
                        "01400000C900050001020101" +
                        "0D0201020301020401000001" +
                        "01010101400000CA000B0001" +
                        "0201010D0201020301020401" +
                        "020501020601020801000901" +
                        "000A01000B01000001010101" +
                        "01010101010101400000CE00" +
                        "0C0001020101020201020401" +
                        "020502070601020701010902" +
                        "070A02070D02840E02840F01" +
                        "020001010101610001016100" +
                        "61000100010001400000CF00" +
                        "030202840301020404860001" +
                        "000101000000400000D00002" +
                        "FD0486000284007F45334301" +
                        "00400000D10002FD04860002" +
                        "84007F4533430100400000D2" +
                        "0006FD048600010001048602" +
                        "0486030486040485007F4533" +
                        "430101000000010000000100" +
                        "000001000000400000D30003" +
                        "FD0486000102010102007F45" +
                        "33430101400000D80009FD04" +
                        "860002840102840A01000B01" +
                        "020C01020D01020E01000F02" +
                        "84007F453343010001000101" +
                        "0101010100400000E10009FE" +
                        "048600048603028404028405" +
                        "01020604860902840A02840B" +
                        "0284007F453343E803000001" +
                        "001000017F45334301000100" +
                        "0100400000E3000200028301" +
                        "04860001007F453343400000" +
                        "E50008000486020284050100" +
                        "0601000801000901000C0100" +
                        "0D0100007F4533430A000101" +
                        "010101014000000201231A01" +
                        "021B04861D01001E01002301" +
                        "00240102250100FD0486FE02" +
                        "840002070101000201020301" +
                        "020401000504880601020701" +
                        "020801020901000A04880B04" +
                        "860C01000D04860E01000F01" +
                        "021001021102841202841301" +
                        "001401021502841601001701" +
                        "021804861901000064E80300" +
                        "000101010A017F4533430100" +
                        "6100FF0101010000803F6464" +
                        "64010000803F010000000101" +
                        "000000010101010001000101" +
                        "01000164E803000001400000" +
                        "030105FE0284000102010102" +
                        "020100030100000100010101" +
                        "0140000006010CFE02840004" +
                        "860104850201000301000401" +
                        "000604860701000801000901" +
                        "000A01000B0485000100E803" +
                        "000001000000010101010000" +
                        "0001010101E8030000400000" +
                        "080103FE0284000284010284" +
                        "000100010001004000000C01" +
                        "17FD04860002840102840204" +
                        "860304860404860501020601" +
                        "020702840802840902840A04" +
                        "860B04860C02840D02840E02" +
                        "840F04861004861104851604" +
                        "86170486180486190486007F" +
                        "45334301000100E8030000E8" +
                        "030000010000000101010001" +
                        "00010001000000E803000064" +
                        "0064006400E8030000E80300" +
                        "00E8030000E8030000E80300" +
                        "00E8030000E8030000400000" +
                        "0D0104FD0486000102010102" +
                        "020100007F45334301010140" +
                        "0000130102FD048600010000" +
                        "7F453343014000001D010AFD" +
                        "048600048801048802010203" +
                        "048804048805048506048507" +
                        "0284080486007F4533430000" +
                        "803F0000803F010000803F00" +
                        "00803F0100000001000000E8" +
                        "03E8030000400000210106FD" +
                        "048600028401048602028403" +
                        "0102040284007F4533430100" +
                        "010000000100011900400000" +
                        "220102FD0486000284007F45" +
                        "33430100400000290102FD04" +
                        "86000283007F453343640040" +
                        "00002E0104FD048600028401" +
                        "0284050486007F4533430100" +
                        "010001000000400000300102" +
                        "FD0486000284007F45334301" +
                        "00400000310102FD04860002" +
                        "84007F453343010040000032" +
                        "0102FD0486000284007F4533" +
                        "430100400000330102FD0486" +
                        "000284007F45334301004000" +
                        "00340103FD04860002840101" +
                        "02007F453343010001400000" +
                        "380113FE0284000100010486" +
                        "020486030486040486090486" +
                        "0D02840E0284150485160485" +
                        "1704851804851904861A0485" +
                        "1B04861C04864A04866E0486" +
                        "00010002E8030000E8030000" +
                        "64000000E80300007F453343" +
                        "010001000100000001000000" +
                        "0100000001000000E8030000" +
                        "E80300007F45334301000000" +
                        "C9090000E803000040000039" +
                        "010EFE028400010003028404" +
                        "048605048606048607048608" +
                        "02840902840A01020B01020C" +
                        "04850D04864D048600010002" +
                        "0100E803000064000000E803" +
                        "0000E8030000010001000101" +
                        "E803000001000000E8030000" +
                        "4000003A0102FD0486000284" +
                        "007F45334301004000003B01" +
                        "02FD0486000102007F453343" +
                        "014000003D0107FD04860004" +
                        "850104850201000302840401" +
                        "02050488007F453343010000" +
                        "000100000001010001000080" +
                        "3F4000003F0103FD04860004" +
                        "8C010284007F453343010000" +
                        "006400400000430105FD0486" +
                        "00048C010284020284030486" +
                        "007F45334301000000640064" +
                        "00640000004000005A010E00" +
                        "010201010202010203010204" +
                        "010205010206010207010208" +
                        "01020901020A01020B01020E" +
                        "01020F028400010101010101" +
                        "010101010101016400400000" +
                        "720108FD0486000284010284" +
                        "020284030284040284050284" +
                        "060100007F45334380008000" +
                        "800080008000800001400000" +
                        "730102FD0486000284007F45" +
                        "33438000400000740102FD04" +
                        "86000284007F453343010040" +
                        "0000770105FD048600010201" +
                        "0284020102030102007F4533" +
                        "430100010101400000780104" +
                        "FD0486000284010284050486" +
                        "007F45334301000100010000" +
                        "00400000830108FD04860004" +
                        "860104860204860302840401" +
                        "00050486060486007F453343" +
                        "E8030000E8030000E8030000" +
                        "0100010A000000E803000040" +
                        "0000840103FD048600048601" +
                        "0284007F453343E803000001" +
                        "00400000850102FD04860101" +
                        "02007F453343014000008901" +
                        "0CFE02840004860104850201" +
                        "000301000401000604860701" +
                        "000801000901000A01000B04" +
                        "85000100E803000001000000" +
                        "0101010100000001010101E8" +
                        "0300004000008E0105FD0486" +
                        "000486010488020488040488" +
                        "007F45334301000000000080" +
                        "3F0000803F0000803F400000" +
                        "990102FD0486000284007F45" +
                        "33430100E881"
        );
        String expectedOutput =
                "[FitFileId{type=ACTIVITY, manufacturer=255, product=65534, serial_number=1, time_created=1758499199, number=42, product_name=GadgetBridge}, " +
                        "FitCapabilities{workouts_supported=1, connectivity_supported=1}, " +
                        "FitDeviceSettings{active_time_zone=1, utc_offset=1, backlight_mode=1, activity_tracker_enabled=1, clock_time=1127433599, move_alert_enabled=1, date_mode=1, display_orientation=1, mounting_side=1, autosync_min_steps=1, autosync_min_time=1, lactate_threshold_autodetect_enabled=1, ble_auto_upload_enabled=1, auto_sync_frequency=1, auto_activity_detect=1, number_of_screens=1, smart_notification_display_orientation=1, tap_interface=1, tap_sensitivity=1}, " +
                        "FitUserProfile{friendly_name=a, gender=1, age=1, height=100, weight=1.0, language=french, elev_setting=imperial, weight_setting=imperial, resting_heart_rate=1, default_max_running_heart_rate=1, default_max_biking_heart_rate=1, default_max_heart_rate=1, hr_setting=1, speed_setting=imperial, dist_setting=imperial, power_setting=1, activity_class=127, position_setting=1, temperature_setting=imperial, local_id=1, wake_time=1, sleep_time=1, height_setting=imperial, user_running_step_length=1000, user_walking_step_length=1000, depth_setting=imperial, dive_count=1, message_index=1}, " +
                        "FitHrmProfile{enabled=true, hrm_ant_id=1, log_hrv=1, hrm_ant_id_trans_type=1, message_index=1}, " +
                        "FitSdmProfile{enabled=true, sdm_ant_id=1, sdm_cal_factor=1.0, odometer=1.0, speed_source=1, sdm_ant_id_trans_type=1, odometer_rollover=1, message_index=1}, " +
                        "FitBikeProfile{name=a, sport=1, sub_sport=1, odometer=1.0, bike_spd_ant_id=1, bike_cad_ant_id=1, bike_spdcad_ant_id=1, bike_power_ant_id=1, custom_wheelsize=1.0, auto_wheelsize=1.0, bike_weight=1.0, power_cal_factor=1.0, auto_wheel_cal=true, auto_power_zero=true, id=1, spd_enabled=true, cad_enabled=true, spdcad_enabled=true, power_enabled=true, enabled=true, bike_spd_ant_id_trans_type=1, bike_cad_ant_id_trans_type=1, bike_spdcad_ant_id_trans_type=1, bike_power_ant_id_trans_type=1, odometer_rollover=1, front_gear_num=1, rear_gear_num=1, shimano_di2_enabled=true, message_index=1}, " +
                        "FitZonesTarget{max_heart_rate=1, threshold_heart_rate=1, functional_threshold_power=1, hr_calc_type=1, pwr_calc_type=1}, " +
                        "FitHrZone{high_bpm=1, name=a, message_index=1}, " +
                        "FitPowerZone{high_value=1, name=a, message_index=1}, " +
                        "FitMetZone{high_bpm=1, calories=1.0, fat_calories=1.0, message_index=1}, " +
                        "FitSport{sport=1, sub_sport=1, name=a}, " +
                        "FitTrainingSettings{target_distance=1.0, target_speed=1.0, target_time=1, precise_target_speed=1.0}, " +
                        "FitGoals{sport=1, sub_sport=1, start_date=1127433599, end_date=1127433599, type=distance, value=1, repeat=1, target_value=1, recurrence=1, recurrence_value=1, enabled=1, source=community, message_index=1}, " +
                        "FitSession{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_latitude=8.381903171539307E-8, start_longitude=8.381903171539307E-8, sport=1, sub_sport=1, total_elapsed_time=1000, total_timer_time=1000, total_distance=100, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, average_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, total_training_effect=1.0, first_lap_index=1, num_laps=1, event_group=1, trigger=1, nec_latitude=8.381903171539307E-8, nec_longitude=8.381903171539307E-8, swc_latitude=8.381903171539307E-8, swc_longitude=8.381903171539307E-8, num_lengths=1, normalized_power=1, training_stress_score=1.0, intensity_factor=1.0, left_right_balance=1, end_latitude=8.381903171539307E-8, end_longitude=8.381903171539307E-8, avg_stroke_count=10, avg_stroke_distance=1.0, swim_stroke=1, pool_length=1.0, threshold_power=1, pool_length_unit=1, num_active_lengths=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, min_heart_rate=1, avg_lap_time=1.0, best_lap_index=1, min_altitude=1.0, player_score=1, opponent_score=1, opponent_name=a, max_ball_speed=1.0, avg_ball_speed=1.0, avg_vertical_oscillation=1.0, avg_stance_time_percent=1.0, avg_stance_time=1.0, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, sport_profile_name=a, sport_index=1, stand_time=1000, stand_count=1, avg_left_pco=1, avg_right_pco=1, enhanced_avg_speed=1.0, enhanced_max_speed=1.0, enhanced_avg_altitude=1.0, enhanced_min_altitude=1.0, enhanced_max_altitude=1.0, avg_lev_motor_power=1, max_lev_motor_power=1, lev_battery_consumption=1.0, avg_vertical_ratio=1.0, avg_stance_time_balance=1.0, avg_step_length=1.0, total_anaerobic_training_effect=1.0, avg_vam=1.0, avg_depth=1.0, max_depth=1.0, surface_interval=1, start_cns=1, end_cns=1, start_n2=1, end_n2=1, avg_respiration_rate=1, max_respiration_rate=1, min_respiration_rate=1, min_temperature=1, o2_toxicity=1, dive_number=1, training_load_peak=1.0, enhanced_avg_respiration_rate=1.0, enhanced_max_respiration_rate=1.0, enhanced_min_respiration_rate=1.0, total_grit=1.0, total_flow=1.0, jump_count=1, avg_grit=1.0, avg_flow=1.0, workout_feel=1, workout_rpe=1, avg_spo2=1, avg_stress=1, hrv_sdrr=1, hrv_rmssd=1, total_fractional_ascent=1.0, total_fractional_descent=1.0, avg_core_temperature=1.0, min_core_temperature=1.0, max_core_temperature=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitLap{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_lat=8.381903171539307E-8, start_long=8.381903171539307E-8, end_lat=8.381903171539307E-8, end_long=8.381903171539307E-8, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, avg_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, intensity=1, lap_trigger=1, sport=1, event_group=1, num_lengths=1, normalized_power=1, left_right_balance=1, first_length_index=1, avg_stroke_distance=100, swim_style=BACKSTROKE, sub_sport=1, num_active_lengths=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, repetition_num=1, min_altitude=1.0, min_heart_rate=1, wkt_step_index=1, opponent_score=1, avg_vertical_oscillation=1.0, avg_stance_time_percent=1.0, avg_stance_time=1.0, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, player_score=1, avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, time_standing=1.0, stand_count=1, avg_left_pco=1, avg_right_pco=1, enhanced_avg_speed=10.0, enhanced_max_speed=10.0, enhanced_avg_altitude=1.0, enhanced_min_altitude=1.0, enhanced_max_altitude=1.0, avg_lev_motor_power=1, max_lev_motor_power=1, lev_battery_consumption=1.0, avg_vertical_ratio=1.0, avg_stance_time_balance=1.0, avg_step_length=1.0, avg_vam=1.0, avg_depth=1.0, max_depth=1.0, min_temperature=1, enhanced_avg_respiration_rate=1.0, enhanced_max_respiration_rate=1.0, avg_respiration_rate=1, max_respiration_rate=1, total_grit=1.0, total_flow=1.0, jump_count=1, avg_grit=1.0, avg_flow=1.0, total_fractional_ascent=1.0, total_fractional_descent=1.0, avg_core_temperature=1.0, min_core_temperature=1.0, max_core_temperature=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitRecord{2025-09-22 01:59:59.000, latitude=8.381903171539307E-8, longitude=8.381903171539307E-8, altitude=1.0, heart_rate=1, cadence=1, distance=1.0, speed=1.0, power=1, grade=1.0, resistance=1, time_from_course=1.0, cycle_length=1.0, temperature=1, cycles=1, total_cycles=1, compressed_accumulated_power=1, accumulated_power=1, left_right_balance=1, gps_accuracy=1, vertical_speed=1.0, calories=1, oscillation=1.0, stance_time_percent=1.0, stance_time=1.0, activity=1, left_torque_effectiveness=1.0, right_torque_effectiveness=1.0, left_pedal_smoothness=1.0, right_pedal_smoothness=1.0, combined_pedal_smoothness=1.0, time128=1.0, stroke_type=1, zone=1, ball_speed=1.0, cadence256=1.0, fractional_cadence=1.0, avg_total_hemoglobin_conc=1.0, min_total_hemoglobin_conc=1.0, max_total_hemoglobin_conc=1.0, avg_saturated_hemoglobin_percent=1.0, min_saturated_hemoglobin_percent=1.0, max_saturated_hemoglobin_percent=1.0, device_index=1, left_pco=1, right_pco=1, enhanced_speed=1.0, enhanced_altitude=1.0, battery_soc=1.0, motor_power=1, vertical_ratio=1.0, stance_time_balance=1.0, step_length=1.0, cycle_length16=1.0, absolute_pressure=1, depth=1.0, next_stop_depth=1.0, next_stop_time=1, time_to_surface=1, ndl_time=1, cns_load=1, n2_load=1, respiration_rate=1, enhanced_respiration_rate=100, grit=1.0, flow=1.0, current_stress=1.0, ebike_travel_rang=1, ebike_battery_level=1, ebike_assist_mode=1, ebike_assist_level_percent=1, air_time_remaining=1, pressure_sac=1.0, volume_sac=1.0, rmv=1.0, ascent_rate=1.0, po2=1.0, core_temperature=1.0, timestamp=1758499199}, " +
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
                        "FitMonitoring{2025-09-22 01:59:59.000, device_index=1, calories=1, distance=100, cycles=2, active_time=1000, activity_type=1, activity_subtype=1, activity_level=1, distance_16=1, cycles_16=1, active_time_16=1, local_timestamp=1, temperature=1.0, temperature_min=1.0, temperature_max=1.0, active_calories=1, current_activity_type_intensity=1, timestamp_min_8=1, timestamp_16=1, heart_rate=1, intensity=1.0, duration_min=1, duration=1, ascent=1.0, descent=1.0, moderate_activity_minutes=1, vigorous_activity_minutes=1, timestamp=1758499199}, " +
                        "FitTrainingFile{2025-09-22 01:59:59.000, type=2, manufacturer=1, product=1, serial_number=1, time_created=1127433599, timestamp=1758499199}, " +
                        "FitHrv{2025-09-22 01:59:59.000}, " +
                        "FitAntRx{2025-09-22 01:59:59.000, fractional_timestamp=1.0, mesg_id=1, channel_number=1, timestamp=1758499199}, " +
                        "FitAntTx{2025-09-22 01:59:59.000, fractional_timestamp=1.0, mesg_id=1, channel_number=1, timestamp=1758499199}, " +
                        "FitAntChannelId{2025-09-22 01:59:59.000, channel_number=1, device_type=1, device_number=1, transmission_type=1, device_index=1}, " +
                        "FitLength{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, total_elapsed_time=1.0, total_timer_time=1.0, total_strokes=1, avg_speed=1.0, swim_stroke=1, avg_swimming_cadence=1, event_group=1, total_calories=1, length_type=1, player_score=1, opponent_score=1, enhanced_avg_respiration_rate=0.1, enhanced_max_respiration_rate=0.1, avg_respiration_rate=1, max_respiration_rate=1, timestamp=1758499199, message_index=1}, " +
                        "FitMonitoringInfo{2025-09-22 01:59:59.000, local_timestamp=1, resting_metabolic_rate=1, timestamp=1758499199}, " +
                        "FitPad{2025-09-22 01:59:59.000}, " +
                        "FitSlaveDevice{2025-09-22 01:59:59.000, manufacturer=1, product=1}, " +
                        "FitConnectivity{2025-09-22 01:59:59.000, bluetooth_enabled=1, bluetooth_le_enabled=1, ant_enabled=1, name=a, live_tracking_enabled=1, weather_conditions_enabled=1, weather_alerts_enabled=1, auto_activity_upload_enabled=1, course_download_enabled=1, workout_download_enabled=1, gps_ephemeris_download_enabled=1, incident_detection_enabled=1, grouptrack_enabled=1}, " +
                        "FitWeather{2025-09-22 01:59:59.000, weather_report=1, temperature=274, condition=PARTLY_CLOUDY, wind_direction=1, wind_speed=3.3557048, precipitation_probability=1, temperature_feels_like=274, relative_humidity=1, location=a, observed_at_time=1758499199, observed_location_lat=1, observed_location_long=1, day_of_week=MONDAY, high_temperature=274, low_temperature=274, timestamp=1758499199}, " +
                        "FitWeatherAlert{2025-09-22 01:59:59.000, report_id=a, issue_time=1127433599, expire_time=1127433599, severity=1, type=1, timestamp=1758499199}, " +
                        "FitCadenceZone{2025-09-22 01:59:59.000, high_value=1, name=a, message_index=1}, " +
                        "FitHr{2025-09-22 01:59:59.000, fractional_timestamp=1.0, timestamp=1758499199}, " +
                        "FitSegmentLap{2025-09-22 01:59:59.000, event=3, event_type=1, start_time=1127433599, start_position_lat=8.381903171539307E-8, start_position_long=8.381903171539307E-8, end_position_lat=8.381903171539307E-8, end_position_long=8.381903171539307E-8, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, total_cycles=1, total_calories=1, total_fat_calories=1, avg_speed=1.0, max_speed=1.0, avg_heart_rate=1, max_heart_rate=1, avg_cadence=1, max_cadence=1, avg_power=1, max_power=1, total_ascent=1, total_descent=1, sport=1, event_group=1, nec_lat=8.381903171539307E-8, nec_long=8.381903171539307E-8, swc_lat=8.381903171539307E-8, swc_long=8.381903171539307E-8, name=a, normalized_power=1, left_right_balance=1, sub_sport=1, total_work=1, avg_altitude=1.0, max_altitude=1.0, gps_accuracy=1, avg_grade=1.0, avg_pos_grade=1.0, avg_neg_grade=1.0, max_pos_grade=1.0, max_neg_grade=1.0, avg_temperature=1, max_temperature=1, total_moving_time=1.0, avg_pos_vertical_speed=1.0, avg_neg_vertical_speed=1.0, max_pos_vertical_speed=1.0, max_neg_vertical_speed=1.0, repetition_num=1, min_altitude=1.0, min_heart_rate=1, active_time=1.0, wkt_step_index=1, sport_event=1, avg_left_torque_effectiveness=1.0, avg_right_torque_effectiveness=1.0, avg_left_pedal_smoothness=1.0, avg_right_pedal_smoothness=1.0, avg_combined_pedal_smoothness=1.0, status=1, uuid=a, avg_fractional_cadence=1.0, max_fractional_cadence=1.0, total_fractional_cycles=1.0, front_gear_shift_count=1, rear_gear_shift_count=1, time_standing=1.0, stand_count=1, avg_left_pco=1, avg_right_pco=1, manufacturer=1, total_grit=1.0, total_flow=1.0, avg_grit=1.0, avg_flow=1.0, total_fractional_ascent=1.0, total_fractional_descent=1.0, enhanced_avg_altitude=1.0, enhanced_max_altitude=1.0, enhanced_min_altitude=1.0, timestamp=1758499199, message_index=1}, " +
                        "FitMemoGlob{2025-09-22 01:59:59.000, mesg_num=1, parent_index=1, field_num=1, part_index=1}, " +
                        "FitSegmentId{2025-09-22 01:59:59.000, name=a, uuid=a, sport=1, enabled=1, user_profile_primary_key=1, device_id=1, default_race_leader=1, delete_status=1, selection_type=1}, " +
                        "FitSegmentLeaderboardEntry{2025-09-22 01:59:59.000, name=a, type=1, group_primary_key=1, activity_id=1, segment_time=1.0, activity_id_string=a, message_index=1}, " +
                        "FitSegmentPoint{2025-09-22 01:59:59.000, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, distance=1.0, altitude=1.0, enhanced_altitude=1.0, message_index=1}, " +
                        "FitSegmentFile{2025-09-22 01:59:59.000, file_uuid=a, enabled=1, user_profile_primary_key=1, default_race_leader=1, message_index=1}, " +
                        "FitWorkoutSession{2025-09-22 01:59:59.000, sport=1, sub_sport=1, num_valid_steps=1, first_step_index=1, pool_length=1.0, pool_length_unit=1, message_index=1}, " +
                        "FitWatchfaceSettings{2025-09-22 01:59:59.000, mode=1, layout=1, message_index=1}, " +
                        "FitGpsMetadata{2025-09-22 01:59:59.000, timestamp_ms=1, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, enhanced_altitude=1.0, enhanced_speed=1.0, heading=1.0, utc_timestamp=1127433599, timestamp=1758499199}, " +
                        "FitCameraEvent{2025-09-22 01:59:59.000, timestamp_ms=1, camera_event_type=1, camera_file_uuid=a, camera_orientation=1, timestamp=1758499199}, " +
                        "FitTimestampCorrelation{2025-09-22 01:59:59.000, fractional_timestamp=1.0, system_timestamp=1127433599, fractional_system_timestamp=1.0, local_timestamp=1, timestamp_ms=1, system_timestamp_ms=1, timestamp=1758499199}, " +
                        "RecordData{UNK_164_gyroscope_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, 253_timestamp=1758499199}, " +
                        "RecordData{UNK_165_accelerometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, 253_timestamp=1758499199}, " +
                        "RecordData{UNK_167_three_d_sensor_calibration, 2025-09-22 01:59:59.000, unknown_0(ENUM/1)=1, unknown_1(UINT32/4)=1, unknown_2(UINT32/4)=1, unknown_3(UINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitVideoFrame{2025-09-22 01:59:59.000, timestamp_ms=1, frame_number=1, timestamp=1758499199}, " +
                        "FitObdiiData{2025-09-22 01:59:59.000, timestamp_ms=1, pid=1, start_timestamp=1127433599, start_timestamp_ms=1, timestamp=1758499199}, " +
                        "FitNmeaSentence{2025-09-22 01:59:59.000, timestamp_ms=1, sentence=a, timestamp=1758499199}, " +
                        "RecordData{UNK_178_aviation_attitude, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, 253_timestamp=1758499199}, " +
                        "FitVideo{2025-09-22 01:59:59.000, url=a, hosting_provider=a, duration=1}, " +
                        "FitVideoTitle{2025-09-22 01:59:59.000, message_count=1, text=a, message_index=1}, " +
                        "FitVideoDescription{2025-09-22 01:59:59.000, message_count=1, text=a, message_index=1}, " +
                        "FitVideoClip{2025-09-22 01:59:59.000, clip_number=1, start_timestamp=1127433599, start_timestamp_ms=1, end_timestamp=1127433599, end_timestamp_ms=1, clip_start=1, clip_end=1}, " +
                        "FitOhrSettings{2025-09-22 01:59:59.000, enabled=1, timestamp=1758499199}, " +
                        "RecordData{UNK_200_exd_screen_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(UINT8/1)=1, unknown_2(ENUM/1)=1, unknown_3(ENUM/1)=1}, " +
                        "RecordData{UNK_201_exd_data_field_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(BASE_TYPE_BYTE/1)=1, unknown_2(UINT8/1)=1, unknown_3(UINT8/1)=1, unknown_4(ENUM/1)=1}, " +
                        "RecordData{UNK_202_exd_data_concept_configuration, 2025-09-22 01:59:59.000, unknown_0(UINT8/1)=1, unknown_1(BASE_TYPE_BYTE/1)=1, unknown_2(UINT8/1)=1, unknown_3(UINT8/1)=1, unknown_4(UINT8/1)=1, unknown_5(UINT8/1)=1, unknown_6(UINT8/1)=1, unknown_8(ENUM/1)=1, unknown_9(ENUM/1)=1, unknown_10(ENUM/1)=1, unknown_11(ENUM/1)=1}, " +
                        "FitFieldDescription{2025-09-22 01:59:59.000, developer_data_index=1, field_definition_number=1, fit_base_type_id=1, array=1, components=a, scale=1, offset=1, bits=a, accumulate=a, fit_base_unit_id=1, native_mesg_num=1, native_field_num=1}, " +
                        "FitDeveloperData{2025-09-22 01:59:59.000, manufacturer_id=1, developer_data_index=1, application_version=1}, " +
                        "RecordData{UNK_208_magnetometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, 253_timestamp=1758499199}, " +
                        "FitBarometerData{2025-09-22 01:59:59.000, timestamp_ms=1, timestamp=1758499199}, " +
                        "RecordData{UNK_210_one_d_sensor_calibration, 2025-09-22 01:59:59.000, unknown_0(ENUM/1)=1, unknown_1(UINT32/4)=1, unknown_2(UINT32/4)=1, unknown_3(UINT32/4)=1, unknown_4(SINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitMonitoringHrData{2025-09-22 01:59:59.000, resting_heart_rate=1, current_day_resting_heart_rate=1, timestamp=1758499199}, " +
                        "FitTimeInZone{2025-09-22 01:59:59.000, reference_message=1, reference_index=1, hr_calc_type=1, max_heart_rate=1, resting_heart_rate=1, threshold_heart_rate=1, pwr_calc_type=1, functional_threshold_power=1, timestamp=1758499199}, " +
                        "FitSet{2025-09-22 01:59:59.000, duration=1.0, repetitions=1, weight=1.0, set_type=1, start_time=1758499199, weight_display_unit=1, message_index=1, wkt_step_index=1, timestamp=1758499199}, " +
                        "FitStressLevel{2025-09-22 01:59:59.000, stress_level_value=1, stress_level_time=1758499199}, " +
                        "FitMaxMetData{2025-09-22 01:59:59.000, update_time=1758499199, vo2_max=1.0, sport=1, sub_sport=1, max_met_category=1, calibrated_data=1, hr_source=1, speed_source=1}, " +
                        "FitDiveSettings{2025-09-22 01:59:59.000, name=a, gf_low=1, gf_high=1, water_type=1, water_density=1.0, po2_warn=1.0, po2_critical=1.0, po2_deco=1.0, safety_stop_enabled=1, bottom_depth=1.0, bottom_time=1, apnea_countdown_enabled=1, apnea_countdown_time=1, backlight_mode=1, backlight_brightness=1, backlight_timeout=1, repeat_dive_interval=1, safety_stop_time=1, heart_rate_source_type=1, heart_rate_source=1, travel_gas=1, ccr_low_setpoint_switch_mode=1, ccr_low_setpoint=1.0, ccr_low_setpoint_depth=1.0, ccr_high_setpoint_switch_mode=1, ccr_high_setpoint=1.0, ccr_high_setpoint_depth=1.0, gas_consumption_display=1, up_key_enabled=1, dive_sounds=1, last_stop_multiple=1.0, no_fly_time_mode=1, timestamp=1758499199, message_index=1}, " +
                        "FitDiveGas{2025-09-22 01:59:59.000, helium_content=1, oxygen_content=1, status=1, mode=1, message_index=1}, " +
                        "FitDiveAlarm{2025-09-22 01:59:59.000, depth=1.0, time=1, enabled=true, alarm_type=1, sound=1, id=1, popup_enabled=true, trigger_on_descent=true, trigger_on_ascent=true, repeating=true, speed=1.0, message_index=1}, " +
                        "FitExerciseTitle{2025-09-22 01:59:59.000, exercise_category=1, exercise_name=1, message_index=1}, " +
                        "FitDiveSummary{2025-09-22 01:59:59.000, reference_mesg=1, reference_index=1, avg_depth=1.0, max_depth=1.0, surface_interval=1, start_cns=1, end_cns=1, start_n2=1, end_n2=1, o2_toxicity=1, dive_number=1, bottom_time=1.0, avg_pressure_sac=1.0, avg_volume_sac=1.0, avg_rmv=1.0, descent_time=1.0, ascent_time=1.0, avg_ascent_rate=1.0, avg_descent_rate=1.0, max_ascent_rate=1.0, max_descent_rate=1.0, hang_time=1.0, timestamp=1758499199}, " +
                        "FitSpo2{2025-09-22 01:59:59.000, reading_spo2=1, reading_confidence=1, mode=1, timestamp=1758499199}, " +
                        "FitSleepStage{2025-09-22 01:59:59.000, sleep_stage=AWAKE, timestamp=1758499199}, " +
                        "FitJump{2025-09-22 01:59:59.000, distance=1.0, heigh=1.0, rotations=1, hang_time=1.0, score=1.0, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, speed=1.0, enhanced_speed=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_289_aad_accel_features, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT32/4)=1, unknown_2(UINT16/2)=1, unknown_3(UINT8/1)=1, unknown_4(UINT16/2)=25, 253_timestamp=1758499199}, " +
                        "FitBeatIntervals{2025-09-22 01:59:59.000, timestamp_ms=1, timestamp=1758499199}, " +
                        "FitRespirationRate{2025-09-22 01:59:59.000, respiration_rate=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_302_hsa_accelerometer_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/2)=1, unknown_5(UINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitHsaStepData{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}, " +
                        "FitHsaSpo2Data{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}, " +
                        "FitHsaStressData{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}, " +
                        "FitHsaRespirationData{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}, " +
                        "FitHsaHeartRateData{2025-09-22 01:59:59.000, processing_interval=1, status=1, timestamp=1758499199}, " +
                        "FitSplit{2025-09-22 01:59:59.000, split_type=2, total_elapsed_time=1.0, total_timer_time=1.0, total_distance=1.0, avg_speed=1.0, start_time=1127433599, total_ascent=1, total_descent=1, start_position_lat=8.381903171539307E-8, start_position_long=8.381903171539307E-8, end_position_lat=8.381903171539307E-8, end_position_long=8.381903171539307E-8, max_speed=1.0, avg_vert_speed=1.0, end_time=1127433599, total_calories=1, start_elevation=1.0, total_moving_time=1.0, message_index=1}, " +
                        "FitSplitSummary{2025-09-22 01:59:59.000, split_type=2, num_splits=1, total_timer_time=1.0, total_distance=1.0, avg_speed=1.0, max_speed=1.0, total_ascent=1, total_descent=1, avg_heart_rate=1, max_heart_rate=1, avg_vert_speed=1.0, total_calories=1, total_moving_time=1.0, message_index=1}, " +
                        "FitHsaBodyBatteryData{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}, " +
                        "FitHsaEvent{2025-09-22 01:59:59.000, event_id=1, timestamp=1758499199}, " +
                        "FitClimbPro{2025-09-22 01:59:59.000, position_lat=8.381903171539307E-8, position_long=8.381903171539307E-8, climb_pro_event=1, climb_number=1, climb_category=1, current_dist=1.0, timestamp=1758499199}, " +
                        "FitTankUpdate{2025-09-22 01:59:59.000, sensor=1, pressure=1.0, timestamp=1758499199}, " +
                        "FitTankSummary{2025-09-22 01:59:59.000, sensor=1, start_pressure=1.0, end_pressure=1.0, volume_used=1.0, timestamp=1758499199}, " +
                        "FitSleepStats{2025-09-22 01:59:59.000, combined_awake_score=1, awake_time_score=1, awakenings_count_score=1, deep_sleep_score=1, sleep_duration_score=1, light_sleep_score=1, overall_sleep_score=1, sleep_quality_score=1, sleep_recovery_score=1, rem_sleep_score=1, sleep_restlessness_score=1, awakenings_count=1, interruptions_score=1, average_stress_during_sleep=1.0}, " +
                        "FitHrvSummary{2025-09-22 01:59:59.000, weekly_average=1.0, last_night_average=1.0, last_night_5_min_high=1.0, baseline_low_upper=1.0, baseline_balanced_lower=1.0, baseline_balanced_upper=1.0, status=POOR, timestamp=1758499199}, " +
                        "FitHrvValue{2025-09-22 01:59:59.000, value=1.0, timestamp=1758499199}, " +
                        "RecordData{UNK_372_raw_bbi, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, 253_timestamp=1758499199}, " +
                        "FitDeviceAuxBatteryInfo{2025-09-22 01:59:59.000, device_index=1, battery_voltage=1.0, battery_status=1, battery_identifier=1, timestamp=1758499199}, " +
                        "RecordData{UNK_376_hsa_gyroscope_data, 2025-09-22 01:59:59.000, unknown_0(UINT16/2)=1, unknown_1(UINT16/2)=1, unknown_5(UINT32/4)=1, 253_timestamp=1758499199}, " +
                        "FitChronoShotSession{2025-09-22 01:59:59.000, min_speed=1.0, max_speed=1.0, avg_speed=1.0, shot_count=1, projectile_type=1, grain_weight=1.0, standard_deviation=1.0, timestamp=1758499199}, " +
                        "FitChronoShotData{2025-09-22 01:59:59.000, shot_speed=1.0, shot_num=1, timestamp=1758499199}, " +
                        "FitHsaConfigurationData{2025-09-22 01:59:59.000, data_size=1, timestamp=1758499199}, " +
                        "FitDiveApneaAlarm{2025-09-22 01:59:59.000, depth=1.0, time=1, enabled=true, alarm_type=1, sound=1, id=1, popup_enabled=true, trigger_on_descent=true, trigger_on_ascent=true, repeating=true, speed=1.0, message_index=1}, " +
                        "FitSkinTempOvernight{2025-09-22 01:59:59.000, local_timestamp=1, average_deviation=1.0, average_7_day_deviation=1.0, nightly_value=1.0, timestamp=1758499199}, " +
                        "FitHsaWristTemperatureData{2025-09-22 01:59:59.000, processing_interval=1, timestamp=1758499199}]";

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
                String name = method.getName();
                if (!name.startsWith("get")) {
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
                    String message = name + " failed for " + record;
                    if ("FitRecord".equals(recordClass.getSimpleName()) && "getLatitude".equals(name)){
                        // TODO GarminSupportTest.TestFitFileDevelopersField -> FitRecord / getLatitude
                        // FIXME java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Double
                        continue;
                    }
                    throw new AssertionError(message, e);
                }
            }
        }
    }
}