package nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

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
            GBApplication.deviceService().onSleepAsAndroidAction(action, sanitizeExtras(intent));
        }
    }

    // This receiver is RECEIVER_EXPORTED, so extras come from an untrusted source. Forwarding
    // the received Bundle as-is into an Intent that starts our own service trips StrictMode's
    // unsafe intent launch detection, since the taint follows the Bundle even into a new Intent.
    // Copying only the known keys into a fresh Bundle breaks that taint.
    private static Bundle sanitizeExtras(Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        final Bundle sanitized = new Bundle();
        if (extras.containsKey("TIMESTAMP")) {
            sanitized.putLong("TIMESTAMP", extras.getLong("TIMESTAMP"));
        }
        if (extras.containsKey("SUSPENDED")) {
            sanitized.putBoolean("SUSPENDED", extras.getBoolean("SUSPENDED", false));
        }
        if (extras.containsKey("SIZE")) {
            sanitized.putLong("SIZE", extras.getLong("SIZE", 12L));
        }
        if (extras.containsKey("REPEAT")) {
            sanitized.putInt("REPEAT", extras.getInt("REPEAT", 1));
        }
        if (extras.containsKey("TITLE")) {
            sanitized.putString("TITLE", extras.getString("TITLE"));
        }
        if (extras.containsKey("TEXT")) {
            sanitized.putString("TEXT", extras.getString("TEXT"));
        }
        if (extras.containsKey("DELAY")) {
            sanitized.putInt("DELAY", extras.getInt("DELAY", 60000));
        }
        return sanitized;
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
