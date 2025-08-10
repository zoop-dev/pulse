package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.AlarmDao;

public class GadgetbridgeUpdate_112 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(AlarmDao.TABLENAME, AlarmDao.Properties.SoundCode.columnName, db)) {
            String ADD_COLUMN_SOUND_CODE = "ALTER TABLE " + AlarmDao.TABLENAME + " ADD COLUMN "
                    + AlarmDao.Properties.SoundCode.columnName + " INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(ADD_COLUMN_SOUND_CODE);
        }

        if (!DBHelper.existsColumn(AlarmDao.TABLENAME, AlarmDao.Properties.Backlight.columnName, db)) {
            String ADD_COLUMN_SOUND_CODE = "ALTER TABLE " + AlarmDao.TABLENAME + " ADD COLUMN "
                    + AlarmDao.Properties.Backlight.columnName + " BOOLEAN NOT NULL DEFAULT TRUE;";
            db.execSQL(ADD_COLUMN_SOUND_CODE);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
