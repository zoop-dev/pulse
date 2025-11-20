package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdRealtimeSampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class GadgetbridgeUpdate_118 implements DBUpdateScript {
    private void addColumnIfNotExists(final SQLiteDatabase database,
                                      final String tableName,
                                      final String columnName,
                                      final int defaultValue) {
        if (!DBHelper.existsColumn(tableName, columnName, database)) {
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " INTEGER NOT NULL DEFAULT " + defaultValue + ";");
        }
    }

    private void addTemperatureColumnsIfNotExists(SQLiteDatabase database, String tableName, int defaultTemperatureType, int defaultTemperatureLocation) {
        // Add TemperatureType column if it doesn't exist
        if (!DBHelper.existsColumn(tableName, GenericTemperatureSampleDao.Properties.TemperatureType.columnName, database)) {
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + GenericTemperatureSampleDao.Properties.TemperatureType.columnName + " INTEGER NOT NULL DEFAULT " + defaultTemperatureType);
        }
        // Add TemperatureLocation column if it doesn't exist
        if (!DBHelper.existsColumn(tableName, GenericTemperatureSampleDao.Properties.TemperatureLocation.columnName, database)) {
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + GenericTemperatureSampleDao.Properties.TemperatureLocation.columnName + " INTEGER NOT NULL DEFAULT " + defaultTemperatureLocation);
        }
    }

    @Override
    public void upgradeSchema(SQLiteDatabase database) {
        addColumnIfNotExists(database, HuaweiTemperatureSampleDao.TABLENAME, HuaweiTemperatureSampleDao.Properties.TemperatureType.columnName, TemperatureSample.TYPE_SKIN);
        addColumnIfNotExists(database, HuaweiTemperatureSampleDao.TABLENAME, HuaweiTemperatureSampleDao.Properties.TemperatureLocation.columnName, TemperatureSample.LOCATION_WRIST);

        addColumnIfNotExists(database, ColmiTemperatureSampleDao.TABLENAME, ColmiTemperatureSampleDao.Properties.TemperatureType.columnName, TemperatureSample.TYPE_SKIN);
        addColumnIfNotExists(database, ColmiTemperatureSampleDao.TABLENAME, ColmiTemperatureSampleDao.Properties.TemperatureLocation.columnName, TemperatureSample.LOCATION_WRIST);

        addColumnIfNotExists(database, FemometerVinca2TemperatureSampleDao.TABLENAME, FemometerVinca2TemperatureSampleDao.Properties.TemperatureType.columnName, TemperatureSample.TYPE_BODY);
        addColumnIfNotExists(database, FemometerVinca2TemperatureSampleDao.TABLENAME, FemometerVinca2TemperatureSampleDao.Properties.TemperatureLocation.columnName, TemperatureSample.LOCATION_MOUTH);

        addColumnIfNotExists(database, MijiaLywsdRealtimeSampleDao.TABLENAME, MijiaLywsdRealtimeSampleDao.Properties.TemperatureType.columnName, TemperatureSample.TYPE_AMBIENT);
        addColumnIfNotExists(database, MijiaLywsdRealtimeSampleDao.TABLENAME, MijiaLywsdRealtimeSampleDao.Properties.TemperatureLocation.columnName, TemperatureSample.LOCATION_UNKNOWN);

        addColumnIfNotExists(database, GenericTemperatureSampleDao.TABLENAME, GenericTemperatureSampleDao.Properties.TemperatureType.columnName, TemperatureSample.TYPE_UNKNOWN);
        addColumnIfNotExists(database, GenericTemperatureSampleDao.TABLENAME, GenericTemperatureSampleDao.Properties.TemperatureLocation.columnName, TemperatureSample.LOCATION_UNKNOWN);
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}

