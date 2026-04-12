/*  Copyright (C) 2015-2024 Andreas Shimokawa, Daniele Gobbetti, José Rebelo,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.BatteryInfoActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevel;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GBDeviceEventBatteryInfo extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventBatteryInfo.class);

    ///  Device Address -> Battery notification ID
    private static final Map<String, Integer> NOTIFICATION_IDS = new HashMap<>(2);

    public GregorianCalendar lastChargeTime = null;
    public BatteryState state = BatteryState.UNKNOWN;
    public int batteryIndex = 0;
    public int level = 50;
    public int numCharges = -1;
    public float voltage = -1f;

    public boolean extendedInfoAvailable() {
        return numCharges != -1 && lastChargeTime != null;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() +
                "index: " + batteryIndex +
                ", state: " + state +
                ", level: " + level;
    }

    protected void setDeviceValues(final GBDevice device) {
        device.setBatteryLevel(this.level, this.batteryIndex);
        device.setBatteryState(this.state, this.batteryIndex);
        device.setBatteryVoltage(this.voltage, this.batteryIndex);
    }

    @Override
    public void evaluate(@NonNull final Context context, @NonNull final GBDevice device) {
        if ((level < 0 || level > 100) && level != GBDevice.BATTERY_UNKNOWN) {
            LOG.error("Battery level must be within range 0-100: {}", level);
            return;
        }

        final int previousLevel = device.getBatteryLevel(this.batteryIndex);
        final int levelDifference = previousLevel != GBDevice.BATTERY_UNKNOWN && level != GBDevice.BATTERY_UNKNOWN ?
                level - previousLevel : 0;
        // Treat first connect (unknown -> known) as both a potential drop and increase,
        // so notifications fire immediately if the battery is already low or full on connect.
        final boolean isLevelDrop = previousLevel == GBDevice.BATTERY_UNKNOWN || levelDifference < 0;
        final boolean isLevelIncrease = previousLevel == GBDevice.BATTERY_UNKNOWN || levelDifference > 0;

        setDeviceValues(device);

        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);

        final int notificationSignal = shouldHaveNotification(devicePrefs, device, batteryIndex);

        if (this.level == GBDevice.BATTERY_UNKNOWN) {
            // no level available, just "high" or "low"
            if (notificationSignal < 0) {
                updateBatteryLowNotification(
                        context.getString(R.string.notif_battery_low, device.getAliasOrName()),
                        this.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_extended, device.getAliasOrName(),
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(this.lastChargeTime.getTime())) +
                                                context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(this.numCharges)))
                                : ""
                        , context, device, batteryIndex);
            } else if (notificationSignal > 0) {
                updateBatteryFullNotification(context, device, batteryIndex);
            } else {
                removeIfNoBatteryNotifying(context, device);
            }
        } else {
            //noinspection unchecked
            new StoreDataTask(context, device, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            if (notificationSignal < 0 && isLevelDrop) {
                updateBatteryLowNotification(context.getString(R.string.notif_battery_low_percent, device.getAliasOrName(), String.valueOf(this.level)),
                        this.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_percent, device.getAliasOrName(), String.valueOf(this.level)) + "\n" +
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(this.lastChargeTime.getTime())) +
                                        context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(this.numCharges))
                                : ""
                        , context, device, batteryIndex);
            } else if (notificationSignal > 0 && isLevelIncrease) {
                updateBatteryFullNotification(context, device, batteryIndex);
            } else {
                removeIfNoBatteryNotifying(context, device);
            }
        }

        device.sendDeviceUpdateIntent(context);
    }

    private static void removeIfNoBatteryNotifying(final Context context,
                                                   final GBDevice device) {
        boolean activeNotification = false;

        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
        final int batteryCount = device.getDeviceCoordinator().getBatteryCount(device);

        for (int batteryIndex = 0; batteryIndex < batteryCount; batteryIndex++) {
            if (shouldHaveNotification(devicePrefs, device, batteryIndex) > 0) {
                activeNotification = true;
                break;
            }
        }

        if (!activeNotification) {
            GB.removeNotification(getNotificationId(device), context);
        }
    }

    /**
     * Returns 1 if the battery index should have a full battery notification, -1 if it should have a low battery
     * notification, 0 if there should be no notification.
     */
    private static int shouldHaveNotification(final DevicePrefs devicePrefs,
                                              final GBDevice device,
                                              final int batteryIndex) {
        final BatteryConfig batteryConfig = device.getDeviceCoordinator().getBatteryConfig(device)[batteryIndex];
        final int level = device.getBatteryLevel(batteryIndex);
        final BatteryState state = device.getBatteryState(batteryIndex);
        if (level == GBDevice.BATTERY_UNKNOWN) {
            if (devicePrefs.getBatteryNotifyLowEnabled(batteryConfig) && BatteryState.BATTERY_LOW.equals(state)) {
                return -1;
            } else if (devicePrefs.getBatteryNotifyFullEnabled(batteryConfig) && BatteryState.BATTERY_CHARGING_FULL.equals(state)) {
                return 1;
            }
        } else {
            final boolean batteryNotifyLowEnabled = devicePrefs.getBatteryNotifyLowEnabled(batteryConfig);
            final boolean isBatteryLow = level <= devicePrefs.getBatteryNotifyLowThreshold(batteryConfig) &&
                    (BatteryState.BATTERY_LOW.equals(state) || BatteryState.BATTERY_NORMAL.equals(state) || BatteryState.UNKNOWN.equals(state));

            final boolean batteryNotifyFullEnabled = devicePrefs.getBatteryNotifyFullEnabled(batteryConfig);
            final boolean isBatteryFull = level >= devicePrefs.getBatteryNotifyFullThreshold(batteryConfig) &&
                    (BatteryState.BATTERY_CHARGING.equals(state) || BatteryState.BATTERY_CHARGING_FULL.equals(state));
            if (batteryNotifyLowEnabled && isBatteryLow) {
                return -1;
            } else if (batteryNotifyFullEnabled && isBatteryFull) {
                return 1;
            }
        }

        return 0;
    }

    private static void updateBatteryLowNotification(final CharSequence text,
                                                     final CharSequence bigText,
                                                     final Context context,
                                                     final GBDevice device,
                                                     final int batteryIndex) {
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }

        final int notificationId = getNotificationId(device);

        final Intent notificationIntent = new Intent(context, BatteryInfoActivity.class);
        notificationIntent.setPackage(BuildConfig.APPLICATION_ID);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        notificationIntent.putExtra(GBDevice.BATTERY_INDEX, batteryIndex);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(context, GB.NOTIFICATION_CHANNEL_ID_LOW_BATTERY)
                .setContentTitle(context.getString(R.string.notif_battery_low_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_low_battery)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        if (bigText != null) {
            nb.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        }

        GB.notify(notificationId, nb.build(), context);
    }

    private static void updateBatteryFullNotification(final Context context,
                                                      final GBDevice device,
                                                      final int batteryIndex) {
        if (GBEnvironment.env().isLocalTest()) {
            return;
        }

        final int notificationId = getNotificationId(device);

        final Intent notificationIntent = new Intent(context, BatteryInfoActivity.class);
        notificationIntent.setPackage(BuildConfig.APPLICATION_ID);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        notificationIntent.putExtra(GBDevice.BATTERY_INDEX, batteryIndex);

        final PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(context, GB.NOTIFICATION_CHANNEL_ID_FULL_BATTERY)
                .setContentTitle(context.getString(R.string.notif_battery_full_title))
                .setContentText(context.getString(R.string.notif_battery_full, device.getAliasOrName()))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_full_battery)
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(false);

        GB.notify(notificationId, nb.build(), context);
    }

    private static int getNotificationId(final GBDevice device) {
        return NOTIFICATION_IDS.computeIfAbsent(device.getAddress(), ignored -> new Random().nextInt(Integer.MAX_VALUE));
    }

    private static class StoreDataTask extends DBAccess {
        private final GBDeviceEventBatteryInfo deviceEvent;
        private final GBDevice gbDevice;

        public StoreDataTask(final Context context, final GBDevice device, final GBDeviceEventBatteryInfo deviceEvent) {
            super("Storing battery data", context, true);
            this.deviceEvent = deviceEvent;
            this.gbDevice = device;
        }

        @Override
        protected void doInBackground(final DBHandler handler) {
            final DaoSession daoSession = handler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, daoSession);
            final int ts = (int) (System.currentTimeMillis() / 1000);
            final BatteryLevel batteryLevel = new BatteryLevel();
            batteryLevel.setTimestamp(ts);
            batteryLevel.setBatteryIndex(deviceEvent.batteryIndex);
            batteryLevel.setDevice(device);
            batteryLevel.setLevel(deviceEvent.level);
            handler.getDaoSession().getBatteryLevelDao().insert(batteryLevel);
        }
    }
}
