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
    private static final int ID_IMAGE_ERROR = 14;
    private int MESSAGE_KEY_WEATHER_TEMPERATURE;
    private int MESSAGE_KEY_WEATHER_ICON;
    private int MESSAGE_KEY_WEATHER_TEMP_RANGE;

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

    private int getIconForConditionCode(int code) {
        // from javascript
        if (code >= 200 && code <= 232) return 0;
        if (code >= 300 && code <= 531) return 11;
        if (code >= 600 && code <= 622) return 13;
        if (code >= 700 && code <= 781) return 20;
        if (code >= 801 && code <= 804) return 26;
        if (code == 800) return 36;
        return ID_IMAGE_ERROR;
    }

    private byte[] encodeTrekV3ReworkedWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }


        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_ICON, getIconForConditionCode(weatherSpec.getCurrentConditionCode())));
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
