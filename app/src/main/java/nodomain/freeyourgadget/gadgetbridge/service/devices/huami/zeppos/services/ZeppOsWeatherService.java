/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;

public class ZeppOsWeatherService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsWeatherService.class);

    private static final short ENDPOINT = 0x000e;

    private static final byte CMD_SET_DEFAULT_LOCATION = 0x09;
    private static final byte CMD_DEFAULT_LOCATION_ACK = 0x0a;

    public ZeppOsWeatherService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case CMD_DEFAULT_LOCATION_ACK:
                LOG.info("Weather default location ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected weather byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void onSendWeather() {
        final WeatherSpec weatherSpec = Weather.getWeatherSpec();
        if (weatherSpec == null) {
            LOG.warn("No weather found in singleton");
            return;
        }

        // Weather is not sent directly to the bands, they send HTTP requests for each location.
        // When we have a weather update, set the default location to that location on the band.
        // TODO: Support for multiple weather locations

        float lat = weatherSpec.getLatitude();
        float lon = weatherSpec.getLongitude();
        if (lat == 0f && lon == 0f) {
            // Some weather providers don't populate WeatherSpec coords on broadcast.
            // Fall back to user-configured location pref so the watch gets a valid
            // default location and accepts subsequent v5 HTTP responses (issue #5653).
            lat = GBApplication.getPrefs().getFloat("location_latitude", 0f);
            lon = GBApplication.getPrefs().getFloat("location_longitude", 0f);
        }
        final String coordKey = String.format(Locale.ROOT, "%.4f,%.4f", lat, lon);
        final long locationKeyId = ((long) coordKey.hashCode()) & 0xFFFFFFFFL;
        final String locationKey = String.format(Locale.ROOT, "%.4f,%.4f,xiaomi_accu:%d", lat, lon, locationKeyId);
        final String locationName = weatherSpec.getLocation();

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(CMD_SET_DEFAULT_LOCATION);
            baos.write((byte) 0x02); // ? 2 for current, 4 for default
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write(locationKey.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00); // ?
            baos.write(locationName.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00); // ?

            write("set weather location", baos.toByteArray());
        } catch (final Exception e) {
            LOG.error("Failed to set weather location", e);
        }
    }
}
