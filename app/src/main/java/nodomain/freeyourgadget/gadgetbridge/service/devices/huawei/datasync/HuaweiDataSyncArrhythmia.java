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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class HuaweiDataSyncArrhythmia implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncArrhythmia.class);

    private final HuaweiSupportProvider support;

    public static final String SRC_PKG_NAME = "hw.health.ppgjsmodule";
    public static final String PKG_NAME = "hw.watch.health.arrhythmia";

    public HuaweiDataSyncArrhythmia(HuaweiSupportProvider support) {
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    private boolean sendCommonData(int configId, byte configAction, byte[] configData) {
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData config = new HuaweiDataSyncCommon.ConfigData();
        config.configId = configId;
        config.configAction = configAction;
        config.configData = configData;
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(config);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }

    public boolean changeState(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300004, (byte) 1, tlv.serialize());
    }

    // TODO:
    public boolean queryState() {
        return sendCommonData(900300004, (byte) 2, new byte[0]);
    }

    // TODO:
    public boolean measure(byte command) { // 1 - start, 0 - end
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, command);
        return sendCommonData(900300003, (byte) 1, tlv.serialize());
    }

    public boolean setAutomatic(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300002, (byte) 1, tlv.serialize());
    }

    public boolean setAlert(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300009, (byte) 1, tlv.serialize());
    }

    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
        LOG.info("Handle Arrhythmia command");
        //TODO:
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
