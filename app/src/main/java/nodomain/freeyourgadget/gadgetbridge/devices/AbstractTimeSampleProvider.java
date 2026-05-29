/*  Copyright (C) 2023-2026 José Rebelo, Thomas Kuehne

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
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractTimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Base class for all time sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 *
 * @param <T> the sample type
 */
public abstract class AbstractTimeSampleProvider<T extends AbstractTimeSample> implements TimeSampleProvider<T>, PersistanceProvider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTimeSampleProvider.class);

    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractTimeSampleProvider(final GBDevice device, final DaoSession session) {
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
    @Override
    public List<T> getAllSamples(final long timestampFrom, final long timestampTo) {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Property timestampProperty = getTimestampSampleProperty();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestampFrom))
                .where(timestampProperty.le(timestampTo));
        final List<T> samples = qb.build().list();
        detachFromSession();
        return samples;
    }

    @Override
    public void addSample(final T activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addSamples(final List<T> activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    @Nullable
    @Override
    public T getLatestSample() {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderDesc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    @Override
    public T getLatestSample(final long until) {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(getTimestampSampleProperty().le(until))
                .where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    public T getLastSampleBefore(final long timestampTo) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final Property deviceIdSampleProp = getDeviceIdentifierSampleProperty();
        final Property timestampSampleProp = getTimestampSampleProperty();
        final List<T> samples = getSampleDao().queryBuilder()
                .where(deviceIdSampleProp.eq(dbDevice.getId()),
                        timestampSampleProp.le(timestampTo))
                .orderDesc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    @Nullable
    public T getNextSampleAfter(final long timestampFrom) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final Property deviceIdSampleProp = getDeviceIdentifierSampleProperty();
        final Property timestampSampleProp = getTimestampSampleProperty();
        final List<T> samples = getSampleDao().queryBuilder()
                .where(deviceIdSampleProp.eq(dbDevice.getId()),
                        timestampSampleProp.ge(timestampFrom))
                .orderAsc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    @Nullable
    @Override
    public T getFirstSample() {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderAsc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    /**
     * Detaches all samples of this type from the session. Changes to them may not be
     * written back to the database.
     * <p>
     * Subclasses should call this method after performing custom queries.
     */
    protected void detachFromSession() {
        getSampleDao().detachAll();
    }

    @NonNull
    public abstract AbstractDao<T, ?> getSampleDao();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();

    /// @deprecated use {@link #persistSamples(List, Context)} instead
    @Deprecated
    public void persistForDevice(@NonNull final Context context, @Nullable final GBDevice gbDevice, @NonNull final List<T> samples) {
        persistSamples(samples, context);
    }

    @Override
    public boolean persistSamples(@NonNull final List<T> samples, @Nullable final Context context) {
        if (samples.isEmpty()) {
            return true;
        }

        LOG.debug(
                "Will persist {} {} samples",
                samples.size(),
                getClass().getSimpleName().replace("SampleProvider", "")
        );

        try {
            final DaoSession session = getSession();

            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.findDevice(gbDevice, session);
            if (device == null) {
                LOG.warn("Device not found in database for '{}'", gbDevice.getAliasOrName());
                return false;
            }
            final long deviceId = device.getId();

            final User user = DBHelper.getUser(session);
            final long userId = user.getId();

            for (final T sample : samples) {
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
            }

            addSamples(samples);
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
