package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSpo2SampleDao;

public class GadgetbridgeUpdate_111 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(GarminSpo2SampleDao.TABLENAME, GarminSpo2SampleDao.Properties.TypeNum.columnName, db)) {
            final String statement = "ALTER TABLE " + GarminSpo2SampleDao.TABLENAME + " ADD COLUMN \""
                    + GarminSpo2SampleDao.Properties.TypeNum.columnName + "\" INTEGER NOT NULL DEFAULT 2;";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase database) {

    }
}
