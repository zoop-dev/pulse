/*  Copyright (C) 2023-2026 José Rebelo, Dany Mestas

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.XiaomiDailySummarySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiDailySummarySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DailySummaryParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(DailySummaryParser.class);

    @FunctionalInterface
    private interface SlotReader {
        void read(ByteBuffer buf, XiaomiDailySummarySample sample, boolean valid);
    }

    /**
     * Ordered slot list shared by all supported daily-summary versions. Slot index
     * matches the bit position in the per-version validity bitmap (MSB-first within
     * each header byte). Each slot always consumes its byte count; only the setter
     * call is gated on the bit so undefined bytes are discarded rather than stored.
     * Older versions read the first N slots; newer versions read the full table.
     */
    private static final SlotReader[] SLOTS = {
            /*  0 steps                       */ (b, s, v) -> { final int x = b.getInt();             if (v) s.setSteps(x); },
            /*  1 active calories             */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setActiveCalories(x); },
            /*  2 reserved (1 byte)           */ (b, s, v) -> { b.get(); },
            /*  3 resting HR                  */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setHrResting(x); },
            /*  4 max HR                      */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setHrMax(x); },
            /*  5 max HR timestamp            */ (b, s, v) -> { final int x = b.getInt();             if (v) s.setHrMaxTs(x); },
            /*  6 min HR                      */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setHrMin(x); },
            /*  7 min HR timestamp            */ (b, s, v) -> { final int x = b.getInt();             if (v) s.setHrMinTs(x); },
            /*  8 avg HR                      */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setHrAvg(x); },
            /*  9 avg stress                  */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setStressAvg(x); },
            /* 10 max stress                  */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setStressMax(x); },
            /* 11 min stress                  */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setStressMin(x); },
            /* 12 24h standing bitmap         */ (b, s, v) -> {
                final byte[] sb = new byte[3];
                b.get(sb);
                // each bit represents one hour where the user was standing up, starting
                // at 00:00-01:00. Pack into a single int; mask each byte first so bytes
                // >= 0x80 don't sign-extend and corrupt the upper bytes of the result.
                final int packed = (sb[0] & 0xff) | ((sb[1] & 0xff) << 8) | ((sb[2] & 0xff) << 16);
                if (v) s.setStanding(packed);
            },
            /* 13 calories                    */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setCalories(x); },
            /* 14 recovery hours              */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setRecoveryHours(x); },
            /* 15 reserved (1 byte)           */ (b, s, v) -> { b.get(); },
            /* 16 max SpO2                    */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setSpo2Max(x); },
            /* 17 max SpO2 timestamp          */ (b, s, v) -> { final int x = b.getInt();             if (v) s.setSpo2MaxTs(x); },
            /* 18 min SpO2                    */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setSpo2Min(x); },
            /* 19 min SpO2 timestamp          */ (b, s, v) -> { final int x = b.getInt();             if (v) s.setSpo2MinTs(x); },
            /* 20 avg SpO2                    */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setSpo2Avg(x); },
            /* 21 training load (day)         */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setTrainingLoadDay(x); },
            /* 22 training load (week)        */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setTrainingLoadWeek(x); },
            /* 23 training load level         */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setTrainingLoadLevel(x); },
            /* 24 vitality light              */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setVitalityIncreaseLight(x); },
            /* 25 vitality moderate           */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setVitalityIncreaseModerate(x); },
            /* 26 vitality high               */ (b, s, v) -> { final int x = b.get() & 0xff;         if (v) s.setVitalityIncreaseHigh(x); },
            /* 27 vitality current            */ (b, s, v) -> { final int x = b.getShort() & 0xffff;  if (v) s.setVitalityCurrent(x); },
            /* 28 reserved (1 byte)           */ (b, s, v) -> { b.get(); },
            /* 29 reserved (1 byte)           */ (b, s, v) -> { b.get(); },
            /* 30 reserved (2 bytes)          */ (b, s, v) -> { b.getShort(); },
            /* 31 reserved (2 bytes)          */ (b, s, v) -> { b.getShort(); },
    };

    @Override
    public boolean parse(final Context context, final GBDevice device, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final XiaomiDailySummarySample sample = decode(fileId, bytes);
        if (sample == null) {
            return false;
        }

        LOG.debug("Persisting 1 daily summary sample");

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            sample.setDevice(DBHelper.getDevice(device, session));
            sample.setUser(DBHelper.getUser(session));

            final XiaomiDailySummarySampleProvider sampleProvider = new XiaomiDailySummarySampleProvider(device, session);
            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(context, "Error saving daily summary", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving daily summary", e);
            return false;
        }

        return true;
    }

    /**
     * Pure decode of the raw bytes. Returns null when the file version is unsupported.
     * v3 and v5 share the same {@link #SLOTS} table; they differ only in the number
     * of slots present and the header (validity bitmap) size, mirroring how
     * {@code WorkoutSummaryParser} sizes its per-version header to match the field
     * count it adds via the simple-parser builder.
     */
    static XiaomiDailySummarySample decode(final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        final int slotCount;
        switch (version) {
            case 3:   // Smart Band 8 Active
                headerSize = 3;
                slotCount = 21;
                break;
            case 5:   // Mi Band 10 and later
                headerSize = 4;
                slotCount = 32;
                break;
            default:
                LOG.warn("Unable to parse daily summary version {}", version);
                return null;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }

        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Header (validity bitmap): {}", GB.hexdump(header));

        final XiaomiDailySummarySample sample = new XiaomiDailySummarySample();
        sample.setTimestamp(fileId.getTimestamp().getTime());
        sample.setTimezone(fileId.getTimezone());

        for (int i = 0; i < slotCount; i++) {
            SLOTS[i].read(buf, sample, validData(header, i));
        }
        return sample;
    }
}
