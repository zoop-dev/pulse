package nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.coordinator;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.activity.CyclingLiveDataActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.db.CyclingSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor.support.CyclingSensorSupport;

public class CyclingSensorCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public Map<AbstractDao<?, ?>, Property> getAllDeviceDao(@NonNull final DaoSession session) {
        Map<AbstractDao<?, ?>, Property> map = new HashMap<>(1);
        map.put(session.getCyclingSampleDao(), CyclingSampleDao.Properties.DeviceId);
        return map;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(CyclingSensorSupport.UUID_CYCLING_SENSOR_SERVICE);
    }

    @Override
    public boolean supportsCyclingData(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracking(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public TimeSampleProvider<CyclingSample> getCyclingSampleProvider(GBDevice device, DaoSession session) {
        return new CyclingSampleProvider(device, session);
    }

    @Override
    public boolean supportsSleepMeasurement(@NonNull GBDevice device) {
        return false;
    }
    @Override
    public boolean supportsStepCounter(@NonNull GBDevice device) {
        return false;
    }
    @Override
    public boolean supportsSpeedzones(@NonNull GBDevice device) {
        return false;
    }
    @Override
    public boolean supportsActivityTabs(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity(final GBDevice device) {
        return CyclingLiveDataActivity.class;
    }

    @Override
    public boolean supportsRealtimeData(@NonNull GBDevice device) {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_cycling_sensor
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return CyclingSensorSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_cycling_sensor;
    }

    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return true;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.UNKNOWN;
    }
}
