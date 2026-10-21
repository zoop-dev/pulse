/*  Copyright (C) 2026 Jeff Laflamme

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util.language.impl;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

/**
 * Romanizes Khmer following the Unicode CLDR "Khmer-Latin/UNGEGN" transform (marked provisional):
 * https://github.com/unicode-org/cldr/blob/main/common/transforms/Khmer-Latin.xml
 * <p>
 * Every Khmer consonant belongs to an a-series or an o-series, which decides how the following
 * vowel is read. As in the CLDR rules, a consonant writes its sound and leaves a marker for its
 * inherent vowel in front of the remaining text. Subscripts and vowel signs consume the marker;
 * a marker that nothing consumes is written as the inherent vowel "â" or "ô". Only runs of Khmer
 * text go through these rules; everything else is copied unchanged.
 * <p>
 * Additions to the CLDR rules:
 * <ul>
 * <li>subscripts of the other series: a sonorant subscript (ង ញ ន ម យ រ ល វ) keeps the series of
 * the base consonant, any other subscript uses its own series</li>
 * <li>the signs ៉ and ៊ switch the series of the consonant before them</li>
 * <li>no inherent vowel on a final consonant: when the word already has a vowel and the consonant
 * ends the word or comes before a consonant that starts a new syllable</li>
 * <li>zero-width space becomes a space; zero-width (non-)joiner and ៎ are removed; ៕ becomes "."</li>
 * <li>digits, punctuation and zero-width space end a word, like an ordinary space</li>
 * <li>the output stays within Latin-1, which watch fonts usually cover: the breve is dropped
 * (ŏ becomes o), œ becomes "oe" and ’ becomes '</li>
 * </ul>
 */
public class KhmerTransliterator implements Transliterator {
    private static final char MARK_A = '';
    private static final char MARK_O = '';
    private static final char COENG = '្';

    // consonant, romanization, series
    private static final String[][] CONSONANTS = {
            {"ក", "k", "a"}, {"ខ", "kh", "a"}, {"គ", "k", "o"}, {"ឃ", "kh", "o"}, {"ង", "ng", "o"},
            {"ច", "ch", "a"}, {"ឆ", "chh", "a"}, {"ជ", "ch", "o"}, {"ឈ", "chh", "o"}, {"ញ", "nh", "o"},
            {"ដ", "d", "a"}, {"ឋ", "th", "a"}, {"ឌ", "d", "o"}, {"ឍ", "th", "o"}, {"ណ", "n", "a"},
            {"ត", "t", "a"}, {"ថ", "th", "a"}, {"ទ", "t", "o"}, {"ធ", "th", "o"}, {"ន", "n", "o"},
            {"ប", "b", "a"}, {"ផ", "ph", "a"}, {"ព", "p", "o"}, {"ភ", "ph", "o"}, {"ម", "m", "o"},
            {"យ", "y", "o"}, {"រ", "r", "o"}, {"ល", "l", "o"}, {"វ", "v", "o"}, {"ស", "s", "a"},
            {"ហ", "h", "a"}, {"ឡ", "l", "a"}, {"អ", "’", "a"},
    };
    private static final String SONORANTS = "ងញនមយរលវ";

    // vowel sign, romanization after an a-series consonant, after an o-series consonant, on its own
    private static final String[][] VOWEL_SIGNS = {
            {"ា", "a", "a", "a"}, {"ិ", "ĕ", "ĭ", "ĭ"}, {"ី", "ei", "i", "i"}, {"ឹ", "œ̆", "œ̆", "œ̆"},
            {"ឺ", "œ", "œ", "œ"}, {"ុ", "ŏ", "ŭ", "ŭ"}, {"ូ", "o", "u", "u"}, {"ួ", "uŏ", "uŏ", "uŏ"},
            {"ើ", "aeu", "eu", "eu"}, {"ឿ", "œă", "œă", "œă"}, {"ៀ", "iĕ", "iĕ", "iĕ"}, {"េ", "é", "é", "é"},
            {"ែ", "ê", "ê", "ê"}, {"ៃ", "ai", "ey", "ey"}, {"ោ", "aô", "oŭ", "oŭ"}, {"ៅ", "au", "ŏu", "ŏu"},
            {"ំ", "âm", "um", "m"}, {"ះ", "ăh", "eăh", "h"},
    };
    // two vowel signs, after an a-series consonant, after an o-series consonant
    private static final String[][] VOWEL_COMBINATIONS = {
            {"ុំ", "om", "ŭm"}, {"ាំ", "ăm", "ŏâm"}, {"ិះ", "ĕh", "ĭh"}, {"ុះ", "ŏh", "ŭh"}, {"េះ", "éh", "éh"}, {"ោះ", "aôh", "ŏăh"},
    };
    private static final String[][] OTHERS = {
            // independent vowels
            {"ឥ", "ĕ"}, {"ឦ", "ei"}, {"ឧ", "ŏ"}, {"ឨ", "ou"}, {"ឩ", "ou"}, {"ឪ", "âu"}, {"ឫ", "rœ̆"}, {"ឬ", "rœ"},
            {"ឭ", "lœ̆"}, {"ឮ", "lœ"}, {"ឯ", "ê"}, {"ឰ", "ai"}, {"ឱ", "aô"}, {"ឳ", "au"}, {"ឲ", "aô"},
            // digits and punctuation
            {"០", "0"}, {"១", "1"}, {"២", "2"}, {"៣", "3"}, {"៤", "4"}, {"៥", "5"}, {"៦", "6"}, {"៧", "7"}, {"៨", "8"}, {"៩", "9"},
            {"៖", ":"}, {"ៗ", "&c."}, {"។", "."}, {"ៈ", "u’"}, {"៕", "."},
            // signs without a romanization, and zero-width (non-)joiners
            {"៉", ""}, {"៊", ""}, {"់", ""}, {"៌", ""}, {"៍", ""}, {"៎", ""}, {"៏", ""}, {"័", ""}, {"្", ""},
            {"‌", ""}, {"‍", ""},
    };
    private static final String INDEPENDENT_VOWELS = "ឥឦឧឨឩឪឫឬឭឮឯឰឱឳឲ";
    private static final String SIGNS_WITHOUT_SOUND = "៉៊់៌៍៎៏័";
    private static final String WORD_ENDS = "០១២៣៤៥៦៧៨៩៖ៗ។៕";
    private static final char ZERO_WIDTH_SPACE = '\u200B';

    private static final class Rule {
        final String output;
        final String keep;
        final boolean isVowel;

        Rule(final String output, final String keep, final boolean isVowel) {
            this.output = output;
            this.keep = keep;
            this.isVowel = isVowel;
        }
    }

    private static final Map<String, Rule> RULES = new HashMap<>();
    private static final Set<Character> CONSONANT_SET = new HashSet<>();
    private static final Set<Character> VOWEL_SIGN_SET = new HashSet<>();

    static {
        for (final String[] c : CONSONANTS) {
            final char consonant = c[0].charAt(0);
            final char own = "a".equals(c[2]) ? MARK_A : MARK_O;
            CONSONANT_SET.add(consonant);
            RULES.put(c[0], new Rule(c[1], String.valueOf(own), false));
            if (consonant != 'ឡ') { // CLDR has no subscript rules for ឡ
                RULES.put("" + COENG + consonant, new Rule(c[1] + (own == MARK_A ? "â" : "ô"), "", false));
                for (final char base : new char[]{MARK_A, MARK_O}) {
                    final char keep = SONORANTS.indexOf(consonant) >= 0 ? base : own;
                    RULES.put("" + base + COENG + consonant, new Rule(c[1], String.valueOf(keep), false));
                }
            }
        }
        for (final String[] v : VOWEL_SIGNS) {
            VOWEL_SIGN_SET.add(v[0].charAt(0));
            RULES.put(MARK_A + v[0], new Rule(v[1], "", true));
            RULES.put(MARK_O + v[0], new Rule(v[2], "", true));
            RULES.put(v[0], new Rule(v[3], "", true));
        }
        for (final String[] v : VOWEL_COMBINATIONS) {
            RULES.put(MARK_A + v[0], new Rule(v[1], "", true));
            RULES.put(MARK_O + v[0], new Rule(v[2], "", true));
        }
        for (final String[] o : OTHERS) {
            RULES.put(o[0], new Rule(o[1], "", INDEPENDENT_VOWELS.contains(o[0])));
        }
    }

    @Override
    public String transliterate(final String txt) {
        if (txt == null || txt.isEmpty()) {
            return txt;
        }
        // the markers are private-use characters, so they must never meet text from outside the runs
        final StringBuilder output = new StringBuilder(txt.length() * 2);
        int pos = 0;
        while (pos < txt.length()) {
            int end = pos;
            while (end < txt.length() && isInRun(txt.charAt(end))) {
                end++;
            }
            if (end > pos) {
                transliterateRun(txt.substring(pos, end), output);
                pos = end;
            } else {
                final char c = txt.charAt(pos++);
                output.append(c == ZERO_WIDTH_SPACE ? ' ' : c);
            }
        }
        return toLatin1(Normalizer.normalize(output, Normalizer.Form.NFD));
    }

    private static void transliterateRun(final String run, final StringBuilder output) {
        final StringBuilder input = new StringBuilder(run);
        boolean hadVowel = false;
        int pos = 0;
        while (pos < input.length()) {
            final char c = input.charAt(pos);
            final boolean isMark = c == MARK_A || c == MARK_O;
            if (isMark && pos + 1 < input.length() && (input.charAt(pos + 1) == '៉' || input.charAt(pos + 1) == '៊')) {
                input.setCharAt(pos, input.charAt(pos + 1) == '៉' ? MARK_A : MARK_O);
                input.deleteCharAt(pos + 1);
                continue;
            }
            Rule rule = null;
            int length = 3;
            for (; length >= 1 && rule == null; length--) {
                if (pos + length <= input.length()) {
                    rule = RULES.get(input.substring(pos, pos + length));
                }
            }
            if (rule != null) {
                length++; // undo the last decrement
                output.append(rule.output);
                hadVowel = WORD_ENDS.indexOf(input.charAt(pos)) < 0 && (hadVowel || rule.isVowel);
                input.replace(pos, pos + length, rule.keep);
                continue;
            }
            if (isMark) {
                int next = pos + 1;
                while (next < input.length() && SIGNS_WITHOUT_SOUND.indexOf(input.charAt(next)) >= 0) {
                    next++;
                }
                final boolean drop;
                if (next >= input.length() || !isKhmerLetter(input.charAt(next))) { // punctuation, digits or other text end the word
                    drop = hadVowel;
                } else if (CONSONANT_SET.contains(input.charAt(next))) {
                    drop = hadVowel && startsSyllable(input, next);
                } else {
                    drop = false;
                }
                if (drop) {
                    hadVowel = false;
                } else {
                    output.append(c == MARK_A ? "â" : "ô");
                    hadVowel = true;
                }
            } else {
                hadVowel &= isKhmerLetter(c); // symbols without a rule, such as ៛, end the word too
                output.append(c);
            }
            pos++;
        }
    }

    /**
     * Expects decomposed text, so a breve is always a separate combining character.
     */
    private static String toLatin1(final String decomposed) {
        final StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            final char c = decomposed.charAt(i);
            switch (c) {
                case '\u0306': // combining breve
                    break;
                case 'œ':
                    sb.append("oe");
                    break;
                case '’':
                    sb.append('\'');
                    break;
                default:
                    sb.append(c);
            }
        }
        return Normalizer.normalize(sb, Normalizer.Form.NFC);
    }

    private static boolean isInRun(final char c) {
        // Khmer, Khmer Symbols, and the zero-width (non-)joiners used inside words
        return (c >= 'ក' && c <= '៿') || (c >= '᧠' && c <= '᧿') || c == '\u200C' || c == '\u200D';
    }

    private static boolean isKhmerLetter(final char c) {
        return c >= '\u1780' && c <= '\u17D3';
    }

    private static boolean startsSyllable(final CharSequence input, final int consonant) {
        int next = consonant + 1;
        while (next < input.length() && SIGNS_WITHOUT_SOUND.indexOf(input.charAt(next)) >= 0) {
            next++;
        }
        return next < input.length() && (VOWEL_SIGN_SET.contains(input.charAt(next)) || input.charAt(next) == COENG);
    }
}
