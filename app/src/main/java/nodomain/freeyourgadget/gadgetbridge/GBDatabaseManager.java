package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;

public class GBDatabaseManager {
    private static final Logger LOG = LoggerFactory.getLogger(GBDatabaseManager.class);

    private static final ReentrantReadWriteLock DB_LOCK = new ReentrantReadWriteLock(true);
    private static final GBDatabase GB_DATABASE = new GBDatabase();

    private GBDatabaseManager() {
    }

    public static void closeDatabase() {
        LOG.trace("Trying to close database");
        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.closeDatabase();
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    public static void setupDatabase(final Context context) {
        LOG.trace("Setting up database");
        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.setupDatabase(context);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    /**
     * Returns the DBHandler instance for reading/writing or throws GBException
     * when that was not successful
     * If acquiring was successful, callers must call close when they
     * are done (from the same thread that acquired the lock!
     * <p>
     * Callers must not hold a reference to the returned instance because it
     * will be invalidated at some point.
     *
     * @return the DBHandler
     */
    public static DBHandler acquireWrite() throws GBException {
        try {
            LOG.trace("Trying to acquire write lock");
            if (DB_LOCK.writeLock().tryLock(30, TimeUnit.SECONDS)) {
                LOG.trace("Acquired write lock");
                return new LockHandler(DB_LOCK.writeLock(), GB_DATABASE.getDaoMaster(), GB_DATABASE.getSession());
            }
        } catch (final InterruptedException e) {
           LOG.error("Interrupted while waiting for write DB lock", e);
        }
        throw new GBException("Failed to acquire database write lock");
    }

    public static DBHandler acquireReadOnly() throws GBException {
        try {
            LOG.trace("Trying to acquire read lock");
            if (DB_LOCK.readLock().tryLock(30, TimeUnit.SECONDS)) {
                LOG.trace("Acquired read lock");
                return new LockHandler(DB_LOCK.readLock(), GB_DATABASE.getDaoMaster(), GB_DATABASE.getSession());
            }
        } catch (final InterruptedException e) {
            LOG.error("Interrupted while waiting for read DB lock", e);
        }
        throw new GBException("Failed to acquire database read lock");
    }

    /**
     * Deletes both the old Activity database and the new one recreates it with empty tables.
     *
     * @return true on successful deletion
     */
    public static boolean deleteActivityDatabase(final Context context) {
        LOG.trace("Deleting activity database");

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.closeDatabase();
            boolean result = deleteOldActivityDatabase(context);
            result &= context.deleteDatabase(GBDatabase.DATABASE_NAME);
            return result;
        } finally {
            GB_DATABASE.setupDatabase(context);
            DB_LOCK.writeLock().unlock();
        }
    }

    public static void exportDB(final File destFile) throws IOException {
        LOG.trace("Exporting database to file");

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.exportDB(destFile);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    public static void exportDB(final OutputStream dest) throws IOException {
        LOG.trace("Exporting database to OutputStream");

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.exportDB(dest);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    /**
     * Deletes the legacy (pre 0.12) Activity database
     *
     * @return true on successful deletion
     */
    public static boolean deleteOldActivityDatabase(final Context context) {
        LOG.trace("Deleting old activity database");

        final DBHelper dbHelper = new DBHelper(context);
        boolean result = true;
        if (dbHelper.existsDB("ActivityDatabase")) {
            result = context.deleteDatabase("ActivityDatabase");
        }
        return result;
    }

    public static void importDB(final File fromFile) throws IllegalStateException, IOException {
        importDB(new FileInputStream(fromFile));
    }

    public static void importDB(final InputStream inputStream) throws IllegalStateException, IOException {
        LOG.trace("Importing database");

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.importDB(inputStream);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }
}
