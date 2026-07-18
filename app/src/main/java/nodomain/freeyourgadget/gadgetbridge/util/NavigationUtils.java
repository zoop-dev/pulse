package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class NavigationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationUtils.class);

    public static boolean shouldSendNavigation(final Context context, final String appName) {
        final Prefs prefs = GBApplication.getPrefs();

        final boolean navigationForward = prefs.getBoolean("navigation_forward", true);
        final boolean navigationApp = prefs.getBoolean("navigation_app_" + appName, true);
        if (!navigationForward || !navigationApp) {
            LOG.info("Not forwarding navigation instruction for {}, user preferences do not allow this", appName);
            return false;
        }

        final boolean navigationScreenOn = prefs.getBoolean("nagivation_screen_on", true);
        if (!navigationScreenOn) {
            final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isScreenOn()) {
                LOG.info("Not forwarding navigation instructions, screen seems to be on and settings do not allow this");
                return false;
            }
        }

        return true;
    }

}
