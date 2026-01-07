package nodomain.freeyourgadget.gadgetbridge.prefs;

import android.content.SharedPreferences;

public abstract class AbstractPreferenceMigrator {
    protected static final String TAG = "GBPrefMigration";

    public abstract void migrate(int oldVersion, SharedPreferences sharedPrefs, SharedPreferences.Editor editor);
}
