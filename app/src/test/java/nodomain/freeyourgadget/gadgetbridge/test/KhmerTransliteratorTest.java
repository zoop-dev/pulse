package nodomain.freeyourgadget.gadgetbridge.test;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.util.language.LanguageUtils;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.KhmerTransliterator;

public class KhmerTransliteratorTest extends TestCase {
    private final Transliterator transliterator = new KhmerTransliterator();

    @Test
    public void testRegistered() {
        Assert.assertNotNull(LanguageUtils.getTransliterator("khmer"));
    }

    @Test
    public void testWords() {
        final Map<String, String> words = new LinkedHashMap<String, String>() {{
            put("ក", "kâ");                 // inherent vowel on a single consonant
            put("កម្ពុជា", "kâmpucha");      // subscript consonant
            put("ភ្នំពេញ", "phnumpénh");      // no inherent vowel on the final consonant
            put("សៀមរាប", "siemrab");        // final consonant before a new syllable
            put("បាត់ដំបង", "batdâmbâng");    // sign without sound, consonant-vowel-consonant syllable
            put("អរគុណ", "'ârkun");
            put("ខ្ញុំ", "khnhom");           // sonorant subscript keeps the series of the base
            put("ថ្ងៃ", "thngai");
            put("ម៉ោង", "maông");            // series switch sign
            put("ឧបករណ៍", "obâkârôn");       // independent vowel
        }};
        for (final Map.Entry<String, String> e : words.entrySet()) {
            Assert.assertEquals("Transliteration failed for " + e.getKey(), e.getValue(), transliterator.transliterate(e.getKey()));
        }
    }

    @Test
    public void testDigitsAndPunctuation() {
        Assert.assertEquals("chhnam 2026", transliterator.transliterate("ឆ្នាំ ២០២៦"));
        // zero-width spaces between words become spaces, ៕ becomes a full stop
        Assert.assertEquals("paky ti muoy.", transliterator.transliterate("ពាក្យ​ទី​មួយ៕"));
    }

    @Test
    public void testMixedStrings() {
        Assert.assertEquals("Hello suostei!", transliterator.transliterate("Hello សួស្តី!"));
        Assert.assertEquals("Plain text stays the same", transliterator.transliterate("Plain text stays the same"));
    }

    @Test
    public void testWordBoundaries() {
        // a word ending in a vowel must not drop the inherent vowel of the next word
        Assert.assertEquals("ka kâ", transliterator.transliterate("កា ក"));
        Assert.assertEquals("ka kâ", transliterator.transliterate("កា\u200Bក"));
        Assert.assertEquals("ka.kâ", transliterator.transliterate("កា។ក"));
        // so do Khmer symbols without a romanization
        Assert.assertEquals("ka៛kâ", transliterator.transliterate("កា៛ក"));
        Assert.assertEquals("ka៚kâ", transliterator.transliterate("កា៚ក"));
    }

    @Test
    public void testPrivateUseCharactersAreKept() {
        final String text = "x\uE000y\uE001z \uE000ា";
        Assert.assertEquals("x\uE000y\uE001z \uE000a", transliterator.transliterate(text));
    }

    @Test
    public void testOutputIsLatin1() {
        // watch fonts often lack Latin Extended-A, such as ŏ, ŭ and œ
        final String text = "សួស្តី កម្ពុជា ឥឡូវ ឫ ពឺ ថ្ងៃនេះ អរគុណ";
        for (final char c : transliterator.transliterate(text).toCharArray()) {
            Assert.assertTrue("Not Latin-1: " + c, c <= '\u00FF');
        }
    }

    @Test
    public void testEmpty() {
        Assert.assertEquals("", transliterator.transliterate(""));
        Assert.assertNull(transliterator.transliterate(null));
    }
}
