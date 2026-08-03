package nodomain.freeyourgadget.gadgetbridge.service.devices.dji

import android.content.SharedPreferences
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs
import kotlin.random.Random
import androidx.core.content.edit

class DjiPrefs(preferences: SharedPreferences, gbDevice: GBDevice) : DevicePrefs(preferences, gbDevice) {
    companion object {
        const val PREF_PAIRING_ID = "dji_pairing_id"
        const val PREF_PAIRING_PIN = "dji_pairing_pin"
    }

    fun getOrCreatePairingId(): String {
        return getOrCreateRandomDecimal(PREF_PAIRING_ID, 15)
    }

    fun getOrCreatePairingPin(): String {
        return getOrCreateRandomDecimal(PREF_PAIRING_PIN, 4)
    }

    private fun getOrCreateRandomDecimal(key: String, digits: Int): String {
        val existingValue = getString(key, "")
        if (!existingValue.isEmpty() && existingValue.length == digits) {
            return existingValue
        }
        val newValue = buildString(digits) { repeat(digits) { append(Random.nextInt(10)) } }
        preferences.edit {
            putString(key, newValue)
        }
        return newValue
    }
}
