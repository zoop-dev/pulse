/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiAppManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFwHelper;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWatchfaceManager;
import nodomain.freeyourgadget.gadgetbridge.util.audio.AudioInfo;

public class HuaweiInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiInstallHandler.class);

    private final Context context;
    protected final HuaweiFwHelper helper;

    boolean valid = false;

    public HuaweiInstallHandler(Uri uri, Context context) {
        this.context = context;
        this.helper = new HuaweiFwHelper(uri, context);
    }

    private HuaweiMusicUtils.FormatRestrictions getRestriction(HuaweiMusicUtils.MusicCapabilities capabilities, String ext) {
        List<HuaweiMusicUtils.FormatRestrictions> restrictions = capabilities.formatsRestrictions;
        if (restrictions == null)
            return null;

        for (HuaweiMusicUtils.FormatRestrictions r : restrictions) {
            if (ext.equals(r.getName())) {
                return r;
            }
        }
        return null;
    }

    //TODO: add proper checks
    private boolean checkMediaCompatibility(HuaweiMusicUtils.MusicCapabilities capabilities, AudioInfo currentMusicInfo) {
        if (capabilities == null) {
            LOG.error("No media info from device");
            return false;
        }
        String ext = currentMusicInfo.getExtension();

        List<String> supportedFormats = capabilities.supportedFormats;
        if (supportedFormats == null) {
            LOG.error("Format not supported {}", ext);
            return false;
        }
        if (!supportedFormats.contains(ext)) {
            LOG.error("Format not supported {}", ext);
            return false;
        }

        HuaweiMusicUtils.FormatRestrictions restrictions = getRestriction(capabilities, ext);
        if (restrictions == null) {
            LOG.info("no restriction for: {}", ext);
            return true;
        }

        LOG.info("bitrate {}", restrictions.bitrate);
        LOG.info("channels {}", restrictions.channels);
        LOG.info("musicEncode {}", restrictions.musicEncode);
        LOG.info("sampleRate {}", Arrays.toString(restrictions.sampleRates));
        LOG.info("unknownBitrate {}", restrictions.unknownBitrate);

        if (currentMusicInfo.getChannels() > restrictions.channels) {
            LOG.error("Not supported channels count {} > {}", currentMusicInfo.getChannels(), restrictions.channels);
            return false;
        }

        //TODO: check other restrictions.

        return true;
    }


    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        if (!(coordinator instanceof HuaweiCoordinatorSupplier huaweiCoordinatorSupplier)) {
            LOG.warn("Coordinator is not a HuaweiCoordinatorSupplier: {}", coordinator.getClass());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (helper.isFirmware) {
            this.valid = true; //NOTE: nothing to verify for now

            if (device.isBusy()) {
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected() || !device.isInitialized()) {
                LOG.error("Firmware cannot be uploaded(not connected or wrong device)");
                installActivity.setInfoText("Firmware cannot be uploaded (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("Firmware cannot be uploaded");
                installActivity.setInstallEnabled(false);
                return;
            }

            GenericItem installItem = new GenericItem();

            installItem.setName(helper.fwInfo.versionName);

            installItem.setDetails(helper.fwInfo.osVersion);

            installItem.setIcon(R.drawable.ic_firmware);

            installActivity.setInstallItem(installItem);

            installActivity.setInfoText(context.getString(R.string.fw_upgrade_notice_huawei, helper.fwInfo.versionName, device.getAliasOrName(), device.getFirmwareVersion()));

            installActivity.setInstallEnabled(true);

            LOG.debug("Initialized HuaweiInstallHandler: Firmware");
            return;
        }

        if (helper.isWatchface()) {

            HuaweiWatchfaceManager.WatchfaceDescription description = helper.getWatchfaceDescription();

            HuaweiWatchfaceManager.Resolution resolution = new HuaweiWatchfaceManager.Resolution();
            String deviceScreen = String.format(
                    Locale.ROOT,
                    "%d*%d",
                    huaweiCoordinatorSupplier.getHuaweiCoordinator().getHeight(),
                    huaweiCoordinatorSupplier.getHuaweiCoordinator().getWidth()
            );
            this.valid = resolution.isValid(description.screen, deviceScreen);

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            if (helper.getPreviewBitmap() != null) {
                installItem.setPreview(helper.getPreviewBitmap());
            }

            installItem.setName(description.title);
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("Watchface cannot be installed (not connected or wrong device)");
                installActivity.setInfoText("Watchface cannot be installed (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("Watchface cannot be installed");
                installActivity.setInfoText(context.getString(R.string.watchface_resolution_doesnt_match,
                        resolution.screenByThemeVersion(description.screen), deviceScreen));
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(description.version);

            installItem.setIcon(R.drawable.ic_watchface);
            installActivity.setInfoText(context.getString(R.string.watchface_install_info, installItem.getName(), description.version, description.author));

            LOG.debug("Initialized HuaweiInstallHandler: Watchface");
        } else if (helper.isAPP()) {
            final HuaweiAppManager.AppConfig config = helper.getAppConfig();

            this.valid = true; //NOTE: nothing to verify for now

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            if (helper.getPreviewBitmap() != null) {
                installItem.setPreview(helper.getPreviewBitmap());
            }

            installItem.setName(config.bundleName);
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("App cannot be installed (not connected or wrong device)");
                installActivity.setInfoText("App cannot be installed (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("App cannot be installed");
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(config.version);

            installItem.setIcon(R.drawable.ic_watchapp);

            installActivity.setInfoText(context.getString(R.string.app_install_info, installItem.getName(), config.version, config.vendor));

            LOG.debug("Initialized HuaweiInstallHandler: App");
        } else if (helper.isMusic()) {
            HuaweiMusicUtils.MusicCapabilities capabilities = huaweiCoordinatorSupplier.getHuaweiCoordinator().getExtendedMusicInfoParams();
            if (capabilities == null) {
                capabilities = huaweiCoordinatorSupplier.getHuaweiCoordinator().getMusicInfoParams();
            }
            AudioInfo currentMusicInfo = helper.getMusicInfo();

            boolean isMediaCompatible = checkMediaCompatibility(capabilities, currentMusicInfo);

            this.valid = isMediaCompatible && !TextUtils.isEmpty(helper.getMusicInfo().getFileName()) && !TextUtils.isEmpty(helper.getMusicInfo().getArtist()) && !TextUtils.isEmpty(helper.getMusicInfo().getTitle());

            installActivity.setInstallEnabled(true);

            GenericItem installItem = new GenericItem();

            installItem.setName(helper.getFileName());
            installActivity.setInstallItem(installItem);
            if (device.isBusy()) {
                installActivity.setInfoText(device.getBusyTask());
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!device.isConnected()) {
                LOG.error("Music cannot be uploaded (not connected or wrong device)");
                installActivity.setInfoText("Music cannot be uploaded (not connected or wrong device)");
                installActivity.setInstallEnabled(false);
                return;
            }

            if (!this.valid) {
                LOG.error("Music cannot be uploaded");
                installActivity.setInfoText("Music cannot be uploaded");
                installActivity.setInstallEnabled(false);
                return;
            }

            installItem.setDetails(helper.getMusicInfo().getFileName());

            installItem.setIcon(R.drawable.ic_music);

            installActivity.setInfoText(context.getString(R.string.music_upload_info, helper.getMusicInfo().getFileName(), helper.getMusicInfo().getTitle(), helper.getMusicInfo().getArtist()));

            LOG.debug("Initialized HuaweiInstallHandler: Music");
        }

    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return helper.isValid();
    }

    @Override
    public void onStartInstall(GBDevice device) {
        helper.unsetFwBytes();
    }
}
