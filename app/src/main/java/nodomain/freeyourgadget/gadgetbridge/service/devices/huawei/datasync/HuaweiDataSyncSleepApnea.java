package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class HuaweiDataSyncSleepApnea implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncSleepApnea.class);

    private final HuaweiSupportProvider support;

    public static final String SRC_PKG_NAME = "hw.health.apneajsmodule";
    public static final String PKG_NAME = "hw.watch.health.osa";

    public HuaweiDataSyncSleepApnea(HuaweiSupportProvider support) {
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

    public boolean changeSleepBreatheState(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300008, (byte) 1, tlv.serialize());
    }

    public boolean changeSleepApneaState(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300007, (byte) 1, tlv.serialize());
    }

    public boolean querySleepApneaState(boolean state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        return sendCommonData(900300007, (byte) 2, tlv.serialize());
    }

    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
        LOG.info("Handle SleepApnea command");
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
