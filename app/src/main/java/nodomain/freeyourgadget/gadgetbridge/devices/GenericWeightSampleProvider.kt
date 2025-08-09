/*  Copyright (C) 2025 Thomas Kuehne

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
import nodomain.freeyourgadget.gadgetbridge.entities.GenericWeightSample
import nodomain.freeyourgadget.gadgetbridge.entities.GenericWeightSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

class GenericWeightSampleProvider(device: GBDevice?, session: DaoSession?) :
    AbstractTimeSampleProvider<GenericWeightSample?>(device, session) {
    override fun getSampleDao(): AbstractDao<GenericWeightSample?, *> {
        return session.genericWeightSampleDao
    }

    override fun getTimestampSampleProperty(): Property {
        return GenericWeightSampleDao.Properties.Timestamp
    }

    override fun getDeviceIdentifierSampleProperty(): Property {
        return GenericWeightSampleDao.Properties.DeviceId
    }

    override fun createSample(): GenericWeightSample {
        return GenericWeightSample()
    }
}