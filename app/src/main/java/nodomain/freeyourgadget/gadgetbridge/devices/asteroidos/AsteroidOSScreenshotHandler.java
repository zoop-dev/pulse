package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AsteroidOSScreenshotHandler {
    /**
     * Holds the state of the handler
     */
    public enum State {
        GetSize,
        GetContent,
        Finished,
        Error
    }
    private byte[] screenshotBytes;
    private int currentIndex;
    private State currentState;
    private String currentError;

    public AsteroidOSScreenshotHandler() {
        this.reset();
    }

    public void reset() {
        currentState = State.GetSize;
        currentIndex = 0;
        screenshotBytes = null;
        currentError = "";
    }

    /**
     * Gets the current state of the screenshot handler
     * @return the state
     */
    public State getCurrentState() { return currentState; }

    /**
     * Gets the current error message
     * @return the message
     * @apiNote This is not valid unless the current state is State.Error
     */
    public String getCurrentError() { return currentError; }

    /**
     * Gets the current screenshot content as an array of bytes
     * @return the screenshot as bytes
     * @apiNote Not valid unless the current state is State.Finished.
     */
    public byte[] getScreenshotContent() { return screenshotBytes; }

    /**
     * Converts a set of 4 bytes to an integer in spec with asteroid-btsyncd (little-endian).
     * @param bytes the raw bytes to convert to an int
     * @return an int representing the bytes given
     */
    private static int bytesToInt(byte[] bytes) {
        assert(bytes.length == 4);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Receives the bytes from the watch and sets the state accordingly
     * @param bytes The bytes from the server (asteroid-btsyncd)
     * @return The current state of the handler.
     */
    public State receiveScreenshotBytes(byte[] bytes) {
        switch (currentState) {
            case Finished:
            case Error:
                currentState = State.GetSize;
                return receiveScreenshotBytes(bytes);
            case GetSize:
                if (bytes.length == 4) {
                    long screenshotLength = bytesToInt(bytes);
                    screenshotBytes = new byte[(int) screenshotLength];
                    currentState = State.GetContent;
                    currentIndex = 0;
                    return currentState;
                } else {
                    currentState = State.Error;
                    currentError = "State was in GetSize, but the value given was not 4 bytes long";
                    return State.Error;
                }
            case GetContent:
                if (screenshotBytes == null) {
                    currentState = State.Error;
                    currentError = "State is in GetContent, but screenshotBytes is null.";
                    return State.Error;
                }
                System.arraycopy(bytes, 0, screenshotBytes, currentIndex, bytes.length);
                currentIndex += bytes.length;
                if (currentIndex >= screenshotBytes.length) {
                    currentState = State.Finished;
                    return State.Finished;
                }
        }
        return currentState;
    }
}
