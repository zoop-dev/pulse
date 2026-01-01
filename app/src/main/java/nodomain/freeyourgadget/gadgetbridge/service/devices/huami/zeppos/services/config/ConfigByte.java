package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigByte {
    private final byte value;
    private final byte[] possibleValues;

    public ConfigByte(final byte value, final byte[] possibleValues) {
        this.value = value;
        this.possibleValues = possibleValues;
    }

    public byte getValue() {
        return value;
    }

    public byte[] getPossibleValues() {
        return possibleValues;
    }

    public static ConfigByte consume(final ByteBuffer buf, final boolean includesConstraints) {
        final byte value = buf.get();

        if (includesConstraints) {
            final int numPossibleValues = buf.get() & 0xff;
            final byte[] possibleValues = new byte[numPossibleValues];

            for (int i = 0; i < numPossibleValues; i++) {
                possibleValues[i] = buf.get();
            }

            return new ConfigByte(value, possibleValues);
        }

        return new ConfigByte(value, new byte[0]);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ConfigByte{value=0x%02x, possibleValues=%s}", value, GB.hexdump(possibleValues));
    }
}
