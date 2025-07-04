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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import android.widget.Toast;

import androidx.annotation.StringRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiSleepSessionSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSleepSessionSample;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFetcher;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches sleep sessions.
 */
public class FetchSleepSessionOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSleepSessionOperation.class);

    public FetchSleepSessionOperation(final HuamiFetcher fetcher) {
        super(fetcher, HuamiFetchDataType.SLEEP_SESSION);
    }

    @StringRes
    @Override
    public int taskDescription() {
        return R.string.busy_task_fetch_sleep_data;
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if (bytes.length % 594 != 0) {
            LOG.error("Unexpected length for sleep session data {}, not divisible by 594", bytes.length);
            return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final List<HuamiSleepSessionSample> samples = new ArrayList<>();

        while (buf.hasRemaining()) {
            final byte[] session = new byte[594];
            buf.get(session);

            final int timestampSeconds = BLETypeConversions.toUint32(session, 0);
            timestamp.setTimeInMillis(timestampSeconds * 1000L);

            final HuamiSleepSessionSample sample = new HuamiSleepSessionSample();
            sample.setTimestamp(timestamp.getTimeInMillis());
            sample.setData(session);
            samples.add(sample);

            LOG.trace("Sleep session at {}: {}", timestamp.getTime(), GB.hexdump(session));
        }

        return persistSamples(samples);
    }

    protected boolean persistSamples(final List<HuamiSleepSessionSample> samples) {
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final HuamiSleepSessionSampleProvider sampleProvider = new HuamiSleepSessionSampleProvider(getDevice(), session);

            sampleProvider.persistForDevice(getContext(), getDevice(), samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving sleep session samples", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSleepSessionTimeMillis";
    }
}
