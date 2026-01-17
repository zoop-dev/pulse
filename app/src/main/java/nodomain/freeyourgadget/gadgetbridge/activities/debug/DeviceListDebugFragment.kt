package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice

class DeviceListDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences_empty, rootKey)
        preferenceScreen?.title = getString(R.string.bottom_nav_devices)
        reloadDevices()
    }

    private fun reloadDevices() {
        removeDynamicPrefs(preferenceScreen)

        val devices = GBApplication.app().deviceManager.devices

        if (!devices.isEmpty()) {
            for (device in devices) {
                addDynamicPref(
                    preferenceScreen,
                    device.aliasOrName,
                    device.address,
                    device.deviceCoordinator.defaultIconResource
                ) {
                    goTo(
                        DeviceDebugFragment().apply {
                            arguments = Bundle().apply {
                                putParcelable(GBDevice.EXTRA_DEVICE, device)
                            }
                        }
                    )
                }
            }
        } else {
            addDynamicPref(preferenceScreen, "", "No devices")
        }
    }
}
