package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Interactions;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunPacketEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(EarFunPacketEncoder.class);
    // the factor to converting equalizer gain between preference value and
    // payload byte (int) value
    // Gaia uses a factor of 60 to convert to dB and EarFun projects 6 dBs on a slider scale of 10
    private static final double EQUALIZER_GAIN_FACTOR = 60 * 0.6;

    public static byte[] encodeCommonSettingsReq() {
        return joinPackets(
                // battery levels
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_BATTERY_STATE_LEFT).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_BATTERY_STATE_RIGHT).encode(),
                // sound settings
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_GAME_MODE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_AMBIENT_SOUND).encode(),
                // touch settings
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_TOUCH_ACTION).encode()
        );
    }

    public static byte[] encodeAirPro4SettingsReq() {
        return joinPackets(
                encodeCommonSettingsReq(),
                // battery levels
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_BATTERY_STATE_CASE).encode(),
                // sound settings
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_ANC_MODE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_TRANSPARENCY_MODE).encode(),
                // touch settings
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_TOUCH_MODE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_DISABLE_IN_EAR_DETECTION).encode(),
                // system settings
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_CONNECT_TWO_DEVICES).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_ADVANCED_AUDIO_MODE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_MICROPHONE_MODE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_FIND_DEVICE).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_VOICE_PROMPT_VOLUME).encode(),
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_AUDIO_CODEC).encode()
        );
    }

    public static byte[] encodeFirmwareVersionReq() {
        return new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_FIRMWARE_VERSION).encode();
    }

    public static byte[] encodeSetGesture(Prefs prefs, String config, Interactions.InteractionType interactionType, Interactions.Position position) {
        byte action = (byte) (Integer.parseInt(prefs.getString(config, "0")) & 0xFF);
        byte[] payload;
        if (position == Interactions.Position.LEFT) {
            payload = new byte[]{interactionType.value, action, (byte) 0x00, (byte) 0x00};
        } else {
            payload = new byte[]{(byte) 0x00, (byte) 0x00, interactionType.value, action};
        }
        return new EarFunPacket(EarFunPacket.Command.SET_TOUCH_ACTION, payload).encode();
    }

    public static byte[] encodeSetEqualizerSixBands(Prefs prefs) {
        List<byte[]> equalizerConfig = Arrays.stream(Equalizer.SixBandEqualizer).map(bandConfig -> {
            if (bandConfig.key != null) {
                return encodeSetEqualizerBand((short) (prefs.getInt(bandConfig.key, 0) & 0xFFFF), bandConfig.band);
            } else {
                return encodeSetEqualizerBand(bandConfig.band.defaultGain, bandConfig.band);
            }
        }).collect(Collectors.toList());
        return joinPackets(equalizerConfig);
    }

    public static byte[] encodeSetEqualizerTenBands(Prefs prefs) {
        List<byte[]> equalizerConfig = Arrays.stream(Equalizer.TenBandEqualizer).map(bandConfig -> {
            if (bandConfig.key != null) {
                return encodeSetEqualizerBand((short) (prefs.getInt(bandConfig.key, 0) & 0xFFFF), bandConfig.band);
            } else {
                return encodeSetEqualizerBand(bandConfig.band.defaultGain, bandConfig.band);
            }
        }).collect(Collectors.toList());
        return joinPackets(equalizerConfig);
    }

    public static byte[] encodeSetEqualizerBand(double gainValue, Equalizer.Band band) {
        short gain = (short) ((int) Math.round(gainValue * EQUALIZER_GAIN_FACTOR) & 0xFFFF);
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put(band.bandId);
        buf.put((byte) 0xFF);
        buf.put((byte) 0x98);
        buf.putShort(band.frequency);
        buf.putShort(gain);
        buf.putShort(band.qFactor);
        byte[] payload = buf.array();
        LOG.debug("equalizer payload: {}", hexdump(payload));
        return new EarFunPacket(EarFunPacket.Command.SET_EQUALIZER_BAND, payload).encode();
    }

    public static byte[] joinPackets(List<byte[]> arrays) {
        int totalLength = arrays.stream().mapToInt(array -> array.length).sum();
        ByteBuffer byteBuffer = ByteBuffer.allocate(totalLength);
        arrays.forEach(byteBuffer::put);
        return byteBuffer.array();
    }

    public static byte[] joinPackets(byte[]... arrays) {
        return joinPackets(Arrays.asList(arrays));
    }
}
