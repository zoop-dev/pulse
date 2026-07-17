package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CasioWorldClockCodecTest {
    // fixed instant inside EU+US DST: 2026-07-17 15:00 UTC
    private static final Instant NOW = Instant.parse("2026-07-17T15:00:00Z");

    private static CasioTimeZone denver() {
        return CasioTimeZone.fromZoneId(ZoneId.of("America/Denver"), NOW, "DENVER");
    }

    private static CasioTimeZone london() {
        return CasioTimeZone.fromZoneId(ZoneId.of("Europe/London"), NOW, "LONDON");
    }

    private static byte[] hex(String s) {
        String[] parts = s.trim().split("\\s+");
        byte[] out = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = (byte) Integer.parseInt(parts[i], 16);
        return out;
    }

    @Test
    public void dstWatchStateFrameIs15BytesWithFfPadding() {
        // capture: 1d 00 01 03 03 <ids> ff ff ff ff ff ff — GB writes ids 00 00
        assertArrayEquals(
                hex("1d 00 01 03 03 00 00 00 00 ff ff ff ff ff ff"),
                CasioWorldClockCodec.dstWatchStateFrame(0, denver(), 1, london()));
    }

    @Test
    public void clockFramesOrderMatchesCaptureWithNames() {
        List<byte[]> frames = CasioWorldClockCodec.clockFrames(
                new CasioTimeZone[]{denver(), london()}, true);
        assertEquals(5, frames.size());
        // 1d pair frame first
        assertArrayEquals(hex("1d 00 01 03 03 00 00 00 00 ff ff ff ff ff ff"), frames.get(0));
        // 1e per slot: DENVER -7h (e4 = -28 quarter-hours), US rules 01; LONDON UTC+0, rules 02
        assertArrayEquals(hex("1e 00 00 00 e4 04 01"), frames.get(1));
        assertArrayEquals(hex("1e 01 00 00 00 04 02"), frames.get(2));
        // 1f per slot: 18-byte zero-padded ASCII names (capture layout)
        assertArrayEquals(hex("1f 00 44 45 4e 56 45 52 00 00 00 00 00 00 00 00 00 00 00 00"), frames.get(3));
        assertArrayEquals(hex("1f 01 4c 4f 4e 44 4f 4e 00 00 00 00 00 00 00 00 00 00 00 00"), frames.get(4));
    }

    @Test
    public void clockFramesWithoutNamesOmits1f() {
        // WS-B1000 profile: no 0x1f frames at all
        List<byte[]> frames = CasioWorldClockCodec.clockFrames(
                new CasioTimeZone[]{denver(), london()}, false);
        assertEquals(3, frames.size());
        assertEquals((byte) 0x1d, frames.get(0)[0]);
        assertEquals((byte) 0x1e, frames.get(1)[0]);
        assertEquals((byte) 0x1e, frames.get(2)[0]);
    }

    @Test
    public void tehranHalfHourOffsetNoDst() {
        // capture: 1e 01 16 01 0e 04 00 — GB computes ids 00 00 and dstOffset 00
        // (no future transitions since Iran abolished DST); offset 0e = +3:30
        CasioTimeZone tehran = CasioTimeZone.fromZoneId(ZoneId.of("Asia/Tehran"), NOW, "TEHRAN");
        assertArrayEquals(hex("1e 01 00 00 0e 00 00"), tehran.dstSettingBytes(1));
    }

    @Test
    public void longLabelTruncatedTo18Bytes() {
        CasioTimeZone zone = CasioTimeZone.fromZoneId(
                ZoneId.of("Europe/London"), NOW, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        byte[] frame = zone.worldCityBytes(1);
        assertEquals(20, frame.length);
        assertEquals((byte) 'R', frame[19]); // 18th name char, frame = 1f + slot + 18 bytes
    }
}
