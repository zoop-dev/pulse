/*  Copyright (C) 2024 Me7c7, José Rebelo

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
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictData;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValues;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValuesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiSleepStatsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiTemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class HuaweiTemperatureSampleProvider extends AbstractTimeSampleProvider<HuaweiTemperatureSample> {

    public HuaweiTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiTemperatureSample, ?> getSampleDao() {
        return getSession().getHuaweiTemperatureSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiTemperatureSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiTemperatureSampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiTemperatureSample createSample() {
        return new HuaweiTemperatureSample();
    }


    public long getLastFetchTimestamp() {
        QueryBuilder<HuaweiTemperatureSample> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return 0;
        final Property deviceProperty = HuaweiTemperatureSampleDao.Properties.DeviceId;
        final Property timestampProperty = HuaweiTemperatureSampleDao.Properties.LastTimestamp;

        qb.where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty)
                .limit(1);

        List<HuaweiTemperatureSample> samples = qb.build().list();
        if (samples.isEmpty())
            return 0;

        HuaweiTemperatureSample sample = samples.get(0);
        return sample.getLastTimestamp();
    }


}
