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
package nodomain.freeyourgadget.gadgetbridge.devices

import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSleepStageSample
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSleepStageSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

class GenericSleepStageSampleProvider(
    device: GBDevice,
    session: DaoSession
) : AbstractTimeSampleProvider<GenericSleepStageSample?>(device, session) {
    override fun getSampleDao(): AbstractDao<GenericSleepStageSample?, *> {
        return session.genericSleepStageSampleDao
    }

    override fun getTimestampSampleProperty(): Property {
        return GenericSleepStageSampleDao.Properties.Timestamp
    }

    override fun getDeviceIdentifierSampleProperty(): Property {
        return GenericSleepStageSampleDao.Properties.DeviceId
    }

    override fun createSample(): GenericSleepStageSample {
        return GenericSleepStageSample()
    }
}
