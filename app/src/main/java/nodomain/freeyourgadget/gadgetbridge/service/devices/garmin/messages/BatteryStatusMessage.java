/*  Copyright (C) 2026 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatteryStatusMessage extends GFDIMessage {
    protected static final Logger LOG = LoggerFactory.getLogger(BatteryStatusMessage.class);

    public BatteryStatusMessage(GarminMessage garminMessage) {
        this.garminMessage = garminMessage;
        this.statusMessage = this.getStatusMessage();
    }

    public static BatteryStatusMessage parseIncoming(MessageReader reader, GarminMessage originalGarminMessage) {
        final int wireStatus = reader.readByte(); // 0
        final float voltage = reader.readByte() / 100.0f; // 1
        // 2-6 - unknown meaning

        final String status = switch (wireStatus & 0x70) {
            case 0x20 -> "good";
            case 0x30 -> "ok";
            case 0x40 -> "low";
            default -> Integer.toBinaryString(wireStatus & 0x70);
        };

        LOG.info("Received not-yet supported BATTERY_STATUS message with status={} and voltage={}",
                status, voltage);

        return new BatteryStatusMessage(originalGarminMessage);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
