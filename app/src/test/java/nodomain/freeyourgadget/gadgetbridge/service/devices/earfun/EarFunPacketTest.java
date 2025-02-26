package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class EarFunPacketTest {
    @Test
    public void testEncode() {
        EarFunPacket.Command command = EarFunPacket.Command.SET_AMBIENT_SOUND;
        byte[] payload = {0x01, 0x02, 0x03};
        EarFunPacket packet = new EarFunPacket(command, payload);
        byte[] encoded = packet.encode();
        assertEquals((byte) 0xff, encoded[0]);
        assertEquals(EarFunPacket.DEFAULT_VERSION, encoded[1]);
        assertEquals(0x00, encoded[2]);
        assertEquals(payload.length, encoded[3]);
        assertEquals(EarFunPacket.DEFAULT_VENDOR_ID, ByteBuffer.wrap(encoded, 4, 2).getShort());
        assertEquals(command.commandId, ByteBuffer.wrap(encoded, 6, 2).getShort());
        byte[] decodedPayload = Arrays.copyOfRange(encoded, 8, encoded.length);
        assertArrayEquals(payload, decodedPayload);
    }

    @Test
    public void testDecode() {
        byte[] payload = {0x01, 0x02, 0x03};
        ByteBuffer buffer = ByteBuffer.allocate(11)
                .put((byte) 0xff)
                .put(EarFunPacket.DEFAULT_VERSION)
                .put((byte) 0x00)
                .put((byte) payload.length)
                .putShort(EarFunPacket.DEFAULT_VENDOR_ID)
                .putShort(EarFunPacket.Command.REQUEST_CONFIGURATION.commandId)
                .put(payload);
        buffer.flip();

        EarFunPacket packet = EarFunPacket.decode(buffer);

        assertNotNull(packet);
        assertEquals(EarFunPacket.Command.REQUEST_CONFIGURATION, packet.getCommand());
        assertArrayEquals(payload, packet.getPayload());
    }

    @Test
    public void testDecodeInvalidPacket() {
        ByteBuffer buffer = ByteBuffer.allocate(2)
                .put((byte) 0xff)
                .put(EarFunPacket.DEFAULT_VERSION);
        buffer.flip();

        EarFunPacket packet = EarFunPacket.decode(buffer);
        assertNull(packet);
    }

    @Test
    public void testGetCommandById() {
        EarFunPacket.Command command = EarFunPacket.Command.getCommandById((short) 0x0001);

        assertNotNull(command);
        assertEquals(EarFunPacket.Command.REQUEST_CONFIGURATION, command);
    }

    @Test
    public void testInvalidCommandId() {
        EarFunPacket.Command command = EarFunPacket.Command.getCommandById((short) 0x1234);
        assertNull(command);
    }
}