package nodomain.freeyourgadget.gadgetbridge.service.devices.haylou;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HaylouS35AncProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(HaylouS35AncProtocol.class);

    public static final int MAX_DEVICE_NAME_BYTES = 26;

    private static final byte[] HEADER = new byte[]{(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xc0};
    private static final byte[] FOOTER = new byte[]{(byte) 0xdd, (byte) 0xee, (byte) 0xff};
    private static final byte SEQUENCE = 0x00;


    public byte[] encodeHandshake() {
        return encodeCommand(GB.hexStringToByteArray("020005000000000f"));
    }

    public byte[] encodeAudioMode(final String mode) {
        byte anc_mode;
        switch (mode) {
            case "off":
                anc_mode = 0x00;
                break;
            case "anc":
                anc_mode = 0x01;
                break;
            case "transparency":
                anc_mode = 0x02;
                break;
            default:
                LOG.error("Invalid Audio Mode selected");
                return null;
        }

        return encodeFeature((byte) 0x04, anc_mode);
    }

    public byte[] encodeGameMode(final boolean enabled) {
        return encodeFeature((byte) 0x05, boolToValue(enabled));
    }

    public byte[] encodeLdacMode(final boolean enabled) {
        return encodeFeature((byte) 0x08, boolToValue(enabled));
    }

    public byte[] encodeMultipoint(final boolean enabled) {
        return encodeFeature((byte) 0x09, boolToValue(enabled));
    }

    public byte[] encodeEqPreset(final String preset) {
        byte eq_preset;
        switch (preset) {
            case "default":
                eq_preset = 0x00;
                break;
            case "rock":
                eq_preset = 0x02;
                break;
            case "classic":
                eq_preset = 0x03;
                break;
            case "bass":
                eq_preset = 0x06;
                break;
            case "soft":
                eq_preset = 0x07;
                break;
            default:
                LOG.error("Invalid EQ preset selected");
                return null;
        }

        return encodeCommand(new byte[]{(byte) 0xf2, 0x00, 0x05, SEQUENCE, 0x03, 0x00, 0x07, eq_preset});
    }

    public byte[] encodeFindDevice(final boolean start) {
        return encodeCommand(new byte[]{(byte) 0xf2, 0x00, 0x06, SEQUENCE, 0x04, 0x00, 0x09, boolToValue(start), 0x03});
    }

    public byte[] encodeReset() {
        return encodeCommand(GB.hexStringToByteArray("1100010e"));
    }

    public GBDeviceEvent[] decodeResponse(final byte[] response) {
        LOG.debug("received data: " + hexdump(response));
        if (response == null || response.length < HEADER.length + FOOTER.length + 1) {
            return new GBDeviceEvent[0];
        }

        if (!hasHeader(response)) {
            LOG.debug("Ignoring response without expected header: {}", GB.hexdump(response));
            return new GBDeviceEvent[0];
        }

        final int length = ((response[5] & 0xff) << 8) | (response[6] & 0xff);
        final int payloadStart = 7;
        final int payloadEnd = payloadStart + length;
        if (length <= 0 || payloadEnd > response.length - FOOTER.length) {
            LOG.debug("Ignoring response with invalid length {}: {}", length, GB.hexdump(response));
            return new GBDeviceEvent[0];
        }

        final byte[] payload = new byte[length];
        System.arraycopy(response, payloadStart, payload, 0, length);

        final List<GBDeviceEvent> events = new ArrayList<>();
        final int battery = payload[3];
        final GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.batteryIndex = 0;
        batteryInfo.level = battery;
        batteryInfo.state = BatteryState.BATTERY_NORMAL;
        events.add(batteryInfo);

        return events.toArray(new GBDeviceEvent[0]);
    }

    private byte[] encodeFeature(final byte feature, final byte value) {
        return encodeCommand(new byte[]{0x08, 0x00, 0x04, SEQUENCE, 0x02, feature, value});
    }

    private byte boolToValue(final boolean enabled) {
        return enabled ? (byte) 0x01 : (byte) 0x00;
    }

    private static byte[] encodeCommand(final byte[] payload) {
        final ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.writeBytes(HEADER);
        packet.writeBytes(payload);
        packet.writeBytes(FOOTER);
        return packet.toByteArray();
    }

    private static boolean hasHeader(final byte[] response) {
        for (int i = 0; i < HEADER.length; i++) {
            if (response[i] != HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
