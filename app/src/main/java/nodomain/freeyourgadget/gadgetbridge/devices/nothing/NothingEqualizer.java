package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

public enum NothingEqualizer {
    ROCK((byte) 0x01),
    ELECTRONIC((byte) 0x02),
    POP((byte) 0x03),
    ENHANCE_VOCALS((byte) 0x04),
    CLASSICAL((byte) 0x05),
    DIRAC((byte) 0x07),
    ;

    private final byte code;

    NothingEqualizer(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return this.code;
    }

    public static NothingEqualizer fromCode(final byte code) {
        for (NothingEqualizer value : NothingEqualizer.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }

        return null;
    }

    public static NothingEqualizer fromPreferenceValue(final String preferenceValue) {
        if (preferenceValue == null) {
            return null;
        }

        try {
            return NothingEqualizer.valueOf(preferenceValue);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}
