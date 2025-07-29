package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class ZeppOsActivitySummaryParserTest extends TestBase {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTest() throws IOException {
        final byte[] data = Files.readAllBytes(Paths.get("/storage/activity.bin"));

        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(data);
        summary.setStartTime(new Date());

        final ZeppOsActivitySummaryParser parser = new ZeppOsActivitySummaryParser(getContext());
        parser.parseBinaryData(summary, summary.getStartTime(), true);
    }
}
