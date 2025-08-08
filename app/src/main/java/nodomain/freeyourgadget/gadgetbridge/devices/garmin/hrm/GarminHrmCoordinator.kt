package nodomain.freeyourgadget.gadgetbridge.devices.garmin.hrm

import de.greenrobot.dao.AbstractDao
import de.greenrobot.dao.Property
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.generic_hr.GenericHeartRateActivitySampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSampleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_hr.GenericHeartRateSupport

/**
 * This class pulls most of the logic from the
 * {@link nodomain.freeyourgadget.gadgetbridge.devices.generic_hr.GenericHeartRateCoordinator}.
 */
abstract class GarminHrmCoordinator: AbstractBLEDeviceCoordinator() {
    override fun isExperimental(): Boolean {
        return true
    }

    override fun getBondingStyle(): Int {
        return BONDING_STYLE_NONE
    }

    override fun suggestUnbindBeforePair(): Boolean {
        // Not needed
        return false
    }

    override fun getManufacturer(): String {
        return "Garmin"
    }

    override fun getDeviceSupportClass(device: GBDevice?): Class<out DeviceSupport> {
        return GenericHeartRateSupport::class.java
    }

    override fun getDefaultIconResource(): Int {
        return R.drawable.ic_device_lovetoy
    }

    override fun supportsHeartRateMeasurement(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActivityTracking(device: GBDevice): Boolean {
        return true
    }

    override fun supportsActivityTabs(device: GBDevice): Boolean {
        return false
    }

    override fun supportsSleepMeasurement(device: GBDevice): Boolean {
        return false
    }

    override fun supportsStepCounter(device: GBDevice): Boolean {
        return false
    }

    override fun supportsSpeedzones(device: GBDevice): Boolean {
        return false
    }

    override fun getSampleProvider(device: GBDevice, session: DaoSession): SampleProvider<out AbstractActivitySample>? {
        return GenericHeartRateActivitySampleProvider(device, session)
    }

    override fun getAllDeviceDao(session: DaoSession): MutableMap<AbstractDao<*, *>, Property> {
        return object : HashMap<AbstractDao<*, *>, Property>() {
            init {
                put(session.genericHeartRateSampleDao, GenericHeartRateSampleDao.Properties.DeviceId)
            }
        }
    }
}
