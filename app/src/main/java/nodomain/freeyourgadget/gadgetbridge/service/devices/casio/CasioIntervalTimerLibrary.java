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

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.gson.GsonSerialized;

@GsonSerialized
public class CasioIntervalTimerLibrary {
    private static final Logger LOG = LoggerFactory.getLogger(CasioIntervalTimerLibrary.class);

    public static final String PREF_INTERVAL_TIMER_LIBRARY = "casio_interval_timers";
    public static final String CONFIG_INTERVAL_TIMER_ACTIVE = "casio_interval_timer_active";
    public static final int MAX_TIMERS = 50;
    private static final Gson GSON = new Gson();

    public List<CasioIntervalTimer> timers = new ArrayList<>();
    public int activeIndex = -1;

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CasioIntervalTimerLibrary fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new CasioIntervalTimerLibrary();
        }
        try {
            CasioIntervalTimerLibrary lib = GSON.fromJson(json, CasioIntervalTimerLibrary.class);
            if (lib == null || lib.timers == null) {
                LOG.warn("Stored interval timer library is incomplete, discarding it");
                return new CasioIntervalTimerLibrary();
            }
            if (lib.activeIndex < -1 || lib.activeIndex >= lib.timers.size()) lib.activeIndex = -1;
            return lib;
        } catch (Exception e) {
            LOG.error("Failed to parse stored interval timer library, discarding it", e);
            return new CasioIntervalTimerLibrary();
        }
    }

    public boolean add(CasioIntervalTimer t) {
        if (timers.size() >= MAX_TIMERS) return false;
        timers.add(t);
        return true;
    }

    public void remove(int index) {
        if (index < 0 || index >= timers.size()) return;
        timers.remove(index);
        if (activeIndex == index) {
            activeIndex = -1;
        } else if (activeIndex > index) {
            activeIndex--;
        }
    }

    public CasioIntervalTimer getActive() {
        if (activeIndex < 0 || activeIndex >= timers.size()) return null;
        return timers.get(activeIndex);
    }

    public void setActive(int index) {
        if (index >= -1 && index < timers.size()) activeIndex = index;
    }

    public boolean reconcileFromWatch(CasioIntervalTimer fromWatch) {
        if (fromWatch == null) return false;
        CasioIntervalTimer active = getActive();
        if (active == null) {
            if (!add(fromWatch)) return false;
            setActive(timers.size() - 1);
            return true;
        }
        if (Arrays.deepEquals(CasioIntervalTimerCodec.encode(active),
                              CasioIntervalTimerCodec.encode(fromWatch))) {
            return false;
        }
        timers.set(activeIndex, fromWatch);
        return true;
    }
}
