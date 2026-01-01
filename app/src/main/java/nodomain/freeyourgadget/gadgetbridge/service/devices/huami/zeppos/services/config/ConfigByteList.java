package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.config;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

@SuppressWarnings("ClassCanBeRecord")
public class ConfigByteList {
    private final byte[] values;
    private final byte[] possibleValues;

    public ConfigByteList(final byte[] values, final byte[] possibleValues) {
        this.values = values;
        this.possibleValues = possibleValues;
    }

    public byte[] getValues() {
        return values;
    }

    public byte[] getPossibleValues() {
        return possibleValues;
    }

    public static ConfigByteList consume(final ByteBuffer buf, final boolean includesConstraints) {
        final int numValues = buf.get() & 0xff;
        final byte[] values = new byte[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = buf.get();
        }

        if (includesConstraints) {
            final int numPossibleValues = buf.get() & 0xff;
            final byte[] possibleValues = new byte[numPossibleValues];

            for (int i = 0; i < numPossibleValues; i++) {
                possibleValues[i] = buf.get();
            }

            return new ConfigByteList(values, possibleValues);
        }

        return new ConfigByteList(values, null);
    }

    @NonNull
    @Override
    public String toString() {
        if (possibleValues != null) {
            return String.format("ConfigByteList{values=%s, possibleValues=%s}", GB.hexdump(values), GB.hexdump(possibleValues));
        } else {
            return String.format("ConfigByteList{values=%s}", GB.hexdump(values));
        }
    }
}
