package nodomain.freeyourgadget.gadgetbridge.service.devices.polar;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.polar.PolarH10ActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.PolarH10ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class PolarH10DeviceSupport extends AbstractBTLESingleDeviceSupport {
    private final DeviceInfoProfile<PolarH10DeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<PolarH10DeviceSupport> batteryInfoProfile;
    private static final Logger LOG = LoggerFactory.getLogger(PolarH10DeviceSupport.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

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

        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);

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

        builder.notify(UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, true);

        // Set defaults
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        // Enter initialized state
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
            int heartrate = Byte.toUnsignedInt(value[1]);
            LOG.debug("onCharacteristicChanged: HeartRateMeasurement:HeartRate={}", heartrate);
            this.processSamples(heartrate);
            return true;
        }

        LOG.info("Characteristic changed UUID: {}", characteristicUUID);
        LOG.info("Characteristic changed value: {}", StringUtils.bytesToHex(value));

        return false;
    }

    private void processSamples(int hr) {
        int timestamp = (int) (Calendar.getInstance().getTimeInMillis() / 1000L);
        try (DBHandler db = GBApplication.acquireDB()) {
            PolarH10ActivitySampleProvider sampleProvider = new PolarH10ActivitySampleProvider(this.getDevice(), db.getDaoSession());
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
            PolarH10ActivitySample sample = new PolarH10ActivitySample(timestamp, deviceId, userId, hr);
            sampleProvider.addGBActivitySamples(new PolarH10ActivitySample[]{sample});
        } catch (Exception e) {
            LOG.error("Error acquiring database", e);
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


}
