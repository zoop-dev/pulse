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

package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadChronicSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTrainingLoadChronicSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GenericTrainingLoadChronicSampleProvider extends AbstractTimeSampleProvider<GenericTrainingLoadChronicSample> {
    public GenericTrainingLoadChronicSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<GenericTrainingLoadChronicSample, ?> getSampleDao() {
        return getSession().getGenericTrainingLoadChronicSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GenericTrainingLoadChronicSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GenericTrainingLoadChronicSampleDao.Properties.DeviceId;
    }

    @Override
    public GenericTrainingLoadChronicSample createSample() {
        return new GenericTrainingLoadChronicSample();
    }
}
