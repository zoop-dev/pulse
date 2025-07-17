package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.aeroFit;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AUTO_POWER_OFF;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_BATTERY_LOW_TONE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TOUCH_DISABLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_GAMING_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.AbstractSoundcoreProtocol;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.SoundcorePacket;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SoundcoreAeroFitProtocol extends AbstractSoundcoreProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreAeroFitProtocol.class);

    private static final int battery_case = 0;
    private static final int battery_earphone_left = 1;
    private static final int battery_earphone_right = 2;

    protected SoundcoreAeroFitProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        SoundcorePacket packet = SoundcorePacket.decode(buf);

        if (packet == null)
            return null;

        List<GBDeviceEvent> devEvts = new ArrayList<>();
        short cmd = packet.getCommand();
        byte[] payload = packet.getPayload();

        if (cmd == (short) 0x0101) {
            String firmware1 = readString(payload, 4, 5);
            String firmware2 = readString(payload, 9, 5);
            String serialNumber = readString(payload, 14, 16);

            handleBatteryInfo(devEvts, null, payload[2], payload[3]);
            devEvts.add(buildVersionInfo(firmware1, firmware2, serialNumber));
        } else if (cmd == (short) 0x010b) {
            // shows connected devices from 50 onwards
            // readString(payload, 50, 112)
            // maybe also other settings
            LOG.debug("Incoming Information about connected devices, dump: " + hexdump(responseData));
        } else if (cmd == (short) 0x0301) { // Battery Update
            handleBatteryInfo(devEvts, payload[2], payload[0], payload[1]);
        } else if (cmd == (short) 0x0401) {
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
                return new SoundcorePacket((short) 0x9410, new byte[]{encodeBoolean(touchDisabled)}).encode();

            // Miscellaneous Settings
            case PREF_SOUNDCORE_TOUCH_TONE:
                boolean touchTone = prefs.getBoolean(PREF_SOUNDCORE_TOUCH_TONE, false);
                return new SoundcorePacket((short) 0x8301, new byte[]{encodeBoolean(touchTone)}).encode();
            case PREF_SOUNDCORE_BATTERY_LOW_TONE:
                boolean batteryLowTone = prefs.getBoolean(PREF_SOUNDCORE_BATTERY_LOW_TONE, false);
                return new SoundcorePacket((short) 0x8210, new byte[]{encodeBoolean(batteryLowTone)}).encode();
            case PREF_SOUNDCORE_GAMING_MODE:
                boolean gamingMode = prefs.getBoolean(PREF_SOUNDCORE_GAMING_MODE, false);
                return new SoundcorePacket((short) 0x8701, new byte[]{encodeBoolean(gamingMode)}).encode();
            case PREF_SOUNDCORE_AUTO_POWER_OFF:
                int duration = Integer.parseInt(prefs.getString(PREF_SOUNDCORE_AUTO_POWER_OFF, "3"));
                return setAutoPowerOff(duration).encode();
            default:
                LOG.debug("Unsupported CONFIG: " + config);
        }

        return super.encodeSendConfiguration(config);
    }

    /**
     * 0: No Auto Power off
     * 1: Auto Power off 10 min
     * 2: Auto Power off 20 min
     * 3: Auto Power off 30 min
     * 4: Auto Power off 60 min
    */
    private SoundcorePacket setAutoPowerOff(int duration) {
        byte[] payload;

        if (duration > 0)
            payload = new byte[] { (byte)0x01, (byte)(duration - 1) };
        else
            payload = new byte[] { (byte)0x00, (byte)0x03 };

        return new SoundcorePacket((short) 0x8601, payload);
    }

    @Override
    public byte[] encodeFindDevice(boolean start) {
        boolean findLeft = start;
        boolean findRight = start;
        byte[] payload = new byte[]{encodeBoolean(findLeft), encodeBoolean(findRight), 0x00};
        return new SoundcorePacket((short) 0x8910, payload).encode();
    }
}
