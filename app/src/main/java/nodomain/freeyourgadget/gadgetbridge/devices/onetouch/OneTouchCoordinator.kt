package nodomain.freeyourgadget.gadgetbridge.devices.onetouch

import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GlucoseSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.onetouch.OneTouchSupport
import java.util.EnumSet
import java.util.regex.Pattern

class OneTouchCoordinator : AbstractBLEDeviceCoordinator() {
    override fun isExperimental(): Boolean {
        return true
    }

    override fun getManufacturer(): String {
        return "LifeScan"
    }

    override fun getDeviceSupportClass(device: GBDevice): Class<out DeviceSupport?> {
        return OneTouchSupport::class.java
    }

    override fun getInitialFlags(): EnumSet<ServiceDeviceSupport.Flags> {
        // Support class has message queuing, we don't need busy checking
        return EnumSet.noneOf(ServiceDeviceSupport.Flags::class.java)
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_BOND
    }

    override fun suggestUnbindBeforePair(): Boolean {
        return false
    }

    override fun getDefaultIconResource(): Int {
        // TODO dedicated icon
        return R.drawable.ic_device_default
    }

    override fun getSupportedDeviceName(): Pattern? {
        // Contains last 4 digits of the serial number
        return Pattern.compile("^OneTouch [A-Z0-9]{4}$")
    }

    override fun getDeviceNameResource(): Int {
        return R.string.devicetype_onetouch
    }

    override fun getDeviceKind(device: GBDevice): DeviceCoordinator.DeviceKind {
        return DeviceCoordinator.DeviceKind.GLUCOSE_METER
    }

    override fun getDeviceSpecificSettings(device: GBDevice): DeviceSpecificSettings {
        val settings = DeviceSpecificSettings()

        settings.addRootScreen(R.xml.devicesettings_glucose_limits)
        // FIXME: Read-only for now
        //settings.addConnectedPreferences(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_LOW)
        //settings.addConnectedPreferences(DeviceSettingsPreferenceConst.PREF_GLUCOSE_THRESHOLD_HIGH)

        return settings
    }

    override fun getDeviceSpecificSettingsCustomizer(device: GBDevice): DeviceSpecificSettingsCustomizer? {
        return OneTouchSettingsCustomizer()
    }

    override fun getAllDeviceDao(session: DaoSession): MutableMap<AbstractDao<*, *>, Property> {
        return object : HashMap<AbstractDao<*, *>, Property>() {
            init {
                put(session.glucoseSampleDao, GlucoseSampleDao.Properties.DeviceId)
            }
        }
    }

    override fun supportsDataFetching(device: GBDevice): Boolean {
        return true
    }

    override fun supportsCharts(device: GBDevice): Boolean {
        return false // TODO charts are not yet implemented
    }

    override fun supportsGlucoseMeasurement(device: GBDevice): Boolean {
        return true
    }
}
