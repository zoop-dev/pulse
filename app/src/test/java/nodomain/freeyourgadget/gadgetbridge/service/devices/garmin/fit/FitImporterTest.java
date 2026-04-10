package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class FitImporterTest extends TestBase {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTest() throws Exception {
        final FitImporter fitImporter = new FitImporter(  null, null);
        fitImporter.importFile(new File("/storage/SKIN_TEMP.fit"), false);
    }

    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void localTestFolder() throws Exception {
        final File dir = new File("/storage/MONITOR/2026/");
        final File[] files = dir.listFiles();
        Assert.assertNotNull(files);
        for (File file : files) {
            if (!file.getName().endsWith(".fit")) {
                continue;
            }
            final FitImporter fitImporter = new FitImporter(  null, null);
            fitImporter.importFile(file, false);
        }
    }
}
