/*  Copyright (C) 2023-2024 José Rebelo, Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.AbstractMijiaLywsdCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsdHistoricSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsdRealtimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdHistoricSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdRealtimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class MijiaLywsdSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MijiaLywsdSupport.class);

    private static final UUID UUID_BASE_SERVICE = UUID.fromString("ebe0ccb0-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_TIME = UUID.fromString("ebe0ccb7-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_BATTERY = UUID.fromString("ebe0ccc4-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_SCALE = UUID.fromString("ebe0ccbe-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_HISTORY = UUID.fromString("ebe0ccbc-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_LIVE_DATA = UUID.fromString("ebe0ccc1-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_HISTORY_LAST_ID = UUID.fromString("ebe0ccba-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_COMFORT_LEVEL = UUID.fromString("ebe0ccd7-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_CONN_INTERVAL = UUID.fromString("ebe0ccd8-7a0a-4b0c-8a1a-6ff2997da3a6");

    private final DeviceInfoProfile<MijiaLywsdSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = intent -> {
        String s = intent.getAction();
        if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
            handleDeviceInfo(Objects.requireNonNull(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)));
        }
    };

    // Length of comfort level characteristic for different devices
    public static final int COMFORT_LEVEL_LENGTH_LYWSD03 = 6;
    public static final int COMFORT_LEVEL_LENGTH_XMWSDJ04 = 8;

    private int startupTime = 0;

    public MijiaLywsdSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(MijiaLywsdSupport.UUID_BASE_SERVICE);
        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        requestDeviceInfo(builder);

        final boolean supportsSetTime = getCoordinator().supportsSetTime();
        if (supportsSetTime && GBApplication.getPrefs().syncTime()) {
            setTime(builder);
        } else {
            getTime(builder);
        }

        // TODO: We can't enable this without properly handling the historic data id and live data, otherwise
        //  it will cause battery drain on both the phone and device
        //builder.notify(getCharacteristic(MijiaLywsdSupport.UUID_HISTORY), true);
        //builder.notify(getCharacteristic(MijiaLywsdSupport.UUID_LIVE_DATA), true);

        getBatteryInfo(builder);
        getComfortLevel(builder);
        setConnectionInterval(builder);
        setInitialized(builder);
        return builder;
    }

    protected AbstractMijiaLywsdCoordinator getCoordinator() {
        return (AbstractMijiaLywsdCoordinator) gbDevice.getDeviceCoordinator();
    }

    private void setTime(TransactionBuilder builder) {
        long ts = System.currentTimeMillis();
        byte offsetHours = (byte) (SimpleTimeZone.getDefault().getOffset(ts) / (1000 * 60 * 60));
        ts = (ts + 250 + 500) / 1000; // round to seconds with +250 ms to compensate for BLE connection interval
        builder.write(MijiaLywsdSupport.UUID_TIME, new byte[]{
                (byte) (ts & 0xff),
                (byte) ((ts >> 8) & 0xff),
                (byte) ((ts >> 16) & 0xff),
                (byte) ((ts >> 24) & 0xff),
                offsetHours});
    }

    private void setConnectionInterval(TransactionBuilder builder) {
        builder.write(MijiaLywsdSupport.UUID_CONN_INTERVAL, new byte[]{(byte) 0xf4, (byte) 0x01}); // maximum interval of 500 ms
    }

    private void getBatteryInfo(TransactionBuilder builder) {
        builder.read(MijiaLywsdSupport.UUID_BATTERY);
    }

    private void getComfortLevel(TransactionBuilder builder) {
        builder.read(MijiaLywsdSupport.UUID_COMFORT_LEVEL);
    }

    private void getTime(TransactionBuilder builder) {
        builder.read(MijiaLywsdSupport.UUID_TIME);
    }

    private void setTemperatureScale(TransactionBuilder builder, SharedPreferences prefs) {
        String scale = prefs.getString(PREF_TEMPERATURE_SCALE_CF, "");
        builder.write(MijiaLywsdSupport.UUID_SCALE, new byte[]{(byte) ("f".equals(scale) ? 0x01 : 0xff)});
    }

    private void setComfortLevel(TransactionBuilder builder, SharedPreferences prefs) {
        int length = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_CHARACTERISTIC_LENGTH, 0);
        int temperatureLower = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER, 19);
        int temperatureUpper = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER, 27);
        int humidityLower = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER, 20);
        int humidityUpper = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER, 85);

        // Ignore invalid values
        if (temperatureLower > temperatureUpper || humidityLower > humidityUpper)
            return;

        ByteBuffer buf = ByteBuffer.allocate(length);

        buf.order(ByteOrder.LITTLE_ENDIAN);

        switch (length) {
            case COMFORT_LEVEL_LENGTH_LYWSD03:
                buf.putShort((short)(temperatureUpper * 100));
                buf.putShort((short)(temperatureLower * 100));
                buf.put((byte)humidityUpper);
                buf.put((byte)humidityLower);
                break;
            case COMFORT_LEVEL_LENGTH_XMWSDJ04:
                buf.putShort((short)(temperatureUpper * 10));
                buf.putShort((short)(temperatureLower * 10));
                buf.putShort((short)(humidityUpper * 10));
                buf.putShort((short)(humidityLower * 10));
                break;
            default:
                return;
        }

        builder.write(MijiaLywsdSupport.UUID_COMFORT_LEVEL, buf.array());
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleBatteryInfo: {}", status);
            return;
        }

        batteryCmd.level = ((short) value[0]);
        batteryCmd.state = (batteryCmd.level > 20) ? BatteryState.BATTERY_NORMAL : BatteryState.BATTERY_LOW;
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleHistory(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleHistory: {}", status);
            return;
        }

        if (value.length != 14) {
            LOG.warn("Unexpected history length {}", value.length);
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int id = buf.getInt();
        final int uptimeOffset = buf.getInt();
        final int maxTemperature = buf.getShort();
        final int maxHumidity = buf.get() & 0xff;
        final int minTemperature = buf.getShort();
        final int minHumidity = buf.get() & 0xff;

        // Devices that do not support setting the time report the live data as an offset from the uptime
        // other devices report the correct timestamp.
        final int ts = (!getCoordinator().supportsSetTime() ? startupTime : 0) + uptimeOffset;

        LOG.info(
                "Got history: id={}, uptimeOffset={}, ts={}, minTemperature={}, maxTemperature={}, minHumidity={}, maxHumidity={}",
                id,
                uptimeOffset,
                ts,
                minTemperature / 10.0f,
                maxTemperature / 10.0f,
                minHumidity,
                maxHumidity
        );

        if (!getCoordinator().supportsSetTime() && startupTime <= 0) {
            LOG.warn("Startup time is unknown - ignoring sample");
            return;
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final MijiaLywsdHistoricSampleProvider sampleProvider = new MijiaLywsdHistoricSampleProvider(gbDevice, session);

            final MijiaLywsdHistoricSample sample = sampleProvider.createSample();
            sample.setTimestamp(ts * 1000L);
            sample.setMinTemperature(minTemperature / 10.0f);
            sample.setMaxTemperature(maxTemperature / 10.0f);
            sample.setMinHumidity(minHumidity);
            sample.setMaxHumidity(maxHumidity);
            sample.setDevice(device);
            sample.setUser(user);

            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving historic sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving historic samples", e);
        }
    }

    private void handleLiveData(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleLiveData: {}", status);
            return;
        }

        if (value.length != 5) {
            LOG.warn("Unexpected live data length {}", value.length);
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int temperature = buf.getShort();
        final int humidity = buf.get() & 0xff;
        final int voltage = buf.getShort();

        LOG.info(
                "Got mijia live data: temperature={}, humidity={}, voltage={}",
                temperature / 100f,
                humidity,
                voltage / 1000f
        );

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final MijiaLywsdRealtimeSampleProvider sampleProvider = new MijiaLywsdRealtimeSampleProvider(gbDevice, session);

            final MijiaLywsdRealtimeSample sample = sampleProvider.createSample();
            sample.setTimestamp(System.currentTimeMillis());
            sample.setTemperature(temperature / 100.0f);
            sample.setHumidity(humidity);
            sample.setDevice(device);
            sample.setUser(user);

            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving live sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving live samples", e);
        }

        // Warning: this voltage value is not reliable, so I am not sure
        // it's even worth using
        batteryCmd.voltage = voltage / 1000f;
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleTime(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleTime: {}", status);
            return;
        }

        if (value.length != 4) {
            LOG.warn("Unexpected time length {}", value.length);
            return;
        }

        final int uptime = BLETypeConversions.toUint32(value);

        startupTime = (int) ((System.currentTimeMillis() / 1000L) - uptime);

        LOG.info("Got mijia time={}, startupTime={}", uptime, startupTime);
    }

    private void handleComfortLevel(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS)
            return;

        ByteBuffer buf = ByteBuffer.wrap(value);
        int temperatureLower, temperatureUpper;
        int humidityLower, humidityUpper;

        buf.order(ByteOrder.LITTLE_ENDIAN);

        switch (value.length) {
            case COMFORT_LEVEL_LENGTH_LYWSD03:
                temperatureUpper = buf.getShort() / 100;
                temperatureLower = buf.getShort() / 100;
                humidityUpper = buf.get();
                humidityLower = buf.get();
                break;
            case COMFORT_LEVEL_LENGTH_XMWSDJ04:
                temperatureUpper = buf.getShort() / 10;
                temperatureLower = buf.getShort() / 10;
                humidityUpper = buf.getShort() / 10;
                humidityLower = buf.getShort() / 10;
                break;
            default:
                LOG.error("Unknown comfort level characteristic: {}", hexdump(value));
                return;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        prefs.edit()
             .putInt(PREF_MIJIA_LYWSD_COMFORT_CHARACTERISTIC_LENGTH, value.length)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER, temperatureLower)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER, temperatureUpper)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER, humidityLower)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER, humidityUpper)
             .apply();
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZED);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    @Override
    public void onSetTime() {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Set time");
            setTime(builder);
            builder.queue();
        } catch (IOException e) {
            LOG.error("Error setting time on LYWSD02", e);
            GB.toast("Error setting time on LYWSD02", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();

        if (MijiaLywsdSupport.UUID_HISTORY.equals(characteristicUUID)) {
            handleHistory(value, BluetoothGatt.GATT_SUCCESS);
            return true;
        }

        if (MijiaLywsdSupport.UUID_LIVE_DATA.equals(characteristicUUID)) {
            handleLiveData(value, BluetoothGatt.GATT_SUCCESS);
            return true;
        }

        if (MijiaLywsdSupport.UUID_TIME.equals(characteristicUUID)) {
            handleTime(value, BluetoothGatt.GATT_SUCCESS);
            return true;
        }

        LOG.warn("Unhandled characteristic changed: {}", characteristicUUID);
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, byte[] value,
                                        int status) {
        if (super.onCharacteristicRead(gatt, characteristic, value, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        if (MijiaLywsdSupport.UUID_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(value, status);
            return true;
        }

        if (MijiaLywsdSupport.UUID_COMFORT_LEVEL.equals(characteristicUUID)) {
            handleComfortLevel(value, status);
            return true;
        }

        if (MijiaLywsdSupport.UUID_HISTORY.equals(characteristicUUID)) {
            handleHistory(value, status);
            return true;
        }

        if (MijiaLywsdSupport.UUID_LIVE_DATA.equals(characteristicUUID)) {
            handleLiveData(value, status);
            return true;
        }

        if (MijiaLywsdSupport.UUID_TIME.equals(characteristicUUID)) {
            handleTime(value, status);
            return true;
        }

        LOG.warn("Unhandled characteristic read: {}", characteristicUUID);
        return false;
    }

    @Override
    public void onSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());

        try {
            TransactionBuilder builder = performInitialized("Sending configuration for option: " + config);

            switch (config) {
                case PREF_TEMPERATURE_SCALE_CF:
                    setTemperatureScale(builder, prefs);
                    break;
                case PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER:
                case PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER:
                case PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER:
                case PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER:
                    setComfortLevel(builder, prefs);
                    break;
            }

            builder.queue();
        } catch (IOException e) {
            LOG.error("Error setting configuration on LYWSD02", e);
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
