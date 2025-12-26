package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec

class CallsDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setupPreferences()
    }

    private fun setupPreferences() {
        setPreferencesFromResource(R.xml.debug_preferences_calls, null)

        onClick(PREF_DEBUG_CALL_SEND) { sendCallSpec() }
        onClick(PREF_DEBUG_CALL_RESET) { resetPreferences() }

        setListPreferenceEntries(
            PREF_DEBUG_CALL_COMMAND,
            CallSpec.Command.entries.map { it.name }.toTypedArray()
        )
    }

    private fun sendCallSpec() {
        val sharedPreferences = preferenceManager.sharedPreferences!!
        val callSpec = CallSpec()

        callSpec.command =
            CallSpec.Command.valueOf(sharedPreferences.getString(PREF_DEBUG_CALL_COMMAND, "INCOMING")!!).ordinal
        callSpec.number = sharedPreferences.getString(PREF_DEBUG_CALL_NUMBER, "6365553226")
        callSpec.name = sharedPreferences.getString(PREF_DEBUG_CALL_NAME, "Mr. Plow")
        callSpec.sourceName = sharedPreferences.getString(PREF_DEBUG_CALL_SOURCE_NAME, null)
        callSpec.sourceAppId = sharedPreferences.getString(PREF_DEBUG_CALL_SOURCE_APP_ID, null)
        callSpec.key = sharedPreferences.getString(PREF_DEBUG_CALL_KEY, null)
        callSpec.channelId = sharedPreferences.getString(PREF_DEBUG_CALL_CHANNEL_ID, null)
        callSpec.category = sharedPreferences.getString(PREF_DEBUG_CALL_CATEGORY, null)
        callSpec.isVoip = sharedPreferences.getBoolean(PREF_DEBUG_CALL_IS_VOIP, false)
        callSpec.dndSuppressed = if (sharedPreferences.getBoolean(PREF_DEBUG_CALL_DND_SUPPRESSED_BOOL, false)) 1 else 0

        runOnDebugDevices("Send CallSpec") {
            GBApplication.deviceService(it).onSetCallState(callSpec)
        }
    }

    private fun resetPreferences() {
        preferenceScreen.removeAll()

        preferenceManager.sharedPreferences!!.edit(true) {
            remove(PREF_DEBUG_CALL_COMMAND)
            remove(PREF_DEBUG_CALL_NUMBER)
            remove(PREF_DEBUG_CALL_NAME)
            remove(PREF_DEBUG_CALL_SOURCE_NAME)
            remove(PREF_DEBUG_CALL_SOURCE_APP_ID)
            remove(PREF_DEBUG_CALL_KEY)
            remove(PREF_DEBUG_CALL_CHANNEL_ID)
            remove(PREF_DEBUG_CALL_CATEGORY)
            remove(PREF_DEBUG_CALL_IS_VOIP)
            remove(PREF_DEBUG_CALL_DND_SUPPRESSED_BOOL)
        }

        // Reload the preference screen to reflect the changes
        setupPreferences()
    }

    companion object {
        private const val PREF_DEBUG_CALL_SEND = "pref_debug_call_send"
        private const val PREF_DEBUG_CALL_RESET = "pref_debug_call_reset"
        private const val PREF_DEBUG_HEADER_CALLSPEC = "pref_debug_header_callspec"
        private const val PREF_DEBUG_CALL_COMMAND = "pref_debug_call_command"
        private const val PREF_DEBUG_CALL_NUMBER = "pref_debug_call_number"
        private const val PREF_DEBUG_CALL_NAME = "pref_debug_call_name"
        private const val PREF_DEBUG_CALL_SOURCE_NAME = "pref_debug_call_source_name"
        private const val PREF_DEBUG_CALL_SOURCE_APP_ID = "pref_debug_call_source_app_id"
        private const val PREF_DEBUG_CALL_KEY = "pref_debug_call_key"
        private const val PREF_DEBUG_CALL_CHANNEL_ID = "pref_debug_call_channel_id"
        private const val PREF_DEBUG_CALL_CATEGORY = "pref_debug_call_category"
        private const val PREF_DEBUG_CALL_IS_VOIP = "pref_debug_call_is_voip"
        private const val PREF_DEBUG_CALL_DND_SUPPRESSED_BOOL = "pref_debug_call_dnd_suppressed_bool"
    }
}
