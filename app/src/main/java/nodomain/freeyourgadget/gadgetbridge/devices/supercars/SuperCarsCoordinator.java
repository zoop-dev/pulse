/*  Copyright (C) 2022-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
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
package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.supercars.SuperCarsSupport;

public class SuperCarsCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("QCAR-.*");
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return SuperCarsSupport.class;
    }

    @Override
    public String getManufacturer() {
        return "Brand Base";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_super_cars;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_supercars;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_supercars_disabled;
    }

    @Override
    public List<DeviceCardAction> getCustomActions() {
        return Collections.singletonList(new ControlDeviceCardAction());
    }

    private static final class ControlDeviceCardAction implements DeviceCardAction {
        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_steering_wheel;
        }

        @Override
        public String getDescription(final GBDevice device, final Context context) {
            return context.getString(R.string.remote_control);
        }

        @Override
        public void onClick(final GBDevice device, final Context context) {
            final Intent startIntent = new Intent(context, ControlActivity.class);
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            context.startActivity(startIntent);
        }
    }
}
