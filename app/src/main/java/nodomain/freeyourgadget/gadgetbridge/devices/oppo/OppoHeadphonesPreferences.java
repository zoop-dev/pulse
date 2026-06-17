/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.oppo;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigSide;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigType;

public class OppoHeadphonesPreferences {
    public static final String TOUCH_PREFIX= "oppo_touch__";

    public static final String LDAC = "pref_soundcore_ldac_mode";
    public static final String MULTIPOINT = "oppo_multipoint";
    public static final String GAME_MODE = "oppo_game_mode";
    public static final String ANC_SELECTOR = "noise_control_selector";

    public static String getTouchKey(final TouchConfigSide side, final TouchConfigType type) {
        return String.format(
                Locale.ROOT,
                "%s%s__%s",
                TOUCH_PREFIX,
                side.name().toLowerCase(Locale.ROOT),
                type.name().toLowerCase(Locale.ROOT)
        );
    }
}
