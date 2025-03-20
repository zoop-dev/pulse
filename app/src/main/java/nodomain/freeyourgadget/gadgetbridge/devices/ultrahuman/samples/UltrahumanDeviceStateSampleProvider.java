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

package nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman.samples;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanDeviceStateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.UltrahumanDeviceStateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class UltrahumanDeviceStateSampleProvider extends AbstractTimeSampleProvider<UltrahumanDeviceStateSample> {
    public UltrahumanDeviceStateSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<UltrahumanDeviceStateSample, ?> getSampleDao() {
        return getSession().getUltrahumanDeviceStateSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return UltrahumanDeviceStateSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return UltrahumanDeviceStateSampleDao.Properties.DeviceId;
    }

    @Override
    public UltrahumanDeviceStateSample createSample() {
        return new UltrahumanDeviceStateSample();
    }
}
