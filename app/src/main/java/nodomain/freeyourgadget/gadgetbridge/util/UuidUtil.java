package nodomain.freeyourgadget.gadgetbridge.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidUtil {
    public static UUID fromBytes(final byte[] bytes) {
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        final long high = buf.getLong();
        final long low = buf.getLong();
        return new UUID(high, low);
    }

    public static byte[] toBytes(final UUID uuid) {
        final ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }
}
