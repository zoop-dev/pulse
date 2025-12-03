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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CalendarEvent {
    private final long begin;
    private final long end;
    private final long id;
    private final long eventId;
    private final String title;
    private final String description;
    private final String location;
    private final String calName;
    private final String calAccountName;
    private final String calAccountType;
    private final String calendarId;
    private final String organizer;
    private final int calendarColor;
    private final int color;
    private final boolean allDay;
    private final String rrule;
    private final int status;
    private final int attendingStatus;
    private List<Long> remindersAbsoluteTs = new ArrayList<>();

    public CalendarEvent(long begin, long end, long id, long eventId, String title, String description,
                         String location, String calName, String calAccountName, int calendarColor,
                         int color, boolean allDay, String organizer, String calAccountType,
                         String calendarId, String rrule, int status, int attendingStatus) {
        this.begin = begin;
        this.end = end;
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.calName = calName;
        this.calAccountName = calAccountName;
        this.calendarColor = calendarColor;
        this.color = color;
        this.allDay = allDay;
        this.organizer = organizer;
        this.calAccountType = calAccountType;
        this.calendarId = calendarId;
        this.rrule = rrule;
        this.status = status;
        this.attendingStatus = attendingStatus;
    }

    public void setRemindersAbsoluteTs(List<Long> remindersAbsoluteTs) {
        this.remindersAbsoluteTs = remindersAbsoluteTs;
    }

    public long getBegin() {
        return begin;
    }

    public int getBeginSeconds() {
        return (int) (begin / 1000);
    }

    public long getEnd() {
        return end;
    }

    public int getEndSeconds() {
        return (int) (end / 1000);
    }

    public long getDuration() {
        return end - begin;
    }

    public int getDurationSeconds() {
        return (int) ((getDuration()) / 1000);
    }

    public short getDurationMinutes() {
        return (short) (getDurationSeconds() / 60);
    }


    public long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getOrganizer() {
        return organizer;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getCalName() {
        return calName;
    }

    public String getCalAccountName() {
        return calAccountName;
    }

    public String getUniqueCalName() {
        return getCalAccountName() + '/' + getCalName();
    }

    public int getCalendarColor() {
        return calendarColor;
    }

    public int getColor() {
        return color;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public String getCalAccountType() {
        return calAccountType;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public String getRrule() {
        return rrule;
    }

    public int getStatus() { return status; }

    public List<Long> getRemindersAbsoluteTs() { return remindersAbsoluteTs; }

    public int getAttendingStatus() { return attendingStatus; }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CalendarEvent e) {
            return (this.getId() == e.getId()) &&
                   (this.getEventId() == e.getEventId()) &&
                    Objects.equals(this.getTitle(), e.getTitle()) &&
                    (this.getBegin() == e.getBegin()) &&
                    Objects.equals(this.getLocation(), e.getLocation()) &&
                    Objects.equals(this.getDescription(), e.getDescription()) &&
                    (this.getEnd() == e.getEnd()) &&
                    Objects.equals(this.getCalName(), e.getCalName()) &&
                    Objects.equals(this.getCalAccountName(), e.getCalAccountName()) &&
                    (this.getCalendarColor() == e.getCalendarColor()) &&
                    (this.getColor() == e.getColor()) &&
                    (this.isAllDay() == e.isAllDay()) &&
                    Objects.equals(this.getOrganizer(), e.getOrganizer()) &&
                    Objects.equals(this.getRemindersAbsoluteTs(), e.getRemindersAbsoluteTs()) &&
                    Objects.equals(this.getCalAccountType(), e.getCalAccountType()) &&
                    Objects.equals(this.getCalendarId(), e.getCalendarId()) &&
                    Objects.equals(this.getRrule(), e.getRrule()) &&
                    Objects.equals(this.getStatus(), e.getStatus()) &&
                    Objects.equals(this.getAttendingStatus(), e.getAttendingStatus());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = (int) id;
        result = 31 * result + Objects.hash(eventId);
        result = 31 * result + Objects.hash(title);
        result = 31 * result + Long.valueOf(begin).hashCode();
        result = 31 * result + Objects.hash(location);
        result = 31 * result + Objects.hash(description);
        result = 31 * result + Long.valueOf(end).hashCode();
        result = 31 * result + Objects.hash(calName);
        result = 31 * result + Objects.hash(calAccountName);
        result = 31 * result + Integer.valueOf(calendarColor).hashCode();
        result = 31 * result + Integer.valueOf(color).hashCode();
        result = 31 * result + Boolean.valueOf(allDay).hashCode();
        result = 31 * result + Objects.hash(organizer);
        result = 31 * result + Objects.hash(remindersAbsoluteTs);
        result = 31 * result + Objects.hash(calAccountType);
        result = 31 * result + Objects.hash(calendarId);
        result = 31 * result + Objects.hash(rrule);
        result = 31 * result + Objects.hash(status);
        result = 31 * result + Objects.hash(attendingStatus);
        return result;
    }
}
