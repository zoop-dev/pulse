package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigStringList {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigStringList.class);

    private final String value;
    private final int maxLength;
    private final List<String> possibleValues;

    public ConfigStringList(final String value,
                            final int maxLength,
                            final List<String> possibleValues) {
        this.value = value;
        this.maxLength = maxLength;
        this.possibleValues = possibleValues;
    }

    public String getValue() {
        return value;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public static ConfigStringList consume(final ByteBuffer buf, final boolean includesConstraints) {
        final String value = StringUtils.untilNullTerminator(buf);
        if (value == null) {
            LOG.error("Null terminator not found in buffer");
            return null;
        }

        if (!includesConstraints) {
            return new ConfigStringList(value, Integer.MAX_VALUE, Collections.emptyList());
        }

        final int maxLength = buf.get() & 0xff;
        final int numPossibleValues = buf.get() & 0xff;
        final List<String> possibleValues = new ArrayList<>();
        for (int i = 0; i < numPossibleValues; i++) {
            final String possibleValue = StringUtils.untilNullTerminator(buf);
            possibleValues.add(possibleValue);
        }

        return new ConfigStringList(value, maxLength, possibleValues);
    }

    @NonNull
    @Override
    public String toString() {
        if (possibleValues.isEmpty()) {
            return String.format("ConfigStringList{value=%s}", value);
        } else {
            return String.format("ConfigStringList{value=%s, possibleValues=%s}", value, possibleValues);
        }
    }
}
