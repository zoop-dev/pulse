package nodomain.freeyourgadget.gadgetbridge.service.devices.polar;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.CallSuper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
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

    public PolarH10DeviceSupport() {
        super(LOG);

        addSupportedService(UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(UUID_SERVICE_HEART_RATE);

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

        LOG.warn("Unhandled characteristic change: {} = {}", characteristic.getUuid(), GB.hexdump(value));

        return false;
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
            polarSampleProvider.addGBActivitySamples(new PolarH10ActivitySample[]{sample});

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
