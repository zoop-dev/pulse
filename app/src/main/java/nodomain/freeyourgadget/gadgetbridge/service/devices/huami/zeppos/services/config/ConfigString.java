package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigString {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigString.class);

    private final String value;
    private final int maxLength;

    public ConfigString(final String value, final int maxLength) {
        this.value = value;
        this.maxLength = maxLength;
    }

    public String getValue() {
        return value;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public static ConfigString consume(final ByteBuffer buf, final boolean includesConstraints) {
        final String value = StringUtils.untilNullTerminator(buf);
        if (value == null) {
            LOG.error("Null terminator not found in buffer");
            return null;
        }

        if (!includesConstraints) {
            return new ConfigString(value, -1);
        }

        final int maxLength = buf.get() & 0xff;

        return new ConfigString(value, maxLength);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ConfigString{value=%s}", value);
    }
}
