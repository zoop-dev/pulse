package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

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

public class EarFunIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(EarFunIOThread.class);
    private final EarFunProtocol earFunProtocol;
    public static final UUID GAIA_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID GAIA_UUID = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");

    @Override
    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return GAIA_SPP_UUID;
    }

    public EarFunIOThread(GBDevice device, Context context, EarFunProtocol deviceProtocol,
                          EarFunDeviceSupport earFunDeviceSupport, BluetoothAdapter bluetoothAdapter) {
        super(device, context, deviceProtocol, earFunDeviceSupport, bluetoothAdapter);
        earFunProtocol = deviceProtocol;
    }

    @Override
    protected void initialize() {
        super.initialize();
        write(earFunProtocol.encodeFirmwareVersionReq());
        write(earFunProtocol.encodeSettingsReq());
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }
}
