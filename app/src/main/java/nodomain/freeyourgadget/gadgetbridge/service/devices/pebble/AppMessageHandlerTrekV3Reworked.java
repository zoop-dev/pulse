/*  Copyright (C)2026 Andreas Shimokawa

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
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTrekV3Reworked extends AppMessageHandler {
    private int MESSAGE_KEY_WEATHER_TEMPERATURE;
    private int MESSAGE_KEY_WEATHER_ICON;
    private int MESSAGE_KEY_WEATHER_TEMP_RANGE;

    private static final int ID_WEATHER_CLEAR_DAY = 0;
    private static final int ID_WEATHER_CLEAR_NIGHT = 1;
    private static final int ID_WEATHER_WINDY = 2;
    private static final int ID_WEATHER_COLD = 3;
    private static final int ID_WEATHER_PARTLY_CLOUDY_DAY = 4;
    private static final int ID_WEATHER_PARTLY_CLOUDY_NIGHT = 5;
    private static final int ID_WEATHER_HAZE = 6;
    private static final int ID_WEATHER_CLOUD = 7;
    private static final int ID_WEATHER_RAIN = 8;
    private static final int ID_WEATHER_SNOW = 9;
    private static final int ID_WEATHER_HAIL = 10;
    private static final int ID_WEATHER_CLOUDY = 11;
    private static final int ID_WEATHER_STORM = 12;
    private static final int ID_WEATHER_FOG = 13;
    private static final int ID_WEATHER_NA = 14;
    AppMessageHandlerTrekV3Reworked(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            MESSAGE_KEY_WEATHER_TEMPERATURE = appKeys.getInt("temperature");
            MESSAGE_KEY_WEATHER_ICON = appKeys.getInt("icon");
            MESSAGE_KEY_WEATHER_TEMP_RANGE = appKeys.getInt("temp_range");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the TrekV3Reworked watchface configuration.", Toast.LENGTH_LONG, GB.ERROR, e);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? ID_WEATHER_CLEAR_NIGHT : ID_WEATHER_CLEAR_DAY;
        } else if (conditionCode == 801 || conditionCode == 802) {
            return isNight ? ID_WEATHER_PARTLY_CLOUDY_NIGHT : ID_WEATHER_PARTLY_CLOUDY_DAY;
        } else if (conditionCode >= 300 && conditionCode < 313) {
            return ID_WEATHER_RAIN;
        } else if ((conditionCode >= 313 && conditionCode < 400) || conditionCode == 500) {
            return ID_WEATHER_RAIN;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return ID_WEATHER_RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return ID_WEATHER_HAZE;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762) {
            return ID_WEATHER_FOG;
        } else if (conditionCode == 771) {
            return ID_WEATHER_WINDY;
        } else if (conditionCode == 781) {
            return ID_WEATHER_STORM;
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return ID_WEATHER_STORM;
        } else if (conditionCode == 600 || conditionCode == 601 || conditionCode == 602) {
            return ID_WEATHER_SNOW;
        } else if (conditionCode == 611 || conditionCode == 612) {
            return ID_WEATHER_HAIL;
        } else if (conditionCode == 615 || conditionCode == 616 || conditionCode == 620 || conditionCode == 621 || conditionCode == 622) {
            return ID_WEATHER_SNOW;
        } else if (conditionCode == 906) {
            return ID_WEATHER_SNOW;
        } else if (conditionCode == 803) {
            return ID_WEATHER_CLOUDY;
        } else if (conditionCode == 804) {
            return ID_WEATHER_CLOUD;
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return ID_WEATHER_STORM;
        } else if (conditionCode == 905) {
            return ID_WEATHER_STORM;
        } else if (conditionCode == 900) {
            return ID_WEATHER_STORM;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return ID_WEATHER_STORM;
        } else if (conditionCode == 903) {
            return ID_WEATHER_COLD;
        }

        return ID_WEATHER_NA;
    }

    private byte[] encodeTrekV3ReworkedWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        boolean isNight = false;
        if (weatherSpec.getSunRise() != 0 && weatherSpec.getSunSet() != 0) {
            isNight = weatherSpec.getSunRise() * 1000L > System.currentTimeMillis() || weatherSpec.getSunSet() * 1000L < System.currentTimeMillis();
        }
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_ICON, getIconForConditionCode(weatherSpec.getCurrentConditionCode(), isNight)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE, (weatherSpec.getCurrentTemp() - 273) + "°"));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMP_RANGE, (weatherSpec.getTodayMinTemp() - 273) + "° " + (weatherSpec.getTodayMaxTemp() - 273) + "°"));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTrekV3ReworkedWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTrekV3ReworkedWeather(weatherSpec);
    }
}
