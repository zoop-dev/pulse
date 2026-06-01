/*  Copyright (C) 2025 Vitaliy Tomin, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.igpsport;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;


public class IGPSportRouteInstallHandler implements InstallHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IGPSportRouteInstallHandler.class);

    protected final Context mContext;
    private byte[] rawBytes = null;
    private String filename;
    private String extension;
    private List<String> routeExtensions = Arrays.asList("cnx", "gpx", "fit", "tcx", "xml");

    public IGPSportRouteInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
            filename = uriHelper.getFileName();
            int strLength = filename.lastIndexOf(".");
            if(strLength > 0)
                extension = filename.substring(strLength + 1).toLowerCase();
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            rawBytes = FileUtils.readAll(in, 1024 * 1024); // 1MB

            try (ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes)) {

            } catch (final IOException e) {
                LOG.error("Failed to read xml", e);
            }

        } catch (final Exception e) {
            LOG.error("Failed to read file", e);
        }
    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        if (routeExtensions.contains(extension))
            return true;
        else
            return false;
    }

    @Override
    public void validateInstallation(@NonNull InstallActivity installActivity, @NonNull GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof IGPSportAbstractCoordinator)) {
            LOG.warn("Coordinator is not a IGPSportCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        final GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(coordinator.getDefaultIconResource());

        if (rawBytes == null) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }

        final StringBuilder builder = new StringBuilder();
        final String cnxRoute = mContext.getString(R.string.kind_gpx_route);
        builder.append(mContext.getString(R.string.fw_upgrade_notice, filename));
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);

    }

    @Override
    public void onStartInstall(@NonNull GBDevice device) {

    }

    private GenericItem createInstallItem(final GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final String firmwareName = mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(R.string.kind_gpx_route),
                filename
        );
        return new GenericItem(firmwareName);
    }

    public byte[] getBytes() {
        return rawBytes;
    }

    public int getSize() {
        return rawBytes.length;
    }

    public String getFilename() {
        return filename;
    }

    public String getExtension() {
        return extension;
    }
}
