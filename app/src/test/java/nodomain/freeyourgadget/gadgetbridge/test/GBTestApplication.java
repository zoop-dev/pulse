package nodomain.freeyourgadget.gadgetbridge.test;

import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class GBTestApplication extends GBApplication {
    @Override
    protected void migratePrefs(final int oldVersion) {
        // In tests, do not migrate preferences
        // FIXME: This is not ideal. In tests, do not migrate preferences. We should be able to initialize
        //  the database before the application is created so that this works and is actually testable., but
        //  right now all it does is spam the logs with dozens of stack traces for failed preference migrations
        Log.i("GBTestApplication", "Skipping preference migration during tests");
    }
}
