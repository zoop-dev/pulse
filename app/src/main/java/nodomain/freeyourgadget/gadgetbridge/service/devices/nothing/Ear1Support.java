/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniele Gobbetti, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.AbstractEarCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;

import static nodomain.freeyourgadget.gadgetbridge.util.CheckSums.getCRC16ansi;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class Ear1Support extends AbstractHeadphoneBTBRDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Ear1Support.class);
    private static final int MAX_MTU = 512;
    private static final UUID UUID_DEVICE_CTRL = UUID.fromString("aeac4a03-dff5-498f-843a-34487cf133eb");

    private NothingProtocol nothingProtocol;

    public Ear1Support() {
        super(LOG, MAX_MTU);
        addSupportedService(UUID_DEVICE_CTRL);
    }

    @Override
    public void onTestNewFunction() {
        //getDeviceIOThread().write(((NothingProtocol) getDeviceProtocol()).encodeBatteryStatusReq());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        nothingProtocol = new NothingProtocol(getCoordinator().incrementCounter());

        sendCommand(builder, nothingProtocol.encodeBatteryStatusReq());
        sendCommand(builder, nothingProtocol.encodeAudioModeStatusReq());

        return builder;
    }

    private AbstractEarCoordinator getCoordinator() {
        return (AbstractEarCoordinator) getDevice().getDeviceCoordinator();
    }

    private void sendCommand(final TransactionBuilder builder, final byte[] payload) {
        builder.write(payload);
    }

    private void sendCommand(final String taskName, final byte[] payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendCommand(builder, payload);
        builder.queue();
    }


    @Override
    public void onSocketRead(byte[] data) {
        for (GBDeviceEvent deviceEvent : nothingProtocol.decodeResponse(data)) {
            if (deviceEvent == null) {
                continue;
            }
            evaluateGBDeviceEvent(deviceEvent);
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        sendCommand("find device", nothingProtocol.encodeFindDevice(start));
    }

    @Override
    public void onSetTime() {
        sendCommand("set time", nothingProtocol.encodeSetTime());
    }

    @Override
    public void onSendConfiguration(final String config) {

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_INEAR:
                byte enabled = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_INEAR, true) ? 0x01 : 0x00);
                sendCommand("set in ear detection", nothingProtocol.encodeInEarDetection(enabled));
                // response: 55 20 01 04 70 00 00 00
            case DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_AUDIOMODE:
                sendCommand("set audio mode", nothingProtocol.encodeAudioMode(prefs.getString(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_AUDIOMODE, "off")));
                // response: 55 20 01 0F 70 00 00 00
            default:
                LOG.debug("CONFIG: " + config);
        }

        super.onSendConfiguration(config);
    }

    static class NothingProtocol {

        private boolean isFirstExchange = true;

        private static final byte CONTROL_DEVICE_TYPE_TWS_HEADSET = 1;

        private static final int CONTROL_CRC = 0x20;

        private static final byte MASK_RSP_CODE = 0x1f;
        private static final short MASK_DEVICE_TYPE = 0x0F00;

        private static final short MASK_REQUEST_CMD = (short) 0x8000;

        private static final byte MASK_BATTERY = 0x7f;
        private static final byte MASK_BATTERY_CHARGING = (byte) 0x80;

        //incoming
        private static final short battery_status = (short) 0xe001;
        private static final short battery_status2 = (short) 0xc007;
        private static final short audio_mode_status = (short) 0xc01e;

        private static final short unk_maybe_ack = (short) 0xf002;
        private static final short unk_close_case = (short) 0xe002; //sent twice when the case is closed with earphones in

        //outgoing
        private static final short find_device = (short) 0xf002;
        private static final short in_ear_detection = (short) 0xf004;
        private static final short audio_mode = (short) 0xf00f;

        private final boolean incrementCounter;
        private int messageCounter = 0x00;

        private final HashMap<Byte, GBDeviceEventBatteryInfo> batteries;
        private static final byte battery_earphone_left = 0x02;
        private static final byte battery_earphone_right = 0x03;
        private static final byte battery_case = 0x04;

        private enum NothingAudioMode {
            anc(0x01),
            anclight(0x03),
            off(0x05),
            transparency(0x07),
            ;

            private final int bitmask;

            NothingAudioMode (int bitmask) {
                this.bitmask = bitmask;
            }

            public int getBitmask() { return bitmask; }

            public static NothingAudioMode fromBitmask(int bitmask) {
                for (NothingAudioMode flag : values()) {
                    if (flag.bitmask == bitmask) {
                        return flag;
                    }
                }
                throw new IllegalArgumentException("Unknown NothingAudioMode: 0x" +
                        Integer.toHexString(bitmask));
            }
        }

        public GBDeviceEvent[] decodeResponse(byte[] responseData) {
            List<GBDeviceEvent> devEvts = new ArrayList<>();

            if (isFirstExchange) {
                isFirstExchange = false;
                devEvts.add(new GBDeviceEventVersionInfo()); //TODO: this is a weird hack to make the DBHelper happy. Replace with proper firmware detection
                devEvts.add(new GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED));

            }

            ByteBuffer incoming = ByteBuffer.wrap(responseData);
            incoming.order(ByteOrder.LITTLE_ENDIAN);

            byte sof = incoming.get();
            if (sof != 0x55) {
                LOG.error("Error in message, wrong start of frame: " + hexdump(responseData));
                return null;
            }

            short control = incoming.getShort();
            if (!isSupportedDevice(control)) {
                LOG.error("Unsupported device specified in message: " + hexdump(responseData));
                return null;
            }
            if (!isOk(control)) {
                LOG.error("Message is not ok: " + hexdump(responseData));
                return null;
            }

            short command = incoming.getShort();
            short length = incoming.getShort();
            incoming.get();

            byte[] payload = Arrays.copyOfRange(responseData, incoming.position(), incoming.position() + length);


            switch (getRequestCommand(command)) {
                case battery_status:
                case battery_status2:
                    devEvts.addAll(handleBatteryInfo(payload));
                    break;
                case audio_mode_status:
                    devEvts.add(handleAudioModeStatus(payload));
                    break;

                case unk_maybe_ack:
                    LOG.debug("received ack");
                    break;
                case unk_close_case:
                    LOG.debug("case closed");
                    break;

                default:
                    LOG.debug("Incoming message - control:" + control + " requestCommand: " + (getRequestCommand(command) & 0xffff) + "length: " + length + " dump: " + hexdump(responseData));

            }
            return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
        }

        boolean isCrcNeeded(short control) {
            return (control & CONTROL_CRC) != 0;
        }

        byte[] encodeMessage(short control, short command, byte[] payload) {

            ByteBuffer msgBuf = ByteBuffer.allocate(8 + payload.length);
            msgBuf.order(ByteOrder.LITTLE_ENDIAN);
            msgBuf.put((byte) 0x55); //sof
            msgBuf.putShort((short) (incrementCounter ? (control | 0x40) : control));
            msgBuf.putShort(command);
            msgBuf.putShort((short) payload.length);
            msgBuf.put((byte) messageCounter); //fsn
            if (incrementCounter) {
                messageCounter++;
                if ((byte) messageCounter == (byte) 0xfd) {
                    messageCounter = 0x00;
                }
            }
            msgBuf.put(payload);

            if (isCrcNeeded(control)) {
                msgBuf.position(0);
                ByteBuffer crcBuf = ByteBuffer.allocate(msgBuf.capacity() + 2);
                crcBuf.order(ByteOrder.LITTLE_ENDIAN);
                crcBuf.put(msgBuf);
                crcBuf.putShort((short) getCRC16ansi(msgBuf.array()));
                return crcBuf.array();
            }

            return msgBuf.array();
        }

        byte[] encodeBatteryStatusReq() {
            return encodeMessage((short) 0x5120, battery_status2, new byte[]{});
        }

        byte[] encodeAudioModeStatusReq() {
            return encodeMessage((short) 0x120, audio_mode_status, new byte[]{});
        }

        private GBDeviceEventUpdatePreferences handleAudioModeStatus(byte[] payload) {
            final GBDeviceEventUpdatePreferences preferencesEvent = new GBDeviceEventUpdatePreferences();

            if (payload.length == 3 && payload[0] == 0x01 && payload[2] == 0x00) {
                try {
                    NothingAudioMode mode = NothingAudioMode.fromBitmask(payload[1]);
                    LOG.info("Audio mode: " + mode.name());
                    preferencesEvent.withPreference(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_AUDIOMODE, mode.name());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unknown audio mode. Payload: " + hexdump(payload));
                }
            } else {
                LOG.warn("Invalid audio mode payload format. Payload: " + hexdump(payload));
            }
            return preferencesEvent;
        }

        byte[] encodeInEarDetection(byte enabled) {
            return encodeMessage((short) 0x120, in_ear_detection, new byte[]{0x01, 0x01, enabled});
        }

        byte[] encodeAudioMode(String desired) {
            int modeBitmask = NothingAudioMode.valueOf("off").getBitmask();
            try {
                modeBitmask = NothingAudioMode.valueOf(desired).getBitmask();
            } catch (IllegalArgumentException e) {
                LOG.warn("Illegal audio mode requested: {} , using default", desired);
            }
            byte[] payload = new byte[]{0x01, (byte) modeBitmask, 0x00};

            return encodeMessage((short) 0x120, audio_mode, payload);
        }

        public byte[] encodeFindDevice(boolean start) {
            byte payload = (byte) (start ? 0x01 : 0x00);
            return encodeMessage((short) 0x120, find_device, new byte[]{payload});
        }

        public byte[] encodeSetTime() {
            // This are earphones, there is no time to set here. However this method gets called soon
            // after connecting, hence we use it to perform some initializations.
            // TODO: Find a way to send more requests during the first connection
            return encodeAudioModeStatusReq();
        }

        private List<GBDeviceEvent> handleBatteryInfo(byte[] payload) {
            List<GBDeviceEvent> batEvts = new ArrayList<>();

            //LOG.debug("Battery payload: " + hexdump(payload));

            /* payload:
            1st byte is number of batteries, then $number pairs follow:
            {idx, value}

            idx is 0x02 for left ear, 0x03 for right ear, 0x04 for case
            value goes from 0-64 (equivalent of 0-100 in hexadecimal)


            Since Gadgetbridge supports only one battery, we use an average of the levels for the
            battery level.
            If one of the batteries is recharging, we consider the battery as recharging.
             */

    //        GBDeviceEventBatteryInfo evBattery = new GBDeviceEventBatteryInfo();
    //        evBattery.level = 0;
    //        boolean batteryCharging = false;

            int numBatteries = payload[0];
            for (int i = 0; i < numBatteries; i++) {

                batteries.get(payload[1 + 2 * i]).level = (payload[2 + 2 * i] & MASK_BATTERY);
                batteries.get(payload[1 + 2 * i]).state =
                        ((payload[2 + 2 * i] & MASK_BATTERY_CHARGING) == MASK_BATTERY_CHARGING) ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;

                batEvts.add(batteries.get(payload[1 + 2 * i]));

    //            evBattery.level += (short) ((payload[2 + 2 * i] & MASK_BATTERY) / numBatteries);
    //            if (!batteryCharging) {
    //                batteryCharging = ((payload[2 + 2 * i] & MASK_BATTERY_CHARGING) == MASK_BATTERY_CHARGING);
    //            }
    //            LOG.debug("single battery level: " + hexdump(payload, 2+2*i,1) +"-"+ ((payload[2+2*i] & 0xff))+":" + evBattery.level);
                }

    //        evBattery.state = BatteryState.UNKNOWN;
    //        evBattery.state = batteryCharging ? BatteryState.BATTERY_CHARGING : evBattery.state;

    //        return evBattery;
            return batEvts;
        }

        private short getRequestCommand(short command) {
            return (short) (command | MASK_REQUEST_CMD);
        }

        private boolean isOk(short control) {
            return (control & MASK_RSP_CODE) == 0;
        }

        private boolean isSupportedDevice(short control) {
            return getDeviceType(control) == CONTROL_DEVICE_TYPE_TWS_HEADSET;
        }

        private byte getDeviceType(short control) {
            return (byte) ((control & MASK_DEVICE_TYPE) >> 8);
        }

        protected NothingProtocol(boolean incrementCounter) {
            batteries = new HashMap<>(3);

            batteries.put(battery_earphone_left, new GBDeviceEventBatteryInfo());
            batteries.put(battery_earphone_right, new GBDeviceEventBatteryInfo());
            batteries.put(battery_case, new GBDeviceEventBatteryInfo());

            batteries.get(battery_case).batteryIndex=0;
            batteries.get(battery_earphone_left).batteryIndex=1;
            batteries.get(battery_earphone_right).batteryIndex=2;

            this.incrementCounter = incrementCounter;
        }
    }
}
