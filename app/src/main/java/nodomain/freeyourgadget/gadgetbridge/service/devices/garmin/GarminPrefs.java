package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.content.SharedPreferences;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GarminPrefs extends DevicePrefs {
    public static final String PREF_FAKE_OAUTH_WARNING = "garmin_fake_oauth_warning";
    public static final String PREF_FAKE_OAUTH_ENABLED = "garmin_fake_oauth_enabled";

    public GarminPrefs(final SharedPreferences preferences, final GBDevice gbDevice) {
        super(preferences, gbDevice);
    }

    public boolean fakeOauthEnabled() {
        return getBoolean(PREF_FAKE_OAUTH_WARNING, false) &&
                getBoolean(PREF_FAKE_OAUTH_ENABLED, false);
    }
}
