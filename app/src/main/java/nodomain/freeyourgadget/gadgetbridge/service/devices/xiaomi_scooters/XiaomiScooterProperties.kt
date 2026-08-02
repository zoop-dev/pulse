package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryCurrentSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryPowerSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryTemperatureSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryVoltageSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.GenericTemperatureSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryCurrentSample
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryPowerSample
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryTemperatureSample
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryVoltageSample
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericTemperatureSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterProperties.RIDE_HISTORY_CODES
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterProperties.TIRE_PRESSURE_ENCODERS
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterProperties.decodeTirePressureMaintenance
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters.XiaomiScooterProperties.numeric
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
    const val CODE_TIRE_PRESSURE_MAINTENANCE = 0x0307
    const val CODE_LAST_RIDE_1 = 0x0601
    const val CODE_LAST_RIDE_2 = 0x0602
    const val CODE_LAST_RIDE_3 = 0x0603
    const val CODE_LAST_RIDE_4 = 0x0604
    const val CODE_LAST_RIDE_5 = 0x0605

    /** Ride history codes, ordered oldest to newest. */
    val RIDE_HISTORY_CODES: List<Int> = listOf(
        CODE_LAST_RIDE_1,
        CODE_LAST_RIDE_2,
        CODE_LAST_RIDE_3,
        CODE_LAST_RIDE_4,
        CODE_LAST_RIDE_5,
    )

    /**
     * Anything that can be written to the device via SET: a 2-byte `code`, its wire type, and how
     * to turn the current SharedPreferences state into the bytes to send.
     */
    sealed interface Encodable {
        val code: Int
        val prefKey: String
        val wireType: Int

        /** Reads the current preference value and returns the bytes to SET on the device. */
        fun encode(prefs: Prefs): ByteArray
    }

    /**
     * One row of the declarative apply/parse mapping for a property that maps 1:1 to a single
     * preference: encode and decode live on the same object here, so they can never drift apart.
     *
     * Properties where one wire value maps to *several* preferences at once (e.g.
     * tire_pressure_maintenance, which packs an enabled flag, an interval and a remaining-days
     * count into a single value) don't fit that shape and aren't modeled as a [PropertyMapping] at
     * all -- see [TIRE_PRESSURE_ENCODERS] for their (still 1:1, per-preference) encode side, and
     * [decodeTirePressureMaintenance] plus [XiaomiScooterSupport.handlePropertyEntry] for the
     * necessarily-multi-preference decode side.
     */
    sealed class PropertyMapping : Encodable {
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

    /** A single writable preference/action belonging to a composite property; see [TIRE_PRESSURE_ENCODERS]. */
    class CompositeEncoder(
        override val code: Int,
        override val prefKey: String,
        override val wireType: Int,
        private val encodeFn: (Prefs) -> ByteArray,
    ) : Encodable {
        override fun encode(prefs: Prefs): ByteArray = encodeFn(prefs)
    }

    // tire_pressure_maintenance's wire value is `[status:1][interval:3]` for SET, plus a trailing
    // `[remaining:3]` on GET_RSP/NOTIFY. Status 0 = enabled, 2 = disabled (SET "2030" -> GET_RSP "2030030");
    // 3 = reset the remaining-days countdown back to the full interval, a one-shot action rather than a persisted
    // status (only ever observed in a SET, never echoed back in a GET_RSP).
    private const val TIRE_PRESSURE_STATUS_ENABLED = 0
    private const val TIRE_PRESSURE_STATUS_DISABLED = 2
    private const val TIRE_PRESSURE_STATUS_RESET = 3
    private val DEFAULT_TIRE_PRESSURE_INTERVAL = XiaomiScooterTirePressureInterval.DAYS_30

    private fun encodeTirePressureMaintenanceSet(status: Int, days: Int): ByteArray =
        "$status${days.toString().padStart(3, '0')}".toByteArray(Charsets.US_ASCII)

    private fun Prefs.tirePressureInterval(): XiaomiScooterTirePressureInterval {
        val prefValue = getString(
            DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_INTERVAL_DAYS,
            DEFAULT_TIRE_PRESSURE_INTERVAL.name.lowercase(),
        )
        return XiaomiScooterTirePressureInterval.fromPreference(prefValue) ?: DEFAULT_TIRE_PRESSURE_INTERVAL
    }

    private fun Prefs.tirePressureEnabled(): Boolean =
        getBoolean(DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED, true)

    private fun Prefs.tirePressureStatus(): Int =
        if (tirePressureEnabled()) TIRE_PRESSURE_STATUS_ENABLED else TIRE_PRESSURE_STATUS_DISABLED

    /**
     * Encoders for tire_pressure_maintenance's three writable preferences/actions: the interval
     * list setting, the enabled switch, and the one-shot reset action. All three read each other's
     * current state (via the `Prefs.tirePressure*()` helpers above) to reconstruct the single
     * combined wire value, since none of them alone carries the full picture. The read side is
     * [decodeTirePressureMaintenance], applied directly in [XiaomiScooterSupport.handlePropertyEntry].
     */
    val TIRE_PRESSURE_ENCODERS: List<CompositeEncoder> = listOf(
        CompositeEncoder(
            code = CODE_TIRE_PRESSURE_MAINTENANCE,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_INTERVAL_DAYS,
            wireType = XiaomiScooterProtocol.TYPE_STR,
        ) { prefs -> encodeTirePressureMaintenanceSet(prefs.tirePressureStatus(), prefs.tirePressureInterval().days) },
        CompositeEncoder(
            code = CODE_TIRE_PRESSURE_MAINTENANCE,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_ENABLED,
            wireType = XiaomiScooterProtocol.TYPE_STR,
        ) { prefs -> encodeTirePressureMaintenanceSet(prefs.tirePressureStatus(), prefs.tirePressureInterval().days) },
        CompositeEncoder(
            code = CODE_TIRE_PRESSURE_MAINTENANCE,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_TIRE_PRESSURE_RESET,
            wireType = XiaomiScooterProtocol.TYPE_STR,
        ) { prefs -> encodeTirePressureMaintenanceSet(TIRE_PRESSURE_STATUS_RESET, prefs.tirePressureInterval().days) },
    )

    /** Decoded `tire_pressure_maintenance` GET_RSP/NOTIFY value: `[status:1][interval:3][remaining:3]` ASCII digits. */
    data class TirePressureMaintenance(val enabled: Boolean, val intervalDays: Int, val remainingDays: Int)

    fun decodeTirePressureMaintenance(raw: String): TirePressureMaintenance? {
        if (raw.length < 7 || !raw.all { it.isDigit() }) {
            return null
        }
        val status = raw.substring(0, 1).toIntOrNull() ?: return null
        val interval = raw.substring(1, 4).toIntOrNull() ?: return null
        val remaining = raw.substring(4, 7).toIntOrNull() ?: return null
        return TirePressureMaintenance(status != TIRE_PRESSURE_STATUS_DISABLED, interval, remaining)
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
    val SETTINGS_BY_PREF_KEY: Map<String, Encodable> = SETTINGS.plus<Encodable>(
        TIRE_PRESSURE_ENCODERS
    ).associateBy { it.prefKey }

    /** A read-only property, decoded straight into the display string for a `text` setting. */
    class TelemetryProperty(
        val code: Int,
        val prefKey: String,
        val decode: (XiaomiScooterProtocol.RawValue) -> Any?,
        /**
         * Set for telemetry that is a measurement rather than an identifier, so that it also gets
         * persisted as a sample and can be charted over time. Null for the rest (serials, firmware
         * versions, manufacturing dates).
         */
        val sample: TelemetrySample? = null,
    )

    /**
     * The numeric side of a [TelemetryProperty]: the same wire value decoded to a plain number,
     * plus the table it gets persisted into. Built by [numeric], which derives it from the very
     * same decode function that produces [TelemetryProperty.decode]'s display string.
     */
    class TelemetrySample(
        val value: (XiaomiScooterProtocol.RawValue) -> Float?,
        val sink: SampleSink,
    )

    /** Writes one decoded telemetry reading to the database. */
    fun interface SampleSink {
        /** [timestamp] is in milliseconds since the epoch. */
        fun persist(device: GBDevice, session: DaoSession, context: Context?, timestamp: Long, value: Float)
    }

    /** A [TelemetryProperty] for a measurement: decoded once, then both formatted and persisted. */
    private fun numeric(
        code: Int,
        prefKey: String,
        value: (XiaomiScooterProtocol.RawValue) -> Float?,
        format: (Float) -> String,
        sink: SampleSink,
    ) = TelemetryProperty(
        code = code,
        prefKey = prefKey,
        decode = { raw -> value(raw)?.let(format) },
        sample = TelemetrySample(value, sink),
    )

    /** Read-only telemetry pushed to a read-only settings field. */
    val TELEMETRY: List<TelemetryProperty> = listOf(
        numeric(
            code = CODE_VOLTAGE,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_VOLTAGE,
            value = { it.asF32()?.div(100f) },
            format = { "%.2f V".format(it) },
            sink = { device, session, context, timestamp, value ->
                val sample = BatteryVoltageSample()
                sample.timestamp = timestamp
                sample.batteryIndex = 0 // single battery
                sample.voltage = value
                BatteryVoltageSampleProvider(device, session).persistSamples(sample, context)
            }
        ),
        numeric(
            code = CODE_CURRENT,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_CURRENT,
            value = { it.asF32()?.div(100f) },
            format = { "%.2f A".format(it) },
            sink = { device, session, context, timestamp, value ->
                val sample = BatteryCurrentSample()
                sample.timestamp = timestamp
                sample.batteryIndex = 0 // single battery
                sample.current = value
                BatteryCurrentSampleProvider(device, session).persistSamples(sample, context)
            }
        ),
        numeric(
            code = CODE_POWER,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_POWER,
            value = { it.asF32()?.div(100f) },
            format = { "%.2f W".format(it) },
            sink = { device, session, context, timestamp, value ->
                val sample = BatteryPowerSample()
                sample.timestamp = timestamp
                sample.batteryIndex = 0 // single battery
                sample.power = value
                BatteryPowerSampleProvider(device, session).persistSamples(sample, context)
            }
        ),
        numeric(
            code = CODE_BATTERY_TEMP,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_TEMP,
            value = { it.asI8()?.toFloat() },
            format = { "%.0f °C".format(it) },
            sink = { device, session, context, timestamp, value ->
                val sample = BatteryTemperatureSample()
                sample.timestamp = timestamp
                sample.batteryIndex = 0 // single battery
                sample.temperature = value
                BatteryTemperatureSampleProvider(device, session).persistSamples(sample, context)
            }
        ),
        numeric(
            code = CODE_SCOOTER_TEMP,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_SCOOTER_TEMP,
            value = { it.asI8()?.toFloat() },
            format = { "%.0f °C".format(it) },
            sink = { device, session, context, timestamp, value ->
                val sample = GenericTemperatureSample()
                sample.timestamp = timestamp
                sample.temperature = value
                sample.temperatureType = TemperatureSample.TYPE_UNKNOWN
                sample.temperatureLocation = TemperatureSample.LOCATION_UNKNOWN
                GenericTemperatureSampleProvider(device, session).persistSamples(sample, context)
            },
        ),
        TelemetryProperty(
            code = CODE_BATTERY_CYCLES,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_CYCLES,
            decode = { it.asU8()?.toString() },
        ),
        TelemetryProperty(
            code = CODE_BATTERY_MFG_DATE,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_MFG_DATE,
            decode = { it.asString() },
        ),
        TelemetryProperty(
            code = CODE_BATTERY_SERIAL,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BATTERY_SERIAL,
            decode = { it.asString() },
        ),
        TelemetryProperty(
            code = CODE_BMS_FW_VERSION,
            prefKey = DeviceSettingsPreferenceConst.PREF_XIAOMI_SCOOTER_BMS_FW_VERSION,
            decode = { it.asString() },
        ),
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

    /**
     * Codes fetched once via GET right after login to seed the settings screen and battery card.
     *
     * [RIDE_HISTORY_CODES] is deliberately NOT included here and fetched separately, see requestRideHistory().
     */
    // TODO: Review
    val INITIAL_GET_CODES: List<Int> = SETTINGS.map { it.code } + TELEMETRY.map { it.code } + listOf(
        CODE_BATTERY_PERCENT,
        CODE_SERIAL_NUMBER,
        CODE_FIRMWARE_VERSION,
        CODE_TIRE_PRESSURE_MAINTENANCE,
    )
}
