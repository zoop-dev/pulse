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

public class GadgetbridgeUpdate_84 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.MaxAltitude.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.MaxAltitude.columnName + "\" INTEGER";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.MinAltitude.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.MinAltitude.columnName + "\" INTEGER";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.ElevationGain.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.ElevationGain.columnName + "\" INTEGER";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.ElevationLoss.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.ElevationLoss.columnName + "\" INTEGER";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.WorkoutLoad.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.WorkoutLoad.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.WorkoutAerobicEffect.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.WorkoutAerobicEffect.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.WorkoutAnaerobicEffect.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.WorkoutAnaerobicEffect.columnName + "\" INTEGER NOT NULL DEFAULT -1;";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RecoveryTime.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RecoveryTime.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.MinHeartRatePeak.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.MinHeartRatePeak.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.MaxHeartRatePeak.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.MaxHeartRatePeak.columnName + "\" INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(HuaweiWorkoutSummarySampleDao.TABLENAME, HuaweiWorkoutSummarySampleDao.Properties.RecoveryHeartRates.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutSummarySampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutSummarySampleDao.Properties.RecoveryHeartRates.columnName + "\" BLOB";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}