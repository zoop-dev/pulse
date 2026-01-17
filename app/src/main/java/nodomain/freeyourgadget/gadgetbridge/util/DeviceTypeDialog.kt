package nodomain.freeyourgadget.gadgetbridge.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.databinding.DialogTestDeviceBinding
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType
import nodomain.freeyourgadget.gadgetbridge.util.preferences.MacAddressInputFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Random
import java.util.TreeMap
import kotlin.collections.map

class DeviceTypeDialog(
    private val activity: Activity,
    private val dialogTitle: Int,
    private val macAddress: String?
) {
    var selectedTestDeviceMAC = macAddress ?: randomMac()
    var selectedTestDeviceKey = -1L
    private lateinit var updateOkButtonState: (() -> Unit)

    fun show(preselectedType: DeviceType? = null, onSelection: (String, DeviceType) -> Unit) {
        val binding = DialogTestDeviceBinding.inflate(activity.layoutInflater)

        val allDevicesByName: MutableMap<String, DeviceTypeWithIcon> = getAllSupportedDevices(activity)

        // Get unique manufacturers
        val manufacturerMap = mutableMapOf<String, MutableList<Map.Entry<String, DeviceTypeWithIcon>>>()
        val manufacturers = mutableSetOf<String>()
        for (entry in allDevicesByName.entries) {
            val manufacturer = entry.value.deviceType.getDeviceCoordinator().getManufacturer()
            manufacturers.add(manufacturer)
            manufacturerMap.getOrPut(manufacturer) { mutableListOf() }.add(entry)
        }

        val manufacturerPriority = listOf(
            "Gadgetbridge",
            activity.getString(R.string.pref_header_generic),
            activity.getString(R.string.unknown)
        )
        val sortedManufacturers = manufacturers.toList().sortedWith(
            compareBy<String> { item ->
                val index = manufacturerPriority.indexOfFirst { it.equals(item, ignoreCase = true) }
                if (index >= 0) index else Int.MAX_VALUE
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it }
        )
        val manufacturerListArray = ArrayList<String>()
        manufacturerListArray.add(activity.getString(R.string.all_manufacturers))
        manufacturerListArray.addAll(sortedManufacturers)

        // Create full device list
        val allDeviceListArray = allDevicesByName.entries
            .map { SpinnerWithIconItem(it.key, it.value.deviceType.ordinal.toLong(), it.value.icon) }
            .toCollection(ArrayList())

        val currentDeviceList = ArrayList(allDeviceListArray)

        val deviceListAdapter = SpinnerWithIconAdapter(
            activity,
            R.layout.spinner_with_image_layout,
            R.id.spinner_item_text,
            currentDeviceList
        )
        binding.deviceTypeDropdown.setAdapter(deviceListAdapter)
        binding.deviceTypeDropdown.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as SpinnerWithIconItem
            selectedTestDeviceKey = selectedItem.getId()
            updateOkButtonState.invoke()
        }

        // Set up manufacturer dropdown
        binding.deviceManufacturerDropdown.setAdapter(
            ArrayAdapter(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                manufacturerListArray
            )
        )
        binding.deviceManufacturerDropdown.setText(activity.getString(R.string.all_manufacturers), false)
        binding.deviceManufacturerDropdown.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedManufacturer = parent.getItemAtPosition(position) as String

                // Filter device list based on selected manufacturer
                currentDeviceList.clear()
                if (position == 0) {
                    // Show all devices
                    currentDeviceList.addAll(allDeviceListArray)
                } else {
                    // Show only devices from selected manufacturer
                    val filteredDevices = getFilteredDevices(selectedManufacturer, manufacturerMap)
                    currentDeviceList.addAll(filteredDevices)
                }
                deviceListAdapter.notifyDataSetChanged()

                // Reset device selection
                binding.deviceTypeDropdown.setText("", false)
                selectedTestDeviceKey = -1L
                updateOkButtonState.invoke()
            }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(true)
            .setTitle(dialogTitle)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                onSelection.invoke(
                    binding.macEditText.text.toString(),
                    DeviceType.entries[selectedTestDeviceKey.toInt()]
                )
            }
            .setNegativeButton(R.string.Cancel) { _, _ -> }
            .create()

        // Validate and update OK button state
        updateOkButtonState = {
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            val isMacValid = isValidMacAddress(binding.macEditText.text.toString())
            val isDeviceSelected = selectedTestDeviceKey != -1L
            val typeIsDifferentFromDefault =
                preselectedType == null || preselectedType.ordinal.toLong() != selectedTestDeviceKey
            positiveButton?.isEnabled = isMacValid && isDeviceSelected && typeIsDifferentFromDefault
        }

        dialog.setOnShowListener {
            updateOkButtonState.invoke()
        }

        setupMacAddressInput(binding.macEditText)

        // Preselect manufacturer and device type if provided
        preselectedType?.let { deviceType ->
            val coordinator = deviceType.getDeviceCoordinator()
            val manufacturer = when (coordinator.getManufacturer()) {
                "Unknown" -> activity.getString(R.string.unknown)
                "Generic" -> activity.getString(R.string.pref_header_generic)
                else -> coordinator.getManufacturer()
            }

            // Set manufacturer dropdown
            if (manufacturer in sortedManufacturers) {
                binding.deviceManufacturerDropdown.setText(manufacturer, false)

                // Filter device list based on selected manufacturer
                currentDeviceList.clear()
                val filteredDevices = getFilteredDevices(manufacturer, manufacturerMap)
                currentDeviceList.addAll(filteredDevices)
                deviceListAdapter.notifyDataSetChanged()
            }

            // Find and set the device type in the list
            val matchingItem = currentDeviceList.find { it.getId() == deviceType.ordinal.toLong() }
            matchingItem?.let {
                binding.deviceTypeDropdown.setText(it.getText(), false)
                selectedTestDeviceKey = it.getId()
                updateOkButtonState.invoke()
            }
        }

        dialog.show()
    }

    private fun getFilteredDevices(
        manufacturer: String,
        manufacturerMap: Map<String, List<Map.Entry<String, DeviceTypeWithIcon>>>
    ): List<SpinnerWithIconItem> {
        return manufacturerMap[manufacturer]?.map {
            SpinnerWithIconItem(
                it.key.replace("^${manufacturer} ".toRegex(), "")
                    .replace(" \\(${manufacturer}\\)$".toRegex(), ""),
                it.value.deviceType.ordinal.toLong(),
                it.value.icon
            )
        } ?: emptyList()
    }

    private fun getAllSupportedDevices(context: Context): MutableMap<String, DeviceTypeWithIcon> {
        var newMap = LinkedHashMap<String, DeviceTypeWithIcon>(1)
        for (deviceType in DeviceType.entries) {
            val coordinator = deviceType.getDeviceCoordinator()
            val icon = coordinator.getDefaultIconResource()
            var name = context.getString(coordinator.getDeviceNameResource())
            val manufacturer = when (coordinator.getManufacturer()) {
                "Unknown" -> context.getString(R.string.unknown)
                "Generic" -> context.getString(R.string.pref_header_generic)
                else -> coordinator.getManufacturer()
            }
            if (!name.startsWith(manufacturer)) {
                name += " (${manufacturer})"
            }
            newMap[name] = DeviceTypeWithIcon(deviceType, icon)
        }

        val sortedMap = TreeMap<String, DeviceTypeWithIcon>(String.CASE_INSENSITIVE_ORDER)
        sortedMap.putAll(newMap)
        newMap = LinkedHashMap(sortedMap.size + 3)

        // Ensure some devices are first
        //newMap[context.getString(R.string.devicetype_scannable)] =
        //    DeviceTypeWithIcon(DeviceType.SCANNABLE, R.drawable.ic_device_scannable)
        //newMap[context.getString(R.string.devicetype_ble_gatt_client)] =
        //    DeviceTypeWithIcon(DeviceType.BLE_GATT_CLIENT, R.drawable.ic_device_scannable)

        newMap.putAll(sortedMap)

        return newMap
    }

    private fun setupMacAddressInput(editText: EditText) {
        editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        if (macAddress != null) {
            editText.isEnabled = false
        }

        editText.filters = arrayOf(MacAddressInputFilter())

        editText.setText(selectedTestDeviceMAC)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                selectedTestDeviceMAC = editable.toString()
                updateOkButtonState.invoke()
            }
        })
    }

    private data class DeviceTypeWithIcon(val deviceType: DeviceType, val icon: Int)

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DeviceTypeDialog::class.java)

        private fun isValidMacAddress(mac: String): Boolean {
            if (BuildConfig.INTERNET_ACCESS) {
                // For builds with internet access (Bangle.js), allow more flexible formats
                return mac.isNotEmpty()
            }
            // Standard MAC address validation: XX:XX:XX:XX:XX:XX
            val macRegex = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$".toRegex()
            return macRegex.matches(mac)
        }

        private fun randomMac(): String {
            val random = Random()
            return String.format(
                "%02X:%02X:%02X:%02X:%02X:%02X",
                random.nextInt(0xff),
                random.nextInt(0xff),
                random.nextInt(0xff),
                random.nextInt(0xff),
                random.nextInt(0xff),
                random.nextInt(0xff)
            )
        }

        fun createTestDevice(
            context: Context,
            deviceType: DeviceType,
            deviceMac: String,
            inputDeviceName: String?
        ) {
            val deviceNameResource = deviceType.getDeviceCoordinator().getDeviceNameResource()
            val deviceName: String =
                inputDeviceName ?: if (deviceNameResource == 0) deviceType.name else context.getString(
                    deviceNameResource
                )

            try {
                GBApplication.acquireDB().use { db ->
                    val daoSession = db.getDaoSession()
                    val gbDevice = GBDevice(deviceMac, deviceName, "", null, deviceType)
                    gbDevice.firmwareVersion = "N/A"
                    gbDevice.firmwareVersion2 = "N/A"

                    //this causes the attributes (fw version) to be stored as well. Not much useful, but still...
                    gbDevice.setState(GBDevice.State.INITIALIZED)

                    DBHelper.getDevice(gbDevice, daoSession) //the addition happens here

                    val refreshIntent = Intent(DeviceManager.ACTION_REFRESH_DEVICELIST)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent)
                    GB.toast(context, "Added test device: $deviceName", Toast.LENGTH_SHORT, GB.INFO)
                }
            } catch (e: Exception) {
                LOG.error("Error accessing database", e)
            }
        }
    }
}
