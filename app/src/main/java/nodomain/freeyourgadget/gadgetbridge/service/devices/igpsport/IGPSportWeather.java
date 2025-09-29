/*  Copyright (C) 2025 Vitaliy Tomin, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.igpsport;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Back;
import nodomain.freeyourgadget.gadgetbridge.proto.igpsport.Common;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class IGPSportWeather {
    Logger LOG = LoggerFactory.getLogger(IGPSportWeather.class);
    IGPSportDeviceSupport support;
    public IGPSportWeather(IGPSportDeviceSupport support) {
        this.support = support;
    }

    // A static map to hold the conversion logic from OWM to QWeather.
    private static final Map<Integer, Integer> codeMap = new HashMap<>();


    // Static initializer block to populate the map with conversion data.
    static {
        // === OWM Group 2xx: Thunderstorm ===
        codeMap.put(200, 302); // thunderstorm with light rain -> Thundershower
        codeMap.put(201, 302); // thunderstorm with rain -> Thundershower
        codeMap.put(202, 303); // thunderstorm with heavy rain -> Heavy Thunderstorm
        codeMap.put(210, 302); // light thunderstorm -> Thundershower
        codeMap.put(211, 302); // thunderstorm -> Thundershower
        codeMap.put(212, 303); // heavy thunderstorm -> Heavy Thunderstorm
        codeMap.put(221, 302); // ragged thunderstorm -> Thundershower
        codeMap.put(230, 302); // thunderstorm with light drizzle -> Thundershower
        codeMap.put(231, 302); // thunderstorm with drizzle -> Thundershower
        codeMap.put(232, 302); // thunderstorm with heavy drizzle -> Thundershower

        // === OWM Group 3xx: Drizzle ===
        codeMap.put(300, 309); // light intensity drizzle -> Drizzle Rain
        codeMap.put(301, 309); // drizzle -> Drizzle Rain
        codeMap.put(302, 309); // heavy intensity drizzle -> Drizzle Rain
        codeMap.put(310, 309); // light intensity drizzle rain -> Drizzle Rain
        codeMap.put(311, 309); // drizzle rain -> Drizzle Rain
        codeMap.put(312, 309); // heavy intensity drizzle rain -> Drizzle Rain
        codeMap.put(313, 309); // shower rain and drizzle -> Drizzle Rain
        codeMap.put(314, 309); // heavy shower rain and drizzle -> Drizzle Rain
        codeMap.put(321, 309); // shower drizzle -> Drizzle Rain

        // === OWM Group 5xx: Rain ===
        codeMap.put(500, 305); // light rain -> Light Rain
        codeMap.put(501, 306); // moderate rain -> Moderate Rain
        codeMap.put(502, 307); // heavy intensity rain -> Heavy Rain
        codeMap.put(503, 311); // very heavy rain -> Heavy Rainstorm
        codeMap.put(504, 308); // extreme rain -> Extreme Rain
        codeMap.put(511, 313); // freezing rain -> Freezing Rain
        codeMap.put(520, 300); // light intensity shower rain -> Shower Rain
        codeMap.put(521, 300); // shower rain -> Shower Rain
        codeMap.put(522, 301); // heavy intensity shower rain -> Heavy Shower Rain
        codeMap.put(531, 300); // ragged shower rain -> Shower Rain

        // === OWM Group 6xx: Snow ===
        codeMap.put(600, 400); // light snow -> Light Snow
        codeMap.put(601, 401); // snow -> Moderate Snow
        codeMap.put(602, 402); // heavy snow -> Heavy Snow
        codeMap.put(611, 404); // Sleet -> Sleet
        codeMap.put(612, 404); // Light shower sleet -> Sleet
        codeMap.put(613, 404); // Shower sleet -> Sleet
        codeMap.put(615, 405); // Light rain and snow -> Rain and Snow
        codeMap.put(616, 405); // Rain and snow -> Rain and Snow
        codeMap.put(620, 407); // Light shower snow -> Snow Flurry
        codeMap.put(621, 406); // Shower snow -> Shower Snow
        codeMap.put(622, 403); // Heavy shower snow -> Snowstorm

        // === OWM Group 7xx: Atmosphere ===
        codeMap.put(701, 500); // mist -> Mist
        codeMap.put(711, 502); // Smoke -> Haze
        codeMap.put(721, 502); // Haze -> Haze
        codeMap.put(731, 507); // sand/ dust whirls -> Duststorm
        codeMap.put(741, 501); // fog -> Fog
        codeMap.put(751, 503); // sand -> Sand
        codeMap.put(761, 504); // dust -> Dust
        codeMap.put(762, 999); // volcanic ash -> Unknown
        codeMap.put(771, 999); // squalls -> Unknown
        codeMap.put(781, 999); // tornado -> Unknown

        // === OWM Group 800: Clear ===
        codeMap.put(800, 100); // clear sky -> Sunny (day code)

        // === OWM Group 80x: Clouds ===
        codeMap.put(801, 102); // few clouds: 11-25% -> Few Clouds
        codeMap.put(802, 101); // scattered clouds: 25-50% -> Cloudy
        codeMap.put(803, 103); // broken clouds: 51-84% -> Partly Cloudy
        codeMap.put(804, 104); // overcast clouds: 85-100% -> Overcast

        // === OWM Group 9xx: Extreme ===
        codeMap.put(900, 999); // tornado -> Unknown
        codeMap.put(901, 999); // tropical storm -> Unknown
        codeMap.put(902, 999); // hurricane -> Unknown
        codeMap.put(903, 901); // cold -> Cold
        codeMap.put(904, 900); // hot -> Hot
        codeMap.put(905, 999); // windy -> Unknown
        codeMap.put(906, 304); // hail -> Hail
    }


    static int convertOWMtoQWeather(int openWeatherMapCode) {
        return codeMap.getOrDefault(openWeatherMapCode, 999); // Return 999 for unknown codes
    }

    public void handleWeather(WeatherSpec weatherSpec) {
        //example weather packet
        //010302ff02ffff00fe3601ffffffffffffffff04080310021802221508ac021012180b20002a0a323032342d30382d3330221508ac021015180a20002a0a323032342d30382d3331221408651019180c20002a0a323032342d30392d30312a20081310651812200b2a10323032342d30382d33302030333a34373201303a0132322108651011180d2210323032342d30382d33302030353a30302a0331353832023133322108651012180d2210323032342d30382d33302030363a30302a033136373202313332210865101018112210323032342d30382d33302030373a30302a0331373932023133322208ac02100f18332210323032342d30382d33302030383a30302a03323031320231333a0308e807

        try {
            Instant currentTime = Instant.now();
            DateTimeFormatter formatterHourly = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneOffset.UTC); //device requires UTC time here and in hourly
            TransactionBuilder builder = support.performInitialized("set weather");
            Back.back_msg.Builder weatherMsg = Back.back_msg.newBuilder();
            Back.weather_current_data_message.Builder currentWeatherMsg =Back.weather_current_data_message.newBuilder();
            currentWeatherMsg.setCurDayMinTemp(weatherSpec.getTodayMinTemp()-273);
            currentWeatherMsg.setCurDayMaxTemp(weatherSpec.getTodayMaxTemp()-273);
            currentWeatherMsg.setCurTemperature(weatherSpec.getCurrentTemp()-273);
            currentWeatherMsg.setCurWeather(IGPSportWeather.convertOWMtoQWeather(weatherSpec.getCurrentConditionCode()));
            currentWeatherMsg.setWindDeg(String.valueOf(weatherSpec.getWindDirection()));
            currentWeatherMsg.setWindSpd(String.valueOf(Math.round(weatherSpec.getWindSpeed())));
            currentWeatherMsg.setTime(formatterHourly.format(currentTime));

            int currentDay=0;

            for (final WeatherSpec.Daily forecast : weatherSpec.getForecasts()) {

                LocalDateTime now = LocalDateTime.now();
                currentDay++;
                LocalDateTime tomorrow = now.plusDays(currentDay);
                DateTimeFormatter formatterDaily = DateTimeFormatter.ISO_LOCAL_DATE; //"yyyy-MM-dd"
                weatherMsg.addThreeDaysMsg(Back.weather_three_days_data_message.newBuilder()
                        .setWeatherIndex(IGPSportWeather.convertOWMtoQWeather(forecast.getConditionCode()))
                        .setRainProb(forecast.getPrecipProbability())
                        .setMaxTemp(forecast.getMaxTemp()-273)
                        .setMinTemp(forecast.getMinTemp()-273)
                        .setDate(tomorrow.format(formatterDaily)).build());

                if (currentDay > 2) //we only need 3 days
                    break;
            }

            int currentHour=0;
            for (final WeatherSpec.Hourly hourly : weatherSpec.getHourly()) {
                weatherMsg.addThreeHoursMsg(Back.weather_three_hour_data_memsage.newBuilder()
                        .setWatherIndex(IGPSportWeather.convertOWMtoQWeather(hourly.getConditionCode()))
                        .setRainProb(hourly.getPrecipProbability())
                        .setTemp(hourly.getTemp()-273)
                        .setTime(formatterHourly.format(Instant.ofEpochMilli(hourly.getTimestamp() * 1000L)))
                        .setWindDeg(String.valueOf(hourly.getWindDirection()))
                        .setWindSpd(String.valueOf(Math.round(hourly.getWindSpeed()))).build());

                currentHour++;
                if (currentHour > 3 ) // its called three hour weather but shows actually 4 entries
                    break;
            }


            weatherMsg.setCurMsg(currentWeatherMsg);
            weatherMsg.setServiceType(Common.service_type_index.enum_SERVICE_TYPE_INDEX_BACK);
            weatherMsg.setBackOperateType(Back.BACK_OPERATE_TYPE.enum_BACK_OPERATE_TYPE_SEND);
            weatherMsg.setBackServiceType(Back.BACK_SERVICE_TYPE.enum_BACK_SERVICE_TYPE_WEATHER);



            byte[] weatherBytes = IGPSportDeviceSupport.craftData(weatherMsg.getServiceType().getNumber(),
                    weatherMsg.getBackServiceType().getNumber(),
                    weatherMsg.getBackOperateType().getNumber(),
                    weatherMsg.build().toByteArray());
            builder.writeChunkedData(support.writeCharacteristicFourth, weatherBytes, support.getMTU());
            builder.queue();

        } catch (IOException e) {
            LOG.error("Failed to encode weather ", e);
        }
    }


}
