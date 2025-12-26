package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.util.BondingUtil
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class CompanionDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_companion, rootKey)

        reloadDevices()
    }

    private fun reloadDevices() {
        val companionDevicesHeader: PreferenceCategory = findPreference(PREF_HEADER_COMPANION_DEVICES)!!
        val otherDevicesHeader: PreferenceCategory = findPreference(PREF_HEADER_OTHER_DEVICES)!!
        removeDynamicPrefs(companionDevicesHeader)
        removeDynamicPrefs(otherDevicesHeader)

        val manager = GBApplication.getContext().getSystemService(Context.COMPANION_DEVICE_SERVICE)
                as CompanionDeviceManager

        val associations: Set<String> = manager.associations.map { it.uppercase(Locale.ROOT) }.toSet()

        val companionDevices = associations
            .map {
                val deviceByAddress = GBApplication.app().deviceManager.getDeviceByAddress(it)
                Device(
                    it,
                    deviceByAddress?.aliasOrName ?: getString(R.string.unknown),
                    deviceByAddress?.deviceCoordinator?.defaultIconResource ?: R.drawable.ic_device_unknown
                )
            }
            .sortedBy { it.name }

        val otherDevices = GBApplication.app().deviceManager.devices
            .filter { !associations.contains(it.address.uppercase(Locale.ROOT)) }
            .map {
                Device(
                    it.address,
                    it.aliasOrName,
                    it.deviceCoordinator.defaultIconResource
                )
            }

        if (!companionDevices.isEmpty()) {
            for (device in companionDevices) {
                addDynamicPref(companionDevicesHeader, device.name, device.address, device.icon) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setCancelable(true)
                        .setIcon(device.icon)
                        .setTitle("Unpair companion device")
                        .setMessage("Unpair ${device.name} (${device.address}) from companion device?")
                        .setNeutralButton(R.string.Cancel) { _, _ -> }
                        .setPositiveButton(R.string.ok) { _, _ -> unpairCompanion(device) }
                        .show()
                }
            }
        } else {
            addDynamicPref(companionDevicesHeader, "", "No companion devices")
        }

        if (!otherDevices.isEmpty()) {
            otherDevicesHeader.isVisible = true
            for (device in otherDevices) {
                addDynamicPref(otherDevicesHeader, device.name, device.address, device.icon) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setCancelable(true)
                        .setIcon(device.icon)
                        .setTitle("Pair as companion")
                        .setMessage("Pair ${device.name} (${device.address}) as companion device?")
                        .setNeutralButton(R.string.Cancel) { _, _ -> }
                        .setPositiveButton(R.string.ok) { _, _ -> pairAsCompanion(device) }
                        .show()
                }
            }
        } else {
            otherDevicesHeader.isVisible = false
        }
    }

    private data class Device(val address: String, val name: String, val icon: Int)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_DEVICE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val deviceToPair = data?.getParcelableExtra<BluetoothDevice?>(CompanionDeviceManager.EXTRA_DEVICE)

            if (deviceToPair != null) {
                if (deviceToPair.getBondState() != BluetoothDevice.BOND_BONDED) {
                    GB.toast("Creating bond...", Toast.LENGTH_SHORT, GB.INFO)
                    deviceToPair.createBond()
                } else {
                    GB.toast("Bonding complete", Toast.LENGTH_LONG, GB.INFO)
                    reloadDevices()
                }
            } else {
                GB.toast("No device to pair", Toast.LENGTH_LONG, GB.ERROR)
            }
        }
    }

    private fun unpairCompanion(device: Device) {
        BondingUtil.Disassociate(requireContext(), device.address)
        reloadDevices()
    }

    private fun pairAsCompanion(device: Device) {
        val manager = requireActivity().getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

        if (manager.associations.contains(device.address)) {
            GB.toast(device.name + " already paired as companion", Toast.LENGTH_LONG, GB.INFO)
            return
        }

        val deviceFilter = BluetoothDeviceFilter.Builder()
            .setAddress(device.address)
            .build()

        val pairingRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(true)
            .build()

        val callback: CompanionDeviceManager.Callback = object : CompanionDeviceManager.Callback() {
            override fun onFailure(error: CharSequence?) {
                GB.toast("Companion pairing failed: $error", Toast.LENGTH_LONG, GB.ERROR)
            }

            override fun onAssociationPending(chooserLauncher: IntentSender) {
                GB.toast("Found device", Toast.LENGTH_SHORT, GB.INFO)

                try {
                    ActivityCompat.startIntentSenderForResult(
                        requireActivity(),
                        chooserLauncher,
                        SELECT_DEVICE_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (e: SendIntentException) {
                    LOG.error("Failed to send intent", e)
                }
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                GB.toast("Companion pairing success", Toast.LENGTH_SHORT, GB.INFO)
                reloadDevices()
            }
        }

        manager.associate(pairingRequest, callback, null)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CompanionDebugFragment::class.java)
        const val SELECT_DEVICE_REQUEST_CODE: Int = 1424

        private const val PREF_HEADER_COMPANION_DEVICES = "pref_header_companion_devices"
        private const val PREF_HEADER_OTHER_DEVICES = "pref_header_other_devices"
    }
}
