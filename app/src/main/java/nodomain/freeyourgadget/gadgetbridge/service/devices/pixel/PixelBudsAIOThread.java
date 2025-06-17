package nodomain.freeyourgadget.gadgetbridge.service.devices.pixel;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public class PixelBudsAIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(PixelBudsAIOThread.class);
    private static final UUID UUID_DEVICE_CTRL = UUID.fromString("df21fe2c-2515-4fdb-8886-f12c4d67927c");

    public PixelBudsAIOThread(final GBDevice gbDevice,
                              final Context context,
                              final PixelBudsAProtocol deviceProtocol,
                              final AbstractSerialDeviceSupport deviceSupport,
                              final BluetoothAdapter btAdapter) {
        super(gbDevice, context, deviceProtocol, deviceSupport, btAdapter);
    }

    @Override
    protected void initialize() {
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    @Override
    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return UUID_DEVICE_CTRL;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read {} bytes. {}", bytes, hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }
}
