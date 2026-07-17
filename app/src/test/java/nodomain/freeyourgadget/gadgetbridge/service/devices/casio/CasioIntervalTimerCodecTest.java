package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CasioIntervalTimerCodecTest {
    // Captured from the official app (protocol doc). Order: name1..5, config.
    private static final String[] ACTIVE = {
        "44 01 53 4c 4f 54 2d 31 00 00 00 00 00 00 00 00 00 00 00 00",
        "44 02 53 4c 4f 54 2d 32 00 00 00 00 00 00 00 00 00 00 00 00",
        "44 03 53 4c 4f 54 2d 33 00 00 00 00 00 00 00 00 00 00 00 00",
        "44 04 53 4c 4f 54 2d 34 00 00 00 00 00 00 00 00 00 00 00 00",
        "44 05 53 4c 4f 54 2d 35 00 00 00 00 00 00 00 00 00 00 00 00",
        "2a 0d 02 01 00 04 03 00 06 05 00 08 07 00 10 09 00",
    };
    private static final String[] SKIP = {
        "44 01 57 4f 52 4b 4f 55 54 5f 31 00 00 00 00 00 00 00 00 00",
        "44 02 57 4f 52 4b 4f 55 54 5f 32 00 00 00 00 00 00 00 00 00",
        "44 03 57 4f 52 4b 4f 55 54 5f 33 00 00 00 00 00 00 00 00 00",
        "44 04 57 4f 52 4b 4f 55 54 5f 34 00 00 00 00 00 00 00 00 00",
        "44 05 57 4f 52 4b 4f 55 54 5f 35 00 00 00 00 00 00 00 00 00",
        "2a 01 11 00 00 22 00 00 00 00 00 44 00 00 55 00 00",
    };

    private static byte[] hex(String s) {
        String[] parts = s.trim().split("\\s+");
        byte[] out = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return out;
    }

    private static byte[][] packets(String[] rows) {
        byte[][] out = new byte[rows.length][];
        for (int i = 0; i < rows.length; i++) out[i] = hex(rows[i]);
        return out;
    }

    @Test
    public void decode_active_yieldsSignatureTimer() {
        CasioIntervalTimer t = CasioIntervalTimerCodec.decode(packets(ACTIVE));
        assertEquals(13, t.autoRepeat);          // repeat is binary 0x0d
        assertEquals(5, t.slots.length);
        assertFalse(t.slots[0].skipped);
        assertEquals(1, t.slots[0].minutes);     // BCD 0x01
        assertEquals(2, t.slots[0].seconds);     // BCD 0x02
        assertEquals("SLOT-1", t.slots[0].name);
        assertEquals(9, t.slots[4].minutes);
        assertEquals(10, t.slots[4].seconds);    // BCD 0x10 = 10
        assertEquals("SLOT-5", t.slots[4].name);
    }

    @Test
    public void decode_skip_zeroDurationIsSkippedNamePreserved() {
        CasioIntervalTimer t = CasioIntervalTimerCodec.decode(packets(SKIP));
        assertEquals(1, t.autoRepeat);
        assertFalse(t.slots[0].skipped);
        assertEquals(11, t.slots[0].seconds);    // BCD 0x11
        assertTrue(t.slots[2].skipped);          // slot 3 = 00'00"
        assertEquals("WORKOUT_3", t.slots[2].name); // name still present
        assertFalse(t.slots[3].skipped);
        assertEquals(44, t.slots[3].seconds);
    }

    @Test
    public void encode_roundTripsActivePacketsExactly() {
        byte[][] original = packets(ACTIVE);
        byte[][] out = CasioIntervalTimerCodec.encode(CasioIntervalTimerCodec.decode(original));
        assertEquals(6, out.length);
        for (int i = 0; i < original.length; i++) assertArrayEquals(original[i], out[i]);
    }

    @Test
    public void encode_roundTripsSkipPacketsExactly() {
        byte[][] original = packets(SKIP);
        byte[][] out = CasioIntervalTimerCodec.encode(CasioIntervalTimerCodec.decode(original));
        for (int i = 0; i < original.length; i++) assertArrayEquals(original[i], out[i]);
    }

    @Test
    public void encode_producesSixPacketsConfigLast() {
        byte[][] out = CasioIntervalTimerCodec.encode(new CasioIntervalTimer());
        assertEquals(6, out.length);
        for (int i = 0; i < 5; i++) {
            assertEquals(CasioIntervalTimerCodec.FEATURE_NAME, out[i][0]);
            assertEquals(i + 1, out[i][1]);      // 1-based slot number
            assertEquals(20, out[i].length);
        }
        assertEquals(CasioIntervalTimerCodec.FEATURE_CONFIG, out[5][0]);
        assertEquals(17, out[5].length);
    }

    @Test
    public void decode_noConfigPacket_returnsNull() {
        assertNull(CasioIntervalTimerCodec.decode(new byte[][]{}));
        assertNull(CasioIntervalTimerCodec.decode(null));
    }

    @Test
    public void encode_normalizesNameAndRepeat() {
        CasioIntervalTimer t = new CasioIntervalTimer();
        t.slots[0].name = "workout-longname12"; // lowercase + > 14 chars
        t.autoRepeat = 99;                       // out of range
        CasioIntervalTimer back = CasioIntervalTimerCodec.decode(CasioIntervalTimerCodec.encode(t));
        assertEquals(20, back.autoRepeat);
        assertEquals(CasioIntervalTimer.normalizeName("workout-longname12"), back.slots[0].name);
    }
}
