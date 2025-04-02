/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiDataSyncFindDevice implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncFindDevice.class);

    private final HuaweiSupportProvider support;

    public static final int FIND_DEVICE = 900100002;

    public static final String SRC_PKG_NAME = "hw.unitedevice.finddevice";
    public static final String PKG_NAME = "findDevice";

    public HuaweiDataSyncFindDevice(HuaweiSupportProvider support) {
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    public boolean sendStartFindDevice() {
        return sendCommonFindDevice((byte) 2);
    }

    public boolean sendStopFindDevice() {
        return sendCommonFindDevice((byte) 1);
    }

    private boolean sendCommonFindDevice(byte action) {
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData goalConfigData = new HuaweiDataSyncCommon.ConfigData();
        goalConfigData.configId = FIND_DEVICE;
        goalConfigData.configAction = action;
        goalConfigData.configData = null;
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(goalConfigData);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }


    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
        //TODO: handle this
        LOG.info("HuaweiDataSyncFindDevice code: {}", data.getCode());
        if(data.getConfigDataList() != null && !data.getConfigDataList().isEmpty()) {
            HuaweiDataSyncCommon.ConfigData dt = data.getConfigDataList().get(0);
            LOG.info("HuaweiDataSyncFindDevice config Action: {}, ID: {}, Data: {}", dt.configAction, dt.configId, GB.hexdump(dt.configData));
            //action 2 or 4
            //data, if 2  data is byte, if 4 is null
        }

    }

    @Override
    public void onEventCommand(HuaweiDataSyncCommon.EventCommandData data) {

    }

    @Override
    public void onDataCommand(HuaweiDataSyncCommon.DataCommandData data) {

    }

    @Override
    public void onDictDataCommand(HuaweiDataSyncCommon.DictDataCommandData data) {

    }
}
