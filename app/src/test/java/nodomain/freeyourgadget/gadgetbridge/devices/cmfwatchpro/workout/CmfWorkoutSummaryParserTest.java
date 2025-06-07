package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CmfWorkoutSummaryParserTest extends TestBase {
    @Test
    public void testParseSummary2() {
        // From https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/4530
        final byte[] bytes = GB.hexStringToByteArray("9F7734685D02039D8B0008060000FA0500007B01000000000000057A346800001B0064000000B40001000100000006000000401F0000");

        final CmfWorkoutSummaryParser parser = new CmfWorkoutSummaryParser(null, getContext());
        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(bytes);
        parser.parseBinaryData(summary, false);

        assertEquals(1748268959000L, summary.getStartTime().getTime());
        assertEquals(1748269573000L, summary.getEndTime().getTime());
        assertEquals(ActivityKind.INDOOR_RUNNING.getCode(), summary.getActivityKind());

        final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summary.getSummaryData());
        assertNotNull(summaryData);

        assertEquals(1530d, summaryData.getNumber(ActivitySummaryEntries.DISTANCE_METERS, -1));
        assertEquals(605d, summaryData.getNumber(ActivitySummaryEntries.ACTIVE_SECONDS, -1));
        assertEquals(139d, summaryData.getNumber(ActivitySummaryEntries.CALORIES_BURNT, -1));

        // TODO max pace 6'23''
        // TODO avg speed 6.50kmph
        // TODO avg pace 6'19"

        assertEquals(1544d, summaryData.getNumber(ActivitySummaryEntries.STEPS, -1));

        // TODO avg step rate 151 steps/min
        // TODO avg step stride 102cm

        assertEquals(157d, summaryData.getNumber(ActivitySummaryEntries.HR_AVG, -1));

        // TODO max hr 190

        assertEquals(1 * 60d, summaryData.getNumber(ActivitySummaryEntries.HR_ZONE_WARM_UP, -1)); // 1 min
        assertEquals(1 * 60d, summaryData.getNumber(ActivitySummaryEntries.HR_ZONE_FAT_BURN, -1)); // 1 min
        assertEquals(0d, summaryData.getNumber(ActivitySummaryEntries.HR_ZONE_AEROBIC, -1)); // 0 min
        assertEquals(6 * 60d, summaryData.getNumber(ActivitySummaryEntries.HR_ZONE_ANAEROBIC, -1)); // 6 min

        // TODO aerobic endurance 1.0 / relaxed

        assertEquals(27d, summaryData.getNumber(ActivitySummaryEntries.TRAINING_LOAD, -1));
        assertEquals(3 * 60 * 60d, summaryData.getNumber(ActivitySummaryEntries.RECOVERY_TIME, -1)); // 3h
        assertEquals(8d, summaryData.getNumber(ActivitySummaryEntries.ACTIVE_SCORE, -1)); // +8
    }
}
