package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Locale;

public class ConfigInt {
    private final int value;
    private final int min;
    private final int max;
    private final boolean minMaxKnown;

    public ConfigInt(final int value) {
        this.value = value;
        this.min = this.max = 0;
        minMaxKnown = false;
    }

    public ConfigInt(final int value, final int min, final int max) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.minMaxKnown = true;
    }

    public int getValue() {
        return value;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isMinMaxKnown() {
        return minMaxKnown;
    }

    public static ConfigInt consume(final ByteBuffer buf, final boolean includesConstraints) {
        final int value = buf.getInt();

        if (!includesConstraints) {
            return new ConfigInt(value);
        }

        final int min = buf.getInt();
        final int max = buf.getInt();

        return new ConfigInt(value, min, max);
    }

    @NonNull
    @Override
    public String toString() {
        if (isMinMaxKnown()) {
            return String.format(Locale.ROOT, "ConfigInt{value=%d, min=%d, max=%d}", value, min, max);
        } else {
            return String.format(Locale.ROOT, "ConfigInt{value=%d}", value);
        }
    }
}
