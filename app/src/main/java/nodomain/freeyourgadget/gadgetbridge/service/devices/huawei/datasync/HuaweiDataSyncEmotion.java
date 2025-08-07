package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class HuaweiDataSyncEmotion implements HuaweiDataSyncCommon.DataCallback {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncEmotion.class);

    private final HuaweiSupportProvider support;

    public static final int EMOTIONAL_ID = 900500028;


    public static final String SRC_PKG_NAME = "hw.health.emotion";
    public static final String PKG_NAME = "hw.watch.health.emotion";

    public HuaweiDataSyncEmotion(HuaweiSupportProvider support) {
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    private boolean sendCommonData(byte state) {
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, state);
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData goalConfigData = new HuaweiDataSyncCommon.ConfigData();
        goalConfigData.configId = EMOTIONAL_ID;
        goalConfigData.configAction = 1;
        goalConfigData.configData = tlv.serialize();
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(goalConfigData);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }

    public boolean changeEmotionsState(boolean state) {
        return sendCommonData((byte) (state ? 1 : 0));
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
