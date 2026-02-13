package nodomain.freeyourgadget.gadgetbridge.prefs.migrators;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.prefs.AbstractPreferenceMigrator;

public class PreferenceMigrator56 extends AbstractPreferenceMigrator {
    private static final Logger LOG = LoggerFactory.getLogger(PreferenceMigrator56.class);

    @Override
    public void migrate(final int oldVersion, final SharedPreferences sharedPrefs, final SharedPreferences.Editor editor) {
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession daoSession = db.getDaoSession();
            final List<Device> activeDevices = DBHelper.getActiveDevices(daoSession);
            for (final Device dbDevice : activeDevices) {
                if (dbDevice.getTypeName().startsWith("NOTHING_CMF_WATCH_PRO")) {
                    // The defaults were not valid CmfActivityType values, which would crash on connection for users
                    // that only opened the preference screen without ever changing them
                    final SharedPreferences deviceSpecificSharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    final String activityTypes = deviceSpecificSharedPrefs.getString("workout_activity_types_sortable", "");
                    if ("indoor_run,outdoor_run".equals(activityTypes)) {
                        deviceSpecificSharedPrefs.edit()
                                .putString("workout_activity_types_sortable", "indoor_running,outdoor_running")
                                .apply();
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to migrate prefs to version 56", e);
        }
    }
}
