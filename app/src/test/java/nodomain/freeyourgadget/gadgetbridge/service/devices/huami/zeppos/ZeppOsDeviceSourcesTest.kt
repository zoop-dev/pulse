package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos

import org.junit.Assert.*
import org.junit.Test

class ZeppOsDeviceSourcesTest {
    @Test
    fun testNoDuplicateProductIdAndVersion() {
        // We need all (productId, productVersion) pairs to be distinct
        val pairs = ZeppOsDeviceSources.DEVICE_SOURCES.map { Pair(it.productId, it.productVersion) }
        val uniquePairs = pairs.toSet()
        assertEquals(
            "Found duplicate (productId, productVersion) pairs in DEVICE_SOURCES",
            pairs.size,
            uniquePairs.size
        )
    }
}
