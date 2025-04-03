/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSilentMode;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.util.SilentMode;

public class ZeppOsSilentModeService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsSilentModeService.class);

    private static final short ENDPOINT = 0x003b;

    public static final byte SILENT_MODE_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte SILENT_MODE_CMD_CAPABILITIES_RESPONSE = 0x02;
    // Notify silent mode, from phone
    public static final byte SILENT_MODE_CMD_NOTIFY_BAND = 0x03;
    public static final byte SILENT_MODE_CMD_NOTIFY_BAND_ACK = 0x04;
    // Query silent mode on phone, from band
    public static final byte SILENT_MODE_CMD_QUERY = 0x05;
    public static final byte SILENT_MODE_CMD_REPLY = 0x06;
    // Set silent mode on phone, from band
    // After this, phone sends ACK + NOTIFY
    public static final byte SILENT_MODE_CMD_SET = 0x07;
    public static final byte SILENT_MODE_CMD_ACK = 0x08;

    public ZeppOsSilentModeService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case SILENT_MODE_CMD_NOTIFY_BAND_ACK:
                LOG.info("Band acknowledged current phone silent mode, status = {}", payload[1]);
                return;
            case SILENT_MODE_CMD_QUERY:
                LOG.info("Got silent mode query from band");
                sendPhoneSilentMode(SilentMode.isPhoneInSilenceMode(getSupport().getDevice().getAddress()));
                return;
            case SILENT_MODE_CMD_SET:
                LOG.info("Band setting silent mode = {}", payload[1]);
                final boolean silentModeEnabled = (payload[1] == 1);
                ackSilentModeSet();
                sendPhoneSilentMode(silentModeEnabled);
                evaluateGBDeviceEvent(new GBDeviceEventSilentMode(silentModeEnabled));
                return;
            default:
                LOG.warn("Unexpected silent mode payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void ackSilentModeSet() {
        write("ack silent mode set", new byte[]{SILENT_MODE_CMD_ACK, 0x01});
    }

    private void sendPhoneSilentMode(final boolean enabled) {
        write("send phone silent mode to band", new byte[]{SILENT_MODE_CMD_NOTIFY_BAND, bool(enabled)});
    }
}
