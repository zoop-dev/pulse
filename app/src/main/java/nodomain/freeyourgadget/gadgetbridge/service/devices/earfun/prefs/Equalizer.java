package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.EarFunSettingsPreferenceConst.*;

import java.util.Arrays;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;

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

        public byte getBandId() {
            return bandId;
        }

        public short getFrequency() {
            return frequency;
        }

        public short getqFactor() {
            return qFactor;
        }

        public double getDefaultGain() {
            return defaultGain;
        }
    }

    public static class BandConfig {
        public BandConfig(Band band, String key) {
            this.band = band;
            this.key = key;
        }

        public Band getBand() {
            return band;
        }

        public String getKey() {
            return key;
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

    public interface EqualizerPreset {
        String getPresetName();

        int getLocalizedPresetName();

        double[] getSettings();

        default String getFormattedSettings() {
            return Arrays.toString(getSettings());
        }

        static void printAllPresets(EqualizerPreset[] presets) {
            for (EqualizerPreset preset : presets) {
                System.out.println(preset.getPresetName() + ": " + preset.getFormattedSettings());
            }
        }
    }

    public enum SixBandPreset implements EqualizerPreset {
        // Default: Keeps all bands at their default values
        DEFAULT(R.string.pref_title_equalizer_normal, new double[]{0, 0, 0, 0, 0, 0}),
        // Natural: Balanced and natural sound profile that reproduces audio without any coloration
        NATURAL(R.string.pref_title_equalizer_natural, new double[]{0, 0, 1, 1, 2, 3}),
        // Bass Boost: Emphasizes the low frequencies for a deep, powerful bass
        BASS_BOOST(R.string.pref_title_equalizer_bass_boost, new double[]{8, 3, 2, 0, 0, 0}),
        // Treble Boost: Enhances the high frequencies for a crisp and bright sound
        TREBLE_BOOST(R.string.pref_title_equalizer_trebble, new double[]{0, 0, 0, 2, 3, 5}),
        // Soft: Creates a gentle, smooth, and mellow sound
        SOFT(R.string.pref_title_equalizer_soft, new double[]{-5, -2, +2, +3, 0, -3}),
        // Dynamic: Produces a lively and energetic sound with well-defined bass and crisp highs
        DYNAMIC(R.string.pref_title_equalizer_dynamic, new double[]{+7, +3, +2, +3, +5, +7}),
        // Clear: Achieves a balanced and transparent sound, ideal for detailed audio work
        CLEAR(R.string.pref_title_equalizer_clear, new double[]{+2, 0, +3, +5, +3, +5}),
        // Relaxed: Produces a calming and soothing sound, perfect for unwinding
        RELAXED(R.string.sony_equalizer_preset_relaxed, new double[]{+2, +1, 0, -1, -3, -5}),
        // Vocal: Enhances the mid-range frequencies for clear and prominent vocals
        VOCAL(R.string.sony_equalizer_preset_vocal, new double[]{-2, 0, +4, +5, +2, -1});

        public final String presetName;
        public final int localizedPresetName;
        public final double[] settings;

        SixBandPreset(String name, double[] settings) {
            this.presetName = name;
            this.localizedPresetName = -1;
            this.settings = settings;
        }

        SixBandPreset(int localizedName, double[] settings) {
            this.presetName = "";
            this.localizedPresetName = localizedName;
            this.settings = settings;
        }

        @Override
        public String getPresetName() {
            return presetName;
        }

        @Override
        public int getLocalizedPresetName() {
            return localizedPresetName;
        }

        @Override
        public double[] getSettings() {
            return settings;
        }
    }

    public enum TenBandPreset implements EqualizerPreset {
        // Default: Keeps all bands at their default values
        DEFAULT(R.string.pref_title_equalizer_normal, new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
        // Natural: Balanced and natural sound profile that reproduces audio without any coloration
        NATURAL(R.string.pref_title_equalizer_natural, new double[]{0, 0, 1, 2, 2, -1, -1, -1, -2, 1}),
        // Bass Boost: Emphasizes the low frequencies for a deep, powerful bass
        BASS_BOOST(R.string.pref_title_equalizer_bass_boost, new double[]{+8, +6, +4, +2, 0, 0, 0, 0, 0, 0}),
        // Treble Boost: Enhances the high frequencies for a crisp and bright sound
        TREBLE_BOOST(R.string.pref_title_equalizer_trebble, new double[]{0, 0, 0, 0, 0, 0, +2, +2, +3, +4}),
        // Soft: Creates a gentle, smooth, and mellow sound
        SOFT(R.string.pref_title_equalizer_soft, new double[]{-5, -4, -2, 0, +2, +3, 0, -2, -3, -5}),
        // Dynamic: Produces a lively and energetic sound with well-defined bass and crisp highs
        DYNAMIC(R.string.pref_title_equalizer_dynamic, new double[]{+6, +6, +4, +2, +2, +3, +4, +5, +6, +7}),
        // Clear: Achieves a balanced and transparent sound, ideal for detailed audio work
        CLEAR(R.string.pref_title_equalizer_clear, new double[]{+3, +2, +2, +2, +3, +5, +3, +3, +4, +5}),
        // Relaxed: Produces a calming and soothing sound, perfect for unwinding
        RELAXED(R.string.sony_equalizer_preset_relaxed, new double[]{+2, +1, 0, 0, 0, -1, -2, -3, -4, -5}),
        // Vocal: Enhances the mid-range frequencies for clear and prominent vocals
        VOCAL(R.string.sony_equalizer_preset_vocal, new double[]{-3, -2, 0, +2, +3, +5, +3, +2, 0, -1});

        public final String presetName;
        public final int localizedPresetName;
        public final double[] settings;


        TenBandPreset(String name, double[] settings) {
            this.presetName = name;
            this.localizedPresetName = -1;
            this.settings = settings;
        }

        TenBandPreset(int localizedName, double[] settings) {
            this.presetName = "";
            this.localizedPresetName = localizedName;
            this.settings = settings;
        }

        @Override
        public String getPresetName() {
            return presetName;
        }

        @Override
        public int getLocalizedPresetName() {
            return localizedPresetName;
        }

        @Override
        public double[] getSettings() {
            return settings;
        }
    }

    public static boolean containsKey(Equalizer.BandConfig[] array, String key) {
        return Arrays.stream(array)
                .anyMatch(element -> Objects.equals(element.key, key));
    }

    public static EqualizerPreset[] SixBandEqualizerPresets = {
            SixBandPreset.DEFAULT,
            SixBandPreset.NATURAL,
            SixBandPreset.BASS_BOOST,
            SixBandPreset.TREBLE_BOOST,
            SixBandPreset.SOFT,
            SixBandPreset.DYNAMIC,
            SixBandPreset.CLEAR,
            SixBandPreset.RELAXED,
            SixBandPreset.VOCAL
    };

    public static EqualizerPreset[] TenBandEqualizerPresets = {
            TenBandPreset.DEFAULT,
            TenBandPreset.NATURAL,
            TenBandPreset.BASS_BOOST,
            TenBandPreset.TREBLE_BOOST,
            TenBandPreset.SOFT,
            TenBandPreset.DYNAMIC,
            TenBandPreset.CLEAR,
            TenBandPreset.RELAXED,
            TenBandPreset.VOCAL
    };
}
