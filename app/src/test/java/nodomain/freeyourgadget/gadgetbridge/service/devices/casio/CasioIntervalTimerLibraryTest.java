package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CasioIntervalTimerLibraryTest {
    private CasioIntervalTimer named(String label) {
        CasioIntervalTimer t = new CasioIntervalTimer();
        t.label = label;
        return t;
    }

    @Test
    public void fromJson_null_givesEmptyLibrary() {
        CasioIntervalTimerLibrary lib = CasioIntervalTimerLibrary.fromJson(null);
        assertEquals(0, lib.timers.size());
        assertEquals(-1, lib.activeIndex);
        assertNull(lib.getActive());
    }

    @Test
    public void jsonRoundTrip_preservesTimersAndActive() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        lib.add(named("A"));
        lib.add(named("B"));
        lib.setActive(1);
        CasioIntervalTimerLibrary back = CasioIntervalTimerLibrary.fromJson(lib.toJson());
        assertEquals(2, back.timers.size());
        assertEquals("B", back.getActive().label);
    }

    @Test
    public void add_enforcesCap() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        for (int i = 0; i < CasioIntervalTimerLibrary.MAX_TIMERS; i++) {
            assertTrue(lib.add(named("T" + i)));
        }
        assertFalse(lib.add(named("overflow")));
        assertEquals(CasioIntervalTimerLibrary.MAX_TIMERS, lib.timers.size());
    }

    @Test
    public void remove_adjustsActiveIndex() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        lib.add(named("A")); lib.add(named("B")); lib.add(named("C"));
        lib.setActive(2);
        lib.remove(0);                 // shifts C from 2 -> 1
        assertEquals("C", lib.getActive().label);
        lib.remove(1);                 // removes the active one
        assertNull(lib.getActive());
        assertEquals(-1, lib.activeIndex);
    }

    @Test
    public void reconcile_emptyLibrary_seedsAndActivates() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        CasioIntervalTimer fromWatch = named("WATCH");
        fromWatch.autoRepeat = 7;
        assertTrue(lib.reconcileFromWatch(fromWatch));
        assertEquals(1, lib.timers.size());
        assertEquals(7, lib.getActive().autoRepeat);
    }

    @Test
    public void reconcile_activeMatches_noChange() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        CasioIntervalTimer t = named("X"); t.autoRepeat = 5;
        lib.add(t); lib.setActive(0);
        CasioIntervalTimer same = named("X"); same.autoRepeat = 5;
        assertFalse(lib.reconcileFromWatch(same));
    }

    @Test
    public void reconcile_activeDiffers_overwritesActive() {
        CasioIntervalTimerLibrary lib = new CasioIntervalTimerLibrary();
        CasioIntervalTimer t = named("X"); t.autoRepeat = 5;
        lib.add(t); lib.setActive(0);
        CasioIntervalTimer edited = named("X"); edited.autoRepeat = 9;
        assertTrue(lib.reconcileFromWatch(edited));
        assertEquals(9, lib.getActive().autoRepeat);
    }
}
