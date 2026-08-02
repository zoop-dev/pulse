package nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class SleepAsAndroidReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(SleepAsAndroidReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        LOG.debug("Got Sleep as Android action {}", action);

        if (action != null && GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_enable", false)) {
            GBApplication.deviceService().onSleepAsAndroidAction(action, intent.getExtras());
        }
    }

    public IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(SleepAsAndroidAction.START_TRACKING);
        intentFilter.addAction(SleepAsAndroidAction.STOP_TRACKING);
        intentFilter.addAction(SleepAsAndroidAction.SET_PAUSE);
        intentFilter.addAction(SleepAsAndroidAction.SET_SUSPENDED);
        intentFilter.addAction(SleepAsAndroidAction.SET_BATCH_SIZE);
        intentFilter.addAction(SleepAsAndroidAction.START_ALARM);
        intentFilter.addAction(SleepAsAndroidAction.STOP_ALARM);
        intentFilter.addAction(SleepAsAndroidAction.UPDATE_ALARM);
        intentFilter.addAction(SleepAsAndroidAction.SHOW_NOTIFICATION);
        intentFilter.addAction(SleepAsAndroidAction.HINT);
        intentFilter.addAction(SleepAsAndroidAction.CHECK_CONNECTED);
        intentFilter.addAction(SleepAsAndroidAction.CONFIRM_CONNECTED);

        return intentFilter;
    }
}
