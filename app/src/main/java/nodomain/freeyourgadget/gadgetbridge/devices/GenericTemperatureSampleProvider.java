/*  Copyright (C) 2025  Thomas Kuehne, José Rebelo, Gideon Zenz
    Copyright (C) 2023-2024 Alicia Hormann (Original AbstractTemperatureSample)

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
import androidx.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class GenericTemperatureSampleProvider extends AbstractTimeSampleProvider<GenericTemperatureSample> {
    private final int defaultType;
    private final int defaultLocation;

    public GenericTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        this(device, session, TemperatureSample.TYPE_UNKNOWN, TemperatureSample.LOCATION_UNKNOWN);
    }

    public GenericTemperatureSampleProvider(final GBDevice device,
                                            final DaoSession session,
                                            final int defaultType,
                                            final int defaultLocation) {
        super(device, session);
        this.defaultType = defaultType;
        this.defaultLocation = defaultLocation;
    }

    private GenericTemperatureSample applyDefaults(final GenericTemperatureSample sample) {
        if (sample != null) {
            if (defaultType != TemperatureSample.TYPE_UNKNOWN && sample.getTemperatureType() == TemperatureSample.TYPE_UNKNOWN) {
                sample.setTemperatureType(defaultType);
            }
            if (defaultLocation != TemperatureSample.LOCATION_UNKNOWN && sample.getTemperatureLocation() == TemperatureSample.LOCATION_UNKNOWN) {
                sample.setTemperatureLocation(defaultLocation);
            }
        }
        return sample;
    }

    @NonNull
    @Override
    public AbstractDao<GenericTemperatureSample, ?> getSampleDao() {
        return getSession().getGenericTemperatureSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GenericTemperatureSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GenericTemperatureSampleDao.Properties.DeviceId;
    }

    @Override
    public GenericTemperatureSample createSample() {
        final GenericTemperatureSample sample = new GenericTemperatureSample();
        sample.setTemperatureType(defaultType);
        sample.setTemperatureLocation(defaultLocation);
        return sample;
    }

    @NonNull
    @Override
    public List<GenericTemperatureSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        return super.getAllSamples(timestampFrom, timestampTo).stream()
                .map(this::applyDefaults)
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public GenericTemperatureSample getLatestSample() {
        return applyDefaults(super.getLatestSample());
    }

    @Nullable
    @Override
    public GenericTemperatureSample getLatestSample(final long until) {
        return applyDefaults(super.getLatestSample(until));
    }

    @Nullable
    public GenericTemperatureSample getLastSampleBefore(final long timestampTo) {
        return applyDefaults(super.getLastSampleBefore(timestampTo));
    }

    @Nullable
    public GenericTemperatureSample getNextSampleAfter(final long timestampFrom) {
        return applyDefaults(super.getNextSampleAfter(timestampFrom));
    }

    @Nullable
    @Override
    public GenericTemperatureSample getFirstSample() {
        return applyDefaults(super.getFirstSample());
    }
}