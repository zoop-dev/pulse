/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.widget.Toast;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetAppNames;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendAppDelete;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiAppManager {

    static Logger LOG = LoggerFactory.getLogger(HuaweiAppManager.class);

    public static class AppConfig {

        public String bundleName;
        public String vendor;
        public String version;


        public AppConfig(String jsonStr) {

            try {
                JSONObject config = new JSONObject(jsonStr);
                this.bundleName = config.getJSONObject("app").getString("bundleName");
                this.vendor = config.getJSONObject("app").getString("vendor");
                this.version = config.getJSONObject("app").getJSONObject("version").getString("name");

            } catch (Exception e) {
                LOG.error("Error decode app config", e);
            }
        }

    }

    private final HuaweiSupportProvider support;

    private List<App.InstalledAppInfo> installedAppList = null;

    public HuaweiAppManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    public void setInstalledAppList(List<App.InstalledAppInfo> installedAppList) {
        this.installedAppList = installedAppList;
        handleAppList();
    }

    public void handleAppList() {
        if (this.installedAppList == null) {
            return;
        }

        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID uuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    uuid,
                    appInfo.appName,
                    "",
                    appInfo.version,
                    GBDeviceApp.Type.APP_GENERIC
            );
            gbDeviceApps.add(gbDeviceApp);
        }
        support.setGbWatchApps(gbDeviceApps);
    }

    /**
     * Handle an install report the watch sends on its own while it unpacks a bundle we uploaded.
     * The 0x28 file upload service only tells us the bytes arrived, so without this the user is
     * left with the "installation complete, 100%" notification the upload posted even when the
     * watch goes on to reject the bundle - which is what an incompatible or untrusted app does.
     */
    public void handleInstallStatus(int status, String packageName) {
        if (status <= App.AppInstallStatus.PROGRESS_MAX) {
            LOG.debug("Installing app {}: {}%", packageName, status);
            support.onUploadProgress(R.string.updatefirmwareoperation_update_in_progress, status, true);
            return;
        }
        switch (status) {
            case App.AppInstallStatus.STATE_INSTALL_STARTED:
                LOG.info("Watch started installing app {}", packageName);
                support.onUploadProgress(R.string.updatefirmwareoperation_update_in_progress, 0, true);
                break;
            case App.AppInstallStatus.STATE_UNINSTALL_STARTED:
                LOG.info("Watch started removing app {}", packageName);
                break;
            case App.AppInstallStatus.STATE_INSTALLED:
                LOG.info("Watch installed app {}", packageName);
                support.onUploadProgress(R.string.updatefirmwareoperation_update_complete, 100, false);
                requestAppList();
                break;
            case App.AppInstallStatus.STATE_UNINSTALLED:
                LOG.info("Watch removed app {}", packageName);
                requestAppList();
                break;
            case App.AppInstallStatus.STATE_INSTALL_FAILED:
                LOG.error("Watch refused to install app {}", packageName);
                support.onUploadProgress(R.string.updatefirmwareoperation_write_failed, 0, false);
                GB.toast(support.getContext(), R.string.huawei_app_install_failed, Toast.LENGTH_LONG, GB.ERROR);
                break;
            case App.AppInstallStatus.STATE_UNINSTALL_FAILED:
                LOG.error("Watch failed to remove app {}", packageName);
                GB.toast(support.getContext(), R.string.huawei_app_uninstall_failed, Toast.LENGTH_LONG, GB.ERROR);
                requestAppList();
                break;
            default:
                LOG.warn("Unknown install status {} for app {}", status, packageName);
        }
    }

    public void requestAppList() {
        if (!this.support.getDeviceState().supportsAppParams())
            return;
        try {
            GetAppNames getAppNames = new GetAppNames(support);
            getAppNames.doPerform();
        } catch (IOException e) {
            LOG.error("Error request applications list", e);
        }
    }

    public boolean startApp(UUID uuid) {
        if (this.installedAppList == null)
            return false;

        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID appUuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            if (appUuid.equals(uuid))
                return true;
        }
        return false;
    }

    public boolean deleteApp(UUID uuid) {
        if (this.installedAppList == null)
            return false;
        
        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID appUuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            if (appUuid.equals(uuid)) {
                try {
                    SendAppDelete sendAppDelete = new SendAppDelete(support, appInfo.packageName);
                    sendAppDelete.doPerform();
                } catch (IOException e) {
                    LOG.error("Could not delete app: {}", appInfo.packageName, e);
                }
                return true;
            }
        }
        return false;
    }
}
