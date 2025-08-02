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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.audio.AudioInfo;
import nodomain.freeyourgadget.gadgetbridge.util.audio.MusicUtils;

public class ZeppOsMusicInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMusicInstallHandler.class);

    protected final Context mContext;
    private final Uri mUri;
    private final AudioInfo mAudioInfo;

    public ZeppOsMusicInstallHandler(final Uri uri, final Context context) {
        this.mContext = context;
        this.mUri = uri;
        this.mAudioInfo = MusicUtils.audioInfoFromUri(context, uri);
    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return mAudioInfo != null &&
                "audio/mpeg".equals(mAudioInfo.getMimeType()) &&
                mAudioInfo.getExtension().equals("mp3");
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof ZeppOsCoordinator)) {
            LOG.warn("Coordinator is not a ZeppOsCoordinator: {}", coordinator.getClass());
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }
        final ZeppOsCoordinator zeppOsCoordinator = (ZeppOsCoordinator) coordinator;
        if (!zeppOsCoordinator.supportsMusicUpload(device)) {
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

        installActivity.setInfoText(mContext.getString(R.string.fw_upgrade_notice, mContext.getString(R.string.menuitem_music)));
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
    }

    public AudioInfo getAudioInfo() {
        return mAudioInfo;
    }

    @Nullable
    public byte[] readFileBytes() {
        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(mUri, mContext);
        } catch (final IOException e) {
            LOG.error("Failed to get uri", e);
            return null;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            // FIXME we should be able to stream this
            return FileUtils.readAll(in, 50 * 1024 * 1024); // 50MB
        } catch (final Exception e) {
            LOG.error("Failed to read music file", e);
        }

        return null;
    }

    private GenericItem createInstallItem(final GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final String firmwareName = mContext.getString(
                R.string.installhandler_firmware_name,
                mContext.getString(coordinator.getDeviceNameResource()),
                mContext.getString(R.string.menuitem_music),
                mAudioInfo.getFileName()
        );
        return new GenericItem(firmwareName);
    }
}
