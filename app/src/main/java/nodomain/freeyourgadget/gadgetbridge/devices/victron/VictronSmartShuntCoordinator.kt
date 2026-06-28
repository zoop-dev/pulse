package nodomain.freeyourgadget.gadgetbridge.devices.victron

import androidx.annotation.StringRes
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.deviceCardAction
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.victron.VictronSmartShuntSupport
import java.util.regex.Pattern

class VictronSmartShuntCoordinator : AbstractBLEDeviceCoordinator() {
    override fun getSupportedDeviceName(): Pattern {
        return Pattern.compile("^SmartShunt [A-Z0-9]+$")
    }

    override fun getManufacturer(): String {
        return "Victron"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport> {
        return VictronSmartShuntSupport::class.java
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_victron_smartshunt
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getBondingStyle(): Int {
        // Must be paired for the service to show up
        return BONDING_STYLE_BOND
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_car
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.BATTERY_MONITOR
    }

    override fun getCustomActions(): List<DeviceCardAction> = listOf(
        displayAction(R.string.consumed_electrical_energy, EXTRA_CONSUMED),
        displayAction(R.string.power_w, EXTRA_POWER),
        displayAction(R.string.electrical_current, EXTRA_CURRENT),
    )

    private fun displayAction(@StringRes descriptionRes: Int, extraKey: String) = deviceCardAction {
        icon = { R.drawable.ic_bolt }
        isVisible = { device -> device.isConnected && !(device.getExtraInfo(extraKey) as? String).isNullOrBlank() }
        description = { _, context -> context.getString(descriptionRes) }
        label = { device, _ -> device.getExtraInfo(extraKey) as? String ?: "" }
        onClick = { _, _ -> }
    }

    companion object {
        const val EXTRA_CONSUMED = "consumed"
        const val EXTRA_POWER = "power"
        const val EXTRA_CURRENT = "current"
    }
}
