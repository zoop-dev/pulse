package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import androidx.annotation.Nullable;

public enum NothingTapAction {
    OFF(0x01),
    PREVIOUS_TRACK(0x08),
    NEXT_TRACK(0x09),
    VOICE_ASSISTANT(0x0b),
    VOLUME_UP(0x12),
    VOLUME_DOWN(0x13),
    ANC_MODE__ANC_TRANSPARENCY_OFF(0x0a),
    ANC_MODE__ANC_TRANSPARENCY(0x16),
    ANC_MODE__ANC_OFF(0x14),
    ANC_MODE__TRANSPARENCY_OFF(0x15),
    ;

    private final int code;

    NothingTapAction(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static NothingTapAction fromCode(final int code) {
        for (NothingTapAction type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
