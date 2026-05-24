/*  Copyright (C) 2026 Dany Mestas

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiDailySummarySample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;

/**
 * Synthetic-input tests for {@link DailySummaryParser}. No raw bins on disk — every
 * input is constructed in-memory so the tests are portable and reviewable.
 */
public class DailySummaryParserTest {

    private static final long FAKE_TIMESTAMP_MS = 1_700_000_000_000L;
    private static final int FAKE_TIMEZONE = 4;

    private static final int V5_BODY_SIZE = 57;
    private static final int V3_BODY_SIZE = 41;

    // ----- file-id builders ---------------------------------------------------

    private static XiaomiActivityFileId v5FileId() {
        // ACTIVITY / ACTIVITY_DAILY / SUMMARY / version=5
        return new XiaomiActivityFileId(new Date(FAKE_TIMESTAMP_MS), FAKE_TIMEZONE, 0, 0, 1, 5);
    }

    private static XiaomiActivityFileId v3FileId() {
        return new XiaomiActivityFileId(new Date(FAKE_TIMESTAMP_MS), FAKE_TIMEZONE, 0, 0, 1, 3);
    }

    /** Builds bytes = 7B fileId + 1B padding(0) + bitmap + body. */
    private static byte[] buildFile(final XiaomiActivityFileId id, final byte[] bitmap, final byte[] body) {
        final byte[] out = new byte[7 + 1 + bitmap.length + body.length];
        System.arraycopy(id.toBytes(), 0, out, 0, 7);
        // out[7] padding already 0
        System.arraycopy(bitmap, 0, out, 8, bitmap.length);
        System.arraycopy(body, 0, out, 8 + bitmap.length, body.length);
        return out;
    }

    /** Body with every slot set to a recognisable, non-zero value. */
    private static byte[] v5BodyAllValues() {
        final ByteBuffer b = ByteBuffer.allocate(V5_BODY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(12345);              // slot 0  steps
        b.putShort((short) 250);      // slot 1  active calories
        b.put((byte) 75);             // slot 2  reserved
        b.put((byte) 60);             // slot 3  resting HR
        b.put((byte) 180);            // slot 4  max HR
        b.putInt(1700001000);         // slot 5  max HR ts
        b.put((byte) 55);             // slot 6  min HR
        b.putInt(1700002000);         // slot 7  min HR ts
        b.put((byte) 80);             // slot 8  avg HR
        b.put((byte) 40);             // slot 9  avg stress
        b.put((byte) 90);             // slot 10 max stress
        b.put((byte) 10);             // slot 11 min stress
        // slot 12 standing: bits 0, 8, 16 → hours 0, 8, 16
        b.put(new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x01});
        b.putShort((short) 2200);     // slot 13 calories
        b.putShort((short) 480);      // slot 14 recovery hours
        b.put((byte) 96);             // slot 15 reserved
        b.put((byte) 99);             // slot 16 max SpO2
        b.putInt(1700003000);         // slot 17 max SpO2 ts
        b.put((byte) 92);             // slot 18 min SpO2
        b.putInt(1700004000);         // slot 19 min SpO2 ts
        b.put((byte) 95);             // slot 20 avg SpO2
        b.putShort((short) 130);      // slot 21 training load (day)
        b.putShort((short) 700);      // slot 22 training load (week)
        b.put((byte) 2);              // slot 23 training load level
        b.put((byte) 5);              // slot 24 vitality light
        b.put((byte) 25);             // slot 25 vitality moderate
        b.put((byte) 40);             // slot 26 vitality high
        b.putShort((short) 280);      // slot 27 vitality current
        b.put((byte) 3);              // slot 28 reserved
        b.put((byte) 30);             // slot 29 reserved
        b.putShort((short) 7);        // slot 30 reserved
        b.putShort((short) 65);       // slot 31 reserved
        return b.array();
    }

    /** v3 reads only slots 0..20, which share byte layout with the v5 prefix. */
    private static byte[] v3BodyAllValues() {
        return Arrays.copyOfRange(v5BodyAllValues(), 0, V3_BODY_SIZE);
    }

    // ----- tests --------------------------------------------------------------

    @Test
    public void v5_allBitsSet_persistsEverySlot() {
        final byte[] bitmap = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        final byte[] bytes = buildFile(v5FileId(), bitmap, v5BodyAllValues());
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
        assertNotNull(s);

        assertEquals(Integer.valueOf(12345),      s.getSteps());
        assertEquals(Integer.valueOf(250),        s.getActiveCalories());
        assertEquals(Integer.valueOf(60),         s.getHrResting());
        assertEquals(Integer.valueOf(180),        s.getHrMax());
        assertEquals(Integer.valueOf(1700001000), s.getHrMaxTs());
        assertEquals(Integer.valueOf(55),         s.getHrMin());
        assertEquals(Integer.valueOf(1700002000), s.getHrMinTs());
        assertEquals(Integer.valueOf(80),         s.getHrAvg());
        assertEquals(Integer.valueOf(40),         s.getStressAvg());
        assertEquals(Integer.valueOf(90),         s.getStressMax());
        assertEquals(Integer.valueOf(10),         s.getStressMin());
        // standing: 3 bytes [01,01,01] → bit 0 of each byte set → hours 0, 8, 16
        assertEquals(Integer.valueOf(0x010101),   s.getStanding());
        assertEquals(Integer.valueOf(2200),       s.getCalories());
        assertEquals(Integer.valueOf(480),        s.getRecoveryHours());
        assertEquals(Integer.valueOf(99),         s.getSpo2Max());
        assertEquals(Integer.valueOf(1700003000), s.getSpo2MaxTs());
        assertEquals(Integer.valueOf(92),         s.getSpo2Min());
        assertEquals(Integer.valueOf(1700004000), s.getSpo2MinTs());
        assertEquals(Integer.valueOf(95),         s.getSpo2Avg());
        assertEquals(Integer.valueOf(130),        s.getTrainingLoadDay());
        assertEquals(Integer.valueOf(700),        s.getTrainingLoadWeek());
        assertEquals(Integer.valueOf(2),          s.getTrainingLoadLevel());
        assertEquals(Integer.valueOf(5),          s.getVitalityIncreaseLight());
        assertEquals(Integer.valueOf(25),         s.getVitalityIncreaseModerate());
        assertEquals(Integer.valueOf(40),         s.getVitalityIncreaseHigh());
        assertEquals(Integer.valueOf(280),        s.getVitalityCurrent());
    }

    @Test
    public void v5_noBitsSet_persistsNothing() {
        final byte[] bitmap = new byte[4];                          // all zero
        final byte[] body = new byte[V5_BODY_SIZE];
        Arrays.fill(body, (byte) 0xff);                             // garbage that must NOT be stored
        final byte[] bytes = buildFile(v5FileId(), bitmap, body);
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
        assertNotNull(s);

        assertNull(s.getSteps());
        assertNull(s.getActiveCalories());
        assertNull(s.getHrResting());
        assertNull(s.getHrMax());
        assertNull(s.getHrMaxTs());
        assertNull(s.getStanding());
        assertNull(s.getCalories());
        assertNull(s.getRecoveryHours());
        assertNull(s.getTrainingLoadDay());
        assertNull(s.getVitalityIncreaseLight());
        assertNull(s.getVitalityIncreaseModerate());
        assertNull(s.getVitalityIncreaseHigh());
        assertNull(s.getVitalityCurrent());
    }

    /**
     * Reproduces the user-reported regression scenario: low/medium/high day vitality
     * are marked invalid, only the current vitality value is valid. Garbage bytes in the
     * three invalid slots must not be persisted.
     */
    @Test
    public void v5_partialVitality_invalidBitsYieldNull() {
        // Bitmap: set only slot 0 (steps) and slot 27 (vitality current). All other bits clear.
        //   slot 0  → byte 0, bit 7 (MSB) → 0x80
        //   slot 27 → byte 3, bit 4       → 1 << (7 - 3) = 0x10
        final byte[] bitmap = new byte[]{(byte) 0x80, 0, 0, (byte) 0x10};
        // Body: same recognisable values as the "all valid" test
        final byte[] body = v5BodyAllValues();
        final byte[] bytes = buildFile(v5FileId(), bitmap, body);
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
        assertNotNull(s);

        // Valid bits → values present
        assertEquals(Integer.valueOf(12345), s.getSteps());
        assertEquals(Integer.valueOf(280),   s.getVitalityCurrent());

        // Invalid vitality bits → null even though the body bytes carry real numbers
        assertNull(s.getVitalityIncreaseLight());
        assertNull(s.getVitalityIncreaseModerate());
        assertNull(s.getVitalityIncreaseHigh());

        // Spot-check a handful of other invalid slots
        assertNull(s.getActiveCalories());
        assertNull(s.getHrMax());
        assertNull(s.getCalories());
    }

    /**
     * Verifies the bitmap is interpreted MSB-first within each byte. A single bit set
     * at slot index N must map to bit (7 - N % 8) of byte (N / 8).
     */
    @Test
    public void v5_bitmap_isMsbFirstWithinEachByte() {
        for (int slot : new int[]{0, 7, 8, 23}) {
            final byte[] bitmap = new byte[4];
            bitmap[slot / 8] = (byte) (1 << (7 - (slot % 8)));
            final byte[] bytes = buildFile(v5FileId(), bitmap, v5BodyAllValues());
            final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
            assertNotNull(s);
            // The slot that got its bit set must have a non-null getter; an adjacent slot must be null.
            switch (slot) {
                case 0:  assertNotNull("slot 0 valid → steps non-null",      s.getSteps());
                         assertNull   ("slot 1 invalid → activeCalories null", s.getActiveCalories()); break;
                case 7:  assertNotNull("slot 7 valid → hrMinTs non-null",    s.getHrMinTs());
                         assertNull   ("slot 6 invalid → hrMin null",        s.getHrMin()); break;
                case 8:  assertNotNull("slot 8 valid → hrAvg non-null",      s.getHrAvg());
                         assertNull   ("slot 7 invalid → hrMinTs null",      s.getHrMinTs()); break;
                case 23: assertNotNull("slot 23 valid → trainingLoadLevel non-null", s.getTrainingLoadLevel());
                         assertNull   ("slot 22 invalid → trainingLoadWeek null",    s.getTrainingLoadWeek()); break;
                default: throw new AssertionError("unhandled slot " + slot);
            }
        }
    }

    /** Multi-byte fields are little-endian. */
    @Test
    public void v5_multibyteFields_areLittleEndian() {
        // Bitmap: slot 0 (steps int) and slot 27 (vitality current short).
        final byte[] bitmap = new byte[]{(byte) 0x80, 0, 0, (byte) 0x10};
        final byte[] body = new byte[V5_BODY_SIZE];
        // steps = 0x04030201 LE → bytes 0..3 = 01 02 03 04
        body[0] = 0x01; body[1] = 0x02; body[2] = 0x03; body[3] = 0x04;
        // Position of slot 27: cumulative byte offset of all earlier slots.
        final int slot27Offset = 4 + 2 + 1 + 1 + 1 + 4 + 1 + 4 + 1 + 1 + 1 + 1 + 3 + 2 + 2 + 1 + 1 + 4 + 1 + 4 + 1 + 2 + 2 + 1 + 1 + 1 + 1;
        body[slot27Offset]     = 0x02;
        body[slot27Offset + 1] = 0x01;
        final byte[] bytes = buildFile(v5FileId(), bitmap, body);
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
        assertNotNull(s);

        assertEquals(Integer.valueOf(0x04030201), s.getSteps());
        assertEquals(Integer.valueOf(0x0102),     s.getVitalityCurrent());
    }

    /** Three standing bytes pack into low 24 bits of an int. */
    @Test
    public void v5_standing_packs3BytesInto24BitInt() {
        // Bitmap: only slot 12 valid.
        //   slot 12 → byte 1, bit 3 → 1 << (7 - 4) = 0x08
        final byte[] bitmap = new byte[]{0, (byte) 0x08, 0, 0};
        final byte[] body = new byte[V5_BODY_SIZE];
        final int standingOffset = 4 + 2 + 1 + 1 + 1 + 4 + 1 + 4 + 1 + 1 + 1 + 1;
        body[standingOffset]     = (byte) 0xAA;
        body[standingOffset + 1] = (byte) 0xBB;
        body[standingOffset + 2] = (byte) 0xCC;
        final byte[] bytes = buildFile(v5FileId(), bitmap, body);
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v5FileId(), bytes);
        assertNotNull(s);

        // packed = byte0 | (byte1 << 8) | (byte2 << 16)
        assertEquals(Integer.valueOf(0xCCBBAA), s.getStanding());
    }

    /** Unsupported version → null sample, no exception. */
    @Test
    public void unsupportedVersion_returnsNull() {
        final XiaomiActivityFileId id = new XiaomiActivityFileId(
                new Date(FAKE_TIMESTAMP_MS), FAKE_TIMEZONE, 0, 0, 1, /* version */ 99);
        final byte[] bytes = new byte[8];
        System.arraycopy(id.toBytes(), 0, bytes, 0, 7);
        assertNull(DailySummaryParser.decode(id, bytes));
    }

    /**
     * v3 path with every bit set: slots 0..20 must be populated, v5-only slots remain null.
     * v3 has a 3-byte bitmap (24 bits) covering 21 slots, the same as workout summaries'
     * per-version header sizing.
     */
    @Test
    public void v3_allBitsSet_persistsSlots0to20() {
        final byte[] bitmap = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff};
        final byte[] bytes = buildFile(v3FileId(), bitmap, v3BodyAllValues());
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v3FileId(), bytes);
        assertNotNull(s);

        assertEquals(Integer.valueOf(12345),      s.getSteps());
        assertEquals(Integer.valueOf(250),        s.getActiveCalories());
        assertEquals(Integer.valueOf(60),         s.getHrResting());
        assertEquals(Integer.valueOf(180),        s.getHrMax());
        assertEquals(Integer.valueOf(1700001000), s.getHrMaxTs());
        assertEquals(Integer.valueOf(55),         s.getHrMin());
        assertEquals(Integer.valueOf(1700002000), s.getHrMinTs());
        assertEquals(Integer.valueOf(80),         s.getHrAvg());
        assertEquals(Integer.valueOf(40),         s.getStressAvg());
        assertEquals(Integer.valueOf(90),         s.getStressMax());
        assertEquals(Integer.valueOf(10),         s.getStressMin());
        assertEquals(Integer.valueOf(0x010101),   s.getStanding());
        assertEquals(Integer.valueOf(2200),       s.getCalories());
        assertEquals(Integer.valueOf(480),        s.getRecoveryHours());
        assertEquals(Integer.valueOf(99),         s.getSpo2Max());
        assertEquals(Integer.valueOf(1700003000), s.getSpo2MaxTs());
        assertEquals(Integer.valueOf(92),         s.getSpo2Min());
        assertEquals(Integer.valueOf(1700004000), s.getSpo2MinTs());
        assertEquals(Integer.valueOf(95),         s.getSpo2Avg());

        // v5-only slots must remain null.
        assertNull(s.getTrainingLoadDay());
        assertNull(s.getTrainingLoadWeek());
        assertNull(s.getTrainingLoadLevel());
        assertNull(s.getVitalityIncreaseLight());
        assertNull(s.getVitalityIncreaseModerate());
        assertNull(s.getVitalityIncreaseHigh());
        assertNull(s.getVitalityCurrent());
    }

    /** v3 with zero bitmap: nothing persisted despite garbage in the body. */
    @Test
    public void v3_noBitsSet_persistsNothing() {
        final byte[] bitmap = new byte[3];                          // all zero
        final byte[] body = new byte[V3_BODY_SIZE];
        Arrays.fill(body, (byte) 0xff);                             // garbage that must NOT be stored
        final byte[] bytes = buildFile(v3FileId(), bitmap, body);
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v3FileId(), bytes);
        assertNotNull(s);

        assertNull(s.getSteps());
        assertNull(s.getActiveCalories());
        assertNull(s.getHrResting());
        assertNull(s.getHrMax());
        assertNull(s.getHrMaxTs());
        assertNull(s.getStanding());
        assertNull(s.getCalories());
        assertNull(s.getRecoveryHours());
        assertNull(s.getSpo2Avg());
    }

    /** v3 partial bitmap: only the bits set are persisted, the rest are null. */
    @Test
    public void v3_partialBitmap_gatesPerField() {
        // Bitmap: only slot 4 (hrMax) set → byte 0, bit 3 = 1 << (7 - 4) = 0x08
        final byte[] bitmap = new byte[]{(byte) 0x08, 0, 0};
        final byte[] bytes = buildFile(v3FileId(), bitmap, v3BodyAllValues());
        final XiaomiDailySummarySample s = DailySummaryParser.decode(v3FileId(), bytes);
        assertNotNull(s);

        assertEquals(Integer.valueOf(180), s.getHrMax());

        // Everything else null.
        assertNull(s.getSteps());
        assertNull(s.getActiveCalories());
        assertNull(s.getHrResting());
        assertNull(s.getHrMaxTs());
        assertNull(s.getHrMin());
        assertNull(s.getHrAvg());
        assertNull(s.getStanding());
        assertNull(s.getCalories());
        assertNull(s.getSpo2Avg());
    }
}
