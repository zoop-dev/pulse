package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import androidx.annotation.Nullable;

public enum WeatherReport {
    current(0),
    hourly_forecast(1),
    daily_forecast(2),
    ;

    public final int id;

    WeatherReport(final int i) {
        id = i;
    }
}
