/*  Copyright (C) 2025 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.zendure;


import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;

import nodomain.freeyourgadget.gadgetbridge.devices.SolarEquipmentStatusActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.zendure.SolarFlowDeviceSupport;


public class SolarFlowDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_solarflow;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_vesc;
    }

    @Override
    public String getManufacturer() {
        return "Zendure";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return SolarFlowDeviceSupport.class;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid solarflowService = new ParcelUuid(UUID.fromString("0000a002-0000-1000-8000-00805f9b34fb"));
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(solarflowService).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Zen.*");
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return SolarEquipmentStatusActivity.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_solar_panel1_peak_w,
                R.xml.devicesettings_solar_panel2_peak_w,
                R.xml.devicesettings_solar_panel3_peak_w,
                R.xml.devicesettings_solar_panel4_peak_w,
                R.xml.devicesettings_battery_minimum_charge,
                R.xml.devicesettings_battery_maximum_charge,
                R.xml.devicesettings_battery_allow_bypass,
                R.xml.devicesettings_output_power_grid,
                R.xml.devicesettings_offgrid_mode_on_off_eco,
                R.xml.devicesettings_always_on_display,
        };
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.PV_EQUIPMENT;
    }
}
