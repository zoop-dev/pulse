/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiStressSampleProvider extends AbstractTimeSampleProvider<HuaweiStressSample> {

    public HuaweiStressSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiStressSample, ?> getSampleDao() {
        return getSession().getHuaweiStressSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiStressSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiStressSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiStressSample createSample() {
        return new HuaweiStressSample();
    }

    @NonNull
    @Override
    public List<HuaweiStressSample> getAllSamples(long timestampFrom, long timestampTo) {
        final long delta = 300000;
        final long interval = 1800000;
        List<HuaweiStressSample> samples = super.getAllSamples(timestampFrom, timestampTo);
        List<HuaweiStressSample> newSamples = new ArrayList<>();
        for (HuaweiStressSample sample : samples) {
            long startTime = (((sample.getStartTime() / interval)) * interval) + delta;
            long endTime = (((sample.getTimestamp() / interval) + 1) * interval) - delta;
            for (long i = startTime; i < endTime; i += delta) {
                if (i > timestampFrom && i < timestampTo) {
                    newSamples.add(new HuaweiStressSample(i, sample.getDeviceId(), sample.getUserId(), sample.getStress(), sample.getLevel(), sample.getStartTime()));
                }
            }
        }
        return newSamples;
    }

}
