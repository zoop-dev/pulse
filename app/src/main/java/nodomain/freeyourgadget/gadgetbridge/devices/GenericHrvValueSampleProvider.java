/*  Copyright (C) 2025  Thomas Kuehne

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

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GenericHrvValueSampleProvider extends AbstractTimeSampleProvider<GenericHrvValueSample> {
    public GenericHrvValueSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<GenericHrvValueSample, ?> getSampleDao() {
        return getSession().getGenericHrvValueSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GenericHrvValueSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GenericHrvValueSampleDao.Properties.DeviceId;
    }

    @Override
    public GenericHrvValueSample createSample() {
        return new GenericHrvValueSample();
    }
}
