package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MoyoungWorkoutSummaryParserTest extends TestBase {
    @Test
    public void testParseWorkout1() {
        // #6053 - 00:36:34, 2.3km, 163kcal, 15'41'', 2767 steps, 76 spm, 89bpm
        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(GB.hexStringToByteArray("030D257C126AB984126A92080022CF0A00001A090000A3005900A9C1C440"));

        new MoyoungWorkoutSummaryParser(null).parseBinaryData(summary, false);

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summary.getSummaryData());
        assertNotNull(summaryData);

        assertEquals((double) (36 * 60 + 34), summaryData.getNumber(ActivitySummaryEntries.ACTIVE_SECONDS, -1));
        assertEquals(2330d, summaryData.getNumber(ActivitySummaryEntries.DISTANCE_METERS, -1));
        assertEquals(163d, summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, -1));
        assertEquals(2767d, summaryData.getNumber(ActivitySummaryEntries.STEPS, -1));
        assertEquals(89d, summaryData.getNumber(ActivitySummaryEntries.HR_AVG, -1));
    }

    @Test
    public void testParseWorkout2() {
        // #6053 - 00:32:01, 1.7km, 122kcal, 18'20'', 2011 steps, 62 spm, 93bpm
        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(GB.hexStringToByteArray("030E9BAF136A1CB7136A8107001EDB070000D20600007A005D003DB8C740"));

        new MoyoungWorkoutSummaryParser(null).parseBinaryData(summary, false);

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summary.getSummaryData());
        assertNotNull(summaryData);

        assertEquals((double) (32 * 60 + 1), summaryData.getNumber(ActivitySummaryEntries.ACTIVE_SECONDS, -1));
        assertEquals(1746d, summaryData.getNumber(ActivitySummaryEntries.DISTANCE_METERS, -1));
        assertEquals(122d, summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, -1));
        assertEquals(2011d, summaryData.getNumber(ActivitySummaryEntries.STEPS, -1));
        assertEquals(93d, summaryData.getNumber(ActivitySummaryEntries.HR_AVG, -1));
    }
}
