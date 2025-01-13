package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import junit.framework.TestCase;

import org.junit.Ignore;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GFDIMessageTest extends TestCase {
    @Test
    @Ignore("helper test for development, remove this while debugging")
    public void testDecode() {
        final CobsCoDec cobsCoDec = new CobsCoDec();
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00020c0baa1380a4bd796705196600"));
        final byte[] deCobs = cobsCoDec.retrieveMessage();
        final GFDIMessage gfdiMessage = GFDIMessage.parseIncoming(deCobs);
    }
}