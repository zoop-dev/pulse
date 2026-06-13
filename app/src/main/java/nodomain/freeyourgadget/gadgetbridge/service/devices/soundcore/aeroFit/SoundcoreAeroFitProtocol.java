package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.aeroFit;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AUTO_POWER_OFF;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_BATTERY_LOW_TONE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TOUCH_DISABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_GAMING_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.SoundcorePacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.protocol.impl.v1.SoundcoreProtocolImplV1;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SoundcoreAeroFitProtocol extends SoundcoreProtocolImplV1 {

    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreAeroFitProtocol.class);

    private static final short CMD_GET_CONNECTED_DEVICES = (short) 0x010b;
    private static final short CMD_SET_BATTERY_LOW_TONE = (short) 0x8210;
    private static final short CMD_SET_GAMING_MODE = (short) 0x8701;
    private static final short CMD_SET_TOUCH_LOCK = (short) 0x9410;

    private static final int battery_case = 0;
    private static final int battery_earphone_left = 1;
    private static final int battery_earphone_right = 2;

    protected SoundcoreAeroFitProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        SoundcorePacket packet = decodePacket(responseData);

        if (packet == null)
            return null;

        List<GBDeviceEvent> devEvts = new ArrayList<>();
        short cmd = packet.getCommand();
        byte[] payload = packet.getPayload();

        if (cmd == CMD_GET_DEVICE_INFO) {
            String firmware1 = readString(payload, 4, 5);
            String firmware2 = readString(payload, 9, 5);
            String serialNumber = readString(payload, 14, 16);

            handleBatteryInfo(devEvts, null, payload[2], payload[3]);
            devEvts.add(buildVersionInfo(firmware1, firmware2, serialNumber));
        } else if (cmd == CMD_GET_CONNECTED_DEVICES) {
            // shows connected devices from 50 onwards
            // readString(payload, 50, 112)
            // maybe also other settings
            LOG.debug("Incoming Information about connected devices, dump: " + hexdump(responseData));
        } else if (cmd == CMD_NOTIFY_BATTERY_INFO) {
            handleBatteryInfo(devEvts, payload[2], payload[0], payload[1]);
        } else if (cmd == CMD_NOTIFY_CHARGING_INFO) {
            boolean leftInCase = payload[0] == 0x01;
            boolean rightInCase = payload[1] == 0x01;
            LOG.info("Left Earbud in Charging Case: " + leftInCase + ", Right Earbud in Charging Case: " + rightInCase);
        } else {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
    }

    private void handleBatteryInfo(List<GBDeviceEvent> devEvts, Byte batteryCase, byte batteryLeft, byte batteryRight) {
        int batteryLeftLevel = (batteryLeft + 1) * 10;
        int batteryRightLevel = (batteryRight + 1) * 10;
        devEvts.add(buildBatteryInfo(battery_earphone_left, batteryLeftLevel));
        devEvts.add(buildBatteryInfo(battery_earphone_right, batteryRightLevel));

        if (batteryCase != null) {
            int batteryCaseLevel = (batteryCase + 1) * 10;
            devEvts.add(buildBatteryInfo(battery_case, batteryCaseLevel));
        }
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        Prefs prefs = getDevicePrefs();

        switch (config) {
            // Control
            case PREF_SOUNDCORE_CONTROL_TOUCH_DISABLED:
                boolean touchDisabled = prefs.getBoolean(PREF_SOUNDCORE_CONTROL_TOUCH_DISABLED, false);
                return encodeBooleanCommand(CMD_SET_TOUCH_LOCK, touchDisabled);

            // Miscellaneous Settings
            case PREF_SOUNDCORE_TOUCH_TONE:
                boolean touchTone = prefs.getBoolean(PREF_SOUNDCORE_TOUCH_TONE, false);
                return encodeBooleanCommand(CMD_SET_TOUCH_TONE, touchTone);
            case PREF_SOUNDCORE_BATTERY_LOW_TONE:
                boolean batteryLowTone = prefs.getBoolean(PREF_SOUNDCORE_BATTERY_LOW_TONE, false);
                return encodeBooleanCommand(CMD_SET_BATTERY_LOW_TONE, batteryLowTone);
            case PREF_SOUNDCORE_GAMING_MODE:
                boolean gamingMode = prefs.getBoolean(PREF_SOUNDCORE_GAMING_MODE, false);
                return encodeBooleanCommand(CMD_SET_GAMING_MODE, gamingMode);
            case PREF_SOUNDCORE_AUTO_POWER_OFF:
                int duration = Integer.parseInt(prefs.getString(PREF_SOUNDCORE_AUTO_POWER_OFF, "3"));
                return encodeAutoPowerOff(CMD_SET_AUTO_POWER_OFF, duration, (byte) 0x03);
            default:
                LOG.debug("Unsupported CONFIG: " + config);
        }

        return super.encodeSendConfiguration(config);
    }

}
