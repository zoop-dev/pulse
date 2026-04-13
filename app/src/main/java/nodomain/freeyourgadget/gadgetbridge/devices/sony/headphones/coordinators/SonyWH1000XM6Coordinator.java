/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SonyWH1000XM6Coordinator extends SonyHeadphonesCoordinator {
    @Override
    public boolean isExperimental() {
        // Battery, equalizer, and quite a few others probably not working
        return true;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(".*WH-1000XM6.*");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wh_1000xm6;
    }

    @Override
    public Set<SonyHeadphonesCapabilities> getCapabilities() {
        return new HashSet<>(Arrays.asList(
                SonyHeadphonesCapabilities.BatterySingle,
                SonyHeadphonesCapabilities.AmbientSoundControl,
                SonyHeadphonesCapabilities.SpeakToChatEnabled,
                SonyHeadphonesCapabilities.SpeakToChatConfig,
                // TODO SonyHeadphonesCapabilities.AudioUpsampling, // DSEE Extreme
                // TODO SonyHeadphonesCapabilities.VoiceNotifications,
                SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff,
                // TODO SonyHeadphonesCapabilities.TouchSensorSingle,
                // TODO 9 bands SonyHeadphonesCapabilities.EqualizerWithCustomBands,
                SonyHeadphonesCapabilities.PauseWhenTakenOff
        ));
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.HEADPHONES;
    }
}
