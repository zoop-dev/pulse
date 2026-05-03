package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.nothing.NothingEqualizer;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.util.CheckSums.getCRC16ansi;

public class NothingProtocolTest extends TestBase {
    @Test
    public void testDecodeEqualizerAndUltraBassStatus() {
        final Ear1Support.NothingProtocol protocol = new Ear1Support.NothingProtocol(true);

        final GBDeviceEvent[] equalizerEvents = protocol.decodeResponse(GB.hexStringToByteArray("5560015040010060057f43"));
        final GBDeviceEventUpdatePreferences equalizerUpdate = findEvent(equalizerEvents, GBDeviceEventUpdatePreferences.class);
        Assert.assertNotNull(equalizerUpdate);
        Assert.assertEquals(NothingEqualizer.CLASSICAL.name(), equalizerUpdate.preferences.get(DeviceSettingsPreferenceConst.PREF_HEADPHONES_EQUALIZER));

        final GBDeviceEvent[] ultraBassEvents = protocol.decodeResponse(GB.hexStringToByteArray("5560014e4002001b00028071"));
        final GBDeviceEventUpdatePreferences ultraBassUpdate = findEvent(ultraBassEvents, GBDeviceEventUpdatePreferences.class);
        Assert.assertNotNull(ultraBassUpdate);
        Assert.assertEquals(false, ultraBassUpdate.preferences.get(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_ULTRA_BASS_ENABLED));
        Assert.assertEquals(1, ultraBassUpdate.preferences.get(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_ULTRA_BASS_LEVEL));
    }

    @Test
    public void testEncodeEqualizerCommand() {
        final Ear1Support.NothingProtocol protocol = new Ear1Support.NothingProtocol(true);

        final byte[] packet = protocol.encodeEqualizer(NothingEqualizer.ELECTRONIC.name());
        final byte[] expected = GB.hexStringToByteArray("5560011df00200140200ebcd");
        assertMessageEquals(expected, packet);
    }

    @Test
    public void testEncodeUltraBassCommand() {
        final Ear1Support.NothingProtocol protocol = new Ear1Support.NothingProtocol(true);

        final byte[] packet = protocol.encodeUltraBass(true, 2);
        final byte[] expected = GB.hexStringToByteArray("55600151f0020070010426e5");
        assertMessageEquals(expected, packet);
    }

    private void assertMessageEquals(final byte[] expected, final byte[] actual) {
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);

        Assert.assertTrue("message too short", expected.length >= 8 && actual.length >= 8);
        Assert.assertEquals("sof differs", expected[0], actual[0]);

        final int expectedControl = readLeShort(expected, 1);
        final int actualControl = readLeShort(actual, 1);
        Assert.assertEquals("control differs", expectedControl, actualControl);

        final int expectedCommand = readLeShort(expected, 3);
        final int actualCommand = readLeShort(actual, 3);
        Assert.assertEquals("command differs", expectedCommand, actualCommand);

        final int expectedPayloadLength = readLeShort(expected, 5);
        final int actualPayloadLength = readLeShort(actual, 5);
        Assert.assertEquals("payload length field differs", expectedPayloadLength, actualPayloadLength);

        final boolean expectedCrc = (expectedControl & 0x20) != 0;
        final boolean actualCrc = (actualControl & 0x20) != 0;
        Assert.assertEquals("crc flag differs", expectedCrc, actualCrc);

        final int expectedTotalLength = 8 + expectedPayloadLength + (expectedCrc ? 2 : 0);
        final int actualTotalLength = 8 + actualPayloadLength + (actualCrc ? 2 : 0);
        Assert.assertEquals("expected message has invalid size", expectedTotalLength, expected.length);
        Assert.assertEquals("actual message has invalid size", actualTotalLength, actual.length);

        // Counter byte at offset 7 is intentionally ignored.
        Assert.assertArrayEquals(
                "payload differs",
                Arrays.copyOfRange(expected, 8, 8 + expectedPayloadLength),
                Arrays.copyOfRange(actual, 8, 8 + actualPayloadLength)
        );

        assertCrcIsValid(expected, expectedCrc);
        assertCrcIsValid(actual, actualCrc);
    }

    private static int readLeShort(final byte[] msg, final int offset) {
        return (msg[offset] & 0xff) | ((msg[offset + 1] & 0xff) << 8);
    }

    private static void assertCrcIsValid(final byte[] msg, final boolean hasCrc) {
        if (!hasCrc) {
            return;
        }

        final int payloadLength = readLeShort(msg, 5);
        final int crcOffset = 8 + payloadLength;
        final int expectedCrc = readLeShort(msg, crcOffset);
        final int actualCrc = getCRC16ansi(Arrays.copyOf(msg, crcOffset));
        Assert.assertEquals("invalid crc", expectedCrc, actualCrc);
    }

    @SuppressWarnings("unchecked")
    private static <T extends GBDeviceEvent> T findEvent(final GBDeviceEvent[] events, final Class<T> cls) {
        for (GBDeviceEvent event : events) {
            if (cls.isInstance(event)) {
                return (T) event;
            }
        }

        return null;
    }
}
