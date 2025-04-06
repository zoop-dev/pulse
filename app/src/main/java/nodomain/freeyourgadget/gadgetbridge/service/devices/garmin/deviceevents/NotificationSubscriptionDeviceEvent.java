package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class NotificationSubscriptionDeviceEvent extends GBDeviceEvent {
    public boolean enable;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        // Handled in support class
    }
}
