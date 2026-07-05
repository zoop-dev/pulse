package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.dictionarysync;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiState;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiUtil;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PDataDictionarySyncService;

public class HuaweiDictionarySyncDebug implements HuaweiDictionarySyncInterface {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDictionarySyncDebug.class);

    @Override
    public int getDataClass() {
        return 0;
    }

    @Override
    public boolean supports(HuaweiState state) {
        return false;
    }

    @Override
    public long getLastDataSyncTimestamp(GBDevice gbDevice) {
        return 0;
    }

    @Override
    public void handleData(Context context, GBDevice gbDevice, List<HuaweiP2PDataDictionarySyncService.DictData> dictData) {
        for (HuaweiP2PDataDictionarySyncService.DictData dt : dictData) {
            LOG.info("DictionarySyncDebug: {}", dt);
            for (HuaweiP2PDataDictionarySyncService.DictData.DictDataValue val : dt.getData()) {
                if (val.getTag() == 10) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    LOG.info("DictionarySyncDebug tag 10 : {} -- {}", val.getDataType(), value);
                } else if (val.getTag() == 11) {
                    LOG.info("DictionarySyncDebug tag 11: {} -- {}", val.getDataType(), new String(val.getValue()));
                } else {
                    LOG.info("DictionarySyncDebug unsupported tag: {}", val.getTag());
                }
            }
        }
    }
}
