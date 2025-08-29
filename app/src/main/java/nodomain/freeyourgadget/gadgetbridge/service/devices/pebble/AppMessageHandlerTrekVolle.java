/*  Copyright (C) 2017-2024 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTrekVolle extends AppMessageHandler {
    private int MESSAGE_KEY_WEATHER_TEMPERATURE;
    private int MESSAGE_KEY_WEATHER_CONDITIONS;
    private int MESSAGE_KEY_WEATHER_ICON;
    private int MESSAGE_KEY_WEATHER_TEMPERATURE_MIN;
    private int MESSAGE_KEY_WEATHER_TEMPERATURE_MAX;
    private int MESSAGE_KEY_WEATHER_LOCATION;

    private static final int ID_IMAGE_ERROR = 0;
    private static final int ID_WEATHER_CLEARNIGHT = 1;
    private static final int ID_WEATHER_CLEAR = 2;
    private static final int ID_WEATHER_CLOUDYNIGHT = 3;
    private static final int ID_WEATHER_CLOUDY = 4;
    private static final int ID_WEATHER_CLOUDS = 5;
    private static final int ID_WEATHER_THICKCLOUDS = 6;
    private static final int ID_WEATHER_RAIN = 7;
    private static final int ID_WEATHER_RAINYNIGHT = 8;
    private static final int ID_WEATHER_RAINY = 9;
    private static final int ID_WEATHER_LIGHTNING = 10;
    private static final int ID_WEATHER_SNOW = 11;
    private static final int ID_WEATHER_MIST = 12;

    AppMessageHandlerTrekVolle(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            MESSAGE_KEY_WEATHER_TEMPERATURE = appKeys.getInt("WEATHER_TEMPERATURE");
            MESSAGE_KEY_WEATHER_CONDITIONS = appKeys.getInt("WEATHER_CONDITIONS");
            MESSAGE_KEY_WEATHER_ICON = appKeys.getInt("WEATHER_ICON");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MIN = appKeys.getInt("WEATHER_TEMPERATURE_MIN");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MAX = appKeys.getInt("WEATHER_TEMPERATURE_MAX");
            MESSAGE_KEY_WEATHER_LOCATION = appKeys.getInt("WEATHER_LOCATION");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the TrekVolle watchface configuration.", Toast.LENGTH_LONG, GB.ERROR, e);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? ID_WEATHER_CLEARNIGHT : ID_WEATHER_CLEAR;
        } else if (conditionCode == 801 || conditionCode == 802) {
            return isNight ? ID_WEATHER_CLOUDYNIGHT : ID_WEATHER_CLOUDY;
        } else if (conditionCode >= 300 && conditionCode < 313) {
            return ID_WEATHER_RAIN;
        } else if (conditionCode >= 313 && conditionCode < 400) {
            return ID_WEATHER_RAIN;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return ID_WEATHER_RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return ID_WEATHER_CLOUDY;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762 ) {
            return ID_WEATHER_MIST;
        //} else if (conditionCode == 771) {
        //    return WINDY;
        } else if (conditionCode == 781) {
            return ID_WEATHER_LIGHTNING;
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return ID_WEATHER_LIGHTNING;
        } else if (conditionCode == 600 || conditionCode == 601 || conditionCode == 602 ) {
            return ID_WEATHER_SNOW;
        //} else if (conditionCode == 611 || conditionCode == 612) {
        //    return HAIL;
        } else if (conditionCode == 615 || conditionCode == 616 || conditionCode == 620 || conditionCode == 621 || conditionCode == 622) {
            return ID_WEATHER_SNOW;
        } else if (conditionCode == 906) {
            return ID_WEATHER_SNOW;
        } else if (conditionCode == 803) {
            return ID_WEATHER_CLOUDS;
        } else if (conditionCode == 804) {
            return ID_WEATHER_THICKCLOUDS;
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return ID_WEATHER_LIGHTNING;
        } else if (conditionCode == 905) {
            return ID_WEATHER_LIGHTNING;
        } else if (conditionCode == 900) {
            return ID_WEATHER_LIGHTNING;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return ID_WEATHER_LIGHTNING;
        //} else if (conditionCode == 903) {
        //    return COLD;
        }

        return ID_IMAGE_ERROR;
    }

    private byte[] encodeTrekVolleWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        boolean isNight = false; // FIXME
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE, weatherSpec.getCurrentTemp() - 273));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_CONDITIONS, weatherSpec.getCurrentCondition()));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_ICON, getIconForConditionCode(weatherSpec.getCurrentConditionCode(), isNight)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MAX, weatherSpec.getTodayMaxTemp() - 273));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MIN, weatherSpec.getTodayMinTemp() - 273));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_LOCATION, weatherSpec.getLocation()));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTrekVolleWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTrekVolleWeather(weatherSpec);
    }
}
