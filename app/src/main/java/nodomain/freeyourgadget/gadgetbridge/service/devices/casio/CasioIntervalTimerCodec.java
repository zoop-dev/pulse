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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class CasioIntervalTimerCodec {
    public static final byte FEATURE_NAME   = (byte) 0x44; // per-slot name packet
    public static final byte FEATURE_CONFIG = (byte) 0x2a; // durations + repeat packet

    private static final int NAME_PACKET_LEN = 20;  // 0x44 + slot + 18-byte name
    private static final int NAME_FIELD_LEN  = 18;  // bytes available for the name
    private static final int CONFIG_LEN      = 17;  // 0x2a + repeat + 5*(sec,min,reserved)

    private CasioIntervalTimerCodec() {}

    private static byte toBcd(int v) { return (byte) (((v / 10) << 4) | (v % 10)); }
    private static int fromBcd(byte b) { return ((b >> 4) & 0x0f) * 10 + (b & 0x0f); }
    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    /** Returns the 6 ALL_FEATURES packets: five 0x44 name packets then one 0x2a config. */
    public static byte[][] encode(CasioIntervalTimer timer) {
        byte[][] out = new byte[CasioIntervalTimer.SLOT_COUNT + 1][];
        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            byte[] p = new byte[NAME_PACKET_LEN];
            p[0] = FEATURE_NAME;
            p[1] = (byte) (i + 1); // 1-based slot number
            byte[] name = CasioIntervalTimer.normalizeName(timer.slots[i].name)
                    .getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(name, 0, p, 2, Math.min(name.length, NAME_FIELD_LEN));
            // remaining bytes stay 0x00
            out[i] = p;
        }
        byte[] cfg = new byte[CONFIG_LEN];
        cfg[0] = FEATURE_CONFIG;
        cfg[1] = (byte) CasioIntervalTimer.clampRepeat(timer.autoRepeat); // binary
        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            int base = 2 + i * 3;
            CasioIntervalTimer.Interval s = timer.slots[i];
            int min = s.skipped ? 0 : clamp(s.minutes, 0, 60);
            int sec = s.skipped ? 0 : clamp(s.seconds, 0, 59);
            cfg[base]     = toBcd(sec); // seconds precede minutes
            cfg[base + 1] = toBcd(min);
            cfg[base + 2] = 0x00;       // reserved
        }
        out[CasioIntervalTimer.SLOT_COUNT] = cfg;
        return out;
    }

    /** Rebuilds a timer from a set of packets (names + config, any order). */
    public static CasioIntervalTimer decode(byte[][] packets) {
        if (packets == null) return null;
        CasioIntervalTimer t = new CasioIntervalTimer();
        byte[] config = null;
        for (byte[] raw : packets) {
            if (raw == null || raw.length == 0) continue;
            byte[] p = raw;
            if (p.length >= 2 && p[0] == (byte) 0xFF && p[1] == (byte) 0x81) {
                p = Arrays.copyOfRange(p, 2, p.length); // strip read-back prefix
            }
            if (p.length == 0) continue;
            if (p[0] == FEATURE_CONFIG) {
                config = p;
            } else if (p[0] == FEATURE_NAME && p.length >= 2) {
                applyName(p, t);
            }
        }
        if (config == null || config.length < CONFIG_LEN) return null;
        t.autoRepeat = CasioIntervalTimer.clampRepeat(config[1] & 0xff);
        for (int i = 0; i < CasioIntervalTimer.SLOT_COUNT; i++) {
            int base = 2 + i * 3;
            CasioIntervalTimer.Interval s = t.slots[i];
            s.seconds = fromBcd(config[base]);
            s.minutes = fromBcd(config[base + 1]);
            s.skipped = (s.seconds == 0 && s.minutes == 0); // skip == 00'00"
        }
        return t;
    }

    private static void applyName(byte[] p, CasioIntervalTimer t) {
        int slot = p[1] & 0xff; // 1-based
        if (slot < 1 || slot > CasioIntervalTimer.SLOT_COUNT) return;
        int end = 2;
        while (end < p.length && end < 2 + NAME_FIELD_LEN && p[end] != 0) end++;
        t.slots[slot - 1].name = new String(p, 2, end - 2, StandardCharsets.US_ASCII);
    }
}
