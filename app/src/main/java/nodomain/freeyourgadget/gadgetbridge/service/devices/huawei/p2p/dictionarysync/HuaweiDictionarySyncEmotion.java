package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.dictionarysync;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiState;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiEmotionsSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiUtil;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiEmotionsSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PDataDictionarySyncService;

public class HuaweiDictionarySyncEmotion implements  HuaweiDictionarySyncInterface {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiDictionarySyncEmotion.class);

    public static final int EMOTION_CLASS = 500031;
    public static final int EMOTION_STATUS_VALUE = 500031195;
    public static final int EMOTION_VALENCE_CHARACTER_VALUE = 500031347;
    public static final int EMOTION_ORIGIN_STATUS_VALUE = 500031160;
    public static final int EMOTION_AROUSAL_CHARACTER_VALUE = 500031842;

    @Override
    public int getDataClass() {
        return EMOTION_CLASS;
    }

    @Override
    public boolean supports(HuaweiState state) {
        return state.supportsEmotion();
    }

    @Override
    public long getLastDataSyncTimestamp(GBDevice gbDevice) {
        try (DBHandler db = GBApplication.acquireDB()) {
            HuaweiEmotionsSampleProvider emotionsStatsSampleProvider = new HuaweiEmotionsSampleProvider(gbDevice, db.getDaoSession());
            return emotionsStatsSampleProvider.getLastFetchTimestamp();
        } catch (Exception e) {
            LOG.warn("Exception for getting emotion start time", e);
        }
        return 0;
    }

    @Override
    public void handleData(Context context, GBDevice gbDevice, List<HuaweiP2PDataDictionarySyncService.DictData> dictData) {
        List<HuaweiEmotionsSample> emotionsSamples = new ArrayList<>();
        for (HuaweiP2PDataDictionarySyncService.DictData dt : dictData) {
            long timestamp = dt.getStartTimestamp();
            long lastTime = Math.max(dt.getEndTimestamp(), dt.getModifyTimestamp());
            Integer status = null;
            Double valenceCharacter = null;
            Integer originStatus = null;
            Double arousalCharacter = null;
            for (HuaweiP2PDataDictionarySyncService.DictData.DictDataValue val : dt.getData()) {
                if (val.getTag() != 10) {
                    LOG.info("emotions unexpected tag: {}", val.getTag());
                    continue;
                }
                if (val.getDataType() == EMOTION_STATUS_VALUE) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    if (value >= 0 && value < 100) {
                        status = (int) value;
                    }
                } else if (val.getDataType() == EMOTION_VALENCE_CHARACTER_VALUE) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    if (value >= 0 && value <= 100) {
                        valenceCharacter = value;
                    }
                } else if (val.getDataType() == EMOTION_ORIGIN_STATUS_VALUE) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    if (value >= 0 && value < 100) {
                        originStatus = (int) value;
                    }
                } else if (val.getDataType() == EMOTION_AROUSAL_CHARACTER_VALUE) {
                    double value = HuaweiUtil.convBytes2Double(val.getValue());
                    if (value >= 0 && value <= 100) {
                        arousalCharacter = value;
                    }
                } else {
                    LOG.info("emotions unknown data type: {}", val.getDataType());
                }
            }
            if (status != null || valenceCharacter != null || originStatus != null || arousalCharacter != null) {
                HuaweiEmotionsSample sample = new HuaweiEmotionsSample();
                sample.setTimestamp(timestamp);
                sample.setLastTimestamp(lastTime);
                sample.setStatus(status == null ? 0 : status);
                sample.setValenceCharacter(valenceCharacter);
                sample.setOriginStatus(originStatus);
                sample.setArousalCharacter(arousalCharacter);
                emotionsSamples.add(sample);
            }
        }
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            new HuaweiEmotionsSampleProvider(gbDevice, session).persistForDevice(context, gbDevice, emotionsSamples);
        } catch (Exception e) {
            LOG.error("Cannot save emotions samples, continue");
        }
    }
}
