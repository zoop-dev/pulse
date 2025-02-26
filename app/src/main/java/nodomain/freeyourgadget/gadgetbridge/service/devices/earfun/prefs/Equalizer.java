package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

public class Equalizer {
    public enum Band {
        SIX_BAND_63((byte) 0xA1, (short) 0x00BD, (short) 0x0B33),
        SIX_BAND_180((byte) 0xA2, (short) 0x021C, (short) 0x0999),
        SIX_BAND_500((byte) 0xA3, (short) 0x05DC, (short) 0x0999),
        SIX_BAND_1000((byte) 0xA4, (short) 0x0BB8, (short) 0x0999),
        SIX_BAND_8000((byte) 0xA5, (short) 0x5DC0, (short) 0x0999),
        SIX_BAND_15000((byte) 0xA6, (short) 0xAFC8, (short) 0x0999),
        // channels 7 - 10 are never used by the EarFun Air S, but they need to be set with dummy values
        SIX_BAND_DUMMY_SEVEN((byte) 0xA7, (short) 0x0063, (short) 0x0800, 4.5),
        SIX_BAND_DUMMY_EIGHT((byte) 0xA8, (short) 0x0258, (short) 0x0999, -4.5),
        SIX_BAND_DUMMY_NINE((byte) 0xA9, (short) 0x1770, (short) 0x0E66, 1.5),
        SIX_BAND_DUMMY_TEN((byte) 0xAA, (short) 0xAFC8, (short) 0x0999, 3.5),

        TEN_BAND_31_5((byte) 0xA1, (short) 0x005E, (short) 0x0B33),
        TEN_BAND_63((byte) 0xA2, (short) 0x00BD, (short) 0x0B33),
        TEN_BAND_125((byte) 0xA3, (short) 0x0177, (short) 0x0B33),
        TEN_BAND_250((byte) 0xA4, (short) 0x02EE, (short) 0x0B33),
        TEN_BAND_500((byte) 0xA5, (short) 0x05DC, (short) 0x0B33),
        TEN_BAND_1000((byte) 0xA6, (short) 0x0BB8, (short) 0x0B33),
        TEN_BAND_2000((byte) 0xA7, (short) 0x1770, (short) 0x0B33),
        TEN_BAND_4000((byte) 0xA8, (short) 0x2EE0, (short) 0x0B33),
        TEN_BAND_8000((byte) 0xA9, (short) 0x5DC0, (short) 0x0B33),
        TEN_BAND_16000((byte) 0xAA, (short) 0xBB80, (short) 0x0B33);

        public final byte bandId;
        public final short frequency;
        public final short qFactor;
        public final double defaultGain;

        Band(byte bandId, short frequency, short qFactor) {
            this(bandId, frequency, qFactor, 0);
        }

        Band(byte bandId, short frequency, short qFactor, double defaultGain) {
            this.bandId = bandId;
            this.frequency = frequency;
            this.qFactor = qFactor;
            this.defaultGain = defaultGain;
        }
    }

    public static class BandConfig {
        public BandConfig(Band band, String key) {
            this.band = band;
            this.key = key;
        }

        public Band band;
        public String key;
    }

    public static BandConfig[] SixBandEqualizer = {
            new BandConfig(Band.SIX_BAND_63, PREF_EARFUN_EQUALIZER_BAND_63),
            new BandConfig(Band.SIX_BAND_180, PREF_EARFUN_EQUALIZER_BAND_180),
            new BandConfig(Band.SIX_BAND_500, PREF_EARFUN_EQUALIZER_BAND_500),
            new BandConfig(Band.SIX_BAND_1000, PREF_EARFUN_EQUALIZER_BAND_1000),
            new BandConfig(Band.SIX_BAND_8000, PREF_EARFUN_EQUALIZER_BAND_8000),
            new BandConfig(Band.SIX_BAND_15000, PREF_EARFUN_EQUALIZER_BAND_15000),
            new BandConfig(Band.SIX_BAND_DUMMY_SEVEN, null),
            new BandConfig(Band.SIX_BAND_DUMMY_EIGHT, null),
            new BandConfig(Band.SIX_BAND_DUMMY_NINE, null),
            new BandConfig(Band.SIX_BAND_DUMMY_TEN, null),
    };

    public static BandConfig[] TenBandEqualizer = {
            new BandConfig(Band.TEN_BAND_31_5, PREF_EARFUN_EQUALIZER_BAND_31_5),
            new BandConfig(Band.TEN_BAND_63, PREF_EARFUN_EQUALIZER_BAND_63),
            new BandConfig(Band.TEN_BAND_125, PREF_EARFUN_EQUALIZER_BAND_125),
            new BandConfig(Band.TEN_BAND_250, PREF_EARFUN_EQUALIZER_BAND_250),
            new BandConfig(Band.TEN_BAND_500, PREF_EARFUN_EQUALIZER_BAND_500),
            new BandConfig(Band.TEN_BAND_1000, PREF_EARFUN_EQUALIZER_BAND_1000),
            new BandConfig(Band.TEN_BAND_2000, PREF_EARFUN_EQUALIZER_BAND_2000),
            new BandConfig(Band.TEN_BAND_4000, PREF_EARFUN_EQUALIZER_BAND_4000),
            new BandConfig(Band.TEN_BAND_8000, PREF_EARFUN_EQUALIZER_BAND_8000),
            new BandConfig(Band.TEN_BAND_16000, PREF_EARFUN_EQUALIZER_BAND_16000),
    };
}
