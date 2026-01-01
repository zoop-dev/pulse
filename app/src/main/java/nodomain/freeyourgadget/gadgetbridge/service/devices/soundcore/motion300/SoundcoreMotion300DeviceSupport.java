/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.motion300;

import android.os.Handler;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreMotion300DeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreMotion300Protocol> {
    private final Handler handler = new Handler();

    public SoundcoreMotion300DeviceSupport() {
        addSupportedService(UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3135"));
    }

    @Override
    protected SoundcoreMotion300Protocol createDeviceProtocol() {
        return new SoundcoreMotion300Protocol(getDevice());
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        // Device requires a little delay to respond to commands
        handler.postDelayed(() -> {
            final TransactionBuilder builderDelayed = createTransactionBuilder("initialize delayed");
            builderDelayed.write(mDeviceProtocol.encodeGetDeviceInfo());
            builderDelayed.queue();
        }, 500);

        builder.setDeviceState(GBDevice.State.INITIALIZING);
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            handler.removeCallbacksAndMessages(null);
            super.dispose();
        }
    }
}
