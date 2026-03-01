/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;

public class XiaomiRpkService extends AbstractXiaomiService implements XiaomiDataUploadService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiRpkService.class);

    public static final int COMMAND_TYPE = 20;

    public static final int CMD_RPK_LIST = 0;
    public static final int CMD_RPK_SET = 1;
    public static final int CMD_RPK_DELETE = 3;
    public static final int CMD_RPK_INSTALL = 1;
    public static final int CMD_RPK_INSTALLED = 2;


    // Not null if we're installing a firmware
    private XiaomiFWHelper fwHelper = null;

    public XiaomiRpkService(final XiaomiSupport support) {
        super(support);
    }

    private final List<GBDeviceApp> apps = new ArrayList<>();
    private final Map<String, ByteString> shaMap = new HashMap<>();

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_RPK_LIST:
                handleRpkList(cmd.getRpk().getRpkList());
                return;
            case CMD_RPK_INSTALLED:
                LOG.debug("Got rpk install complete notification");
                requestRpkList();
                return;
            case CMD_RPK_DELETE:
                // Band may send a delete response in some cases
                LOG.debug("Got rpk delete response");
                requestRpkList();
                return;
            case CMD_RPK_INSTALL:
                final int installStatus = cmd.getRpk().getRpkInstallStart().getCmd();
                LOG.debug("Got rpk install response, status={}", installStatus);
                if (installStatus != 0) {
                    LOG.warn("Rpk install rejected with status {} for {}", installStatus, fwHelper != null ? fwHelper.getId() : "null");
                    return;
                }
                LOG.debug("Rpk install accepted, starting upload");
                setDeviceBusy();
                getSupport().getDataUploadService().setCallback(this);
                getSupport().getDataUploadService().requestUpload(XiaomiDataUploadService.TYPE_RPK, fwHelper.getBytes());
                return;
        }

        LOG.warn("Unknown rpk command {}", cmd.getSubtype());
    }

    public void requestRpkList() {
        getSupport().sendCommand("request rpk list", COMMAND_TYPE, CMD_RPK_LIST);
    }

    public void deleteRpk(UUID uuid) {
        for (GBDeviceApp app : apps) {
            if (app.getUUID().equals(uuid)) {
                String packageName = app.getCreator();
                ByteString sha = shaMap.get(packageName);
                LOG.debug("Deleting rpk: id={}", packageName);
                getSupport().sendCommand("delete rpk",
                        XiaomiProto.Command.newBuilder()
                                .setType(COMMAND_TYPE)
                                .setSubtype(CMD_RPK_DELETE)
                                .setRpk(XiaomiProto.Rpk.newBuilder()
                                        .setRpkDel(
                                                XiaomiProto.RpkInfoList.newBuilder()
                                                        .setId(packageName)
                                                        .setSha(sha)
                                        )
                                )
                                .build());
                // Band sends no response to delete — request list immediately after
                requestRpkList();
                return;
            }
        }
        LOG.warn("Unknown rpk uuid {}", uuid);
    }

    public void installRpk(final XiaomiFWHelper fwHelper) {
        assert fwHelper.isValid();
        assert fwHelper.isRpk();

        this.fwHelper = fwHelper;

        LOG.debug("Sending install request for rpk: id={}, versionCode={}, size={}", fwHelper.getId(), fwHelper.getVersionCode(), fwHelper.getBytes().length);
        getSupport().sendCommand("install rpk " + fwHelper.getId(),
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_RPK_INSTALL)
                        .setRpk(XiaomiProto.Rpk.newBuilder()
                                .setRpkInfo(
                                        XiaomiProto.RpkInfo.newBuilder()
                                                .setId(fwHelper.getId())
                                                .setUnknown2(fwHelper.getVersionCode())
                                                .setSize(fwHelper.getBytes().length)
                                ))
                        .build());
    }

    private void handleRpkList(final XiaomiProto.RpkList rpkList) {
        LOG.debug("Got {} rpks", rpkList.getRpkInfoCount());
        apps.clear();
        shaMap.clear();
        for (XiaomiProto.RpkInfoList info : rpkList.getRpkInfoList()) {
            shaMap.put(info.getId(), info.getSha());
            String packageName = info.getId();
            String appName = info.getName();

            GBDeviceApp gbDeviceApp = new GBDeviceApp(UUID.nameUUIDFromBytes(packageName.getBytes()), appName, packageName, "", GBDeviceApp.Type.APP_GENERIC);
            apps.add(gbDeviceApp);
        }
        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = apps.toArray(new GBDeviceApp[0]);
        getSupport().evaluateGBDeviceEvent(appInfoCmd);
    }


    @Override
    public void onUploadFinish(final boolean success) {
        final int notificationMessage = success ? R.string.uploadrpkoperation_complete : R.string.uploadrpkoperation_failed;

        onUploadProgress(notificationMessage, 100, false);

        if (getSupport().getConnectionSpecificSupport() != null) {
            getSupport().getConnectionSpecificSupport().runOnQueue("rpk upload finish", () -> {
                LOG.debug("Rpk upload finished: {}", success);
                getSupport().getDataUploadService().setCallback(null);
                unsetDeviceBusy();

                // List refresh is triggered by CMD_RPK_INSTALLED notification from the band

                fwHelper = null;
            });
        }
    }

    @Override
    public void onUploadProgress(final int progressPercent) {
        onUploadProgress(R.string.uploadwatchfaceoperation_in_progress, progressPercent, true);
    }

    private void onUploadProgress(final int stringResource, final int progressPercent, final boolean ongoing) {
        if (getSupport().getConnectionSpecificSupport() != null)
            getSupport().getConnectionSpecificSupport().onUploadProgress(stringResource, progressPercent, ongoing);
    }

    private void setDeviceBusy() {
        final GBDevice device = getSupport().getDevice();
        device.setBusyTask(R.string.uploading_rpk, getSupport().getContext());
        device.sendDeviceUpdateIntent(getSupport().getContext());
    }

    private void unsetDeviceBusy() {
        final GBDevice device = getSupport().getDevice();
        if (device != null && device.isConnected()) {
            if (device.isBusy()) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(getSupport().getContext());
            }
            device.sendDeviceUpdateIntent(getSupport().getContext());
        }
    }
}
