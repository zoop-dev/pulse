/*  Copyright (C) 2019-2025 vappster, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.overmax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.OVTouch26ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.OVTouch26ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class OVTouch26SampleProvider extends AbstractSampleProvider<OVTouch26ActivitySample> {
    public static final int TYPE_ACTIVITY = 1;
    private static final Logger LOG = LoggerFactory.getLogger(OVTouch26SampleProvider.class);

    public OVTouch26SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 255.0f;
    }

    @Override
    public OVTouch26ActivitySample createActivitySample() {
        return new OVTouch26ActivitySample();
    }

    @Override
    public AbstractDao<OVTouch26ActivitySample, ?> getSampleDao() {
        return getSession().getOVTouch26ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return OVTouch26ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return OVTouch26ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return OVTouch26ActivitySampleDao.Properties.DeviceId;
    }

    @Override
    protected List<OVTouch26ActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        //Code from GarminActivitySampleProvider, thank you José Rebelo!
        LOG.trace(
                "Getting OVTouch26 activity samples between {} and {}",
                timestamp_from,
                timestamp_to
        );

        final long nanoStart = System.nanoTime();

        final List<OVTouch26ActivitySample> samples = fillGaps(
                super.getGBActivitySamples(timestamp_from + 60, timestamp_to + 60),
                timestamp_from + 60,
                timestamp_to + 60
        );

        samples.forEach(s -> s.setTimestamp(s.getTimestamp() - 60));

        if (!samples.isEmpty()) {
            convertCumulativeSteps(samples, OVTouch26ActivitySampleDao.Properties.Steps);
        }

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        LOG.trace("Getting OVTouch26 samples took {}ms", executionTime);

        return samples;
    }
}