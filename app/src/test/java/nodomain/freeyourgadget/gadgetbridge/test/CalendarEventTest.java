package nodomain.freeyourgadget.gadgetbridge.test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;

public class CalendarEventTest extends TestBase {
    private static final long BEGIN = 1;
    private static final long END = 2;
    private static final long ID_1 = 100;
    private static final long ID_2 = 101;
    private static final String CALNAME_1 = "cal1";
    private static final String CALACCOUNTNAME_1 = "account1";
    private static final int COLOR_1 = 185489;
    private static final int COLOR_CAL = 0xFF00FF00;

    @Test
    public void testHashCode() {
        CalendarEvent c1 =
                new CalendarEvent(BEGIN, END, ID_1, 30,"something", null, null, CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, null, null, null, null, 0, 1);
        CalendarEvent c2 =
                new CalendarEvent(BEGIN, END, ID_1, 30,null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, null, null, null, null, 3, 0);
        CalendarEvent c3 =
                new CalendarEvent(BEGIN, END, ID_1, 30, null, null, "something", CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, null, null, null, null, 2, 2);
        CalendarEvent c4 =
                new CalendarEvent(BEGIN, END, ID_1, 30,null, null, "something", CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, "some", null, null, null, 1, 3);

        assertEquals(c1.hashCode(), c1.hashCode());
        assertNotEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c2.hashCode(), c3.hashCode());
        assertNotEquals(c3.hashCode(), c4.hashCode());
    }


    @Test
    public void testSync() {
        List<CalendarEvent> eventList = new ArrayList<>();
        eventList.add(new CalendarEvent(BEGIN, END, ID_1, 55, null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, null, null, null, null, 3, 0));

        GBDevice dummyGBDevice = createDummyGDevice("00:00:01:00:03");
        dummyGBDevice.setState(GBDevice.State.INITIALIZED);
//        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        CalendarReceiver testCR = new CalendarReceiver(getContext(), dummyGBDevice);

        testCR.syncCalendar(eventList);

        eventList.add(new CalendarEvent(BEGIN, END, ID_2, 63, null, "something", null, CALNAME_1, CALACCOUNTNAME_1, COLOR_CAL, COLOR_1, false, null, null, null, null, 3, 0));
        testCR.syncCalendar(eventList);

        CalendarSyncStateDao calendarSyncStateDao = daoSession.getCalendarSyncStateDao();
        assertEquals(2, calendarSyncStateDao.count());
    }

}
