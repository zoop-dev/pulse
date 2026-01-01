package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Locale;

public class ConfigShort {
    private final short value;
    private final short min;
    private final short max;
    private final boolean minMaxKnown;

    public ConfigShort(final short value) {
        this.value = value;
        this.min = this.max = 0;
        minMaxKnown = false;
    }

    public ConfigShort(final short value, final short min, final short max) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.minMaxKnown = true;
    }

    public short getValue() {
        return value;
    }

    public short getMin() {
        return min;
    }

    public short getMax() {
        return max;
    }

    public boolean isMinMaxKnown() {
        return minMaxKnown;
    }

    public static ConfigShort consume(final ByteBuffer buf, final boolean includesConstraints) {
        final short value = buf.getShort();

        if (!includesConstraints) {
            return new ConfigShort(value);
        }

        final short min = buf.getShort();
        final short max = buf.getShort();

        return new ConfigShort(value, min, max);
    }

    @NonNull
    @Override
    public String toString() {
        if (isMinMaxKnown()) {
            return String.format(Locale.ROOT, "ConfigShort{value=%d, min=%d, max=%d}", value, min, max);
        } else {
            return String.format(Locale.ROOT, "ConfigShort{value=%d}", value);
        }
    }
}
