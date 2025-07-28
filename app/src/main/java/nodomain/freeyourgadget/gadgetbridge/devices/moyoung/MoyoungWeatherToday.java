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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class MoyoungWeatherToday {
    private static final Logger LOG = LoggerFactory.getLogger(MoyoungWeatherToday.class);

    public final byte conditionId;
    public final byte currentTemp;
    public final Short pm25; // (*)
    public final String lunar_or_festival; // (*)
    public final String city; // (*)

    public MoyoungWeatherToday(byte conditionId, byte currentTemp, @Nullable Short pm25, @NonNull String lunar_or_festival, @NonNull String city) {
        if (lunar_or_festival.length() == 4) {
            this.lunar_or_festival = lunar_or_festival;
        } else {
            LOG.warn("lunar_or_festival should be 4 bytes, not {}, ignoring", lunar_or_festival.length());
            this.lunar_or_festival = StringUtils.pad("", 4);
        }
        if (city.length() == 4) {
            this.city = city;
        } else {
            LOG.warn("city should be 4 bytes, not {}, ignoring", city.length());
            this.city = StringUtils.pad("", 4);
        }
        this.conditionId = conditionId;
        this.currentTemp = currentTemp;
        this.pm25 = pm25;
    }

    public MoyoungWeatherToday(WeatherSpec weatherSpec) {
        conditionId = MoyoungConstants.openWeatherConditionToMoyoungConditionId(weatherSpec.getCurrentConditionCode());
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            currentTemp = (byte) WeatherUtils.celsiusToFahrenheit(weatherSpec.getCurrentTemp() - 273); // Kelvin -> Fahrenheit
        } else {
            currentTemp = (byte) (weatherSpec.getCurrentTemp() - 273); // Kelvin -> Celcius
        }
        pm25 = null;
        lunar_or_festival = StringUtils.pad("", 4);
        city = StringUtils.pad(weatherSpec.getLocation().substring(0, 4), 4);
    }
}
