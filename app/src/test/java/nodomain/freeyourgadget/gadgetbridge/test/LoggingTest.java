package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.Logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests dynamic enablement and disablement of file appenders.
 */
public class LoggingTest extends TestBase {

    public LoggingTest() {
    }

    @Test
    public void testToggleLogging() {
        final Logging logging = Logging.getInstance();

        try {
            logging.setFileLoggingEnabled(true);
            assertNotNull(logging.getFileLogger());
            assertTrue(logging.getFileLogger().isStarted());

            logging.setFileLoggingEnabled(false);
            assertNull(logging.getFileLogger());

            logging.setFileLoggingEnabled(true);
            assertNotNull(logging.getFileLogger());
            assertTrue(logging.getFileLogger().isStarted());
        } catch (AssertionError ex) {
            logging.debugLoggingConfiguration();
            System.err.println(System.getProperty("java.class.path"));
            throw ex;
        }
    }

    @Test
    public void testLogFormat() {
        String tempOut = Logging.formatBytes(new byte[] {0xa});
        assertEquals("0a", tempOut);

        tempOut = Logging.formatBytes(new byte[] {0xa, 1, (byte) 255});
        assertEquals("0a 01 ff", tempOut);
    }
}
