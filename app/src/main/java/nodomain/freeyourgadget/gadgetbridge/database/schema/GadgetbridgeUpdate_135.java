/*  Copyright (C) 2026 Dany Mestas

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

import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiDailySummarySampleDao;

public class GadgetbridgeUpdate_135 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        final String table = XiaomiDailySummarySampleDao.TABLENAME;
        final Property[] newColumns = new Property[]{
                XiaomiDailySummarySampleDao.Properties.ActiveCalories,
                XiaomiDailySummarySampleDao.Properties.RecoveryHours,
        };
        for (final Property p : newColumns) {
            if (!DBHelper.existsColumn(table, p.columnName, db)) {
                db.execSQL("ALTER TABLE " + table + " ADD COLUMN \"" + p.columnName + "\" INTEGER");
            }
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}
