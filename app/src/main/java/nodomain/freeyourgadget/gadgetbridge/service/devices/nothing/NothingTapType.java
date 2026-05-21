package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import androidx.annotation.Nullable;

public enum NothingTapType {
    TAP_2(0x02),
    TAP_3(0x03),
    TAP_1_HOLD(0x07),
    TAP_2_HOLD(0x09),
    ;

    private final int code;

    NothingTapType(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static NothingTapType fromCode(final int code) {
        for (NothingTapType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
