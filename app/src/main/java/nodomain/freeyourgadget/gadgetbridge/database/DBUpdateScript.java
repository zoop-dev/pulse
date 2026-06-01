/*  Copyright (C) 2015-2026 Andreas Shimokawa, Carsten Pfeiffer, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

/**
 * Interface for updating a database schema.
 * Implementors provide the update from the prior schema
 * version to this version, and the downgrade from this schema
 * version to the next lower version.
 * <p>
 * Implementations must have a public, no-arg constructor.
 * </p>
 */
public interface DBUpdateScript {
    void upgradeSchema(@NonNull SQLiteDatabase database);

    void downgradeSchema(@NonNull SQLiteDatabase database);
}
