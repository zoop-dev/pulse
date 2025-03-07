package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunPacketEncoder.joinPackets;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.EarFunResponseHandler.*;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Interactions;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(EarFunProtocol.class);

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] data) {
        List<GBDeviceEvent> events = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data);

        while (buf.hasRemaining()) {
            EarFunPacket packet = EarFunPacket.decode(buf);

            if (packet == null) break;

            EarFunPacket.Command command = packet.getCommand();
            byte[] payload = packet.getPayload();
            LOG.info("received {} {}", packet.getCommand().name(), packet);

            switch (command) {
                case REQUEST_RESPONSE_BATTERY_STATE_LEFT:
                    events.add(handleBatteryInfo(0, payload));
                    break;
                case REQUEST_RESPONSE_BATTERY_STATE_RIGHT:
                    events.add(handleBatteryInfo(1, payload));
                    break;
                case REQUEST_RESPONSE_BATTERY_STATE_CASE:
                    events.add(handleBatteryInfo(2, payload));
                    break;
                case REQUEST_RESPONSE_FIRMWARE_VERSION:
                    events.add(handleFirmwareVersionInfo(payload));
                    break;
                case REQUEST_RESPONSE_GAME_MODE:
                    events.add(handleGameModeInfo(payload));
                    break;
                case REQUEST_RESPONSE_AMBIENT_SOUND:
                    events.add(handleAmbientSoundInfo(payload));
                    break;
                case REQUEST_RESPONSE_ANC_MODE:
                    events.add(handleAncModeInfo(payload));
                    break;
                case REQUEST_RESPONSE_TRANSPARENCY_MODE:
                    events.add(handleTransparencyModeInfo(payload));
                    break;
                case REQUEST_RESPONSE_TOUCH_ACTION:
                    events.add(handleTouchActionInfo(payload));
                    break;
                // do nothing with these, they are returned after each EQ set operation and always return 01
                case RESPONSE_EQUALIZER_BAND:
                    break;
                default:
                    LOG.error("no handler for packet type {}", packet.getCommand().name());
            }
        }
        return events.toArray(new GBDeviceEvent[0]);
    }

    @Override
    public byte[] encodeTestNewFunction() {
        return joinPackets(
                new EarFunPacket(EarFunPacket.Command.REQUEST_RESPONSE_0318).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0321).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0326).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_032C).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_032F).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0331).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0333).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0335).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0339).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_034A).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_034C).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_034D).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0348).encode(),
                new EarFunPacket(EarFunPacket.Command.UNIDENTIFIED_0350).encode()
        );
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        Prefs prefs = getDevicePrefs();
        switch (config) {
            case PREF_EARFUN_AMBIENT_SOUND_CONTROL:
                int ambientSound = Integer.parseInt(prefs.getString(PREF_EARFUN_AMBIENT_SOUND_CONTROL, "0"));
                return new EarFunPacket(EarFunPacket.Command.SET_AMBIENT_SOUND, (byte) ambientSound).encode();
            case PREF_EARFUN_SINGLE_TAP_LEFT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.SINGLE, Interactions.Position.LEFT);
            case PREF_EARFUN_SINGLE_TAP_RIGHT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.SINGLE, Interactions.Position.RIGHT);
            case PREF_EARFUN_DOUBLE_TAP_LEFT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.DOUBLE, Interactions.Position.LEFT);
            case PREF_EARFUN_DOUBLE_TAP_RIGHT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.DOUBLE, Interactions.Position.RIGHT);
            case PREF_EARFUN_TRIPPLE_TAP_LEFT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.TRIPLE, Interactions.Position.LEFT);
            case PREF_EARFUN_TRIPPLE_TAP_RIGHT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.TRIPLE, Interactions.Position.RIGHT);
            case PREF_EARFUN_LONG_TAP_LEFT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.LONG, Interactions.Position.LEFT);
            case PREF_EARFUN_LONG_TAP_RIGHT_ACTION:
                return EarFunPacketEncoder.encodeSetGesture(prefs, config, Interactions.InteractionType.LONG, Interactions.Position.RIGHT);
            case PREF_EARFUN_GAME_MODE:
                int gameMode = prefs.getBoolean(PREF_EARFUN_GAME_MODE, false) ? 1 : 0;
                return new EarFunPacket(EarFunPacket.Command.SET_GAME_MODE, (byte) gameMode).encode();
            case PREF_EARFUN_DEVICE_NAME:
                String deviceName = prefs.getString(PREF_EARFUN_DEVICE_NAME, "");
                byte[] utf8EncodedName = deviceName.getBytes(StandardCharsets.UTF_8);
                return new EarFunPacket(EarFunPacket.Command.SET_DEVICENAME, utf8EncodedName).encode();
            case PREF_EARFUN_ANC_MODE:
                byte ancMode = (byte) (Integer.parseInt(prefs.getString(PREF_EARFUN_ANC_MODE, "0")) & 0xFF);
                return new EarFunPacket(EarFunPacket.Command.SET_ANC_MODE, ancMode).encode();
            case PREF_EARFUN_TRANSPARENCY_MODE:
                byte transparencyMode = (byte) (Integer.parseInt(prefs.getString(PREF_EARFUN_TRANSPARENCY_MODE, "0")) & 0xFF);
                return new EarFunPacket(EarFunPacket.Command.SET_TRANSPARENCY_MODE, transparencyMode).encode();
            default:
                LOG.error("unhandled send configuration {}", config);
        }
        return null;
    }

    public byte[] encodeBatteryReq() {
        return EarFunPacketEncoder.encodeBatteryReq();
    }

    public byte[] encodeSoundReq() {
        return EarFunPacketEncoder.encodeSoundReq();
    }

    public byte[] encodeTouchActionReq() {
        return EarFunPacketEncoder.encodeTouchActionReq();
    }

    @Override
    public byte[] encodeFirmwareVersionReq() {
        return EarFunPacketEncoder.encodeFirmwareVersionReq();
    }

    protected EarFunProtocol(GBDevice device) {
        super(device);
    }
}
