package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DeviceDebugFragment : AbstractDebugFragment() {
    private lateinit var gbDevice: GBDevice

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_device, rootKey)

        @Suppress("DEPRECATION")
        gbDevice = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(GBDevice.EXTRA_DEVICE, GBDevice::class.java)!!
        } else {
            arguments?.getParcelable<GBDevice>(GBDevice.EXTRA_DEVICE)!!
        }

        preferenceScreen?.title = gbDevice.aliasOrName

        findPreference<Preference>(PREF_DEBUG_DEVICE_NAME)?.summary = gbDevice.name ?: "<null>"
        findPreference<Preference>(PREF_DEBUG_DEVICE_ALIAS)?.summary = gbDevice.alias ?: getString(R.string.not_set)
        findPreference<Preference>(PREF_DEBUG_DEVICE_MAC_ADDRESS)?.summary = gbDevice.address
        findPreference<Preference>(PREF_DEBUG_DEVICE_TYPE)?.summary = gbDevice.type.name

        val headerDetails = findPreference<PreferenceCategory>(PREF_HEADER_DETAILS)
        for (detail in gbDevice.deviceInfos) {
            addDynamicPref(headerDetails, detail.name.replace(": *$".toRegex(), ""), detail.details)
        }

        val headerCoordinator = findPreference<PreferenceCategory>(PREF_HEADER_COORDINATOR)
        val coordinator = gbDevice.deviceCoordinator

        // Use reflection to find all supports* methods that return boolean
        val coordinatorClass = coordinator.javaClass
        val methods = coordinatorClass.methods
            .filter { it.name.startsWith("supports") }
            .filter { it.returnType == Boolean::class.javaPrimitiveType || it.returnType == Boolean::class.java }
            .filter { it.parameterCount == 0 || (it.parameterCount == 1 && it.parameterTypes[0] == GBDevice::class.java) }
            .sortedBy { it.name }

        for (method in methods) {
            try {
                val result = if (method.parameterCount == 0) {
                    method.invoke(coordinator) as Boolean
                } else {
                    method.invoke(coordinator, gbDevice) as Boolean
                }
                addDynamicCheckbox(headerCoordinator, method.name, checked = result)
            } catch (e: Exception) {
                LOG.error("Error invoking method ${method.name}", e)
                addDynamicPref(headerCoordinator, method.name, "Error: ${e.message}")
            }
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DeviceDebugFragment::class.java)

        private const val PREF_DEBUG_DEVICE_NAME = "pref_debug_device_name"
        private const val PREF_DEBUG_DEVICE_ALIAS = "pref_debug_device_alias"
        private const val PREF_DEBUG_DEVICE_MAC_ADDRESS = "pref_debug_device_mac_address"
        private const val PREF_DEBUG_DEVICE_TYPE = "pref_debug_device_type"
        private const val PREF_HEADER_DETAILS = "pref_header_details"
        private const val PREF_HEADER_COORDINATOR = "pref_header_coordinator"
    }
}
