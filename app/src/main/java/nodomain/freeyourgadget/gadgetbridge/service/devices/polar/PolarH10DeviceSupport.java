package nodomain.freeyourgadget.gadgetbridge.service.devices.polar;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ANTPLUS_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DUAL_CONNECTION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_GYMLINK_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_HR_BROADCAST;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.CallSuper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.HeartRrIntervalSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.polar.PolarH10ActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.HeartRrIntervalSample;
import nodomain.freeyourgadget.gadgetbridge.entities.PolarH10ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.SensorContact;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PolarH10DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private final DeviceInfoProfile<PolarH10DeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<PolarH10DeviceSupport> batteryInfoProfile;
    private final HeartRateProfile<PolarH10DeviceSupport> heartRateProfile;
    private static final Logger LOG = LoggerFactory.getLogger(PolarH10DeviceSupport.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    private boolean newSamples = false;

    public static final UUID UUID_SERVICE_DEVICE_INFORMATION = GattService.UUID_SERVICE_DEVICE_INFORMATION;
    public static final UUID UUID_SERVICE_BATTERY_SERVICE = GattService.UUID_SERVICE_BATTERY_SERVICE;
    public static final UUID UUID_SERVICE_HEART_RATE = GattService.UUID_SERVICE_HEART_RATE;
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT;

    public static final UUID UUID_SERVICE_POLAR_SETTINGS = UUID.fromString("6217ff4b-fb31-1140-ad5a-a45545d7ecf3");
    public static final UUID UUID_CHARACTERISTIC_POLAR_SETTINGS = UUID.fromString("6217ff4d-91bb-91d0-7e2a-7cd3bda8a1f3");

    public static final byte CMD_HR_BROADCAST_SET = (byte) 0x01;
    public static final byte CMD_HR_BROADCAST_GET = (byte) 0x02;
    public static final byte CMD_GYM_LINK_SET = (byte) 0x03;
    public static final byte CMD_GYM_LINK_GET = (byte) 0x04;
    public static final byte CMD_DUAL_CONNECTION_SET = (byte) 0x08;
    public static final byte CMD_DUAL_CONNECTION_GET = (byte) 0x09;
    public static final byte CMD_ANT_SET = (byte) 0x0a;
    public static final byte CMD_ANT_GET = (byte) 0x0b;
    public static final byte CMD_RESPONSE = (byte) 0xf0;

    public PolarH10DeviceSupport() {
        super(LOG);

        addSupportedService(UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(UUID_SERVICE_HEART_RATE);
        addSupportedService(UUID_SERVICE_POLAR_SETTINGS);

        IntentListener mListener = intent -> {
            String action = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                handleDeviceInfo(Objects.requireNonNull(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)));
            }

            if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                handleBatteryInfo(Objects.requireNonNull(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)));
            }

            if (HeartRateProfile.ACTION_HEART_RATE.equals(action)) {
                handleHeartRate(Objects.requireNonNull(intent.getParcelableExtra(HeartRateProfile.EXTRA_HEART_RATE)));
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);

        heartRateProfile = new HeartRateProfile<>(this);
        heartRateProfile.addListener(mListener);
        addSupportedProfile(heartRateProfile);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);

        deviceInfoProfile.requestDeviceInfo(builder);

        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        heartRateProfile.enableNotify(builder, true);

        builder.notify(UUID_CHARACTERISTIC_POLAR_SETTINGS, true);
        builder.write(UUID_CHARACTERISTIC_POLAR_SETTINGS, CMD_HR_BROADCAST_GET);
        builder.write(UUID_CHARACTERISTIC_POLAR_SETTINGS, CMD_GYM_LINK_GET);
        builder.write(UUID_CHARACTERISTIC_POLAR_SETTINGS, CMD_DUAL_CONNECTION_GET);
        builder.write(UUID_CHARACTERISTIC_POLAR_SETTINGS, CMD_ANT_GET);

        // Set defaults
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // Enter initialized state
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @CallSuper
    @Override
    public void disconnect() {
        if (newSamples) {
            // Since we always receive samples in realtime, signal that there are new samples when we disconnect
            GB.signalActivityDataFinish(getDevice());
            newSamples = false;
        }

        super.disconnect();
    }

    @CallSuper
    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (newSamples) {
                // Since we always receive samples in realtime, signal that there are new samples when we disconnect
                GB.signalActivityDataFinish(getDevice());
                newSamples = false;
            }

            super.dispose();
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        if (UUID_CHARACTERISTIC_POLAR_SETTINGS.equals(characteristic.getUuid())) {
            handlePolarSetting(value);
            return true;
        }

        LOG.warn("Unhandled characteristic change: {} = {}", characteristic.getUuid(), GB.hexdump(value));

        return false;
    }

    @Override
    public void onSendConfiguration(final String config) {
        final byte preferenceCommand;
        final boolean preferenceValue;

        switch (config) {
            case PREF_GYMLINK_ENABLED:
                preferenceCommand = CMD_GYM_LINK_SET;
                preferenceValue = getDevicePrefs().getBoolean(config, false);
                break;
            case PREF_ANTPLUS_ENABLED:
                preferenceCommand = CMD_ANT_SET;
                preferenceValue = getDevicePrefs().getBoolean(config, false);
                break;
            case PREF_HR_BROADCAST:
                preferenceCommand = CMD_HR_BROADCAST_SET;
                preferenceValue = getDevicePrefs().getBoolean(config, false);
                break;
            case PREF_DUAL_CONNECTION:
                preferenceCommand = CMD_DUAL_CONNECTION_SET;
                preferenceValue = getDevicePrefs().getBoolean(config, false);
                break;
            default:
                super.onSendConfiguration(config);
                return;
        }

        LOG.debug("Setting {} = {}", config, preferenceValue);

        final TransactionBuilder builder = createTransactionBuilder("set " + config + " = " + preferenceValue);
        builder.write(UUID_CHARACTERISTIC_POLAR_SETTINGS, preferenceCommand, (byte) (preferenceValue ? 1 : 0));
        builder.queue();
    }

    private void handlePolarSetting(final byte[] value) {
        if (value[0] != CMD_RESPONSE) {
            LOG.warn("Unexpected first byte {}", String.format("0x%02x", value[0]));
            return;
        }

        switch (value[1]) {
            case CMD_HR_BROADCAST_SET:
                LOG.debug("HR broadcast set response, success = {}", value[2]);
                break;
            case CMD_HR_BROADCAST_GET:
                if (value[2] != 0x01) {
                    LOG.warn("HR broadcast get failed, status = {}", String.format("0x%02x", value[2]));
                    return;
                }
                LOG.debug("Got HR broadcast setting, value = {}", value[3] != 0);
                evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_HR_BROADCAST, value[3] != 0));
                break;
            case CMD_GYM_LINK_SET:
                LOG.debug("GymLink set response, success = {}", value[2]);
                break;
            case CMD_GYM_LINK_GET:
                if (value[2] != 0x01) {
                    LOG.warn("GymLink get failed, status = {}", String.format("0x%02x", value[2]));
                    return;
                }
                LOG.debug("Got GymLink setting, value = {}", value[3] != 0);
                evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_GYMLINK_ENABLED, value[3] != 0));
                break;
            case CMD_DUAL_CONNECTION_SET:
                LOG.debug("Dual connection set response, success = {}", value[2]);
                break;
            case CMD_DUAL_CONNECTION_GET:
                if (value[2] != 0x01) {
                    LOG.warn("Dual connection get failed, status = {}", String.format("0x%02x", value[2]));
                    return;
                }
                LOG.debug("Got dual connection setting, value = {}", value[3] != 0);
                evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_DUAL_CONNECTION, value[3] != 0));
                break;
            case CMD_ANT_SET:
                LOG.debug("ANT+ set response, success = {}", value[2]);
                break;
            case CMD_ANT_GET:
                if (value[2] != 0x01) {
                    LOG.warn("ANT+ get failed, status = {}", String.format("0x%02x", value[2]));
                    return;
                }
                LOG.debug("Got ANT+ setting, value = {}", value[3] != 0);
                evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_ANTPLUS_ENABLED, value[3] != 0));
                break;
            default:
                LOG.warn("Unknown response byte: {}", String.format("0x%02x", value[1]));
        }
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: {}", info);

        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        versionCmd.fwVersion2 = info.getSoftwareRevision();

        handleGBDeviceEvent(versionCmd);
    }

    private void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        LOG.debug("Battery info: {}", info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleHeartRate(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRate info) {
        LOG.debug("Heart Rate: {}", info);

        if (info == null || info.getSensorContact() == SensorContact.CONTACT_NOT_DETECTED || info.getHeartRate() <= 0) {
            return;
        }

        try (DBHandler db = GBApplication.acquireDB()) {
            final PolarH10ActivitySampleProvider polarSampleProvider = new PolarH10ActivitySampleProvider(this.getDevice(), db.getDaoSession());
            final Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            final Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
            final PolarH10ActivitySample sample = new PolarH10ActivitySample((int) (info.getTimestamp() / 1000), deviceId, userId, info.getHeartRate());
            polarSampleProvider.addGBActivitySamples(Collections.singletonList(sample));

            final ArrayList<@NotNull Integer> rrIntervals = info.getRrIntervals();
            if (!rrIntervals.isEmpty()) {
                final List<HeartRrIntervalSample> rrIntervalSampleList = new ArrayList<>();
                for (int i = 0; i < rrIntervals.size(); i++) {
                    final HeartRrIntervalSample rrSample = new HeartRrIntervalSample();
                    rrSample.setTimestamp(info.getTimestamp());
                    rrSample.setSeq(i);
                    rrSample.setRrMillis(rrIntervals.get(i));
                    rrIntervalSampleList.add(rrSample);
                }

                final HeartRrIntervalSampleProvider rrIntervalSampleProvider = new HeartRrIntervalSampleProvider(this.getDevice(), db.getDaoSession());
                rrIntervalSampleProvider.persistForDevice(getContext(), getDevice(), rrIntervalSampleList);
            }

            newSamples = true;
        } catch (Exception e) {
            LOG.error("Error acquiring database", e);
        }
    }
}
