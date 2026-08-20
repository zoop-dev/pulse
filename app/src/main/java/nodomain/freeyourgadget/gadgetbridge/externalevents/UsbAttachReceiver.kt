package nodomain.freeyourgadget.gadgetbridge.externalevents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import nodomain.freeyourgadget.gadgetbridge.activities.UsbAccessoryConnectActivity
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getParcelableCompat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UsbAttachReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val accessory = intent.getParcelableCompat<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)

        LOG.debug("Got {} for {}", intent.action, accessory)

        if (accessory == null) {
            // Should never happen?
            return
        }

        val deviceType = DeviceHelper.getInstance().resolveDeviceType(accessory)
        if (deviceType == null) {
            LOG.warn("Unsupported usb accessory {}", accessory)
            return
        }

        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == intent.action) {
            val connectIntent = Intent(context, UsbAccessoryConnectActivity::class.java)
                .putExtra(UsbManager.EXTRA_ACCESSORY, accessory)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(connectIntent)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(UsbAttachReceiver::class.java)
    }
}
