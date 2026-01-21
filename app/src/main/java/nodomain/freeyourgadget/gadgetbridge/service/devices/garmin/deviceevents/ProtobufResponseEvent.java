package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;

public class ProtobufResponseEvent extends GBDeviceEvent {
    public GdiSmartProto.Smart payload;
    public int messageId;

    public ProtobufResponseEvent(final GdiSmartProto.Smart payload, final int messageId) {
        this.payload = payload;
        this.messageId = messageId;
    }

    @Override
    public void evaluate(@NonNull final Context context, @NonNull final GBDevice device) {
        // Handled in support class
    }
}
