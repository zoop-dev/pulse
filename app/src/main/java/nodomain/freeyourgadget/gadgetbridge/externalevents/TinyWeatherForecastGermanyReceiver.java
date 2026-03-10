/*  Copyright (C) 2020-2024 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class TinyWeatherForecastGermanyReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(TinyWeatherForecastGermanyReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            LOG.warn("Got null intent");
            return;
        }
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            LOG.warn("Got null bundle");
            return;
        }

        LOG.debug("Got intent for {}", intent.getAction());

        try {
            bundle.setClassLoader(WeatherSpec.class.getClassLoader());

            final WeatherSpec weatherSpec;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                weatherSpec = bundle.getParcelable("WeatherSpec", WeatherSpec.class);
            } else {
                weatherSpec = bundle.getParcelable("WeatherSpec");
            }

            if (weatherSpec == null) {
                LOG.warn("Got null WeatherSpec");
                return;
            }

            final ArrayList<WeatherSpec> weatherSpecs = new ArrayList<>(Collections.singletonList(weatherSpec));
            weatherSpec.setTimestamp((int) (System.currentTimeMillis() / 1000));
            Weather.setWeatherSpec(weatherSpecs);
            GBApplication.deviceService().onSendWeather();
        } catch (Exception e) {
            GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);

            for (String key : bundle.keySet()) {
                try {
                    LOG.debug("WeatherSpec {} -> {}", key, bundle.get(key));
                } catch (final Exception e2) {
                    LOG.warn("Failed to log WeatherSpec key {}", key, e2);
                }
            }
        }
    }
}