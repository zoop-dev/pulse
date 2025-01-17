package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class HuaweiDataSyncGoals implements HuaweiDataSyncCommon.DataCallback {

    private final HuaweiSupportProvider support;

    public static final int STEPS_GOAL = 900200006;
    public static final int CALORIES_GOAL = 900200007;
    public static final int EXERCISES_GOAL = 900200008;
    public static final int STAND_GOAL = 900200009;

    public static final String SRC_PKG_NAME = "hw.sport.config";
    public static final String PKG_NAME = "in.huawei.motion";

    public HuaweiDataSyncGoals(HuaweiSupportProvider support) {
        this.support = support;
        this.support.getHuaweiDataSyncManager().registerCallback(PKG_NAME, this);
    }

    private boolean sendCommonGoal(int configId, int goal) {
        int time = (int) (System.currentTimeMillis() / 1000);
        HuaweiTLV tlv = new HuaweiTLV().put(0x01, time).put(0x02, goal);
        HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
        HuaweiDataSyncCommon.ConfigData goalConfigData = new HuaweiDataSyncCommon.ConfigData();
        goalConfigData.configId = configId;
        goalConfigData.configAction = 1;
        goalConfigData.configData = tlv.serialize();
        List<HuaweiDataSyncCommon.ConfigData> list = new ArrayList<>();
        list.add(goalConfigData);
        data.setConfigDataList(list);
        return this.support.getHuaweiDataSyncManager().sendConfigCommand(SRC_PKG_NAME, PKG_NAME, data);
    }

    public boolean sendStepsGoal(int stepsGoal) {
        return sendCommonGoal(STEPS_GOAL, stepsGoal);
    }

    public boolean sendCaloriesBurntGoal(int caloriesBurntGoal) {
        return sendCommonGoal(CALORIES_GOAL, caloriesBurntGoal * 1000);
    }

    public boolean sendExerciseGoal(int exerciseGoal) {
        return sendCommonGoal(EXERCISES_GOAL, exerciseGoal);
    }

    public boolean sendStandGoal(int standGoal) {
        return sendCommonGoal(STAND_GOAL, standGoal);
    }

    @Override
    public void onConfigCommand(HuaweiDataSyncCommon.ConfigCommandData data) {
        if(data.getCode() != HuaweiDataSyncCommon.REPLY_OK) {
            return;
        }
        List<HuaweiDataSyncCommon.ConfigData> list = data.getConfigDataList();
        if(list.isEmpty()) {
            return;
        }
        HuaweiDataSyncCommon.ConfigData config = list.get(0);
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.parse(config.configData);
        if(!tlv.contains(0x02)) {
            return;
        }
        try {
        switch(config.configId) {
            case STEPS_GOAL: {
                int goal = tlv.getInteger(0x02);
                SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ActivityUser.PREF_USER_STEPS_GOAL, String.valueOf(goal));
                editor.apply();
            } break;
            case CALORIES_GOAL: {
                int goal = tlv.getInteger(0x02);
                SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ActivityUser.PREF_USER_CALORIES_BURNT, String.valueOf(goal / 1000));
                editor.apply();
            } break;
            case EXERCISES_GOAL: {
                int goal = tlv.getInteger(0x02);
                SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES, String.valueOf(goal));
                editor.apply();
            } break;
            case STAND_GOAL: {
                int goal = tlv.getInteger(0x02);
                SharedPreferences prefs = GBApplication.getPrefs().getPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS, String.valueOf(goal));
                editor.apply();
            } break;
        }
        } catch (HuaweiPacket.MissingTagException e) {
            throw new RuntimeException(e);
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
