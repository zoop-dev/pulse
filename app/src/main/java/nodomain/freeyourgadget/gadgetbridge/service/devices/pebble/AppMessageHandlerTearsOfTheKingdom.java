/*  Copyright (C) 2017-2025 Andreas Shimokawa, Daniele Gobbetti, Avery Sterk

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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTearsOfTheKingdom extends AppMessageHandler {
    private int TEMPERATURE_KEY;
    private int CONDITIONS_KEY;

    // weather icon numbers
    private static final int ICON_CLEAR_DAY = 0;
    private static final int ICON_PARTLY_CLOUDY_DAY = 1;
    private static final int ICON_CLOUDY = 2;
    private static final int ICON_RAINY = 3;
    private static final int ICON_SNOWY = 4;
    private static final int ICON_STORMY = 5;
    private static final int ICON_CLEAR_NIGHT = 6;
    private static final int ICON_PARTY_CLOUDY_NIGHT = 7;


    AppMessageHandlerTearsOfTheKingdom(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            TEMPERATURE_KEY = appKeys.getInt("TEMPERATURE");
            CONDITIONS_KEY = appKeys.getInt("CONDITIONS");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the Tears of the Kingdom watchface configuration.", Toast.LENGTH_LONG, GB.ERROR, e);
        } catch (IOException ignore) {
        }
    }


    private int getIconFromConditionCode(int conditionCode, boolean isNight) {
        if (conditionCode >= 803 ) return ICON_CLOUDY;
        if (conditionCode >= 801 ) return (isNight ? ICON_PARTY_CLOUDY_NIGHT : ICON_PARTLY_CLOUDY_DAY);
        if (conditionCode == 800 ) return (isNight ? ICON_CLEAR_NIGHT : ICON_CLEAR_DAY);
        if (conditionCode >= 700 ) return ICON_CLOUDY;
        if (conditionCode >= 600 ) return ICON_SNOWY;
        if (conditionCode == 511 ) return ICON_SNOWY; // freezing rain
        if (conditionCode >= 300) return ICON_RAINY;
        if (conditionCode >= 200) return ICON_STORMY;
        return ICON_CLOUDY;
    }

    private byte[] encodeTotkWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        int weather_time = weatherSpec.getTimestamp();
        // approximate night as the time as any time before sunrise or after sunset
        boolean isNight = ( weather_time < weatherSpec.getSunRise() ) || ( weather_time > weatherSpec.getSunSet()) ;

        int temperature_f = (weatherSpec.getCurrentTemp() - 255) * 9 / 5; // 255K is 0 degF
        int condition = getIconFromConditionCode(weatherSpec.getCurrentConditionCode(), isNight);

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(TEMPERATURE_KEY, (Object) temperature_f));
        pairs.add(new Pair<>(CONDITIONS_KEY, (Object) condition));
         byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);

        ByteBuffer buf = ByteBuffer.allocate(weatherMessage.length);

        buf.put(weatherMessage);

        return buf.array();
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTotkWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTotkWeatherMessage(weatherSpec);
    }
}
