/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class MoyoungWeatherForecast {
    public final byte conditionId;
    public final byte minTemp;
    public final byte maxTemp;

    public MoyoungWeatherForecast(byte conditionId, byte minTemp, byte maxTemp) {
        this.conditionId = conditionId;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public MoyoungWeatherForecast(WeatherSpec.Daily forecast) {
        conditionId = MoyoungConstants.openWeatherConditionToMoyoungConditionId(forecast.getConditionCode());
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            minTemp = (byte) WeatherUtils.celsiusToFahrenheit(forecast.getMinTemp() - 273); // Kelvin -> Fahrenheit
            maxTemp = (byte) WeatherUtils.celsiusToFahrenheit(forecast.getMaxTemp() - 273); // Kelvin -> Fahrenheit
        } else {
            minTemp = (byte) (forecast.getMinTemp() - 273); // Kelvin -> Celcius
            maxTemp = (byte) (forecast.getMaxTemp() - 273); // Kelvin -> Celcius
        }
    }
}
