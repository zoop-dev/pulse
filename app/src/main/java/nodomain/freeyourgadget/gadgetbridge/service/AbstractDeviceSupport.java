/*  Copyright (C) 2015-2024 Alicia Hormann, Andreas Böhler, Andreas Shimokawa,
    Arjan Schrijver, Carsten Pfeiffer, Daniele Gobbetti, Davis Mosenkovs,
    Dmitriy Bogdanov, foxstidious, Ganblejs, José Rebelo, Pauli Salmenrinne,
    Petr Vaněk, Taavi Eomäe, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
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
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

// TODO: support option for a single reminder notification when notifications could not be delivered?
// conditions: app was running and received notifications, but device was not connected.
// maybe need to check for "unread notifications" on device for that.

/**
 * Abstract implementation of DeviceSupport with some implementations for
 * common functionality. Still transport independent.
 */
public abstract class AbstractDeviceSupport implements DeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceSupport.class);

    protected GBDevice gbDevice;
    private BluetoothAdapter btAdapter;
    private Context context;
    private boolean autoReconnect, scanReconnect;



    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        this.gbDevice = gbDevice;
        this.btAdapter = btAdapter;
        this.context = context;
    }

    /**
     * Default implementation just calls #connect()
     */
    @Override
    public boolean connectFirstTime() {
        return connect();
    }

    @Override
    public boolean isConnected() {
        return gbDevice.isConnected();
    }

    /**
     * Returns true if the device is not only connected, but also
     * initialized.
     *
     * @see GBDevice#isInitialized()
     */
    protected boolean isInitialized() {
        return gbDevice.isInitialized();
    }

    @Override
    public void setAutoReconnect(boolean enable) {
        autoReconnect = enable;
    }

    @Override
    public boolean getAutoReconnect() {
        return autoReconnect;
    }

    @Override
    public void setScanReconnect(boolean scanReconnect) {
        this.scanReconnect = scanReconnect;
    }

    @Override
    public boolean getScanReconnect(){
        return this.scanReconnect;
    }

    @Override
    public GBDevice getDevice() {
        return gbDevice;
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return btAdapter;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void evaluateGBDeviceEvent(final GBDeviceEvent deviceEvent) {
        LOG.debug("Evaluating event: {}", deviceEvent);
        deviceEvent.evaluate(context, gbDevice);
    }

    public void handleGBDeviceEvent(final GBDeviceEvent deviceEvent) {
        LOG.debug("Handling event: {}", deviceEvent);
        deviceEvent.evaluate(getContext(), gbDevice);
    }

    public DevicePrefs getDevicePrefs() {
        return GBApplication.getDevicePrefs(gbDevice);
    }

    @Override
    public String customStringFilter(String inputString) {
        return inputString;
    }


    // Empty functions following, leaving optional implementation up to child classes

    /**
     * If the device supports a "find phone" functionality, this method can
     * be overridden and implemented by the device support class.
     * @param start true if starting the search, false if stopping
     */
    @Override
    public void onFindPhone(boolean start) {

    }

    /**
     * If the device supports a "find device" functionality, this method can
     * be overridden and implemented by the device support class.
     * @param start true if starting the search, false if stopping
     */
    @Override
    public void onFindDevice(boolean start) {

    }

    /**
     * If the device supports a "set FM frequency" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param frequency the FM frequency to set
     */
    @Override
    public void onSetFmFrequency(float frequency) {

    }

    /**
     * If the device supports a "set LED color" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param color the new color, in ARGB, with alpha = 255
     */
    @Override
    public void onSetLedColor(int color) {

    }

    /**
     * If the device can be turned off by sending a command, this method
     * can be overridden and implemented by the device support class.
     */
    @Override
    public void onPowerOff() {

    }

    /**
     * If the device has a functionality to set the phone volume, this method
     * can be overridden and implemented by the device support class.
     * @param volume the volume percentage (0 to 100).
     */
    @Override
    public void onSetPhoneVolume(final float volume) {

    }

    /**
     * Called when the phone's interruption filter or ringer mode is changed.
     * @param ringerMode as per {@link android.media.AudioManager#getRingerMode()}
     */
    @Override
    public void onChangePhoneSilentMode(int ringerMode) {

    }

    /**
     * If the device can receive the GPS location from the phone, this method
     * can be overridden and implemented by the device support class.
     * @param location {@link android.location.Location} object containing the current GPS coordinates
     */
    @Override
    public void onSetGpsLocation(Location location) {

    }

    /**
     * If reminders can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param reminders {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Reminder} instances
     */
    @Override
    public void onSetReminders(ArrayList<? extends Reminder> reminders) {

    }

    /**
     * If loyalty cards can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param cards {@link java.util.ArrayList} containing {@link LoyaltyCard} instances
     */
    @Override
    public void onSetLoyaltyCards(ArrayList<LoyaltyCard> cards) {

    }

    /**
     * If world clocks can be configured on the device, this method can be
     * overridden and implemented by the device support class.
     * @param clocks {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.WorldClock} instances
     */
    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {

    }

    /**
     * If contacts can be configured on the device, this method can be
     * overridden and implemented by the device support class.
     * @param contacts {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Contact} instances
     */
    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {

    }

    /**
     * If the device can receive and display notifications, this method
     * can be overridden and implemented by the device support class.
     * @param notificationSpec notification details
     */
    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    /**
     * If notifications can be deleted from the device, this method can be
     * overridden and implemented by the device support class.
     * @param id the unique notification identifier
     */
    @Override
    public void onDeleteNotification(int id) {

    }

    /**
     * If the time can be set on the device, this method can be
     * overridden and implemented by the device support class.
     */
    @Override
    public void onSetTime() {

    }

    /**
     * If alarms can be set on the device, this method can be
     * overridden and implemented by the device support class.
     * @param alarms {@link java.util.ArrayList} containing {@link nodomain.freeyourgadget.gadgetbridge.model.Alarm} instances
     */
    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    /**
     * If the device can receive and show or handle phone call details, this
     * method can be overridden and implemented by the device support class.
     * @param callSpec the call state details
     */
    @Override
    public void onSetCallState(CallSpec callSpec) {

    }

    /**
     * If the device has a "canned messages" functionality, this method
     * can be overridden and implemented by the device support class.
     * @param cannedMessagesSpec the canned messages to send to the device
     */
    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    /**
     * If the music play state can be set on the device, this method
     * can be overridden and implemented by the device support class.
     * @param stateSpec the current state of music playback
     */
    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    /**
     * If the music information can be shown on the device, this method can be
     * overridden and implemented by the device support class.
     * @param musicSpec the current music information, like track name and artist
     */
    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    /**
     * If apps can be installed on the device, this method can be
     * overridden and implemented by the device support class.
     *
     * @param uri     reference to a watch app file
     * @param options a bundle of custom options
     */
    @Override
    public void onInstallApp(Uri uri, @NonNull final Bundle options) {

    }

    /**
     * If the list of apps on the device can be retrieved, this method
     * can be overridden and implemented by the device support class.
     */
    @Override
    public void onAppInfoReq() {

    }

    /**
     * If the device supports starting an app with a command, this method
     * can be overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     * @param start true to start, false to stop the app (if supported)
     */
    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    /**
     * If apps can be downloaded from the device, this method can be
     * overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     */
    @Override
    public void onAppDownload(UUID uuid) {

    }

    /**
     * If apps on the device can be deleted with a command, this method
     * can be overridden and implemented by the device support class.
     * @param uuid the Gadgetbridge internal UUID of the app
     */
    @Override
    public void onAppDelete(UUID uuid) {

    }

    /**
     * If apps on the device can be configured, this method can be
     * overridden and implemented by the device support class.
     * @param appUuid the Gadgetbridge internal UUID of the app
     * @param config the configuration of the app
     * @param id
     */
    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    /**
     * If apps on the device can be reordered, this method can be
     * overridden and implemented by the device support class.
     * @param uuids array of Gadgetbridge internal UUIDs of the apps
     */
    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    /**
     * If recorded data can be fetched from the device, this method
     * can be overridden and implemented by the device support class.
     * @param dataTypes which data types to fetch
     */
    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    /**
     * If a device can be reset with a command, this method can be
     * overridden and implemented by the device support class.
     * @param flags can be used to pass flags with the reset command
     */
    @Override
    public void onReset(int flags) {

    }

    /**
     * If the device can perform a heart rate measurement on request, this
     * method can be overridden and implemented by the device support class.
     */
    @Override
    public void onHeartRateTest() {

    }

    /**
     * If the device has the functionality to enable/disable realtime heart rate measurement,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable realtime heart rate measurement
     */
    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    /**
     * If the device has the functionality to enable/disable realtime steps information,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable realtime steps
     */
    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    /**
     * If the device has a functionality to enable constant vibration, this
     * method can be overridden and implemented by the device support class.
     * @param integer the vibration intensity
     */
    @Override
    public void onSetConstantVibration(int integer) {

    }

    /**
     * If the device supports taking screenshots of the screen, this method can
     * be overridden and implemented by the device support class.
     */
    @Override
    public void onScreenshotReq() {

    }

    /**
     * If the device has a toggle to enable the use of heart rate for sleep detection,
     * this method can be overridden and implemented by the device support class.
     * @param enable true to enable, false to disable using heart rate for sleep detection
     */
    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    /**
     * If the heart rate measurement interval can be changed on the device,
     * this method can be overridden and implemented by the device support class.
     * @param seconds the interval to configure on the device
     */
    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    /**
     * If calendar events can be sent to the device, this method can be
     * overridden and implemented by the device support class.
     * @param calendarEventSpec calendar event details
     */
    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    /**
     * If calendar events can be deleted from the device, this method can
     * be overridden and implemented by the device support class.
     * @param type type of calendar event
     * @param id id of calendar event
     */
    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    /**
     * If configuration options can be set on the device, this method
     * can be overridden and implemented by the device support class.
     * @param config the device specific option to set on the device
     */
    @Override
    public void onSendConfiguration(String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR:
            case DeviceSettingsPreferenceConst.PREF_SYNC_BIRTHDAYS:
                CalendarReceiver.forceSync(getDevice());
                break;
        }
    }

    /**
     * If the configuration can be retrieved from the device, this method
     * can be overridden and implemented by the device support class.
     * @param config the device specific option to get from the device
     */
    @Override
    public void onReadConfiguration(String config) {

    }

    /**
     * If the device can receive weather information, this method can be
     * overridden and implemented by the device support class. Support classes
     * should use {@link Weather#getWeatherSpecs()} to obtain the list of WeatherSpecs.
     */
    @Override
    public void onSendWeather() {

    }

    /**
     * For testing new features, this method can be overridden and
     * implemented by the device support class.
     * It's called by clicking the "test new functionality" button
     * in the Debug menu.
     */
    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {

    }

    @Override
    public void onSleepAsAndroidAction(String action, Bundle extras) {

    }

    @Override
    public void onCameraStatusChange(GBDeviceEventCameraRemote.Event event, String filename) {}

    @Override
    public void onMusicListReq() {}

    @Override
    public void onMusicOperation(int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {}

    @Override
    public boolean canReconnect() {
        final boolean defaultValue = true;
        DevicePrefs prefs = getDevicePrefs();
        if (prefs != null) {
            return prefs.getBoolean(DeviceSettingsPreferenceConst.PREFS_DEVICE_SUPPORT_CAN_RECONNECT,
                    defaultValue);
        }
        return defaultValue;
    }
}
