package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

public class GreeBleInfoMessage extends AbstractGreeMessage {
    public static final String TYPE = "bleinfo";

    private final int wificon;
    private final String mac;
    private final String mid;
    private final String ver;

    public GreeBleInfoMessage(final int wificon, final String mac, final String mid, final String ver) {
        this.wificon = wificon;
        this.mac = mac;
        this.mid = mid;
        this.ver = ver;
    }

    public int getWificon() {
        return wificon;
    }

    public String getMac() {
        return mac;
    }

    public String getMid() {
        return mid;
    }

    public String getVer() {
        return ver;
    }
}
