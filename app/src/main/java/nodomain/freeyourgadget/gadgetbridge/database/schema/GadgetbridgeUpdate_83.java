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
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;

public class GadgetbridgeUpdate_83 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(HuaweiWorkoutPaceSampleDao.TABLENAME, HuaweiWorkoutPaceSampleDao.Properties.PaceIndex.columnName, db) && !DBHelper.existsColumn(HuaweiWorkoutPaceSampleDao.TABLENAME, HuaweiWorkoutPaceSampleDao.Properties.PointIndex.columnName, db)) {
            String MOVE_DATA_TO_TEMP_TABLE = "ALTER TABLE " + HuaweiWorkoutPaceSampleDao.TABLENAME + " RENAME TO " +HuaweiWorkoutPaceSampleDao.TABLENAME + "_temp;";
            db.execSQL(MOVE_DATA_TO_TEMP_TABLE);

            String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS \"" + HuaweiWorkoutPaceSampleDao.TABLENAME +
                    "\" (\""+HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.columnName+"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.PaceIndex.columnName +"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.Distance.columnName +"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.Type.columnName +"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.Pace.columnName +"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.PointIndex.columnName +"\" INTEGER  NOT NULL ," +
                    "\""+ HuaweiWorkoutPaceSampleDao.Properties.Correction.columnName +"\" INTEGER ," +
                    "PRIMARY KEY (\""+HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.columnName+"\" ,\""+ HuaweiWorkoutPaceSampleDao.Properties.PaceIndex.columnName +"\" ,\""+ HuaweiWorkoutPaceSampleDao.Properties.Distance.columnName +"\", \""+ HuaweiWorkoutPaceSampleDao.Properties.Type.columnName +"\") ON CONFLICT REPLACE) WITHOUT ROWID;";
            db.execSQL(CREATE_TABLE);

            String MIGATE_DATA = "INSERT INTO " + HuaweiWorkoutPaceSampleDao.TABLENAME
                    + " (" +HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.columnName+ ","
                    + HuaweiWorkoutPaceSampleDao.Properties.PaceIndex.columnName + ","
                    + HuaweiWorkoutPaceSampleDao.Properties.Distance.columnName+ ","
                    + HuaweiWorkoutPaceSampleDao.Properties.Type.columnName + ","
                    + HuaweiWorkoutPaceSampleDao.Properties.Pace.columnName + ","
                    + HuaweiWorkoutPaceSampleDao.Properties.PointIndex.columnName + ","
                    + HuaweiWorkoutPaceSampleDao.Properties.Correction.columnName + ") "
                    + " SELECT WORKOUT_ID, -1, DISTANCE, TYPE, PACE, 0, CORRECTION  FROM " +HuaweiWorkoutPaceSampleDao.TABLENAME + "_temp;";

            db.execSQL(MIGATE_DATA);

            String DROP_TEMP_TABLE = "drop table if exists " +HuaweiWorkoutPaceSampleDao.TABLENAME + "_temp;";
            db.execSQL(DROP_TEMP_TABLE);
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}
