package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

public class Interactions {
    public enum InteractionType {
        SINGLE((byte) 0x01),
        DOUBLE((byte) 0x02),
        TRIPLE((byte) 0x03),
        LONG((byte) 0x11);

        public final byte value;

        InteractionType(byte value) {
            this.value = value;
        }
    }

    public enum Position {
        LEFT,
        RIGHT
    }

    public static String[] interactionPrefs = {
            PREF_EARFUN_SINGLE_TAP_LEFT_ACTION,
            PREF_EARFUN_SINGLE_TAP_RIGHT_ACTION,
            PREF_EARFUN_DOUBLE_TAP_LEFT_ACTION,
            PREF_EARFUN_DOUBLE_TAP_RIGHT_ACTION,
            PREF_EARFUN_TRIPPLE_TAP_LEFT_ACTION,
            PREF_EARFUN_TRIPPLE_TAP_RIGHT_ACTION,
            PREF_EARFUN_LONG_TAP_LEFT_ACTION,
            PREF_EARFUN_LONG_TAP_RIGHT_ACTION
    };
}
