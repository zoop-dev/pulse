/*  Copyright (C) 2025 Ilya Nikitenkov

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei.freebudspro;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiFreebudsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFreebudsSupport;

public class HuaweiFreebudsProCoordinator extends HuaweiFreebudsCoordinator {

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("huawei freebuds pro.*", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Set<HuaweiHeadphonesCapabilities> getCapabilities() {
        return new HashSet<>(Arrays.asList(
                HuaweiHeadphonesCapabilities.NoiseCancellationModes,
                HuaweiHeadphonesCapabilities.InEarDetection,
                HuaweiHeadphonesCapabilities.AudioModes,
                HuaweiHeadphonesCapabilities.VoiceBoost
        ));
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return HuaweiFreebudsSupport.class;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HUAWEI_FREEBUDS_PRO;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_huawei_freebuds_pro;
    }
}
