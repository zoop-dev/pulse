/*  Copyright (C) 2026 Vladimir Tasic

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.fitcloud.ak102;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;

public class Ak102DeviceSupportTest {

    private static Alarm alarm(final int position, final boolean enabled, final int repetition,
                               final int hour, final int minute, final String title) {
        final Alarm alarm = new Alarm();
        alarm.setPosition(position);
        alarm.setEnabled(enabled);
        alarm.setRepetition(repetition);
        alarm.setHour(hour);
        alarm.setMinute(minute);
        alarm.setTitle(title);
        alarm.setUnused(false);
        return alarm;
    }

    @Test
    public void testAlarmEncodeDecodeRoundtrip() {
        final int weekdays = nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_MON
                | nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_TUE
                | nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_FRI;
        final ByteArrayOutputStream records = new ByteArrayOutputStream();
        final byte[] first = Ak102DeviceSupport.encodeAlarm(
                alarm(2, true, weekdays, 6, 45, "Wake"));
        final byte[] second = Ak102DeviceSupport.encodeAlarm(
                alarm(7, false, weekdays, 23, 59, ""));
        records.write(first, 0, first.length);
        records.write(second, 0, second.length);

        final List<Ak102DeviceSupport.WatchAlarm> decoded =
                Ak102DeviceSupport.decodeAlarms(records.toByteArray());

        assertEquals(2, decoded.size());
        final Ak102DeviceSupport.WatchAlarm a = decoded.get(0);
        assertEquals(2, a.position);
        assertTrue(a.enabled);
        assertEquals(weekdays, a.repetition);
        assertEquals(6, a.hour);
        assertEquals(45, a.minute);
        assertEquals("Wake", a.label);
        final Ak102DeviceSupport.WatchAlarm b = decoded.get(1);
        assertEquals(7, b.position);
        assertFalse(b.enabled);
        assertEquals(23, b.hour);
        assertEquals(59, b.minute);
        assertEquals("", b.label);
    }

    @Test
    public void testDecodeAlarmsToleratesTruncatedBuffer() {
        final byte[] record = Ak102DeviceSupport.encodeAlarm(
                alarm(1, true, nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_DAILY,
                        8, 0, "Morning"));
        // Drop the last byte: the truncated record must be ignored, not crash.
        final byte[] truncated = new byte[record.length - 1];
        System.arraycopy(record, 0, truncated, 0, truncated.length);

        assertTrue(Ak102DeviceSupport.decodeAlarms(truncated).isEmpty());
    }
}
