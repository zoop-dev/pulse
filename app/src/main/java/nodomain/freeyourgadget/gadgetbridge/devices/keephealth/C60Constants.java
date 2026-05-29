package nodomain.freeyourgadget.gadgetbridge.devices.keephealth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class C60Constants {
    public static final UUID SERVICE = UUID.fromString("000ff00-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_WRITE = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_READ = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    public static final int CHECKSUM_CODE = 86;

//    UNUSED might be used later
//    public static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
//    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
//    public static final UUID CFG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//    public static final UUID OTA_CHARACTERISTIC_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12");
//    public static final UUID OTA_SERVICE_UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1912");
//    public static final UUID VERSION_CHARACTERISTIC_UUID = UUID.fromString("0000ffd4-0000-1000-8000-00805f9b34fb");
//    public static final UUID VERSION_SERVICE_UUID = UUID.fromString("0000d0ff-3c17-d293-8e48-14fe2e4da212");
//
//    public static final UUID READ_ECG = UUID.fromString("0000ef01-0000-1000-8000-00805f9b34fb");
//    public static final UUID READ_FFD2 = UUID.fromString("0000ffd2-0000-1000-8000-00805f9b34fb");
//    public static final UUID SERVICE_ACTIVE_UPLOAD = UUID.fromString("0000fc00-0000-1000-8000-00805f9b34fb");
//    public static final UUID SERVICE_ACTIVE_UPLOAD_READ = UUID.fromString("0000fc01-0000-1000-8000-00805f9b34fb");
//    public static final UUID SERVICE_ECG = UUID.fromString("0000ef00-0000-1000-8000-00805f9b34fb");
//    public static final UUID SERVICE_FFD2 = UUID.fromString("0000ffd0-0000-1000-8000-00805f9b34fb");
//    public static final UUID SERVICE_PAIR = UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb");
//    public static final UUID WRITE_ECG = UUID.fromString("0000ef02-0000-1000-8000-00805f9b34fb");
//    public static final UUID WRITE_FFD2 = UUID.fromString("0000ffd1-0000-1000-8000-00805f9b34fb");

    public static final Map<String, Integer> LANGUAGES = new HashMap<>() {{
        put("en_US", 0);
        put("zh_CN", 1);
        put("ru_RU", 2);
        put("fr_FR", 4);
        put("es_ES", 5);
        put("de_DE", 7);
        put("ja_JP", 8);
        put("pl_PL", 9);
        put("it_IT", 10);
        put("zh_TW", 12);
        put("nl_NL", 15);
    }};

    // UNUSED might be used later
//    public static final Map<String, Integer> SPORT_MODES = new HashMap<String, Integer>() {{
//        put("walk", 0);
//        put("run", 1);
//        put("by_bike", 3);
//        put("run_in_door", 4);
//        put("train", 5);
//        put("football", 6);
//        put("basketball", 7);
//        put("badminton", 8);
//        put("rope_skipping", 9);
//        put("push_up", 10);
//        put("sit_up", 11);
//        put("mountain_climbing", 12);
//        put("tennis", 13);
//        put("high_intensity", 14);
//        put("indoor_cycling", 15);
//        put("fitness", 16);
//        put("rugby", 17);
//        put("golf", 18);
//        put("dynamic_bicycle", 19);
//        put("weightlifting", 20);
//        put("roller", 21);
//        put("dancing", 22);
//        put("yoga", 23);
//        put("indoor_walk", 24);
//        put("on_foot", 25);
//    }};

}
