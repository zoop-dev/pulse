package nodomain.freeyourgadget.gadgetbridge.service.devices.soundbrenner;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.soundbrenner.SoundbrennerConstants;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;

public class SoundbrennerSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(SoundbrennerSupport.class);

    /** Mirrors the last command sent so the UI and onCharacteristicChanged stay in sync. */
    private boolean metronomeRunning = false;

    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final BatteryInfoProfile<SoundbrennerSupport> batteryInfoProfile;

    public SoundbrennerSupport() {
        super(LOG);
        addSupportedService(SoundbrennerConstants.UUID_SERVICE_METRONOME);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(new IntentListener() {
            @Override
            public void notify(Intent intent) {
                if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(intent.getAction())) {
                    handleBatteryInfo((BatteryInfo)
                            intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
                }
            }
        });
        addSupportedProfile(batteryInfoProfile);
    }

    // -------------------------------------------------------------------------
    // AbstractBTLESingleDeviceSupport
    // -------------------------------------------------------------------------

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing Soundbrenner Core");

        // Request maximum MTU first – the 32-byte config packet exceeds the default 20-byte limit.
        builder.requestMtu(512);

        // Battery Service: handled via BatteryInfoProfile (standard GATT Battery Service)
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        builder.setDeviceState(GBDevice.State.INITIALIZING);

        // Enable NOTIFY on CHAR_CONFIG to receive watch-side parameter changes.
        BluetoothGattCharacteristic configChar =
                getCharacteristic(SoundbrennerConstants.UUID_CHARACTERISTIC_CONFIG);
        if (configChar != null) {
            builder.notify(configChar, true);
        } else {
            LOG.warn("CHAR_CONFIG not found during init – notifications disabled");
        }

        // Enable INDICATE on CHAR_CONTROL to track start/stop confirmations.
        BluetoothGattCharacteristic controlChar =
                getCharacteristic(SoundbrennerConstants.UUID_CHARACTERISTIC_CONTROL);
        if (controlChar != null) {
            builder.notify(controlChar, true);
        } else {
            LOG.warn("CHAR_CONTROL not found during init – indications disabled");
        }

        // Restore previously persisted metronome state flag.
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(
                getDevice().getAddress());
        metronomeRunning = prefs.getBoolean(SoundbrennerConstants.PREF_METRONOME_RUNNING, false);

        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    // -------------------------------------------------------------------------
    // Metronome control
    // -------------------------------------------------------------------------

    /**
     * Toggle the metronome on/off. If turning on, the current configuration
     * is written first so the device is always in a consistent state.
     */
    public void toggleMetronome() {
        try {
            if (metronomeRunning) {
                stopMetronome();
            } else {
                startMetronome();
            }
        } catch (Exception e) {
            LOG.error("Error toggling metronome", e);
        }
    }

    /** Write config then send start command. */
    public void startMetronome() throws IOException {
        TransactionBuilder builder = performInitialized("StartMetronome");
        writeConfigPacket(builder);
        sendPreStartCommand(builder);   //  0xd1 to 0x0063 before start
        writeConfigPacket(builder);     // seconds Config Write as in snoop log
        sendControlCommand(builder, SoundbrennerConstants.CMD_METRONOME_START);
        builder.queue();
        persistMetronomeRunning(true);
        metronomeRunning = true;
        LOG.info("Metronome start command queued");
    }

    /** Send stop command only (no config write needed). */
    public void stopMetronome() throws IOException {
        TransactionBuilder builder = performInitialized("StopMetronome");
        sendControlCommand(builder, SoundbrennerConstants.CMD_METRONOME_STOP);
        builder.queue();
        persistMetronomeRunning(false);
        metronomeRunning = false;
        LOG.info("Metronome stop command queued");
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Called by Gadgetbridge whenever a device-specific preference changes.
     * Checks the key against known prefix constants – no hard-coded strings.
     */
     @Override
     public void onSendConfiguration(String key) {
       // UI button toggle
         if ((SoundbrennerConstants.PREF_METRONOME_RUNNING + "_toggle").equals(key)) {
             toggleMetronome();
             return;
         }

         boolean isSoundbrennerPref = key.startsWith(SoundbrennerConstants.PREF_SOUNDBRENNER_PREFIX)
                 || key.startsWith(SoundbrennerConstants.PREF_BEAT_ACCENT_PREFIX);

         if (!isSoundbrennerPref) {
             return;
         }

         LOG.info("Soundbrenner pref changed: {} – rebuilding config packet", key);
         try {
             TransactionBuilder builder = performInitialized("SendConfig");
             writeConfigPacket(builder);
             builder.queue();
         } catch (IOException e) {
             LOG.error("Failed to send configuration packet", e);
         }
     }

    // -------------------------------------------------------------------------
    // GATT callbacks
    // -------------------------------------------------------------------------

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        if (SoundbrennerConstants.UUID_CHARACTERISTIC_CONTROL.equals(
                characteristic.getUuid())) {
            handleControlNotify(value);
            return true;
        }

        if (SoundbrennerConstants.UUID_CHARACTERISTIC_CONFIG.equals(
                characteristic.getUuid())) {
            handleConfigNotify(value);
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic,
                                        byte[] value,
                                        int status) {
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    // -------------------------------------------------------------------------
    // Private – packet builder
    // -------------------------------------------------------------------------

    /**
     * Build and write the full 32-byte configuration packet from the current
     * device-specific SharedPreferences.
     *
     * All SeekBarPreference values are stored as {@code int} via {@code putInt}.
     */
    private void writeConfigPacket(TransactionBuilder builder) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(
                getDevice().getAddress());

        // --- Read preferences ---
        // PREF_BPM is stored by SeekBarPreference as int.
        int bpm = prefs.getInt(SoundbrennerConstants.PREF_BPM,
                SoundbrennerConstants.DEFAULT_BPM);

        // PREF_TIME_SIGNATURE is a ListPreference → stored as String.
        String timeSig = prefs.getString(SoundbrennerConstants.PREF_TIME_SIGNATURE,
                SoundbrennerConstants.DEFAULT_TIME_SIG);

        // PREF_SUBDIVISION is a ListPreference → stored as String, never as int.
        // Using getInt() here causes a ClassCastException → crash.
        int subdivision;
        try {
            subdivision = Integer.parseInt(
                    prefs.getString(SoundbrennerConstants.PREF_SUBDIVISION,
                            String.valueOf(SoundbrennerConstants.DEFAULT_SUBDIVISION)));
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse subdivision pref, using default");
            subdivision = SoundbrennerConstants.DEFAULT_SUBDIVISION;
        }

        // Parse time signature (e.g. "4/4" -> numerator=4, denominator=4)
        int numerator   = 4;
        int denominator = 4;
        try {
            String[] parts = timeSig.split("/");
            numerator   = Integer.parseInt(parts[0]);
            denominator = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            LOG.error("Failed to parse time signature '{}', using 4/4", timeSig);
        }

        // Read per-beat accent strengths (beats 1..8; unused beats default to soft)
        int[] beatStrengths = new int[8];
        for (int i = 0; i < 8; i++) {
            int defaultStrength = (i == 0)
                    ? SoundbrennerConstants.DEFAULT_BEAT_ACCENT_1
                    : SoundbrennerConstants.DEFAULT_BEAT_ACCENT_N;
            beatStrengths[i] = prefs.getInt(SoundbrennerConstants.PREF_BEATS[i], defaultStrength);
        }

        // --- Build packet (structure mirrors the validated buildPacket reference) ---
        byte[] packet = new byte[SoundbrennerConstants.PACKET_SIZE];

        // Bytes 0-3: BPM as IEEE 754 float, big-endian
        int bpmBits = Float.floatToIntBits((float) bpm);
        packet[0] = (byte) ((bpmBits >> 24) & 0xFF);
        packet[1] = (byte) ((bpmBits >> 16) & 0xFF);
        packet[2] = (byte) ((bpmBits >>  8) & 0xFF);
        packet[3] = (byte) ( bpmBits        & 0xFF);

        // Bytes 4: time signature
        // High nibble = (numerator-1), low nibble = log2(denominator)
        int log2denom = (int) (Math.log(denominator) / Math.log(2));
        byte timeSigByte = (byte) (((numerator - 1) & 0xF) << 4 | (log2denom & 0xF));
        packet[4] = timeSigByte;

        // Byte 5: beat strengths beats 1-4, 2 bits per beat MSB-first
        // Byte 6: beat strengths beats 5-8; unused slots = 0x00 (wire default)
        // Byte 7: unused, fixed 0x00
        int strengthHi = 0;
        int strengthLo = 0x00;
        for (int i = 0; i < Math.min(numerator, 4); i++) {
            strengthHi |= (strengthToDeviceBits(beatStrengths[i]) << (6 - i * 2));
        }
        for (int i = 4; i < Math.min(numerator, 8); i++) {
            int slot = i - 4;
            strengthLo &= ~(0x3 << (6 - slot * 2));
            strengthLo |=  (strengthToDeviceBits(beatStrengths[i]) << (6 - slot * 2));
        }
        packet[5] = (byte) strengthHi;
        packet[6] = (byte) strengthLo;
        packet[7] = (byte) 0x00;

        // Byte 8: fixed 0x00
        packet[8] = (byte) 0x00;

        // Bytes 9-10: subdivision constant, 16-bit big-endian
        // Formula: (steps - 1) * (9352 + 2 * numerator)
        int subdivConst = (subdivision - 1) * (9352 + 2 * numerator);
        packet[9]  = (byte) ((subdivConst >> 8) & 0xFF);
        packet[10] = (byte) ( subdivConst       & 0xFF);

        // Bytes 11-14: fixed 0x00
        packet[11] = (byte) 0x00;
        packet[12] = (byte) 0x00;
        packet[13] = (byte) 0x00;
        packet[14] = (byte) 0x00;

        // Bytes 15..(15+numerator-1): one pattern byte per active beat
        // subdivisionPatternByte() returns the bitmask shifted to bits 7-4
        byte beatPatternByte = subdivisionPatternByte(subdivision);
        for (int i = 0; i < numerator; i++) {
            packet[15 + i] = beatPatternByte;
        }

        // Bytes (15+numerator)..30: inactive beat slots = 0x00
        for (int i = 15 + numerator; i <= 30; i++) {
            packet[i] = (byte) 0x00;
        }

        // Byte 31: footer
        packet[31] = (byte) 0x00;

        // --- Write ---
        BluetoothGattCharacteristic configChar =
                getCharacteristic(SoundbrennerConstants.UUID_CHARACTERISTIC_CONFIG);
        if (configChar != null) {
            builder.write(configChar, packet);
            LOG.debug("Config packet queued: bpm={}, timeSig={}, subdiv={}", bpm, timeSig, subdivision);
        } else {
            LOG.error("CHAR_CONFIG not found – cannot write configuration");
        }
    }


    /**
     * Map UI accent strength (0-3) to the 2-bit inverted wire value.
     * UI to wire: 0 to 3 (silent), 1 to 0 (soft), 2 to 1 (medium), 3 to 2 (strong)
     */
    private int strengthToDeviceBits(int uiStrength) {
        return (uiStrength == 0) ? 3 : (uiStrength - 1);
    }

    /**
     * Return the beat-pattern byte for a given subdivision step count.
     * Bits 7-4 = active subdivision steps; bits 3-0 = always 0.
     */
    private byte subdivisionPatternByte(int steps) {
        switch (steps) {
            case 2:  return SoundbrennerConstants.BEAT_PATTERN_EIGHTH;
            case 3:  return SoundbrennerConstants.BEAT_PATTERN_TRIPLET;
            case 4:  return SoundbrennerConstants.BEAT_PATTERN_SIXTEENTH;
            default: return SoundbrennerConstants.BEAT_PATTERN_QUARTER; // 1 = quarter
        }
    }

    /** Write a single-byte command to CHAR_CONTROL. */
    private void sendControlCommand(TransactionBuilder builder, byte command) {
        BluetoothGattCharacteristic controlChar =
                getCharacteristic(SoundbrennerConstants.UUID_CHARACTERISTIC_CONTROL);
        if (controlChar != null) {
            builder.write(controlChar, new byte[]{command});
        } else {
            LOG.error("CHAR_CONTROL not found – cannot send command 0x{:02X}", command & 0xFF);
        }
    }

    /**
     * Write the magic pre-start byte (0xd1) to the unknown control characteristic
     * (handle 0x0063, UUID 061a5b49). The official app always sends this before
     * writing the final config and issuing CMD_METRONOME_START.
     */
    private void sendPreStartCommand(TransactionBuilder builder) {
        BluetoothGattCharacteristic unknownCtrl =
                getCharacteristic(SoundbrennerConstants.UUID_CHARACTERISTIC_UNKNOWN_CTRL);
        if (unknownCtrl != null) {
            builder.write(unknownCtrl, SoundbrennerConstants.CMD_PRE_START);
            LOG.debug("Pre-start byte (0xd1) queued on 0x0063");
        } else {
            LOG.warn("UUID_CHARACTERISTIC_UNKNOWN_CTRL not found – skipping pre-start");
        }
    }

    // -------------------------------------------------------------------------
    // Private – notify handlers
    // -------------------------------------------------------------------------

    /**
     * Handle an INDICATE from CHAR_CONTROL (device confirms start/stop).
     */
    private void handleControlNotify(byte[] value) {
        if (value == null || value.length == 0) return;
        if (value[0] == SoundbrennerConstants.CMD_METRONOME_START) {
            metronomeRunning = true;
            persistMetronomeRunning(true);
            LOG.info("Device confirmed: metronome started");
        } else if (value[0] == SoundbrennerConstants.CMD_METRONOME_STOP) {
            metronomeRunning = false;
            persistMetronomeRunning(false);
            LOG.info("Device confirmed: metronome stopped");
        }
    }

    private void handleBatteryInfo(BatteryInfo info) {
        if (info == null) return;
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
        LOG.info("Battery level: {}%", batteryCmd.level);
    }
    /**
     * Handle a NOTIFY from CHAR_CONFIG (user changed settings on the watch).
     * Parses the 32-byte packet and writes the new values back to SharedPreferences
     * so the Gadgetbridge UI stays in sync.
     */
    private void handleConfigNotify(byte[] value) {
        if (value == null || value.length < SoundbrennerConstants.PACKET_SIZE) {
            LOG.warn("Received short/null config notify payload ({} bytes)",
                    value == null ? 0 : value.length);
            return;
        }

        ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);

        // Bytes 0-3: BPM
        float bpm = buf.getFloat();

        // Byte 4: time signature
        buf.position(4);
        int timeSigByte = buf.get() & 0xFF;
        int numerator   = (timeSigByte >> 4) + 1;
        int log2denom   = timeSigByte & 0x0F;
        int denominator = 1 << log2denom;
        String timeSig  = numerator + "/" + denominator;

        // Byte 5: beat strengths 1-4 (position is already 5 after the get() above)
        int rawStrengths1_4 = buf.get() & 0xFF;

        // Byte 6: beat strengths 5-8
        int rawStrengths5_8 = buf.get() & 0xFF;

        // Bytes 9-10: subdivision constant -> steps
        buf.position(9);
        int subdivConst = buf.getShort() & 0xFFFF;
        int base        = 9352 + 2 * numerator;
        int subdivision = (base > 0) ? (subdivConst / base) + 1 : 1;
        if (subdivision < 1 || subdivision > 4) subdivision = 1;

        // Decode strengths
        int[] beatStrengths = new int[8];
        for (int slot = 0; slot < 4; slot++) {
            int wire = (rawStrengths1_4 >> (6 - slot * 2)) & 0x03;
            beatStrengths[slot] = wireToUiStrength(wire);
        }
        for (int slot = 0; slot < 4; slot++) {
            int wire = (rawStrengths5_8 >> (6 - slot * 2)) & 0x03;
            beatStrengths[4 + slot] = wireToUiStrength(wire);
        }

        // Persist back to SharedPreferences
        SharedPreferences.Editor editor = GBApplication
                .getDeviceSpecificSharedPrefs(getDevice().getAddress())
                .edit();
        editor.putInt(SoundbrennerConstants.PREF_BPM, (int) bpm);
        editor.putString(SoundbrennerConstants.PREF_TIME_SIGNATURE, timeSig);
        // PREF_SUBDIVISION is a ListPreference → must be stored as String.
        editor.putString(SoundbrennerConstants.PREF_SUBDIVISION, String.valueOf(subdivision));
        for (int i = 0; i < 8; i++) {
            editor.putInt(SoundbrennerConstants.PREF_BEATS[i], beatStrengths[i]);
        }
        editor.apply();

        LOG.info("Config notify from watch parsed: bpm={}, timeSig={}, subdiv={}", bpm, timeSig, subdivision);
    }

    /** Invert the wire encoding back to a UI strength value. */
    private int wireToUiStrength(int wire) {
        switch (wire & 0x03) {
            case 0b11: return 0;
            case 0b00: return 1;
            case 0b01: return 2;
            case 0b10: return 3;
            default:   return 2;
        }
    }

    /**
     * Persist the running flag so it survives reconnects. Goes through
     * GBDeviceEventUpdatePreferences (instead of writing SharedPreferences
     * directly) so that other listeners relying on this event are notified.
     */
    private void persistMetronomeRunning(boolean running) {
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                SoundbrennerConstants.PREF_METRONOME_RUNNING,
                running
        );
        evaluateGBDeviceEvent(eventUpdatePreferences);
    }
}
