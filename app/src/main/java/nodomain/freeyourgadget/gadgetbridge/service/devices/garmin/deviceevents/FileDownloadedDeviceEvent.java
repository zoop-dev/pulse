package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileTransferHandler;

public class FileDownloadedDeviceEvent extends GBDeviceEvent {
    public boolean success = true;
    public FileTransferHandler.DirectoryEntry directoryEntry;
    public String localPath;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        // Handled in support class
    }
}
