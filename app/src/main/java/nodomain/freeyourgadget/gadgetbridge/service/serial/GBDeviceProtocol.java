/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo,
    Julien Pivotto, Steffen Liebergeld

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
package nodomain.freeyourgadget.gadgetbridge.service.serial;

import android.location.Location;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public abstract class GBDeviceProtocol {

    public static final int RESET_FLAGS_REBOOT = 1;
    public static final int RESET_FLAGS_FACTORY_RESET = 2;

    private GBDevice mDevice;

    protected GBDeviceProtocol(GBDevice device) {
        mDevice = device;
    }

    @Nullable
    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        return null;
    }

    @Nullable
    public byte[] encodeDeleteNotification(int id) {
        return null;
    }

    @Nullable
    public byte[] encodeSetTime() {
        return null;
    }

    @Nullable
    public byte[] encodeSetCallState(String number, String name, int command) {
        return null;
    }

    @Nullable
    public byte[] encodeSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        return null;
    }

    @Nullable
    public byte[] encodeSetMusicInfo(String artist, String album, String track, int duration, int trackCount, int trackNr) {
        return null;
    }

    @Nullable
    public byte[] encodeVolume(float volume) {
        return null;
    }

    @Nullable
    public byte[] encodeSetMusicState(byte state, int position, int playRate, byte shuffle, byte repeat) {
        return null;
    }

    @Nullable
    public byte[] encodeFirmwareVersionReq() {
        return null;
    }

    @Nullable
    public byte[] encodeAppInfoReq() {
        return null;
    }

    @Nullable
    public byte[] encodeScreenshotReq() {
        return null;
    }

    @Nullable
    public byte[] encodeAppDelete(UUID uuid) {
        return null;
    }

    @Nullable
    public byte[] encodeAppStart(UUID uuid, boolean start) {
        return null;
    }

    @Nullable
    public byte[] encodeAppReorder(UUID[] uuids) {
        return null;
    }

    @Nullable
    public byte[] encodeSynchronizeActivityData() {
        return null;
    }

    @Nullable
    public byte[] encodeReset(int flags) {
        return null;
    }

    @Nullable
    public byte[] encodeFindDevice(boolean start) {
        return null;
    }

    @Nullable
    public byte[] encodeFindPhone(boolean start) {
        return null;
    }

    @Nullable
    public byte[] encodeEnableRealtimeSteps(boolean enable) {
        return null;
    }

    @Nullable
    public byte[] encodeEnableHeartRateSleepSupport(boolean enable) {
        return null;
    }

    @Nullable
    public byte[] encodeEnableRealtimeHeartRateMeasurement(boolean enable) { return null; }

    @Nullable
    public byte[] encodeAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        return null;
    }

    @Nullable
    public byte[] encodeDeleteCalendarEvent(byte type, long id) {
        return null;
    }

    @Nullable
    public byte[] encodeSendConfiguration(String config) {
        return null;
    }

    @Nullable
    public byte[] encodeTestNewFunction() { return null; }

    @Nullable
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        return null;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    @Nullable
    public byte[] encodeSendWeather() {
        return null;
    }

    @Nullable
    public byte[] encodeLedColor(int color) {
        return null;
    }

    @Nullable
    public byte[] encodePowerOff() {
        return null;
    }

    @Nullable
    public byte[] encodeSetAlarms(ArrayList<? extends Alarm> alarms)  {
        return null;
    }

    @Nullable
    public byte[] encodeReminders(ArrayList<? extends Reminder> reminders) {
        return null;
    }

    @Nullable
    public byte[] encodeWorldClocks(ArrayList<? extends WorldClock> clocks) {
        return null;
    }

    @Nullable
    public byte[] encodeFmFrequency(float frequency) {
        return null;
    }

    @Nullable
    public byte[] encodeGpsLocation(Location location) {
        return null;
    }

    protected DevicePrefs getDevicePrefs() {
        return GBApplication.getDevicePrefs(getDevice());
    }
}
