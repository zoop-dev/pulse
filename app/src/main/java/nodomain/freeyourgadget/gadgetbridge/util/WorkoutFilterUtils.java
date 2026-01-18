/*  Copyright (C) 2020-2024 Daniel Dakhno, Petr Vaněk, Gideon Zenz

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Pair;

import java.util.Calendar;

public class WorkoutFilterUtils {

    public static Pair<Long, Long> getDateRangeForFilter(String filterSelection) {
        if (filterSelection == null || filterSelection.equals("noselection")) {
            return null;
        }

        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        long firstdate;
        long lastdate;

        switch (filterSelection) {
            case "today":
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "thisweek":
                date.set(Calendar.DAY_OF_WEEK, date.getFirstDayOfWeek());
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "thismonth":
                date.set(Calendar.DAY_OF_MONTH, 1);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "lastweek":
                int i = date.get(Calendar.DAY_OF_WEEK) - date.getFirstDayOfWeek();
                date.add(Calendar.DATE, -i - 7);
                firstdate = date.getTimeInMillis();
                date.add(Calendar.DATE, 6);
                lastdate = date.getTimeInMillis();
                break;
            case "lastmonth":
                date.set(Calendar.DATE, 1);
                date.add(Calendar.DAY_OF_MONTH, -1);
                lastdate = date.getTimeInMillis();
                date.set(Calendar.DATE, 1);
                firstdate = date.getTimeInMillis();
                break;
            case "7days":
                date.add(Calendar.DATE, -7);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "30days":
                date.add(Calendar.DATE, -30);
                firstdate = date.getTimeInMillis();
                lastdate = Calendar.getInstance().getTimeInMillis();
                break;
            case "distantpast":
                firstdate = 0L;
                lastdate = 0L;
                break;
            default:
                return null;
        }

        return new Pair<>(firstdate, lastdate);
    }
}
