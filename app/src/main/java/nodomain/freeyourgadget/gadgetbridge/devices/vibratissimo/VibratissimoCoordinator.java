/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Damien
    Gaignon, Daniel Dakhno, Daniele Gobbetti, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.vibratissimo;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.EnumSet;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vibratissimo.VibratissimoSupport;

public class VibratissimoCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Vibratissimo.*");
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "Amor AG";
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false; // hmmm well, it has a temperature sensor :D
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return VibratissimoSupport.class;
    }

    @Override
    public EnumSet<ServiceDeviceSupport.Flags> getInitialFlags() {
        return EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
        // nothing to delete, yet
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_vibratissimo;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_lovetoy;
    }
}
