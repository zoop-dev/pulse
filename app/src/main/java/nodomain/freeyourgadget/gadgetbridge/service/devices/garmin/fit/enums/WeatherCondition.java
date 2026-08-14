package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum WeatherCondition {
    CLEAR(0),
    PARTLY_CLOUDY(1),
    MOSTLY_CLOUDY(2),
    RAIN(3),
    SNOW(4),
    WINDY(5),
    THUNDERSTORMS(6),
    WINTRY_MIX(7),
    FOG(8),
    UNK9(9),
    UNK10(10),
    HAZY(11),
    HAIL(12),
    SCATTERED_SHOWERS(13),
    SCATTERED_THUNDERSTORMS(14),
    UNKNOWN_PRECIPITATION(15),
    LIGHT_RAIN(16),
    HEAVY_RAIN(17),
    LIGHT_SNOW(18),
    HEAVY_SNOW(19),
    LIGHT_RAIN_SNOW(20),
    HEAVY_RAIN_SNOW(21),
    CLOUDY(22),
    ;

    public final int id;

    WeatherCondition(final int i) {
        id = i;
    }
}
