/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;

public class OppoHeadphonesIoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(OppoHeadphonesIoThread.class);

    private final OppoHeadphonesProtocol mProtocol;

    private final Handler handler = new Handler();

    // Some devices will not reply to the first battery request, so we need to retry a few times
    private int batteryRetries = 0;
    private final Runnable batteryReqRunnable = new Runnable() {
        @Override
        public void run() {
            final int batteryCount = getDevice().getDeviceCoordinator().getBatteryCount(getDevice());
            boolean knownBattery = false;
            for (int i = 0; i < batteryCount; i++) {
                if (getDevice().getBatteryState(i) != BatteryState.UNKNOWN) {
                    knownBattery = true;
                    break;
                }
            }
            if (!knownBattery) {
                if (batteryRetries++ < 2) {
                    LOG.warn("Battery request retry {}", batteryRetries);

                    write(mProtocol.encodeBatteryReq());
                    scheduleBatteryRequestRetry();
                } else {
                    LOG.error("Failed to get battery after {} tries", batteryRetries);
                    // Since this is not fatal, we stay connected
                }
            }
        }
    };

    public OppoHeadphonesIoThread(final GBDevice gbDevice,
                                  final Context context,
                                  final OppoHeadphonesProtocol deviceProtocol,
                                  final AbstractSerialDeviceSupport deviceSupport,
                                  final BluetoothAdapter btAdapter) {
        super(gbDevice, context, deviceProtocol, deviceSupport, btAdapter);
        this.mProtocol = deviceProtocol;
    }

    @NonNull
    @Override
    protected UUID getUuidToConnect(@NonNull final ParcelUuid[] uuids) {
        return UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5");
    }

    @Override
    protected void initialize() {
        write(mProtocol.encodeFirmwareVersionReq());
        write(mProtocol.encodeConfigurationReq());
        write(mProtocol.encodeBatteryReq());
        scheduleBatteryRequestRetry();
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    @Override
    public void quit() {
        handler.removeCallbacksAndMessages(null);
        super.quit();
    }

    @Override
    protected byte[] parseIncoming(final InputStream inStream) throws IOException {
        final byte[] buffer = new byte[1048576]; //HUGE read
        final int bytes = inStream.read(buffer);
        // FIXME: We should buffer this and handle partial commands
        LOG.debug("Read {} bytes: {}", bytes, hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }

    private void scheduleBatteryRequestRetry() {
        LOG.info("Scheduling battery request retry");

        handler.postDelayed(batteryReqRunnable, 2000);
    }
}
