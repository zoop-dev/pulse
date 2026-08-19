/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleToTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;

public class XiaomiStressSampleProvider extends AbstractSampleToTimeSampleProvider<StressSample, XiaomiActivitySample> {
    private final XiaomiManualSampleProvider manualSampleProvider;

    public XiaomiStressSampleProvider(final GBDevice device, final DaoSession session) {
        super(new XiaomiSampleProvider(device, session), device, session);
        manualSampleProvider = new XiaomiManualSampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<StressSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<StressSample> samples = new ArrayList<>(super.getAllSamples(timestampFrom, timestampTo));

        for (final XiaomiManualSample manualSample : manualSampleProvider.getAllSamples(timestampFrom, timestampTo)) {
            final StressSample converted = convertManualSample(manualSample);
            if (converted != null) {
                samples.add(converted);
            }
        }

        samples.sort(Comparator.comparingLong(StressSample::getTimestamp));
        return samples;
    }

    @Nullable
    @Override
    protected StressSample convertSample(final XiaomiActivitySample sample) {
        if (sample == null || sample.getStress() == null || sample.getStress() == 0) {
            return null;
        }

        return new XiaomiStressSample(
                sample.getTimestamp() * 1000L,
                sample.getStress(),
                StressSample.Type.UNKNOWN
        );
    }

    @Nullable
    private StressSample convertManualSample(final XiaomiManualSample sample) {
        if (sample == null || sample.getType() != XiaomiManualSampleProvider.TYPE_STRESS || sample.getValue() <= 0) {
            return null;
        }

        return new XiaomiStressSample(
                sample.getTimestamp(),
                sample.getValue(),
                StressSample.Type.MANUAL
        );
    }

    @Nullable
    @Override
    public StressSample getLatestSample() {
        return latest(
                super.getLatestSample(),
                convertManualSample(manualSampleProvider.getLatestSample(XiaomiManualSampleProvider.TYPE_STRESS))
        );
    }

    @Nullable
    @Override
    public StressSample getLatestSample(final long until) {
        return latest(
                super.getLatestSample(until),
                convertManualSample(manualSampleProvider.getLatestSample(XiaomiManualSampleProvider.TYPE_STRESS, until))
        );
    }

    @Nullable
    @Override
    public StressSample getFirstSample() {
        return earliest(
                super.getFirstSample(),
                convertManualSample(manualSampleProvider.getFirstSample(XiaomiManualSampleProvider.TYPE_STRESS))
        );
    }

    @Nullable
    private static StressSample latest(@Nullable final StressSample first, @Nullable final StressSample second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.getTimestamp() >= second.getTimestamp() ? first : second;
    }

    @Nullable
    private static StressSample earliest(@Nullable final StressSample first, @Nullable final StressSample second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.getTimestamp() <= second.getTimestamp() ? first : second;
    }

    @NonNull
    @Override
    protected Property getFilterColumn() {
        return XiaomiActivitySampleDao.Properties.Stress;
    }

    protected static class XiaomiStressSample implements StressSample {
        private final long timestamp;
        private final int stress;
        private final Type type;

        public XiaomiStressSample(final long timestamp, final int stress, final Type type) {
            this.timestamp = timestamp;
            this.stress = stress;
            this.type = type;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int getStress() {
            return stress;
        }
    }
}
