/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiUtils;

public class ZeppOsBtleSupport extends AbstractBTLESingleDeviceSupport implements ZeppOsCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsBtleSupport.class);

    private final ZeppOsSupport zeppOsSupport = new ZeppOsSupport(this);

    private final DeviceInfoProfile<ZeppOsBtleSupport> deviceInfoProfile;

    public ZeppOsBtleSupport() {
        super(LOG);

        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);

        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
        addSupportedService(HuamiService.UUID_SERVICE_FIRMWARE_SERVICE);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(intent -> {
            String s = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(s)) {
                final DeviceInfo info = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                LOG.debug("Device info: {}", info);
                if (info == null) {
                    return;
                }
                final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
                versionCmd.hwVersion = info.getHardwareRevision();
                versionCmd.fwVersion = info.getFirmwareRevision();
                if (versionCmd.fwVersion == null) {
                    versionCmd.fwVersion = info.getSoftwareRevision();
                }
                if (versionCmd.fwVersion != null && !versionCmd.fwVersion.isEmpty() && versionCmd.fwVersion.charAt(0) == 'V') {
                    versionCmd.fwVersion = versionCmd.fwVersion.substring(1);
                }
                handleGBDeviceEvent(versionCmd);
            }
        });
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        zeppOsSupport.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            zeppOsSupport.dispose();
            super.dispose();
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic characteristicChunked2021Read = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ);
        final BluetoothGattCharacteristic characteristicChunked2021Write = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE);
        if (characteristicChunked2021Write == null || characteristicChunked2021Read == null) {
            LOG.warn("Chunked 2021 characteristics are null, will attempt to reconnect");
            builder.setDeviceState(GBDevice.State.WAITING_FOR_RECONNECT);
            return builder;
        }

        builder.notify(characteristicChunked2021Read, true);

        final ZeppOsBtleTransactionBuilder zeppOsTransactionBuilder = new ZeppOsBtleTransactionBuilder(this, builder);

        // #3219 - Reset the MTU before re-initializing the device, otherwise initialization will sometimes fail
        zeppOsSupport.setMtu(23);
        zeppOsSupport.initializeDevice(zeppOsTransactionBuilder);

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            // handled upstream
            return true;
        }

        return zeppOsSupport.onCharacteristicChanged(characteristic.getUuid(), value);
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        zeppOsSupport.setMtu(mtu);
    }

    // =============================================================================================
    // ZeppOsCommunicator
    // =============================================================================================

    @Override
    public ZeppOsTransactionBuilder createZeppOsTransactionBuilder(final String taskName) {
        return new ZeppOsBtleTransactionBuilder(this, taskName);
    }

    @Override
    public void setCurrentTime(final ZeppOsTransactionBuilder builder) {
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME, HuamiUtils.getCurrentTimeBytes());
    }

    @Override
    public void requestDeviceInfo(final ZeppOsTransactionBuilder builder) {
        deviceInfoProfile.requestDeviceInfo(((ZeppOsBtleTransactionBuilder) builder).getTransactionBuilder());
    }

    @Override
    public void onAuthenticationSuccess(final ZeppOsTransactionBuilder builder) {
        if (getDevicePrefs().allowHighMtu()) {
            ((ZeppOsBtleTransactionBuilder) builder).getTransactionBuilder().requestMtu(247);
        }
    }

    // =============================================================================================
    // Pass-through to zeppOsSupport
    // =============================================================================================

    @Override
    public void onSendConfiguration(final String config) {
        zeppOsSupport.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        zeppOsSupport.onTestNewFunction();
    }

    @Override
    public void onFindDevice(final boolean start) {
        zeppOsSupport.onFindDevice(start);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        zeppOsSupport.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onFindPhone(final boolean start) {
        zeppOsSupport.onFindPhone(start);
    }

    @Override
    public void onScreenshotReq() {
        zeppOsSupport.onScreenshotReq();
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        zeppOsSupport.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        zeppOsSupport.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        zeppOsSupport.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onHeartRateTest() {
        zeppOsSupport.onHeartRateTest();
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        zeppOsSupport.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onSetAlarms(final ArrayList<? extends Alarm> alarms) {
        zeppOsSupport.onSetAlarms(alarms);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        zeppOsSupport.onSetCallState(callSpec);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        zeppOsSupport.onNotification(notificationSpec);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        zeppOsSupport.onSetReminders(reminders);
    }

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {
        zeppOsSupport.onSetWorldClocks(clocks);
    }

    @Override
    public void onSetLoyaltyCards(final ArrayList<LoyaltyCard> cards) {
        zeppOsSupport.onSetLoyaltyCards(cards);
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        zeppOsSupport.onSetContacts(contacts);
    }

    @Override
    public void onDeleteNotification(final int id) {
        zeppOsSupport.onDeleteNotification(id);
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        zeppOsSupport.onSetGpsLocation(location);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        zeppOsSupport.onSetCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        zeppOsSupport.onSetPhoneVolume(volume);
    }

    @Override
    public void onSetMusicState(final MusicStateSpec stateSpec) {
        zeppOsSupport.onSetMusicState(stateSpec);
    }

    @Override
    public void onSetMusicInfo(final MusicSpec musicSpec) {
        zeppOsSupport.onSetMusicInfo(musicSpec);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        zeppOsSupport.onEnableRealtimeSteps(enable);
    }

    @Override
    public void onInstallApp(final Uri uri, @NonNull final Bundle options) {
        zeppOsSupport.onInstallApp(uri, options);
    }

    @Override
    public void onAppInfoReq() {
        zeppOsSupport.onAppInfoReq();
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        zeppOsSupport.onAppStart(uuid, start);
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        zeppOsSupport.onAppDelete(uuid);
    }

    @Override
    public void onEnableHeartRateSleepSupport(final boolean enable) {
        zeppOsSupport.onEnableHeartRateSleepSupport(enable);
    }

    @Override
    public void onSetTime() {
        zeppOsSupport.onSetTime();
    }

    @Override
    public void onSendWeather() {
        zeppOsSupport.onSendWeather();
    }

    @Override
    public void onSleepAsAndroidAction(final String action, final Bundle extras) {
        zeppOsSupport.onSleepAsAndroidAction(action, extras);
    }
}
