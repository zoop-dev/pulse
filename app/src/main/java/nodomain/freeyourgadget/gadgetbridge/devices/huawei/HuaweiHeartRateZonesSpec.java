package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.content.Context;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZones;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesUtils;

public class HuaweiHeartRateZonesSpec extends HeartRateZonesSpec {

    private static final Logger LOG = LoggerFactory.getLogger(HuaweiHeartRateZonesSpec.class);

    public static final int HUAWEI_TYPE_UPRIGHT = 1;
    public static final int HUAWEI_TYPE_SITTING = 2;
    public static final int HUAWEI_TYPE_SWIMMING = 3;
    public static final int HUAWEI_TYPE_OTHER = 4;

    public static final int HUAWEI_CALCULATE_METHOD_MHR = 0;
    public static final int HUAWEI_CALCULATE_METHOD_HRR = 1;
    public static final int HUAWEI_CALCULATE_METHOD_LTHR = 3;

    private final HuaweiState state;

    public HuaweiHeartRateZonesSpec(GBDevice device, HuaweiState state) {
        super(device);
        this.state = state;
    }

    @Override
    public String getNameByType(Context context, HeartRateZonesSpec.PostureType type) {
        return switch (type) {
            case UPRIGHT -> context.getString(R.string.workout_posture_type_upright);
            case SITTING -> context.getString(R.string.workout_posture_type_sitting);
            case SWIMMING -> context.getString(R.string.activity_type_swimming);
            case OTHER -> context.getString(R.string.activity_type_other);
        };
    }

    private HeartRateZones loadOrCreateZones(HeartRateZonesSpec.PostureType type, HeartRateZones.CalculationMethod method, int maxHRThreshold) {
        String prefs = getHrZoneConfig(type, method);
        if (prefs != null) {
            try {
                Gson gson = new Gson();
                return gson.fromJson(prefs, HuaweiHeartRateZones.class);
            } catch (Exception e) {
                LOG.error("Error load heart zones config from prefs ", e);
            }
        }
        return new HuaweiHeartRateZones(method, maxHRThreshold);
    }

    @Override
    public List<HeartRateZonesConfig> getDeviceConfig() {
        List<HeartRateZonesConfig> res = new ArrayList<>();

        int age = new ActivityUser().getAge();

        if (!state.supportsTrack() && state.supportsHeartRateZones()) {
            List<HeartRateZones> zones = new ArrayList<>();
            int maxHRThreshold = (HeartRateZonesUtils.MAXIMUM_HEART_RATE - age) - getHRCorrection(HeartRateZonesSpec.PostureType.UPRIGHT);
            zones.add(loadOrCreateZones(HeartRateZonesSpec.PostureType.UPRIGHT, HeartRateZones.CalculationMethod.MHR, maxHRThreshold));
            if (state.supportsExtendedHeartRateZones())
                zones.add(loadOrCreateZones(HeartRateZonesSpec.PostureType.UPRIGHT, HeartRateZones.CalculationMethod.HRR, maxHRThreshold));
            res.add(loadOrCreateHeartRateZonesConfig(HeartRateZonesSpec.PostureType.UPRIGHT,
                    true,
                    HeartRateZonesUtils.MAXIMUM_HEART_RATE - age,
                    HeartRateZones.CalculationMethod.MHR,
                    zones
            ));
            return res;
        }

        if (!state.supportsTrack())
            return null;

        res.add(loadOrCreateHeartRateZonesConfig(HeartRateZonesSpec.PostureType.UPRIGHT,
                true,
                HeartRateZonesUtils.MAXIMUM_HEART_RATE - age,
                HeartRateZones.CalculationMethod.MHR,
                getUprightZone(age)
        ));
        res.add(loadOrCreateHeartRateZonesConfig(HeartRateZonesSpec.PostureType.SITTING,
                true,
                HeartRateZonesUtils.MAXIMUM_HEART_RATE - age,
                HeartRateZones.CalculationMethod.MHR,
                getSittingZone(age)
        ));
        res.add(loadOrCreateHeartRateZonesConfig(HeartRateZonesSpec.PostureType.SWIMMING,
                true,
                HeartRateZonesUtils.MAXIMUM_HEART_RATE - age,
                HeartRateZones.CalculationMethod.MHR,
                getSwimmingZone(age)
        ));
        res.add(loadOrCreateHeartRateZonesConfig(HeartRateZonesSpec.PostureType.OTHER,
                true,
                HeartRateZonesUtils.MAXIMUM_HEART_RATE - age,
                HeartRateZones.CalculationMethod.MHR,
                getOtherZone(age)
        ));
        return res;
    }

    private List<HeartRateZones> getUprightZone(int age) {
        List<HeartRateZones> res = new ArrayList<>();
        int maxHRThreshold = (HeartRateZonesUtils.MAXIMUM_HEART_RATE - age) - getHRCorrection(PostureType.UPRIGHT);
        res.add(loadOrCreateZones(PostureType.UPRIGHT, HeartRateZones.CalculationMethod.MHR, maxHRThreshold));
        res.add(loadOrCreateZones(PostureType.UPRIGHT, HeartRateZones.CalculationMethod.HRR, maxHRThreshold));
        res.add(loadOrCreateZones(PostureType.UPRIGHT, HeartRateZones.CalculationMethod.LTHR, maxHRThreshold));
        return res;
    }

    private List<HeartRateZones> getSittingZone(int age) {
        List<HeartRateZones> res = new ArrayList<>();
        int maxHRThreshold = (HeartRateZonesUtils.MAXIMUM_HEART_RATE - age) - getHRCorrection(PostureType.SITTING);
        res.add(loadOrCreateZones(PostureType.SITTING, HeartRateZones.CalculationMethod.MHR, maxHRThreshold));
        res.add(loadOrCreateZones(PostureType.SITTING, HeartRateZones.CalculationMethod.HRR, maxHRThreshold));
        return res;
    }

    private List<HeartRateZones> getSwimmingZone(int age) {
        List<HeartRateZones> res = new ArrayList<>();
        int maxHRThreshold = (HeartRateZonesUtils.MAXIMUM_HEART_RATE - age) - getHRCorrection(PostureType.SWIMMING);
        res.add(loadOrCreateZones(PostureType.SWIMMING, HeartRateZones.CalculationMethod.MHR, maxHRThreshold));
        res.add(loadOrCreateZones(PostureType.SWIMMING, HeartRateZones.CalculationMethod.HRR, maxHRThreshold));
        return res;
    }

    private List<HeartRateZones> getOtherZone(int age) {
        List<HeartRateZones> res = new ArrayList<>();
        int maxHRThreshold = (HeartRateZonesUtils.MAXIMUM_HEART_RATE - age) - getHRCorrection(HeartRateZonesSpec.PostureType.OTHER);
        res.add(loadOrCreateZones(PostureType.OTHER, HeartRateZones.CalculationMethod.MHR, maxHRThreshold));
        res.add(loadOrCreateZones(PostureType.OTHER, HeartRateZones.CalculationMethod.HRR, maxHRThreshold));
        return res;
    }

    //TODO: I am not sure about this. But it looks correct.
    private int getHRCorrection(HeartRateZonesSpec.PostureType type) {
        return switch (type) {
            case SITTING -> 6;
            case SWIMMING -> 10;
            case OTHER -> 5;
            default -> 0;
        };
    }

    public static HeartRateZonesConfig getByPosture(List<HeartRateZonesConfig> zones, HeartRateZonesSpec.PostureType type) {
        for (HeartRateZonesConfig cfg : zones) {
            if (cfg.getType() == type) {
                return cfg;
            }
        }
        return null;
    }

    public static HeartRateZones getByMethod(HeartRateZonesConfig cfg, HeartRateZones.CalculationMethod method) {
        for (HeartRateZones zn : cfg.getConfigByMethods()) {
            if (zn.getMethod() == method) {
                return zn;
            }
        }
        return null;
    }

    public static HeartRateZones getHRZonesConfigByPostureAndCalculationMethod(List<HeartRateZonesConfig> zones, HeartRateZonesSpec.PostureType type, HeartRateZones.CalculationMethod method) {

        HeartRateZonesConfig cfg = getByPosture(zones, type);
        if (cfg == null) {
            cfg = getByPosture(zones, PostureType.UPRIGHT); // Use as default
        }
        if (cfg == null)
            return null;

        return getByMethod(cfg, method);
    }

    public static HeartRateZonesSpec.PostureType fromHuaweiPostureType(int type) {
        return switch (type) {
            case HUAWEI_TYPE_SITTING -> PostureType.SITTING;
            case HUAWEI_TYPE_SWIMMING -> PostureType.SWIMMING;
            case HUAWEI_TYPE_OTHER -> PostureType.OTHER;
            default -> PostureType.UPRIGHT;
        };
    }

    public static int toHuaweiPostureType(HeartRateZonesSpec.PostureType type) {
        return switch (type) {
            case UPRIGHT -> HUAWEI_TYPE_UPRIGHT;
            case SITTING -> HUAWEI_TYPE_SITTING;
            case SWIMMING -> HUAWEI_TYPE_SWIMMING;
            case OTHER -> HUAWEI_TYPE_OTHER;
        };
    }

    public static HeartRateZones.CalculationMethod fromHuaweiCalculationMethod(int type) {
        return switch (type) {
            case HUAWEI_CALCULATE_METHOD_MHR -> HeartRateZones.CalculationMethod.MHR;
            case HUAWEI_CALCULATE_METHOD_LTHR -> HeartRateZones.CalculationMethod.LTHR;
            default -> HeartRateZones.CalculationMethod.HRR;
        };
    }

    public static int toHuaweiCalculationMethod(HeartRateZones.CalculationMethod type) {
        return switch (type) {
            case MHR -> HUAWEI_CALCULATE_METHOD_MHR;
            case HRR -> HUAWEI_CALCULATE_METHOD_HRR;
            case LTHR -> HUAWEI_CALCULATE_METHOD_LTHR;
        };
    }

    public static int getZoneForHR(int heartRate, HeartRateZones zones) {
        return getZoneForHR(heartRate, zones.getZone5(), zones.getZone4(), zones.getZone3(), zones.getZone2(), zones.getZone1());
    }

    private static int getZoneForHR(int heartRate, int zone5Threshold, int zone4Threshold, int zone3Threshold, int zone2Threshold, int zone1Threshold) {
        if (heartRate >= HeartRateZonesUtils.MAXIMUM_HEART_RATE) {
            return -1;
        }
        if (heartRate >= zone5Threshold) {
            return 4;
        }
        if (heartRate >= zone4Threshold) {
            return 3;
        }
        if (heartRate >= zone3Threshold) {
            return 2;
        }
        if (heartRate >= zone2Threshold) {
            return 1;
        }
        return heartRate >= zone1Threshold ? 0 : -1;
    }
}
