package nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages;

public class GreePackMessage extends AbstractGreeMessage {
    public static final String TYPE = "pack";

    public static final int KEY_DEFAULT = 1;
    public static final int KEY_BIND = 0;

    private final String pack;
    private final int i;
    private final int pIn;

    public GreePackMessage(final String pack, final int encryptionKey) {
        this.pack = pack;
        this.i = encryptionKey;
        this.pIn = 0;
    }

    public String getPack() {
        return pack;
    }

    public int getEncryptionKey() {
        return i;
    }
}
