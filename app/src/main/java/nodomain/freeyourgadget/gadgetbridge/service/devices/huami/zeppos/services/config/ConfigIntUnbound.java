package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Locale;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigIntUnbound {
    private final int value;

    public ConfigIntUnbound(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ConfigIntUnbound consume(final ByteBuffer buf, final boolean includesConstraints) {
        final int value = buf.getInt();

        // Looks to have no constraints at all

        return new ConfigIntUnbound(value);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "ConfigIntUnbound{value=%s}", String.format("0x%08x", value));
    }
}
