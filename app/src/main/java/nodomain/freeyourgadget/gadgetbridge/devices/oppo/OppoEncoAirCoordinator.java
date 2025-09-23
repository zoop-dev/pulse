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

import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigSide;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigValue;

public class OppoEncoAirCoordinator extends OppoHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("OPPO Enco Air", Pattern.LITERAL);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_oppo_enco_air;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    protected Map<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> getTouchOptions() {
        return new LinkedHashMap<>() {{
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_2), Arrays.asList(
                    TouchConfigValue.OFF,
                    TouchConfigValue.PLAY_PAUSE,
                    TouchConfigValue.PREVIOUS,
                    TouchConfigValue.NEXT,
                    TouchConfigValue.VOICE_ASSISTANT
            ));
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_3), Arrays.asList(
                    TouchConfigValue.OFF,
                    TouchConfigValue.VOICE_ASSISTANT,
                    TouchConfigValue.GAME_MODE
            ));
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.HOLD), Arrays.asList(
                    TouchConfigValue.OFF,
                    TouchConfigValue.VOLUME_UP,
                    TouchConfigValue.VOLUME_DOWN
            ));

            // Right side is the same
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.TAP_2), get(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_2)));
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.TAP_3), get(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_3)));
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.HOLD), get(Pair.create(TouchConfigSide.LEFT, TouchConfigType.HOLD)));
        }};
    }
}
