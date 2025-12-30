package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import java.util.function.Function;

public abstract class G1CommandHandler {
    protected final byte sequence;
    private final boolean expectResponse;
    private final Function<byte[], Boolean> callback;
    private byte[] responsePayload;
    private int retryCount;

    protected G1CommandHandler(byte sequence, boolean expectResponse, Function<byte[], Boolean> callback) {
        this.sequence = sequence;
        this.expectResponse = expectResponse;
        this.callback = callback;
        this.responsePayload = null;
        this.retryCount = 0;
    }

    protected G1CommandHandler(boolean expectResponse, Function<byte[], Boolean> callback) {
        /* sequence is not used */
        this((byte)0, expectResponse, callback);
    }

    public int getTimeout() {
        return G1Constants.DEFAULT_COMMAND_TIMEOUT_MS;
    }
    public int getMaxRetryCount() {
        return G1Constants.DEFAULT_RETRY_COUNT;
    }
    private synchronized boolean continueWaiting() {
        return !hasResponsePayload() && hasRetryRemaining();
    }

    public synchronized void notifyAttempt() {
        retryCount++;
        notify();
    }

    public synchronized void setResponsePayload(byte[] payload) {
        this.responsePayload = payload;
        notify();
    }

    public synchronized boolean hasRetryRemaining() {
        return retryCount < getMaxRetryCount();
    }
    public synchronized boolean hasResponsePayload() {
        return responsePayload != null;
    }

    public boolean waitForResponsePayload() {
        // Go to sleep until the either a response is gotten or there is a timeout.
        while (continueWaiting()) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignored) {}
            }
        }

        // If the reties were exhausted return false to indicate that there was no response from
        // the glasses.
        return hasRetryRemaining();
    }

    public byte[] getResponsePayload() {
        if (responsePayload == null) {
            throw new RuntimeException("Null payload. Did you call waitForPayload()?");
        }
        return responsePayload;
    }

    public Function<byte[], Boolean> getCallback() {
        return callback;
    }

    public boolean expectResponse() {
        return expectResponse;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public abstract byte[] serialize();
    public abstract boolean responseMatches(byte[] payload);
    public abstract String getName();
}

