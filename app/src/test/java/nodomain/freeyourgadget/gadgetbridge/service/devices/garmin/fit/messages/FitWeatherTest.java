/*  Copyright (C) 2025-2026 José Rebelo, Daniele Gobbetti, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupportTest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;

public class FitWeatherTest {

    @NonNull
    public static WeatherSpec getWeatherSpec() {
        final WeatherSpec weather = new WeatherSpec();

        int timestamp = 1764364324;

        weather.setTimestamp(timestamp);
        weather.setLocation("Green Hill");
        weather.setCurrentTemp(15 + 273);
        weather.setCurrentConditionCode(601); // snow
        weather.setCurrentCondition("Snowy");
        weather.setCurrentHumidity(70);
        weather.setTodayMinTemp(10 + 273);
        weather.setTodayMaxTemp(25 + 273);
        weather.setWindSpeed(10);
        weather.setWindDirection(12);
        weather.setUvIndex(15.0f);
        weather.setPrecipProbability(99);
        weather.setDewPoint(10 + 273);
        weather.setPressure(812);
        weather.setCloudCover(100);
        weather.setVisibility(3210.0f);
        weather.setLatitude(38.250139f);
        weather.setLongitude(-122.410806f);
        weather.setFeelsLikeTemp(13 + 273);

        WeatherSpec.AirQuality airQuality = new WeatherSpec.AirQuality();
        airQuality.setAqi(50);
        weather.setAirQuality(airQuality);

        int[] conditions = {800, 801, 803, 771, 211, 613, 741, 721, 906, 321, 221, 511, 600, 602, 615, 616};

        weather.setHourly(new ArrayList<>());
        for (int i = 0; i < 24; i++) {
            final WeatherSpec.Hourly gbForecast = new WeatherSpec.Hourly();
            gbForecast.setTimestamp(timestamp - 3600 * (timestamp % 3600) + 3600 * (i + 1));
            gbForecast.setTemp((-25 + i * 7) % 45 + 273);
            gbForecast.setConditionCode(conditions[i % conditions.length]);
            gbForecast.setPrecipProbability((10 * i) % 105);
            gbForecast.setWindDirection((30 * i) % 360);
            gbForecast.setWindSpeed(18 * i);
            gbForecast.setDewPoint(gbForecast.getTemp() - 3);
            gbForecast.setHumidity((5 + 10 * i) % 101);
            gbForecast.setUvIndex((1.5f * i) % 15.5f);
            gbForecast.setPressure(800 + 5 * i);
            gbForecast.setCloudCover((14 * i) % 101);

            weather.getHourly().add(gbForecast);
        }

        weather.setForecasts(new ArrayList<>());
        for (int i = 0; i < 5; i++) {
            final WeatherSpec.Daily gbForecast = new WeatherSpec.Daily();
            gbForecast.setMinTemp(10 + i + 273);
            gbForecast.setMaxTemp(25 + i + 273);
            gbForecast.setConditionCode(800); // clear
            gbForecast.setHumidity(25 * i);
            gbForecast.setWindSpeed(15.0f * i);
            gbForecast.setWindDirection(45 * i);
            gbForecast.setUvIndex(3.0f * i);
            gbForecast.setPrecipProbability(50 + i);
            WeatherSpec.AirQuality airQualityDaily = new WeatherSpec.AirQuality();
            airQualityDaily.setAqi(120 + i);
            gbForecast.setAirQuality(airQualityDaily);
            gbForecast.setPressure(900 + i * 10);
            gbForecast.setCloudCover(25 * i);
            weather.getForecasts().add(gbForecast);
        }

        return weather;
    }

    @Test
    public void testEncode() throws IOException {
        final WeatherSpec weather = getWeatherSpec();
        final FitLocalMessageBuilder weatherLocalMessage = GarminSupport.encodeWeather(weather);

        final FitFile fitFile = new FitFile(weatherLocalMessage.getRecordDataList());
        final String actual = fitFile.toString().replace("}, Fit", "},\nFit").replace("}, RecordData{", "},\nRecordData{");

        final String expected = GarminSupportTest.readTextResource("/FitWeatherTestEncode.txt");
        Assert.assertEquals(expected, actual);
    }
}
