package nodomain.freeyourgadget.gadgetbridge.devices;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class AbstractDeviceCoordinatorTest extends TestBase {
    @Test
    public void deviceMatchingByNameTest() {
        final Map<String, DeviceType> bluetoothNameToExpectedType = new HashMap<>() {{
            put("Active 2 NFC (Round)", DeviceType.AMAZFITACTIVE2NFC);
            put("Amazfit Band 7", DeviceType.AMAZFITBAND7); // #2945
            put("Amazfit GTR 3 Pro", DeviceType.AMAZFITGTR3PRO); // #2442
            put("Amazfit GTS", DeviceType.AMAZFITGTS); // #5391
            put("Amazfit GTS 3", DeviceType.AMAZFITGTS3); // #2442
            put("Amazfit Helio Ring", DeviceType.AMAZFITHELIORING);
            put("Amazfit T-Rex 2", DeviceType.AMAZFITTREX2); // #3033
            put("Xiaomi Band 9 Active AB01", DeviceType.MIBAND9ACTIVE);
            put("vívoactive 6", DeviceType.GARMIN_VIVOACTIVE_6);
            put("HC96", DeviceType.HC96);
            put("P8", DeviceType.WASPOS); // from wasp-os source
            put("P8DFU", DeviceType.WASPOS); // from wasp-os source
            put("P80", DeviceType.COLMI_P80);
            put("BT103(ID-AB01)", DeviceType.OUKITEL_BT103);
            put("P66D(ID-AB01)", DeviceType.DOTN_P66D);
            put("HAYLOU Watch 2 Pro", DeviceType.HAYLOU_WATCH_2_PRO);
            put("Y66(ID-AB01)", DeviceType.Y66);
            put("R11C_B200", DeviceType.YAWELL_R11);
            put("R11_B200", DeviceType.YAWELL_R11);
            put("WF-C710N", DeviceType.SONY_WF_C710N);
            put("John's WF-C710N", DeviceType.SONY_WF_C710N);
            put("LE_WF-C710N", null);
            put("Polar H10 96C0B12D", DeviceType.POLARH10);
            put("Forerunner 165 Music", DeviceType.GARMIN_FORERUNNER_165_MUSIC);
            put("R60", DeviceType.BLACKVIEW_R60);
            put("eTrex SE", DeviceType.GARMIN_ETREX_SE);
            put("Instinct Tactical", DeviceType.GARMIN_INSTINCT_TACTICAL);
            put("Xiaomi Smart Band 10 8C9F", DeviceType.MIBAND10);
            put("Venu X1", DeviceType.GARMIN_VENU_X1);
            put("Redmi Watch 5 163A", DeviceType.REDMIWATCH5);
            put("Descent Mk3 43mm", DeviceType.GARMIN_DESCENT_MK3);
            put("Active 2 NFC (Round)-4B84", DeviceType.AMAZFITACTIVE2NFC);
            put("Redmi Buds 6 Pro", DeviceType.REDMIBUDS6PRO);
            put("Xiaomi Watch S4 AB01", DeviceType.XIAOMI_WATCH_S4);
            put("HUAWEI WATCH FIT 4 Pro-CC6", DeviceType.HUAWEIWATCHFIT4PRO);
            put("Edge Explore 2", DeviceType.GARMIN_EDGE_EXPLORE_2);
            put("Redmi Smart Band 3 CCF1", DeviceType.REDMISMARTBAND3);
            put("fenix 6X Pro", DeviceType.GARMIN_FENIX_6X_PRO);
            put("R50Pro", DeviceType.R50PRO);
            put("R11_0500", DeviceType.YAWELL_R11); // #4711
            put("Lily 2 Active", DeviceType.GARMIN_LILY_2_ACTIVE);
            put("Instinct 3 - 45mm", DeviceType.GARMIN_INSTINCT_3); // #4923
            put("Instinct E - 45mm", DeviceType.GARMIN_INSTINCT_E); // #4526
            put("fenix 7S Pro", DeviceType.GARMIN_FENIX_7S_PRO); // #4488
            put("Forerunner 45", DeviceType.GARMIN_FORERUNNER_45);
            put("Sony ULT", DeviceType.SONY_WH_ULT900N); // #4444
            put("Instinct Dual Power", DeviceType.GARMIN_INSTINCT_SOLAR); // #4380
            put("Redmi Buds 4 Active", DeviceType.REDMIBUDS4ACTIVE); // #4359
            put("OPPO Enco Air2", DeviceType.OPPO_ENCO_AIR2);
            put("Forerunner 55", DeviceType.GARMIN_FORERUNNER_55);
            put("fenix 7", DeviceType.GARMIN_FENIX_7);
            put("Venu Sq", DeviceType.GARMIN_VENU_SQ);
            put("fenix 8 - 51mm", DeviceType.GARMIN_FENIX_8); // #4226
            put("Forerunner 265S", DeviceType.GARMIN_FORERUNNER_265S);
            put("Forerunner 955", DeviceType.GARMIN_FORERUNNER_955); // #4124
            put("Enduro 3", DeviceType.GARMIN_ENDURO_3);
            put("Redmi Watch 5 Active E7B7", DeviceType.REDMIWATCH5ACTIVE);
            put("Forerunner 165", DeviceType.GARMIN_FORERUNNER_165);
            put("Xiaomi Smart Band 9 7E1E", DeviceType.MIBAND9);
            put("Venu 2S", DeviceType.GARMIN_VENU_2S); // #4010
            put("Venu", DeviceType.GARMIN_VENU); // #4003
            put(".bohemic", DeviceType.BOHEMIC_SMART_BRACELET); // #3190
            put("IMP-2027", DeviceType.VIVITAR_HR_BP_MONITOR_ACTIVITY_TRACKER); // #3925
            put("CMF Buds Pro 2", DeviceType.NOTHING_CMF_BUDS_PRO_2); // #3924
            put("CMF Watch Pro 2-0DCA", DeviceType.NOTHING_CMF_WATCH_PRO_2); // #3899
            put("vívomove Trend", DeviceType.GARMIN_VIVOMOVE_TREND); // #3875
            put("fenix 5", DeviceType.GARMIN_FENIX_5); // #3869
            put("Forerunner 255S", DeviceType.GARMIN_FORERUNNER_255S); // #3841
            put("Venu 2", DeviceType.GARMIN_VENU_2); // #3835
            put("Forerunner 265", DeviceType.GARMIN_FORERUNNER_265); // #3831
            put("EPIX PRO - 51mm", DeviceType.GARMIN_EPIX_PRO); // #3810
            put("Amazfit GTR", DeviceType.AMAZFITGTR); // #3809 / #2442
            put("Amazfit Bip 3", DeviceType.AMAZFITBIP3); // #3627
            put("Xiaomi Band 8 Active 0C09", DeviceType.MIBAND8ACTIVE); // #3614
            put("UAT-4261", DeviceType.GARMIN_VENU_3S); // #3602
            put("Redmi Watch 3 892C", DeviceType.REDMIWATCH3); // #3581
            put("Redmi Buds 5 Pro", DeviceType.REDMIBUDS5PRO); // #3566
            put("Redmi Watch 2 C21E", DeviceType.REDMIWATCH2); // #3543
            put("MHO-C303", DeviceType.MIJIA_MHO_C303); // #3513
            put("Xiaomi Smart Band 8 Pro CF08", DeviceType.MIBAND8PRO); // #3471
            put("Xiaomi Smart Band 8 D3BD", DeviceType.MIBAND8); // #3146
            put("Watch Pro", DeviceType.NOTHING_CMF_WATCH_PRO); // #3468
            put("vívoactive 5", DeviceType.GARMIN_VIVOACTIVE_5); // #3459
            put("ColaCao23", DeviceType.COLACAO23); // #3455
            put("ColaCao21", DeviceType.COLACAO21); // #2955
            put("Xiaomi Watch S1 Pro CF1B", DeviceType.XIAOMI_WATCH_S1_PRO); // #3450
            put("Ear (2)", DeviceType.NOTHING_EAR2); // #3450
            put("realme Buds T110", DeviceType.REALME_BUDS_T110);
            put("Redmi Smart Band 2 604D", DeviceType.REDMISMARTBAND2); // #3274
            put("vívosmart 5", DeviceType.GARMIN_VIVOSMART_5); // #3269
            put("Instinct Crossover", DeviceType.GARMIN_INSTINCT_CROSSOVER); // #3252
            put("Xiaomi Watch S3 eSIM 9BD2", DeviceType.XIAOMI_WATCH_S3); // #3506
            put("Amazfit Bip Lite", DeviceType.AMAZFITBIP_LITE); // #1648
            put("Mi Band 3", DeviceType.MIBAND3); // #1113
            put("Q82543", DeviceType.Q8); // #978
            put("vívomove HR", DeviceType.VIVOMOVE_HR); // #959
            put("WH-1000XM4", DeviceType.SONY_WH_1000XM4); // #2549
            put("Mi Smart Band 6", DeviceType.MIBAND6); // #2263
            put("Mi Watch Lite_ABF4", DeviceType.MIWATCHLITE); // #2234
            put("Zepp E", DeviceType.ZEPP_E); // #2176
            put("Bip S Lite", DeviceType.AMAZFITBIPS_LITE); // #2055
            put("Mi Smart Band 5", DeviceType.MIBAND5); // #1929
            put("CASIO STB-1000", DeviceType.CASIOGB6900); // #1902
            put("Amazfit Bip 3 Pro", DeviceType.AMAZFITBIP3PRO); // #3249
            put("Redmi Band Pro AB01", DeviceType.REDMISMARTBANDPRO); // #3069
            put("HRMPro+:123456", DeviceType.GARMIN_HRM_PRO_PLUS); // #5364
            put("Instinct 2S Solar", DeviceType.GARMIN_INSTINCT_2S_SOLAR); // #3063
            put("LE_WH-1000XM5", DeviceType.SONY_WH_1000XM5); // #2969
            put("WH-1000XM2", DeviceType.SONY_WH_1000XM2); // #2935
            put("WF-1000XM4", DeviceType.SONY_WF_1000XM4); // #2925
            put("Xiaomi Smart Band 7 Pro", DeviceType.MIBAND7PRO); // #2781
            put("Galaxy Buds Pro (B352)", DeviceType.GALAXY_BUDS_PRO); // #2642
            put("Redmi Watch 2 Lite 31A5", DeviceType.REDMIWATCH2LITE); // #2637
            put("HUAWEI Band 6-A47", DeviceType.HUAWEIBAND6); // #2569
            put("716", DeviceType.FITPRO);
            put("LH716", DeviceType.FITPRO);
            put("Sunset 6", DeviceType.FITPRO);
            put("Watch7", DeviceType.FITPRO);
            put("Fit1900", DeviceType.FITPRO);
            put("M6-4711", DeviceType.FITPRO);
            put("M4-4711", DeviceType.FITPRO);
        }};

        for (Map.Entry<String, DeviceType> e : bluetoothNameToExpectedType.entrySet()) {
            final String bluetoothName = e.getKey();
            final DeviceType expectedType = e.getValue();
            final List<DeviceType> matches = new ArrayList<>(1);
            // Check the bluetooth name against all existing coordinators
            for (DeviceType type : DeviceType.values()) {
                final Pattern pattern = ((AbstractDeviceCoordinator) type.getDeviceCoordinator()).getSupportedDeviceName();
                if (pattern != null) {
                    if (pattern.matcher(bluetoothName).matches()) {
                        matches.add(type);
                    }
                }
            }

            if (expectedType != null) {
                Assert.assertEquals(
                        "Bluetooth name " + bluetoothName + " should only match the expected DeviceType",
                        Collections.singletonList(expectedType),
                        matches
                );
            } else {
                Assert.assertTrue(
                        "Bluetooth name " + bluetoothName + " should match no DeviceType",
                        matches.isEmpty()
                );
            }
        }
    }
}
