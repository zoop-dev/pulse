package nodomain.freeyourgadget.gadgetbridge.devices.victron

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
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

    override fun getCustomActions(): List<DeviceCardAction> {
        return listOf(
            // Consumed
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_bolt
                }

                override fun isVisible(device: GBDevice): Boolean {
                    val value = device.getExtraInfo(EXTRA_CONSUMED) as? String
                    return device.isConnected && !value.isNullOrBlank()
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.consumed_electrical_energy)
                }

                override fun getLabel(device: GBDevice, context: Context): String {
                    return device.getExtraInfo(EXTRA_CONSUMED) as? String ?: ""
                }

                override fun onClick(device: GBDevice, context: Context) {
                    // No UI for this yet
                }
            },

            // Power
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_bolt
                }

                override fun isVisible(device: GBDevice): Boolean {
                    val value = device.getExtraInfo(EXTRA_POWER) as? String
                    return device.isConnected && !value.isNullOrBlank()
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.power_w)
                }

                override fun getLabel(device: GBDevice, context: Context): String {
                    return device.getExtraInfo(EXTRA_POWER) as? String ?: ""
                }

                override fun onClick(device: GBDevice, context: Context) {
                    // No UI for this yet
                }
            },

            // Current
            object : DeviceCardAction {
                override fun getIcon(device: GBDevice): Int {
                    return R.drawable.ic_bolt
                }

                override fun isVisible(device: GBDevice): Boolean {
                    val value = device.getExtraInfo(EXTRA_CURRENT) as? String
                    return device.isConnected && !value.isNullOrBlank()
                }

                override fun getDescription(device: GBDevice, context: Context): String {
                    return context.getString(R.string.electrical_current)
                }

                override fun getLabel(device: GBDevice, context: Context): String {
                    return device.getExtraInfo(EXTRA_CURRENT) as? String ?: ""
                }

                override fun onClick(device: GBDevice, context: Context) {
                    // No UI for this yet
                }
            }
        )
    }

    companion object {
        const val EXTRA_CONSUMED = "consumed"
        const val EXTRA_POWER = "power"
        const val EXTRA_CURRENT = "current"
    }
}
