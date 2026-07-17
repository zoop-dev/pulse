package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CasioIntervalTimerTest {
    @Test
    public void normalizeName_uppercases_filters_and_truncates() {
        assertEquals("AB-C", CasioIntervalTimer.normalizeName("ab-c"));
        // '*' is not allowed and is dropped; space maps to underscore
        assertEquals("AB_", CasioIntervalTimer.normalizeName("a*b "));
        // truncated to 14
        assertEquals("ABCDEFGHIJKLMN", CasioIntervalTimer.normalizeName("ABCDEFGHIJKLMNOP"));
        assertEquals("", CasioIntervalTimer.normalizeName(null));
    }

    @Test
    public void normalizeName_mapsSpacesToUnderscores() {
        assertEquals("HI_THERE", CasioIntervalTimer.normalizeName("hi there"));
        // mapped underscores count toward the 14-char limit
        assertEquals("ABC_DEF_GHI_JK", CasioIntervalTimer.normalizeName("ABC DEF GHI JKL"));
    }

    @Test
    public void clampRepeat_bounds() {
        assertEquals(1, CasioIntervalTimer.clampRepeat(0));
        assertEquals(1, CasioIntervalTimer.clampRepeat(-5));
        assertEquals(20, CasioIntervalTimer.clampRepeat(21));
        assertEquals(13, CasioIntervalTimer.clampRepeat(13));
    }

    @Test
    public void defaultTimer_hasFiveSlots() {
        CasioIntervalTimer t = new CasioIntervalTimer();
        assertEquals(5, t.slots.length);
        assertEquals(1, t.autoRepeat);
    }

    @Test
    public void formatDuration_rendersMinutesSecondsAndHours() {
        assertEquals("00:00", CasioIntervalTimer.formatDuration(0));
        assertEquals("05:30", CasioIntervalTimer.formatDuration(330));
        assertEquals("59:59", CasioIntervalTimer.formatDuration(3599));
        assertEquals("1:00:00", CasioIntervalTimer.formatDuration(3600));
        assertEquals("1:02:03", CasioIntervalTimer.formatDuration(3723));
        // max possible: 5 slots x 60'00" x 20 repeats
        assertEquals("100:00:00", CasioIntervalTimer.formatDuration(360000));
    }
}
