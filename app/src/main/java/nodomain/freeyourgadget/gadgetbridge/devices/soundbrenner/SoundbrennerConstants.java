package nodomain.freeyourgadget.gadgetbridge.devices.soundbrenner;

import java.util.UUID;

/**
 * BLE UUIDs and protocol constants for the Soundbrenner Core.
 * Reverse-engineered using nRF Connect + HCI Snoop Log (May/June 2026).
 *
 * === 32-byte packet structure for UUID_CHARACTERISTIC_CONFIG ===
 *
 * Bytes  0-3:   BPM as IEEE 754 float, big-endian
 *
 * Byte   4:     Bytes 4: time signature
 *               Upper nibble = numerator - 1
 *               Lower nibble = log2(denominator)
 *               Examples: 4/4=0x32, 3/4=0x22, 3/8=0x23, 5/4=0x42
 *
 * Byte   5:     beat strengths beats 1-4, 2 bits per beat MSB-first
 * Byte   6:     beat strengths beats 5-8; unused slots = 0x00 (wire default)
 *               Inverted encoding: UI 0->11, 1->00, 2->01, 3->10
 *               Example (3:2:1:0) -> 10 01 00 11 = 0x93
 *
 * Byte   7:     unused, fixed 0x00
 *
 * Byte   8:     0x00 (fixed)
 *
 * Bytes  9-10:  Subdivision type constant, 16-bit big-endian.
 *               Formula: (subdivisionSteps - 1) * (9352 + 2 * numerator)
 *               4/4 examples: eighth=0x2490, triplet=0x4920, sixteenth=0x6DB0
 *               5/4 examples: eighth=0x2492, triplet=0x4924, sixteenth=0x6DB6
 *
 * Bytes 11-14:  0x00 (fixed)
 *
 * Bytes 15-(15+numerator-1): Beat pattern byte, one per beat.
 *               Bits 7-4 = active subdivision steps (bit 7=step1 .. bit 4=step4)
 *               Bits 3-0 = always 0000
 *               Examples: quarter=0x80, both eighths=0xC0, all triplets=0xE0,
 *                         all sixteenths=0xF0
 *
 * Bytes (15+numerator)-30:  0x00 (inactive beat slots)
 *
 * Byte  31:     0x00 (footer)
 */
public final class SoundbrennerConstants {

    private SoundbrennerConstants() {}

    // -------------------------------------------------------------------------
    // Services
    // -------------------------------------------------------------------------

    public static final UUID UUID_SERVICE_METRONOME =
            UUID.fromString("f3f6ce01-b257-4336-acd8-3010817837e4");
    public static final UUID UUID_SERVICE_VIBRATION =
            UUID.fromString("77ef8bb5-7d4d-4a66-bd64-3f93cc01ccda");
    /** Unknown service – retained for completeness, not yet used. */
    public static final UUID UUID_SERVICE_UNKNOWN_A =
            UUID.fromString("af7da518-f5f6-46d7-8ed1-546c33daf83b");
    /** Unknown service – retained for completeness, not yet used. */
    public static final UUID UUID_SERVICE_UNKNOWN_B =
            UUID.fromString("c8befab9-5cc3-4af1-9102-394c40987d1e");
    /** Unknown service – probably used for handshake. */
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_CTRL =
            UUID.fromString("061a5b49-8c3a-561e-9f34-dae895a3477a");
    //** Standard BLE Battery Service (0x180F / 0x2A19) */
    public static final UUID UUID_SERVICE_BATTERY =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    // -------------------------------------------------------------------------
    // Characteristics
    // -------------------------------------------------------------------------

    /**
     * READ + WRITE + NOTIFY.
     * Write: 32-byte configuration packet (BPM, time signature, beat strengths, subdivision).
     * Notify: device pushes the same packet when the user changes settings on the watch.
     */
    public static final UUID UUID_CHARACTERISTIC_CONFIG =
            UUID.fromString("5ae0ba81-1053-4183-826e-7a6be885c142");

    /**
     * WRITE + INDICATE.
     * Start metronome: {@link #CMD_METRONOME_START}.
     * Stop  metronome: {@link #CMD_METRONOME_STOP}.
     */
    public static final UUID UUID_CHARACTERISTIC_CONTROL =
            UUID.fromString("920475b8-17ee-4474-a500-35037152b860");

    /**
     * WRITE + INDICATE.
     * Continuous vibration – exact payload still under investigation.
     */
    public static final UUID UUID_CHARACTERISTIC_VIBRATION =
            UUID.fromString("252aeb30-c6ec-43ec-8072-5b08f13dae36");

    /**
     * READ + WRITE.
     * Unknown purpose; the official app reads this before every config write.
     */
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_RW =
            UUID.fromString("203b9729-4d3f-4898-8fb1-1ed1e6759229");

    /**
     * READ + WRITE + NOTIFY.
     * Unknown purpose; lives inside the metronome service.
     */
    public static final UUID UUID_CHARACTERISTIC_UNKNOWN_NOTIFY =
            UUID.fromString("05094f38-7d29-450d-8f23-c9d784923668");

    // -------------------------------------------------------------------------
    // Control commands
    // -------------------------------------------------------------------------

    public static final byte CMD_METRONOME_START = 0x01;
    public static final byte CMD_METRONOME_STOP  = 0x02;
    /** magic byte **/
    public static final byte[] CMD_PRE_START = new byte[]{ (byte) 0xd1 };

    // -------------------------------------------------------------------------
    // Packet layout constants
    // -------------------------------------------------------------------------

    public static final int  PACKET_SIZE         = 32;

    // -------------------------------------------------------------------------
    // Subdivision beat-pattern bytes
    // -------------------------------------------------------------------------

    /** Quarter note: only subdivision step 1 active. */
    public static final byte BEAT_PATTERN_QUARTER   = (byte) 0x80;
    /** Both eighth notes active. */
    public static final byte BEAT_PATTERN_EIGHTH    = (byte) 0xC0;
    /** All three triplet subdivisions active. */
    public static final byte BEAT_PATTERN_TRIPLET   = (byte) 0xE0;
    /** All four sixteenth-note subdivisions active. */
    public static final byte BEAT_PATTERN_SIXTEENTH = (byte) 0xF0;

    // -------------------------------------------------------------------------
    // Shared-preference keys
    // -------------------------------------------------------------------------

    public static final String PREF_SOUNDBRENNER_PREFIX = "soundbrenner_";
    public static final String PREF_BEAT_ACCENT_PREFIX  = "soundbrenner_beat_accent_";

    public static final String PREF_BPM            = "soundbrenner_bpm";
    public static final String PREF_TIME_SIGNATURE  = "soundbrenner_time_signature";
    public static final String PREF_SUBDIVISION     = "soundbrenner_subdivision";

    /** Stores the last known running/stopped state so the UI can restore it. */
    public static final String PREF_METRONOME_RUNNING = "soundbrenner_metronome_running";

    // Individual beat accent strengths (UI values 0-3); beats 1-8.
    public static final String PREF_BEAT_1 = "soundbrenner_beat_accent_1";
    public static final String PREF_BEAT_2 = "soundbrenner_beat_accent_2";
    public static final String PREF_BEAT_3 = "soundbrenner_beat_accent_3";
    public static final String PREF_BEAT_4 = "soundbrenner_beat_accent_4";
    public static final String PREF_BEAT_5 = "soundbrenner_beat_accent_5";
    public static final String PREF_BEAT_6 = "soundbrenner_beat_accent_6";
    public static final String PREF_BEAT_7 = "soundbrenner_beat_accent_7";
    public static final String PREF_BEAT_8 = "soundbrenner_beat_accent_8";
    public static final String PREF_BEAT_9 = "soundbrenner_beat_accent_9";
    public static final String PREF_BEAT_10 = "soundbrenner_beat_accent_10";
    public static final String PREF_BEAT_11 = "soundbrenner_beat_accent_11";
    public static final String PREF_BEAT_12 = "soundbrenner_beat_accent_12";
    public static final String PREF_BEAT_13 = "soundbrenner_beat_accent_13";
    public static final String PREF_BEAT_14 = "soundbrenner_beat_accent_14";
    public static final String PREF_BEAT_15 = "soundbrenner_beat_accent_15";
    public static final String PREF_BEAT_16 = "soundbrenner_beat_accent_16";

    /** Ordered list of all per-beat accent pref keys; index 0 = beat 1. */
    public static final String[] PREF_BEATS = {
        PREF_BEAT_1, PREF_BEAT_2, PREF_BEAT_3, PREF_BEAT_4,
        PREF_BEAT_5, PREF_BEAT_6, PREF_BEAT_7, PREF_BEAT_8,
        PREF_BEAT_9, PREF_BEAT_10, PREF_BEAT_11, PREF_BEAT_12,
        PREF_BEAT_13, PREF_BEAT_14, PREF_BEAT_15, PREF_BEAT_16
    };

    // -------------------------------------------------------------------------
    // Default values
    // -------------------------------------------------------------------------

    public static final int    DEFAULT_BPM          = 120;
    public static final String DEFAULT_TIME_SIG     = "4/4";
    public static final int    DEFAULT_SUBDIVISION   = 1;   // 1=quarter, 2=eighth, 3=triplet, 4=sixteenth
    public static final int    DEFAULT_BEAT_ACCENT_1 = 3;   // accent (loudest) on beat 1
    public static final int    DEFAULT_BEAT_ACCENT_N = 1;   // soft on all other beats
}
