package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class ZeppOsActivityDetailsParserTest extends TestBase {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTest() throws Exception {
        final byte[] bytes = Files.readAllBytes(Paths.get("/storage/downloads/raw_details.bin"));

        final BaseActivitySummary summary = new BaseActivitySummary();
        summary.setRawSummaryData(bytes);
        summary.setBaseLatitude(0);
        summary.setBaseLongitude(0);
        summary.setBaseAltitude(0);

        final ZeppOsActivityDetailsParser parser = new ZeppOsActivityDetailsParser(summary);
        final ZeppOsActivityTrack track = parser.parse(bytes);
    }
}
