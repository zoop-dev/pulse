package nodomain.freeyourgadget.gadgetbridge.activities

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityUsbConnectBinding
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractUsbDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper
import nodomain.freeyourgadget.gadgetbridge.util.DeviceTypeDialog
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getParcelableCompat
import org.slf4j.LoggerFactory

class UsbAccessoryConnectActivity : AbstractGBActivity() {

    private lateinit var accessory: UsbAccessory

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return

            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (granted) {
                connectAndFinish(accessory)
            } else {
                LOG.warn("USB accessory permission denied for {}", accessory)
                Toast.makeText(
                    this@UsbAccessoryConnectActivity,
                    R.string.usb_connect_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityUsbConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val accessory = intent.getParcelableCompat<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
        if (accessory == null) {
            LOG.error("USB accessory extra is null")
            finish()
            return
        }
        this.accessory = accessory

        binding.textAccessoryManufacturer.text = accessory.manufacturer
        binding.textAccessoryModel.text = accessory.model
        binding.textAccessoryDescription.text = accessory.description

        binding.buttonConnect.setOnClickListener {
            requestPermission(accessory)
        }

        ContextCompat.registerReceiver(
            this,
            permissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(permissionReceiver)
        } catch (_: IllegalArgumentException) {
            // never registered, or already unregistered
        }
        super.onDestroy()
    }

    private fun requestPermission(accessory: UsbAccessory) {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        if (usbManager.hasPermission(accessory)) {
            connectAndFinish(accessory)
            return
        }

        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(BuildConfig.APPLICATION_ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(accessory, permissionIntent)
    }

    private fun connectAndFinish(accessory: UsbAccessory) {
        val serial = accessory.serial
        if (serial == null) {
            LOG.warn("USB accessory has no serial number")
            return
        }

        val address = "usb:$serial"

        val existingDevice = GBApplication.app().deviceManager.getDeviceByAddress(address)
        if (existingDevice != null) {
            // Already paired (possibly with a forced type, if it was unsupported at pairing time)
            // no need to resolve or prompt for a type again.
            connectAndFinish(existingDevice)
            return
        }

        val resolvedType = DeviceHelper.getInstance().resolveDeviceType(accessory)
        if (!resolvedType.isSupported) {
            // Unsupported device, allow user to select an USB DeviceType
            LOG.warn("Unsupported usb accessory {}", accessory)
            DeviceTypeDialog(
                this,
                R.string.usb_connect_select_device_type,
                address
            ) { it.deviceCoordinator is AbstractUsbDeviceCoordinator }
                .show(null) { selectedAddress, selectedType ->
                    DeviceHelper.getInstance().setForcedDeviceType(selectedAddress, selectedType)
                    connectAndFinish(
                        GBDevice(
                            selectedAddress,
                            accessory.description ?: selectedType.name,
                            null,
                            null,
                            selectedType
                        )
                    )
                }
            return
        }

        connectAndFinish(GBDevice(address, accessory.description ?: resolvedType.name, null, null, resolvedType))
    }

    private fun connectAndFinish(device: GBDevice) {
        GBApplication.deviceService(device).connect(true)

        val mainScreenIntent = Intent(this, ControlCenterv2::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(mainScreenIntent)
        finish()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UsbAccessoryConnectActivity::class.java)

        private const val ACTION_USB_PERMISSION = "nodomain.freeyourgadget.gadgetbridge.USB_PERMISSION"
    }
}
