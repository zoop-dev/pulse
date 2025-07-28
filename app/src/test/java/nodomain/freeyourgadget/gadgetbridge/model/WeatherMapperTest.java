/*  Copyright (C) 2023 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.weather.WeatherMapper;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class WeatherMapperTest extends TestBase {
    @Test
    public void ensureConversionToKotlin() {

        for (int i = 0; i <= 3200; i++) {
            Assert.assertEquals(Weather.mapToPebbleCondition(i), WeatherMapper.mapToPebbleCondition(i));
            Assert.assertEquals(Weather.mapToYahooCondition(i), WeatherMapper.mapToYahooCondition(i));
            Assert.assertEquals(Weather.mapToOpenWeatherMapIcon(i), WeatherMapper.mapToOpenWeatherMapIcon(i));
            Assert.assertEquals(Weather.mapToOpenWeatherMapCondition(i), WeatherMapper.mapToOpenWeatherMapCondition(i));
            Assert.assertEquals(Weather.getConditionString(getContext(), i), WeatherMapper.getConditionString(getContext(), i));
            Assert.assertEquals(Weather.getAqiLevelString(getContext(), i), WeatherMapper.getAqiLevelString(getContext(), i));
            Assert.assertEquals(Weather.mapToZeTimeConditionOld(i), WeatherMapper.mapToZeTimeConditionOld(i));
            Assert.assertEquals(Weather.mapToZeTimeCondition(i), WeatherMapper.mapToZeTimeCondition(i));
            Assert.assertEquals(Weather.mapToCmfCondition(i), WeatherMapper.mapToCmfCondition(i));
            Assert.assertEquals(Weather.mapToFitProCondition(i), WeatherMapper.mapToFitProCondition(i));
        }
    }

    private class Weather {
        private static final Logger LOG = LoggerFactory.getLogger(Weather.class);

        private final ArrayList<WeatherSpec> weatherSpecs = new ArrayList<>();

        private JSONObject reconstructedOWMForecast = null;

        private File cacheFile;

        private Weather() {
            // Use getInstance
        }

        @Nullable
        public WeatherSpec getWeatherSpec() {
            if (weatherSpecs.isEmpty()) {
                return null;
            }

            return weatherSpecs.get(0);
        }

        public List<WeatherSpec> getWeatherSpecs() {
            return weatherSpecs;
        }

        public void setWeatherSpec(final Collection<WeatherSpec> newWeatherSpecs) {
            weatherSpecs.clear();
            weatherSpecs.addAll(newWeatherSpecs);
            saveToCache();
        }

        @Nullable
        public JSONObject createReconstructedOWMWeatherReply() {
            final WeatherSpec weatherSpec = getWeatherSpec();
            if (weatherSpec == null) {
                return null;
            }
            JSONObject reconstructedOWMWeather = new JSONObject();
            JSONArray weather = new JSONArray();
            JSONObject condition = new JSONObject();
            JSONObject main = new JSONObject();
            JSONObject wind = new JSONObject();

            try {
                condition.put("id", weatherSpec.getCurrentConditionCode());
                condition.put("main", weatherSpec.getCurrentCondition());
                condition.put("description", weatherSpec.getCurrentCondition());
                condition.put("icon", Weather.mapToOpenWeatherMapIcon(weatherSpec.getCurrentConditionCode()));
                weather.put(condition);


                main.put("temp", weatherSpec.getCurrentTemp());
                main.put("humidity", weatherSpec.getCurrentHumidity());
                main.put("temp_min", weatherSpec.getTodayMinTemp());
                main.put("temp_max", weatherSpec.getTodayMaxTemp());

                wind.put("speed", (weatherSpec.getWindSpeed() / 3.6f)); //meter per second
                wind.put("deg", weatherSpec.getWindDirection());

                reconstructedOWMWeather.put("weather", weather);
                reconstructedOWMWeather.put("main", main);
                reconstructedOWMWeather.put("name", weatherSpec.getLocation());
                reconstructedOWMWeather.put("wind", wind);

            } catch (JSONException e) {
                LOG.error("Error while reconstructing OWM weather reply");
                return null;
            }
            LOG.debug("Weather JSON for WEBVIEW: " + reconstructedOWMWeather.toString());
            return reconstructedOWMWeather;
        }

        public JSONObject getReconstructedOWMForecast() {
            return reconstructedOWMForecast;
        }

        public void setReconstructedOWMForecast(JSONObject reconstructedOWMForecast) {
            this.reconstructedOWMForecast = reconstructedOWMForecast;
        }

//        private static final Weather weather = new Weather();
//        public static Weather getInstance() { return weather; }

        public static byte mapToPebbleCondition(int openWeatherMapCondition) {
/* deducted values:
    0 = sun + cloud
    1 = clouds
    2 = some snow
    3 = some rain
    4 = heavy rain
    5 = heavy snow
    6 = sun + cloud + rain (default icon?)
    7 = sun
    8 = rain + snow
    9 = 6
    10, 11, ... = empty icon
 */
            switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
                case 200:  //thunderstorm with light rain:  //11d
                case 201:  //thunderstorm with rain:  //11d
                case 202:  //thunderstorm with heavy rain:  //11d
                case 210:  //light thunderstorm::  //11d
                case 211:  //thunderstorm:  //11d
                case 230:  //thunderstorm with light drizzle:  //11d
                case 231:  //thunderstorm with drizzle:  //11d
                case 232:  //thunderstorm with heavy drizzle:  //11d
                case 212:  //heavy thunderstorm:  //11d
                case 221:  //ragged thunderstorm:  //11d
                    return 4;
//Group 3xx: Drizzle
                case 300:  //light intensity drizzle:  //09d
                case 301:  //drizzle:  //09d
                case 302:  //heavy intensity drizzle:  //09d
                case 310:  //light intensity drizzle rain:  //09d
                case 311:  //drizzle rain:  //09d
                case 312:  //heavy intensity drizzle rain:  //09d
                case 313:  //shower rain and drizzle:  //09d
                case 314:  //heavy shower rain and drizzle:  //09d
                case 321:  //shower drizzle:  //09d
                case 500:  //light rain:  //10d
                case 501:  //moderate rain:  //10d
                    return 3;
//Group 5xx: Rain
                case 502:  //heavy intensity rain:  //10d
                case 503:  //very heavy rain:  //10d
                case 504:  //extreme rain:  //10d
                case 511:  //freezing rain:  //13d
                case 520:  //light intensity shower rain:  //09d
                case 521:  //shower rain:  //09d
                case 522:  //heavy intensity shower rain:  //09d
                case 531:  //ragged shower rain:  //09d
                    return 4;
//Group 6xx: Snow
                case 600:  //light snow:  //[[file:13d.png]]
                case 601:  //snow:  //[[file:13d.png]]
                case 620:  //light shower snow:  //[[file:13d.png]]
                    return 2;
                case 602:  //heavy snow:  //[[file:13d.png]]
                case 611:  //sleet:  //[[file:13d.png]]
                case 612:  //shower sleet:  //[[file:13d.png]]
                case 621:  //shower snow:  //[[file:13d.png]]
                case 622:  //heavy shower snow:  //[[file:13d.png]]
                    return 5;
                case 615:  //light rain and snow:  //[[file:13d.png]]
                case 616:  //rain and snow:  //[[file:13d.png]]
                    return 8;
//Group 7xx: Atmosphere
                case 701:  //mist:  //[[file:50d.png]]
                case 711:  //smoke:  //[[file:50d.png]]
                case 721:  //haze:  //[[file:50d.png]]
                case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                case 741:  //fog:  //[[file:50d.png]]
                case 751:  //sand:  //[[file:50d.png]]
                case 761:  //dust:  //[[file:50d.png]]
                case 762:  //volcanic ash:  //[[file:50d.png]]
                case 771:  //squalls:  //[[file:50d.png]]
                case 781:  //tornado:  //[[file:50d.png]]
                case 900:  //tornado
                    return 6;
//Group 800: Clear
                case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                    return 7;
//Group 80x: Clouds
                case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                    return 0;
//Group 90x: Extreme
                case 901:  //tropical storm
                case 903:  //cold
                case 904:  //hot
                case 905:  //windy
                case 906:  //hail
//Group 9xx: Additional
                case 951:  //calm
                case 952:  //light breeze
                case 953:  //gentle breeze
                case 954:  //moderate breeze
                case 955:  //fresh breeze
                case 956:  //strong breeze
                case 957:  //high windcase  near gale
                case 958:  //gale
                case 959:  //severe gale
                case 960:  //storm
                case 961:  //violent storm
                case 902:  //hurricane
                case 962:  //hurricane
                default:
                    return 6;

            }
        }

        public static int mapToYahooCondition(int openWeatherMapCondition) {
            // openweathermap.org conditions:
            // http://openweathermap.org/weather-conditions
            switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
                case 200:  //thunderstorm with light rain:  //11d
                case 201:  //thunderstorm with rain:  //11d
                case 202:  //thunderstorm with heavy rain:  //11d
                case 210:  //light thunderstorm::  //11d
                case 211:  //thunderstorm:  //11d
                case 230:  //thunderstorm with light drizzle:  //11d
                case 231:  //thunderstorm with drizzle:  //11d
                case 232:  //thunderstorm with heavy drizzle:  //11d
                    return 4;
                case 212:  //heavy thunderstorm:  //11d
                case 221:  //ragged thunderstorm:  //11d
                    return 3;
//Group 3xx: Drizzle
                case 300:  //light intensity drizzle:  //09d
                case 301:  //drizzle:  //09d
                case 302:  //heavy intensity drizzle:  //09d
                case 310:  //light intensity drizzle rain:  //09d
                case 311:  //drizzle rain:  //09d
                case 312:  //heavy intensity drizzle rain:  //09d
                    return 9;
                case 313:  //shower rain and drizzle:  //09d
                case 314:  //heavy shower rain and drizzle:  //09d
                case 321:  //shower drizzle:  //09d
                    return 11;
//Group 5xx: Rain
                case 500:  //light rain:  //10d
                case 501:  //moderate rain:  //10d
                case 502:  //heavy intensity rain:  //10d
                case 503:  //very heavy rain:  //10d
                case 504:  //extreme rain:  //10d
                case 511:  //freezing rain:  //13d
                    return 10;
                case 520:  //light intensity shower rain:  //09d
                    return 40;
                case 521:  //shower rain:  //09d
                case 522:  //heavy intensity shower rain:  //09d
                case 531:  //ragged shower rain:  //09d
                    return 12;
//Group 6xx: Snow
                case 600:  //light snow:  //[[file:13d.png]]
                    return 7;
                case 601:  //snow:  //[[file:13d.png]]
                    return 16;
                case 602:  //heavy snow:  //[[file:13d.png]]
                    return 15;
                case 611:  //sleet:  //[[file:13d.png]]
                case 612:  //shower sleet:  //[[file:13d.png]]
                    return 18;
                case 615:  //light rain and snow:  //[[file:13d.png]]
                case 616:  //rain and snow:  //[[file:13d.png]]
                    return 5;
                case 620:  //light shower snow:  //[[file:13d.png]]
                    return 14;
                case 621:  //shower snow:  //[[file:13d.png]]
                    return 46;
                case 622:  //heavy shower snow:  //[[file:13d.png]]
//Group 7xx: Atmosphere
                case 701:  //mist:  //[[file:50d.png]]
                case 711:  //smoke:  //[[file:50d.png]]
                    return 22;
                case 721:  //haze:  //[[file:50d.png]]
                    return 21;
                case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                    return 3200;
                case 741:  //fog:  //[[file:50d.png]]
                    return 20;
                case 751:  //sand:  //[[file:50d.png]]
                case 761:  //dust:  //[[file:50d.png]]
                    return 19;
                case 762:  //volcanic ash:  //[[file:50d.png]]
                case 771:  //squalls:  //[[file:50d.png]]
                    return 3200;
                case 781:  //tornado:  //[[file:50d.png]]
                case 900:  //tornado
                    return 0;
//Group 800: Clear
                case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                    return 32;
//Group 80x: Clouds
                case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                    return 34;
                case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                    return 44;
//Group 90x: Extreme
                case 901:  //tropical storm
                    return 1;
                case 903:  //cold
                    return 25;
                case 904:  //hot
                    return 36;
                case 905:  //windy
                    return 24;
                case 906:  //hail
                    return 17;
//Group 9xx: Additional
                case 951:  //calm
                case 952:  //light breeze
                case 953:  //gentle breeze
                case 954:  //moderate breeze
                case 955:  //fresh breeze
                    return 34;
                case 956:  //strong breeze
                case 957:  //high windcase  near gale
                    return 24;
                case 958:  //gale
                case 959:  //severe gale
                case 960:  //storm
                case 961:  //violent storm
                    return 3200;
                case 902:  //hurricane
                case 962:  //hurricane
                    return 2;
                default:
                    return 3200;

            }
        }

        public static String mapToOpenWeatherMapIcon(int openWeatherMapCondition) {
            //see https://openweathermap.org/weather-conditions
            String condition = "02d"; //generic "variable" icon

            if (openWeatherMapCondition >= 200 && openWeatherMapCondition < 300) {
                condition = "11d";
            } else if (openWeatherMapCondition >= 300 && openWeatherMapCondition < 500) {
                condition = "09d";
            } else if (openWeatherMapCondition >= 500 && openWeatherMapCondition < 510) {
                condition = "10d";
            } else if (openWeatherMapCondition >= 511 && openWeatherMapCondition < 600) {
                condition = "09d";
            } else if (openWeatherMapCondition >= 600 && openWeatherMapCondition < 700) {
                condition = "13d";
            } else if (openWeatherMapCondition >= 700 && openWeatherMapCondition < 800) {
                condition = "50d";
            } else if (openWeatherMapCondition == 800) {
                condition = "01d"; //TODO: night?
            } else if (openWeatherMapCondition == 801) {
                condition = "02d"; //TODO: night?
            } else if (openWeatherMapCondition == 802) {
                condition = "03d"; //TODO: night?
            } else if (openWeatherMapCondition == 803 || openWeatherMapCondition == 804) {
                condition = "04d"; //TODO: night?
            }

            return condition;
        }

        public static int mapToOpenWeatherMapCondition(int yahooCondition) {
            switch (yahooCondition) {
//yahoo weather conditions:
//https://developer.yahoo.com/weather/documentation.html
                case 0:  //tornado
                    return 900;
                case 1:  //tropical storm
                    return 901;
                case 2:  //hurricane
                    return 962;
                case 3:  //severe thunderstorms
                    return 212;
                case 4:  //thunderstorms
                    return 211;
                case 5:  //mixed rain and snow
                case 6:  //mixed rain and sleet
                    return 616;
                case 7:  //mixed snow and sleet
                    return 600;
                case 8:  //freezing drizzle
                case 9:  //drizzle
                    return 301;
                case 10:  //freezing rain
                    return 511;
                case 11:  //showers
                case 12:  //showers
                    return 521;
                case 13:  //snow flurries
                case 14:  //light snow showers
                    return 620;
                case 15:  //blowing snow
                case 41:  //heavy snow
                case 42:  //scattered snow showers
                case 43:  //heavy snow
                case 46:  //snow showers
                    return 602;
                case 16:  //snow
                    return 601;
                case 17:  //hail
                case 35:  //mixed rain and hail
                    return 906;
                case 18:  //sleet
                    return 611;
                case 19:  //dust
                    return 761;
                case 20:  //foggy
                    return 741;
                case 21:  //haze
                    return 721;
                case 22:  //smoky
                    return 711;
                case 23:  //blustery
                case 24:  //windy
                    return 905;
                case 25:  //cold
                    return 903;
                case 26:  //cloudy
                case 27:  //mostly cloudy (night)
                case 28:  //mostly cloudy (day)
                    return 804;
                case 29:  //partly cloudy (night)
                case 30:  //partly cloudy (day)
                    return 801;
                case 31:  //clear (night)
                case 32:  //sunny
                    return 800;
                case 33:  //fair (night)
                case 34:  //fair (day)
                    return 801;
                case 36:  //hot
                    return 904;
                case 37:  //isolated thunderstorms
                case 38:  //scattered thunderstorms
                case 39:  //scattered thunderstorms
                    return 210;
                case 40:  //scattered showers
                    return 520;
                case 44:  //partly cloudy
                    return 801;
                case 45:  //thundershowers
                case 47:  //isolated thundershowers
                    return 211;
                case 3200:  //not available
                default:
                    return -1;
            }
        }

        public static String getConditionString(final Context context, int openWeatherMapCondition) {
            switch (openWeatherMapCondition) {
                case 200:
                    return context.getString(R.string.weather_condition_thunderstorm_with_light_rain);
                case 201:
                    return context.getString(R.string.weather_condition_thunderstorm_with_rain);
                case 202:
                    return context.getString(R.string.weather_condition_thunderstorm_with_heavy_rain);
                case 210:
                    return context.getString(R.string.weather_condition_light_thunderstorm);
                case 211:
                    return context.getString(R.string.weather_condition_thunderstorm);
                case 230:
                    return context.getString(R.string.weather_condition_thunderstorm_with_light_drizzle);
                case 231:
                    return context.getString(R.string.weather_condition_thunderstorm_with_drizzle);
                case 232:
                    return context.getString(R.string.weather_condition_thunderstorm_with_heavy_drizzle);
                case 212:
                    return context.getString(R.string.weather_condition_heavy_thunderstorm);
                case 221:
                    return context.getString(R.string.weather_condition_ragged_thunderstorm);
                // Group 3xx: Drizzle
                case 300:
                    return context.getString(R.string.weather_condition_light_intensity_drizzle);
                case 301:
                    return context.getString(R.string.weather_condition_drizzle);
                case 302:
                    return context.getString(R.string.weather_condition_heavy_intensity_drizzle);
                case 310:
                    return context.getString(R.string.weather_condition_light_intensity_drizzle_rain);
                case 311:
                    return context.getString(R.string.weather_condition_drizzle_rain);
                case 312:
                    return context.getString(R.string.weather_condition_heavy_intensity_drizzle_rain);
                case 313:
                    return context.getString(R.string.weather_condition_shower_rain_and_drizzle);
                case 314:
                    return context.getString(R.string.weather_condition_heavy_shower_rain_and_drizzle);
                case 321:
                    return context.getString(R.string.weather_condition_shower_drizzle);
                // Group 5xx: Rain
                case 500:
                    return context.getString(R.string.weather_condition_light_rain);
                case 501:
                    return context.getString(R.string.weather_condition_moderate_rain);
                case 502:
                    return context.getString(R.string.weather_condition_heavy_intensity_rain);
                case 503:
                    return context.getString(R.string.weather_condition_very_heavy_rain);
                case 504:
                    return context.getString(R.string.weather_condition_extreme_rain);
                case 511:
                    return context.getString(R.string.weather_condition_freezing_rain);
                case 520:
                    return context.getString(R.string.weather_condition_light_intensity_shower_rain);
                case 521:
                    return context.getString(R.string.weather_condition_shower_rain);
                case 522:
                    return context.getString(R.string.weather_condition_heavy_intensity_shower_rain);
                case 531:
                    return context.getString(R.string.weather_condition_ragged_shower_rain);
                // Group 6xx: Snow
                case 600:
                    return context.getString(R.string.weather_condition_light_snow);
                case 601:
                    return context.getString(R.string.weather_condition_snow);
                case 620:
                    return context.getString(R.string.weather_condition_light_shower_snow);
                case 602:
                    return context.getString(R.string.weather_condition_heavy_snow);
                case 611:
                    return context.getString(R.string.weather_condition_sleet);
                case 612:
                    return context.getString(R.string.weather_condition_shower_sleet);
                case 621:
                    return context.getString(R.string.weather_condition_shower_snow);
                case 622:
                    return context.getString(R.string.weather_condition_heavy_shower_snow);
                case 615:
                    return context.getString(R.string.weather_condition_light_rain_and_snow);
                case 616:
                    return context.getString(R.string.weather_condition_rain_and_snow);
                // Group 7xx: Atmosphere
                case 701:
                    return context.getString(R.string.weather_condition_mist);
                case 711:
                    return context.getString(R.string.weather_condition_smoke);
                case 721:
                    return context.getString(R.string.weather_condition_haze);
                case 731:
                    return context.getString(R.string.weather_condition_sandcase_dust_whirls);
                case 741:
                    return context.getString(R.string.weather_condition_fog);
                case 751:
                    return context.getString(R.string.weather_condition_sand);
                case 761:
                    return context.getString(R.string.weather_condition_dust);
                case 762:
                    return context.getString(R.string.weather_condition_volcanic_ash);
                case 771:
                    return context.getString(R.string.weather_condition_squalls);
                case 781:
                    return context.getString(R.string.weather_condition_tornado);
                case 900:
                    return context.getString(R.string.weather_condition_tornado);
                case 800:
                    return context.getString(R.string.weather_condition_clear_sky);
                // Group 80x: Clouds
                case 801:
                    return context.getString(R.string.weather_condition_few_clouds);
                case 802:
                    return context.getString(R.string.weather_condition_scattered_clouds);
                case 803:
                    return context.getString(R.string.weather_condition_broken_clouds);
                case 804:
                    return context.getString(R.string.weather_condition_overcast_clouds);
                // Group 90x: Extreme
                case 901:
                    return context.getString(R.string.weather_condition_tropical_storm);
                case 903:
                    return context.getString(R.string.weather_condition_cold);
                case 904:
                    return context.getString(R.string.weather_condition_hot);
                case 905:
                    return context.getString(R.string.weather_condition_windy);
                case 906:
                    return context.getString(R.string.weather_condition_hail);
                // Group 9xx: Additional
                case 951:
                    return context.getString(R.string.weather_condition_calm);
                case 952:
                    return context.getString(R.string.weather_condition_light_breeze);
                case 953:
                    return context.getString(R.string.weather_condition_gentle_breeze);
                case 954:
                    return context.getString(R.string.weather_condition_moderate_breeze);
                case 955:
                    return context.getString(R.string.weather_condition_fresh_breeze);
                case 956:
                    return context.getString(R.string.weather_condition_strong_breeze);
                case 957:
                    return context.getString(R.string.weather_condition_high_windcase_near_gale);
                case 958:
                    return context.getString(R.string.weather_condition_gale);
                case 959:
                    return context.getString(R.string.weather_condition_severe_gale);
                case 960:
                    return context.getString(R.string.weather_condition_storm);
                case 961:
                    return context.getString(R.string.weather_condition_violent_storm);
                case 902:
                    return context.getString(R.string.weather_condition_hurricane);
                case 962:
                    return context.getString(R.string.weather_condition_hurricane);
                default:
                    return "";
            }
        }

        public static String getAqiLevelString(final Context context, int aqi) {
            // Uses the [2023 Plume index](https://plumelabs.files.wordpress.com/2023/06/plume_aqi_2023.pdf) as a reference
            if (aqi < 0) {
                return context.getString(R.string.aqi_level_unknown);
            }
            if (aqi < 20) {
                return context.getString(R.string.aqi_level_excellent);
            } else if (aqi < 50) {
                return context.getString(R.string.aqi_level_fair);
            } else if (aqi < 100) {
                return context.getString(R.string.aqi_level_poor);
            } else if (aqi < 150) {
                return context.getString(R.string.aqi_level_unhealthy);
            } else if (aqi < 250) {
                return context.getString(R.string.aqi_level_very_unhealthy);
            } else {
                return context.getString(R.string.aqi_level_dangerous);
            }
        }

        public static byte mapToZeTimeConditionOld(int openWeatherMapCondition) {
/* deducted values:
    0 = partly cloudy
    1 = cloudy
    2 = sunny
    3 = windy/gale
    4 = heavy rain
    5 = snowy
    6 = storm
 */
            switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
                case 200:  //thunderstorm with light rain:  //11d
                case 201:  //thunderstorm with rain:  //11d
                case 202:  //thunderstorm with heavy rain:  //11d
                case 210:  //light thunderstorm::  //11d
                case 211:  //thunderstorm:  //11d
                case 230:  //thunderstorm with light drizzle:  //11d
                case 231:  //thunderstorm with drizzle:  //11d
                case 232:  //thunderstorm with heavy drizzle:  //11d
                case 212:  //heavy thunderstorm:  //11d
                case 221:  //ragged thunderstorm:  //11d
//Group 7xx: Atmosphere
                case 771:  //squalls:  //[[file:50d.png]]
                case 781:  //tornado:  //[[file:50d.png]]
//Group 90x: Extreme
                case 900:  //tornado
                case 901:  //tropical storm
//Group 9xx: Additional
                case 960:  //storm
                case 961:  //violent storm
                case 902:  //hurricane
                case 962:  //hurricane
                    return 6;
//Group 3xx: Drizzle
                case 300:  //light intensity drizzle:  //09d
                case 301:  //drizzle:  //09d
                case 302:  //heavy intensity drizzle:  //09d
                case 310:  //light intensity drizzle rain:  //09d
                case 311:  //drizzle rain:  //09d
                case 312:  //heavy intensity drizzle rain:  //09d
                case 313:  //shower rain and drizzle:  //09d
                case 314:  //heavy shower rain and drizzle:  //09d
                case 321:  //shower drizzle:  //09d
//Group 5xx: Rain
                case 500:  //light rain:  //10d
                case 501:  //moderate rain:  //10d
                case 502:  //heavy intensity rain:  //10d
                case 503:  //very heavy rain:  //10d
                case 504:  //extreme rain:  //10d
                case 511:  //freezing rain:  //13d
                case 520:  //light intensity shower rain:  //09d
                case 521:  //shower rain:  //09d
                case 522:  //heavy intensity shower rain:  //09d
                case 531:  //ragged shower rain:  //09d
//Group 90x: Extreme
                case 906:  //hail
                    return 4;
//Group 6xx: Snow
                case 600:  //light snow:  //[[file:13d.png]]
                case 601:  //snow:  //[[file:13d.png]]
                case 620:  //light shower snow:  //[[file:13d.png]]
                case 602:  //heavy snow:  //[[file:13d.png]]
                case 611:  //sleet:  //[[file:13d.png]]
                case 612:  //shower sleet:  //[[file:13d.png]]
                case 621:  //shower snow:  //[[file:13d.png]]
                case 622:  //heavy shower snow:  //[[file:13d.png]]
                case 615:  //light rain and snow:  //[[file:13d.png]]
                case 616:  //rain and snow:  //[[file:13d.png]]
//Group 90x: Extreme
                case 903:  //cold
                    return 5;
//Group 7xx: Atmosphere
                case 701:  //mist:  //[[file:50d.png]]
                case 711:  //smoke:  //[[file:50d.png]]
                case 721:  //haze:  //[[file:50d.png]]
                case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                case 741:  //fog:  //[[file:50d.png]]
                case 751:  //sand:  //[[file:50d.png]]
                case 761:  //dust:  //[[file:50d.png]]
                case 762:  //volcanic ash:  //[[file:50d.png]]
                    return 1;
//Group 800: Clear
                case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
//Group 90x: Extreme
                case 904:  //hot
                    return 2;
//Group 80x: Clouds
                case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                default:
                    return 0;

//Group 9xx: Additional
                case 905:  //windy
                case 951:  //calm
                case 952:  //light breeze
                case 953:  //gentle breeze
                case 954:  //moderate breeze
                case 955:  //fresh breeze
                case 956:  //strong breeze
                case 957:  //high windcase  near gale
                case 958:  //gale
                case 959:  //severe gale
                    return 3;
            }
        }

        public static byte mapToZeTimeCondition(int openWeatherMapCondition) {
/* deducted values:
    0 = tornado
    1 = typhoon
    2 = hurricane
    3 = thunderstorm
    4 = rain and snow
    5 = unavailable
    6 = freezing rain
    7 = drizzle
    8 = showers
    9 = snow flurries
    10 = blowing snow
    11 = snow
    12 = sleet
    13 = foggy
    14 = windy
    15 = cloudy
    16 = partly cloudy (night)
    17 = partly cloudy (day)
    18 = clear night
    19 = sunny
    20 = thundershower
    21 = hot
    22 = scattered thunders
    23 = snow showers
    24 = heavy snow
 */
            switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
                case 210:  //light thunderstorm::  //11d
                    return 22;

//Group 2xx: Thunderstorm
                case 200:  //thunderstorm with light rain:  //11d
                case 201:  //thunderstorm with rain:  //11d
                case 202:  //thunderstorm with heavy rain:  //11d
                case 230:  //thunderstorm with light drizzle:  //11d
                case 231:  //thunderstorm with drizzle:  //11d
                case 232:  //thunderstorm with heavy drizzle:  //11d
                    return 20;

//Group 2xx: Thunderstorm
                case 211:  //thunderstorm:  //11d
                case 212:  //heavy thunderstorm:  //11d
                case 221:  //ragged thunderstorm:  //11d
                    return 3;

//Group 7xx: Atmosphere
                case 781:  //tornado:  //[[file:50d.png]]
//Group 90x: Extreme
                case 900:  //tornado
                    return 0;

//Group 90x: Extreme
                case 901:  //tropical storm
                    return 1;

// Group 7xx: Atmosphere
                case 771:  //squalls:  //[[file:50d.png]]
//Group 9xx: Additional
                case 960:  //storm
                case 961:  //violent storm
                case 902:  //hurricane
                case 962:  //hurricane
                    return 2;

//Group 3xx: Drizzle
                case 300:  //light intensity drizzle:  //09d
                case 301:  //drizzle:  //09d
                case 302:  //heavy intensity drizzle:  //09d
                case 310:  //light intensity drizzle rain:  //09d
                case 311:  //drizzle rain:  //09d
                case 312:  //heavy intensity drizzle rain:  //09d
                case 313:  //shower rain and drizzle:  //09d
                case 314:  //heavy shower rain and drizzle:  //09d
                case 321:  //shower drizzle:  //09d
                    return 7;

//Group 5xx: Rain
                case 500:  //light rain:  //10d
                case 501:  //moderate rain:  //10d
                case 502:  //heavy intensity rain:  //10d
                case 503:  //very heavy rain:  //10d
                case 504:  //extreme rain:  //10d
                case 520:  //light intensity shower rain:  //09d
                case 521:  //shower rain:  //09d
                case 522:  //heavy intensity shower rain:  //09d
                case 531:  //ragged shower rain:  //09d
//Group 90x: Extreme
                case 906:  //hail
                    return 8;

//Group 5xx: Rain
                case 511:  //freezing rain:  //13d
                    return 6;

//Group 6xx: Snow
                case 620:  //light shower snow:  //[[file:13d.png]]
                case 621:  //shower snow:  //[[file:13d.png]]
                case 622:  //heavy shower snow:  //[[file:13d.png]]
                    return 23;

//Group 6xx: Snow
                case 615:  //light rain and snow:  //[[file:13d.png]]
                case 616:  //rain and snow:  //[[file:13d.png]]
                    return 4;

//Group 6xx: Snow
                case 611:  //sleet:  //[[file:13d.png]]
                case 612:  //shower sleet:  //[[file:13d.png]]
                    return 12;

//Group 6xx: Snow
                case 600:  //light snow:  //[[file:13d.png]]
                case 601:  //snow:  //[[file:13d.png]]
                    return 11;
//Group 6xx: Snow
                case 602:  //heavy snow:  //[[file:13d.png]]
                    return 24;

//Group 7xx: Atmosphere
                case 701:  //mist:  //[[file:50d.png]]
                case 711:  //smoke:  //[[file:50d.png]]
                case 721:  //haze:  //[[file:50d.png]]
                case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                case 741:  //fog:  //[[file:50d.png]]
                case 751:  //sand:  //[[file:50d.png]]
                case 761:  //dust:  //[[file:50d.png]]
                case 762:  //volcanic ash:  //[[file:50d.png]]
                    return 13;

//Group 800: Clear
                case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                    return 19;

//Group 90x: Extreme
                case 904:  //hot
                    return 21;

//Group 80x: Clouds
                case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                    return 17;

//Group 80x: Clouds
                case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                    return 15;

//Group 9xx: Additional
                case 905:  //windy
                case 951:  //calm
                case 952:  //light breeze
                case 953:  //gentle breeze
                case 954:  //moderate breeze
                case 955:  //fresh breeze
                case 956:  //strong breeze
                case 957:  //high windcase  near gale
                case 958:  //gale
                case 959:  //severe gale
                    return 14;

                default:
//Group 90x: Extreme
                case 903:  //cold
                    return 5;
            }
        }
        public static byte mapToCmfCondition(int openWeatherMapCondition) {
/* deducted values:
    1 = sunny
    2 = cloudy
    3 = overcast
    4 = showers
    5 = snow showers
    6 = fog

    9 = thunder showers

    14 = sleet

    19 = hot (extreme)
    20 = cold (extreme)

    21 = strong wind
    22 = (night) sunny - with moon
    23 = (night) sunny with stars
    24 = (night) cloudy - with moon
    25 = sun with haze
    26 = cloudy (sun with cloud)
 */
            switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
                case 210:  //light thunderstorm::  //11d
                case 200:  //thunderstorm with light rain:  //11d
                case 201:  //thunderstorm with rain:  //11d
                case 202:  //thunderstorm with heavy rain:  //11d
                case 230:  //thunderstorm with light drizzle:  //11d
                case 231:  //thunderstorm with drizzle:  //11d
                case 232:  //thunderstorm with heavy drizzle:  //11d
                case 211:  //thunderstorm:  //11d
                case 212:  //heavy thunderstorm:  //11d
                case 221:  //ragged thunderstorm:  //11d
                    return 9;

//Group 90x: Extreme
                case 901:  //tropical storm
//Group 7xx: Atmosphere
                case 781:  //tornado:  //[[file:50d.png]]
//Group 90x: Extreme
                case 900:  //tornado
// Group 7xx: Atmosphere
                case 771:  //squalls:  //[[file:50d.png]]
//Group 9xx: Additional
                case 960:  //storm
                case 961:  //violent storm
                case 902:  //hurricane
                case 962:  //hurricane
                    return 21;

//Group 3xx: Drizzle
                case 300:  //light intensity drizzle:  //09d
                case 301:  //drizzle:  //09d
                case 302:  //heavy intensity drizzle:  //09d
                case 310:  //light intensity drizzle rain:  //09d
                case 311:  //drizzle rain:  //09d
                case 312:  //heavy intensity drizzle rain:  //09d
                case 313:  //shower rain and drizzle:  //09d
                case 314:  //heavy shower rain and drizzle:  //09d
                case 321:  //shower drizzle:  //09d
//Group 5xx: Rain
                case 500:  //light rain:  //10d
                case 501:  //moderate rain:  //10d
                case 502:  //heavy intensity rain:  //10d
                case 503:  //very heavy rain:  //10d
                case 504:  //extreme rain:  //10d
                case 520:  //light intensity shower rain:  //09d
                case 521:  //shower rain:  //09d
                case 522:  //heavy intensity shower rain:  //09d
                case 531:  //ragged shower rain:  //09d
                    return 4;

//Group 90x: Extreme
                case 906:  //hail
                case 615:  //light rain and snow:  //[[file:13d.png]]
                case 616:  //rain and snow:  //[[file:13d.png]]
                case 511:  //freezing rain:  //13d
                    return 14;

//Group 6xx: Snow
                case 611:  //sleet:  //[[file:13d.png]]
                case 612:  //shower sleet:  //[[file:13d.png]]
//Group 6xx: Snow
                case 600:  //light snow:  //[[file:13d.png]]
                case 601:  //snow:  //[[file:13d.png]]
//Group 6xx: Snow
                case 602:  //heavy snow:  //[[file:13d.png]]
//Group 6xx: Snow
                case 620:  //light shower snow:  //[[file:13d.png]]
                case 621:  //shower snow:  //[[file:13d.png]]
                case 622:  //heavy shower snow:  //[[file:13d.png]]
                    return 5;


//Group 7xx: Atmosphere
                case 701:  //mist:  //[[file:50d.png]]
                case 711:  //smoke:  //[[file:50d.png]]
                case 721:  //haze:  //[[file:50d.png]]
                case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                case 741:  //fog:  //[[file:50d.png]]
                case 751:  //sand:  //[[file:50d.png]]
                case 761:  //dust:  //[[file:50d.png]]
                case 762:  //volcanic ash:  //[[file:50d.png]]
                    return 6;

//Group 800: Clear
                case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                    return 1;

//Group 90x: Extreme
                case 904:  //hot
                    return 19;

//Group 80x: Clouds
                case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                    return 26;
                case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                    return 2;

//Group 80x: Clouds
                case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                    return 3;

//Group 9xx: Additional
                case 905:  //windy
                case 951:  //calm
                case 952:  //light breeze
                case 953:  //gentle breeze
                case 954:  //moderate breeze
                case 955:  //fresh breeze
                case 956:  //strong breeze
                case 957:  //high windcase  near gale
                case 958:  //gale
                case 959:  //severe gale
                    return 21;

                default:
//Group 90x: Extreme
                case 903:  //cold
                    return 20;
            }
        }

        public static byte mapToFitProCondition(int openWeatherMapCondition) {
            switch (openWeatherMapCondition) {
                case 100:
                    return 1;
                case 104:
                    return 2;
                case 101:
                case 102:
                case 103:
                    return 3;
                case 305:
                case 309:
                    return 4;
                case 306:
                case 314:
                case 399:
                    return 5;
                case 307:
                case 308:
                case 310:
                case 311:
                case 312:
                case 315:
                case 316:
                case 317:
                case 318:
                    return 6;
                case 300:
                case 301:
                case 302:
                case 303:
                    return 7;
                case 400:
                case 407:
                    return 8;
                case 401:
                case 408:
                case 499:
                    return 9;
                case 402:
                case 403:
                case 409:
                case 410:
                    return 10;
                case 404:
                case 405:
                case 406:
                    return 11;
                case 500:
                case 501:
                case 502:
                case 509:
                case 510:
                case 511:
                case 512:
                case 513:
                case 514:
                case 515:
                    return 12;
                case 304:
                case 313:
                    return 13;
                case 503:
                case 504:
                case 507:
                case 508:
                    return 14;
                case 200:
                case 201:
                case 202:
                case 203:
                case 204:
                    return 15;
                case 205:
                case 206:
                case 207:
                case 208:
                    return 16;
                case 209:
                case 210:
                case 211:
                    return 17;
                case 212:
                    return 18;
                case 231:
                    return 19;
                default:
                    return 3;
            }
        }

        /**
         * Set the weather cache file. If enabled and the current weather is null, load the cache file.
         *
         * @param cacheDir the cache directory, where the cache file will be created
         * @param enabled whether caching is enabled
         */
        public void setCacheFile(final File cacheDir, final boolean enabled) {
            // FIXME: Do not use serializable for this

            cacheFile = new File(cacheDir, "weatherCache.bin");

            if (enabled) {
                LOG.info("Setting weather cache file to {}", cacheFile.getPath());

                if (cacheFile.isFile() && weatherSpecs.isEmpty()) {
                    try (final ObjectInputStream o = new ObjectInputStream(new FileInputStream(cacheFile))) {
                        final ArrayList<WeatherSpec> cachedSpecs = (ArrayList<WeatherSpec>) o.readObject();
                        weatherSpecs.addAll(cachedSpecs);
                    } catch (final ObjectStreamException e) {
                        LOG.error("Failed to deserialize weather from cache", e);
                        // keep cacheFile set - it's most likely an older version
                    } catch (final IOException e) {
                        LOG.error("Failed to read weather cache file", e);
                        // Something is wrong with the file
                        cacheFile = null;
                    } catch (final Throwable e) {
                        LOG.error("Failed to read weather from cache", e);
                        // keep cacheFile set - it's most likely an older version
                    }
                } else if (!weatherSpecs.isEmpty()) {
                    saveToCache();
                }
            } else {
                if (cacheFile.isFile()) {
                    LOG.info("Deleting weather cache file {}", cacheFile.getPath());

                    try {
                        cacheFile.delete();
                    } catch (final Throwable e) {
                        LOG.error("Failed to delete cache file", e);
                        cacheFile = null;
                    }
                }
            }
        }

        /**
         * Save the current weather to cache, if a cache file is enabled and the weather is not null.
         */
        public void saveToCache() {
            if (weatherSpecs.isEmpty() || cacheFile == null) {
                return;
            }

            LOG.info("Loading weather from cache {}", cacheFile.getPath());

            try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                o.writeObject(weatherSpecs);
            } catch (final Throwable e) {
                LOG.error("Failed to save weather to cache", e);
            }
        }
    }
}
