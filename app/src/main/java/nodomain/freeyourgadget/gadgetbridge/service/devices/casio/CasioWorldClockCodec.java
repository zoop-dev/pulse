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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Builds world-clock write frames in the official app's order (capture 2026-07-17):
 *  0x1d pairs (padded to the captured 15-byte length), then 0x1e per slot, then
 *  optional 0x1f names — the WS-B1000 protocol never uses 0x1f. */
public final class CasioWorldClockCodec {
    public static final int DST_WATCH_STATE_FRAME_LENGTH = 15;

    /** 0x21 settings-session bracket: the watch silently discards clock writes not
     *  preceded by SESSION_OPEN and NACKs close frames without a matching open.
     *  Hardware-verified on GBD-200 2026-07-17; the WS-B1000 app uses the same bracket. */
    public static final byte[] SESSION_OPEN = {0x21, 0x00, 0x01};
    public static final byte[] SESSION_CLOSE_A = {0x21, 0x01, 0x01};
    public static final byte[] SESSION_CLOSE_B = {0x21, 0x00, 0x04};
    public static final byte[] SESSION_CLOSE_C = {0x21, 0x01, 0x04};

    private CasioWorldClockCodec() {
    }

    public static byte[] dstWatchStateFrame(int slotA, CasioTimeZone zoneA, int slotB, CasioTimeZone zoneB) {
        byte[] frame = Arrays.copyOf(
                CasioTimeZone.dstWatchStateBytes(slotA, zoneA, slotB, zoneB),
                DST_WATCH_STATE_FRAME_LENGTH);
        for (int i = 13; i < DST_WATCH_STATE_FRAME_LENGTH; i++) {
            frame[i] = (byte) 0xff;
        }
        return frame;
    }

    public static List<byte[]> clockFrames(CasioTimeZone[] zones, boolean withNames) {
        List<byte[]> frames = new ArrayList<>();
        for (int i = 0; i + 1 < zones.length; i += 2) {
            frames.add(dstWatchStateFrame(i, zones[i], i + 1, zones[i + 1]));
        }
        for (int i = 0; i < zones.length; i++) {
            frames.add(zones[i].dstSettingBytes(i));
        }
        if (withNames) {
            for (int i = 0; i < zones.length; i++) {
                frames.add(zones[i].worldCityBytes(i));
            }
        }
        return frames;
    }
}
