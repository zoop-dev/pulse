package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.dictionarysync;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiState;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiUtil;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PDataDictionarySyncService;

public class HuaweiDictionarySyncHRV implements  HuaweiDictionarySyncInterface {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDictionarySyncHRV.class);

    public static final int HRV_CLASS = 500044;
    public static final int HRV_RMSSD_VALUE = 500044831;

    @Override
    public int getDataClass() {
        return HRV_CLASS;
    }

    @Override
    public boolean supports(HuaweiState state) {
        return state.supportsHRV();
    }

    @Override
    public long getLastDataSyncTimestamp(GBDevice gbDevice) {
        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiHrvValueSampleProvider hrvStatsSampleProvider = new HuaweiHrvValueSampleProvider(gbDevice, db.getDaoSession());
            return hrvStatsSampleProvider.getLastFetchTimestamp();
        } catch (Exception e) {
            LOG.warn("Exception for getting HRV start time", e);
        }
        return 0;
    }

    @Override
    public void handleData(Context context, GBDevice gbDevice, List<HuaweiP2PDataDictionarySyncService.DictData> dictData) {
        List<HuaweiHrvValueSample> hrvSamples = new ArrayList<>();
        for (HuaweiP2PDataDictionarySyncService.DictData dt : dictData) {
            long timestamp = dt.getStartTimestamp();
            long lastTime = Math.max(dt.getEndTimestamp(), dt.getModifyTimestamp());
            Integer rmssd = null;
            for (HuaweiP2PDataDictionarySyncService.DictData.DictDataValue val : dt.getData()) {
                if (val.getTag() != 10) {
                    LOG.info("HRV unexpected tag: {}", val.getTag());
                    continue;
                }
                if (val.getDataType() == HRV_RMSSD_VALUE) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    if (value >= 0 && value <= 200) {
                        rmssd = (int) value;
                    }
                } else {
                    LOG.info("HRV unknown data type: {}", val.getDataType());
                }
            }
            if(rmssd != null) {
                HuaweiHrvValueSample sample = new HuaweiHrvValueSample();
                sample.setTimestamp(timestamp);
                sample.setLastTimestamp(lastTime);
                sample.setValue(rmssd);
                hrvSamples.add(sample);
            }
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            new HuaweiHrvValueSampleProvider(gbDevice, session).persistForDevice(context, gbDevice, hrvSamples);
        } catch (Exception e) {
            LOG.error("Cannot save HRV samples, continue");
        }
    }
}
