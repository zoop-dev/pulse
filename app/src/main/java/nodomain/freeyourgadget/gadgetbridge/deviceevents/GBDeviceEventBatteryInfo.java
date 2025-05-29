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


import android.content.Context;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
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
        return super.toString() + "index: " + batteryIndex + ", level: " + level;
    }

    protected void setDeviceValues(final GBDevice device) {
        device.setBatteryLevel(this.level, this.batteryIndex);
        device.setBatteryState(this.state, this.batteryIndex);
        device.setBatteryVoltage(this.voltage, this.batteryIndex);
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        if ((level < 0 || level > 100) && level != GBDevice.BATTERY_UNKNOWN) {
            LOG.error("Battery level must be within range 0-100: {}", level);
            return;
        }

        // for devices that do not report charging, but the level just increased
        final boolean levelJustIncreased = device.getBatteryLevel(this.batteryIndex) != GBDevice.BATTERY_UNKNOWN &&
                this.level != GBDevice.BATTERY_UNKNOWN &&
                this.level > device.getBatteryLevel(this.batteryIndex);

        setDeviceValues(device);

        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(device);
        final BatteryConfig batteryConfig = device.getDeviceCoordinator().getBatteryConfig(device)[this.batteryIndex];

        if (this.level == GBDevice.BATTERY_UNKNOWN) {
            // no level available, just "high" or "low"
            GB.removeBatteryFullNotification(context);
            if (devicePrefs.getBatteryNotifyLowEnabled(batteryConfig) && BatteryState.BATTERY_LOW.equals(this.state)) {
                GB.updateBatteryLowNotification(context.getString(R.string.notif_battery_low, device.getAliasOrName()),
                        this.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_extended, device.getAliasOrName(),
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(this.lastChargeTime.getTime())) +
                                                context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(this.numCharges)))
                                : ""
                        , context);
            } else if (devicePrefs.getBatteryNotifyFullEnabled(batteryConfig) && BatteryState.BATTERY_CHARGING_FULL.equals(this.state)) {
                GB.removeBatteryLowNotification(context);
                GB.updateBatteryFullNotification(context.getString(R.string.notif_battery_full, device.getAliasOrName()), "", context);
            } else {
                GB.removeBatteryLowNotification(context);
                GB.removeBatteryFullNotification(context);
            }
        } else {
            new StoreDataTask("Storing battery data", context, device, this).execute();

            final boolean batteryNotifyLowEnabled = devicePrefs.getBatteryNotifyLowEnabled(batteryConfig);
            final boolean isBatteryLow = this.level <= devicePrefs.getBatteryNotifyLowThreshold(batteryConfig) &&
                    (BatteryState.BATTERY_LOW.equals(this.state) || BatteryState.BATTERY_NORMAL.equals(this.state));

            final boolean batteryNotifyFullEnabled = devicePrefs.getBatteryNotifyFullEnabled(batteryConfig);
            final boolean isBatteryFull = this.level >= devicePrefs.getBatteryNotifyFullThreshold(batteryConfig) &&
                    (BatteryState.BATTERY_CHARGING.equals(this.state) ||
                            BatteryState.BATTERY_CHARGING_FULL.equals(this.state) ||
                            levelJustIncreased
                    );

            //show the notification if the battery level is below threshold and only if not connected to charger
            if (batteryNotifyLowEnabled && isBatteryLow) {
                GB.removeBatteryFullNotification(context);
                GB.updateBatteryLowNotification(context.getString(R.string.notif_battery_low_percent, device.getAliasOrName(), String.valueOf(this.level)),
                        this.extendedInfoAvailable() ?
                                context.getString(R.string.notif_battery_low_percent, device.getAliasOrName(), String.valueOf(this.level)) + "\n" +
                                        context.getString(R.string.notif_battery_low_bigtext_last_charge_time, DateFormat.getDateTimeInstance().format(this.lastChargeTime.getTime())) +
                                        context.getString(R.string.notif_battery_low_bigtext_number_of_charges, String.valueOf(this.numCharges))
                                : ""
                        , context);
            } else if (batteryNotifyFullEnabled && isBatteryFull) {
                GB.removeBatteryLowNotification(context);
                GB.updateBatteryFullNotification(context.getString(R.string.notif_battery_full, device.getAliasOrName()), "", context);
            } else {
                GB.removeBatteryLowNotification(context);
                GB.removeBatteryFullNotification(context);
            }
        }

        device.sendDeviceUpdateIntent(context);
    }

    public static class StoreDataTask extends DBAccess {
        GBDeviceEventBatteryInfo deviceEvent;
        GBDevice gbDevice;

        public StoreDataTask(String task, Context context, GBDevice device, GBDeviceEventBatteryInfo deviceEvent) {
            super(task, context);
            this.deviceEvent = deviceEvent;
            this.gbDevice = device;
        }

        @Override
        protected void doInBackground(DBHandler handler) {
            DaoSession daoSession = handler.getDaoSession();
            Device device = DBHelper.getDevice(gbDevice, daoSession);
            int ts = (int) (System.currentTimeMillis() / 1000);
            BatteryLevel batteryLevel = new BatteryLevel();
            batteryLevel.setTimestamp(ts);
            batteryLevel.setBatteryIndex(deviceEvent.batteryIndex);
            batteryLevel.setDevice(device);
            batteryLevel.setLevel(deviceEvent.level);
            handler.getDaoSession().getBatteryLevelDao().insert(batteryLevel);
        }
    }
}
