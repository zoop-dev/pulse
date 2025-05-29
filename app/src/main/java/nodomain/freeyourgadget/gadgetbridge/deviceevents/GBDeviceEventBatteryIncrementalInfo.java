package nodomain.freeyourgadget.gadgetbridge.deviceevents;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

/** This event is basically the same as GBDeviceEventBatteryInfo, but it will only update the fields
 * that are explicitly set. This can be used when the caller that is sending the event doesn't have
 * all of the battery state available to make the call. For example the device has a sent an event
 * that indicates the device has started charging, but does not send the current level. If that
 * caller tries to fetch the current state and resend it, there is a race condition that may cause a
 * concurrent update to be dropped.
 */
 public class GBDeviceEventBatteryIncrementalInfo extends GBDeviceEventBatteryInfo {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventBatteryIncrementalInfo.class);

    private enum UpdateType {
        LAST_CHARGE_TIME,
        STATE,
        LEVEL,
        VOLTAGE;
    }

    private final UpdateType updateType;

    public GBDeviceEventBatteryIncrementalInfo(int batteryIndex, GregorianCalendar lastChargeTime) {
        super();
        super.batteryIndex = batteryIndex;
        super.lastChargeTime = lastChargeTime;
        super.level = GBDevice.BATTERY_UNKNOWN;
        this.updateType = UpdateType.LAST_CHARGE_TIME;
    }

    public GBDeviceEventBatteryIncrementalInfo(int batteryIndex, BatteryState state) {
        super();
        super.batteryIndex = batteryIndex;
        super.state = state;
        super.level = GBDevice.BATTERY_UNKNOWN;
        this.updateType = UpdateType.STATE;

    }

    public GBDeviceEventBatteryIncrementalInfo(int batteryIndex, int level) {
        super();
        super.batteryIndex = batteryIndex;
        super.level = level;
        this.updateType = UpdateType.LEVEL;
    }

    public GBDeviceEventBatteryIncrementalInfo(int batteryIndex, float voltage) {
        super();
        super.batteryIndex = batteryIndex;
        super.level = GBDevice.BATTERY_UNKNOWN;
        super.voltage = voltage;
        this.updateType = UpdateType.VOLTAGE;
    }

    @Override
    protected void setDeviceValues(final GBDevice device) {
        switch (updateType) {
            case STATE -> device.setBatteryState(super.state, this.batteryIndex);
            case LEVEL -> device.setBatteryLevel(super.level, this.batteryIndex);
            case VOLTAGE -> device.setBatteryVoltage(super.voltage, this.batteryIndex);
        }
    }
}
