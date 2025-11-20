package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSampleDao;

public class GadgetbridgeUpdate_119  implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.HrvDayToBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.HrvDayToBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MaxHrvBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MaxHrvBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MinHrvBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MinHrvBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.AvgHrv.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.AvgHrv.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.BreathRateDayToBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.BreathRateDayToBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MaxBreathRateBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MaxBreathRateBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MinBreathRateBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MinBreathRateBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.AvgBreathRate.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.AvgBreathRate.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.OxygenSaturationDayToBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.OxygenSaturationDayToBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MaxOxygenSaturationBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MaxOxygenSaturationBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MinOxygenSaturationBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MinOxygenSaturationBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.AvgOxygenSaturation.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.AvgOxygenSaturation.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.HeartRateDayToBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.HeartRateDayToBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MaxHeartRateBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MaxHeartRateBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.MinHeartRateBaseline.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.MinHeartRateBaseline.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.AvgHeartRate.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.AvgHeartRate.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.Rdi.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.Rdi.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.WakeCount.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.WakeCount.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.TurnOverCount.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.TurnOverCount.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.PrepareSleepTime.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.PrepareSleepTime.columnName + "\" LONG NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.WakeUpFeeling.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.WakeUpFeeling.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiSleepStatsSampleDao.TABLENAME, HuaweiSleepStatsSampleDao.Properties.SleepVersion.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiSleepStatsSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiSleepStatsSampleDao.Properties.SleepVersion.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiActivitySampleDao.TABLENAME, HuaweiActivitySampleDao.Properties.RestingHeartRate.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiActivitySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiActivitySampleDao.Properties.RestingHeartRate.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase database) {

    }
}