package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.LabeledEntry

enum class XiaomiScooterRideMode(val code: Int, override val label: Int) : LabeledEntry {
    STANDARD(0x02, R.string.xiaomi_scooter_ride_mode_standard),
    SPORT(0x03, R.string.xiaomi_scooter_ride_mode_sport),
    WALK(0x0b, R.string.xiaomi_scooter_ride_mode_walk),
    ;

    companion object {
        fun fromCode(code: Int): XiaomiScooterRideMode? = entries.find { it.code == code }
    }
}

enum class XiaomiScooterEnergyRecovery(val code: Int, override val label: Int) : LabeledEntry {
    LOW(0x1e, R.string.xiaomi_scooter_energy_recovery_low),
    MEDIUM(0x3c, R.string.xiaomi_scooter_energy_recovery_medium),
    HIGH(0x5a, R.string.xiaomi_scooter_energy_recovery_high),
    ;

    companion object {
        fun fromPreference(value: String): XiaomiScooterEnergyRecovery? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): XiaomiScooterEnergyRecovery? = entries.find { it.code == code }
    }
}

enum class XiaomiScooterUnits(val code: Int, override val label: Int) : LabeledEntry {
    MPH(0x00, R.string.mi_h),
    KMH(0x01, R.string.km_h),
    ;

    companion object {
        fun fromPreference(value: String): XiaomiScooterUnits? = entries.find { it.name == value.uppercase() }
        fun fromCode(code: Int): XiaomiScooterUnits? = entries.find { it.code == code }
    }
}
