/*  Copyright (C) 2019-2025 vappster

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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.OVTouch26ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.overmax.OVTouch26DeviceSupport;

public class OVTouch26Coordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ovtouch_26;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miwatch;
    }

    @Override
    public String getManufacturer() {
        return "Overmax";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return OVTouch26DeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("OV-Touch2.6_LE");
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsFindDevice(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsDataFetching(@NonNull final GBDevice device) {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_disconnectnotification,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_transliteration
        };
    }

    @Override
    public boolean supportsWeather(GBDevice device) {
        return false;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.FITNESS_BAND;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new OVTouch26SampleProvider(device, session);
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(1);
        map.put(session.getOVTouch26ActivitySampleDao(), OVTouch26ActivitySampleDao.Properties.DeviceId);
        return map;
    }
}
