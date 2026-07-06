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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuamiActivitySummaryParserTest extends TestBase {
    // Captured on device: Mi Band 6 running workout, classic summary version 519.
    private static final String SUMMARY_V519_RUNNING =
            "070201003d94446a7e9a446a0000000000000000e0b1ffff0000ffff00000000" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000100000000000f00" +
            "0800000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000ff1000003d06000000000080ffffff7f00000080" +
            "ffffff7fdcfcb3437ab7484500000000000000000050c3c70050c34700000000" +
            "6666664000000000835f384000808a43e4388e3e73a3fe3ebcbb3b4000000000" +
            "98762d40fc67613f000000003d56423ff4371a450e0c0000ac00f1014f00bb00" +
            "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Regression test for the version 519 summary layout, where Min HR and the slowest pace
     * were emitted as garbage sentinel values (-256 bpm and 277 s/m). Both should now be
     * suppressed while the genuine stats remain intact.
     */
    @Test
    public void testParseSummary_v519_running_dropsGarbageMinHrAndSlowestPace() {
        final byte[] bytes = GB.hexStringToByteArray(SUMMARY_V519_RUNNING);

        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(bytes);
        summary.setStartTime(new Date(1782879293000L));

        final HuamiActivitySummaryParser parser = new HuamiActivitySummaryParser();
        parser.parseBinaryData(summary, false);

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summary.getSummaryData());
        assertNotNull(summaryData);

        // The two bugs: garbage sentinels must not be reported.
        assertFalse("min HR (-256 sentinel) must be dropped", summaryData.has(ActivitySummaryEntries.HR_MIN));
        assertFalse("slowest pace (277 s/m sentinel) must be dropped", summaryData.has(ActivitySummaryEntries.PACE_MIN));

        // Genuine stats stay intact.
        assertTrue(summaryData.has(ActivitySummaryEntries.HR_MAX));
        assertEquals(187d, summaryData.getNumber(ActivitySummaryEntries.HR_MAX, -1));
        assertEquals(172d, summaryData.getNumber(ActivitySummaryEntries.HR_AVG, -1));
        assertEquals(4351d, summaryData.getNumber(ActivitySummaryEntries.STEPS, -1));
        assertEquals(1597d, summaryData.getNumber(ActivitySummaryEntries.ACTIVE_SECONDS, -1));
        assertEquals(359.975d, summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, -1).doubleValue(), 0.01);
        assertEquals(3211.467d, summaryData.getNumber(ActivitySummaryEntries.DISTANCE_METERS, -1).doubleValue(), 0.01);
    }
}
