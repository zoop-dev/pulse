/*  Copyright (C) 2023 Daniele Gobbetti

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.sbm_67;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.SBM67BloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.SBM67BloodPressureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SBM67BloodPressureSampleProvider extends AbstractTimeSampleProvider<SBM67BloodPressureSample> {
    public SBM67BloodPressureSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<SBM67BloodPressureSample, ?> getSampleDao() {
        return getSession().getSBM67BloodPressureSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return SBM67BloodPressureSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return SBM67BloodPressureSampleDao.Properties.DeviceId;
    }

    @Override
    public SBM67BloodPressureSample createSample() {
        return new SBM67BloodPressureSample();
    }
}
