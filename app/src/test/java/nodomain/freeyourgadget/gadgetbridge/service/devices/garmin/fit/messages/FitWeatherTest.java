package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitLocalMessageBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FitWeatherTest {
    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testEncode() {
        final WeatherSpec weather = getWeatherSpec();

        /*
        List<RecordData> weatherData = new ArrayList<>();

        final RecordDefinition recordDefinitionToday = PredefinedLocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition();
        final RecordDefinition recordDefinitionHourly = PredefinedLocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition();
        final RecordDefinition recordDefinitionDaily = PredefinedLocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition();

        List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        weatherDefinitions.add(recordDefinitionToday);
        weatherDefinitions.add(recordDefinitionHourly);
        weatherDefinitions.add(recordDefinitionDaily);

        RecordData today = new RecordData(recordDefinitionToday, recordDefinitionToday.getRecordHeader());
        today.setFieldByName("weather_report", 0); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        today.setFieldByName("timestamp", weather.getTimestamp());
        today.setFieldByName("observed_at_time", weather.getTimestamp());
        today.setFieldByName("temperature", weather.getCurrentTemp());
        today.setFieldByName("low_temperature", weather.getTodayMinTemp());
        today.setFieldByName("high_temperature", weather.getTodayMaxTemp());
        today.setFieldByName("condition", weather.getCurrentConditionCode());
        today.setFieldByName("wind_direction", weather.getWindDirection());
        today.setFieldByName("precipitation_probability", weather.getPrecipProbability());
        today.setFieldByName("wind_speed", Math.round(weather.getWindSpeed()));
        today.setFieldByName("temperature_feels_like", weather.getFeelsLikeTemp());
        today.setFieldByName("relative_humidity", weather.getCurrentHumidity());
        today.setFieldByName("observed_location_lat", weather.getLatitude());
        today.setFieldByName("observed_location_long", weather.getLongitude());
        today.setFieldByName("dew_point", weather.getDewPoint());
        today.setFieldByName("air_quality", weather.getAirQuality().getAqi());
        today.setFieldByName("location", weather.getLocation());
        weatherData.add(today);

        for (int hour = 0; hour <= 11; hour++) {
            if (hour < weather.getHourly().size()) {
                WeatherSpec.Hourly hourly = weather.getHourly().get(hour);
                RecordData weatherHourlyForecast = new RecordData(recordDefinitionHourly, recordDefinitionHourly.getRecordHeader());
                weatherHourlyForecast.setFieldByName("weather_report", 1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherHourlyForecast.setFieldByName("timestamp", hourly.getTimestamp());
                weatherHourlyForecast.setFieldByName("temperature", hourly.getTemp());
                weatherHourlyForecast.setFieldByName("condition", hourly.getConditionCode());
                weatherHourlyForecast.setFieldByName("temperature_feels_like", hourly.getTemp()); //TODO: switch to actual feels like field once Hourly contains this information
                weatherHourlyForecast.setFieldByName("wind_direction", hourly.getWindDirection());
                weatherHourlyForecast.setFieldByName("wind_speed", Math.round(hourly.getWindSpeed()));
                weatherHourlyForecast.setFieldByName("precipitation_probability", hourly.getPrecipProbability());
                weatherHourlyForecast.setFieldByName("relative_humidity", hourly.getHumidity());
//                    weatherHourlyForecast.setFieldByName("dew_point", 0); // TODO: add once Hourly contains this information
                weatherHourlyForecast.setFieldByName("uv_index", hourly.getUvIndex());
//                    weatherHourlyForecast.setFieldByName("air_quality", 0); // TODO: add once Hourly contains this information
                weatherData.add(weatherHourlyForecast);
            }
        }
//
        RecordData todayDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
        todayDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        todayDailyForecast.setFieldByName("timestamp", weather.getTimestamp());
        todayDailyForecast.setFieldByName("low_temperature", weather.getTodayMinTemp());
        todayDailyForecast.setFieldByName("high_temperature", weather.getTodayMaxTemp());
        todayDailyForecast.setFieldByName("condition", weather.getCurrentConditionCode());
        todayDailyForecast.setFieldByName("precipitation_probability", weather.getPrecipProbability());
        todayDailyForecast.setFieldByName("day_of_week", weather.getTimestamp());
        todayDailyForecast.setFieldByName("air_quality", weather.getAirQuality().getAqi());
        weatherData.add(todayDailyForecast);


        for (int day = 0; day < 4; day++) {
            if (day < weather.getForecasts().size()) {
                //noinspection ExtractMethodRecommender
                WeatherSpec.Daily daily = weather.getForecasts().get(day);
                int ts = weather.getTimestamp() + (day + 1) * 24 * 60 * 60;
                RecordData weatherDailyForecast = new RecordData(recordDefinitionDaily, recordDefinitionDaily.getRecordHeader());
                weatherDailyForecast.setFieldByName("weather_report", 2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherDailyForecast.setFieldByName("timestamp", weather.getTimestamp());
                weatherDailyForecast.setFieldByName("low_temperature", daily.getMinTemp());
                weatherDailyForecast.setFieldByName("high_temperature", daily.getMaxTemp());
                weatherDailyForecast.setFieldByName("condition", daily.getConditionCode());
                weatherDailyForecast.setFieldByName("precipitation_probability", daily.getPrecipProbability());
                weatherDailyForecast.setFieldByName("air_quality", daily.getAirQuality().getAqi());
                weatherData.add(weatherDailyForecast);
            }
        }

        /*
        List<RecordData> weatherData = new ArrayList<>();

        final FitWeather.Builder today = new FitWeather.Builder();
        today.setWeatherReport(0); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        today.setTimestamp((long) weather.getTimestamp());
        today.setObservedAtTime((long) weather.getTimestamp());
        today.setTemperature(weather.getCurrentTemp());
        today.setLowTemperature(weather.getTodayMinTemp());
        today.setHighTemperature(weather.getTodayMaxTemp());
        today.setCondition(FieldDefinitionWeatherCondition.openWeatherCodeToFitWeatherStatus(weather.getCurrentConditionCode()));
        today.setWindDirection(weather.getWindDirection());
        today.setPrecipitationProbability(weather.getPrecipProbability());
        today.setWindSpeed(weather.getWindSpeed());
        today.setTemperatureFeelsLike(weather.getFeelsLikeTemp());
        today.setRelativeHumidity(weather.getCurrentHumidity());
        today.setObservedLocationLat((long) weather.getLatitude());
        today.setObservedLocationLong((long) weather.getLongitude());
        today.setDewPoint(weather.getDewPoint());
        if (null != weather.getAirQuality()) {
            today.setAirQuality(FieldDefinitionWeatherAqi.aqiAbsoluteValueToEnum(weather.getAirQuality().getAqi()));
        }
        today.setLocation(weather.getLocation());
        weatherData.add(today.build(6));

        for (int hour = 0; hour <= 11; hour++) {
            if (hour < weather.getHourly().size()) {
                WeatherSpec.Hourly hourly = weather.getHourly().get(hour);
                final FitWeather.Builder weatherHourlyForecast = new FitWeather.Builder();
                weatherHourlyForecast.setWeatherReport(1); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherHourlyForecast.setTimestamp((long) hourly.getTimestamp());
                weatherHourlyForecast.setTemperature(hourly.getTemp());
                weatherHourlyForecast.setCondition(FieldDefinitionWeatherCondition.openWeatherCodeToFitWeatherStatus(hourly.getConditionCode()));
                weatherHourlyForecast.setTemperatureFeelsLike(hourly.getTemp()); //TODO: switch to actual feels like field once Hourly contains this information
                weatherHourlyForecast.setWindDirection(hourly.getWindDirection());
                weatherHourlyForecast.setWindSpeed(hourly.getWindSpeed());
                weatherHourlyForecast.setPrecipitationProbability(hourly.getPrecipProbability());
                weatherHourlyForecast.setRelativeHumidity(hourly.getHumidity());
//                    weatherHourlyForecast.setDewPoint(0); // TODO: add once Hourly contains this information
                weatherHourlyForecast.setUvIndex(hourly.getUvIndex());
//                    weatherHourlyForecast.setAirQuality(0); // TODO: add once Hourly contains this information
                weatherData.add(weatherHourlyForecast.build(9));
            }
        }
//
        final FitWeather.Builder todayDailyForecast = new FitWeather.Builder();
        todayDailyForecast.setWeatherReport(2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
        todayDailyForecast.setTimestamp((long) weather.getTimestamp());
        todayDailyForecast.setLowTemperature(weather.getTodayMinTemp());
        todayDailyForecast.setHighTemperature(weather.getTodayMaxTemp());
        todayDailyForecast.setCondition(FieldDefinitionWeatherCondition.openWeatherCodeToFitWeatherStatus(weather.getCurrentConditionCode()));
        todayDailyForecast.setPrecipitationProbability(weather.getPrecipProbability());
        todayDailyForecast.setDayOfWeek(Instant.ofEpochMilli(weather.getTimestamp() * 1000L).atZone(ZoneId.systemDefault()).getDayOfWeek());
        if (null != weather.getAirQuality()) {
            todayDailyForecast.setAirQuality(FieldDefinitionWeatherAqi.aqiAbsoluteValueToEnum(weather.getAirQuality().getAqi()));
        }
        weatherData.add(todayDailyForecast.build(10));


        for (int day = 0; day < 4; day++) {
            if (day < weather.getForecasts().size()) {
                WeatherSpec.Daily daily = weather.getForecasts().get(day);
                int ts = weather.getTimestamp() + (day + 1) * 24 * 60 * 60;
                final FitWeather.Builder weatherDailyForecast = new FitWeather.Builder();
                weatherDailyForecast.setWeatherReport(2); // 0 = current, 1 = hourly_forecast, 2 = daily_forecast
                weatherDailyForecast.setTimestamp((long) weather.getTimestamp());
                weatherDailyForecast.setLowTemperature(daily.getMinTemp());
                weatherDailyForecast.setHighTemperature(daily.getMaxTemp());
                weatherDailyForecast.setCondition(FieldDefinitionWeatherCondition.openWeatherCodeToFitWeatherStatus(daily.getConditionCode()));
                weatherDailyForecast.setPrecipitationProbability(daily.getPrecipProbability());
                if (null != daily.getAirQuality()) {
                    weatherDailyForecast.setAirQuality(FieldDefinitionWeatherAqi.aqiAbsoluteValueToEnum(daily.getAirQuality().getAqi()));
                }
                weatherDailyForecast.setDayOfWeek(Instant.ofEpochMilli(ts * 1000L).atZone(ZoneId.systemDefault()).getDayOfWeek());
                weatherData.add(weatherDailyForecast.build(10));
            }
        }
        */

        // Get all distinct record definitions
        //Set<Integer> seenDefinitions = new HashSet<>();
        //List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        //for (RecordData d : weatherData) {
        //    final int localMessageType = d.getRecordDefinition().getRecordHeader().getLocalMessageType();
        //    if (!seenDefinitions.contains(localMessageType)) {
        //        seenDefinitions.add(localMessageType);
        //        weatherDefinitions.add(d.getRecordDefinition());
        //    }
        //}

        /*
        Map<Integer, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < PredefinedLocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition().getFieldDefinitions().size(); i++) {
            orderMap.put(PredefinedLocalMessage.TODAY_WEATHER_CONDITIONS.getRecordDefinition().getFieldDefinitions().get(i).getNumber(), i);
        }
        weatherDefinitions.get(0).getFieldDefinitions()
                .sort(Comparator.comparingInt(fd -> orderMap.getOrDefault(fd.getNumber(), Integer.MAX_VALUE)));

        orderMap.clear();
        for (int i = 0; i < PredefinedLocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition().getFieldDefinitions().size(); i++) {
            orderMap.put(PredefinedLocalMessage.HOURLY_WEATHER_FORECAST.getRecordDefinition().getFieldDefinitions().get(i).getNumber(), i);
        }
        weatherDefinitions.get(1).getFieldDefinitions()
                .sort(Comparator.comparingInt(fd -> orderMap.getOrDefault(fd.getNumber(), Integer.MAX_VALUE)));

        orderMap.clear();
        for (int i = 0; i < PredefinedLocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition().getFieldDefinitions().size(); i++) {
            orderMap.put(PredefinedLocalMessage.DAILY_WEATHER_FORECAST.getRecordDefinition().getFieldDefinitions().get(i).getNumber(), i);
        }
        weatherDefinitions.get(2).getFieldDefinitions()
                .sort(Comparator.comparingInt(fd -> orderMap.getOrDefault(fd.getNumber(), Integer.MAX_VALUE)));

        Assert.assertEquals(3, weatherDefinitions.size());
        Assert.assertEquals("FieldDefinition{baseType=ENUM}", weatherDefinitions.get(0).getFieldDefinitions().get(0).toString());
        Assert.assertEquals("FieldDefinitionTimestamp{baseType=UINT32, offset=-631065600, size=4}", weatherDefinitions.get(0).getFieldDefinitions().get(1).toString());
        Assert.assertEquals("FieldDefinitionTimestamp{baseType=UINT32, offset=-631065600, size=4}", weatherDefinitions.get(0).getFieldDefinitions().get(2).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(0).getFieldDefinitions().get(3).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(0).getFieldDefinitions().get(4).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(0).getFieldDefinitions().get(5).toString());
        Assert.assertEquals("FieldDefinitionWeatherCondition{baseType=ENUM}", weatherDefinitions.get(0).getFieldDefinitions().get(6).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT16, size=2}", weatherDefinitions.get(0).getFieldDefinitions().get(7).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT8}", weatherDefinitions.get(0).getFieldDefinitions().get(8).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT16, scale=298, size=2}", weatherDefinitions.get(0).getFieldDefinitions().get(9).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(0).getFieldDefinitions().get(10).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT8}", weatherDefinitions.get(0).getFieldDefinitions().get(11).toString());
        Assert.assertEquals("FieldDefinition{baseType=SINT32, size=4}", weatherDefinitions.get(0).getFieldDefinitions().get(12).toString());
        Assert.assertEquals("FieldDefinition{baseType=SINT32, size=4}", weatherDefinitions.get(0).getFieldDefinitions().get(13).toString());
        Assert.assertEquals("FieldDefinitionWeatherAqi{baseType=ENUM}", weatherDefinitions.get(0).getFieldDefinitions().get(14).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(0).getFieldDefinitions().get(15).toString());
        Assert.assertEquals("FieldDefinition{baseType=STRING, size=15}", weatherDefinitions.get(0).getFieldDefinitions().get(16).toString());

        Assert.assertEquals("FieldDefinition{baseType=ENUM}", weatherDefinitions.get(1).getFieldDefinitions().get(0).toString());
        Assert.assertEquals("FieldDefinitionTimestamp{baseType=UINT32, offset=-631065600, size=4}", weatherDefinitions.get(1).getFieldDefinitions().get(1).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(1).getFieldDefinitions().get(2).toString());
        Assert.assertEquals("FieldDefinitionWeatherCondition{baseType=ENUM}", weatherDefinitions.get(1).getFieldDefinitions().get(3).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT16, size=2}", weatherDefinitions.get(1).getFieldDefinitions().get(4).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT16, scale=298, size=2}", weatherDefinitions.get(1).getFieldDefinitions().get(5).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT8}", weatherDefinitions.get(1).getFieldDefinitions().get(6).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(1).getFieldDefinitions().get(7).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT8}", weatherDefinitions.get(1).getFieldDefinitions().get(8).toString());
        //Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(1).getFieldDefinitions().get(9).toString());
        // FIXME Assert.assertEquals("FieldDefinition{baseType=FLOAT32, size=4}", weatherDefinitions.get(1).getFieldDefinitions().get(10).toString());
        // FIXME Assert.assertEquals("FieldDefinitionWeatherAqi{baseType=ENUM}", weatherDefinitions.get(1).getFieldDefinitions().get(11).toString());

        Assert.assertEquals("FieldDefinition{baseType=ENUM}", weatherDefinitions.get(2).getFieldDefinitions().get(0).toString());
        Assert.assertEquals("FieldDefinitionTimestamp{baseType=UINT32, offset=-631065600, size=4}", weatherDefinitions.get(2).getFieldDefinitions().get(1).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(2).getFieldDefinitions().get(2).toString());
        Assert.assertEquals("FieldDefinitionTemperature{baseType=SINT8, offset=-273}", weatherDefinitions.get(2).getFieldDefinitions().get(3).toString());
        Assert.assertEquals("FieldDefinitionWeatherCondition{baseType=ENUM}", weatherDefinitions.get(2).getFieldDefinitions().get(4).toString());
        Assert.assertEquals("FieldDefinition{baseType=UINT8}", weatherDefinitions.get(2).getFieldDefinitions().get(5).toString());
        Assert.assertEquals("FieldDefinitionDayOfWeek{baseType=ENUM}", weatherDefinitions.get(2).getFieldDefinitions().get(6).toString());
        Assert.assertEquals("FieldDefinitionWeatherAqi{baseType=ENUM}", weatherDefinitions.get(2).getFieldDefinitions().get(7).toString());

        Assert.assertEquals(18, weatherData.size());
        Assert.assertEquals("FitWeather{weather_report=0, temperature=288, condition=SNOW, wind_direction=12, wind_speed=10.0, precipitation_probability=99, temperature_feels_like=286, relative_humidity=30, location=Green Hill, observed_at_time=1764364324, observed_location_lat=38, observed_location_long=-122, high_temperature=298, low_temperature=283, dew_point=283, air_quality=UNHEALTHY_SENSITIVE, timestamp=1764364324}", weatherData.get(0).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=283, condition=CLEAR, wind_direction=30, wind_speed=20.0, precipitation_probability=50, temperature_feels_like=283, relative_humidity=10, uv_index=2.0}", weatherData.get(1).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=284, condition=CLEAR, wind_direction=31, wind_speed=21.0, precipitation_probability=51, temperature_feels_like=284, relative_humidity=11, uv_index=3.0}", weatherData.get(2).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=285, condition=CLEAR, wind_direction=32, wind_speed=22.0, precipitation_probability=52, temperature_feels_like=285, relative_humidity=12, uv_index=4.0}", weatherData.get(3).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=286, condition=CLEAR, wind_direction=33, wind_speed=23.0, precipitation_probability=53, temperature_feels_like=286, relative_humidity=13, uv_index=5.0}", weatherData.get(4).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=287, condition=CLEAR, wind_direction=34, wind_speed=24.0, precipitation_probability=54, temperature_feels_like=287, relative_humidity=14, uv_index=6.0}", weatherData.get(5).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=288, condition=CLEAR, wind_direction=35, wind_speed=25.0, precipitation_probability=55, temperature_feels_like=288, relative_humidity=15, uv_index=7.0}", weatherData.get(6).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=289, condition=CLEAR, wind_direction=36, wind_speed=26.0, precipitation_probability=56, temperature_feels_like=289, relative_humidity=16, uv_index=8.0}", weatherData.get(7).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=290, condition=CLEAR, wind_direction=37, wind_speed=27.0, precipitation_probability=57, temperature_feels_like=290, relative_humidity=17, uv_index=9.0}", weatherData.get(8).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=291, condition=CLEAR, wind_direction=38, wind_speed=28.0, precipitation_probability=58, temperature_feels_like=291, relative_humidity=18, uv_index=10.0}", weatherData.get(9).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=292, condition=CLEAR, wind_direction=39, wind_speed=29.0, precipitation_probability=59, temperature_feels_like=292, relative_humidity=19, uv_index=11.0}", weatherData.get(10).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=293, condition=CLEAR, wind_direction=40, wind_speed=30.0, precipitation_probability=60, temperature_feels_like=293, relative_humidity=20, uv_index=12.0}", weatherData.get(11).toString());
        Assert.assertEquals("FitWeather{weather_report=1, temperature=294, condition=CLEAR, wind_direction=41, wind_speed=31.0, precipitation_probability=61, temperature_feels_like=294, relative_humidity=21, uv_index=13.0}", weatherData.get(12).toString());
        Assert.assertEquals("FitWeather{weather_report=2, condition=SNOW, precipitation_probability=99, day_of_week=FRIDAY, high_temperature=298, low_temperature=283, air_quality=UNHEALTHY_SENSITIVE, timestamp=1764364324}", weatherData.get(13).toString());
        Assert.assertEquals("FitWeather{weather_report=2, condition=CLEAR, precipitation_probability=50, day_of_week=SATURDAY, high_temperature=298, low_temperature=283, timestamp=1764364324}", weatherData.get(14).toString());
        Assert.assertEquals("FitWeather{weather_report=2, condition=CLEAR, precipitation_probability=51, day_of_week=SUNDAY, high_temperature=299, low_temperature=284, timestamp=1764364324}", weatherData.get(15).toString());
        Assert.assertEquals("FitWeather{weather_report=2, condition=CLEAR, precipitation_probability=52, day_of_week=MONDAY, high_temperature=300, low_temperature=285, timestamp=1764364324}", weatherData.get(16).toString());
        Assert.assertEquals("FitWeather{weather_report=2, condition=CLEAR, precipitation_probability=53, day_of_week=TUESDAY, high_temperature=301, low_temperature=286, timestamp=1764364324}", weatherData.get(17).toString());
        */

        FitLocalMessageBuilder weatherLocalMessage = GarminSupport.encodeWeather(weather);
        List<RecordData> weatherData = weatherLocalMessage.getRecordDataList();

        // Get all distinct record definitions
        Set<Integer> seenDefinitions = new HashSet<>();
        List<RecordDefinition> weatherDefinitions = new ArrayList<>(3);
        for (RecordData d : weatherData) {
            final int localMessageType = d.getRecordDefinition().getRecordHeader().getLocalMessageType();
            if (!seenDefinitions.contains(localMessageType)) {
                seenDefinitions.add(localMessageType);
                weatherDefinitions.add(d.getRecordDefinition());
            }
        }


        for (RecordDefinition weatherDefinition : weatherDefinitions) {
            MessageWriter writer = new MessageWriter();
            weatherDefinition.generateOutgoingPayload(writer);
            System.out.println("def: " + GB.hexdump(writer.getBytes()));
        }
        for (RecordData recordData : weatherData) {
            MessageWriter writer = new MessageWriter();
            recordData.generateOutgoingDataPayload(writer);
            System.out.println("data: " + GB.hexdump(writer.getBytes()));
        }

        Assert.assertEquals(3, weatherDefinitions.size());
        assertEquals("460001008011000100FD04860904860101010E01010D01010201000302840501020402840601010701020A04850B04851101000F0101080F07", weatherDefinitions.get(0));
        assertEquals("49000100800C000100FD04860101010201000302840402840501020601010701020F0101100488110100", weatherDefinitions.get(1));
        assertEquals("4A0001008008000100FD04860E01010D01010201000501020C0100110100", weatherDefinitions.get(2));
        Assert.assertEquals(18, weatherData.size());
        assertEquals("0600438CC424438CC4240F0A1904000C630BA40D1E00000026FFFFFF86020A477265656E2048696C6C0000000000", weatherData.get(0));
        assertEquals("0901FFFFFFFF0A00001E1748320A0A7F40000000FF", weatherData.get(1));
        assertEquals("0901FFFFFFFF0B00001F1872330B0B7F40400000FF", weatherData.get(2));
        assertEquals("0901FFFFFFFF0C000020199C340C0C7F40800000FF", weatherData.get(3));
        assertEquals("0901FFFFFFFF0D0000211AC6350D0D7F40A00000FF", weatherData.get(4));
        assertEquals("0901FFFFFFFF0E0000221BF0360E0E7F40C00000FF", weatherData.get(5));
        assertEquals("0901FFFFFFFF0F0000231D1A370F0F7F40E00000FF", weatherData.get(6));
        assertEquals("0901FFFFFFFF100000241E443810107F41000000FF", weatherData.get(7));
        assertEquals("0901FFFFFFFF110000251F6E3911117F41100000FF", weatherData.get(8));
        assertEquals("0901FFFFFFFF1200002620983A12127F41200000FF", weatherData.get(9));
        assertEquals("0901FFFFFFFF1300002721C23B13137F41300000FF", weatherData.get(10));
        assertEquals("0901FFFFFFFF1400002822EC3C14147F41400000FF", weatherData.get(11));
        assertEquals("0901FFFFFFFF1500002924163D15157F41500000FF", weatherData.get(12));
        assertEquals("0A02438CC4240A1904630502", weatherData.get(13));
        assertEquals("0A02438CC4240A190032FF03", weatherData.get(14));
        assertEquals("0A02438CC4240B1A0033FF03", weatherData.get(15));
        assertEquals("0A02438CC4240C1B0034FF03", weatherData.get(16));
        assertEquals("0A02438CC4240D1C0035FF03", weatherData.get(17));
    }

    private static void assertEquals(final String expectedPayload, final RecordDefinition recordDefinition) {
        final MessageWriter writer = new MessageWriter();
        recordDefinition.generateOutgoingPayload(writer);
        Assert.assertEquals(
                expectedPayload,
                GB.hexdump(writer.getBytes())
        );
    }

    private static void assertEquals(final String expectedPayload, final RecordData recordData) {
        final MessageWriter writer = new MessageWriter();
        recordData.generateOutgoingDataPayload(writer);
        Assert.assertEquals(
                expectedPayload,
                GB.hexdump(writer.getBytes())
        );
    }

    @NonNull
    private static WeatherSpec getWeatherSpec() {
        final WeatherSpec weather = new WeatherSpec();

        weather.setLocation("Green Hill");
        weather.setTimestamp(1764364324);
        weather.setCurrentTemp(15 + 273);
        weather.setTodayMinTemp(10 + 273);
        weather.setTodayMaxTemp(25 + 273);
        weather.setCurrentConditionCode(601); // snow
        weather.setCurrentCondition("Snowy");
        weather.setWindDirection(12);
        weather.setPrecipProbability(99);
        weather.setWindSpeed(10);
        weather.setFeelsLikeTemp(13 + 273);
        weather.setCurrentHumidity(70);
        weather.setLatitude(38.250139f);
        weather.setLongitude(-122.410806f);
        weather.setDewPoint(10 + 273);
        WeatherSpec.AirQuality airQuality = new WeatherSpec.AirQuality();
        airQuality.setAqi(50);
        weather.setAirQuality(airQuality);
        weather.setCurrentHumidity(30);

        weather.setHourly(new ArrayList<>());
        for (int i = 0; i < 24; i++) {
            final WeatherSpec.Hourly gbForecast = new WeatherSpec.Hourly();
            gbForecast.setTemp(10 + i + 273);
            gbForecast.setConditionCode(800); // clear
            gbForecast.setPrecipProbability(50 + i);
            gbForecast.setWindDirection(30 + i);
            gbForecast.setWindSpeed(20 + i);
            gbForecast.setHumidity(10 + i);
            gbForecast.setUvIndex(2 + i);

            weather.getHourly().add(gbForecast);
        }

        weather.setForecasts(new ArrayList<>());
        for (int i = 0; i < 5; i++) {
            final WeatherSpec.Daily gbForecast = new WeatherSpec.Daily();
            gbForecast.setMinTemp(10 + i + 273);
            gbForecast.setMaxTemp(25 + i + 273);
            gbForecast.setConditionCode(800); // clear
            gbForecast.setPrecipProbability(50 + i);
            WeatherSpec.AirQuality airQualityDaily = new WeatherSpec.AirQuality();
            airQualityDaily.setAqi(120 + i);
            gbForecast.setAirQuality(airQualityDaily);
            weather.getForecasts().add(gbForecast);
        }

        return weather;
    }
}
