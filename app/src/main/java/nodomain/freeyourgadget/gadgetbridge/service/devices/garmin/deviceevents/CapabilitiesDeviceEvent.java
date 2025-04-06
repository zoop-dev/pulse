package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CapabilitiesDeviceEvent extends GBDeviceEvent {
    public Set<GarminCapability> capabilities;

    public CapabilitiesDeviceEvent(final Set<GarminCapability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        // Handled in support class
    }
}
