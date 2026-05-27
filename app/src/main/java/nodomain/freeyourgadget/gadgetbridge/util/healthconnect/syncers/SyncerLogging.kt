/*  Copyright (C) 2025 Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util.healthconnect.syncers

import nodomain.freeyourgadget.gadgetbridge.util.healthconnect.HealthConnectUtils
import org.slf4j.Logger

internal fun Logger.skipOutOfRange(
    deviceName: String,
    fieldLabel: String,
    value: Any?,
    rangeDescription: String
) {
    info(
        "${HealthConnectUtils.HC_SYNC_TAG} Skipping {} for device '{}': value {} out of range ({}).",
        fieldLabel, deviceName, value, rangeDescription
    )
}
