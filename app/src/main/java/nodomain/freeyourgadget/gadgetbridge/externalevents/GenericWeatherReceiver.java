/*  Copyright (C) 2022-2025 Daniele Gobbetti, Enrico Brambilla, José Rebelo,
    TylerWilliamson, Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.CompressionUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericWeatherReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GenericWeatherReceiver.class);

    public final static String ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    public final static String EXTRA_WEATHER_GZ = "WeatherGz";
    public final static String EXTRA_WEATHER_JSON = "WeatherJson";
    public final static String EXTRA_WEATHER_SECONDARY_JSON = "WeatherSecondaryJson";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) {
            LOG.warn("Intent is null");
            return;
        }

        if (!ACTION_GENERIC_WEATHER.equals(intent.getAction())) {
            LOG.warn("Unknown action {}", intent.getAction());
            return;
        }

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            LOG.warn("Intent has no extras");
            return;
        }

        try {
            final ArrayList<WeatherSpec> weathers = new ArrayList<>();

            if (bundle.containsKey(EXTRA_WEATHER_GZ)) {
                LOG.debug("use extra {}", EXTRA_WEATHER_GZ);
                byte[] compressed = bundle.getByteArray(EXTRA_WEATHER_GZ);
                String json = CompressionUtils.INSTANCE.gunzipUtf8String(compressed);

                if (json != null && json.length() > 1){
                    JSONArray weather = new JSONArray(json);
                    for (int i = 0; i < weather.length(); i++) {
                        weathers.add(weatherFromJson(weather.getJSONObject(i)));
                    }
                }
            } else {
                LOG.debug("use extra {}", EXTRA_WEATHER_JSON);
                if (!bundle.containsKey(EXTRA_WEATHER_JSON)) {
                    LOG.warn("Bundle key {} not found", EXTRA_WEATHER_JSON);
                    return;
                }

                final JSONObject primaryWeatherJson = new JSONObject(Objects.requireNonNull(bundle.getString(EXTRA_WEATHER_JSON)));
                final WeatherSpec primaryWeather = weatherFromJson(primaryWeatherJson);

                weathers.add(primaryWeather);

                if (bundle.containsKey(EXTRA_WEATHER_SECONDARY_JSON)) {
                    final JSONArray secondaryWeatherJson = new JSONArray(bundle.getString(EXTRA_WEATHER_SECONDARY_JSON, "[]"));

                    for (int i = 0; i < secondaryWeatherJson.length(); i++) {
                        weathers.add(weatherFromJson(secondaryWeatherJson.getJSONObject(i)));
                    }
                }
            }

            LOG.info("Got generic weather for {} locations", weathers.size());

            // try to avoid TransactionTooLargeException in DeviceService
            bundle.clear();
            intent.replaceExtras((Bundle)null);

            Weather.setWeatherSpec(weathers);
            GBApplication.deviceService().onSendWeather();
        } catch (final Exception e) {
            GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    private WeatherSpec weatherFromJson(final JSONObject weatherJson) throws JSONException {
        final WeatherSpec weatherSpec = new WeatherSpec();

        weatherSpec.setTimestamp(safelyGet(weatherJson, Integer.class, "timestamp", (int) (System.currentTimeMillis() / 1000)));
        weatherSpec.setLocation(safelyGet(weatherJson, String.class, "location", ""));
        weatherSpec.setCurrentTemp(safelyGet(weatherJson, Integer.class, "currentTemp", 0));
        weatherSpec.setTodayMinTemp(safelyGet(weatherJson, Integer.class, "todayMinTemp", 0));
        weatherSpec.setTodayMaxTemp(safelyGet(weatherJson, Integer.class, "todayMaxTemp", 0));
        weatherSpec.setCurrentCondition(safelyGet(weatherJson, String.class, "currentCondition", ""));
        weatherSpec.setCurrentConditionCode(safelyGet(weatherJson, Integer.class, "currentConditionCode", 0));
        weatherSpec.setCurrentHumidity(safelyGet(weatherJson, Integer.class, "currentHumidity", 0));
        weatherSpec.setWindSpeed(safelyGet(weatherJson, Number.class, "windSpeed", 0d).floatValue());
        weatherSpec.setWindDirection(safelyGet(weatherJson, Integer.class, "windDirection", 0));
        weatherSpec.setUvIndex(safelyGet(weatherJson, Number.class, "uvIndex", 0d).floatValue());
        weatherSpec.setPrecipProbability(safelyGet(weatherJson, Integer.class, "precipProbability", 0));
        weatherSpec.setDewPoint(safelyGet(weatherJson, Integer.class, "dewPoint", 0));
        weatherSpec.setPressure(safelyGet(weatherJson, Number.class, "pressure", 0).floatValue());
        weatherSpec.setCloudCover(safelyGet(weatherJson, Integer.class, "cloudCover", 0));
        weatherSpec.setVisibility(safelyGet(weatherJson, Number.class, "visibility", 0).floatValue());
        weatherSpec.setSunRise(safelyGet(weatherJson, Integer.class, "sunRise", 0));
        weatherSpec.setSunSet(safelyGet(weatherJson, Integer.class, "sunSet", 0));
        weatherSpec.setMoonRise(safelyGet(weatherJson, Integer.class, "moonRise", 0));
        weatherSpec.setMoonSet(safelyGet(weatherJson, Integer.class, "moonSet", 0));
        weatherSpec.setMoonPhase(safelyGet(weatherJson, Integer.class, "moonPhase", 0));
        weatherSpec.setLatitude(safelyGet(weatherJson, Number.class, "latitude", 0).floatValue());
        weatherSpec.setLongitude(safelyGet(weatherJson, Number.class, "longitude", 0).floatValue());
        weatherSpec.setFeelsLikeTemp(safelyGet(weatherJson, Integer.class, "feelsLikeTemp", 0));
        weatherSpec.setIsCurrentLocation(safelyGet(weatherJson, Integer.class, "isCurrentLocation", -1));

        if (weatherJson.has("airQuality")) {
            weatherSpec.setAirQuality(toAirQuality(weatherJson.getJSONObject("airQuality")));
        }

        if (weatherJson.has("forecasts")) {
            final JSONArray forecastArray = weatherJson.getJSONArray("forecasts");
            weatherSpec.setForecasts(new ArrayList<>());

            for (int i = 0, l = forecastArray.length(); i < l; i++) {
                final JSONObject forecastJson = forecastArray.getJSONObject(i);

                final WeatherSpec.Daily forecast = new WeatherSpec.Daily();

                forecast.setConditionCode(safelyGet(forecastJson, Integer.class, "conditionCode", 0));
                forecast.setHumidity(safelyGet(forecastJson, Integer.class, "humidity", 0));
                forecast.setMaxTemp(safelyGet(forecastJson, Integer.class, "maxTemp", 0));
                forecast.setMinTemp(safelyGet(forecastJson, Integer.class, "minTemp", 0));
                forecast.setWindSpeed(safelyGet(forecastJson, Number.class, "windSpeed", 0).floatValue());
                forecast.setWindDirection(safelyGet(forecastJson, Integer.class, "windDirection", 0));
                forecast.setUvIndex(safelyGet(forecastJson, Number.class, "uvIndex", 0d).floatValue());
                forecast.setPrecipProbability(safelyGet(forecastJson, Integer.class, "precipProbability", 0));
                forecast.setSunRise(safelyGet(forecastJson, Integer.class, "sunRise", 0));
                forecast.setSunSet(safelyGet(forecastJson, Integer.class, "sunSet", 0));
                forecast.setMoonRise(safelyGet(forecastJson, Integer.class, "moonRise", 0));
                forecast.setMoonSet(safelyGet(forecastJson, Integer.class, "moonSet", 0));
                forecast.setMoonPhase(safelyGet(forecastJson, Integer.class, "moonPhase", 0));

                if (forecastJson.has("airQuality")) {
                    forecast.setAirQuality(toAirQuality(forecastJson.getJSONObject("airQuality")));
                }

                weatherSpec.getForecasts().add(forecast);
            }
        }

        if (weatherJson.has("hourly")) {
            final JSONArray forecastArray = weatherJson.getJSONArray("hourly");
            weatherSpec.setHourly(new ArrayList<>());

            for (int i = 0, l = forecastArray.length(); i < l; i++) {
                final JSONObject forecastJson = forecastArray.getJSONObject(i);

                final WeatherSpec.Hourly forecast = new WeatherSpec.Hourly();

                forecast.setTimestamp(safelyGet(forecastJson, Integer.class, "timestamp", 0));
                forecast.setTemp(safelyGet(forecastJson, Integer.class, "temp", 0));
                forecast.setConditionCode(safelyGet(forecastJson, Integer.class, "conditionCode", 0));
                forecast.setHumidity(safelyGet(forecastJson, Integer.class, "humidity", 0));
                forecast.setWindSpeed(safelyGet(forecastJson, Number.class, "windSpeed", 0).floatValue());
                forecast.setWindDirection(safelyGet(forecastJson, Integer.class, "windDirection", 0));
                forecast.setUvIndex(safelyGet(forecastJson, Number.class, "uvIndex", 0d).floatValue());
                forecast.setPrecipProbability(safelyGet(forecastJson, Integer.class, "precipProbability", 0));

                weatherSpec.getHourly().add(forecast);
            }
        }

        return weatherSpec;
    }

    private WeatherSpec.AirQuality toAirQuality(final JSONObject jsonObject) {
        final WeatherSpec.AirQuality airQuality = new WeatherSpec.AirQuality();
        airQuality.setAqi(safelyGet(jsonObject, Integer.class, "aqi", -1));
        airQuality.setCo(safelyGet(jsonObject, Number.class, "co", -1).floatValue());
        airQuality.setNo2(safelyGet(jsonObject, Number.class, "no2", -1).floatValue());
        airQuality.setO3(safelyGet(jsonObject, Number.class, "o3", -1).floatValue());
        airQuality.setPm10(safelyGet(jsonObject, Number.class, "pm10", -1).floatValue());
        airQuality.setPm25(safelyGet(jsonObject, Number.class, "pm25", -1).floatValue());
        airQuality.setSo2(safelyGet(jsonObject, Number.class, "so2", -1).floatValue());
        airQuality.setCoAqi(safelyGet(jsonObject, Integer.class, "coAqi", -1));
        airQuality.setNo2Aqi(safelyGet(jsonObject, Integer.class, "no2Aqi", -1));
        airQuality.setO3Aqi(safelyGet(jsonObject, Integer.class, "o3Aqi", -1));
        airQuality.setPm10Aqi(safelyGet(jsonObject, Integer.class, "pm10Aqi", -1));
        airQuality.setPm25Aqi(safelyGet(jsonObject, Integer.class, "pm25Aqi", -1));
        airQuality.setSo2Aqi(safelyGet(jsonObject, Integer.class, "so2Aqi", -1));

        return airQuality;
    }

    private <T> T safelyGet(JSONObject jsonObject, Class<T> tClass, String name, T defaultValue) {
        try {
            if (jsonObject.has(name)) {
                Object value = jsonObject.get(name);

                if (tClass.isInstance(value)) {
                    return (T) value;
                }
            }
        } catch (Exception e) {
            //
        }
        return defaultValue;
    }
}