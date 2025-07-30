package nodomain.freeyourgadget.gadgetbridge.service.devices.shokz

import org.junit.Assert.*
import org.junit.Test

class ShokzSupportTest {
    @Test
    fun testEncodeCommand() {
        assertEquals(
            "a55a2c000100000028005ab701000000200000000200000004000000020000000300000004000000020000000400000000000000",
            ShokzSupport.encodeCommand(ShokzCommand.FIRMWARE_GET).toHexString()
        )
    }
}
