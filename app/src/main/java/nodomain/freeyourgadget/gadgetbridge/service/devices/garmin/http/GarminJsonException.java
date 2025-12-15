package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

public class GarminJsonException extends Exception {
    public GarminJsonException(final String message) {
        super(message);
    }

    public GarminJsonException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
