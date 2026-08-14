package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum WeatherAqi {
    GOOD(0),
    MODERATE(1),
    UNHEALTHY_SENSITIVE(2),
    UNHEALTHY(3),
    VERY_UNHEALTHY(4),
    HAZARDOUS(5),
    ;
    public final int id;

    WeatherAqi(final int i) {
        id = i;
    }
}
