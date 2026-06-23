/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;

/**
 * Pulse: fetches weather from Open-Meteo (free, no API key) and pushes it to the watch.
 * Gadgetbridge ships without a weather source, so this fills the "waiting for data" gap.
 */
public final class PulseWeather {
    private static final Logger LOG = LoggerFactory.getLogger(PulseWeather.class);
    private static volatile long lastFetch = 0;
    private static final long THROTTLE_MS = 15 * 60 * 1000;

    private PulseWeather() {
    }

    /** Fetch + send if we haven't recently. Runs on a background thread. */
    public static void maybeFetch(final Context context) {
        if (System.currentTimeMillis() - lastFetch < THROTTLE_MS) {
            return;
        }
        final Context app = context.getApplicationContext();
        new Thread(() -> fetchAndSend(app), "pulse-weather").start();
    }

    public static void fetchAndSend(final Context context) {
        try {
            final Location loc = lastKnownLocation(context);
            if (loc == null) {
                LOG.warn("Pulse weather: no location available");
                return;
            }
            final double lat = loc.getLatitude();
            final double lon = loc.getLongitude();
            final String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,"
                            + "wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,dew_point_2m"
                            + "&hourly=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m,"
                            + "wind_direction_10m,precipitation_probability,uv_index,dew_point_2m,visibility"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,"
                            + "uv_index_max,precipitation_probability_max"
                            + "&timezone=auto&timeformat=unixtime&wind_speed_unit=ms&forecast_days=7",
                    lat, lon);

            final String body = httpGet(url);
            if (body == null) {
                return;
            }
            final JSONObject root = new JSONObject(body);
            final WeatherSpec spec = buildSpec(context, root, lat, lon);
            final List<WeatherSpec> list = new ArrayList<>();
            list.add(spec);
            Weather.setWeatherSpec(list);
            GBApplication.deviceService().onSendWeather();
            lastFetch = System.currentTimeMillis();
            LOG.info("Pulse weather sent: {} {}K", spec.getLocation(), spec.getCurrentTemp());
        } catch (final Exception e) {
            LOG.error("Pulse weather fetch failed", e);
        }
    }

    private static WeatherSpec buildSpec(final Context context, final JSONObject root,
                                         final double lat, final double lon) throws Exception {
        final WeatherSpec spec = new WeatherSpec();
        spec.setTimestamp((int) (System.currentTimeMillis() / 1000));
        spec.setLatitude((float) lat);
        spec.setLongitude((float) lon);
        spec.setIsCurrentLocation(1);
        spec.setLocation(resolveLocationName(context, lat, lon));

        final JSONObject cur = root.getJSONObject("current");
        spec.setCurrentTemp(toKelvin(cur.getDouble("temperature_2m")));
        spec.setFeelsLikeTemp(toKelvin(cur.optDouble("apparent_temperature", cur.getDouble("temperature_2m"))));
        spec.setCurrentHumidity(cur.optInt("relative_humidity_2m", 0));
        spec.setWindSpeed((float) cur.optDouble("wind_speed_10m", 0));
        spec.setWindDirection(cur.optInt("wind_direction_10m", 0));
        spec.setPressure((float) cur.optDouble("surface_pressure", 0));
        spec.setCloudCover(cur.optInt("cloud_cover", 0));
        spec.setDewPoint(toKelvin(cur.optDouble("dew_point_2m", cur.getDouble("temperature_2m"))));
        final int wmo = cur.optInt("weather_code", 0);
        spec.setCurrentConditionCode(wmoToOwm(wmo));
        spec.setCurrentCondition(conditionText(wmo));

        final JSONObject daily = root.getJSONObject("daily");
        final JSONArray dCode = daily.getJSONArray("weather_code");
        final JSONArray dMax = daily.getJSONArray("temperature_2m_max");
        final JSONArray dMin = daily.getJSONArray("temperature_2m_min");
        final JSONArray dRise = daily.optJSONArray("sunrise");
        final JSONArray dSet = daily.optJSONArray("sunset");
        final JSONArray dUv = daily.optJSONArray("uv_index_max");
        final JSONArray dPop = daily.optJSONArray("precipitation_probability_max");

        if (dMax.length() > 0) {
            spec.setTodayMaxTemp(toKelvin(dMax.getDouble(0)));
            spec.setTodayMinTemp(toKelvin(dMin.getDouble(0)));
            if (dRise != null && dRise.length() > 0) spec.setSunRise(dRise.getInt(0));
            if (dSet != null && dSet.length() > 0) spec.setSunSet(dSet.getInt(0));
            if (dUv != null && dUv.length() > 0) spec.setUvIndex((float) dUv.optDouble(0, 0));
            if (dPop != null && dPop.length() > 0) spec.setPrecipProbability(dPop.optInt(0, 0));
        }

        final ArrayList<WeatherSpec.Daily> forecasts = new ArrayList<>();
        for (int i = 1; i < dCode.length(); i++) {
            final WeatherSpec.Daily f = new WeatherSpec.Daily();
            f.setConditionCode(wmoToOwm(dCode.getInt(i)));
            f.setMaxTemp(toKelvin(dMax.getDouble(i)));
            f.setMinTemp(toKelvin(dMin.getDouble(i)));
            if (dRise != null && dRise.length() > i) f.setSunRise(dRise.getInt(i));
            if (dSet != null && dSet.length() > i) f.setSunSet(dSet.getInt(i));
            if (dUv != null && dUv.length() > i) f.setUvIndex((float) dUv.optDouble(i, 0));
            if (dPop != null && dPop.length() > i) f.setPrecipProbability(dPop.optInt(i, 0));
            forecasts.add(f);
        }
        spec.setForecasts(forecasts);

        // Hourly forecasts (next 24h) — Garmin's weather widget needs these to populate.
        final JSONObject hourly = root.optJSONObject("hourly");
        if (hourly != null) {
            final JSONArray hTime = hourly.getJSONArray("time");
            final JSONArray hTemp = hourly.getJSONArray("temperature_2m");
            final JSONArray hCode = hourly.getJSONArray("weather_code");
            final JSONArray hHum = hourly.optJSONArray("relative_humidity_2m");
            final JSONArray hWind = hourly.optJSONArray("wind_speed_10m");
            final JSONArray hWdir = hourly.optJSONArray("wind_direction_10m");
            final JSONArray hPop = hourly.optJSONArray("precipitation_probability");
            final JSONArray hUv = hourly.optJSONArray("uv_index");
            final JSONArray hVis = hourly.optJSONArray("visibility");
            final long nowSec = System.currentTimeMillis() / 1000;
            final ArrayList<WeatherSpec.Hourly> hourlyList = new ArrayList<>();
            int start = 0;
            for (int i = 0; i < hTime.length(); i++) {
                if (hTime.getLong(i) >= nowSec - 3600) { start = i; break; }
            }
            // Current visibility comes from the hourly series (Open-Meteo has no "current" visibility)
            if (hVis != null && hVis.length() > start) {
                spec.setVisibility((float) hVis.optDouble(start, 10000));
            }
            for (int i = start; i < hTime.length() && hourlyList.size() < 24; i++) {
                final WeatherSpec.Hourly h = new WeatherSpec.Hourly();
                h.setTimestamp((int) hTime.getLong(i));
                h.setTemp(toKelvin(hTemp.getDouble(i)));
                h.setConditionCode(wmoToOwm(hCode.getInt(i)));
                if (hHum != null) h.setHumidity(hHum.optInt(i, 0));
                if (hWind != null) h.setWindSpeed((float) hWind.optDouble(i, 0));
                if (hWdir != null) h.setWindDirection(hWdir.optInt(i, 0));
                if (hPop != null) h.setPrecipProbability(hPop.optInt(i, 0));
                if (hUv != null) h.setUvIndex((float) hUv.optDouble(i, 0));
                hourlyList.add(h);
            }
            spec.setHourly(hourlyList);
        }

        return spec;
    }

    private static int toKelvin(final double celsius) {
        return (int) Math.round(celsius + 273.15);
    }

    private static String resolveLocationName(final Context context, final double lat, final double lon) {
        try {
            final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            final List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                final Address a = addresses.get(0);
                if (a.getLocality() != null) return a.getLocality();
                if (a.getSubAdminArea() != null) return a.getSubAdminArea();
                if (a.getAdminArea() != null) return a.getAdminArea();
            }
        } catch (final Exception e) {
            LOG.debug("Geocoder failed", e);
        }
        return "";
    }

    private static Location lastKnownLocation(final Context context) {
        // Manual location override (set via the Weather menu)
        final Prefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean("pulse_weather_auto", true)) {
            final String latS = prefs.getString("pulse_weather_lat", "");
            final String lonS = prefs.getString("pulse_weather_lon", "");
            if (!latS.isEmpty() && !lonS.isEmpty()) {
                try {
                    final Location l = new Location("pulse");
                    l.setLatitude(Double.parseDouble(latS));
                    l.setLongitude(Double.parseDouble(lonS));
                    return l;
                } catch (final NumberFormatException ignored) {
                }
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return null;
        }
        Location best = null;
        for (final String provider : lm.getAllProviders()) {
            try {
                final Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            } catch (final SecurityException ignored) {
            }
        }
        return best;
    }

    private static String httpGet(final String urlString) {
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Pulse/1.0");
            if (conn.getResponseCode() != 200) {
                LOG.warn("Pulse weather HTTP {}", conn.getResponseCode());
                return null;
            }
            final StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (final Exception e) {
            LOG.error("Pulse weather HTTP failed", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** Map WMO weather codes (Open-Meteo) to OpenWeatherMap condition codes (Gadgetbridge). */
    private static int wmoToOwm(final int wmo) {
        switch (wmo) {
            case 0: return 800;       // clear
            case 1: return 801;       // mainly clear
            case 2: return 802;       // partly cloudy
            case 3: return 804;       // overcast
            case 45: case 48: return 741; // fog
            case 51: return 300; case 53: return 301; case 55: return 302; // drizzle
            case 56: case 57: return 511; // freezing drizzle
            case 61: return 500; case 63: return 501; case 65: return 502; // rain
            case 66: case 67: return 511; // freezing rain
            case 71: return 600; case 73: return 601; case 75: return 602; // snow
            case 77: return 611;      // snow grains
            case 80: return 520; case 81: return 521; case 82: return 522; // rain showers
            case 85: return 620; case 86: return 621; // snow showers
            case 95: return 211;      // thunderstorm
            case 96: case 99: return 212; // thunderstorm with hail
            default: return 800;
        }
    }

    private static String conditionText(final int wmo) {
        switch (wmo) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: case 63: case 65: return "Rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: return "Snow";
            case 77: return "Snow grains";
            case 80: case 81: case 82: return "Rain showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Thunderstorm with hail";
            default: return "";
        }
    }
}
