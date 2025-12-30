/*  Copyright (C) 2019-2024 Andreas Shimokawa, Daniel Dakhno, Ganblejs,
    José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class AutoConnectIntervalReceiver extends BroadcastReceiver {

    private final DeviceCommunicationService service;
    // Delay is in milliseconds
    private static int mDelay = Integer.MAX_VALUE;

    /// don't increase {@link #mDelay} while alarm is already scheduled
    private static volatile boolean mScheduled = false;
    private static volatile boolean mBackingOff = false;

    private static final Logger LOG = LoggerFactory.getLogger(AutoConnectIntervalReceiver.class);

    public AutoConnectIntervalReceiver(DeviceCommunicationService service) {
        this.service = service;
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        LocalBroadcastManager.getInstance(service).registerReceiver(this, filterLocal);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        GBDevice[] devices = service.getGBDevices();
        if (action.equals(DeviceManager.ACTION_DEVICES_CHANGED)) {
            boolean scheduleAutoConnect = false;
            boolean allDevicesInitialized = true;
            for (GBDevice device : devices) {
                if (!device.isInitialized()) {
                    allDevicesInitialized = false;
                    if (device.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                        scheduleAutoConnect = true;
                        // If we are not backing off, find the device with the lowest reconnect delay
                        // to set the value to.
                        if (!mBackingOff) {
                            mDelay = Math.min(mDelay, device.getDeviceCoordinator().getReconnectionDelay());
                        }
                    }
                }
            }

            if (allDevicesInitialized) {
                LOG.info("will reset connection delay, all devices are initialized!");
                mDelay = Integer.MAX_VALUE;
                mBackingOff = false;
                return;
            }
            if (scheduleAutoConnect && !mScheduled) {
                scheduleReconnect();
            }
        } else if (action.equals("GB_RECONNECT")) {
            mScheduled = false;
            for (GBDevice device : devices) {
                if (device.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                    LOG.info("time based re-connect to {} ({})", device.getAddress(), device.getName());
                    GBApplication.deviceService(device).connect();
                }
            }
        }
    }

    private void scheduleReconnect() {
        scheduleReconnect(mDelay);

        // Exponential backoff with a limit of 64 seconds.
        mBackingOff = true;
        mDelay = Math.min(mDelay * 2, 64000);
    }

    private void scheduleReconnect(int delay) {
        LOG.info("scheduling reconnect in {}ms", delay);
        AlarmManager am = (AlarmManager) (GBApplication.getContext().getSystemService(Context.ALARM_SERVICE));
        Intent intent = new Intent("GB_RECONNECT");
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(GBApplication.getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().
                getTimeInMillis() + delay,
                pendingIntent
        );
        mScheduled = true;
    }

    public void destroy() {
        LocalBroadcastManager.getInstance(service).unregisterReceiver(this);
    }

}
