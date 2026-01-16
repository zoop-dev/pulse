package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.http.HttpAppsSettingsHandler.FirmwareWeatherSetting;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

@SuppressWarnings("SimplifiableAssertion")
public class HttpAppsSettingsHandlerTest extends TestBase {
    private static final Gson GSON = new GsonBuilder().create();

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testBatchWeatherSettingsRequest() {
        // Payload source: https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/5653#issuecomment-9886832

        // Prepare the query parameters
        final Map<String, String> query = new HashMap<>();
        query.put("mode", "BATCH");
        query.put("settingName", "firmware_weather_hourly_weather,firmware_weather_daily_weather,firmware_weather_hourly_air_quality,firmware_weather_daily_air_quality,firmware_weather_daily_tide,firmware_weather_daily_indices");
        query.put("deviceSource", "1234567");
        query.put("format", "true");

        // Call the handler
        final String result = HttpAppsSettingsHandler.handleHttpRequest("/apps/com.huami.midong/settings", query);

        // Verify result is not null
        Assert.assertNotNull("Result should not be null", result);

        // Parse the JSON result
        final Type type = new TypeToken<Map<String, FirmwareWeatherSetting>>(){}.getType();
        final Map<String, FirmwareWeatherSetting> settings = GSON.fromJson(result, type);

        // Verify all settings are present
        Assert.assertTrue("Should contain firmware_weather_daily_tide", settings.containsKey("firmware_weather_daily_tide"));
        Assert.assertTrue("Should contain firmware_weather_hourly_air_quality", settings.containsKey("firmware_weather_hourly_air_quality"));
        Assert.assertTrue("Should contain firmware_weather_daily_weather", settings.containsKey("firmware_weather_daily_weather"));
        Assert.assertTrue("Should contain firmware_weather_daily_indices", settings.containsKey("firmware_weather_daily_indices"));
        Assert.assertTrue("Should contain firmware_weather_daily_air_quality", settings.containsKey("firmware_weather_daily_air_quality"));
        Assert.assertTrue("Should contain firmware_weather_hourly_weather", settings.containsKey("firmware_weather_hourly_weather"));

        // Verify firmware_weather_daily_tide
        FirmwareWeatherSetting dailyTide = settings.get("firmware_weather_daily_tide");
        Assert.assertEquals("firmware_weather_daily_tide -> normal -> trigger_on_demand_enabled", true, dailyTide.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_tide -> normal -> trigger_default_cooldown_minutes", 480, dailyTide.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_tide -> normal -> schedule_enabled", true, dailyTide.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_tide -> normal -> schedule_interval_minutes", 960, dailyTide.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_tide -> power_saving -> trigger_on_demand_enabled", true, dailyTide.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_tide -> power_saving -> trigger_default_cooldown_minutes", 480, dailyTide.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_tide -> power_saving -> schedule_enabled", true, dailyTide.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_tide -> power_saving -> schedule_interval_minutes", 960, dailyTide.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_tide -> alert_expire_minutes", 1440, dailyTide.alert_expire_minutes());

        // Verify firmware_weather_hourly_air_quality
        FirmwareWeatherSetting hourlyAirQuality = settings.get("firmware_weather_hourly_air_quality");
        Assert.assertEquals("firmware_weather_hourly_air_quality -> normal -> trigger_on_demand_enabled", true, hourlyAirQuality.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> normal -> trigger_default_cooldown_minutes", 30, hourlyAirQuality.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> normal -> schedule_enabled", true, hourlyAirQuality.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> normal -> schedule_interval_minutes", 120, hourlyAirQuality.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> power_saving -> trigger_on_demand_enabled", true, hourlyAirQuality.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> power_saving -> trigger_default_cooldown_minutes", 90, hourlyAirQuality.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> power_saving -> schedule_enabled", true, hourlyAirQuality.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> power_saving -> schedule_interval_minutes", 360, hourlyAirQuality.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_hourly_air_quality -> alert_expire_minutes", 180, hourlyAirQuality.alert_expire_minutes());

        // Verify firmware_weather_daily_weather
        FirmwareWeatherSetting dailyWeather = settings.get("firmware_weather_daily_weather");
        Assert.assertEquals("firmware_weather_daily_weather -> normal -> trigger_on_demand_enabled", true, dailyWeather.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_weather -> normal -> trigger_default_cooldown_minutes", 480, dailyWeather.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_weather -> normal -> schedule_enabled", true, dailyWeather.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_weather -> normal -> schedule_interval_minutes", 960, dailyWeather.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_weather -> power_saving -> trigger_on_demand_enabled", true, dailyWeather.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_weather -> power_saving -> trigger_default_cooldown_minutes", 480, dailyWeather.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_weather -> power_saving -> schedule_enabled", true, dailyWeather.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_weather -> power_saving -> schedule_interval_minutes", 960, dailyWeather.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_weather -> alert_expire_minutes", 1440, dailyWeather.alert_expire_minutes());

        // Verify firmware_weather_daily_indices
        FirmwareWeatherSetting dailyIndices = settings.get("firmware_weather_daily_indices");
        Assert.assertEquals("firmware_weather_daily_indices -> normal -> trigger_on_demand_enabled", true, dailyIndices.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_indices -> normal -> trigger_default_cooldown_minutes", 480, dailyIndices.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_indices -> normal -> schedule_enabled", true, dailyIndices.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_indices -> normal -> schedule_interval_minutes", 960, dailyIndices.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_indices -> power_saving -> trigger_on_demand_enabled", true, dailyIndices.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_indices -> power_saving -> trigger_default_cooldown_minutes", 480, dailyIndices.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_indices -> power_saving -> schedule_enabled", true, dailyIndices.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_indices -> power_saving -> schedule_interval_minutes", 960, dailyIndices.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_indices -> alert_expire_minutes", 1440, dailyIndices.alert_expire_minutes());

        // Verify firmware_weather_daily_air_quality
        FirmwareWeatherSetting dailyAirQuality = settings.get("firmware_weather_daily_air_quality");
        Assert.assertEquals("firmware_weather_daily_air_quality -> normal -> trigger_on_demand_enabled", true, dailyAirQuality.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_air_quality -> normal -> trigger_default_cooldown_minutes", 480, dailyAirQuality.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_air_quality -> normal -> schedule_enabled", true, dailyAirQuality.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_air_quality -> normal -> schedule_interval_minutes", 960, dailyAirQuality.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_air_quality -> power_saving -> trigger_on_demand_enabled", true, dailyAirQuality.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_daily_air_quality -> power_saving -> trigger_default_cooldown_minutes", 480, dailyAirQuality.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_daily_air_quality -> power_saving -> schedule_enabled", true, dailyAirQuality.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_daily_air_quality -> power_saving -> schedule_interval_minutes", 960, dailyAirQuality.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_daily_air_quality -> alert_expire_minutes", 960, dailyAirQuality.alert_expire_minutes());

        // Verify firmware_weather_hourly_weather
        FirmwareWeatherSetting hourlyWeather = settings.get("firmware_weather_hourly_weather");
        Assert.assertEquals("firmware_weather_hourly_weather -> normal -> trigger_on_demand_enabled", true, hourlyWeather.normal().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_hourly_weather -> normal -> trigger_default_cooldown_minutes", 30, hourlyWeather.normal().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_hourly_weather -> normal -> schedule_enabled", true, hourlyWeather.normal().schedule_enabled());
        Assert.assertEquals("firmware_weather_hourly_weather -> normal -> schedule_interval_minutes", 120, hourlyWeather.normal().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_hourly_weather -> power_saving -> trigger_on_demand_enabled", true, hourlyWeather.power_saving().trigger_on_demand_enabled());
        Assert.assertEquals("firmware_weather_hourly_weather -> power_saving -> trigger_default_cooldown_minutes", 90, hourlyWeather.power_saving().trigger_default_cooldown_minutes());
        Assert.assertEquals("firmware_weather_hourly_weather -> power_saving -> schedule_enabled", true, hourlyWeather.power_saving().schedule_enabled());
        Assert.assertEquals("firmware_weather_hourly_weather -> power_saving -> schedule_interval_minutes", 360, hourlyWeather.power_saving().schedule_interval_minutes());
        Assert.assertEquals("firmware_weather_hourly_weather -> alert_expire_minutes", 180, hourlyWeather.alert_expire_minutes());
    }
}
