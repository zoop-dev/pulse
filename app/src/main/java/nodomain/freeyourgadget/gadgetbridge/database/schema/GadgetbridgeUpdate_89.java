/*  Copyright (C) 2024 Me7c7

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;

public class GadgetbridgeUpdate_89 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.MaxMET.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.MaxMET.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.HrZoneType.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.HrZoneType.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone1Min.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone1Min.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone2Min.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone2Min.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone3Min.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone3Min.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone4Min.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone4Min.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Min.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Min.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Max.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Max.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone1Time.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone1Time.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone2Time.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone2Time.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone3Time.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone3Time.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone4Time.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone4Time.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Time.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RunPaceZone5Time.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.AlgType.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.AlgType.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.TrainingPoints.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.TrainingPoints.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}