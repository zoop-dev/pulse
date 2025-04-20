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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.content.SharedPreferences;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class HuamiDevicePrefs extends DevicePrefs {
    public HuamiDevicePrefs(final SharedPreferences preferences, final GBDevice gbDevice) {
        super(preferences, gbDevice);
    }

    public byte getLanguageId() {
        byte languageCode = 0x02; // english default

        String localeString = getString("language", "auto");
        if (StringUtils.isBlank(localeString) || localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            localeString = language + "_" + country.toUpperCase();
        }

        final Integer id = HuamiLanguageType.idLookup.get(localeString);
        if (id != null) {
            languageCode = id.byteValue();
        }

        return languageCode;
    }
}
