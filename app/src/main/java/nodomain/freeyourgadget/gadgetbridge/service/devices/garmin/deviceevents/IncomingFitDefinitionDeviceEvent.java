package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;


public class IncomingFitDefinitionDeviceEvent extends GBDeviceEvent {
    public List<RecordDefinition> getRecordDefinitions() {
        return recordDefinitions;
    }

    private final List<RecordDefinition> recordDefinitions;

    public IncomingFitDefinitionDeviceEvent(List<RecordDefinition> recordDefinitions) {
        this.recordDefinitions = recordDefinitions;
    }

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        // Handled in support class
    }
}
