package nodomain.freeyourgadget.gadgetbridge.service.devices.onetouch

import nodomain.freeyourgadget.gadgetbridge.test.TestBase
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OneTouchMessageTest: TestBase() {

    // ==================== TimeGet Tests ====================
    @Test
    fun testTimeGetEncode() {
        val message = OneTouchMessage.TimeGet
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("0102090004200203F9C3")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ReadingCountGet Tests ====================
    @Test
    fun testReadingCountGetEncode() {
        val message = OneTouchMessage.ReadingCountGet
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020900042700030B20")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ThresholdLowGet Tests ====================
    @Test
    fun testThresholdLowGetEncode() {
        val message = OneTouchMessage.ThresholdLowGet
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020A00040A020903E048")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ThresholdHighGet Tests ====================
    @Test
    fun testThresholdHighGetEncode() {
        val message = OneTouchMessage.ThresholdHighGet
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020A00040A020A03B31D")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ThresholdLowSet Tests ====================
    @Test
    fun testThresholdLowSetEncode() {
        val message = OneTouchMessage.ThresholdLowSet(70)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020E00030A010946000000035C71")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ThresholdHighSet Tests ====================
    @Test
    fun testThresholdHighSetEncode() {
        val message = OneTouchMessage.ThresholdHighSet(180)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020E00030A010AB40000000369C4")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ReadingGet Tests ====================
    @Test
    fun testReadingGetEncode_offset0() {
        val message = OneTouchMessage.ReadingGet(0)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C0004310200000003A955")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset1() {
        val message = OneTouchMessage.ReadingGet(1)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C00043102010000031D23")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset2() {
        val message = OneTouchMessage.ReadingGet(2)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C0004310202000003C1B8")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset45() {
        val message = OneTouchMessage.ReadingGet(45)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C000431022D000003615B")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset46() {
        val message = OneTouchMessage.ReadingGet(46)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C000431022E000003BDC0")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset47() {
        val message = OneTouchMessage.ReadingGet(47)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C000431022F00000309B6")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset48() {
        val message = OneTouchMessage.ReadingGet(48)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C00043102300000034079")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testReadingGetEncode_offset61() {
        val message = OneTouchMessage.ReadingGet(61)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020C000431023D000003C640")

        assertArrayEquals(expected, encoded)
    }

    // ==================== TimeSet Tests ====================
    @Test
    fun testTimeSetEncode_2026_01_04_22_36_20() {
        // 2026-01-04 22:36:20
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        calendar.set(2026, Calendar.JANUARY, 4, 22, 36, 20)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val message = OneTouchMessage.TimeSet(calendar.timeInMillis)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020D00042001E4A7ED30037B06")

        assertArrayEquals(expected, encoded)
    }

    @Test
    fun testTimeSetEncode_2026_01_04_23_36_13() {
        // 2026-01-04 23:36:13
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        calendar.set(2026, Calendar.JANUARY, 4, 23, 36, 13)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val message = OneTouchMessage.TimeSet(calendar.timeInMillis)
        val encoded = message.encode()
        val expected = GB.hexStringToByteArray("01020D00042001EDB5ED3003C858")

        assertArrayEquals(expected, encoded)
    }

    // ==================== ReadingRet Decode Tests ====================
    @Test
    fun testReadingRetDecode_2018_12_18() {
        // ReadingRet: 2018-12-18 18:13:00 - 92
        val packet = GB.hexStringToByteArray("0202180004063D000003002CF5AB235C0000002D0B0003F84D")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(92, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2018, cal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH))
        assertEquals(18, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2018_12_19() {
        // ReadingRet: 2018-12-19 11:08:04 - 221
        val packet = GB.hexStringToByteArray("0202180004063A0000060014E3AC23DD000000FD0A00039866")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(221, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2018, cal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH))
        assertEquals(19, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2018_12_23() {
        // ReadingRet: 2018-12-23 18:25:02 - 95
        val packet = GB.hexStringToByteArray("0202180004063400000C007E8FB2235F000000F40A00039B01")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(95, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2018, cal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH))
        assertEquals(23, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2019_01_01() {
        // ReadingRet: 2019-01-01 12:01:06 - 196
        val packet = GB.hexStringToByteArray("02021800040624000027000213BE23C4000000B70A0003AB5D")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(196, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2019, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2019_02_09() {
        // ReadingRet: 2019-02-09 14:11:20 - 226
        val packet = GB.hexStringToByteArray("0202180004061D00003500089CF123E2000000320B0003F14E")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(226, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2019, cal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(9, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2019_03_20() {
        // ReadingRet: 2019-03-20 16:14:22 - 105
        val packet = GB.hexStringToByteArray("0202180004061500003D005E232524690000003F0B0003F432")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(105, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2019, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testReadingRetDecode_2019_04_05() {
        // ReadingRet: 2019-04-05 09:09:02 - 98
        val packet = GB.hexStringToByteArray("0202180004060F000043009EC93924620000002F0B000393B6")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingGet(0))

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingRet)
        
        val readingRet = message as OneTouchMessage.ReadingRet
        assertEquals(98, readingRet.value)
        
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = readingRet.timestampMillis
        assertEquals(2019, cal.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH))
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH))
    }

    // ==================== TimeRet Decode Tests ====================
    @Test
    fun testTimeRetDecode_2026_01_04_18_18_22() {
        // TimeRet: 2026-01-04 18:18:22
        val packet = GB.hexStringToByteArray("01020C0004066E6BED3003AB80")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.TimeRet)
        
        val timeRet = message as OneTouchMessage.TimeRet
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = timeRet.timestampMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(18, cal.get(Calendar.MINUTE))
        assertEquals(22, cal.get(Calendar.SECOND))
    }

    @Test
    fun testTimeRetDecode_2026_01_04_18_19_24() {
        // TimeRet: 2026-01-04 18:19:24
        val packet = GB.hexStringToByteArray("01020C000406AC6BED300390F7")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.TimeRet)
        
        val timeRet = message as OneTouchMessage.TimeRet
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = timeRet.timestampMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(19, cal.get(Calendar.MINUTE))
        assertEquals(24, cal.get(Calendar.SECOND))
    }

    @Test
    fun testTimeRetDecode_2026_01_04_18_28_25() {
        // TimeRet: 2026-01-04 18:28:25
        val packet = GB.hexStringToByteArray("01020C000406C96DED300382EA")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.TimeRet)
        
        val timeRet = message as OneTouchMessage.TimeRet
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = timeRet.timestampMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(28, cal.get(Calendar.MINUTE))
        assertEquals(25, cal.get(Calendar.SECOND))
    }

    @Test
    fun testTimeRetDecode_2026_01_04_18_29_14() {
        // TimeRet: 2026-01-04 18:29:14
        val packet = GB.hexStringToByteArray("01020C000406FA6DED3003BE08")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.TimeRet)
        
        val timeRet = message as OneTouchMessage.TimeRet
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = timeRet.timestampMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(29, cal.get(Calendar.MINUTE))
        assertEquals(14, cal.get(Calendar.SECOND))
    }

    @Test
    fun testTimeRetDecode_2026_01_04_22_36_49() {
        // TimeRet: 2026-01-04 22:36:49
        val packet = GB.hexStringToByteArray("01020C00040601A8ED3003F6D4")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.TimeRet)
        
        val timeRet = message as OneTouchMessage.TimeRet
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeInMillis = timeRet.timestampMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(22, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(36, cal.get(Calendar.MINUTE))
        assertEquals(49, cal.get(Calendar.SECOND))
    }

    // ==================== ThresholdHighRet Decode Tests ====================
    @Test
    fun testThresholdHighRetDecode_180() {
        // ThresholdHighRet: 180
        val packet = GB.hexStringToByteArray("01020C000406B400000003DF51")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ThresholdHighGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ThresholdHighRet)
        
        val thresholdRet = message as OneTouchMessage.ThresholdHighRet
        assertEquals(180, thresholdRet.threshold)
    }

    @Test
    fun testThresholdHighRetDecode_181() {
        // ThresholdHighRet: 181
        val packet = GB.hexStringToByteArray("01020C000406B5000000038EFB")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ThresholdHighGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ThresholdHighRet)
        
        val thresholdRet = message as OneTouchMessage.ThresholdHighRet
        assertEquals(181, thresholdRet.threshold)
    }

    // ==================== ThresholdLowRet Decode Tests ====================
    @Test
    fun testThresholdLowRetDecode_69() {
        // ThresholdLowRet: 69
        val packet = GB.hexStringToByteArray("01020C0004064500000003D8C4")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ThresholdLowGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ThresholdLowRet)
        
        val thresholdRet = message as OneTouchMessage.ThresholdLowRet
        assertEquals(69, thresholdRet.threshold)
    }

    @Test
    fun testThresholdLowRetDecode_70() {
        // ThresholdLowRet: 70
        val packet = GB.hexStringToByteArray("01020C00040646000000030A2A")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ThresholdLowGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ThresholdLowRet)
        
        val thresholdRet = message as OneTouchMessage.ThresholdLowRet
        assertEquals(70, thresholdRet.threshold)
    }

    // ==================== ReadingCountRet Decode Tests ====================
    @Test
    fun testReadingCountRetDecode_62() {
        // ReadingCountRet: 62
        val packet = GB.hexStringToByteArray("01020A0004063E00038E0D")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.ReadingCountGet)

        assertNotNull(message)
        assertTrue(message is OneTouchMessage.ReadingCountRet)
        
        val countRet = message as OneTouchMessage.ReadingCountRet
        assertEquals(62.toShort(), countRet.count)
    }

    // ==================== Invalid Packet Tests ====================
    @Test
    fun testDecodeInvalidPacket_tooShort() {
        val packet = GB.hexStringToByteArray("010203")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNull(message)
    }

    @Test
    fun testDecodeInvalidPacket_wrongHeader() {
        val packet = GB.hexStringToByteArray("0003090004200203F9C3")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNull(message)
    }

    @Test
    fun testDecodeInvalidPacket_wrongChecksum() {
        val packet = GB.hexStringToByteArray("0102090004200203FFFF")
        val message = OneTouchMessage.decode(packet, OneTouchMessage.TimeGet)

        assertNull(message)
    }
}
