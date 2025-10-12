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

public class HuaweiDataSyncEcg implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncEcg.class);

    private final HuaweiSupportProvider support;

    public static final String SRC_PKG_NAME = "hw.health.ecganalysis";
    public static final String PKG_NAME = "hw.watch.health.ecganalysis";

    public HuaweiDataSyncEcg(HuaweiSupportProvider support) {
        LOG.info("HuaweiDataSyncEcg");
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    private boolean sendCommonConfig(int configId, byte configAction, byte[] configData) {
        LOG.info("HuaweiDataSyncEcg sendCommonConfig");
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData ecgConfigData = new HuaweiDataSyncCommon.ConfigData();
        ecgConfigData.configId = configId;
        ecgConfigData.configAction = configAction;
        ecgConfigData.configData = configData;
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(ecgConfigData);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }

    public boolean changeECGState(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, (byte) (state ? 1 : 0)).put(0x02, "hw.health.ecganalysis");
        return sendCommonConfig(900300005, (byte) 1,  tlv.serialize());
    }

    public boolean activateECG() {
        return sendCommonConfig(900300006, (byte) 2,  new byte[0]);
    }

    // TODO:
    public boolean sendNotifications(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV()
                .put(0x01, (byte)0)  // 1 - enable, 0 - disable
                .put(0x02, (byte)0)  // days as bits sun,mon,tue,wed,thu,fri,sat. 1 is set. ex. 0100001 - mon and sat. CAn be wrong, not tested.
                .put(0x03, (byte)0)  // hours
                .put(0x04, (byte)0); // minutes
        return sendCommonConfig(900300014, (byte) 1,  tlv.serialize());
    }


    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
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
