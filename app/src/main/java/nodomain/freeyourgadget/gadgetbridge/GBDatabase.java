package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

/**
 * This class is NOT thread-safe, all calls should be guarded by the upstream write lock.
 */
public class GBDatabase {
    private static final String TAG = "GBDatabase";
    public static final String DATABASE_NAME = "Gadgetbridge";

    private DaoMaster daoMaster = null;
    private SQLiteOpenHelper helper = null;
    private DaoSession session = null;

    public DaoMaster getDaoMaster() {
        return daoMaster;
    }

    public SQLiteOpenHelper getHelper() {
        return helper;
    }

    public DaoSession getSession() {
        return session;
    }

    void closeDatabase() {
        if (session == null) {
            Log.w(TAG, "Database was already closed");
            return;
        }
        Log.d(TAG, "Trying to close database from " + Thread.currentThread().getName());
        session.clear();
        session.getDatabase().close();
        session = null;
        daoMaster = null;
        helper = null;
        Log.d(TAG, "Database closed");
    }

    void setupDatabase(final Context context) {
        if (session != null) {
            Log.w(TAG, "Database already setup");
            return;
        }

        Log.i(TAG, "Setting up database from " + Thread.currentThread().getName());

        if (GBEnvironment.env().isTest()) {
            helper = new DaoMaster.DevOpenHelper(context, null, null);
        } else {
            helper = new DBOpenHelper(context, DATABASE_NAME, null);
        }
        final SQLiteDatabase db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        session = daoMaster.newSession();
        if (session == null) {
            throw new RuntimeException("Unable to create database session");
        }
        Log.d(TAG, "Database setup finished");
    }

    void importDB(final InputStream inputStream) throws IllegalStateException, IOException {
        final String dbPath = getClosedDBPath();
        try {
            final File toFile = new File(dbPath);
            FileUtils.copyStreamToFile(inputStream, toFile);
        } finally {
            setupDatabase(GBApplication.app());
        }

        // Validate database - must be AFTER setupDatabase so that helper is not null
        if (!helper.getReadableDatabase().isDatabaseIntegrityOk()) {
            throw new IOException("Database integrity is not OK");
        }
    }

    void exportDB(final File destFile) throws IllegalStateException, IOException {
        try {
            final String dbPath = getClosedDBPath();
            final File sourceFile = new File(dbPath);
            FileUtils.copyFile(sourceFile, destFile);
        } finally {
            setupDatabase(GBApplication.app());
        }
    }

    void exportDB(final OutputStream dest) throws IOException {
        try {
            final String dbPath = getClosedDBPath();
            final File source = new File(dbPath);
            FileUtils.copyFileToStream(source, dest);
        } finally {
            setupDatabase(GBApplication.app());
        }
    }

    /**
     * Closes the database and returns its name.
     * Important: after calling this, you have set the database up again.
     */
    String getClosedDBPath() throws IllegalStateException {
        final SQLiteDatabase db = daoMaster.getDatabase();
        final String path = db.getPath();
        closeDatabase();
        if (db.isOpen()) { // reference counted, so may still be open
            throw new IllegalStateException("Database must be closed");
        }
        return path;
    }
}
