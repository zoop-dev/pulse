/*  Copyright (C) 2018-2024 Andreas Shimokawa, Damien Gaignon, Daniel Dakhno,
    Daniele Gobbetti, José Rebelo, mamucho, maxirnilian, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.watch9;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.watch9.Watch9DeviceSupport;

public class Watch9DeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid watch9Service = new ParcelUuid(Watch9Constants.UUID_SERVICE_WATCH9);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(watch9Service).build();
        return Collections.singletonList(filter);
    }

    /** @noinspection RedundantIfStatement*/
    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        String macAddress = candidate.getMacAddress().toUpperCase();
        String deviceName = candidate.getName().toUpperCase();
        if (candidate.supportsService(Watch9Constants.UUID_SERVICE_WATCH9)) {
            return true;
            // add support for Watch X non-plus (same MAC address)
            // add support for Watch X Plus (same MAC address)
        } else if ((macAddress.startsWith("1C:87:79")) && ((!deviceName.equalsIgnoreCase("WATCH X")) && (!deviceName.equalsIgnoreCase("WATCH XPLUS")))) {
            return true;
        } else if (deviceName.equals("WATCH 9")) {
            return true;
        }
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return Watch9PairingActivity.class;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 3; // FIXME - check the real value
    }

    @Override
    public String getManufacturer() {
        return "Lenovo";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return Watch9DeviceSupport.class;
    }

    @Override
    public EnumSet<ServiceDeviceSupport.Flags> getInitialFlags() {
        return EnumSet.of(ServiceDeviceSupport.Flags.THROTTLING, ServiceDeviceSupport.Flags.BUSY_CHECKING);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_watch9;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getCalibrationActivity() {
        return Watch9CalibrationActivity.class;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.WATCH;
    }
}
