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

import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class OppoHeadphonesSupport extends AbstractHeadphoneSerialDeviceSupportV2<OppoHeadphonesProtocol> {
    private static final Logger LOG = LoggerFactory.getLogger(OppoHeadphonesSupport.class);

    private final Handler handler = new Handler();

    // Some devices will not reply to the first battery request, so we need to retry a few times
    private int batteryRetries = 0;
    private final Runnable batteryReqRunnable = () -> {
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
                final TransactionBuilder builder = createTransactionBuilder("battery retry");
                builder.write(mDeviceProtocol.encodeBatteryReq());
                builder.queue();
                scheduleBatteryRequestRetry();
            } else {
                LOG.error("Failed to get battery after {} tries", batteryRetries);
                // Since this is not fatal, we stay connected
            }
        }
    };

    public OppoHeadphonesSupport() {
        addSupportedService(UUID.fromString("0000079a-d102-11e1-9b23-00025b00a5a5"));
    }

    @Override
    protected OppoHeadphonesProtocol createDeviceProtocol() {
        return new OppoHeadphonesProtocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.write(mDeviceProtocol.encodeFirmwareVersionReq());
        builder.write(mDeviceProtocol.encodeConfigurationReq());
        builder.write(mDeviceProtocol.encodeBatteryReq());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        scheduleBatteryRequestRetry();
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            handler.removeCallbacksAndMessages(null);
            super.dispose();
        }
    }

    private void scheduleBatteryRequestRetry() {
        LOG.info("Scheduling battery request retry");

        handler.postDelayed(batteryReqRunnable, 2000);
    }
}
