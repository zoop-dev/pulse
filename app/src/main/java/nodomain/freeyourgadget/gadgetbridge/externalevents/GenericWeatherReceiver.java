/*  Copyright (C) 2022-2026 Daniele Gobbetti, Enrico Brambilla, José Rebelo,
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

import androidx.annotation.NonNull;

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
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
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

            // #6186 - Avoid starting the DeviceCommunicationService if it is not yet running
            if (DeviceCommunicationService.isRunning(context)) {
                GBApplication.deviceService().onSendWeather();
            }
        } catch (final Exception e) {
            GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    private WeatherSpec weatherFromJson(final JSONObject weatherJson) throws JSONException {
        final WeatherSpec weatherSpec = new WeatherSpec();

        weatherSpec.setTimestamp(safelyGet(weatherJson, Integer.class, "timestamp", (int) (System.currentTimeMillis() / 1000)));
        weatherSpec.setLocation(safelyGet(weatherJson, String.class, "location", ""));
        weatherSpec.setCurrentTemp(getInt(weatherJson, "currentTemp", 0));
        weatherSpec.setTodayMinTemp(getInt(weatherJson, "todayMinTemp", 0));
        weatherSpec.setTodayMaxTemp(getInt(weatherJson, "todayMaxTemp", 0));
        weatherSpec.setCurrentCondition(safelyGet(weatherJson, String.class, "currentCondition", ""));
        weatherSpec.setCurrentConditionCode(safelyGet(weatherJson, Integer.class, "currentConditionCode", 0));
        weatherSpec.setCurrentHumidity(getInt(weatherJson, "currentHumidity", 0));
        weatherSpec.setWindSpeed(getFloat(weatherJson, "windSpeed", 0.0f));
        weatherSpec.setWindDirection(getInt(weatherJson, "windDirection", 0));
        weatherSpec.setUvIndex(getFloat(weatherJson, "uvIndex", 0.0f));
        weatherSpec.setPrecipProbability(getInt(weatherJson, "precipProbability", 0));
        weatherSpec.setDewPoint(getInt(weatherJson, "dewPoint", 0));
        weatherSpec.setPressure(getFloat(weatherJson, "pressure", 0.0f));
        weatherSpec.setCloudCover(getInt(weatherJson, "cloudCover", 0));
        weatherSpec.setVisibility(getFloat(weatherJson, "visibility", 0.0f));
        weatherSpec.setSunRise(safelyGet(weatherJson, Integer.class, "sunRise", 0));
        weatherSpec.setSunSet(safelyGet(weatherJson, Integer.class, "sunSet", 0));
        weatherSpec.setMoonRise(safelyGet(weatherJson, Integer.class, "moonRise", 0));
        weatherSpec.setMoonSet(safelyGet(weatherJson, Integer.class, "moonSet", 0));
        weatherSpec.setMoonPhase(safelyGet(weatherJson, Integer.class, "moonPhase", 0));
        weatherSpec.setLatitude(getFloat(weatherJson, "latitude", 0.0f));
        weatherSpec.setLongitude(getFloat(weatherJson, "longitude", 0.0f));
        weatherSpec.setFeelsLikeTemp(getInt(weatherJson, "feelsLikeTemp", 0));
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
                forecast.setHumidity(getInt(forecastJson, "humidity", 0));
                forecast.setMaxTemp(getInt(forecastJson, "maxTemp", 0));
                forecast.setMinTemp(getInt(forecastJson, "minTemp", 0));
                forecast.setWindSpeed(getFloat(forecastJson, "windSpeed", 0.0f));
                forecast.setWindDirection(getInt(forecastJson, "windDirection", 0));
                forecast.setUvIndex(getFloat(forecastJson, "uvIndex", 0.0f));
                forecast.setPrecipProbability(getInt(forecastJson, "precipProbability", 0));
                forecast.setSunRise(safelyGet(forecastJson, Integer.class, "sunRise", 0));
                forecast.setSunSet(safelyGet(forecastJson, Integer.class, "sunSet", 0));
                forecast.setMoonRise(safelyGet(forecastJson, Integer.class, "moonRise", 0));
                forecast.setMoonSet(safelyGet(forecastJson, Integer.class, "moonSet", 0));
                forecast.setMoonPhase(safelyGet(forecastJson, Integer.class, "moonPhase", 0));
                forecast.setPressure(getFloat(forecastJson, "pressure", 0.0f));
                forecast.setCloudCover(getInt(forecastJson, "cloudCover", 0));

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
                forecast.setTemp(getInt(forecastJson, "temp", 0));
                forecast.setConditionCode(safelyGet(forecastJson, Integer.class, "conditionCode", 0));
                forecast.setHumidity(getInt(forecastJson, "humidity", 0));
                forecast.setWindSpeed(getFloat(forecastJson, "windSpeed", 0.0f));
                forecast.setWindDirection(getInt(forecastJson, "windDirection", 0));
                forecast.setUvIndex(getFloat(forecastJson, "uvIndex", 0.0f));
                forecast.setPrecipProbability(getInt(forecastJson, "precipProbability", 0));
                forecast.setDewPoint(getInt(forecastJson, "dewPoint", 0));
                forecast.setPressure(getFloat(forecastJson, "pressure", 0.0f));
                forecast.setCloudCover(getInt(forecastJson, "cloudCover", 0));

                weatherSpec.getHourly().add(forecast);
            }
        }

        return weatherSpec;
    }

    private WeatherSpec.AirQuality toAirQuality(final JSONObject jsonObject) {
        final WeatherSpec.AirQuality airQuality = new WeatherSpec.AirQuality();
        airQuality.setAqi(safelyGet(jsonObject, Integer.class, "aqi", -1));
        airQuality.setCo(getFloat(jsonObject, "co", -1.0f));
        airQuality.setNo2(getFloat(jsonObject, "no2", -1.0f));
        airQuality.setO3(getFloat(jsonObject, "o3", -1.0f));
        airQuality.setPm10(getFloat(jsonObject, "pm10", -1.0f));
        airQuality.setPm25(getFloat(jsonObject, "pm25", -1.0f));
        airQuality.setSo2(getFloat(jsonObject, "so2", -1.0f));
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

    /**
     * Returns the number mapped by {@code name} as a float, or {@code defaultValue} if no such mapping
     * exists.
     */
    private static float getFloat(@NonNull final JSONObject jsonObject, @NonNull final String name, final float defaultValue) {
        try {
            final Object raw = jsonObject.opt(name);
            if (raw instanceof final Number value) {
                return value.floatValue();
            }
        } catch (final Exception ignored) {
            //
        }
        return defaultValue;
    }

    /**
     * Returns the number mapped by {@code name} as an integer, or {@code defaultValue} if no such mapping
     * exists.
     */
    private static int getInt(@NonNull final JSONObject jsonObject, @NonNull final String name, final int defaultValue) {
        try {
            final Object raw = jsonObject.opt(name);
            if (raw instanceof final Number value) {
                return value.intValue();
            }
        } catch (final Exception ignored) {
            //
        }
        return defaultValue;
    }
}