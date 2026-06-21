package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeShort implements BaseTypeInterface {
    private final int min;
    private final int max;
    private final int invalid;
    private final boolean unsigned;
    private final int size = 2;

    BaseTypeShort(boolean unsigned, int invalid) {
        if (unsigned) {
            this.min = 0;
            this.max = 0xffff;
        } else {
            this.min = Short.MIN_VALUE;
            this.max = Short.MAX_VALUE;
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }

    @Override
    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, double scale, int offset) {
        int s = unsigned ? Short.toUnsignedInt(byteBuffer.getShort()) : byteBuffer.getShort();
        if (s < min || s > max)
            return null;
        if (s == invalid)
            return null;
        return (s / scale) - offset;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        // Use doubleValue() rather than intValue() so that fractional inputs (Float/Double
        // physical values like 2.5 m/s on a scale=1000 field) survive the multiply by
        // scale instead of being truncated to 2 first. For Integer/Long inputs the result
        // is identical.
        int i = (int) ((((Number) o).doubleValue() + offset) * scale);
        if (i < min || i > max) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putShort((short) i);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putShort((short) invalid);
    }
}
