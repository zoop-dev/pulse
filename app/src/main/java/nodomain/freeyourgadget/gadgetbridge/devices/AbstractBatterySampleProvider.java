/*  Copyright (C) 2026 José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractBatterySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Interface to retrieve battery samples from the database, and also create and add samples to the database.
 * <p>
 * Note that the provided samples must typically be considered read-only, because they are immediately
 * removed from the session before they are returned.
 *
 * @param <T> the device/provider specific sample type (must extend AbstractBatterySample).
 */
public abstract class AbstractBatterySampleProvider<T extends AbstractBatterySample> implements PersistenceProvider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBatterySampleProvider.class);

    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractBatterySampleProvider(final GBDevice device, final DaoSession session) {
        mDevice = device;
        mSession = session;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @NonNull
    public abstract AbstractDao<T, ?> getSampleDao();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();

    @NonNull
    protected abstract Property getBatteryIndexSampleProperty();

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider.
     *
     * @return the newly created "empty" sample
     */
    public abstract T createSample();

    @NonNull
    public List<T> getAllSamples(final int batteryIndex, final long timestampFrom, final long timestampTo) {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Property timestampProperty = getTimestampSampleProperty();
        final Property batteryIndexProperty = getBatteryIndexSampleProperty();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestampFrom))
                .where(timestampProperty.le(timestampTo))
                .where(batteryIndexProperty.eq(batteryIndex));
        final List<T> samples = qb.build().list();
        getSampleDao().detachAll();
        return samples;
    }

    @Override
    public boolean persistSamples(@NonNull final List<T> samples, @Nullable final Context context) {
        if (samples.isEmpty()) {
            return true;
        }

        LOG.debug(
                "Will persist {} {} samples",
                samples.size(),
                getClass().getSimpleName().replace("SampleProvider", "").replace("Provider", "")
        );

        try {
            final DaoSession session = getSession();

            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.findDevice(gbDevice, session);
            if (device == null) {
                LOG.warn("Device not found in database for '{}'", gbDevice.getAliasOrName());
                return false;
            }
            @SuppressWarnings("DataFlowIssue") final long deviceId = device.getId();

            for (final T sample : samples) {
                sample.setDeviceId(deviceId);
            }

            getSampleDao().insertOrReplaceInTx(samples);
        } catch (final Exception e) {
            LOG.error("Error saving samples", e);
            final Context ctx = (context != null) ? context : GBApplication.getContext();
            final String message = ctx.getString(R.string.persisting_samples_failed, e.getLocalizedMessage());
            toast(ctx, message, Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }
        return true;
    }
}
