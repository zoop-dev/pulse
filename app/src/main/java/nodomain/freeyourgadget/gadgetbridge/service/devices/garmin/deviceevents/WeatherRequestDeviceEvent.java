package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class WeatherRequestDeviceEvent extends GBDeviceEvent {
    private final int format;
    private final int latitude;
    private final int longitude;
    private final int hoursOfForecast;

    public WeatherRequestDeviceEvent(int format, int latitude, int longitude, int hoursOfForecast) {
        this.format = format;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hoursOfForecast = hoursOfForecast;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        // Handled in support class
    }
}
