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
    private void addTemperatureColumnsIfNotExists(SQLiteDatabase database, String tableName, int defaultTemperatureType, int defaultTemperatureLocation) {
        // Add TemperatureType column if it doesn't exist
        if (!DBHelper.existsColumn(tableName, GenericTemperatureSampleDao.Properties.TemperatureType.columnName, database)) {
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + GenericTemperatureSampleDao.Properties.TemperatureType.columnName + " INTEGER DEFAULT " + defaultTemperatureType);
        }
        // Add TemperatureLocation column if it doesn't exist
        if (!DBHelper.existsColumn(tableName, GenericTemperatureSampleDao.Properties.TemperatureLocation.columnName, database)) {
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + GenericTemperatureSampleDao.Properties.TemperatureLocation.columnName + " INTEGER DEFAULT " + defaultTemperatureLocation);
        }
    }

    @Override
    public void upgradeSchema(SQLiteDatabase database) {
        addTemperatureColumnsIfNotExists(database, HuaweiTemperatureSampleDao.TABLENAME, TemperatureSample.TYPE_SKIN, TemperatureSample.LOCATION_WRIST);
        addTemperatureColumnsIfNotExists(database, ColmiTemperatureSampleDao.TABLENAME, TemperatureSample.TYPE_SKIN, TemperatureSample.LOCATION_WRIST);
        addTemperatureColumnsIfNotExists(database, FemometerVinca2TemperatureSampleDao.TABLENAME, TemperatureSample.TYPE_BODY, TemperatureSample.LOCATION_MOUTH);
        addTemperatureColumnsIfNotExists(database, MijiaLywsdRealtimeSampleDao.TABLENAME, TemperatureSample.TYPE_AMBIENT, TemperatureSample.LOCATION_UNKNOWN);
        addTemperatureColumnsIfNotExists(database, GenericTemperatureSampleDao.TABLENAME, TemperatureSample.TYPE_UNKNOWN, TemperatureSample.LOCATION_UNKNOWN);
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
