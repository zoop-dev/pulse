package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.http;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class HttpAppsSettingsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpAppsSettingsHandler.class);
    private static final Gson GSON = new GsonBuilder().create();

    @SuppressWarnings("unused")
    @Nullable
    public static String handleHttpRequest(final String path, final Map<String, String> query) {
        // https://api-mifit.huami.com/apps/com.huami.midong/settings?mode=BATCH&settingName=firmware_weather_hourly_weather,firmware_weather_daily_weather,firmware_weather_hourly_air_quality,firmware_weather_daily_air_quality,firmware_weather_daily_tide,firmware_weather_daily_indices&deviceSource=<redacted-7-digits>&format=true

        if (path.equals("/apps/com.huami.midong/settings")) {
            final String mode = query.get("mode"); // BATCH
            final String settingName = query.get("settingName");
            final String deviceSource = query.get("deviceSource");
            final String format = query.get("format"); // true

            if (!"BATCH".equals(mode)) {
                LOG.error("Unknown apps settings mode {}", mode);
                return null;
            }

            if (StringUtils.isNullOrEmpty(settingName)) {
                LOG.error("No settingName");
                return null;
            }

            final String[] settings = settingName.split(",");

            final Map<String, FirmwareWeatherSetting> outSettings = new HashMap<>();
            for (String setting : settings) {
                final FirmwareWeatherSetting settingValue = getFirmwareWeatherSetting(setting);
                if (settingValue == null) {
                    LOG.error("Unknown setting {}", setting);
                    return null;
                }

                outSettings.put(setting, settingValue);
            }

            return GSON.toJson(outSettings);
        }

        LOG.error("Unknown apps settings path {}", path);
        return null;
    }

    private static FirmwareWeatherSetting getFirmwareWeatherSetting(final String setting) {
        return switch (setting) {
            case "firmware_weather_hourly_weather", "firmware_weather_hourly_air_quality" -> new FirmwareWeatherSetting(
                    new FirmwareWeatherModeSetting(true, 30, true, 120),
                    new FirmwareWeatherModeSetting(true, 90, true, 360),
                    180
            );
            case "firmware_weather_daily_air_quality" -> new FirmwareWeatherSetting(
                    new FirmwareWeatherModeSetting(true, 480, true, 960),
                    new FirmwareWeatherModeSetting(true, 480, true, 960),
                    960
            );
            case "firmware_weather_daily_weather", "firmware_weather_daily_tide", "firmware_weather_daily_indices" ->
                    new FirmwareWeatherSetting(
                            new FirmwareWeatherModeSetting(true, 480, true, 960),
                            new FirmwareWeatherModeSetting(true, 480, true, 960),
                            1440
                    );
            default -> null;
        };
    }

    public record FirmwareWeatherSetting(
            FirmwareWeatherModeSetting normal,
            FirmwareWeatherModeSetting power_saving,
            int alert_expire_minutes
    ) {
    }

    public record FirmwareWeatherModeSetting(
            boolean trigger_on_demand_enabled,
            int trigger_default_cooldown_minutes,
            boolean schedule_enabled,
            int schedule_interval_minutes
    ) {
    }
}
