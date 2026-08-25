package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

// source: https://raw.githubusercontent.com/muktihari/fit/master/profile/typedef/garmin_product_gen.go
public final class GarminProductNumbers {
    private GarminProductNumbers() {
    }

    private static final Map<Integer, DeviceType> mProductNumbers = new HashMap<>(200);

    @Nullable
    public static DeviceType getDeviceType(final int productNumber) {
        return mProductNumbers.get(productNumber);
    }

    static {
        // mProductNumbers.put(1, DeviceType.GARMIN_HRM1);
        // mProductNumbers.put(2, DeviceType.GARMIN_AXH01); // AXH01 HRM chipset
        // mProductNumbers.put(3, DeviceType.GARMIN_AXB01);
        // mProductNumbers.put(4, DeviceType.GARMIN_AXB02);
        // mProductNumbers.put(5, DeviceType.GARMIN_HRM2SS);
        // mProductNumbers.put(6, DeviceType.GARMIN_DSI_ALF02);
        // mProductNumbers.put(7, DeviceType.GARMIN_HRM3SS);
        // mProductNumbers.put(8, DeviceType.GARMIN_HRM_RUN_SINGLE_BYTE_PRODUCT_ID);  // hrm_run model for HRM ANT+ messaging
        // mProductNumbers.put(9, DeviceType.GARMIN_BSM);  // BSM model for ANT+ messaging
        // mProductNumbers.put(10, DeviceType.GARMIN_BCM); // BCM model for ANT+ messaging
        // mProductNumbers.put(11, DeviceType.GARMIN_AXS01); // AXS01 HRM Bike Chipset model for ANT+ messaging
        // mProductNumbers.put(12, DeviceType.GARMIN_HRM_TRI_SINGLE_BYTE_PRODUCT_ID); // hrm_tri model for HRM ANT+ messaging
        // mProductNumbers.put(13, DeviceType.GARMIN_HRM4_RUN_SINGLE_BYTE_PRODUCT_ID); // hrm4 run model for HRM ANT+ messaging
        // mProductNumbers.put(14, DeviceType.GARMIN_FORERUNNER_225_SINGLE_BYTE_PRODUCT_ID); // fr225 model for HRM ANT+ messaging
        // mProductNumbers.put(15, DeviceType.GARMIN_GEN3_BSM_SINGLE_BYTE_PRODUCT_ID); // gen3_bsm model for Bike Speed ANT+ messaging
        // mProductNumbers.put(16, DeviceType.GARMIN_GEN3_BCM_SINGLE_BYTE_PRODUCT_ID); // gen3_bcm model for Bike Cadence ANT+ messaging
        // mProductNumbers.put(22, DeviceType.GARMIN_HRM_FIT_SINGLE_BYTE_PRODUCT_ID);
        // mProductNumbers.put(255, DeviceType.GARMIN_OHR); // Garmin Wearable Optical Heart Rate Sensor for ANT+ HR Profile Broadcasting
        // mProductNumbers.put(473, DeviceType.GARMIN_FORERUNNER_301_CHINA);
        // mProductNumbers.put(474, DeviceType.GARMIN_FORERUNNER_301_JAPAN);
        // mProductNumbers.put(475, DeviceType.GARMIN_FORERUNNER_301_KOREA);
        // mProductNumbers.put(494, DeviceType.GARMIN_FORERUNNER_301_TAIWAN);
        // mProductNumbers.put(717, DeviceType.GARMIN_FORERUNNER_405); // Forerunner 405
        // mProductNumbers.put(782, DeviceType.GARMIN_FORERUNNER_50); // Forerunner 50
        // mProductNumbers.put(987, DeviceType.GARMIN_FORERUNNER_405_JAPAN);
        // mProductNumbers.put(988, DeviceType.GARMIN_FORERUNNER_60); // Forerunner 60
        // mProductNumbers.put(1011, DeviceType.GARMIN_DSI_ALF01);
        // mProductNumbers.put(1018, DeviceType.GARMIN_FORERUNNER_310XT); // Forerunner 310
        // mProductNumbers.put(1036, DeviceType.GARMIN_EDGE_500);
        // mProductNumbers.put(1124, DeviceType.GARMIN_FORERUNNER_110); // Forerunner 110
        // mProductNumbers.put(1169, DeviceType.GARMIN_EDGE_800);
        // mProductNumbers.put(1199, DeviceType.GARMIN_EDGE_500_TAIWAN);
        // mProductNumbers.put(1213, DeviceType.GARMIN_EDGE_500_JAPAN);
        // mProductNumbers.put(1253, DeviceType.GARMIN_CHIRP);
        // mProductNumbers.put(1274, DeviceType.GARMIN_FORERUNNER_110_JAPAN);
        // mProductNumbers.put(1325, DeviceType.GARMIN_EDGE_200);
        // mProductNumbers.put(1328, DeviceType.GARMIN_FORERUNNER_910XT);
        // mProductNumbers.put(1333, DeviceType.GARMIN_EDGE_800_TAIWAN);
        // mProductNumbers.put(1334, DeviceType.GARMIN_EDGE_800_JAPAN);
        // mProductNumbers.put(1341, DeviceType.GARMIN_ALF04);
        // mProductNumbers.put(1345, DeviceType.GARMIN_FORERUNNER_610);
        // mProductNumbers.put(1360, DeviceType.GARMIN_FORERUNNER_210_JAPAN);
        // mProductNumbers.put(1380, DeviceType.GARMIN_VECTOR_SS);
        // mProductNumbers.put(1381, DeviceType.GARMIN_VECTOR_CP);
        // mProductNumbers.put(1386, DeviceType.GARMIN_EDGE_800_CHINA);
        // mProductNumbers.put(1387, DeviceType.GARMIN_EDGE_500_CHINA);
        // mProductNumbers.put(1405, DeviceType.GARMIN_APPROACH_G10);
        // mProductNumbers.put(1410, DeviceType.GARMIN_FORERUNNER_610_JAPAN);
        // mProductNumbers.put(1422, DeviceType.GARMIN_EDGE_500_KOREA);
        // mProductNumbers.put(1436, DeviceType.GARMIN_FORERUNNER_70);
        // mProductNumbers.put(1446, DeviceType.GARMIN_FORERUNNER_310XT4T);
        // mProductNumbers.put(1461, DeviceType.GARMIN_AMX);
        // mProductNumbers.put(1482, DeviceType.GARMIN_FORERUNNER_10);
        // mProductNumbers.put(1497, DeviceType.GARMIN_EDGE_800_KOREA);
        // mProductNumbers.put(1499, DeviceType.GARMIN_SWIM);
        // mProductNumbers.put(1537, DeviceType.GARMIN_FORERUNNER_910XT_CHINA);
        // mProductNumbers.put(1551, DeviceType.GARMIN_FENIX_);
        // mProductNumbers.put(1555, DeviceType.GARMIN_EDGE_200_TAIWAN);
        mProductNumbers.put(1561, DeviceType.GARMIN_EDGE_510);
        mProductNumbers.put(1567, DeviceType.GARMIN_EDGE_810);
        // mProductNumbers.put(1570, DeviceType.GARMIN_TEMPE);
        // mProductNumbers.put(1600, DeviceType.GARMIN_FORERUNNER_910XT_JAPAN);
        mProductNumbers.put(1623, DeviceType.GARMIN_FORERUNNER_620);
        mProductNumbers.put(1632, DeviceType.GARMIN_FORERUNNER_220);
        // mProductNumbers.put(1664, DeviceType.GARMIN_FORERUNNER_910XT_KOREA);
        // mProductNumbers.put(1688, DeviceType.GARMIN_FORERUNNER_10_JAPAN);
        mProductNumbers.put(1721, DeviceType.GARMIN_EDGE_810); // JAPAN
        // mProductNumbers.put(1735, DeviceType.GARMIN_VIRB_ELITE);
        // mProductNumbers.put(1736, DeviceType.GARMIN_EDGE_TOURING); // Also Edge Touring Plus
        mProductNumbers.put(1742, DeviceType.GARMIN_EDGE_510); // JAPAN
        // mProductNumbers.put(1743, DeviceType.GARMIN_HRM_TRI); // Also HRM-Swim
        // mProductNumbers.put(1752, DeviceType.GARMIN_HRM_RUN);
        mProductNumbers.put(1765, DeviceType.GARMIN_FORERUNNER_920);
        mProductNumbers.put(1821, DeviceType.GARMIN_EDGE_510); // ASIA
        mProductNumbers.put(1822, DeviceType.GARMIN_EDGE_810); // CHINA
        mProductNumbers.put(1823, DeviceType.GARMIN_EDGE_810); // TAIWAN
        mProductNumbers.put(1836, DeviceType.GARMIN_EDGE_1000);
        // mProductNumbers.put(1837, DeviceType.GARMIN_VIVO_FIT);
        // mProductNumbers.put(1853, DeviceType.GARMIN_VIRB_REMOTE);
        // mProductNumbers.put(1885, DeviceType.GARMIN_VIVO_KI);
        // mProductNumbers.put(1903, DeviceType.GARMIN_FORERUNNER_15);
        mProductNumbers.put(1907, DeviceType.GARMIN_VIVOACTIVE);
        mProductNumbers.put(1918, DeviceType.GARMIN_EDGE_510); // KOREA
        mProductNumbers.put(1928, DeviceType.GARMIN_FORERUNNER_620); // JAPAN
        mProductNumbers.put(1929, DeviceType.GARMIN_FORERUNNER_620); // CHINA
        mProductNumbers.put(1930, DeviceType.GARMIN_FORERUNNER_220); // JAPAN
        mProductNumbers.put(1931, DeviceType.GARMIN_FORERUNNER_220); // CHINA
        // mProductNumbers.put(1936, DeviceType.GARMIN_APPROACH_S6);
        mProductNumbers.put(1956, DeviceType.GARMIN_VIVOSMART);
        mProductNumbers.put(1967, DeviceType.GARMIN_FENIX_2);
        mProductNumbers.put(1988, DeviceType.GARMIN_EPIX);
        mProductNumbers.put(2050, DeviceType.GARMIN_FENIX_3);
        mProductNumbers.put(2052, DeviceType.GARMIN_EDGE_1000); // TAIWAN
        mProductNumbers.put(2053, DeviceType.GARMIN_EDGE_1000); // JAPAN
        // mProductNumbers.put(2061, DeviceType.GARMIN_FORERUNNER_15_JAPAN);
        mProductNumbers.put(2067, DeviceType.GARMIN_EDGE_520);
        mProductNumbers.put(2070, DeviceType.GARMIN_EDGE_1000); // CHINA
        mProductNumbers.put(2072, DeviceType.GARMIN_FORERUNNER_620); // RUSSIA
        mProductNumbers.put(2073, DeviceType.GARMIN_FORERUNNER_220); // RUSSIA
        // mProductNumbers.put(2079, DeviceType.GARMIN_VECTOR_S);
        mProductNumbers.put(2100, DeviceType.GARMIN_EDGE_1000); // KOREA
        // mProductNumbers.put(2130, DeviceType.GARMIN_FORERUNNER_920XT_TAIWAN);
        // mProductNumbers.put(2131, DeviceType.GARMIN_FORERUNNER_920XT_CHINA);
        // mProductNumbers.put(2132, DeviceType.GARMIN_FORERUNNER_920XT_JAPAN);
        // mProductNumbers.put(2134, DeviceType.GARMIN_VIRBX);
        mProductNumbers.put(2135, DeviceType.GARMIN_VIVOSMART); // APAC
        // mProductNumbers.put(2140, DeviceType.GARMIN_ETREX_TOUCH);
        mProductNumbers.put(2147, DeviceType.GARMIN_EDGE_25);
        mProductNumbers.put(2148, DeviceType.GARMIN_FORERUNNER_25);
        // mProductNumbers.put(2150, DeviceType.GARMIN_VIVO_FIT2);
        mProductNumbers.put(2153, DeviceType.GARMIN_FORERUNNER_225);
        mProductNumbers.put(2156, DeviceType.GARMIN_FORERUNNER_630);
        mProductNumbers.put(2157, DeviceType.GARMIN_FORERUNNER_230);
        mProductNumbers.put(2158, DeviceType.GARMIN_FORERUNNER_735XT);
        mProductNumbers.put(2160, DeviceType.GARMIN_VIVOACTIVE); // APAC
        // mProductNumbers.put(2161, DeviceType.GARMIN_VECTOR2);
        // mProductNumbers.put(2162, DeviceType.GARMIN_VECTOR2S);
        // mProductNumbers.put(2172, DeviceType.GARMIN_VIRBXE);
        mProductNumbers.put(2173, DeviceType.GARMIN_FORERUNNER_620); // TAIWAN
        mProductNumbers.put(2174, DeviceType.GARMIN_FORERUNNER_220); // TAIWAN
        // mProductNumbers.put(2175, DeviceType.GARMIN_TRUSWING);
        // mProductNumbers.put(2187, DeviceType.GARMIN_D2AIRVENU);
        mProductNumbers.put(2188, DeviceType.GARMIN_FENIX_3); // CHINA
        mProductNumbers.put(2189, DeviceType.GARMIN_FENIX_3); // TWN
        // mProductNumbers.put(2192, DeviceType.GARMIN_VARIA_HEADLIGHT);
        // mProductNumbers.put(2193, DeviceType.GARMIN_VARIA_TAILLIGHT_OLD);
        mProductNumbers.put(2204, DeviceType.GARMIN_EDGE_EXPLORE_1000);
        mProductNumbers.put(2219, DeviceType.GARMIN_FORERUNNER_225); // ASIA
        // mProductNumbers.put(2225, DeviceType.GARMIN_VARIA_RADAR_TAILLIGHT);
        // mProductNumbers.put(2226, DeviceType.GARMIN_VARIA_RADAR_DISPLAY);
        mProductNumbers.put(2238, DeviceType.GARMIN_EDGE_25); // Edge 2x
        mProductNumbers.put(2260, DeviceType.GARMIN_EDGE_520); // ASIA
        mProductNumbers.put(2261, DeviceType.GARMIN_EDGE_520); // JAPAN
        // mProductNumbers.put(2262, DeviceType.GARMIN_D2_BRAVO);
        // mProductNumbers.put(2266, DeviceType.GARMIN_APPROACH_S20);
        mProductNumbers.put(2271, DeviceType.GARMIN_VIVOSMART); // VIVO_SMART2 - hardware revision 2? not a separate model
        mProductNumbers.put(2274, DeviceType.GARMIN_EDGE_1000); // THAI
        // mProductNumbers.put(2276, DeviceType.GARMIN_VARIA_REMOTE);
        mProductNumbers.put(2288, DeviceType.GARMIN_EDGE_25); // ASIA
        mProductNumbers.put(2289, DeviceType.GARMIN_EDGE_25); // JPN
        mProductNumbers.put(2290, DeviceType.GARMIN_EDGE_25); // Edge 2x, ASIA
        // mProductNumbers.put(2292, DeviceType.GARMIN_APPROACH_X40);
        mProductNumbers.put(2293, DeviceType.GARMIN_FENIX_3); // JAPAN
        mProductNumbers.put(2294, DeviceType.GARMIN_VIVOSMART); // EMEA
        mProductNumbers.put(2310, DeviceType.GARMIN_FORERUNNER_630); // ASIA
        mProductNumbers.put(2311, DeviceType.GARMIN_FORERUNNER_630); // JPN
        mProductNumbers.put(2313, DeviceType.GARMIN_FORERUNNER_230); // JPN
        // mProductNumbers.put(2327, DeviceType.GARMIN_HRM4_RUN);
        mProductNumbers.put(2332, DeviceType.GARMIN_EPIX); // JAPAN
        mProductNumbers.put(2337, DeviceType.GARMIN_VIVOACTIVE_HR);
        mProductNumbers.put(2347, DeviceType.GARMIN_VIVOSMART_HR_PLUS); // GPS_HR
        mProductNumbers.put(2348, DeviceType.GARMIN_VIVOSMART_HR);
        mProductNumbers.put(2361, DeviceType.GARMIN_VIVOSMART_HR); // ASIA
        mProductNumbers.put(2362, DeviceType.GARMIN_VIVOSMART_HR_PLUS); // GPS_HR_ASIA
        mProductNumbers.put(2368, DeviceType.GARMIN_VIVOMOVE);
        // mProductNumbers.put(2379, DeviceType.GARMIN_VARIA_TAILLIGHT);
        mProductNumbers.put(2396, DeviceType.GARMIN_FORERUNNER_235); // ASIA
        mProductNumbers.put(2397, DeviceType.GARMIN_FORERUNNER_235); // JAPAN
        // mProductNumbers.put(2398, DeviceType.GARMIN_VARIA_VISION);
        // mProductNumbers.put(2406, DeviceType.GARMIN_VIVO_FIT3);
        mProductNumbers.put(2407, DeviceType.GARMIN_FENIX_3); // KOREA
        mProductNumbers.put(2408, DeviceType.GARMIN_FENIX_3); // SEA
        mProductNumbers.put(2413, DeviceType.GARMIN_FENIX_3_HR);
        // mProductNumbers.put(2417, DeviceType.GARMIN_VIRB_ULTRA30);
        // mProductNumbers.put(2429, DeviceType.GARMIN_INDEX_SMART_SCALE);
        mProductNumbers.put(2431, DeviceType.GARMIN_FORERUNNER_235);
        mProductNumbers.put(2432, DeviceType.GARMIN_FENIX_3_CHRONOS);
        // mProductNumbers.put(2441, DeviceType.GARMIN_OREGON7XX);
        // mProductNumbers.put(2444, DeviceType.GARMIN_RINO7XX);
        mProductNumbers.put(2457, DeviceType.GARMIN_EPIX); // KOREA
        mProductNumbers.put(2473, DeviceType.GARMIN_FENIX_3_HR); // CHN
        mProductNumbers.put(2474, DeviceType.GARMIN_FENIX_3_HR); // TWN
        mProductNumbers.put(2475, DeviceType.GARMIN_FENIX_3_HR); // JPN
        mProductNumbers.put(2476, DeviceType.GARMIN_FENIX_3_HR); // SEA
        mProductNumbers.put(2477, DeviceType.GARMIN_FENIX_3_HR); // KOR
        // mProductNumbers.put(2496, DeviceType.GARMIN_NAUTIX);
        mProductNumbers.put(2497, DeviceType.GARMIN_VIVOACTIVE_HR); // APAC
        mProductNumbers.put(2503, DeviceType.GARMIN_FORERUNNER_35);
        // mProductNumbers.put(2512, DeviceType.GARMIN_OREGON7XX_WW);
        mProductNumbers.put(2530, DeviceType.GARMIN_EDGE_820);
        mProductNumbers.put(2531, DeviceType.GARMIN_EDGE_EXPLORE_820);
        mProductNumbers.put(2533, DeviceType.GARMIN_FORERUNNER_735XT); // APAC
        mProductNumbers.put(2534, DeviceType.GARMIN_FORERUNNER_735XT); // JAPAN
        mProductNumbers.put(2544, DeviceType.GARMIN_FENIX_5S);
        // mProductNumbers.put(2547, DeviceType.GARMIN_D2_BRAVO_TITANIUM);
        // mProductNumbers.put(2567, DeviceType.GARMIN_VARIA_UT800); // Varia UT 800 SW
        // mProductNumbers.put(2593, DeviceType.GARMIN_RUNNING_DYNAMICS_POD);
        mProductNumbers.put(2599, DeviceType.GARMIN_EDGE_820); // CHINA
        mProductNumbers.put(2600, DeviceType.GARMIN_EDGE_820); // JAPAN
        mProductNumbers.put(2604, DeviceType.GARMIN_FENIX_5X);
        // mProductNumbers.put(2606, DeviceType.GARMIN_VIVO_FIT_JR);
        mProductNumbers.put(2622, DeviceType.GARMIN_VIVOSMART_3);
        mProductNumbers.put(2623, DeviceType.GARMIN_VIVOSPORT);
        mProductNumbers.put(2628, DeviceType.GARMIN_EDGE_820); // TAIWAN
        mProductNumbers.put(2629, DeviceType.GARMIN_EDGE_820); // KOREA
        mProductNumbers.put(2630, DeviceType.GARMIN_EDGE_820); // SEA
        mProductNumbers.put(2650, DeviceType.GARMIN_FORERUNNER_35); // HEBREW
        // mProductNumbers.put(2656, DeviceType.GARMIN_APPROACH_S60);
        mProductNumbers.put(2667, DeviceType.GARMIN_FORERUNNER_35); // APAC
        mProductNumbers.put(2668, DeviceType.GARMIN_FORERUNNER_35); // JAPAN
        mProductNumbers.put(2675, DeviceType.GARMIN_FENIX_3_CHRONOS); // ASIA
        // mProductNumbers.put(2687, DeviceType.GARMIN_VIRB360);
        mProductNumbers.put(2691, DeviceType.GARMIN_FORERUNNER_935);
        mProductNumbers.put(2697, DeviceType.GARMIN_FENIX_5);
        mProductNumbers.put(2700, DeviceType.GARMIN_VIVOACTIVE_3);
        mProductNumbers.put(2733, DeviceType.GARMIN_FORERUNNER_235); // CHINA_NFC
        // mProductNumbers.put(2769, DeviceType.GARMIN_FORETREX601701);
        mProductNumbers.put(2772, DeviceType.VIVOMOVE_HR);
        mProductNumbers.put(2713, DeviceType.GARMIN_EDGE_1030);
        mProductNumbers.put(2727, DeviceType.GARMIN_FORERUNNER_35); // SEA
        // mProductNumbers.put(2787, DeviceType.GARMIN_VECTOR3);
        mProductNumbers.put(2796, DeviceType.GARMIN_FENIX_5); // ASIA
        mProductNumbers.put(2797, DeviceType.GARMIN_FENIX_5S); // ASIA
        mProductNumbers.put(2798, DeviceType.GARMIN_FENIX_5X); // ASIA
        // mProductNumbers.put(2806, DeviceType.GARMIN_APPROACH_Z80);
        mProductNumbers.put(2814, DeviceType.GARMIN_FORERUNNER_35); // KOREA
        // mProductNumbers.put(2819, DeviceType.GARMIN_D2CHARLIE);
        mProductNumbers.put(2831, DeviceType.GARMIN_VIVOSMART_3); // APAC
        mProductNumbers.put(2832, DeviceType.GARMIN_VIVOSPORT); // APAC
        mProductNumbers.put(2833, DeviceType.GARMIN_FORERUNNER_935); // ASIA
        // mProductNumbers.put(2859, DeviceType.GARMIN_DESCENT);
        // mProductNumbers.put(2878, DeviceType.GARMIN_VIVO_FIT4);
        mProductNumbers.put(2886, DeviceType.GARMIN_FORERUNNER_645);
        mProductNumbers.put(2888, DeviceType.GARMIN_FORERUNNER_645_MUSIC);
        mProductNumbers.put(2891, DeviceType.GARMIN_FORERUNNER_30);
        mProductNumbers.put(2900, DeviceType.GARMIN_FENIX_5S_PLUS);
        mProductNumbers.put(2909, DeviceType.GARMIN_EDGE_130);
        mProductNumbers.put(2924, DeviceType.GARMIN_EDGE_1030); // ASIA
        mProductNumbers.put(2927, DeviceType.GARMIN_VIVOSMART_4);
        mProductNumbers.put(2945, DeviceType.VIVOMOVE_HR); // ASIA
        // mProductNumbers.put(2962, DeviceType.GARMIN_APPROACH_X10);
        mProductNumbers.put(2977, DeviceType.GARMIN_FORERUNNER_30); // ASIA
        mProductNumbers.put(2988, DeviceType.GARMIN_VIVOACTIVE_3_MUSIC); // W
        mProductNumbers.put(3003, DeviceType.GARMIN_FORERUNNER_645); // ASIA
        mProductNumbers.put(3004, DeviceType.GARMIN_FORERUNNER_645_MUSIC); // ASIA
        mProductNumbers.put(3011, DeviceType.GARMIN_EDGE_EXPLORE);
        // mProductNumbers.put(3028, DeviceType.GARMIN_GPSMAP66);
        // mProductNumbers.put(3049, DeviceType.GARMIN_APPROACH_S10);
        mProductNumbers.put(3066, DeviceType.GARMIN_VIVOACTIVE_3_MUSIC); // L
        mProductNumbers.put(3076, DeviceType.GARMIN_FORERUNNER_245);
        mProductNumbers.put(3077, DeviceType.GARMIN_FORERUNNER_245_MUSIC);
        // mProductNumbers.put(3085, DeviceType.GARMIN_APPROACH_G80);
        mProductNumbers.put(3092, DeviceType.GARMIN_EDGE_130); // ASIA
        mProductNumbers.put(3095, DeviceType.GARMIN_EDGE_1030); // BONTRAGER
        mProductNumbers.put(3110, DeviceType.GARMIN_FENIX_5_PLUS);
        mProductNumbers.put(3111, DeviceType.GARMIN_FENIX_5X_PLUS);
        mProductNumbers.put(3112, DeviceType.GARMIN_EDGE_520_PLUS);
        mProductNumbers.put(3113, DeviceType.GARMIN_FORERUNNER_945);
        mProductNumbers.put(3121, DeviceType.GARMIN_EDGE_530);
        mProductNumbers.put(3122, DeviceType.GARMIN_EDGE_830);
        mProductNumbers.put(3126, DeviceType.GARMIN_INSTINCT); // ESPORTS
        mProductNumbers.put(3134, DeviceType.GARMIN_FENIX_5S_PLUS); // APAC
        mProductNumbers.put(3135, DeviceType.GARMIN_FENIX_5X_PLUS); // APAC
        mProductNumbers.put(3142, DeviceType.GARMIN_EDGE_520_PLUS); // APAC
        // mProductNumbers.put(3143, DeviceType.GARMIN_DESCENT_T1);
        mProductNumbers.put(3144, DeviceType.GARMIN_FORERUNNER_235); // L_ASIA
        mProductNumbers.put(3145, DeviceType.GARMIN_FORERUNNER_245); // ASIA
        mProductNumbers.put(3163, DeviceType.GARMIN_VIVOACTIVE_3_MUSIC); // APAC
        // mProductNumbers.put(3192, DeviceType.GARMIN_GEN3_BSM); // gen3 bike speed sensor
        // mProductNumbers.put(3193, DeviceType.GARMIN_GEN3_BCM); // gen3 bike cadence sensor
        mProductNumbers.put(3218, DeviceType.GARMIN_VIVOSMART_4); // ASIA
        mProductNumbers.put(3224, DeviceType.GARMIN_VIVOACTIVE_4S);
        mProductNumbers.put(3225, DeviceType.GARMIN_VIVOACTIVE_4); // LARGE
        mProductNumbers.put(3226, DeviceType.GARMIN_VENU);
        // mProductNumbers.put(3246, DeviceType.GARMIN_MARQ_DRIVER);
        // mProductNumbers.put(3247, DeviceType.GARMIN_MARQ_AVIATOR);
        // mProductNumbers.put(3248, DeviceType.GARMIN_MARQ_CAPTAIN);
        // mProductNumbers.put(3249, DeviceType.GARMIN_MARQ_COMMANDER);
        // mProductNumbers.put(3250, DeviceType.GARMIN_MARQ_EXPEDITION);
        // mProductNumbers.put(3251, DeviceType.GARMIN_MARQ_ATHLETE);
        // mProductNumbers.put(3258, DeviceType.GARMIN_DESCENT_MK2);
        mProductNumbers.put(3282, DeviceType.GARMIN_FORERUNNER_45);
        // mProductNumbers.put(3284, DeviceType.GARMIN_GPSMAP66I);
        mProductNumbers.put(3287, DeviceType.GARMIN_FENIX_6S_SPORT);
        mProductNumbers.put(3288, DeviceType.GARMIN_FENIX_6S);
        mProductNumbers.put(3289, DeviceType.GARMIN_FENIX_6_SPORT);
        mProductNumbers.put(3290, DeviceType.GARMIN_FENIX_6);
        // mProductNumbers.put(3291, DeviceType.GARMIN_FENIX_6X);
        // mProductNumbers.put(3299, DeviceType.GARMIN_HRM_DUAL); // HRM-Dual
        // mProductNumbers.put(3300, DeviceType.GARMIN_HRM_PRO); // HRM-Pro
        // mProductNumbers.put(3308, DeviceType.GARMIN_VIVO_MOVE3_PREMIUM);
        // mProductNumbers.put(3314, DeviceType.GARMIN_APPROACH_S40);
        mProductNumbers.put(3321, DeviceType.GARMIN_FORERUNNER_245_MUSIC); // ASIA
        mProductNumbers.put(3349, DeviceType.GARMIN_EDGE_530); // APAC
        mProductNumbers.put(3350, DeviceType.GARMIN_EDGE_830); // APAC
        // mProductNumbers.put(3378, DeviceType.GARMIN_VIVO_MOVE3);
        mProductNumbers.put(3387, DeviceType.GARMIN_VIVOACTIVE_4S); // ASIA
        mProductNumbers.put(3388, DeviceType.GARMIN_VIVOACTIVE_4); // LARGE_ASIA
        // mProductNumbers.put(3389, DeviceType.GARMIN_VIVO_ACTIVE4_OLED_ASIA);
        mProductNumbers.put(3405, DeviceType.GARMIN_SWIM_2);
        // mProductNumbers.put(3420, DeviceType.GARMIN_MARQ_DRIVER_ASIA);
        // mProductNumbers.put(3421, DeviceType.GARMIN_MARQ_AVIATOR_ASIA);
        // mProductNumbers.put(3422, DeviceType.GARMIN_VIVO_MOVE3_ASIA);
        mProductNumbers.put(3441, DeviceType.GARMIN_FORERUNNER_945); // ASIA
        // mProductNumbers.put(3446, DeviceType.GARMIN_VIVO_ACTIVE3T_CHN);
        // mProductNumbers.put(3448, DeviceType.GARMIN_MARQ_CAPTAIN_ASIA);
        // mProductNumbers.put(3449, DeviceType.GARMIN_MARQ_COMMANDER_ASIA);
        // mProductNumbers.put(3450, DeviceType.GARMIN_MARQ_EXPEDITION_ASIA);
        // mProductNumbers.put(3451, DeviceType.GARMIN_MARQ_ATHLETE_ASIA);
        // mProductNumbers.put(3461, DeviceType.GARMIN_INDEX_SMART_SCALE2);
        mProductNumbers.put(3466, DeviceType.GARMIN_INSTINCT_SOLAR);
        mProductNumbers.put(3469, DeviceType.GARMIN_FORERUNNER_45); // ASIA
        mProductNumbers.put(3473, DeviceType.GARMIN_VIVOACTIVE_3); // DAIMLER
        // mProductNumbers.put(3498, DeviceType.GARMIN_LEGACY_REY);
        // mProductNumbers.put(3499, DeviceType.GARMIN_LEGACY_DARTH_VADER);
        // mProductNumbers.put(3500, DeviceType.GARMIN_LEGACY_CAPTAIN_MARVEL);
        // mProductNumbers.put(3501, DeviceType.GARMIN_LEGACY_FIRST_AVENGER);
        mProductNumbers.put(3512, DeviceType.GARMIN_FENIX_6S_SPORT); // ASIA
        mProductNumbers.put(3513, DeviceType.GARMIN_FENIX_6S); // ASIA
        mProductNumbers.put(3514, DeviceType.GARMIN_FENIX_6_SPORT); // ASIA
        mProductNumbers.put(3515, DeviceType.GARMIN_FENIX_6); // ASIA
        // mProductNumbers.put(3516, DeviceType.GARMIN_FENIX_6X_ASIA);
        // mProductNumbers.put(3535, DeviceType.GARMIN_LEGACY_CAPTAIN_MARVEL_ASIA);
        // mProductNumbers.put(3536, DeviceType.GARMIN_LEGACY_FIRST_AVENGER_ASIA);
        // mProductNumbers.put(3537, DeviceType.GARMIN_LEGACY_REY_ASIA);
        // mProductNumbers.put(3538, DeviceType.GARMIN_LEGACY_DARTH_VADER_ASIA);
        // mProductNumbers.put(3542, DeviceType.GARMIN_DESCENT_MK2S);
        mProductNumbers.put(3558, DeviceType.GARMIN_EDGE_130_PLUS);
        mProductNumbers.put(3570, DeviceType.GARMIN_EDGE_1030_PLUS);
        // mProductNumbers.put(3578, DeviceType.GARMIN_RALLY200); // Rally 100/200 Power Meter Series
        mProductNumbers.put(3589, DeviceType.GARMIN_FORERUNNER_745);
        mProductNumbers.put(3596, DeviceType.GARMIN_VENU_SQ_MUSIC);
        mProductNumbers.put(3599, DeviceType.GARMIN_VENU_SQ_MUSIC); // V2 (hardware revision?)
        mProductNumbers.put(3600, DeviceType.GARMIN_VENU_SQ);
        mProductNumbers.put(3615, DeviceType.GARMIN_LILY);
        // mProductNumbers.put(3624, DeviceType.GARMIN_MARQ_ADVENTURER);
        mProductNumbers.put(3638, DeviceType.GARMIN_ENDURO);
        mProductNumbers.put(3639, DeviceType.GARMIN_SWIM_2);
        // mProductNumbers.put(3648, DeviceType.GARMIN_MARQ_ADVENTURER_ASIA);
        // mProductNumbers.put(3652, DeviceType.GARMIN_FORERUNNER_945_LTE);
        // mProductNumbers.put(3702, DeviceType.GARMIN_DESCENT_MK2_ASIA); // Mk2 and Mk2i
        mProductNumbers.put(3703, DeviceType.GARMIN_VENU_2);
        mProductNumbers.put(3704, DeviceType.GARMIN_VENU_2S);
        mProductNumbers.put(3737, DeviceType.GARMIN_VENU); // DAIMLER_ASIA
        // mProductNumbers.put(3739, DeviceType.GARMIN_MARQ_GOLFER);
        mProductNumbers.put(3740, DeviceType.GARMIN_VENU); // DAIMLER
        mProductNumbers.put(3794, DeviceType.GARMIN_FORERUNNER_745); // ASIA
        // mProductNumbers.put(3808, DeviceType.GARMIN_VARIA_RCT715);
        mProductNumbers.put(3809, DeviceType.GARMIN_LILY); // ASIA
        mProductNumbers.put(3812, DeviceType.GARMIN_EDGE_1030_PLUS); // ASIA
        mProductNumbers.put(3813, DeviceType.GARMIN_EDGE_130_PLUS); // ASIA
        // mProductNumbers.put(3823, DeviceType.GARMIN_APPROACH_S12);
        mProductNumbers.put(3872, DeviceType.GARMIN_ENDURO); // ASIA
        mProductNumbers.put(3837, DeviceType.GARMIN_VENU_SQ); // ASIA
        mProductNumbers.put(3843, DeviceType.GARMIN_EDGE_1040);
        // mProductNumbers.put(3850, DeviceType.GARMIN_MARQ_GOLFER_ASIA);
        mProductNumbers.put(3851, DeviceType.GARMIN_VENU_2_PLUS);
        // mProductNumbers.put(3865, DeviceType.GARMIN_GNSS); // Airoha AG3335M Family
        mProductNumbers.put(3869, DeviceType.GARMIN_FORERUNNER_55);
        mProductNumbers.put(3888, DeviceType.GARMIN_INSTINCT_2);
        mProductNumbers.put(3889, DeviceType.GARMIN_INSTINCT_2S);
        mProductNumbers.put(3905, DeviceType.GARMIN_FENIX_7S);
        mProductNumbers.put(3906, DeviceType.GARMIN_FENIX_7);
        mProductNumbers.put(3907, DeviceType.GARMIN_FENIX_7X);
        mProductNumbers.put(3908, DeviceType.GARMIN_FENIX_7S); // APAC
        mProductNumbers.put(3909, DeviceType.GARMIN_FENIX_7); // APAC
        mProductNumbers.put(3910, DeviceType.GARMIN_FENIX_7X); // APAC
        // mProductNumbers.put(3927, DeviceType.GARMIN_APPROACH_G12);
        // mProductNumbers.put(3930, DeviceType.GARMIN_DESCENT_MK2S_ASIA);
        // mProductNumbers.put(3934, DeviceType.GARMIN_APPROACH_S42);
        mProductNumbers.put(3943, DeviceType.GARMIN_EPIX_GEN2);
        mProductNumbers.put(3944, DeviceType.GARMIN_EPIX_GEN2); // APAC
        mProductNumbers.put(3949, DeviceType.GARMIN_VENU_2S); // ASIA
        mProductNumbers.put(3950, DeviceType.GARMIN_VENU_2); // ASIA
        // mProductNumbers.put(3978, DeviceType.GARMIN_FORERUNNER_945_LTE); // ASIA
        mProductNumbers.put(3982, DeviceType.GARMIN_VIVOMOVE_SPORT);
        mProductNumbers.put(3983, DeviceType.GARMIN_VIVOMOVE_TREND);
        // mProductNumbers.put(3986, DeviceType.GARMIN_APPROACH_S12_ASIA);
        mProductNumbers.put(3990, DeviceType.GARMIN_FORERUNNER_255_MUSIC);
        mProductNumbers.put(3991, DeviceType.GARMIN_FORERUNNER_255S_MUSIC);
        mProductNumbers.put(3992, DeviceType.GARMIN_FORERUNNER_255);
        mProductNumbers.put(3993, DeviceType.GARMIN_FORERUNNER_255S);
        // mProductNumbers.put(4001, DeviceType.GARMIN_APPROACH_G12_ASIA);
        // mProductNumbers.put(4002, DeviceType.GARMIN_APPROACH_S42_ASIA);
        mProductNumbers.put(4005, DeviceType.GARMIN_DESCENT_G1);
        mProductNumbers.put(4017, DeviceType.GARMIN_VENU_2_PLUS); // ASIA
        mProductNumbers.put(4024, DeviceType.GARMIN_FORERUNNER_955);
        mProductNumbers.put(4033, DeviceType.GARMIN_FORERUNNER_55); // ASIA
        mProductNumbers.put(4061, DeviceType.GARMIN_EDGE_540);
        mProductNumbers.put(4062, DeviceType.GARMIN_EDGE_840);
        mProductNumbers.put(4063, DeviceType.GARMIN_VIVOSMART_5);
        mProductNumbers.put(4071, DeviceType.GARMIN_INSTINCT_2); // ASIA
        // mProductNumbers.put(4105, DeviceType.GARMIN_MARQ_GEN2); // Adventurer, Athlete, Captain, Golfer
        mProductNumbers.put(4115, DeviceType.GARMIN_VENU_SQ_2);
        mProductNumbers.put(4116, DeviceType.GARMIN_VENU_SQ_2_MUSIC);
        // mProductNumbers.put(4124, DeviceType.GARMIN_MARQ_GEN2_AVIATOR);
        // mProductNumbers.put(4125, DeviceType.GARMIN_D2_AIR_X10);
        mProductNumbers.put(4130, DeviceType.GARMIN_HRM_PRO_PLUS);
        mProductNumbers.put(4132, DeviceType.GARMIN_DESCENT_G1); // ASIA
        mProductNumbers.put(4135, DeviceType.GARMIN_TACTIX_7);
        mProductNumbers.put(4155, DeviceType.GARMIN_INSTINCT_CROSSOVER);
        mProductNumbers.put(4169, DeviceType.GARMIN_EDGE_EXPLORE_2);
        mProductNumbers.put(4222, DeviceType.GARMIN_DESCENT_MK3);
        mProductNumbers.put(4223, DeviceType.GARMIN_DESCENT_MK3I);
        // mProductNumbers.put(4233, DeviceType.GARMIN_APPROACH_S70);
        mProductNumbers.put(4257, DeviceType.GARMIN_FORERUNNER_265); // LARGE
        mProductNumbers.put(4258, DeviceType.GARMIN_FORERUNNER_265S);
        mProductNumbers.put(4260, DeviceType.GARMIN_VENU_3);
        mProductNumbers.put(4261, DeviceType.GARMIN_VENU_3S);
        // mProductNumbers.put(4265, DeviceType.GARMIN_TACX_NEO_SMART); // Neo Smart, Tacx
        // mProductNumbers.put(4266, DeviceType.GARMIN_TACX_NEO2_SMART); // Neo 2 Smart, Tacx
        // mProductNumbers.put(4267, DeviceType.GARMIN_TACX_NEO2T_SMART); // Neo 2T Smart, Tacx
        // mProductNumbers.put(4268, DeviceType.GARMIN_TACX_NEO_SMART_BIKE); // Neo Smart Bike, Tacx
        // mProductNumbers.put(4269, DeviceType.GARMIN_TACX_SATORI_SMART); // Satori Smart, Tacx
        // mProductNumbers.put(4270, DeviceType.GARMIN_TACX_FLOW_SMART); // Flow Smart, Tacx
        // mProductNumbers.put(4271, DeviceType.GARMIN_TACX_VORTEX_SMART); // Vortex Smart, Tacx
        // mProductNumbers.put(4272, DeviceType.GARMIN_TACX_BUSHIDO_SMART); // Bushido Smart, Tacx
        // mProductNumbers.put(4273, DeviceType.GARMIN_TACX_GENIUS_SMART); // Genius Smart, Tacx
        // mProductNumbers.put(4274, DeviceType.GARMIN_TACX_FLUX_FLUX_S_SMART); // Flux/Flux S Smart, Tacx
        // mProductNumbers.put(4275, DeviceType.GARMIN_TACX_FLUX2_SMART); // Flux 2 Smart, Tacx
        // mProductNumbers.put(4276, DeviceType.GARMIN_TACX_MAGNUM); // Magnum, Tacx
        mProductNumbers.put(4305, DeviceType.GARMIN_EDGE_1040); // ASIA
        mProductNumbers.put(4312, DeviceType.GARMIN_EPIX_PRO); // 42
        mProductNumbers.put(4313, DeviceType.GARMIN_EPIX_PRO); // 47
        mProductNumbers.put(4314, DeviceType.GARMIN_EPIX_PRO); // 51
        mProductNumbers.put(4315, DeviceType.GARMIN_FORERUNNER_965);
        mProductNumbers.put(4341, DeviceType.GARMIN_ENDURO_2);
        mProductNumbers.put(4374, DeviceType.GARMIN_FENIX_7S_PRO_SOLAR);
        mProductNumbers.put(4375, DeviceType.GARMIN_FENIX_7_PRO_SOLAR);
        mProductNumbers.put(4376, DeviceType.GARMIN_FENIX_7X_PRO_SOLAR);
        mProductNumbers.put(4380, DeviceType.GARMIN_LILY_2);
        // mProductNumbers.put(4394, DeviceType.GARMIN_INSTINCT_2X);
        mProductNumbers.put(4426, DeviceType.GARMIN_VIVOACTIVE_5);
        mProductNumbers.put(4432, DeviceType.GARMIN_FORERUNNER_165);
        mProductNumbers.put(4433, DeviceType.GARMIN_FORERUNNER_165_MUSIC);
        mProductNumbers.put(4440, DeviceType.GARMIN_EDGE_1050);
        // mProductNumbers.put(4442, DeviceType.GARMIN_DESCENT_T2);
        // mProductNumbers.put(4446, DeviceType.GARMIN_HRM_FIT);
        // mProductNumbers.put(4472, DeviceType.GARMIN_MARQ_GEN2_COMMANDER);
        mProductNumbers.put(4477, DeviceType.GARMIN_LILY_2_ACTIVE); // LILY_ATHLETE
        // mProductNumbers.put(4525, DeviceType.GARMIN_RALLY_X10); // Rally 110/210
        mProductNumbers.put(4532, DeviceType.GARMIN_FENIX_8_SOLAR);
        mProductNumbers.put(4533, DeviceType.GARMIN_FENIX_8_SOLAR); // LARGE
        mProductNumbers.put(4534, DeviceType.GARMIN_FENIX_8); // S
        mProductNumbers.put(4536, DeviceType.GARMIN_FENIX_8);
        // mProductNumbers.put(4556, DeviceType.GARMIN_D2_MACH1_PRO);
        mProductNumbers.put(4575, DeviceType.GARMIN_ENDURO_3);
        mProductNumbers.put(4583, DeviceType.GARMIN_INSTINCT_E); // 40_MM
        mProductNumbers.put(4584, DeviceType.GARMIN_INSTINCT_E); // 45_MM
        mProductNumbers.put(4585, DeviceType.GARMIN_INSTINCT_3_SOLAR); // 45_MM
        mProductNumbers.put(4586, DeviceType.GARMIN_INSTINCT_3); // AMOLED45_MM
        mProductNumbers.put(4587, DeviceType.GARMIN_INSTINCT_3); // AMOLED50_MM
        mProductNumbers.put(4588, DeviceType.GARMIN_DESCENT_G2);
        mProductNumbers.put(4595, DeviceType.GARMIN_FENIX_7_PRO_SOLAR); // NO_WIFI
        mProductNumbers.put(4603, DeviceType.GARMIN_VENU_X1);
        mProductNumbers.put(4606, DeviceType.GARMIN_HRM_200);
        mProductNumbers.put(4625, DeviceType.GARMIN_VIVOACTIVE_6);
        mProductNumbers.put(4631, DeviceType.GARMIN_FENIX_8_PRO);
        mProductNumbers.put(4633, DeviceType.GARMIN_EDGE_550);
        mProductNumbers.put(4634, DeviceType.GARMIN_EDGE_850);
        mProductNumbers.put(4643, DeviceType.GARMIN_VENU_4);
        mProductNumbers.put(4644, DeviceType.GARMIN_VENU_4); // 4S
        // mProductNumbers.put(4647, DeviceType.GARMIN_APPROACHS44);
        mProductNumbers.put(4655, DeviceType.GARMIN_EDGE_MTB);
        // mProductNumbers.put(4656, DeviceType.GARMIN_APPROACHS50);
        mProductNumbers.put(4666, DeviceType.GARMIN_FENIX_E);
        // mProductNumbers.put(4745, DeviceType.GARMIN_BOUNCE2);
        mProductNumbers.put(4759, DeviceType.GARMIN_INSTINCT_3_SOLAR); // 50_MM
        mProductNumbers.put(4775, DeviceType.GARMIN_TACTIX_8); // AMOLED
        mProductNumbers.put(4776, DeviceType.GARMIN_TACTIX_8_SOLAR);
        mProductNumbers.put(4814, DeviceType.GARMIN_FORERUNNER_170_MUSIC);
        mProductNumbers.put(4815, DeviceType.GARMIN_FORERUNNER_170);
        // mProductNumbers.put(4825, DeviceType.GARMIN_APPROACH_J1);
        // mProductNumbers.put(4879, DeviceType.GARMIN_D2_MACH2);
        // mProductNumbers.put(4916, DeviceType.GARMIN_FORERUNNER_702026);
        mProductNumbers.put(4678, DeviceType.GARMIN_INSTINCT_CROSSOVER); // AMOLED
        // mProductNumbers.put(4944, DeviceType.GARMIN_D2_AIR_X15);
        mProductNumbers.put(5019, DeviceType.GARMIN_CIRQA);
        // mProductNumbers.put(5056, DeviceType.GARMIN_D2_MACH2_PRO);
        // mProductNumbers.put(10007, DeviceType.GARMIN_SDM4); // SDM4 footpod
        // mProductNumbers.put(10014, DeviceType.GARMIN_EDGE_REMOTE);
        // mProductNumbers.put(20533, DeviceType.GARMIN_TACX_TRAINING_APP_WIN);
        // mProductNumbers.put(20534, DeviceType.GARMIN_TACX_TRAINING_APP_MAC);
        // mProductNumbers.put(20565, DeviceType.GARMIN_TACX_TRAINING_APP_MAC_CATALYST);
        // mProductNumbers.put(20119, DeviceType.GARMIN_TRAINING_CENTER);
        // mProductNumbers.put(30045, DeviceType.GARMIN_TACX_TRAINING_APP_ANDROID);
        // mProductNumbers.put(30046, DeviceType.GARMIN_TACX_TRAINING_APP_IOS);
        // mProductNumbers.put(30047, DeviceType.GARMIN_TACX_TRAINING_APP_LEGACY);
        // mProductNumbers.put(65531, DeviceType.GARMIN_CONNECTIQ_SIMULATOR);
        // mProductNumbers.put(65532, DeviceType.GARMIN_ANDROID_ANTPLUS_PLUGIN);
        // mProductNumbers.put(65534, DeviceType.GARMIN_CONNECT); // Garmin Connect website
        // mProductNumbers.put(0xFFFF, DeviceType.GARMIN_INVALID);
    }
}
