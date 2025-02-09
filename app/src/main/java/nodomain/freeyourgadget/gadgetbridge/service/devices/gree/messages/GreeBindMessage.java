package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

public class GreeBindMessage extends AbstractGreeMessage {
    public static final String TYPE = "bind";

    private final String mac;
    private final int IsCP;

    public GreeBindMessage(final String mac) {
        this.mac = mac;
        this.IsCP = 1;
    }
}
