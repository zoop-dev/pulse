/*  Copyright (C) 2026 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public interface PersistanceProvider<T> {
    /// Set the relevant metadata for all samples like device ID, user ID, etc. and then
    /// stores them all in the database. Existing samples with identical primary key
    /// already stored in the database will be replaced.
    ///
    /// @param context optional context for displaying error messages
    /// @return {@code false} if at least one sample couldn't be persisted.
    boolean persistSamples(@NonNull List<T> samples, @Nullable Context context);

    /// @see #persistSamples(List, Context) 
    default boolean persistSamples(@NonNull T sample, @Nullable Context context) {
        return persistSamples(Collections.singletonList(sample), context);
    }
}
