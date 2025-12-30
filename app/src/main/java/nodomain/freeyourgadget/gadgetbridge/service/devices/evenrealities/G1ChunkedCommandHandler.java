package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Certain payloads are too large for one packet, so this class is a simple extension of the
 * G1CommandHandler that allows the subclass to send multiple packets.
 * This works by forcing the caller to pass in a callback to the "send" function they are using
 * and the ChunkG1CommandHandler will intercept calls to the Command callback to send the next
 * chunk in the packet. Once the response for the last chunk is sent, the passed in callback
 * will be sent.
 */
public abstract class G1ChunkedCommandHandler extends G1CommandHandler {
    private final Consumer<G1CommandHandler> sendCallback;
    private byte currentChunk;
    protected final byte[] payload;
    protected final byte chunkCount;
    private final byte[] sequenceIds;

    protected static byte getChunkCountForPayloadLength(int payloadLength, int headerLength) {
        int maxChunkSize = G1Constants.MAX_PACKET_SIZE_BYTES - headerLength;
        return (byte)((payloadLength / maxChunkSize) + 1);
    }

    protected G1ChunkedCommandHandler(byte[] sequenceIds, Consumer<G1CommandHandler> sendCallback,
                                    Function<byte[], Boolean> callback, byte[] payload) {
        super((byte)0, true, callback);
        this.sendCallback = sendCallback;
        this.currentChunk = 0;
        this.payload = payload;
        this.chunkCount = getChunkCountForPayloadLength(this.payload.length, getHeaderSize());
        this.sequenceIds = sequenceIds;
    }

    @Override
    final public Function<byte[], Boolean> getCallback() {
        if (currentChunk < chunkCount) {
            // Return the callback which sends the next chunk.
            return this::sendNextChunk;
        } else {
            // Now that all the chunks have been received, return the user callback.
            return super.getCallback();
        }
    }

    @Override
    final public byte[] serialize() {
        // Calculate the size, begin and end of the chunk.
        int maxPayloadSize = G1Constants.MAX_PACKET_SIZE_BYTES - getHeaderSize();
        int chunkBegin = this.currentChunk * maxPayloadSize;
        int chunkEnd = Math.min(this.payload.length,
                                (this.currentChunk + 1) * maxPayloadSize);
        int payloadSize = chunkEnd - chunkBegin;

        // Create the packet with space for the header.
        byte[] packet = new byte[getHeaderSize() + payloadSize];

        // Get the next sequence in the list if sequence ids are provided.
        byte currentSequenceId = 0x00;
        if (sequenceIds != null) {
            currentSequenceId = sequenceIds[this.currentChunk];
        }

        // Let the subclass write the header.
        writeHeader(this.currentChunk, currentSequenceId, this.chunkCount, packet);

        // Copy the chunk of the payload into the packet.
        System.arraycopy(this.payload, chunkBegin, packet, getHeaderSize(), payloadSize);

        return packet;
    }

    @Override
    final public boolean responseMatches(byte[] payload) {
        byte currentSequenceId = 0x00;
        if (sequenceIds != null) {
            currentSequenceId = sequenceIds[this.currentChunk];
        }

        if (chunkMatches(currentChunk, currentSequenceId, payload)) {
            // Advance the chunk when the response is received.
            currentChunk++;
            return true;
        }
        return false;
    }

    @Override
    public final String getName() {
        return getPacketName() + "_" + currentChunk;
    }

    private boolean sendNextChunk(byte[] payload) {
        sendCallback.accept(this);
        return true;
    }

    protected abstract boolean chunkMatches(byte currentChunk, byte currentSequence, byte[] payload);
    protected abstract void writeHeader(byte currentChunk, byte currentSequence, byte chunkCount, byte[] chunk);
    protected abstract int getHeaderSize();
    protected abstract String getPacketName();
}

