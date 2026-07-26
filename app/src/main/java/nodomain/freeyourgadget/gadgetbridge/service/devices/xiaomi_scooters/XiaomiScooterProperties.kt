package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

/**
 * Declarative table mapping each writable device-settings preference to its on-wire property:
 * a 2-byte `code`, the wire value type, and how to translate between a SharedPreferences value
 * and the wire bytes. Both the settings-apply path ([XiaomiScooterSupport.onSendConfiguration])
 * and the telemetry-parse path (NOTIFY/GET_RSP) are driven from this one table, so encode and
 * decode can never drift apart.
 */
object XiaomiScooterProperties {
    const val CODE_RIDE_MODE = 0x0101
    const val CODE_BATTERY_PERCENT = 0x0102 // TODO unconfirmed
    const val CODE_VOLTAGE = 0x0104
    const val CODE_CURRENT = 0x0105
    const val CODE_POWER = 0x0106
    const val CODE_RANGE_KM = 0x0107 // TODO unused
    const val CODE_MOTOR_LOCKED = 0x0202
    const val CODE_TAILLIGHT_ALWAYS_ON = 0x0204
    const val CODE_ENERGY_RECOVERY_INTENSITY = 0x0205
    const val CODE_AUTO_LIGHTS = 0x020c
    const val CODE_TCS_ANTI_SLIP = 0x020d
    const val CODE_SMART_ENERGY_RECOVERY = 0x020e
    const val CODE_SLOPE_PARKING = 0x020f
    const val CODE_AMBIENT_LIGHT = 0x0210
    const val CODE_BLUETOOTH_ALWAYS_ON = 0x0211
    const val CODE_STATUS = 0x0301 // TODO unconfirmed
    const val CODE_BATTERY_TEMP = 0x0302
    const val CODE_SCOOTER_TEMP = 0x0303
    const val CODE_SPEED_UNIT = 0x0305
    const val CODE_DATE_PUT_INTO_SERVICE = 0x0308 // TODO unused
    const val CODE_BATTERY_CYCLES = 0x030b // TODO unconfirmed
    const val CODE_BATTERY_MFG_DATE = 0x0401
    const val CODE_BATTERY_SERIAL = 0x0402
    const val CODE_BMS_FW_VERSION = 0x0403
    const val CODE_SERIAL_NUMBER = 0x0404
    const val CODE_FIRMWARE_VERSION = 0x0405
    const val CODE_FIND_SCOOTER = 0x040a

    /** One row of the declarative apply/parse mapping. */
    sealed class PropertyMapping {
        abstract val code: Int
        abstract val prefKey: String
        abstract val wireType: Int

        /** Reads the current preference value and returns the bytes to SET on the device. */
        abstract fun encode(prefs: Prefs): ByteArray

        /** Converts a decoded wire value into what `GBDeviceEventUpdatePreferences` expects. */
        abstract fun decode(value: XiaomiScooterProtocol.RawValue): Any?
    }

    class BooleanProperty(
        override val code: Int,
        override val prefKey: String,
        override val wireType: Int,
        private val defaultValue: Boolean = false,
    ) : PropertyMapping() {
        override fun encode(prefs: Prefs): ByteArray =
            byteArrayOf(if (prefs.getBoolean(prefKey, defaultValue)) 1 else 0)

        override fun decode(value: XiaomiScooterProtocol.RawValue): Any? = value.asU8()?.let { it != 0 }
    }

    class EnumProperty(
        override val code: Int,
        override val prefKey: String,
        override val wireType: Int,
        private val defaultPrefValue: String,
        private val toWireCode: (String) -> Int?,
        private val fromWireCode: (Int) -> String?,
    ) : PropertyMapping() {
        override fun encode(prefs: Prefs): ByteArray {
            val prefValue = prefs.getString(prefKey, defaultPrefValue)
            val wireCode = toWireCode(prefValue) ?: toWireCode(defaultPrefValue) ?: 0
            return byteArrayOf(wireCode.toByte())
        }

        override fun decode(value: XiaomiScooterProtocol.RawValue): Any? = value.asU8()?.let { fromWireCode(it) }
    }

    /** Writable settings: settings-screen key <-> wire property. */
    val SETTINGS: List<PropertyMapping> = listOf(
        BooleanProperty(
            code = CODE_MOTOR_LOCKED,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_MOTOR_LOCKED,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_TAILLIGHT_ALWAYS_ON,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TAILLIGHT_ALWAYS_ON,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_AUTO_LIGHTS,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_AUTO_LIGHTS,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_AMBIENT_LIGHT,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_AMBIENT_LIGHT,
            wireType = XiaomiScooterProtocol.TYPE_U8,
        ),
        BooleanProperty(
            code = CODE_SMART_ENERGY_RECOVERY,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SMART_ENERGY_RECOVERY,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_SLOPE_PARKING,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SLOPE_PARKING,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_TCS_ANTI_SLIP,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TCS_ANTI_SLIP,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        BooleanProperty(
            code = CODE_BLUETOOTH_ALWAYS_ON,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_KEEP_BLUETOOTH_ON,
            wireType = XiaomiScooterProtocol.TYPE_BOOL,
        ),
        EnumProperty(
            code = CODE_ENERGY_RECOVERY_INTENSITY,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_ENERGY_RECOVERY_INTENSITY,
            wireType = XiaomiScooterProtocol.TYPE_U8,
            defaultPrefValue = XiaomiScooterEnergyRecovery.MEDIUM.name.lowercase(),
            toWireCode = { XiaomiScooterEnergyRecovery.fromPreference(it)?.code },
            fromWireCode = { XiaomiScooterEnergyRecovery.fromCode(it)?.name?.lowercase() },
        ),
        EnumProperty(
            code = CODE_SPEED_UNIT,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SPEED_UNIT,
            wireType = XiaomiScooterProtocol.TYPE_U8,
            defaultPrefValue = XiaomiScooterUnits.KMH.name.lowercase(),
            toWireCode = { XiaomiScooterUnits.fromPreference(it)?.code },
            fromWireCode = { XiaomiScooterUnits.fromCode(it)?.name?.lowercase() },
        ),
    )

    val SETTINGS_BY_CODE: Map<Int, PropertyMapping> = SETTINGS.associateBy { it.code }
    val SETTINGS_BY_PREF_KEY: Map<String, PropertyMapping> = SETTINGS.associateBy { it.prefKey }

    /** A read-only property, decoded straight into the display string for a `text` setting. */
    class TelemetryProperty(
        val code: Int,
        val prefKey: String,
        val decode: (XiaomiScooterProtocol.RawValue) -> Any?,
    )

    /** Read-only telemetry pushed to a read-only settings field. */
    val TELEMETRY: List<TelemetryProperty> = listOf(
        TelemetryProperty(CODE_VOLTAGE, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_VOLTAGE) {
            // TODO persist and chart
            it.asF32()?.let { raw -> "%.2f V".format(raw / 100f) }
        },
        TelemetryProperty(CODE_CURRENT, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_CURRENT) {
            // TODO persist and chart
            it.asF32()?.let { raw -> "%.2f A".format(raw / 100f) }
        },
        TelemetryProperty(CODE_POWER, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_POWER) {
            // TODO persist and chart
            it.asF32()?.let { raw -> "%.2f W".format(raw / 100f) }
        },
        TelemetryProperty(CODE_BATTERY_TEMP, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_TEMP) {
            // TODO persist and chart
            it.asI8()?.let { raw -> "$raw °C" }
        },
        TelemetryProperty(CODE_SCOOTER_TEMP, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SCOOTER_TEMP) {
            // TODO persist and chart
            it.asI8()?.let { raw -> "$raw °C" }
        },
        TelemetryProperty(CODE_BATTERY_CYCLES, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_CYCLES) {
            it.asU8()?.toString()
        },
        TelemetryProperty(CODE_BATTERY_MFG_DATE, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_MFG_DATE) {
            it.asString()
        },
        TelemetryProperty(CODE_BATTERY_SERIAL, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_SERIAL) {
            it.asString()
        },
        TelemetryProperty(CODE_BMS_FW_VERSION, DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BMS_FW_VERSION) {
            it.asString()
        },
    )

    val TELEMETRY_BY_CODE: Map<Int, TelemetryProperty> = TELEMETRY.associateBy { it.code }

    /**
     * Codes the app subscribes to at connection time for push telemetry (service 05?).
     * Sending any of these seems to trigger the same telemetry notifications, so we just
     * replay the same ones that the vendor app sends.
     **/
    val NOTIFY_SUBSCRIBE_CODES: List<Int> = listOf(
        0x0501,
        0x0504,
        0x0505,
        0x0506,
        0x0507,
        0x0508,
        0x0509,
        0x050a,
        0x0510,
        0x0513,
        0x0514,
        0x0516,
        0x0518,
        0x0520,
        0x0522,
    )

    /** Codes fetched once via GET right after login to seed the settings screen and battery card. */
    // TODO: Review
    val INITIAL_GET_CODES: List<Int> = SETTINGS.map { it.code } + TELEMETRY.map { it.code } + listOf(
        CODE_BATTERY_PERCENT,
        CODE_SERIAL_NUMBER,
        CODE_FIRMWARE_VERSION,
    )
}
