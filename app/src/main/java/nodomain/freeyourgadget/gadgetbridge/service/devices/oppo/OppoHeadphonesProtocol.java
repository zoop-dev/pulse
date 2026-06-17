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

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.oppo.OppoHeadphonesPreferences;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.OppoCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigSide;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigValue;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.MiscConfigType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.AncConfigValue;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class OppoHeadphonesProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(OppoHeadphonesProtocol.class);

    public static final byte CMD_PREAMBLE = (byte) 0xaa;

    private int seqNum = 0;

    protected OppoHeadphonesProtocol(final GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(final byte[] responseData) {
        final List<GBDeviceEvent> events = new ArrayList<>();
        final ByteBuffer buf = ByteBuffer.wrap(responseData);

        while (buf.position() < buf.limit()) {
            final byte preamble = buf.get();
            if (preamble != CMD_PREAMBLE) {
                LOG.warn("Unexpected preamble {}", preamble);
                continue;
            }

            final int totalLength = buf.get() & 0xff;
            if (buf.limit() - buf.position() < totalLength) {
                LOG.error("Got partial response with {} bytes, expected {}", buf.limit() - buf.position(), totalLength);
                break;
            }

            final byte[] singleResponse = new byte[totalLength + 2];
            buf.position(buf.position() - 2);
            buf.get(singleResponse);

            events.addAll(handleSingleResponse(singleResponse));
        }
        return events.toArray(new GBDeviceEvent[0]);
    }

    private static List<GBDeviceEvent> handleSingleResponse(final byte[] responseData) {
        final ByteBuffer responseBuf = ByteBuffer.wrap(responseData).order(ByteOrder.LITTLE_ENDIAN);
        final byte preamble = responseBuf.get();

        if (preamble != CMD_PREAMBLE) {
            LOG.error("Unexpected preamble {}", preamble);
            return Collections.emptyList();
        }

        final int totalLength = responseBuf.get() & 0xff;
        if (responseData.length != totalLength + 2) {
            LOG.error("Invalid number of bytes {}, expected {}", responseData.length, totalLength + 2);
            return Collections.emptyList();
        }

        final short zero = responseBuf.getShort();
        if (zero != 0 && zero != 4) {
            // 0 on oppo, 4 on realme?
            // #4672 - sometimes 1 on oppo?
            LOG.warn("Unexpected bytes: {}, expected 0 or 4", zero);
        }

        final short code = responseBuf.getShort();
        final OppoCommand command = OppoCommand.fromCode(code);
        if (command == null) {
            LOG.warn("Unknown command code {}", String.format(Locale.ROOT, "0x%04x", code));
            return Collections.emptyList();
        }

        final int seq = responseBuf.get();
        final int payloadLength = responseBuf.getShort() & 0xffff;

        if (payloadLength > responseBuf.remaining()) {
            // FIXME: #4672 - avoid crash on some incoming commands - what are we parsing wrong?
            LOG.error("Payload length {} is larger than remaining {}", payloadLength, responseBuf.remaining());
            return Collections.emptyList();
        } else if (payloadLength < responseBuf.remaining()) {
            // leftover bytes?
            LOG.warn("Payload length {} is smaller than remaining {}", payloadLength, responseBuf.remaining());
        }

        final byte[] payload = new byte[payloadLength];
        responseBuf.get(payload);

        final List<GBDeviceEvent> events = new ArrayList<>();

        switch (command) {
            case BATTERY_RET: {
                if (payload[0] != 0) {
                    LOG.error("Unknown battery ret {}", payload[0]);
                    break;
                }
                events.addAll(parseBattery(payload));
                break;
            }
            case DEVICE_INFO: {
                switch (payload[0]) {
                    case 1: // battery
                        events.addAll(parseBattery(payload));
                        break;
                    case 2: // status
                        LOG.debug("Got status");
                        // TODO handle
                        break;
                    default:
                        LOG.warn("Unknown device info {}", payload[0]);
                }

                break;
            }
            case FIRMWARE_RET: {
                if (payload[0] != 0) {
                    LOG.warn("Unexpected firmware ret {}", payload[0]);
                    break;
                }
                events.addAll(parseFirmware(payload));
                break;
            }
            case FIND_DEVICE_ACK: {
                LOG.debug("Got find device ack, status={}", payload[0]);
                break;
            }
            case MISC_CONFIG_RET: {
                if (payload[0] != 0) {
                    LOG.warn("Unknown misc config ret {}", payload[0]);
                    break;
                }
                if (payload.length < 3) {
                    LOG.warn("Unexpected misc config ret payload size {}", payload.length);
                    break;
                }

                events.add(parseMiscConfig(payload));
                break;
            }
            case MISC_CONFIG_ACK: {
                LOG.debug("Got misc config ack, status={}", payload[0]);
                break;
            }
            case TOUCH_CONFIG_RET: {
                if (payload[0] != 0) {
                    LOG.warn("Unknown touch config ret {}", payload[0]);
                    break;
                }
                if ((payload.length - 2) % 4 != 0) {
                    LOG.warn("Unexpected touch config ret payload size {}", payload.length);
                    break;
                }

                events.add(parseTouchConfig(payload));
                break;
            }
            case TOUCH_CONFIG_ACK: {
                LOG.debug("Got touch config ack, status={}", payload[0]);
                break;
            }
            case ANC_CONFIG_RET: {
                if (payload[0] != 0) {
                    LOG.warn("Unknown anc config ret {}", payload[0]);
                    break;
                }
                if (payload.length < 3) {
                    LOG.warn("Unexpected anc config ret payload size {}", payload.length);
                    break;
                }

                events.add(parseAncConfig(payload));
                break;
            }
            case ANC_CONFIG_ACK: {
                LOG.debug("Got anc config ack, status={}", payload[0]);
                break;
            }
            default: {
                LOG.warn("Unhandled command {}", command);
            }
        }

        return events;
    }

    private static List<GBDeviceEvent> parseBattery(final byte[] payload) {
        final List<GBDeviceEvent> events = new ArrayList<>();

        final int numBatteries = payload[1] & 0xff;
        for (int i = 2; i < payload.length; i += 2) {
            if ((payload[i] & 0xff) == 0xff) {
                continue;
            }
            final int batteryIndex = payload[i] - 1;
            if (batteryIndex < 0 || batteryIndex > 2) {
                LOG.error("Unknown battery index {}", payload[i]);
                break;
            }

            final int batteryLevel = payload[i + 1] & 0x7f;
            if (batteryIndex == 2 && batteryLevel == 0) {
                continue;
            }

            final BatteryState batteryState = (payload[i + 1] & 0x80) != 0 ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;

            LOG.debug("Got battery {}: {}%, {}", batteryIndex, batteryLevel, batteryState);

            final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
            eventBatteryInfo.batteryIndex = batteryIndex;
            eventBatteryInfo.level = batteryLevel;
            eventBatteryInfo.state = batteryState;
            events.add(eventBatteryInfo);
        }

        List<Integer> processedBatteries = events.stream()
            .map(event -> ((GBDeviceEventBatteryInfo) event).batteryIndex)
            .toList();

        for (int i = 0; i < 2; i++) {
            if (processedBatteries.contains(i)) {
                continue;
            }
            
            final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
            eventBatteryInfo.batteryIndex = i;
            eventBatteryInfo.level = -1;
            eventBatteryInfo.state = BatteryState.UNKNOWN;
            events.add(eventBatteryInfo);
        }

        return events;
    }

    private static List<GBDeviceEvent> parseFirmware(final byte[] payload) {
        final List<GBDeviceEvent> events = new ArrayList<>();

        final String fwString;
        if (payload[payload.length - 1] == 0) {
            fwString = new String(ArrayUtils.subarray(payload, 2, payload.length - 1)).strip();
        } else {
            fwString = new String(ArrayUtils.subarray(payload, 2, payload.length)).strip();
        }
        final String[] parts = fwString.split(",");
        if (parts.length % 3 != 0) {
            LOG.warn("Fw parts length {} from '{}' is not divisible by 3", parts.length, fwString);

            // We need to persist something, otherwise Gb misbehaves
            final GBDeviceEventVersionInfo eventVersionInfo = new GBDeviceEventVersionInfo();
            eventVersionInfo.fwVersion = fwString;
            eventVersionInfo.hwVersion = GBApplication.getContext().getString(R.string.n_a);
            events.add(eventVersionInfo);

            return events;
        }
        final String[] fwVersionParts = new String[3];
        for (int i = 0; i < parts.length; i += 3) {
            final String versionPart = parts[i];
            final String versionType = parts[i + 1];
            final String version = parts[i + 2];
            if (!"2".equals(versionType)) {
                continue; // not fw
            }

            switch (versionPart) {
                case "1":
                    fwVersionParts[0] = version;
                    break;
                case "2":
                    fwVersionParts[1] = version;
                    break;
                case "3":
                    fwVersionParts[2] = version;
                    break;
                default:
                    LOG.warn("Unknown firmware version part {}", versionPart);
            }
        }

        final List<String> nonNullParts = new ArrayList<>(fwVersionParts.length);
        for (int i = 0; i < fwVersionParts.length; i++) {
            if (fwVersionParts[i] == null) {
                continue;
            }
            nonNullParts.add(fwVersionParts[i]);
            if (fwVersionParts[i].contains(".")) {
                // Realme devices have the version already with the dots, repeated multiple times
                break;
            }
        }
        final String fwVersion = String.join(".", nonNullParts);

        final GBDeviceEventVersionInfo eventVersionInfo = new GBDeviceEventVersionInfo();
        eventVersionInfo.fwVersion = fwVersion;
        eventVersionInfo.hwVersion = GBApplication.getContext().getString(R.string.n_a);
        events.add(eventVersionInfo);

        LOG.debug("Got fw version: {}", fwVersion);
        return events;
    }

    private static GBDeviceEvent parseMiscConfig(final byte[] payload) {
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        for (int i = 2; i + 1 < payload.length; i += 2) {
            final int code = payload[i] & 0xff;
            final int value = payload[i + 1] & 0xff;

            final MiscConfigType type = MiscConfigType.fromCode(code);
            if (type == null) {
                LOG.warn("Unknown misc config code {}", code);
                continue;
            }
            
            final boolean isEnabled = (value == 1);
            switch (type) {
                case LDAC:
                    LOG.debug("Got misc config for LDAC = {}", isEnabled);
                    eventUpdatePreferences.withPreference(
                        OppoHeadphonesPreferences.LDAC,
                        isEnabled
                    );
                    break;
                case MULTIPOINT:
                    LOG.debug("Got misc config for MULTIPOINT = {}", isEnabled);
                    eventUpdatePreferences.withPreference(
                        OppoHeadphonesPreferences.MULTIPOINT,
                        isEnabled
                    );
                    break;
                case GAME_MODE:
                    LOG.debug("Got misc config for GAME_MODE = {}", isEnabled);
                    eventUpdatePreferences.withPreference(
                        OppoHeadphonesPreferences.GAME_MODE,
                        isEnabled
                    );
                    break;
                default:
                    LOG.warn("Unknown misc config code {}", code);
                    break;
            }
        }
        return eventUpdatePreferences; 
    }

    private static GBDeviceEvent parseTouchConfig(final byte[] payload) {
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();

        for (int i = 2; i < payload.length; i += 4) {
            final int sideCode = payload[i] & 0xff;
            final int typeCode = BLETypeConversions.toUint16(payload, i + 1);
            final int valueCode = payload[i + 3] & 0xff;
            final TouchConfigSide side = TouchConfigSide.fromCode(sideCode);
            final TouchConfigType type = TouchConfigType.fromCode(typeCode);
            final TouchConfigValue value = TouchConfigValue.fromCode(valueCode);

            if (side == null) {
                LOG.warn("Unknown touch side code {}", sideCode);
                continue;
            }
            if (type == null) {
                LOG.warn("Unknown touch type code {}", typeCode);
                continue;
            }
            if (value == null) {
                LOG.warn("Unknown touch value code {}", valueCode);
                continue;
            }

            LOG.debug("Got touch config for {} {} = {}", side, type, value);

            eventUpdatePreferences.withPreference(
                    OppoHeadphonesPreferences.getTouchKey(side, type),
                    value.name().toLowerCase(Locale.ROOT)
            );
        }
        return eventUpdatePreferences; 
    }

    private static GBDeviceEvent parseAncConfig(final byte[] payload) {
        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences();

        for (int i = 2; i + 1 < payload.length; i += 2) {
            final int code = payload[i] & 0xff;
            final int value = payload[i + 1] & 0xff;

            switch (code) {
                case 0x01:
                    final AncConfigValue mode = AncConfigValue.fromCode(value);
                    if (mode != null) {
                        event.withPreference(OppoHeadphonesPreferences.ANC_SELECTOR, mode.getPrefId());
                    } else {
                        LOG.warn("Unknown ANC mode {}", value);
                    }
                    break;
                default:
                    LOG.debug("Unknown code {}", code);
                    break;
            }
        }
        return event;
    }

    @Override
    public byte[] encodeFirmwareVersionReq() {
        return encodeMessage(OppoCommand.FIRMWARE_GET, new byte[0]);
    }

    @Override
    public byte[] encodeFindDevice(final boolean start) {
        return encodeMessage(OppoCommand.FIND_DEVICE_REQ, new byte[]{(byte) (start ? 0x01 : 0x00)});
    }

    @Override
    public byte[] encodeSendConfiguration(final String config) {
        final DevicePrefs prefs = getDevicePrefs();

        if (config.startsWith(OppoHeadphonesPreferences.TOUCH_PREFIX)) {
            final String[] parts = config.split("__");
            final TouchConfigSide side = TouchConfigSide.valueOf(parts[1].toUpperCase(Locale.ROOT));
            final TouchConfigType type = TouchConfigType.valueOf(parts[2].toUpperCase(Locale.ROOT));
            final String valueCode = prefs.getString(OppoHeadphonesPreferences.getTouchKey(side, type), null);
            if (valueCode == null) {
                LOG.warn("Failed to get touch option value for {}/{}", side, type);
                return super.encodeSendConfiguration(config);
            }

            final TouchConfigValue value = TouchConfigValue.valueOf(valueCode.toUpperCase(Locale.ROOT));

            LOG.debug("Sending {} {} = {}", side, type, value);
            return encodeTouchConfigSet(side, type, value);
        }

        if (config.equals(OppoHeadphonesPreferences.LDAC)) {
            final boolean value = prefs.getBoolean(OppoHeadphonesPreferences.LDAC, false);
            LOG.debug("Sending ldac = {}", value);
            return encodeLdacSet(value);
        }

        if (config.equals(OppoHeadphonesPreferences.MULTIPOINT)) {
            final boolean value = prefs.getBoolean(OppoHeadphonesPreferences.MULTIPOINT, false);
            LOG.debug("Sending multipoint = {}", value);
            return encodeMultipointSet(value);
        }

        if (config.equals(OppoHeadphonesPreferences.GAME_MODE)) {
            final boolean value = prefs.getBoolean(OppoHeadphonesPreferences.GAME_MODE, false);
            LOG.debug("Sending game mode = {}", value);
            return encodeGameModeSet(value);
        }

        if (config.equals(OppoHeadphonesPreferences.ANC_SELECTOR)) {
            final String prefId = prefs.getString(OppoHeadphonesPreferences.ANC_SELECTOR, "0");
            AncConfigValue mode = AncConfigValue.fromPrefId(prefId);
            if (mode == null) {
                LOG.debug("Unknown ANC prefId = \"{}\"", prefId);
                mode = AncConfigValue.OFF;
            } 
            LOG.debug("Sending ANC mode = {}", mode);
            return encodeAncConfigSet(mode);
        }

        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeBatteryReq() {
        return encodeMessage(OppoCommand.BATTERY_REQ, new byte[0]);
    }

    private byte[] encodeTouchConfigSet(final TouchConfigSide side, final TouchConfigType type, final TouchConfigValue value) {
        final ByteBuffer buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x01);
        buf.put((byte) side.getCode());
        buf.putShort((short) type.getCode());
        buf.put((byte) value.getCode());
        return encodeMessage(OppoCommand.TOUCH_CONFIG_SET, buf.array());
    }

    public byte[] encodeTouchConfigReq() {
        return encodeMessage(OppoCommand.TOUCH_CONFIG_REQ, new byte[]{0x02, 0x03, 0x01});
    }

    public byte[] encodeLdacSet(final boolean enable) {
        final byte[] payload = new byte[] {
            (byte) (enable ? 0x01 : 0x00)
        };
        return encodeMiscConfigSet(MiscConfigType.LDAC, payload);
    }

    public byte[] encodeGameModeSet(final boolean enable) {
        final byte[] payload = new byte[] {
            (byte) (enable ? 0x01 : 0x00)
        };
        return encodeMiscConfigSet(MiscConfigType.GAME_MODE, payload);
    }

    public byte[] encodeMultipointSet(final boolean enable) {
        final byte[] payload = new byte[] {
            (byte) (enable ? 0x01 : 0x00)
        };
        return encodeMiscConfigSet(MiscConfigType.MULTIPOINT, payload);
    }

    private byte[] encodeMiscConfigSet(final MiscConfigType type, final byte[] value) {
        final byte[] payload = new byte[1 + value.length];
        payload[0] = (byte) type.getCode();
        System.arraycopy(value, 0, payload, 1, value.length);
        return encodeMessage(OppoCommand.MISC_CONFIG_SET, payload);
    }

    public byte[] encodeMiscConfigReq(List<MiscConfigType> types) {
        if (types == null || types.isEmpty()) {
            return encodeMessage(OppoCommand.MISC_CONFIG_REQ, new byte[]{0x00});
        }

        byte[] payload = new byte[1 + types.size()];
        payload[0] = (byte) types.size();

        for (int i = 0; i < types.size(); i++) {
            payload[i + 1] = (byte) types.get(i).getCode();
        }
        return encodeMessage(OppoCommand.MISC_CONFIG_REQ, payload);
    }

    public byte[] encodeAncConfigSet(final AncConfigValue mode) {
        final byte[] payload = new byte[] {
            (byte) 0x01,
            (byte) 0x01,
            (byte) mode.getCode()
        };
        return encodeMessage(OppoCommand.ANC_CONFIG_SET, payload);
    }

    public byte[] encodeAncConfigReq() {
        final byte[] payload = new byte[] {
            (byte) 0x01,
            (byte) 0x01,
        };
        return encodeMessage(OppoCommand.ANC_CONFIG_REQ, payload);
    }

    private byte[] encodeMessage(final OppoCommand command, final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.allocate(9 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_PREAMBLE);
        buf.put((byte) (buf.limit() - 2));
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.putShort(command.getCode());
        buf.put((byte) seqNum++);
        buf.putShort((short) payload.length);
        buf.put(payload);
        return buf.array();
    }
}
