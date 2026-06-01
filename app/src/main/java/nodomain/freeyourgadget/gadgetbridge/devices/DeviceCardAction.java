/*  Copyright (C) 2025-2026 José Rebelo, Thomas Kuehne

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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface DeviceCardAction {
    @DrawableRes
    int getIcon(@NonNull final GBDevice device);

    @NonNull
    String getDescription(@NonNull final GBDevice device, @NonNull final Context context);

    @Nullable
    default String getLabel(@NonNull final GBDevice device, @NonNull final Context context) {
        return null;
    }

    default boolean isVisible(@NonNull final GBDevice device) {
        return device.isConnected();
    }

    void onClick(@NonNull final GBDevice device, @NonNull final Context context);
}
