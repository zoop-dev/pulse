/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.vesc;

import android.app.Activity;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vesc.VescDeviceSupport;

public class VescCoordinator extends AbstractBLEDeviceCoordinator {
    public final static String UUID_SERVICE_SERIAL_HM10 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_CHARACTERISTIC_SERIAL_TX_HM10 = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public final static String UUID_CHARACTERISTIC_SERIAL_RX_HM10 = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public final static String UUID_SERVICE_SERIAL_NRF = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_CHARACTERISTIC_SERIAL_TX_NRF = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final static String UUID_CHARACTERISTIC_SERIAL_RX_NRF = "0000ffe1-0000-1000-8000-00805f9b34fb";

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
            R.xml.devicesettings_vesc
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return VescDeviceSupport.class;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        if(!candidate.getName().toLowerCase().contains("vesc")){
            return false;
        }
        ParcelUuid[] uuids = candidate.getServiceUuids();
        for(ParcelUuid uuid : uuids){
            if(uuid.getUuid().toString().equals(UUID_SERVICE_SERIAL_NRF)){
                return true;
            }else if(uuid.getUuid().toString().equals(UUID_SERVICE_SERIAL_HM10)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching(final GBDevice device) {
        return true;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public String getManufacturer() {
        return "Benjamin Vedder";
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return VescControlActivity.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_vesc;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_vesc;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.UNKNOWN;
    }
}
