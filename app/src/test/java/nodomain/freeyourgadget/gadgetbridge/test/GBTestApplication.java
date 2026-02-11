package nodomain.freeyourgadget.gadgetbridge.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class GBTestApplication extends GBApplication {
    private static final Logger LOG = LoggerFactory.getLogger(GBTestApplication.class);

    @Override
    protected void migratePrefsIfNeeded() {
        // In tests, do not migrate preferences
        // FIXME: This is not ideal. In tests, do not migrate preferences. We should be able to initialize
        //  the database before the application is created so that this works and is actually testable., but
        //  right now all it does is spam the logs with dozens of stack traces for failed preference migrations
        LOG.info("Skipping preference migration during tests");
    }
}
