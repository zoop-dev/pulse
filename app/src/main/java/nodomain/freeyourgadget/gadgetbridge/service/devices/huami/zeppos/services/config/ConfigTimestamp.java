package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigTimestamp {
    private final long value;

    public ConfigTimestamp(final long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public static ConfigTimestamp consume(final ByteBuffer buf) {
        final long value = buf.getLong();

        return new ConfigTimestamp(value);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "ConfigTimestamp{value=%s}", new Date(value));
    }
}
