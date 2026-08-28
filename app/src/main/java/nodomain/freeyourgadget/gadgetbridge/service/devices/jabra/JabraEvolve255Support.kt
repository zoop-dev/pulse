/*  Copyright (C) 2026 David Giron

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.jabra

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ACTIVE_NOISE_CANCELLING_TOGGLE
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUSYLIGHT
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BUSYLIGHT_ON_CALL
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_CALL_AUDIO_EQ
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND1
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND2
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND3
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND4
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_EQUALIZER_BAND5
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_SLEEP_MODE
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_HEADSET_GUIDANCE
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_BOOM_ARM_GUIDANCE
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_BUTTON_SOUNDS
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_VOICE_ASSISTANT
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_ANSWER_CALL_BOOM_ARM
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_AUTO_REJECT_CALL
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_MUTE_MIC_BOOM_ARM
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_MUTE_REMINDER
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_JABRA_SIDETONE_VOLUME
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointDevice
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointPairingActivity
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState
import nodomain.freeyourgadget.gadgetbridge.service.AbstractHeadphoneBTBRDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getParcelableCompat
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Support class for the Jabra Evolve2 55 headset (and potentially other Jabra headsets using the
 * same proprietary SPP/RFCOMM protocol).
 *
 * Protocol overview (analysed from btsnoop captures):
 *
 *   Host → Device:  04 09 <SEQ> <CMD>  <ID_HI> <ID_LO> [params...]
 *   Device → Host:  09 04 <SEQ> <RESP> <ID_HI> <ID_LO> [data...]
 *
 * SEQ: monotonically incrementing byte (0x00–0xFF, wraps).
 *
 * CMD (request types): the CMD byte is not a fixed opcode – it encodes a base value for the
 * operation category plus the number of bytes following ID_LO (`CMD = BASE + extraByteCount`):
 *   BASE 0x46  GET family – read a property by feature-ID
 *     0x46  GET       – no extra bytes
 *     0x47  GET_PARAM – + 1-byte param-index
 *     0x48  GET_IDX   – + 2-byte index
 *   BASE 0x86  SET family – write a value to a feature
 *     0x87  SET       – + 1-byte value (no param index)
 *     0x88  SET_PARAM – + param-index (1) + value (1)
 *     0x91  SET_STR   – + N ASCII bytes (variable-length string, e.g. device name); confirmed
 *                       from captures: an 11-byte name -> CMD 0x91, a 16-byte name -> CMD 0x96
 *   (SET_IDX, if it exists, would follow the same rule: BASE 0x86 + 2-byte index + data length.)
 *
 * RESP (response codes from device):
 *   0xC6  OK_EMPTY      – heartbeat acknowledged / no payload
 *   0xC7  OK_1B         – 1-byte payload
 *   0xC8  OK_2B         – 2-byte payload
 *   0xC9  OK_3B         – 3-byte payload
 *   0xCA  OK_4B         – 4-byte payload
 *   0xCB  ERROR         – unsupported / error (followed by error-code + echoed request)
 *   0xCC  OK_BLOB       – variable-length blob (length byte at payload[0])
 *   0xCD  OK_STR_LEN8   – 1-byte length prefix + ASCII string
 *   0xD4  OK_STR_NULL   – null-terminated ASCII string
 *   0xD7  OK_STR_LEN8   – 1-byte length prefix + ASCII string (≤ 16 bytes)
 *
 * Known feature IDs:
 *   0x0200  Product name        (string)
 *   0x0201  MAC address         (string, hex)
 *   0x0202  Device variant / type
 *   0x0203  Firmware version    (string)
 *   0x0208  Protocol version    (3 bytes: major.minor.patch)
 *   0x1308  Protocol sub-version
 *   0x131D  Serial number       (null-terminated string)
 *   0x1356  Device display name (string): GET (0x46) direct; response type 0xD1, ASCII payload
 *           with no length prefix or terminator (delimited by the next frame marker). SET uses
 *           a variable-length CMD byte = 0x86 + payload length (e.g. 0x91 for an 11-byte name,
 *           0x96 for a 16-byte name) – NOT the fixed 0x87 used by other direct-value SETs.
 *   0x13BE  ANC (Active Noise Cancellation):
 *             param 0x00 – 5-byte state blob (bytes 0-4)
 *             param 0x01 – current mode  (0x01=off, 0x04=full ANC)
 *             param 0x03 – transparency level
 *             param 0x05 – unknown
 *   0x131E  Mute reminder tone: SET (0x87) value 0x14 = enabled, 0x00 = disabled.
 *   0x133C  Auto-reject call while busy: SET (0x87) value 0x01 = enabled, 0x00 = disabled.
 *   0x137C  Sidetone: SET_PARAM (0x88), paramIdx 0x00 = enabled, 0x01 = disabled; value byte
 *           encodes the volume level (0-5) as a signed offset: value = (volume - 3) * 3, e.g.
 *           volume 0 → 0xF7 (-9), volume 3 → 0x00, volume 5 → 0x06 (+6). GET responses (OK_2B)
 *           mirror this: payload[0] = last paramIdx (0x00=on/0x01=off), payload[1] = volume
 *           encoded the same way.
 *   0x137D  EQ band data (5-band parametric EQ, raw blob)
 *   0x1313  Active EQ preset
 *   0x1398  Boom-arm actions bitmask: SET (0x87), bit0 (0x01) = auto-mute mic, bit1 (0x02) =
 *           auto-answer call. Writes must read-modify-write the full byte to avoid clobbering
 *           the other bit.
 *   0x1315  Call audio EQ: SET (0x87) direct, no param index; GET (0x46) direct.
 *           value 0x00 = neutral, 0x01 = bass, 0x02 = treble.
 *   0x1390  Sleep mode timeout: SET (0x87) direct; value = minutes / 5 (0 = off).
 *   0x133A  Headset guidance: SET (0x87) direct; value 0x00 = tone, 0x01 = voice, 0x02 = none.
 *   0x13BC  Boom arm guidance: SET (0x87) direct; value 0x00 = tone, 0x01 = voice, 0x02 = none.
 *   0x133F  Headset button sounds: SET (0x87) direct; value 0x00 = off, 0xFF = on.
 *   0xFC00  Heartbeat / keepalive
 */
class JabraEvolve255Support : AbstractHeadphoneBTBRDeviceSupport(LOG) {

    companion object {
        private val LOG = LoggerFactory.getLogger(JabraEvolve255Support::class.java)

        /** Serial Port Profile UUID used by Jabra for its proprietary RFCOMM channel. */
        private val UUID_JABRA_SERVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ── Command type base values ──────────────────────────────────────────
        // CMD = BASE + number of bytes following ID_LO (see class doc comment for details).
        private const val CMD_GET_BASE: Int = 0x46   // GET family (GET / GET_PARAM / GET_IDX)
        private const val CMD_SET_BASE: Int = 0x86   // SET family (SET / SET_PARAM / SET_STR / SET_IDX)

        // ── Feature / property IDs ────────────────────────────────────────────
        private const val FEAT_PRODUCT_NAME: Int      = 0x0200
        private const val FEAT_MAC_ADDRESS: Int       = 0x0201
        private const val FEAT_FIRMWARE_VERSION: Int  = 0x0203
        private const val FEAT_PRODUCT_ID: Int        = 0x0211   // little-endian uint16, e.g. 9461
        private const val FEAT_PROTOCOL_VERSION: Int  = 0x0208
        private const val FEAT_PROTO_SUBVERSION: Int  = 0x1308   // little-endian uint16, FW build/revision
        private const val FEAT_SERIAL_NUMBER: Int     = 0x131D
        private const val FEAT_DEVICE_NAME: Int       = 0x1356   // string, SET (0x91)/GET (0x46) direct

        // ── Multipoint / paired-device enumeration ────────────────────────────
        // The paired-device list is enumerated by walking two features in parallel with GET_IDX
        // (0x48), chained by a 2-byte "next index" at the start of each response payload
        // (0xFFFF = end of list). Responses use the linear length encoding where the response
        // type byte equals 0xC6 + payloadLength (e.g. 0xD6 = 16-byte metadata payload).
        //   0x0D28  paired-device metadata: <nextIdx(2)> <?, ?, ?, connFlag> <MAC(6)> <trailer(4)>
        //   0x0D32  paired-device name:     <nextIdx(2)> <ASCII name...>  (empty name = 2 bytes)
        private const val FEAT_PAIRED_DEVICE_META: Int = 0x0D28
        private const val FEAT_PAIRED_DEVICE_NAME: Int = 0x0D32
        private const val FEAT_VOICE_ASSISTANT: Int    = 0x0D4D   // SET_PARAM/GET_PARAM paramIdx 0x03: 0x00=mobile, 0x10=Alexa
        private const val MULTIPOINT_IDX_END: Int      = 0xFFFF

        private const val FEAT_BATTERY: Int           = 0x1202   // OK_4B: [flags, level%, ?, ?]
        private const val FEAT_ANC: Int               = 0x13BE
        private const val FEAT_BUSYLIGHT: Int          = 0x1208   // LED indicator; SET_PARAM paramIdx=0x01/0x00 encodes on/off
        private const val FEAT_BOOM_ARM_ACTIONS: Int   = 0x1398
        private const val FEAT_BUSYLIGHT_ON_CALL: Int  = 0x1339
        private const val FEAT_AUTO_REJECT_CALL: Int   = 0x133C   // SET direct: 0x01 = on, 0x00 = off
        private const val FEAT_MUTE_REMINDER: Int      = 0x131E   // SET direct: 0x14 = on, 0x00 = off
        private const val FEAT_SIDETONE: Int           = 0x137C   // SET_PARAM: paramIdx 0x00 = on, 0x01 = off; value = (volume-3)*3
        private const val FEAT_CALL_AUDIO_EQ: Int      = 0x1315   // SET/GET direct: 0x00=neutral, 0x01=bass, 0x02=treble
        private const val FEAT_EQUALIZER_MODE: Int     = 0x137E   // SET/GET direct: 0x00=flat/neutral, 0x01=custom curve active
        private const val FEAT_EQUALIZER_CURVE: Int    = 0x137D   // SET/GET param 0x00: 5-band custom curve (see buildEqualizerCurve)
        private const val FEAT_SLEEP_MODE: Int         = 0x1390   // SET/GET direct: minutes/5 (0=off)
        private const val FEAT_HEADSET_GUIDANCE: Int   = 0x133A   // SET/GET direct: 0x00=tone, 0x01=voice, 0x02=none
        private const val FEAT_BOOM_ARM_GUIDANCE: Int  = 0x13BC   // SET/GET direct: 0x00=tone, 0x01=voice, 0x02=none
        private const val FEAT_BUTTON_SOUNDS: Int      = 0x133F   // SET/GET direct: 0x00=off, 0xFF=on

        // ── Call audio EQ preference values (stored in SharedPreferences as strings) ─────────
        const val CALL_AUDIO_EQ_NEUTRAL: String = "neutral"
        const val CALL_AUDIO_EQ_TREBLE: String  = "treble"
        const val CALL_AUDIO_EQ_BASS: String    = "bass"

        // ── Equalizer mode preference values (stored in SharedPreferences as strings) ────────
        // EQ_NEUTRAL disables the custom curve (flat/off). The named presets send a fixed captured
        // curve. EQ_CUSTOM enables the 5-band curve built from the per-band slider preferences.
        const val EQ_NEUTRAL: String  = "neutral"
        const val EQ_SPEECH: String   = "speech"
        const val EQ_BASS: String     = "bass"
        const val EQ_TREBLE: String   = "treble"
        const val EQ_SMOOTH: String   = "smooth"
        const val EQ_ENERGIZE: String = "energize"
        const val EQ_CUSTOM: String   = "custom"

        // Fixed 5-band gains (60 Hz, 250 Hz, 1 kHz, 4 kHz, 7.6 kHz) for the named presets, as the
        // raw signed 16-bit big-endian values observed in captures (byte-exact with the app).
        private val EQUALIZER_PRESETS: Map<String, IntArray> = mapOf(
            EQ_NEUTRAL  to intArrayOf(0x0000, 0x0000, 0x0000, 0x0000, 0x0000),
            EQ_SPEECH   to intArrayOf(0xFEF2, 0x0000, 0x0000, 0x003D, 0xFF88),
            EQ_BASS     to intArrayOf(0x010E, 0xFF8A, 0x0000, 0x0000, 0x0000),
            EQ_TREBLE   to intArrayOf(0xFFA6, 0xFF8A, 0x0000, 0x0076, 0x00F0),
            EQ_SMOOTH   to intArrayOf(0x0000, 0x0076, 0x0000, 0xFF8A, 0x0000),
            EQ_ENERGIZE to intArrayOf(0x00B4, 0x0000, 0xFF8A, 0xFFC3, 0x00B4),
        )

        // Keys of the 5 per-band slider preferences, ordered 60 Hz .. 7.6 kHz.
        private val EQ_BAND_KEYS: Array<String> = arrayOf(
            PREF_JABRA_EQUALIZER_BAND1,
            PREF_JABRA_EQUALIZER_BAND2,
            PREF_JABRA_EQUALIZER_BAND3,
            PREF_JABRA_EQUALIZER_BAND4,
            PREF_JABRA_EQUALIZER_BAND5,
        )

        // Slider centre (0 dB). Sliders range 0..12; centre 6 = flat, ±6 = full boost/cut.
        private const val EQ_BAND_CENTER: Int = 6

        // Maximum raw signed-16-bit gain per band (device clamps symmetrically). A slider at its
        // extreme (0 or 12) maps to ∓/± these values; the centre maps to 0. See buildEqualizerCurve.
        private val EQ_BAND_MAX_RAW: IntArray = intArrayOf(450, 360, 360, 360, 600)

        // ── Guidance preference values (shared by headset/boom-arm guidance settings) ─────────
        const val GUIDANCE_VOICE: String = "voice"
        const val GUIDANCE_TONE: String  = "tone"
        const val GUIDANCE_NONE: String  = "none"

        // ── Voice assistant preference values (stored in SharedPreferences as strings) ────────
        const val VOICE_ASSISTANT_MOBILE: String = "mobile"
        const val VOICE_ASSISTANT_ALEXA: String  = "alexa"

        /**
         * General-purpose device-initiated notification bus (Jabra calls it "timer/counter",
         * feature 0x0D4C). The device sends a push on this feature whenever internal state
         * changes. Known sub-commands (payload[0]):
         *   0x09 – ANC mode changed; poll [FEAT_ANC] for current state.
         *   0x08 – Busylight (manual) changed; payload[2] = 0x01/0x00.
         *   0x04 – Busylight on-call changed; poll [FEAT_BUSYLIGHT_ON_CALL] for current state.
         */
        private const val FEAT_TIMER_COUNTER: Int     = 0x0D4C
        private const val FEAT_HEARTBEAT: Int         = 0xFC00

        // ── ANC parameter / value constants ──────────────────────────────────
        /** Param index for the current ANC mode inside feature 0x13BE. */
        private const val ANC_PARAM_MODE: Byte  = 0x01
        /** Raw value sent/received for ANC fully enabled. */
        private const val ANC_VALUE_ON: Int     = 0x04
        /** Raw value sent/received for ANC off / passthrough. */
        private const val ANC_VALUE_OFF: Int    = 0x01

        // ── Response type codes ───────────────────────────────────────────────
        private const val RESP_OK_EMPTY: Int   = 0xC6
        private const val RESP_OK_1B: Int      = 0xC7
        private const val RESP_OK_2B: Int      = 0xC8
        private const val RESP_OK_3B: Int      = 0xC9
        private const val RESP_OK_4B: Int      = 0xCA
        private const val RESP_ERROR: Int      = 0xCB
        private const val RESP_OK_BLOB: Int    = 0xCC   // payload[0] = length
        private const val RESP_STR_LEN: Int    = 0xCD   // 1-byte length prefix
        private const val RESP_STR_NULL: Int   = 0xD4   // null-terminated
        private const val RESP_STR_LEN2: Int   = 0xD7   // 1-byte length prefix (≤16)
        private const val RESP_STR_NAME: Int   = 0xD1   // device name: null-terminated ASCII (same framing as RESP_STR_NULL)

        /** Interval between keepalive pings (ms). Observed ~14 s in captures. */
        private const val KEEPALIVE_INTERVAL_MS = 15_000L
    }

    // Monotonically incrementing sequence counter, 0–255 wrapping.
    private val seqCounter = AtomicInteger(1)

    // Runs the periodic keepalive pings on the main thread.
    private val keepaliveHandler = Handler(Looper.getMainLooper())

    // Cached version fields – populated incrementally as responses arrive.
    private var cachedFwVersion: String? = null
    private var cachedFwVersion2: String? = null
    private var cachedHwVersion: String? = null

    // Buffers bytes across onSocketRead calls: the Bluetooth stack may coalesce several
    // response frames into a single read, or split one frame across multiple reads. Frames
    // are extracted from here as soon as they're complete.
    private var pendingBuffer: ByteArray = ByteArray(0)

    // ── Multipoint enumeration state ──────────────────────────────────────────
    // Accumulates paired-device entries as the chained GET_IDX responses arrive. Keyed by the
    // requested index so metadata (0x0D28) and name (0x0D32) responses for the same index merge.
    private val multipointEntries = linkedMapOf<Int, MutableMultipointEntry>()
    // Index currently being requested while walking the chained list.
    private var multipointReqIdx = 0
    // "Next index" reported by the most recent metadata response (0xFFFF = end of list).
    private var multipointNextIdx = MULTIPOINT_IDX_END
    // Whether the multipoint broadcast receiver has been registered (guards double-registration).
    private var multipointReceiverRegistered = false

    /** Mutable accumulator merging the metadata and name halves of one paired-device entry. */
    private class MutableMultipointEntry(
        var address: String = "",
        var name: String? = null,
        var connected: Boolean = false,
    )

    init {
        addSupportedService(UUID_JABRA_SERVICE)
    }

    override fun useAutoConnect(): Boolean = true

    override fun setContext(gbDevice: GBDevice, btAdapter: BluetoothAdapter, context: Context) {
        super.setContext(gbDevice, btAdapter, context)
        registerMultipointReceiver()
    }

    // ── Connection lifecycle ──────────────────────────────────────────────────

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        LOG.info("Initializing Jabra Evolve2 55")
        pendingBuffer = ByteArray(0)
        builder.setDeviceState(GBDevice.State.INITIALIZING)

        // Device info
        builder.write(*buildGet(FEAT_PRODUCT_NAME))
        builder.write(*buildGet(FEAT_PRODUCT_ID))
        builder.write(*buildGet(FEAT_FIRMWARE_VERSION))
        builder.write(*buildGet(FEAT_PROTO_SUBVERSION))
        builder.write(*buildGet(FEAT_SERIAL_NUMBER))
        builder.write(*buildGet(FEAT_DEVICE_NAME))

        // Device state
        builder.write(*buildGet(FEAT_BATTERY))
        builder.write(*buildGet(FEAT_ANC, ANC_PARAM_MODE))
        builder.write(*buildGet(FEAT_BUSYLIGHT))
        builder.write(*buildGet(FEAT_BUSYLIGHT_ON_CALL))
        builder.write(*buildGet(FEAT_BOOM_ARM_ACTIONS))
        builder.write(*buildGet(FEAT_AUTO_REJECT_CALL))
        builder.write(*buildGet(FEAT_MUTE_REMINDER))
        builder.write(*buildGet(FEAT_SIDETONE))
        builder.write(*buildGet(FEAT_CALL_AUDIO_EQ))
        builder.write(*buildGet(FEAT_EQUALIZER_MODE))
        builder.write(*buildGet(FEAT_EQUALIZER_CURVE, 0x00))
        builder.write(*buildGet(FEAT_SLEEP_MODE))
        builder.write(*buildGet(FEAT_HEADSET_GUIDANCE))
        builder.write(*buildGet(FEAT_BOOM_ARM_GUIDANCE))
        builder.write(*buildGet(FEAT_BUTTON_SOUNDS))
        builder.write(*buildGet(FEAT_VOICE_ASSISTANT, 0x03))

        // Subscribe to the device-initiated notification bus (0x0D4C) so the headset pushes
        // state changes (e.g. ANC toggled via the physical button). Without this the device
        // never emits the pushes handled by handleTimerCounterNotify. The subscription is a
        // 4-byte bitmask register: the official app writes it three times with growing masks
        // (0x00000000 -> 0x00000010 -> 0x00000210), and only the non-zero masks actually enable
        // the push categories (a bare 0x00000000 leaves every category disabled). Replicate the
        // full sequence so the final effective mask (0x00000210) is applied.
        builder.write(*buildNotifySubscribe(0x00000000))
        builder.write(*buildNotifySubscribe(0x00000010))
        builder.write(*buildNotifySubscribe(0x00000210))

        builder.setDeviceState(GBDevice.State.INITIALIZED)

        // Schedule keepalive after all init writes have been dispatched.
        keepaliveHandler.removeCallbacks(keepaliveRunnable)
        keepaliveHandler.postDelayed(keepaliveRunnable, KEEPALIVE_INTERVAL_MS)

        return builder
    }

    // ── Incoming data ─────────────────────────────────────────────────────────

    /**
     * Handles raw bytes read from the socket. A single read may contain several coalesced
     * response frames (or only part of one), so incoming bytes are appended to [pendingBuffer]
     * and as many complete frames as are available are extracted and dispatched.
     */
    override fun onSocketRead(data: ByteArray) {
        pendingBuffer += data

        while (pendingBuffer.isNotEmpty()) {
            if (pendingBuffer[0] != 0x09.toByte() || (pendingBuffer.size > 1 && pendingBuffer[1] != 0x04.toByte())) {
                // Not aligned on a frame boundary (or leading garbage) – resync by scanning
                // for the next `09 04` marker.
                val markerIdx = findFrameMarker(pendingBuffer, 1)
                if (markerIdx < 0) {
                    LOG.debug("Discarding {} bytes without a valid frame marker", pendingBuffer.size)
                    pendingBuffer = ByteArray(0)
                    break
                }
                LOG.debug("Resyncing: discarding {} leading bytes without a valid frame marker", markerIdx)
                pendingBuffer = pendingBuffer.copyOfRange(markerIdx, pendingBuffer.size)
                continue
            }

            val frameLength = frameLength(pendingBuffer) ?: break // frame incomplete, wait for more data
            if (pendingBuffer.size < frameLength) break // frame incomplete, wait for more data

            val frame = pendingBuffer.copyOfRange(0, frameLength)
            pendingBuffer = pendingBuffer.copyOfRange(frameLength, pendingBuffer.size)
            parseResponse(frame)
        }
    }

    /** Finds the next `09 04` frame marker in [buf] at or after [fromIndex], or -1 if none. */
    private fun findFrameMarker(buf: ByteArray, fromIndex: Int): Int {
        for (i in fromIndex until buf.size - 1) {
            if (buf[i] == 0x09.toByte() && buf[i + 1] == 0x04.toByte()) return i
        }
        return -1
    }

    /**
     * Computes the total length (header + payload) of the frame starting at index 0 of [buf],
     * based on the response type byte at index 3. Returns null if there isn't yet enough data
     * to determine the length (e.g. a length-prefixed/null-terminated payload whose prefix or
     * terminator hasn't arrived yet).
     */
    private fun frameLength(buf: ByteArray): Int? {
        if (buf.size < 6) return null
        val responseType = buf[3].toInt() and 0xFF
        val featureId    = ((buf[4].toInt() and 0xFF) shl 8) or (buf[5].toInt() and 0xFF)

        // Paired-device enumeration (0x0D28 / 0x0D32) uses the linear length encoding: the
        // response type byte directly encodes the payload length as (responseType - 0xC6).
        // Handle these two features explicitly so the generic string/blob rules below (which
        // interpret e.g. 0xD7 as length-prefixed) don't mis-frame them. Errors (0xCB) fall
        // through to the shared handling.
        if ((featureId == FEAT_PAIRED_DEVICE_META || featureId == FEAT_PAIRED_DEVICE_NAME) &&
            responseType != RESP_ERROR && responseType >= RESP_OK_EMPTY) {
            return 6 + (responseType - RESP_OK_EMPTY)
        }

        // Device-initiated notifications on the timer/counter bus (0x0D4C) don't use the
        // standard response codes (>= 0xC6). Instead byte 3 encodes the *total* frame length
        // directly (e.g. 0x09 -> 9-byte frame `09 04 SEQ 09 0D 4C 09 01 04`, 0x08 -> 8 bytes).
        // Without this, byte3 falls through to the generic rules below (assumed 0-length
        // payload), truncating the push and dropping its data.
        if (featureId == FEAT_TIMER_COUNTER && responseType in 6 until RESP_OK_EMPTY) {
            return responseType
        }

        val payloadLength = when (responseType) {
            RESP_OK_EMPTY, RESP_ERROR                           -> 0
            RESP_OK_1B                                          -> 1
            RESP_OK_2B                                          -> 2
            RESP_OK_3B                                          -> 3
            RESP_OK_4B                                          -> 4
            RESP_OK_BLOB, RESP_STR_LEN, RESP_STR_LEN2, 0xD3     -> {
                if (buf.size < 7) return null // length prefix byte not yet available
                (buf[6].toInt() and 0xFF) + 1
            }
            RESP_STR_NULL                                        -> {
                val nulIdx = (6 until buf.size).firstOrNull { buf[it] == 0.toByte() } ?: return null
                (nulIdx - 6) + 1
            }
            RESP_STR_NAME                                        -> {
                // No length prefix and no terminator – the device just sends the raw ASCII
                // name bytes and stops. Delimit by the start of the next frame instead of
                // scanning for a null byte (which could belong to the *next* frame's header
                // and would corrupt the name with unrelated bytes).
                val nextMarker = findFrameMarker(buf, 6)
                if (nextMarker < 0) return null // wait for more data (or the next frame)
                nextMarker - 6
            }
            else -> 0 // unknown response type, assume no payload
        }
        return 6 + payloadLength
    }

    private fun parseResponse(data: ByteArray) {
        val responseType = data[3].toInt() and 0xFF
        val featureId    = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val payload      = if (data.size > 6) data.copyOfRange(6, data.size) else ByteArray(0)

        LOG.trace("Response: type=0x{}, feature=0x{}, payload={} bytes",
            Integer.toHexString(responseType), Integer.toHexString(featureId), payload.size)

        if (responseType == RESP_ERROR) {
            LOG.debug("Device returned error/unsupported for feature 0x{}", Integer.toHexString(featureId))
            return
        }

        when (featureId) {
            FEAT_HEARTBEAT        -> LOG.trace("Heartbeat acknowledged")
            FEAT_PRODUCT_NAME     -> handleProductName(responseType, payload)
            FEAT_PRODUCT_ID       -> handleProductId(payload)
            FEAT_FIRMWARE_VERSION -> handleFirmwareVersion(responseType, payload)
            FEAT_PROTO_SUBVERSION -> handleProtoSubversion(payload)
            FEAT_SERIAL_NUMBER    -> handleSerialNumber(responseType, payload)
            FEAT_DEVICE_NAME      -> handleDeviceNameState(responseType, payload)
            FEAT_BATTERY          -> handleBattery(payload)
            FEAT_ANC              -> handleAncState(payload)
            FEAT_BUSYLIGHT        -> handleBusylightManual(payload)
            FEAT_BUSYLIGHT_ON_CALL -> handleBusylightOnCallState(payload)
            FEAT_BOOM_ARM_ACTIONS -> handleBoomArmActionsState(payload)
            FEAT_AUTO_REJECT_CALL -> handleAutoRejectCallState(payload)
            FEAT_MUTE_REMINDER    -> handleMuteReminderState(payload)
            FEAT_SIDETONE         -> handleSidetoneState(payload)
            FEAT_CALL_AUDIO_EQ    -> handleCallAudioEqState(payload)
            FEAT_EQUALIZER_CURVE  -> handleEqualizerCurveState(payload)
            FEAT_SLEEP_MODE       -> handleSleepModeState(payload)
            FEAT_HEADSET_GUIDANCE -> handleHeadsetGuidanceState(payload)
            FEAT_BOOM_ARM_GUIDANCE -> handleBoomArmGuidanceState(payload)
            FEAT_BUTTON_SOUNDS    -> handleButtonSoundsState(payload)
            FEAT_VOICE_ASSISTANT  -> handleVoiceAssistantState(payload)
            FEAT_PAIRED_DEVICE_META -> handlePairedDeviceMeta(payload)
            FEAT_PAIRED_DEVICE_NAME -> handlePairedDeviceName(payload)
            FEAT_TIMER_COUNTER    -> handleTimerCounterNotify(payload)
        }
    }

    override fun dispose() {
        synchronized(ConnectionMonitor) {
            keepaliveHandler.removeCallbacks(keepaliveRunnable)
            if (multipointReceiverRegistered) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(multipointReceiver)
                multipointReceiverRegistered = false
            }
            super.dispose()
        }
    }

    // ── Response handlers ─────────────────────────────────────────────────────

    private fun handleProductName(responseType: Int, payload: ByteArray) {
        val name = extractString(responseType, payload) ?: return
        LOG.info("Product name: {}", name)
    }

    private fun handleProductId(payload: ByteArray) : String? {
        if (payload.size < 2) return null
        // Feature 0x0211: little-endian uint16 product ID (e.g. 9461 = 0x24F5, raw bytes: f5 24)
        val productId = (payload[0].toInt() and 0xFF) or ((payload[1].toInt() and 0xFF) shl 8)
        LOG.info("Product ID: {}", productId)
        cachedHwVersion = productId.toString()
        dispatchVersionInfo()
        return productId.toString()
    }

    private fun handleFirmwareVersion(responseType: Int, payload: ByteArray) : String? {
        val version = extractString(responseType, payload) ?: return null
        LOG.info("Firmware version: {}", version)
        cachedFwVersion = version
        dispatchVersionInfo()
        return version
    }

    /**
     * Handles the device display name response (feature 0x1356). Reflected back to the app's
     * generic device-name preference so the current name is shown/edited via the standard
     * device-name setting.
     */
    private fun handleDeviceNameState(responseType: Int, payload: ByteArray) {
        val name = extractString(responseType, payload) ?: return
        LOG.info("Device name: {}", name)
        evaluateGBDeviceEvent(
            GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME, name)
        )
    }

    /**
     * Handles the serial number response (feature 0x131D). Surfaced as a device-info item in the
     * device details panel. Response is a null-terminated ASCII string, e.g. "12345-678-899".
     */
    private fun handleSerialNumber(responseType: Int, payload: ByteArray) {
        val serial = extractString(responseType, payload) ?: return
        LOG.info("Serial number: {}", serial)
        handleGBDeviceEvent(GBDeviceEventUpdateDeviceInfo("SERIAL: ", serial))
    }

    private fun handleProtoSubversion(payload: ByteArray) : String? {
        if (payload.size < 2) return null
        // Feature 0x1308: little-endian uint16 firmware build/revision (e.g. 0x0409 = 1033)
        val revision = (payload[0].toInt() and 0xFF) or ((payload[1].toInt() and 0xFF) shl 8)
        LOG.info("Firmware revision: {}", revision)
        cachedFwVersion2 = revision.toString()
        dispatchVersionInfo()
        return revision.toString()
    }

    /**
     * Builds a [GBDeviceEventVersionInfo] from the supplied strings, mirroring the
     * Soundcore helper convention. [fwVersion2] is suppressed when it equals [fwVersion].
     * Any null argument leaves the corresponding device field untouched in [evaluate].
     */
    private fun buildVersionInfo(
        fwVersion: String?,
        fwVersion2: String?,
        hwVersion: String?,
    ): GBDeviceEventVersionInfo = GBDeviceEventVersionInfo().also { info ->
        info.fwVersion  = fwVersion
        info.fwVersion2 = if (fwVersion2 != null && fwVersion2 == fwVersion) null else fwVersion2
        info.hwVersion  = hwVersion
    }

    /**
     * Dispatches a version event built from the currently cached fields.
     * Called every time any cached field is updated so the UI stays current.
     */
    private fun dispatchVersionInfo() {
        handleGBDeviceEvent(buildVersionInfo(cachedFwVersion, cachedFwVersion2, cachedHwVersion))
    }

    /**
     * Parses battery info from feature 0x1202 (OK_4B, 4 bytes).
     *
     * Observed payload: [flags, level%, byte2, byte3]
     *   byte[0] = 0x04 – status flags (charging state TBD)
     *   byte[1] = battery percentage (0–100)
     *   byte[2], byte[3] – unknown auxiliary bytes
     */
    private fun handleBattery(payload: ByteArray) {
        if (payload.size < 2) return
        val level = payload[1].toInt() and 0xFF
        if (level > 100) {
            LOG.warn("Unexpected battery level byte: 0x{}", Integer.toHexString(level))
            return
        }
        LOG.info("Battery level: {}%", level)
        val event = GBDeviceEventBatteryInfo()
        event.level = level
        event.state = BatteryState.BATTERY_NORMAL
        handleGBDeviceEvent(event)
    }

    /**
     * Handles a device-initiated notification on feature 0x0D4C (timer/counter bus).
     *
     * The device uses this feature to push state-change events. Known sub-commands
     * (payload[0]):
     *   0x09 – ANC mode changed by the user pressing the headset button.
     *
     * Rather than trusting the pushed value (which mirrors what the official Jabra app does),
     * we immediately poll [FEAT_ANC] param=0x01 so the confirmed current state is returned
     * and processed by the normal [handleAncState] path.
     */
    private fun handleTimerCounterNotify(payload: ByteArray) {
        if (payload.isEmpty()) return
        when (payload[0].toInt() and 0xFF) {
            0x09 -> {
                // Push layout: [0x09, 0x01, <ancRawValue>]. payload[2] already carries the new
                // mode (0x04 = ANC on, 0x01 = off), so update the UI directly for an immediate
                // reflection, then poll to confirm the authoritative state.
                if (payload.size >= 3) {
                    val enabled = (payload[2].toInt() and 0xFF) == ANC_VALUE_ON
                    LOG.debug("ANC state-change notification from headset: {}", if (enabled) "ON" else "OFF")
                    handleGBDeviceEvent(
                        GBDeviceEventUpdatePreferences(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, enabled)
                    )
                }
                try {
                    createTransactionBuilder("jabra-poll-anc")
                        .write(*buildGet(FEAT_ANC, ANC_PARAM_MODE))
                        .queue()
                } catch (e: Exception) {
                    LOG.warn("Failed to send ANC poll after notification", e)
                }
            }
            0x08 -> {
                if (payload.size >= 3) {
                    val enabled = (payload[2].toInt() and 0xFF) == 0x01
                    LOG.debug("Busylight state-change notification from headset: {}", if (enabled) "ON" else "OFF")
                    handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_BUSYLIGHT, enabled))
                }
            }
            0x04 -> {
                LOG.debug("Busylight on-call state-change notification; polling current state")
                try {
                    createTransactionBuilder("jabra-poll-busylight-on-call")
                        .write(*buildGet(FEAT_BUSYLIGHT_ON_CALL))
                        .queue()
                } catch (e: Exception) {
                    LOG.warn("Failed to send busylight-on-call poll after notification", e)
                }
            }
            else -> LOG.debug("Timer/counter notification sub=0x{} (ignored)",
                Integer.toHexString(payload[0].toInt() and 0xFF))
        }
    }

    /**
     * Parses the ANC state from a response to feature 0x13BE.
     *
     * Two response shapes are seen from the device:
     *   - Response to GET_PARAM param=0x01 (0xC7): 1 byte – the raw mode value.
     *   - Response to GET_PARAM param=0x00 (0xCC): ≥6 bytes blob; mode byte at [3].
     *
     * Mode values:  0x01 = off/passthrough,  0x04 = full ANC.
     */
    private fun handleAncState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val modeValue = when {
            payload.size == 1 -> payload[0].toInt() and 0xFF
            payload.size >= 6 -> payload[3].toInt() and 0xFF  // blob: length + 5 bytes, mode at [3]
            else -> return
        }
        val enabled = modeValue == ANC_VALUE_ON
        LOG.info("ANC state: {} (raw=0x{})", if (enabled) "ON" else "OFF",
            Integer.toHexString(modeValue))
        handleGBDeviceEvent(
            GBDeviceEventUpdatePreferences(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, enabled)
        )
    }

    /**
     * Parses busylight (manual LED) state from a response to feature 0x1208.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = off, 0x01 = on.
     */
    private fun handleBusylightManual(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) == 0x01
        LOG.info("Busylight: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_BUSYLIGHT, enabled))
    }

    /**
     * Parses busylight-on-call state from a response to feature 0x1339.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = off, 0x01 = on.
     */
    private fun handleBusylightOnCallState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) == 0x01
        LOG.info("Busylight on call: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(
            GBDeviceEventUpdatePreferences(PREF_BUSYLIGHT_ON_CALL, enabled)
        )
    }

    /**
     * Parses the boom-arm actions bitmask from a response to feature 0x1398.
     *
     * Response shape (GET → OK_1B): 1 byte – bit0 (0x01) = auto-mute mic, bit1 (0x02) =
     * auto-answer call.
     */
    private fun handleBoomArmActionsState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val raw = payload[0].toInt() and 0xFF
        val answerCallEnabled = (raw and 0x02) != 0
        val muteMicEnabled = (raw and 0x01) != 0
        LOG.info("Boom arm actions: answer-call={}, mute-mic={} (raw=0x{})",
            answerCallEnabled, muteMicEnabled, Integer.toHexString(raw))
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_ANSWER_CALL_BOOM_ARM, answerCallEnabled))
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_MUTE_MIC_BOOM_ARM, muteMicEnabled))
    }

    /**
     * Parses auto-reject-call state from a response to feature 0x133C.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = off, 0x01 = on.
     */
    private fun handleAutoRejectCallState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) == 0x01
        LOG.info("Auto-reject call: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_AUTO_REJECT_CALL, enabled))
    }

    /**
     * Parses mute-reminder state from a response to feature 0x131E.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = off, non-zero (e.g. 0x14) = on.
     */
    private fun handleMuteReminderState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) != 0x00
        LOG.info("Mute reminder: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_MUTE_REMINDER, enabled))
    }

    /**
     * Parses sidetone state from a response to feature 0x137C.
     *
     * Response shape (GET → OK_2B): 2-byte payload.
     *   payload[0] mirrors the paramIdx used in the last SET_PARAM: 0x00 = enabled, 0x01 =
     *   disabled (i.e. the opposite of a plain boolean read).
     *   payload[1] is the volume level (0-5), encoded as a signed offset: value = (volume-3)*3.
     */
    private fun handleSidetoneState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) == 0x00
        LOG.info("Sidetone: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_SIDETONE, enabled))
        if (payload.size >= 2) {
            val volume = (payload[1].toInt() / 3) + 3
            LOG.info("Sidetone volume: {}", volume)
            handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_SIDETONE_VOLUME, volume))
        }
    }

    /**
     * Parses the call audio EQ setting from a response to feature 0x1315.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = neutral, 0x01 = bass, 0x02 = treble.
     */
    private fun handleCallAudioEqState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val value = when (payload[0].toInt() and 0xFF) {
            0x01 -> CALL_AUDIO_EQ_BASS
            0x02 -> CALL_AUDIO_EQ_TREBLE
            else -> CALL_AUDIO_EQ_NEUTRAL
        }
        LOG.info("Call audio EQ: {}", value)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_CALL_AUDIO_EQ, value))
    }

    /**
     * Parses the sleep mode timeout from a response to feature 0x1390.
     *
     * Response shape (GET → OK_1B): 1 byte, encoded as minutes / 5 (0 = off).
     */
    private fun handleSleepModeState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val minutes = (payload[0].toInt() and 0xFF) * 5
        LOG.info("Sleep mode timeout: {} minutes", minutes)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_SLEEP_MODE, minutes.toString()))
    }

    /** Maps a raw guidance byte (0x00=tone, 0x01=voice, 0x02=none) to its preference value. */
    private fun guidanceValueFromRaw(raw: Int): String = when (raw) {
        0x01 -> GUIDANCE_VOICE
        0x02 -> GUIDANCE_NONE
        else -> GUIDANCE_TONE
    }

    /**
     * Parses the headset guidance setting from a response to feature 0x133A.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = tone, 0x01 = voice, 0x02 = none.
     */
    private fun handleHeadsetGuidanceState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val value = guidanceValueFromRaw(payload[0].toInt() and 0xFF)
        LOG.info("Headset guidance: {}", value)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_HEADSET_GUIDANCE, value))
    }

    /**
     * Parses the boom arm guidance setting from a response to feature 0x13BC.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = tone, 0x01 = voice, 0x02 = none.
     */
    private fun handleBoomArmGuidanceState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val value = guidanceValueFromRaw(payload[0].toInt() and 0xFF)
        LOG.info("Boom arm guidance: {}", value)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_BOOM_ARM_GUIDANCE, value))
    }

    /**
     * Parses the voice assistant selection from a response to feature 0x0D4D (param 0x03).
     *
     * Response shape (GET_PARAM): the raw value byte 0x10 = Amazon Alexa, 0x00 = mobile device.
     */
    private fun handleVoiceAssistantState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val raw = payload.last().toInt() and 0xFF
        val value = if (raw == 0x10) VOICE_ASSISTANT_ALEXA else VOICE_ASSISTANT_MOBILE
        LOG.info("Voice assistant: {}", value)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_VOICE_ASSISTANT, value))
    }

    /**
     * Parses the headset button sounds state from a response to feature 0x133F.
     *
     * Response shape (GET → OK_1B): 1 byte – 0x00 = off, 0xFF = on.
     */
    private fun handleButtonSoundsState(payload: ByteArray) {
        if (payload.isEmpty()) return
        val enabled = (payload[0].toInt() and 0xFF) != 0x00
        LOG.info("Button sounds: {}", if (enabled) "ON" else "OFF")
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_JABRA_BUTTON_SOUNDS, enabled))
    }

    // ── Multipoint / paired-device enumeration ────────────────────────────────

    /**
     * Handles a paired-device metadata response (feature 0x0D28). Payload layout (16 bytes):
     *   [0..1] next index (0xFFFF = end of list)
     *   [5]    connection flag (0x01 = currently connected, other = paired but not connected)
     *   [6..11] MAC address (natural byte order)
     * Stores the entry for the current index and requests its name (feature 0x0D32).
     */
    private fun handlePairedDeviceMeta(payload: ByteArray) {
        if (payload.size < 12) {
            LOG.warn("Paired-device metadata payload too short ({} bytes)", payload.size)
            return
        }
        multipointNextIdx = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        val connected = (payload[5].toInt() and 0xFF) == 0x01
        val mac = (6..11).joinToString(":") { "%02X".format(payload[it].toInt() and 0xFF) }
        multipointEntries[multipointReqIdx] = MutableMultipointEntry(address = mac, connected = connected)
        LOG.debug("Paired device idx={} mac={} connected={} next={}",
            multipointReqIdx, mac, connected, Integer.toHexString(multipointNextIdx))
        requestPairedDeviceName(multipointReqIdx)
    }

    /**
     * Handles a paired-device name response (feature 0x0D32). Payload layout:
     *   [0..1] next index   [2..]  ASCII name (empty when only 2 bytes are present)
     * Completes the current entry, then either advances to the next index or, when the list end
     * has been reached, broadcasts the assembled device list to [MultipointPairingActivity].
     */
    private fun handlePairedDeviceName(payload: ByteArray) {
        val name = if (payload.size > 2) String(payload, 2, payload.size - 2, Charsets.US_ASCII) else null
        multipointEntries[multipointReqIdx]?.name = name
        LOG.debug("Paired device idx={} name={}", multipointReqIdx, name)

        if (multipointNextIdx == MULTIPOINT_IDX_END) {
            broadcastMultipointList()
        } else {
            multipointReqIdx = multipointNextIdx
            requestPairedDeviceMeta(multipointReqIdx)
        }
    }

    // ── Outgoing commands ─────────────────────────────────────────────────────

    override fun onSendConfiguration(config: String) {
        LOG.debug("onSendConfiguration: key='{}'" , config)
        when (config) {
            PREF_ACTIVE_NOISE_CANCELLING_TOGGLE -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_ACTIVE_NOISE_CANCELLING_TOGGLE, false)
                sendAncCommand(enable)
            }
            PREF_BUSYLIGHT_ON_CALL -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_BUSYLIGHT_ON_CALL, false)
                sendBusylightOnCallCommand(enable)
            }
            PREF_BUSYLIGHT -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_BUSYLIGHT, false)
                sendBusylightCommand(enable)
            }
            PREF_JABRA_ANSWER_CALL_BOOM_ARM -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_ANSWER_CALL_BOOM_ARM, false)
                sendAnswerCallBoomArmCommand(enable)
            }
            PREF_JABRA_MUTE_MIC_BOOM_ARM -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_MUTE_MIC_BOOM_ARM, false)
                sendMuteMicBoomArmCommand(enable)
            }
            PREF_JABRA_AUTO_REJECT_CALL -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_AUTO_REJECT_CALL, false)
                sendAutoRejectCallCommand(enable)
            }
            PREF_JABRA_MUTE_REMINDER -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_MUTE_REMINDER, false)
                sendMuteReminderCommand(enable)
            }
            PREF_JABRA_SIDETONE -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_SIDETONE, false)
                val volume = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getInt(PREF_JABRA_SIDETONE_VOLUME, 3)
                sendSidetoneCommand(enable, volume)
            }
            PREF_JABRA_SIDETONE_VOLUME -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_SIDETONE, false)
                val volume = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getInt(PREF_JABRA_SIDETONE_VOLUME, 3)
                sendSidetoneCommand(enable, volume)
            }
            PREF_JABRA_CALL_AUDIO_EQ -> {
                val value = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(PREF_JABRA_CALL_AUDIO_EQ, CALL_AUDIO_EQ_NEUTRAL) ?: CALL_AUDIO_EQ_NEUTRAL
                sendCallAudioEqCommand(value)
            }
            PREF_JABRA_EQUALIZER,
            PREF_JABRA_EQUALIZER_BAND1,
            PREF_JABRA_EQUALIZER_BAND2,
            PREF_JABRA_EQUALIZER_BAND3,
            PREF_JABRA_EQUALIZER_BAND4,
            PREF_JABRA_EQUALIZER_BAND5 -> sendEqualizerCommand()
            PREF_JABRA_SLEEP_MODE -> {
                val value = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(PREF_JABRA_SLEEP_MODE, "15") ?: "15"
                sendSleepModeCommand(value)
            }
            PREF_JABRA_HEADSET_GUIDANCE -> {
                val value = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(PREF_JABRA_HEADSET_GUIDANCE, GUIDANCE_VOICE) ?: GUIDANCE_VOICE
                sendHeadsetGuidanceCommand(value)
            }
            PREF_JABRA_BOOM_ARM_GUIDANCE -> {
                val value = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(PREF_JABRA_BOOM_ARM_GUIDANCE, GUIDANCE_TONE) ?: GUIDANCE_TONE
                sendBoomArmGuidanceCommand(value)
            }
            PREF_JABRA_BUTTON_SOUNDS -> {
                val enable = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getBoolean(PREF_JABRA_BUTTON_SOUNDS, false)
                sendButtonSoundsCommand(enable)
            }
            PREF_JABRA_VOICE_ASSISTANT -> {
                val value = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(PREF_JABRA_VOICE_ASSISTANT, VOICE_ASSISTANT_MOBILE) ?: VOICE_ASSISTANT_MOBILE
                sendVoiceAssistantCommand(value)
            }
            DeviceSettingsPreferenceConst.PREF_DEVICE_NAME -> {
                val name = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
                    .getString(DeviceSettingsPreferenceConst.PREF_DEVICE_NAME, gbDevice.name)
                    ?: gbDevice.name
                sendDeviceNameCommand(name)
            }
            else -> super.onSendConfiguration(config)
        }
    }

    /**
     * Sends an ANC enable/disable command.
     *
     * Protocol: SET (0x88) for feature 0x13BE, param 0x01.
     *   ON  → value 0x04
     *   OFF → value 0x01
     */
    private fun sendAncCommand(enable: Boolean) {
        val value = (if (enable) ANC_VALUE_ON else ANC_VALUE_OFF).toByte()
        LOG.info("Sending ANC {} command", if (enable) "ON" else "OFF")
        createTransactionBuilder("jabra-set-anc")
            .write(*buildSetParam(FEAT_ANC, ANC_PARAM_MODE, value))
            .queue()
    }

    /**
     * Sends a busylight on-call enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x1339 – no param index.
     *   ON  → value 0x01
     *   OFF → value 0x00
     */
    private fun sendBusylightOnCallCommand(enable: Boolean) {
        val value = (if (enable) 0x01 else 0x00).toByte()
        val frame = buildSetDirect(FEAT_BUSYLIGHT_ON_CALL, value)
        LOG.info("Sending busylight-on-call {} command: {}", if (enable) "ON" else "OFF", frame.joinToString(" ") { "%02x".format(it) })
        createTransactionBuilder("jabra-set-busylight-on-call")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a busylight (manual LED) enable/disable command.
     *
     * Protocol: SET_PARAM (0x88) for feature 0x1208.
     *   ON  → paramIdx 0x01, value 0x00
     *   OFF → paramIdx 0x00, value 0x00
     */
    private fun sendBusylightCommand(enable: Boolean) {
        val value = (if (enable) 0x01 else 0x00).toByte()
        val frame = buildSetParam(FEAT_BUSYLIGHT, value, 0x00)
        LOG.info("Sending busylight {} command: {}", if (enable) "ON" else "OFF", frame.joinToString(" ") { "%02x".format(it) })
        createTransactionBuilder("jabra-set-busylight")
            .write(*frame)
            .queue()
        // The device acknowledges this SET with an empty frame (no state echo), so reflect the
        // new state optimistically. Without this the device card icon never refreshes.
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(PREF_BUSYLIGHT, enable))
    }

    /**
     * Sends an auto-answer-call (via boom arm) enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x1398 – no param index, full byte.
     * Since this bit shares the byte with the auto-mute-mic bit, the mute-mic bit is read from
     * its own persisted preference so the resulting byte reflects both switches' current state.
     */
    private fun sendAnswerCallBoomArmCommand(enable: Boolean) {
        val muteMicEnabled = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
            .getBoolean(PREF_JABRA_MUTE_MIC_BOOM_ARM, false)
        sendBoomArmActionsCommand(answerCallEnabled = enable, muteMicEnabled = muteMicEnabled)
    }

    /**
     * Sends an auto-mute-mic (via boom arm) enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x1398 – no param index, full byte.
     * Since this bit shares the byte with the auto-answer-call bit, the answer-call bit is read
     * from its own persisted preference so the resulting byte reflects both switches' current
     * state.
     */
    private fun sendMuteMicBoomArmCommand(enable: Boolean) {
        val answerCallEnabled = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
            .getBoolean(PREF_JABRA_ANSWER_CALL_BOOM_ARM, false)
        sendBoomArmActionsCommand(answerCallEnabled = answerCallEnabled, muteMicEnabled = enable)
    }

    /**
     * Combines both boom-arm action bits into a single byte and sends it to feature 0x1398.
     *
     * Protocol: SET (0x87) – no param index, full byte: bit0 (0x01) = auto-mute mic, bit1 (0x02)
     * = auto-answer call. Both bits must always be evaluated and sent together, since the device
     * only accepts the full combined byte.
     */
    private fun sendBoomArmActionsCommand(answerCallEnabled: Boolean, muteMicEnabled: Boolean) {
        var newValue = 0
        if (answerCallEnabled) newValue = newValue or 0x02
        if (muteMicEnabled) newValue = newValue or 0x01
        val frame = buildSetDirect(FEAT_BOOM_ARM_ACTIONS, newValue.toByte())
        LOG.info("Sending boom-arm actions command: answer-call={}, mute-mic={} (raw=0x{})",
            answerCallEnabled, muteMicEnabled, Integer.toHexString(newValue))
        createTransactionBuilder("jabra-set-boom-arm-actions")
            .write(*frame)
            .queue()
    }

    /**
     * Sends an auto-reject-call enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x133C – no param index.
     *   ON  → value 0x01
     *   OFF → value 0x00
     */
    private fun sendAutoRejectCallCommand(enable: Boolean) {
        val value = (if (enable) 0x01 else 0x00).toByte()
        val frame = buildSetDirect(FEAT_AUTO_REJECT_CALL, value)
        LOG.info("Sending auto-reject-call {} command: {}", if (enable) "ON" else "OFF", frame.joinToString(" ") { "%02x".format(it) })
        createTransactionBuilder("jabra-set-auto-reject-call")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a mute-reminder enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x131E – no param index.
     *   ON  → value 0x14
     *   OFF → value 0x00
     */
    private fun sendMuteReminderCommand(enable: Boolean) {
        val value = (if (enable) 0x14 else 0x00).toByte()
        val frame = buildSetDirect(FEAT_MUTE_REMINDER, value)
        LOG.info("Sending mute-reminder {} command: {}", if (enable) "ON" else "OFF", frame.joinToString(" ") { "%02x".format(it) })
        createTransactionBuilder("jabra-set-mute-reminder")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a sidetone enable/disable command with the given volume level.
     *
     * Protocol: SET_PARAM (0x88) for feature 0x137C.
     *   paramIdx: 0x00 = ON, 0x01 = OFF
     *   value: volume level (0-5), encoded as a signed offset: value = (volume-3)*3
     */
    private fun sendSidetoneCommand(enable: Boolean, volume: Int) {
        val paramIdx = (if (enable) 0x00 else 0x01).toByte()
        val value = ((volume.coerceIn(0, 5) - 3) * 3).toByte()
        val frame = buildSetParam(FEAT_SIDETONE, paramIdx, value)
        LOG.info("Sending sidetone {} command, volume={}: {}", if (enable) "ON" else "OFF", volume, frame.joinToString(" ") { "%02x".format(it) })
        createTransactionBuilder("jabra-set-sidetone")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a call audio EQ command.
     *
     * Protocol: SET (0x87) for feature 0x1315 – no param index.
     *   neutral → value 0x00
     *   bass    → value 0x01
     *   treble  → value 0x02
     */
    private fun sendCallAudioEqCommand(value: String) {
        val rawValue = when (value) {
            CALL_AUDIO_EQ_BASS -> 0x01
            CALL_AUDIO_EQ_TREBLE -> 0x02
            else -> 0x00
        }.toByte()
        val frame = buildSetDirect(FEAT_CALL_AUDIO_EQ, rawValue)
        LOG.info("Sending call audio EQ command: {}", value)
        createTransactionBuilder("jabra-set-call-audio-eq")
            .write(*frame)
            .queue()
    }

    /**
     * Sends the 5-band equalizer according to the current preferences.
     *
     * Two features cooperate:
     *  - 0x137E (mode): SET direct 0x00 = flat/neutral (off), 0x01 = custom curve active.
     *  - 0x137D (curve): SET param 0x00 carrying the 5-band custom curve ([buildEqualizerCurve]).
     *
     * The mode preference selects the curve source:
     *  - [EQ_CUSTOM]: gains are read from the per-band slider preferences ([bandSliderToGain]).
     *  - a named preset ([EQUALIZER_PRESETS]): the fixed captured gains for that preset are used.
     *  - [EQ_NEUTRAL] (off): a flat curve is written and the EQ is switched back off afterwards
     *    (0x137E = 0x00), matching the official app.
     * In every non-off case the curve is preceded by enabling custom-curve mode (0x137E = 0x01).
     */
    private fun sendEqualizerCommand() {
        val prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
        val mode = prefs.getString(PREF_JABRA_EQUALIZER, EQ_NEUTRAL) ?: EQ_NEUTRAL
        val gains = when {
            mode == EQ_CUSTOM -> IntArray(5) { band ->
                bandSliderToGain(band, prefs.getInt(EQ_BAND_KEYS[band], EQ_BAND_CENTER))
            }
            else -> EQUALIZER_PRESETS[mode] ?: EQUALIZER_PRESETS.getValue(EQ_NEUTRAL)
        }
        val off = mode == EQ_NEUTRAL
        LOG.info("Sending equalizer: mode={}, gains={}", mode, gains.joinToString(" ") { "%04x".format(it) })

        // When a preset (or off) is selected, mirror its gains back into the per-band sliders so the
        // UI shows an approximate curve and switching to "custom" continues from that starting point.
        if (mode != EQ_CUSTOM) {
            val updates = HashMap<String, Any>()
            for (band in 0 until 5) {
                updates[EQ_BAND_KEYS[band]] = bandGainToSlider(band, gains[band])
            }
            handleGBDeviceEvent(GBDeviceEventUpdatePreferences(updates))
        }

        val builder = createTransactionBuilder("jabra-set-equalizer")
        builder.write(*buildSetDirect(FEAT_EQUALIZER_MODE, 0x01.toByte()))
        builder.write(*buildEqualizerCurve(gains))
        if (off) {
            builder.write(*buildSetDirect(FEAT_EQUALIZER_MODE, 0x00.toByte()))
        }
        builder.queue()
    }

    /**
     * Maps a per-band slider position (0..12, centre [EQ_BAND_CENTER] = flat) to the raw signed
     * 16-bit gain the device expects, scaled linearly to the band's symmetric maximum
     * ([EQ_BAND_MAX_RAW]). Negative results are returned in two's-complement 16-bit form so the
     * curve builder can split them into bytes directly.
     */
    private fun bandSliderToGain(band: Int, slider: Int): Int {
        val offset = slider.coerceIn(0, EQ_BAND_CENTER * 2) - EQ_BAND_CENTER
        val gain = Math.round(offset.toDouble() / EQ_BAND_CENTER * EQ_BAND_MAX_RAW[band]).toInt()
        return gain and 0xFFFF
    }

    /** Inverse of [bandSliderToGain]: maps a raw 16-bit gain back to a 0..12 slider position. */
    private fun bandGainToSlider(band: Int, gain: Int): Int {
        val signed = gain.toShort().toInt()
        val offset = Math.round(signed.toDouble() / EQ_BAND_MAX_RAW[band] * EQ_BAND_CENTER).toInt()
        return (EQ_BAND_CENTER + offset).coerceIn(0, EQ_BAND_CENTER * 2)
    }

    /**
     * Builds the 5-band custom equalizer curve frame for feature 0x137D (param 0x00).
     *
     * The frame is emitted byte-exact from captures; only the 5 band gains vary between presets.
     * Each gain is a signed 16-bit big-endian value inserted before its per-band constant block
     * (frequency/Q scaffold, kept as-is). Layout after the standard `04 09 SEQ` header (40 bytes):
     *   a8 13 7d 05  00 00 00 00 b4
     *   [g1] 06 c2 02 ee  [g2] 06 76 0b b8  [g3] 06 29 2e e0  [g4] 06 3d 59 10  [g5] 08 28
     *
     * Gain is 0x0000 at flat; positive boosts, negative (two's complement) cuts. The device clamps
     * each band symmetrically, but the range differs per band (observed in manual-curve captures):
     *   g1 60 Hz   : +450 (0x01C2) .. -450 (0xFE3E)
     *   g2 250 Hz  : +360 (0x0168) .. -360 (0xFE98)
     *   g3 1 kHz   : +360 (0x0168) .. -360 (0xFE98)
     *   g4 4 kHz   : +360 (0x0168) .. -360 (0xFE98)
     *   g5 7.6 kHz : +600 (0x0258) .. -600 (0xFDA8)
     */
    private fun buildEqualizerCurve(gains: IntArray): ByteArray {
        fun hi(g: Int) = ((g shr 8) and 0xFF).toByte()
        fun lo(g: Int) = (g and 0xFF).toByte()
        return byteArrayOf(
            0x04, 0x09, nextSeq(),
            0xA8.toByte(), 0x13, 0x7D, 0x05,
            0x00, 0x00, 0x00, 0x00, 0xB4.toByte(),
            hi(gains[0]), lo(gains[0]), 0x06, 0xC2.toByte(), 0x02, 0xEE.toByte(),
            hi(gains[1]), lo(gains[1]), 0x06, 0x76, 0x0B, 0xB8.toByte(),
            hi(gains[2]), lo(gains[2]), 0x06, 0x29, 0x2E, 0xE0.toByte(),
            hi(gains[3]), lo(gains[3]), 0x06, 0x3D, 0x59, 0x10,
            hi(gains[4]), lo(gains[4]), 0x08, 0x28,
        )
    }

    /**
     * Parses the equalizer custom-curve response (feature 0x137D). The 5 decoded band gains are
     * always mirrored into the per-band slider preferences ([EQ_BAND_KEYS]). If the gains match a
     * known preset the mode preference is set to that preset, otherwise to [EQ_CUSTOM]. The gains
     * sit right after the 0xB4 marker that ends the curve preamble, each preceding its 4-byte
     * per-band constant block.
     */
    private fun handleEqualizerCurveState(payload: ByteArray) {
        val marker = payload.indexOfFirst { it == 0xB4.toByte() }
        if (marker < 0) return
        val gains = IntArray(5)
        val updates = HashMap<String, Any>()
        for (band in 0 until 5) {
            val idx = marker + 1 + band * 6
            if (idx + 1 >= payload.size) return
            gains[band] = ((payload[idx].toInt() and 0xFF) shl 8) or (payload[idx + 1].toInt() and 0xFF)
            updates[EQ_BAND_KEYS[band]] = bandGainToSlider(band, gains[band])
        }
        val preset = EQUALIZER_PRESETS.entries.firstOrNull { it.value.contentEquals(gains) }?.key ?: EQ_CUSTOM
        updates[PREF_JABRA_EQUALIZER] = preset
        LOG.info("Equalizer curve: mode={}, {}", preset, updates)
        handleGBDeviceEvent(GBDeviceEventUpdatePreferences(updates))
    }

    /**
     * Sends a sleep mode timeout command.
     *
     * Protocol: SET (0x87) for feature 0x1390 – no param index; value = minutes / 5 (0 = off).
     */
    private fun sendSleepModeCommand(minutes: String) {
        val value = ((minutes.toIntOrNull() ?: 0) / 5).coerceIn(0, 255).toByte()
        val frame = buildSetDirect(FEAT_SLEEP_MODE, value)
        LOG.info("Sending sleep mode command: {} minutes", minutes)
        createTransactionBuilder("jabra-set-sleep-mode")
            .write(*frame)
            .queue()
    }

    /** Maps a guidance preference value to its raw byte (0x00=tone, 0x01=voice, 0x02=none). */
    private fun guidanceRawFromValue(value: String): Byte = when (value) {
        GUIDANCE_VOICE -> 0x01
        GUIDANCE_NONE -> 0x02
        else -> 0x00
    }.toByte()

    /**
     * Sends a headset guidance command.
     *
     * Protocol: SET (0x87) for feature 0x133A – no param index.
     */
    private fun sendHeadsetGuidanceCommand(value: String) {
        val frame = buildSetDirect(FEAT_HEADSET_GUIDANCE, guidanceRawFromValue(value))
        LOG.info("Sending headset guidance command: {}", value)
        createTransactionBuilder("jabra-set-headset-guidance")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a boom arm guidance command.
     *
     * Protocol: SET (0x87) for feature 0x13BC – no param index.
     */
    private fun sendBoomArmGuidanceCommand(value: String) {
        val frame = buildSetDirect(FEAT_BOOM_ARM_GUIDANCE, guidanceRawFromValue(value))
        LOG.info("Sending boom arm guidance command: {}", value)
        createTransactionBuilder("jabra-set-boom-arm-guidance")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a voice assistant selection command.
     *
     * Protocol: SET_PARAM (0x88) for feature 0x0D4D, paramIdx 0x03.
     *   Amazon Alexa   → value 0x10
     *   Mobile device  → value 0x00
     */
    private fun sendVoiceAssistantCommand(value: String) {
        val raw = (if (value == VOICE_ASSISTANT_ALEXA) 0x10 else 0x00).toByte()
        val frame = buildSetParam(FEAT_VOICE_ASSISTANT, 0x03, raw)
        LOG.info("Sending voice assistant command: {}", value)
        createTransactionBuilder("jabra-set-voice-assistant")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a headset button sounds enable/disable command.
     *
     * Protocol: SET (0x87) for feature 0x133F – no param index.
     *   ON  → value 0xFF
     *   OFF → value 0x00
     */
    private fun sendButtonSoundsCommand(enable: Boolean) {
        val value = (if (enable) 0xFF else 0x00).toByte()
        val frame = buildSetDirect(FEAT_BUTTON_SOUNDS, value)
        LOG.info("Sending button sounds {} command", if (enable) "ON" else "OFF")
        createTransactionBuilder("jabra-set-button-sounds")
            .write(*frame)
            .queue()
    }

    /**
     * Sends a device rename command.
     *
     * Protocol: variable-length string SET for feature 0x1356 – ASCII name bytes, no length
     * prefix or terminator. The CMD byte encodes the payload length (`0x86 + payloadLength`,
     * see [buildSetString]). Observed in captures: `04 09 SEQ CMD 13 56 <ascii name>` where
     * CMD is 0x91 for an 11-byte name and 0x96 for a 16-byte name.
     */
    private fun sendDeviceNameCommand(name: String) {
        val frame = buildSetString(FEAT_DEVICE_NAME, name)
        LOG.info("Sending device name command: {}", name)
        createTransactionBuilder("jabra-set-device-name")
            .write(*frame)
            .queue()
    }

    // ── Multipoint ────────────────────────────────────────────────────────────

    /**
     * Registers the [LocalBroadcastManager] receiver used by [MultipointPairingActivity] to
     * request the paired-device list. Only the read-only enumeration is currently supported:
     * the enable/disable, connect/disconnect and pairing-mode commands have not been reverse
     * engineered yet (no captures available), so those actions are ignored for now.
     */
    private fun registerMultipointReceiver() {
        if (multipointReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES)
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(multipointReceiver, filter)
        multipointReceiverRegistered = true
    }

    private val multipointReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableCompat<GBDevice>(GBDevice.EXTRA_DEVICE)
            if (device?.address != gbDevice.address) return // not for this device

            when (intent.action) {
                MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES -> requestMultipointDevices()
                else -> LOG.warn("Unhandled multipoint action {}", intent.action)
            }
        }
    }

    /** Starts a fresh walk of the paired-device list from index 0. */
    private fun requestMultipointDevices() {
        LOG.info("Requesting paired multipoint devices")
        multipointEntries.clear()
        multipointReqIdx = 0
        multipointNextIdx = MULTIPOINT_IDX_END
        requestPairedDeviceMeta(0)
    }

    private fun requestPairedDeviceMeta(index: Int) {
        createTransactionBuilder("jabra-multipoint-meta")
            .write(*buildGetIdx(FEAT_PAIRED_DEVICE_META, index))
            .queue()
    }

    private fun requestPairedDeviceName(index: Int) {
        createTransactionBuilder("jabra-multipoint-name")
            .write(*buildGetIdx(FEAT_PAIRED_DEVICE_NAME, index))
            .queue()
    }

    /** Broadcasts the fully assembled paired-device list to [MultipointPairingActivity]. */
    private fun broadcastMultipointList() {
        val devices = ArrayList(multipointEntries.values.map {
            MultipointDevice(it.address, it.name, it.connected)
        })
        LOG.info("Broadcasting {} multipoint devices", devices.size)
        val intent = Intent(MultipointPairingActivity.ACTION_MULTIPOINT_DEVICE_LIST).apply {
            putExtra(GBDevice.EXTRA_DEVICE, gbDevice)
            putParcelableArrayListExtra(MultipointPairingActivity.EXTRA_DEVICE_LIST, devices)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    // ── Keepalive ─────────────────────────────────────────────────────────────

    /**
     * Periodic keepalive sent every ~15 s. The device does not disconnect without it.
     * Observed in captures: host sends GET 0xFC00, device replies with OK_EMPTY.
     */
    private val keepaliveRunnable = object : Runnable {
        override fun run() {
            if (!gbDevice.isConnected) return
            LOG.trace("Sending keepalive")
            try {
                createTransactionBuilder("jabra-keepalive")
                    .write(*buildGet(FEAT_HEARTBEAT))
                    .queue()
            } catch (e: Exception) {
                LOG.warn("Failed to send keepalive", e)
            }
            keepaliveHandler.postDelayed(this, KEEPALIVE_INTERVAL_MS)
        }
    }

    // ── Protocol helpers ──────────────────────────────────────────────────────

    private fun nextSeq(): Byte = (seqCounter.getAndIncrement() and 0xFF).toByte()

    /**
     * Base frame builder. Produces: `04 09 SEQ <cmd> <ID_HI> <ID_LO> [extra...]`
     *
     * All protocol commands share this 6-byte header. The CMD byte is not a fixed opcode – it
     * is computed as `[base] + extra.size`, matching every observed request in captures (e.g.
     * GET=0x46+0, GET_PARAM=0x46+1, GET_IDX=0x46+2, SET=0x86+1, SET_PARAM=0x86+2, and the
     * variable-length device-name SET=0x86+nameLength). Callers supply the category [base]
     * ([CMD_GET_BASE] or [CMD_SET_BASE]) and any trailing [extra] bytes (parameter indices,
     * values, string payloads, etc.).
     */
    private fun buildCommand(base: Int, featureId: Int, vararg extra: Byte): ByteArray = byteArrayOf(
        0x04, 0x09, nextSeq(), (base + extra.size).toByte(),
        (featureId shr 8 and 0xFF).toByte(), (featureId and 0xFF).toByte(),
        *extra
    )

    /**
     * Builds a GET or GET_PARAM frame.
     *  - Without [paramIdx]: `04 09 SEQ 0x46 ID_HI ID_LO`
     *  - With    [paramIdx]: `04 09 SEQ 0x47 ID_HI ID_LO PARAM_IDX`
     */
    private fun buildGet(featureId: Int, paramIdx: Byte? = null): ByteArray =
        if (paramIdx == null) buildCommand(CMD_GET_BASE, featureId)
        else buildCommand(CMD_GET_BASE, featureId, paramIdx)

    /** Builds: `04 09 SEQ 0x48 ID_HI ID_LO IDX_HI IDX_LO` */
    private fun buildGetIdx(featureId: Int, index: Int): ByteArray =
        buildCommand(CMD_GET_BASE, featureId,
            (index shr 8 and 0xFF).toByte(), (index and 0xFF).toByte())

    /** Builds: `04 09 SEQ 0x87 ID_HI ID_LO VALUE` (SET without param index) */
    private fun buildSetDirect(featureId: Int, value: Byte): ByteArray =
        buildCommand(CMD_SET_BASE, featureId, value)

    /** Builds: `04 09 SEQ 0x88 ID_HI ID_LO PARAM_IDX VALUE` */
    private fun buildSetParam(featureId: Int, paramIdx: Byte, value: Byte): ByteArray =
        buildCommand(CMD_SET_BASE, featureId, paramIdx, value)

    /**
     * Builds a subscription frame for the device-initiated notification bus (feature
     * [FEAT_TIMER_COUNTER]): `04 09 SEQ 0x8A 0D 4C <mask big-endian, 4 bytes>`. The [mask]
     * selects which push categories the device emits; the official app ends on 0x00000210.
     */
    private fun buildNotifySubscribe(mask: Int): ByteArray =
        buildCommand(CMD_SET_BASE, FEAT_TIMER_COUNTER,
            (mask shr 24 and 0xFF).toByte(), (mask shr 16 and 0xFF).toByte(),
            (mask shr 8 and 0xFF).toByte(), (mask and 0xFF).toByte())

    /**
     * Extracts an ASCII string from a response payload according to the response type.
     *
     * - Types 0xCD, 0xD3, 0xD7: `payload[0]` is the string length, string follows.
     * - Type 0xD4:               null-terminated ASCII.
     * - Other types:             treat entire payload as raw ASCII.
     */
    private fun extractString(responseType: Int, payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        return when (responseType) {
            RESP_STR_LEN, RESP_STR_LEN2, 0xD3 -> {
                val len = payload[0].toInt() and 0xFF
                if (len > 0 && len < payload.size) String(payload, 1, len) else null
            }
            RESP_STR_NULL, RESP_STR_NAME -> {
                val nul = payload.indexOfFirst { it == 0.toByte() }
                when {
                    nul > 0  -> String(payload, 0, nul)
                    nul < 0  -> String(payload)
                    else     -> null
                }
            }
            else -> String(payload)
        }
    }

    /**
     * Builds: `04 09 SEQ CMD ID_HI ID_LO <ascii bytes>` (SET of a variable-length string value,
     * no param index). The CMD byte is `0x86 + payloadLength`, computed by [buildCommand] from
     * the number of ASCII bytes passed as [extra].
     */
    private fun buildSetString(featureId: Int, value: String): ByteArray =
        buildCommand(CMD_SET_BASE, featureId, *value.toByteArray(Charsets.US_ASCII))
}
