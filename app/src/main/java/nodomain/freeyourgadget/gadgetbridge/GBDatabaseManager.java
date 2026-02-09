package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.util.Log;

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
    private static final String TAG = "GBDatabaseManager";

    private static final ReentrantReadWriteLock DB_LOCK = new ReentrantReadWriteLock(true);
    private static final GBDatabase GB_DATABASE = new GBDatabase();

    private GBDatabaseManager() {
    }

    public static void closeDatabase() {
        Log.v(TAG, "Trying to close database from " + Thread.currentThread().getName());
        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.closeDatabase();
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    public static void setupDatabase(final Context context) {
        Log.v(TAG, "Setting up database from " + Thread.currentThread().getName());
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
            Log.v(TAG, "Trying to acquire write lock from " + Thread.currentThread().getName());
            if (DB_LOCK.writeLock().tryLock(30, TimeUnit.SECONDS)) {
                Log.v(TAG, "Acquired write lock from " + Thread.currentThread().getName());
                return new LockHandler(DB_LOCK.writeLock(), GB_DATABASE.getDaoMaster(), GB_DATABASE.getSession());
            }
        } catch (final InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for DB lock");
        }
        throw new GBException("Failed to acquire database write lock");
    }

    public static DBHandler acquireReadOnly() throws GBException {
        try {
            Log.v(TAG, "Trying to acquire read lock from " + Thread.currentThread().getName());
            if (DB_LOCK.readLock().tryLock(30, TimeUnit.SECONDS)) {
                Log.v(TAG, "Acquired read lock from " + Thread.currentThread().getName());
                return new LockHandler(DB_LOCK.readLock(), GB_DATABASE.getDaoMaster(), GB_DATABASE.getSession());
            }
        } catch (final InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for DB lock");
        }
        throw new GBException("Failed to acquire database read lock");
    }

    /**
     * Deletes both the old Activity database and the new one recreates it with empty tables.
     *
     * @return true on successful deletion
     */
    public static boolean deleteActivityDatabase(final Context context) {
        Log.v(TAG, "Deleting activity database from " + Thread.currentThread().getName());

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
        Log.v(TAG, "Exporting database to file from " + Thread.currentThread().getName());

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.exportDB(destFile);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }

    public static void exportDB(final OutputStream dest) throws IOException {
        Log.v(TAG, "Exporting database to OutputStream from " + Thread.currentThread().getName());

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
        Log.v(TAG, "Deleting old activity database from " + Thread.currentThread().getName());

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
        Log.v(TAG, "Importing database from " + Thread.currentThread().getName());

        DB_LOCK.writeLock().lock();
        try {
            GB_DATABASE.importDB(inputStream);
        } finally {
            DB_LOCK.writeLock().unlock();
        }
    }
}
