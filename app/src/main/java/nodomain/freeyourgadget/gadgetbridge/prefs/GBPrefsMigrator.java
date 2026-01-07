package nodomain.freeyourgadget.gadgetbridge.prefs;

import android.content.SharedPreferences;
import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.prefs.migrators.PreferenceMigrator55;

public class GBPrefsMigrator {
    private static final String TAG = "GBPrefsMigrator";

    public static final String PREFS_VERSION = "shared_preferences_version";
    //if preferences have to be migrated, increment the following and add the migration logic in migratePrefs below
    // see http://stackoverflow.com/questions/16397848/how-can-i-migrate-android-preferences-with-a-new-version
    private static final int CURRENT_PREFS_VERSION = 55;

    public static void migratePrefsIfNeeded(final SharedPreferences sharedPrefs) {
        final int oldVersion = getPrefsFileVersion(sharedPrefs);
        if (oldVersion != CURRENT_PREFS_VERSION) {
            Log.i(TAG, "Migrating preferences from " + oldVersion + " to " + CURRENT_PREFS_VERSION);

            final SharedPreferences.Editor editor = sharedPrefs.edit();

            // Create new migrator classes as needed, one per version
            if (oldVersion < 55) {
                new PreferenceMigrator55().migrate(oldVersion, sharedPrefs, editor);
            }

            editor.putString(PREFS_VERSION, Integer.toString(CURRENT_PREFS_VERSION));
            editor.apply();
        }
    }

    private static int getPrefsFileVersion(final SharedPreferences sharedPrefs) {
        try {
            return Integer.parseInt(sharedPrefs.getString(PREFS_VERSION, "0")); //0 is legacy
        } catch (final Exception e) {
            //in version 1 this was an int
            return 1;
        }
    }
}
