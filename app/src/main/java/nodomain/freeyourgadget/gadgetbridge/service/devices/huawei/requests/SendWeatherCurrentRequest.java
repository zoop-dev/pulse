/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendWeatherCurrentRequest extends Request {
    Weather.Settings settings;
    WeatherSpec weatherSpec;

    public SendWeatherCurrentRequest(HuaweiSupportProvider support, Weather.Settings settings, WeatherSpec weatherSpec) {
        super(support);
        this.serviceId = Weather.id;
        this.commandId = Weather.CurrentWeatherRequest.id;
        this.settings = settings;
        this.weatherSpec = weatherSpec;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        Weather.HuaweiTemperatureFormat temperatureFormat = Weather.HuaweiTemperatureFormat.CELSIUS;
        String unit = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (unit.equals(GBApplication.getContext().getString(R.string.p_unit_imperial)))
            temperatureFormat = Weather.HuaweiTemperatureFormat.FAHRENHEIT;
        try {
            Short pm25 = null;
            Short aqi = null;
            if (weatherSpec.getAirQuality() != null) {
                pm25 = (short) weatherSpec.getAirQuality().getPm25(); // TODO: does this work?
                aqi = (short) weatherSpec.getAirQuality().getAqi();
            }
            return new Weather.CurrentWeatherRequest(
                    this.paramsProvider,
                    settings,
                    supportProvider.openWeatherMapConditionCodeToHuaweiIcon(weatherSpec.getCurrentConditionCode()),
                    (byte) weatherSpec.getWindDirection(),
                    (byte) weatherSpec.windSpeedAsBeaufort(),
                    (byte) (weatherSpec.getTodayMinTemp() - 273),
                    (byte) (weatherSpec.getTodayMaxTemp() - 273),
                    pm25,
                    weatherSpec.getLocation(),
                    (byte) (weatherSpec.getCurrentTemp() - 273),
                    temperatureFormat,
                    aqi,
                    weatherSpec.getTimestamp(),
                    weatherSpec.getUvIndex(),
                    "Gadgetbridge",
                    (byte) weatherSpec.getCurrentHumidity(),
                    (int) weatherSpec.getWindSpeed(),
                    (weatherSpec.getFeelsLikeTemp() - 273)
                ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
