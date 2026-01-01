package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigBoolean {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigBoolean.class);

    private final boolean value;

    public ConfigBoolean(final boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public static ConfigBoolean consume(final ByteBuffer buf) {
        final byte b = buf.get();
        if (b != 0 && b != 1) {
            LOG.error("Unexpected byte value for boolean {}", b);
            return null;
        }

        return new ConfigBoolean(b == 1);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ConfigBoolean{value=%s}", value);
    }
}
