package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

public class GreeWlanMessage extends AbstractGreeMessage {
    public static final String TYPE = "wlan";

    private final String host;
    private final String psw;
    private final String ssid;
    private final int num;

    public GreeWlanMessage(final String host, final String psw, final String ssid, final int num) {
        this.host = host;
        this.psw = psw;
        this.ssid = ssid;
        this.num = num;
    }
}
