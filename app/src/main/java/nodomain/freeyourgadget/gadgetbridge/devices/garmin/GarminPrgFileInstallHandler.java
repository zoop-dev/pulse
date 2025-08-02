/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminPrgFile;

public class GarminPrgFileInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GarminPrgFileInstallHandler.class);

    private final Context mContext;
    private final GarminPrgFile prgFile;

    public GarminPrgFileInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;
        this.prgFile = new GarminPrgFile(uri, context);
    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return prgFile.isValid();
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            installActivity.setCloseEnabled(true);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof GarminCoordinator garminCoordinator)) {
            LOG.warn("Coordinator is not a GarminCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            installActivity.setCloseEnabled(true);
            return;
        }

        // FIXME this might not be the correct capability
        if (!garminCoordinator.supports(device, GarminCapability.CONNECTIQ_APP_MANAGEMENT)) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            installActivity.setCloseEnabled(true);
            return;
        }

        // TODO parse prg and identify type and name
        final GenericItem fwItem = new GenericItem(mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                "PRG",
                prgFile.getName()
        ));
        fwItem.setIcon(coordinator.getDefaultIconResource());

        installActivity.setInfoText(mContext.getString(R.string.fw_upgrade_notice, "PRG"));
        installActivity.setInstallItem(fwItem);

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            installActivity.setCloseEnabled(true);
            return;
        }

        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    public byte[] getRawBytes() {
        return prgFile.getBytes();
    }
}
