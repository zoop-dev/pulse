package nodomain.freeyourgadget.gadgetbridge.externalevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;

public class DeviceAlarmReceiverTest extends TestBase {
    @Rule
    public final TestName testName = new TestName();

    private final DeviceAlarmReceiver receiver = new DeviceAlarmReceiver();

    private GBDevice device;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        device = createThreeSlotDevice();
        device.setState(GBDevice.State.INITIALIZED);
        DBHelper.getDevice(device, daoSession);
        registerDeviceWithManager(device);

        final SharedPreferences devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
        assertNotNull(devicePrefs);
        devicePrefs.edit()
                .clear()
                .putBoolean("third_party_apps_set_alarms", true)
                .commit();

        drainStartedServices();
    }

    @Test
    public void setAlarm_rejectsMissingDeviceAddress() {
        receiver.onReceive(getContext(), new Intent(DeviceAlarmReceiver.COMMAND_SET_ALARM));

        assertNull(getNextStartedService());
        assertEquals(0, DBHelper.getAlarms(device).size());
    }

    @Test
    public void setAlarm_rejectsInvalidMacAddress() {
        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_SET_ALARM);
        intent.putExtra(DeviceAlarmReceiver.EXTRA_MAC_ADDR, "invalid");

        receiver.onReceive(getContext(), intent);

        assertNull(getNextStartedService());
        assertEquals(0, DBHelper.getAlarms(device).size());
    }

    @Test
    public void setAlarm_rejectsUnknownDevice() {
        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_SET_ALARM);
        intent.putExtra(DeviceAlarmReceiver.EXTRA_MAC_ADDR, "00:00:00:00:00:00");

        receiver.onReceive(getContext(), intent);

        assertNull(getNextStartedService());
        assertEquals(0, DBHelper.getAlarms(device).size());
    }

    @Test
    public void setAlarm_rejectsWhenThirdPartyAlarmsAreDisabled() {
        GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).edit()
                .putBoolean("third_party_apps_set_alarms", false)
                .commit();

        final Intent intent = setAlarmIntent(7, 30, "Morning run");

        receiver.onReceive(getContext(), intent);

        assertNull(getNextStartedService());
        assertEquals(0, DBHelper.getAlarms(device).size());
    }

    @Test
    public void setAlarm_usesFirstUntitledDisabledSlotWithRepetition() {
        storeAlarm(0, true, 6, 15, 0, "personal");
        storeAlarm(1, false, 6, 30, 0, "");
        storeAlarm(2, false, 8, 45, 0, "");

        final Intent intent = setAlarmIntent(7, 30, "Morning run");
        intent.putIntegerArrayListExtra(
                DeviceAlarmReceiver.EXTRA_DAYS,
                new ArrayList<>(Arrays.asList(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.SUNDAY))
        );

        receiver.onReceive(getContext(), intent);

        final Alarm alarm = getAlarm(1);
        assertTrue(alarm.getEnabled());
        assertEquals(7, alarm.getHour());
        assertEquals(30, alarm.getMinute());
        assertEquals("Morning run", alarm.getTitle());
        assertEquals(
                Alarm.ALARM_MON + Alarm.ALARM_WED + Alarm.ALARM_SUN,
                alarm.getRepetition()
        );

        final Intent forwardedIntent = getNextStartedService();
        assertForwardedAlarmUpdate(forwardedIntent, 3);
        assertNull(getNextStartedService());
    }

    @Test
    public void setAlarm_skipsDismissedAlarmWithTitleAndUsesUntitledSlot() {
        storeAlarm(0, false, 6, 30, 0, "protected personal alarm");
        storeAlarm(1, false, 6, 30, 0, "");
        storeAlarm(2, false, 6, 30, 0, "second free");

        final Intent intent = setAlarmIntent(9, 45, "Auto slot");

        receiver.onReceive(getContext(), intent);

        final Alarm protectedAlarm = getAlarm(0);
        assertFalse(protectedAlarm.getEnabled());
        assertEquals("protected personal alarm", protectedAlarm.getTitle());

        final Alarm reused = getAlarm(1);
        assertTrue(reused.getEnabled());
        assertEquals(9, reused.getHour());
        assertEquals(45, reused.getMinute());
        assertEquals("Auto slot", reused.getTitle());

        final Alarm untouched2 = getAlarm(2);
        assertFalse(untouched2.getEnabled());
        assertEquals("second free", untouched2.getTitle());

        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void setAlarm_rejectsWhenNoFreeSlotIsAvailable() {
        storeAlarm(0, true, 6, 30, 0, "slot 1");
        storeAlarm(1, false, 7, 30, 0, "protected dismissed slot");
        storeAlarm(2, true, 8, 30, 0, "slot 3");

        final Intent intent = setAlarmIntent(9, 45, "No space");

        receiver.onReceive(getContext(), intent);

        assertNull(getNextStartedService());
        assertEquals(3, DBHelper.getAlarms(device).size());
        assertEquals("slot 1", getAlarm(0).getTitle());
        assertEquals("protected dismissed slot", getAlarm(1).getTitle());
        assertEquals("slot 3", getAlarm(2).getTitle());
    }

    @Test
    public void setAlarm_allowsMissingLabel() {
        storeAlarm(0, false, 6, 30, 0, "");
        storeAlarm(1, true, 7, 45, 0, "personal");
        storeAlarm(2, false, 8, 15, 0, "reserved");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_SET_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 5)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 50);

        receiver.onReceive(getContext(), intent);

        final Alarm alarm = getAlarm(0);
        assertTrue(alarm.getEnabled());
        assertEquals(5, alarm.getHour());
        assertEquals(50, alarm.getMinute());
        assertNull(alarm.getTitle());
        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void setAlarm_rejectsOutOfBoundsHour() {
        storeAlarm(0, false, 6, 30, 0, "");

        final Intent intent = setAlarmIntent(24, 15, "Morning run");

        receiver.onReceive(getContext(), intent);

        final Alarm alarm = getAlarm(0);
        assertFalse(alarm.getEnabled());
        assertEquals(6, alarm.getHour());
        assertEquals(30, alarm.getMinute());
        assertEquals("", alarm.getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void setAlarm_rejectsOutOfBoundsMinute() {
        storeAlarm(0, false, 6, 30, 0, "");

        final Intent intent = setAlarmIntent(7, 60, "Morning run");

        receiver.onReceive(getContext(), intent);

        final Alarm alarm = getAlarm(0);
        assertFalse(alarm.getEnabled());
        assertEquals(6, alarm.getHour());
        assertEquals(30, alarm.getMinute());
        assertEquals("", alarm.getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarm_rejectsUnknownMode() {
        storeAlarm(0, true, 6, 30, 0, "slot 0");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, "bogus");

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarm_rejectsLabelModeWithoutMessage() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TITLE);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByAll_disablesEveryStoredAlarm() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 7, 45, 0, "Standup");
        storeAlarm(2, true, 8, 15, 0, "School run");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_ALL);

        receiver.onReceive(getContext(), intent);

        assertFalse(getAlarm(0).getEnabled());
        assertFalse(getAlarm(1).getEnabled());
        assertFalse(getAlarm(2).getEnabled());
        assertEquals("", getAlarm(0).getTitle());
        assertEquals("", getAlarm(1).getTitle());
        assertEquals("", getAlarm(2).getTitle());
        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void dismissAlarmByTime_rejectsWhenHourIsMissing() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByTime_matchesProvidedHour() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 6, 45, 0, "Standup");
        storeAlarm(2, true, 8, 15, 0, "School run");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 6);

        receiver.onReceive(getContext(), intent);

        assertFalse(getAlarm(0).getEnabled());
        assertFalse(getAlarm(1).getEnabled());
        assertTrue(getAlarm(2).getEnabled());
        assertEquals("", getAlarm(0).getTitle());
        assertEquals("", getAlarm(1).getTitle());
        assertEquals("School run", getAlarm(2).getTitle());
        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void dismissAlarmByTime_rejectsWhenOnlyMinuteIsProvided() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 7, 30, 0, "Standup");
        storeAlarm(2, true, 8, 15, 0, "School run");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 30);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertTrue(getAlarm(2).getEnabled());
        assertEquals("Wake up", getAlarm(0).getTitle());
        assertEquals("Standup", getAlarm(1).getTitle());
        assertEquals("School run", getAlarm(2).getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByTime_matchesHourAndMinuteWhenBothProvided() {
        storeAlarm(0, true, 6, 10, 0, "Hour match");
        storeAlarm(1, true, 9, 30, 0, "Minute match");
        storeAlarm(2, true, 6, 30, 0, "Both match");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 6)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 30);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertFalse(getAlarm(2).getEnabled());
        assertEquals("Hour match", getAlarm(0).getTitle());
        assertEquals("Minute match", getAlarm(1).getTitle());
        assertEquals("", getAlarm(2).getTitle());
        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void dismissAlarmByTime_doesNothingWhenBothProvidedButNoExactMatchExists() {
        storeAlarm(0, true, 6, 10, 0, "Hour match");
        storeAlarm(1, true, 9, 30, 0, "Minute match");
        storeAlarm(2, true, 8, 15, 0, "No match");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 6)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 30);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertTrue(getAlarm(2).getEnabled());
        assertEquals("Hour match", getAlarm(0).getTitle());
        assertEquals("Minute match", getAlarm(1).getTitle());
        assertEquals("No match", getAlarm(2).getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByTime_doesNothingWhenNoAlarmMatches() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 7, 45, 0, "Standup");
        storeAlarm(2, true, 8, 15, 0, "School run");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 11)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 59);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertTrue(getAlarm(2).getEnabled());
        assertEquals("Wake up", getAlarm(0).getTitle());
        assertEquals("Standup", getAlarm(1).getTitle());
        assertEquals("School run", getAlarm(2).getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByTime_outOfBoundsHourDoesNothing() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 7, 45, 0, "Standup");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 24);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertEquals("Wake up", getAlarm(0).getTitle());
        assertEquals("Standup", getAlarm(1).getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByTime_outOfBoundsMinuteDoesNothing() {
        storeAlarm(0, true, 6, 30, 0, "Wake up");
        storeAlarm(1, true, 7, 45, 0, "Standup");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TIME)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, 6)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, 60);

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertEquals("Wake up", getAlarm(0).getTitle());
        assertEquals("Standup", getAlarm(1).getTitle());
        assertNull(getNextStartedService());
    }

    @Test
    public void dismissAlarmByLabel_matchesContainedTitle() {
        storeAlarm(0, true, 6, 30, 0, "Morning workout");
        storeAlarm(1, true, 7, 45, 0, "Workout cooldown");
        storeAlarm(2, true, 8, 15, 0, "School run");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TITLE)
                .putExtra(DeviceAlarmReceiver.EXTRA_TITLE, "workout");

        receiver.onReceive(getContext(), intent);

        assertFalse(getAlarm(0).getEnabled());
        assertTrue(getAlarm(1).getEnabled());
        assertTrue(getAlarm(2).getEnabled());
        assertEquals("", getAlarm(0).getTitle());
        assertEquals("Workout cooldown", getAlarm(1).getTitle());
        assertForwardedAlarmUpdate(getNextStartedService(), 3);
    }

    @Test
    public void dismissAlarmByLabel_isCaseSensitive() {
        storeAlarm(0, true, 6, 30, 0, "Morning workout");

        final Intent intent = baseIntent(DeviceAlarmReceiver.COMMAND_DISMISS_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_ALARM_SEARCH_MODE, DeviceAlarmReceiver.ALARM_SEARCH_MODE_TITLE)
                .putExtra(DeviceAlarmReceiver.EXTRA_TITLE, "Workout");

        receiver.onReceive(getContext(), intent);

        assertTrue(getAlarm(0).getEnabled());
        assertNull(getNextStartedService());
    }

    private GBDevice createThreeSlotDevice() {
        final int suffix = Math.abs(testName.getMethodName().hashCode()) % 256;
        final String address = String.format("AA:BB:CC:DD:EE:%02X", suffix);
        return new ThreeSlotTestDevice(address);
    }

    private void registerDeviceWithManager(final GBDevice device) {
        try {
            final DeviceManager deviceManager = GBApplication.app().getDeviceManager();
            final Field field = DeviceManager.class.getDeclaredField("deviceList");
            field.setAccessible(true);
            final List<GBDevice> deviceList = (List<GBDevice>) field.get(deviceManager);
            deviceList.clear();
            deviceList.add(device);
        } catch (final Exception e) {
            throw new AssertionError("Failed to register test device", e);
        }
    }

    private Intent setAlarmIntent(final int hour, final int minutes, final String message) {
        return baseIntent(DeviceAlarmReceiver.COMMAND_SET_ALARM)
                .putExtra(DeviceAlarmReceiver.EXTRA_HOUR, hour)
                .putExtra(DeviceAlarmReceiver.EXTRA_MINUTES, minutes)
                .putExtra(DeviceAlarmReceiver.EXTRA_TITLE, message);
    }

    private Intent baseIntent(final String action) {
        return new Intent(action)
                .setPackage("nodomain.freeyourgadget.gadgetbridge")
                .putExtra(DeviceAlarmReceiver.EXTRA_MAC_ADDR, device.getAddress());
    }

    private void storeAlarm(final int position, final boolean enabled, final int hour, final int minute, final int repetition, final String title) {
        final Alarm alarm = AlarmUtils.createDefaultAlarm(daoSession, device, position);
        assertNotNull(alarm);
        alarm.setEnabled(enabled);
        alarm.setHour(hour);
        alarm.setMinute(minute);
        alarm.setRepetition(repetition);
        alarm.setTitle(title);
        DBHelper.store(alarm);
    }

    private Alarm getAlarm(final int position) {
        final List<Alarm> alarms = DBHelper.getAlarmsWithDefaults(device);
        for (final Alarm alarm : alarms) {
            if (alarm.getPosition() == position) {
                return alarm;
            }
        }
        throw new AssertionError("Alarm at position " + position + " not found");
    }

    private void assertForwardedAlarmUpdate(final Intent intent, final int expectedAlarmCount) {
        assertNotNull(intent);
        assertEquals(DeviceCommunicationService.class.getName(), intent.getComponent().getClassName());
        assertEquals(DeviceService.ACTION_SET_ALARMS, intent.getAction());

        final ArrayList<? extends Alarm> alarms = (ArrayList<? extends Alarm>) intent.getSerializableExtra(DeviceService.EXTRA_ALARMS);
        assertNotNull(alarms);
        assertEquals(expectedAlarmCount, alarms.size());
    }

    private void drainStartedServices() {
        while (getNextStartedService() != null) {
            // Drain app startup noise so each test can assert only its own service invocation.
        }
    }

    private Intent getNextStartedService() {
        return shadowOf((Application) app).getNextStartedService();
    }

    private static final class ThreeSlotTestDevice extends GBDevice {
        private static final DeviceCoordinator COORDINATOR = new ThreeSlotTestCoordinator();

        private ThreeSlotTestDevice(final String address) {
            super(address, "Testie", "Test Alias", "Test Folder", DeviceType.TEST);
        }

        @Override
        public DeviceCoordinator getDeviceCoordinator() {
            return COORDINATOR;
        }
    }

    private static final class ThreeSlotTestCoordinator extends TestDeviceCoordinator {
        @Override
        public int getAlarmSlotCount(final GBDevice device) {
            return 3;
        }
    }
}
