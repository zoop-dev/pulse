package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType
import nodomain.freeyourgadget.gadgetbridge.util.DeviceTypeDialog
import nodomain.freeyourgadget.gadgetbridge.util.GB
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
        if (gbDevice.alias.isNullOrEmpty()) {
            findPreference<Preference>(PREF_DEBUG_DEVICE_ALIAS)?.summary = getString(R.string.not_set)
        } else {
            findPreference<Preference>(PREF_DEBUG_DEVICE_ALIAS)?.summary = gbDevice.alias ?: getString(R.string.not_set)
        }

        // TODO: MAC Address
        findPreference<EditTextPreference>(PREF_DEBUG_DEVICE_MAC_ADDRESS)?.text = gbDevice.address
        findPreference<EditTextPreference>(PREF_DEBUG_DEVICE_MAC_ADDRESS)?.isSelectable = false
        //findPreference<EditTextPreference>(PREF_DEBUG_DEVICE_MAC_ADDRESS)!!.setOnBindEditTextListener { editText ->
        //    editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        //    editText.filters = arrayOf(MacAddressInputFilter())
        //    editText.addTextChangedListener(MacAddressTextWatcher(editText))
        //}
        //findPreference<EditTextPreference>(PREF_DEBUG_DEVICE_MAC_ADDRESS)!!.setOnPreferenceChangeListener { _: Preference?, newMacAddress: Any? ->
        //    setDeviceMacAddress(gbDevice, newMacAddress as String)
        //}

        // Device Type
        findPreference<Preference>(PREF_DEBUG_DEVICE_TYPE)?.summary = gbDevice.type.name
        findPreference<Preference>(PREF_DEBUG_DEVICE_TYPE)?.setOnPreferenceClickListener {
            DeviceTypeDialog(requireActivity(), R.string.set_device_type, gbDevice.address)
                .show(gbDevice.type) { _: String, deviceType: DeviceType ->
                    setDeviceType(gbDevice, deviceType)
                }
            true
        }

        val headerDetails = findPreference<PreferenceCategory>(PREF_HEADER_DETAILS)
        for (detail in gbDevice.deviceInfos) {
            if (detail.name.startsWith("ADDR:")) {
                // We already show the address above
                continue
            }
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

    private fun setDeviceMacAddress(gbDevice: GBDevice, newAddress: String): Boolean {
        // TODO
        return false
    }

    private fun setDeviceType(gbDevice: GBDevice, newType: DeviceType) {
        LOG.debug("Setting device {} type from {} to {}", gbDevice.address, gbDevice.type, newType)

        if (gbDevice.type == newType) {
            GB.toast("Type is the same!", Toast.LENGTH_SHORT, GB.INFO)
            return
        }

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.getDaoSession()
                DBHelper.updateDeviceType(session, gbDevice.address, newType)
            }
        } catch (e: java.lang.Exception) {
            LOG.error("Failed to update device type", e)
            GB.toast("Failed to update device type", Toast.LENGTH_LONG, GB.ERROR)
            return
        }

        LOG.info("Restarting GB after device type update")

        restart()
    }

    private fun restart() {
        MaterialAlertDialogBuilder(requireActivity())
            .setCancelable(false)
            .setIcon(R.drawable.ic_sync)
            .setTitle(R.string.backup_restore_restart_title)
            .setMessage(getString(R.string.backup_restore_restart_summary, getString(R.string.app_name)))
            .setOnCancelListener((DialogInterface.OnCancelListener { _: DialogInterface? ->
                requireActivity().finishAffinity()
                GBApplication.restart()
            }))
            .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                requireActivity().finishAffinity()
                GBApplication.restart()
            }.show()
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
