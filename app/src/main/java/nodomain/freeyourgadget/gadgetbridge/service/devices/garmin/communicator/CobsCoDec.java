package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class CobsCoDec {
    private static final Logger LOG = LoggerFactory.getLogger(CobsCoDec.class);

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(10_000);
    private byte[] cobsDecodedMessage;

    /**
     * Accumulates received bytes in a local buffer and attempts to parse it.
     * A malformed or truncated frame (e.g. a lost BLE notification) must never
     * leave the decoder in a broken state: on any error the accumulated bytes
     * are discarded and decoding resumes with the next complete frame (frames
     * are self-delimiting via a leading and a trailing 0x00 byte). Note that a
     * decoded message that has not yet been retrieved is discarded as well.
     */
    public void receivedBytes(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        try {
            byteBuffer.put(bytes);
            decode();
        } catch (final BufferOverflowException e) {
            LOG.warn("COBS buffer full, resetting decoder");
            reset();
        }
    }

    public byte[] retrieveMessage() {
        final byte[] resultPacket = cobsDecodedMessage;
        cobsDecodedMessage = null;
        return resultPacket;
    }

    /**
     * COBS decoding algorithm variant, which relies on a leading and a trailing 0 byte (the former
     * is not part of default implementations).
     * This function removes the complete message from the internal buffer, if it could be decoded.
     */
    private void decode() {
        if (cobsDecodedMessage != null) {
            // packet is waiting, unable to parse more
            return;
        }
        if (byteBuffer.position() < 4) {
            // minimal payload length including the padding
            return;
        }
        if (0 != byteBuffer.get(byteBuffer.position() - 1))
            return; //no 0x00 at the end, hence no full packet
        byteBuffer.position(byteBuffer.position() - 1); //don't process the trailing 0
        byteBuffer.flip();
        if (0 != byteBuffer.get()) {
            // No 0x00 at the start - the accumulated bytes are not a valid frame
            // (the frame start was lost, e.g. a BLE notification was dropped).
            // Discard them instead of leaving the buffer in a broken state,
            // which would wedge the decoder until the app is restarted.
            LOG.warn("Received COBS data without leading 0x00, discarding {} bytes", byteBuffer.limit() + 1);
            reset();
            return;
        }
        ByteBuffer decodedBytesBuffer = ByteBuffer.allocate(byteBuffer.limit()); //leading and trailing 0x00 bytes
        try {
            while (byteBuffer.hasRemaining()) {
                byte code = byteBuffer.get();
                if (code == 0) {
                    break;
                }
                int codeValue = code & 0xFF;
                int payloadSize = codeValue - 1;
                for (int i = 0; i < payloadSize; i++) {
                    decodedBytesBuffer.put(byteBuffer.get());
                }
                if (codeValue != 0xFF && byteBuffer.hasRemaining()) {
                    decodedBytesBuffer.put((byte) 0); // Append a zero byte after the payload
                }
            }
        } catch (final BufferUnderflowException e) {
            // The frame is truncated - a code byte claimed more payload bytes
            // than were actually received (e.g. a BLE notification was lost).
            // Discard the accumulated bytes; the next complete frame will
            // decode normally.
            LOG.warn("Received truncated COBS frame, discarding it");
            reset();
            return;
        }

        decodedBytesBuffer.flip();
        cobsDecodedMessage = new byte[decodedBytesBuffer.remaining()];
        decodedBytesBuffer.get(cobsDecodedMessage);
        byteBuffer.compact();
    }

    private void reset() {
        cobsDecodedMessage = null;
        byteBuffer.clear();
    }

    // this implementation of COBS relies on a leading and a trailing 0 byte (the former is not part of default implementations)
    public static byte[] encode(byte[] data) {
        ByteBuffer encodedBytesBuffer = ByteBuffer.allocate((data.length * 2) + 2); // Maximum expansion

        encodedBytesBuffer.put((byte) 0);// Garmin initial padding
        ByteBuffer buffer = ByteBuffer.wrap(data);

        boolean lastByteWasZero = false;

        while (buffer.position() < buffer.limit()) {
            int startPos = buffer.position();
            int zeroIndex = buffer.position();

            while (buffer.hasRemaining() && buffer.get() != 0) {
                zeroIndex++;
            }

            lastByteWasZero = buffer.position() > zeroIndex;

            int payloadSize = zeroIndex - startPos;

            while (payloadSize >= 0xFE) {
                encodedBytesBuffer.put((byte) 0xFF); // Maximum payload size indicator
                encodedBytesBuffer.put(data, startPos, 0xFE);
                payloadSize -= 0xFE;
                startPos += 0xFE;
            }

            encodedBytesBuffer.put((byte) (payloadSize + 1));
            encodedBytesBuffer.put(data, startPos, payloadSize);
        }

        if (lastByteWasZero) {
            encodedBytesBuffer.put((byte) 0x01);
        }

        encodedBytesBuffer.put((byte) 0); // Append a zero byte to indicate end of encoding
        encodedBytesBuffer.flip();

        byte[] encodedBytes = new byte[encodedBytesBuffer.remaining()];
        encodedBytesBuffer.get(encodedBytes);

        return encodedBytes;
    }

}
