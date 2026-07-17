/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

public class CasioIntervalTimer {
    public static final int SLOT_COUNT = 5;
    public static final int NAME_MAX = 14;
    public static final String NAME_ALLOWED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/+-_!?&";

    public String label = "";
    public Interval[] slots = new Interval[SLOT_COUNT];
    public int autoRepeat = 1;

    public CasioIntervalTimer() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = new Interval();
        }
    }

    public int totalSeconds(int slot) {
        Interval s = slots[slot];
        return s.minutes * 60 + s.seconds;
    }

    public static String normalizeName(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toUpperCase(java.util.Locale.ROOT).toCharArray()) {
            if (c == ' ') c = '_';
            if (NAME_ALLOWED.indexOf(c) >= 0 && sb.length() < NAME_MAX) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static int clampRepeat(int v) {
        if (v < 1) return 1;
        if (v > 20) return 20;
        return v;
    }

    public static String formatDuration(int totalSeconds) {
        final int h = totalSeconds / 3600;
        final int m = (totalSeconds % 3600) / 60;
        final int s = totalSeconds % 60;
        if (h > 0) {
            return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, s);
        }
        return String.format(java.util.Locale.ROOT, "%02d:%02d", m, s);
    }

    public static final class Interval {
        public String name = "";
        public boolean skipped = false;
        public int minutes = 0;
        public int seconds = 0;
    }
}
