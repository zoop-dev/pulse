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

import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsFindDeviceService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFindDeviceService.class);

    private static final short ENDPOINT = 0x001a;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte FIND_BAND_START = 0x03;
    public static final byte FIND_BAND_ACK = 0x04;
    public static final byte FIND_BAND_STOP_FROM_PHONE = 0x06;
    public static final byte FIND_BAND_STOP_FROM_BAND = 0x07;
    public static final byte FIND_PHONE_START = 0x11;
    public static final byte FIND_PHONE_ACK = 0x12;
    public static final byte FIND_PHONE_STOP_FROM_BAND = 0x13;
    public static final byte FIND_PHONE_STOP_FROM_PHONE = 0x14;
    public static final byte FIND_PHONE_MODE = 0x15;

    public static final String PREF_VERSION = "zepp_os_find_device_version";
    private int mVersion = 0;

    private final Handler findWatchHandler = new Handler();
    private boolean findingWatch = false;

    private final Handler findPhoneHandler = new Handler();
    private boolean findPhoneStarted;

    public ZeppOsFindDeviceService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                // mb7: 02:01:01
                // active2 / gtr4: 02:01:02
                if (payload.length == 3) {
                    mVersion = payload[2] & 0xff;
                    LOG.debug("Got find device service version={}", mVersion);
                } else {
                    LOG.warn("Got unexpected find device capabilities length {}", payload.length);
                }
                break;
            case FIND_BAND_ACK:
                LOG.info("Band acknowledged find band command");

                if (findingWatch && mVersion < 2) {
                    // continuous find device not supported - schedule periodic
                    findWatchHandler.postDelayed(() -> {
                        LOG.debug("Triggering find device vibration");
                        sendFindDeviceCommand(true);
                    }, HuamiUtils.getFindDeviceInterval(getSupport().getDevice(), true));
                }

                return;
            case FIND_PHONE_START:
                LOG.info("Find Phone Start");
                acknowledgeFindPhone(); // FIXME: Premature, but the band will only send the mode after we ack

                // Delay the find phone start, because we might get the FIND_PHONE_MODE
                findPhoneHandler.postDelayed(() -> {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                    evaluateGBDeviceEvent(findPhoneEvent);
                }, 1500);

                break;
            case FIND_BAND_STOP_FROM_BAND:
                LOG.info("Find Band Stop from Band");
                break;
            case FIND_PHONE_STOP_FROM_BAND:
                LOG.info("Find Phone Stop");
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case FIND_PHONE_MODE:
                findPhoneHandler.removeCallbacksAndMessages(null);

                final int mode = payload[1] & 0xff; // 0 to only vibrate, 1 to ring
                LOG.info("Find Phone Mode: {}", mode);
                if (findPhoneStarted) {
                    // Already started, just change the mode
                    findPhoneEvent.event = mode == 1 ? GBDeviceEventFindPhone.Event.RING : GBDeviceEventFindPhone.Event.VIBRATE;
                } else {
                    findPhoneEvent.event = mode == 1 ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.START_VIBRATE;
                }
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            default:
                LOG.warn("Unexpected find phone byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        findWatchHandler.removeCallbacksAndMessages(null);
        findPhoneHandler.removeCallbacksAndMessages(null);
        findingWatch = false;
        findPhoneStarted = false;

        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    @Override
    public void dispose() {
        findWatchHandler.removeCallbacksAndMessages(null);
        findPhoneHandler.removeCallbacksAndMessages(null);
    }

    public void onFindDevice(final boolean start) {
        LOG.debug("onFindDevice {}", start);
        findWatchHandler.removeCallbacksAndMessages(null);
        findingWatch = start;
        sendFindDeviceCommand(start);
    }

    public void onFindPhone(final boolean start) {
        LOG.info("Find phone: {}", start);

        findPhoneStarted = start;

        if (!start) {
            stopFindPhone();
        }
    }

    // FIXME should be private?
    public void sendFindDeviceCommand(final boolean start) {
        final byte findBandCommand = start ? FIND_BAND_START : FIND_BAND_STOP_FROM_PHONE;

        LOG.info("Sending find band {}", start);

        write("find huami 2021", findBandCommand);
    }

    private void acknowledgeFindPhone() {
        LOG.info("Acknowledging find phone");

        write("ack find phone", new byte[]{FIND_PHONE_ACK, 0x01 /* success */});
    }

    private void stopFindPhone() {
        LOG.info("Stopping find phone");

        write("found phone", FIND_PHONE_STOP_FROM_PHONE);
    }

    public static boolean supportsContinuousFindDevice(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_VERSION, 0) >= 2;
    }
}
