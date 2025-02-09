package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

public class GreeBleKeyMessage extends AbstractGreeMessage {
    public static final String TYPE = "blekey";

    private final String key;

    public GreeBleKeyMessage(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
