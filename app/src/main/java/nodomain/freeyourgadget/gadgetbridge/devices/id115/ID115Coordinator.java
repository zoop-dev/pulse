/*  Copyright (C) 2018-2024 Andreas Shimokawa, Damien Gaignon, Daniel Dakhno,
    Daniele Gobbetti, José Rebelo, Petr Vaněk, Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.devices.id115;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.id115.ID115Support;

public class ID115Coordinator extends AbstractBLEDeviceCoordinator {
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid service = new ParcelUuid(ID115Constants.UUID_SERVICE_ID115);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(ID115Constants.UUID_SERVICE_ID115);
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new ID115SampleProvider(device, session);
    }

    @Override
    public String getManufacturer() {
        return "VeryFit";
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_screenorientation,
                R.xml.devicesettings_transliteration
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return ID115Support.class;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_id115;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_h30_h10;
    }
}
