/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.content.Context;

import androidx.annotation.StringRes;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface ZeppOsTransactionBuilder {
    void setProgress(@StringRes final int textRes, final boolean ongoing, final int percentage, final Context context);

    void setDeviceState(final GBDevice device, final GBDevice.State deviceState, final Context context);

    void setBusy(final GBDevice device, @StringRes final int stringRes, final Context context);

    void notify(final UUID characteristic, final boolean enable);

    void write(final UUID characteristic, final byte[] arr);

    void queue(final ZeppOsSupport support);
}
