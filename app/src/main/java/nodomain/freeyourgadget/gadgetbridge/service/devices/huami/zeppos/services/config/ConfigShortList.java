package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigShortList {
    private final short[] values;

    private final int minCount;
    private final int maxCount;
    private final short minValue;
    private final short maxValue;

    public ConfigShortList(final short[] values,
                           final int minCount,
                           final int maxCount,
                           final short minValue,
                           final short maxValue) {
        this.values = values;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public static ConfigShortList consume(final ByteBuffer buf, final boolean includesConstraints) {
        final int numValues = buf.get() & 0xff;
        final short[] values = new short[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = buf.getShort();
        }

        if (!includesConstraints) {
            return new ConfigShortList(
                    values,
                    0,
                    Integer.MAX_VALUE,
                    Short.MIN_VALUE,
                    Short.MAX_VALUE
            );
        }

        // only seen in WORKOUT 0x05 in what looks like HR zones: [80,100,120,140,160,180]
        final int minCount = buf.get() & 0xff; // 6
        final int maxCount = buf.get() & 0xff; // 6
        final short minValue = buf.getShort(); // 30
        final short maxValue = buf.getShort(); // 220

        return new ConfigShortList(
                values,
                minCount,
                maxCount,
                minValue,
                maxValue
        );
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigShortList{values=[");

        for (short value : values) {
            sb.append(value).append(",");
        }
        if (values.length > 0) {
            sb.setLength(sb.length() - 1); // remove last ,
        }
        sb.append("]");

        if (maxCount != Integer.MAX_VALUE) {
            sb.append(", minCount=").append(minCount);
            sb.append(", maxCount=").append(maxCount);
            sb.append(", minValue=").append(minValue);
            sb.append(", maxValue=").append(maxValue);
        }
        sb.append("}");

        return sb.toString();
    }
}
