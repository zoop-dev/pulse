/*  Copyright (C) 2022-2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util.calendar;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;
import android.provider.ContactsContract;
import android.text.format.Time;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CalendarManager {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarManager.class);

    // needed for pebble: time, duration, layout, reminders, actions
    // layout: type, title, subtitle, body (max 512), tinyIcon, smallIcon, largeIcon
    //further: primaryColor, secondaryColor, backgroundColor, headings, paragraphs, lastUpdated
    // taken from: https://developer.getpebble.com/guides/timeline/pin-structure/

    // needed for MiBand:
    // time

    private final String deviceAddress;
    private final Context mContext;
    private HashSet<String> calendarBlacklist = null;
    private Map<String, HashSet<Integer>> colorBlacklist = null;

    public record CalendarEntry(String displayName, String accountName, int color, Set<Integer> eventColors) {
        public String getUniqueString() {
            return accountName + '/' + displayName;
        }
    }

    public CalendarManager(final Context context, final String deviceAddress) {
        this.mContext = context;
        this.deviceAddress = deviceAddress;
        loadCalendarsBlackList();
        loadColorBlackList();
    }

    public List<CalendarEvent> getCalendarEventList() {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(deviceAddress));
        final int lookaheadDays = Math.max(1, prefs.getInt(DeviceSettingsPreferenceConst.PREF_CALENDAR_LOOKAHEAD_DAYS, 7));
        return getCalendarEventList(lookaheadDays);
    }

    public List<CalendarEvent> getCalendarEventList(final int lookaheadDays) {
        loadCalendarsBlackList();
        loadColorBlackList();

        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(deviceAddress));

        final List<CalendarEvent> calendarEventList = new ArrayList<>();

        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR, false)) {
            calendarEventList.addAll(getCalendarEvents(lookaheadDays));
        }
        if (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SYNC_BIRTHDAYS, false)) {
            calendarEventList.addAll(getBirthdays(lookaheadDays));
            calendarEventList.sort(Comparator.comparingInt(CalendarEvent::getBeginSeconds));
        }

        return calendarEventList;
    }

    private List<CalendarEvent> getCalendarEvents(final int lookaheadDays) {
        final List<CalendarEvent> calendarEventList = new ArrayList<>();

        Calendar cal = GregorianCalendar.getInstance();
        long dtStart = cal.getTimeInMillis();
        cal.add(Calendar.DATE, lookaheadDays);
        long dtEnd = cal.getTimeInMillis();

        Uri.Builder eventsUriBuilder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, dtStart);
        ContentUris.appendId(eventsUriBuilder, dtEnd);
        Uri eventsUri = eventsUriBuilder.build();

        final String[] EVENT_INSTANCE_PROJECTION = new String[]{
                Instances._ID,
                Instances.BEGIN,
                Instances.END,
                Instances.DURATION,
                Instances.TITLE,
                Instances.DESCRIPTION,
                Instances.EVENT_LOCATION,
                Instances.ORGANIZER,
                Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                Instances.CALENDAR_COLOR,
                Instances.EVENT_COLOR,
                Instances.ALL_DAY,
                Instances.EVENT_ID, //needed for reminders
                CalendarContract.Calendars.ACCOUNT_TYPE,
                Instances.CALENDAR_ID,
                Instances.RRULE,
                Instances.STATUS,
                Instances.SELF_ATTENDEE_STATUS,
                Instances.HAS_EXTENDED_PROPERTIES
        };

        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(deviceAddress));
        boolean filterCanceled = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_CANCELED, true);
        boolean filterDeclined = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_DECLINED, true);
        boolean filterAllDay = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_ALL_DAY, true);

        ArrayList<String> selectionArgsList = new ArrayList<>();
        StringJoiner selection = new StringJoiner(" AND ");
        if (filterCanceled) {
            selection.add(Instances.STATUS + " != ?");
            selectionArgsList.add(String.valueOf(CalendarContract.Instances.STATUS_CANCELED));
        }

        if (filterDeclined) {
            selection.add(Instances.SELF_ATTENDEE_STATUS + " != ?");
            selectionArgsList.add(String.valueOf(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED));
        }

        if (filterAllDay) {
            selection.add(Instances.ALL_DAY + " != ?");
            selectionArgsList.add("1");
        }

        String[] selectionArgs = new String[selectionArgsList.size()];
        selectionArgsList.toArray(selectionArgs);

        try (Cursor evtCursor = mContext.getContentResolver().query(eventsUri, EVENT_INSTANCE_PROJECTION, selection.toString(), selectionArgs, Instances.BEGIN + " ASC")) {
            if (evtCursor == null || evtCursor.getCount() == 0) {
                return calendarEventList;
            }
            while (evtCursor.moveToNext()) {
                long start = evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.BEGIN));
                long end = evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.END));
                if (end == 0) {
                    LOG.info("no end time, will parse duration string");
                    Time time = new Time(); //FIXME: deprecated FTW
                    time.parse(evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.DURATION)));
                    end = start + time.toMillis(false);
                }

                CalendarEvent calEvent = new CalendarEvent(
                        start,
                        end,
                        evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances._ID)),
                        evtCursor.getLong(evtCursor.getColumnIndexOrThrow(Instances.EVENT_ID)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.TITLE)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.DESCRIPTION)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.EVENT_LOCATION)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_DISPLAY_NAME)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)),
                        evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_COLOR)),
                        evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.EVENT_COLOR)),
                        !evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.ALL_DAY)).equals("0"),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.ORGANIZER)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.CALENDAR_ID)),
                        evtCursor.getString(evtCursor.getColumnIndexOrThrow(Instances.RRULE)),
                        evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.STATUS)),
                        evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.SELF_ATTENDEE_STATUS))
                );


                // Filter now to avoid any reminder looking up the reminders.
                boolean hasExtendedProperties = evtCursor.getInt(evtCursor.getColumnIndexOrThrow(Instances.HAS_EXTENDED_PROPERTIES)) != 0;
                if (isCalendarBlacklisted(calEvent.getUniqueCalName()) ||
                    isColorBlacklistedForCalendar(calEvent.getUniqueCalName(), calEvent.getColor()) ||
                    (hasExtendedProperties && isExtendedPropertyBlacklisted(calEvent.getEventId()))) {
                    LOG.debug("event {} - {} skipped because it's blacklisted or filtered", calEvent.getUniqueCalName(), calEvent.getTitle());
                    continue;
                }

                // Query reminders for this event
                try (Cursor reminderCursor = mContext.getContentResolver().query(
                        CalendarContract.Reminders.CONTENT_URI,
                        null,
                        CalendarContract.Reminders.EVENT_ID + " = ?",
                        new String[]{String.valueOf(calEvent.getEventId())},
                        null
                )) {
                    if (reminderCursor != null && reminderCursor.getCount() > 0) {
                        final List<Long> reminders = new ArrayList<>();
                        while (reminderCursor.moveToNext()) {
                            int minutes = reminderCursor.getInt(reminderCursor.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES));
                            int method = reminderCursor.getInt(reminderCursor.getColumnIndexOrThrow(CalendarContract.Reminders.METHOD));
                            LOG.trace("Reminder Method: {}, Minutes: {}", method, minutes);

                            if (method == 1) //METHOD_ALERT
                                reminders.add(calEvent.getBegin() - minutes * 60 * 1000L);

                        }
                        reminderCursor.close();

                        calEvent.setRemindersAbsoluteTs(reminders);
                    }
                } catch (final Exception e) {
                    LOG.warn("failed to get reminder for event", e);
                }

                calendarEventList.add(calEvent);
            }
            return calendarEventList;
        } catch (final Exception e) {
            LOG.error("could not query calendar, permission denied?", e);
            return calendarEventList;
        }
    }

    public List<CalendarEvent> getBirthdays(final int lookaheadDays) {
        final String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Event.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.DISPLAY_NAME
        };
        final String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                ContactsContract.CommonDataKinds.Event.TYPE + " = ?";
        final String[] selectionArgs = new String[]{
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                String.valueOf(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
        };
        final List<CalendarEvent> birthdays = new LinkedList<>();
        final LocalDate maxDate = LocalDate.now().plusDays(lookaheadDays);

        try (Cursor birthdayCursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, ContactsContract.CommonDataKinds.Event.START_DATE + " ASC")) {
            if (birthdayCursor == null || birthdayCursor.getCount() == 0) {
                return birthdays;
            }
            while (birthdayCursor.moveToNext()) {
                final String contactId = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.CONTACT_ID));
                final String birthdayStr = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE));
                final String displayName = birthdayCursor.getString(birthdayCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME));
                final LocalDate birthday = parseBirthday(birthdayStr);
                if (birthday == null || birthday.isAfter(maxDate)) {
                    continue;
                }

                // Follow the same logic as CalendarContract - all day events have the start
                // timestamp at the UTC midnight boundary
                final long startTimestampUtc = DateTimeUtils.dayStartUtc(birthday).getTime();

                birthdays.add(new CalendarEvent(
                        startTimestampUtc,
                        startTimestampUtc + 86400000L - 1L,
                        contactId.hashCode(),
                        contactId.hashCode(),
                        mContext.getString(R.string.contact_birthday, displayName),
                        null,
                        null,
                        mContext.getString(R.string.birthdays),
                        mContext.getString(R.string.pref_contacts_title),
                        0,
                        0,
                        true,
                        null,
                        CalendarContract.ACCOUNT_TYPE_LOCAL,
                        null,
                        null,
                        0,
                        0
                ));
            }
        } catch (final Exception e) {
            LOG.error("could not query birthdays, permission denied?", e);
        }
        return birthdays;
    }

    @Nullable
    private LocalDate parseBirthday(final String birthdayStr) {
        final LocalDate birthday;
        final LocalDate now = LocalDate.now();

        try {
            if (birthdayStr.startsWith("--")) {
                // MM-DD
                final String monthDay = birthdayStr.substring(2);
                birthday = LocalDate.parse(now.getYear() + "-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else {
                birthday = LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE).withYear(now.getYear());
            }
        } catch (final DateTimeParseException e) {
            LOG.error("Failed to parse birthday {}", birthdayStr, e);
            return null;
        }

        if (birthday.isAfter(now) || birthday.isEqual(now)) {
            return birthday;
        }

        return birthday.plusYears(1);
    }

    public List<CalendarEntry> getCalendars() {
        final String[] COLOR_PROJECTION = new String[] {
            CalendarContract.Colors.COLOR, CalendarContract.Colors.ACCOUNT_NAME
        };

        final String COLOR_SELECTION = CalendarContract.Colors.COLOR_TYPE + " = " + CalendarContract.Colors.TYPE_EVENT;

        Map<String, Set<Integer>> eventColors = new HashMap<>();
        try (Cursor colorCursor = mContext.getContentResolver().query(CalendarContract.Colors.CONTENT_URI, COLOR_PROJECTION, COLOR_SELECTION, null,  CalendarContract.Colors.ACCOUNT_NAME)) {
            if (colorCursor != null && colorCursor.getCount() > 0) {
                while (colorCursor.moveToNext()) {
                    final int color = colorCursor.getInt(colorCursor.getColumnIndexOrThrow(CalendarContract.Colors.COLOR));
                    final String account = colorCursor.getString(colorCursor.getColumnIndexOrThrow(CalendarContract.Colors.ACCOUNT_NAME));
                    if (eventColors.containsKey(account)) {
                        eventColors.get(account).add(color);
                    } else {
                        // Some events have no color, aka 0, add that as an option to all lists.
                        eventColors.put(account, new HashSet<>(List.of(0, color)));
                    }
                }
            }
        } catch (final Exception e) {
            LOG.warn("failed to getting colors", e);
        }

        final String[] CALENDAR_PROJECTION = new String[]{
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR
        };

        ArrayList<CalendarEntry> out = new ArrayList<>();
        try (Cursor cur = mContext.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI, CALENDAR_PROJECTION, null, null, null)) {
            while (cur != null && cur.moveToNext()) {
                out.add(new CalendarEntry(cur.getString(0),
                                          cur.getString(1),
                                          cur.getInt(2),
                                          eventColors.getOrDefault(cur.getString(1),
                                                                   new HashSet<>(List.of()))));
            }
        }
        return out;
    }

    private boolean isExtendedPropertyBlacklisted(long eventId) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(deviceAddress));
        boolean filterFocusTime = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_FOCUS_TIME, true);
        boolean filterWorkingLocations = !prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_WORKING_LOCATION, true);

        // If both filters are disabled, or the API level is not high enough, no need to even try
        // querying the properties table.
        if (!filterFocusTime && !filterWorkingLocations) {
            return false;
        }

        try (Cursor propCursor = mContext.getContentResolver().query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                new String[] { CalendarContract.ExtendedProperties.VALUE },
                CalendarContract.Reminders.EVENT_ID + " = ? AND " +
                    CalendarContract.ExtendedProperties.NAME + " = ?",
                new String[]{ String.valueOf(eventId), "shared:calendarProviderEventType" },
                null)) {
            while (propCursor != null && propCursor.moveToNext()) {
                String propValue = propCursor.getString(propCursor.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE));
                if (filterFocusTime && propValue.equals("USER_STATUS_FOCUS_TIME")) {
                    return true;
                } else if (filterWorkingLocations && propValue.equals("WORKING_LOCATION")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isCalendarBlacklisted(String calendarUniqueName) {
        if (calendarBlacklist == null) {
            LOG.warn("isCalendarBlacklisted: calendarBlacklist is null!");
        }
        return calendarBlacklist != null && calendarBlacklist.contains(calendarUniqueName);
    }

    public void addCalendarToBlacklist(String calendarUniqueName) {
        if (calendarBlacklist.add(calendarUniqueName)) {
            LOG.info("Blacklisted calendar {}", calendarUniqueName);
            saveCalendarsBlackList();
        } else {
            LOG.warn("Calendar {} already blacklisted!", calendarUniqueName);
        }
    }

    public void removeFromCalendarBlacklist(String calendarUniqueName) {
        calendarBlacklist.remove(calendarUniqueName);
        LOG.info("Unblacklisted calendar {}", calendarUniqueName);
        saveCalendarsBlackList();
    }

    private void loadCalendarsBlackList() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);

        LOG.info("Loading calendarBlacklist");
        calendarBlacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.CALENDAR_BLACKLIST, null);
        if (calendarBlacklist == null) {
            calendarBlacklist = new HashSet<>();
        }
        LOG.info("Loaded calendarBlacklist has {} entries", calendarBlacklist.size());
    }

    private void saveCalendarsBlackList() {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);

        LOG.info("Saving calendarBlacklist with {} entries", calendarBlacklist.size());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (calendarBlacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.CALENDAR_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.CALENDAR_BLACKLIST, calendarBlacklist);
        }
        editor.apply();
    }

    public boolean isColorBlacklistedForCalendar(String calendarUniqueName, int color) {
        if (colorBlacklist == null) {
            LOG.warn("isColorBlacklistedForCalendar: colorBlacklist is null!");
        }
        return colorBlacklist != null &&
               colorBlacklist.containsKey(calendarUniqueName) &&
               colorBlacklist.get(calendarUniqueName).contains(color);
    }

    public void addColorToBlacklistForCalendar(String calendarUniqueName, int color) {
        if (colorBlacklist.containsKey(calendarUniqueName)) {
            if (colorBlacklist.get(calendarUniqueName).add(color)) {
                LOG.info("Blacklisted color {} {}", calendarUniqueName, color);
                saveColorBlackList();
            } else {
                LOG.warn("Color {} {} already blacklisted!", calendarUniqueName, color);
            }
        } else {
            colorBlacklist.put(calendarUniqueName, new HashSet<>(List.of(color)));
            LOG.info("Blacklisted color {} {}", calendarUniqueName, color);
            saveColorBlackList();
        }
    }

    public void removeColorFromBlacklistForCalendar(String calendarUniqueName, int color) {
        if (colorBlacklist.containsKey(calendarUniqueName)) {
            colorBlacklist.get(calendarUniqueName).remove(color);
            if (colorBlacklist.get(calendarUniqueName).isEmpty()) {
                colorBlacklist.remove(calendarUniqueName);
            }
            LOG.info("Unblacklisted color {} {}", calendarUniqueName, color);
            saveColorBlackList();
        }
    }

    private void loadColorBlackList() {
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);
        colorBlacklist = new HashMap<>();
        String serializedColorBlackList = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_COLOR_BLACKLIST, null);
        if (serializedColorBlackList != null && !serializedColorBlackList.isEmpty()) {
            LOG.info("Loading colorBlacklist from json {}", serializedColorBlackList);
            JSONObject json = null;
            try {
                json = new JSONObject(serializedColorBlackList);
                for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                    String calendarName = it.next();
                    JSONArray colorJson = json.getJSONArray(calendarName);
                    HashSet<Integer> colors = new HashSet<>();
                    for (int i = 0; i < colorJson.length(); i++) {
                        colors.add(colorJson.getInt(i));
                    }
                    colorBlacklist.put(calendarName, colors);
                }
            } catch (JSONException e) {
                LOG.warn("Error parsing colors from prefs: {}", e.toString());
            }
        }

        LOG.info("Loaded colorBlacklist has {} entries", colorBlacklist.size());
    }

    private void saveColorBlackList() {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);

        LOG.info("Saving colorBlacklist with {} entries", colorBlacklist.size());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (colorBlacklist.isEmpty()) {
            editor.putString(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_COLOR_BLACKLIST, null);
        } else {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, HashSet<Integer>> entry : colorBlacklist.entrySet()) {
                JSONArray colorList = new JSONArray();
                for (int color : entry.getValue()) {
                    colorList.put(color);
                }
                try {
                    json.put(entry.getKey(), colorList);
                } catch (JSONException e) {
                    LOG.warn("Error serializing colors to prefs: {}", e.toString());
                }
            }

            editor.putString(DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_COLOR_BLACKLIST, json.toString());
        }
        editor.apply();
    }

}
