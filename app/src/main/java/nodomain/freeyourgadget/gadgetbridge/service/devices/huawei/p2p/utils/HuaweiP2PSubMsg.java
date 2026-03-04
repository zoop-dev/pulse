package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HuaweiP2PSubMsg {
    private static final int HEADER_LENGTH = 36;

    static class Sequence {
        int counter;

        private Sequence() {
            this.counter = 0;
        }

        public int getNext() {
            synchronized (this) {
                this.counter = (this.counter + 1) % 1000000;
                return this.counter;
            }
        }
    }

    private static final Sequence counter = new Sequence();

    public static int getNext() {
        return counter.getNext();
    }

    private int type;
    private int version = 1;
    private int totalLength = 0;
    private int unknown = 0;
    private int messageId;
    private byte[] data = null;

    public HuaweiP2PSubMsg(int type, int messageId, byte[] data) {
        this.type = type;
        this.messageId = messageId;
        this.data = data;
        this.totalLength = HEADER_LENGTH + data.length;
    }

    public HuaweiP2PSubMsg(byte[] data) {
        if (data == null || data.length < HEADER_LENGTH) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.type = buffer.getInt();
        this.version = buffer.getInt();
        this.totalLength = buffer.getInt();
        this.unknown = buffer.getInt();
        this.messageId = buffer.getInt();
        buffer.position(HEADER_LENGTH); // skin unknown data or padding

        int dataLen = this.totalLength - HEADER_LENGTH;
        if(dataLen > 0) {
            this.data = new byte[dataLen];
            buffer.get(this.data);
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        totalLength = HEADER_LENGTH + data.length;
    }

    public byte[] getBytes() {
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(type);
        header.putInt(version);
        header.putInt(totalLength);
        header.putInt(0);
        header.putInt(messageId);
        header.flip();

        ByteBuffer packet = ByteBuffer.allocate(HEADER_LENGTH + data.length);
        packet.put(header.array());
        packet.put(data);
        packet.flip();
        return packet.array();
    }




}
