package nodomain.freeyourgadget.gadgetbridge.util.language;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.CzechTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.ExtendedAsciiTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.FlattenToAsciiTransliterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_LANGUAGES;

/**
 * Tests LanguageUtils
 */
public class LanguageUtilsTest extends TestBase {

    private GBDevice dummyGBDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
    }


    @Test
    public void testStringTransliterateCyrillic() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("russian");

        // input with cyrillic and diacritic letters
        String input = "Прõсто текčт";
        String output = transliterator.transliterate(input);
        String result = "Prosto tekct";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateSerbian() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("serbian");

        final Map<String, String> tests = new LinkedHashMap<String, String>() {{
            put("Тхе qицк брон фоx јумпед овер тхе лаз* дог", "The qick bron fox jumped over the laz* dog");
            put("Српска ћирилица", "Srpska cirilica");
            put("Novak Đoković", "Novak Djokovic");
            put("Џ, Њ and Љ", "Dz, Nj and Lj");
            put("Љуљачка", "Ljuljacka");
            put("Наковањ", "Nakovanj");
            put("Качкаваљ", "Kackavalj");
            put("Чачак", "Cacak");
            put("Ч, ч", "C, c");
            put("Ћ, ћ", "C, c");
            put("Ж, ж", "Z, z");
            put("Ш, ш", "S, s");
            put("Ђ, ђ", "Dj, dj");
            put("Џ, џ", "Dz, dz");
            put("Њ, њ", "Nj, nj");
            put("Љ, љ", "Lj, lj");
        }};

        for (final Map.Entry<String, String> e : tests.entrySet()) {
            assertEquals("Transliteration failed for " + e.getKey(), e.getValue(), transliterator.transliterate(e.getKey()));
        }
    }

    @Test
    public void testStringTransliterateHebrew() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("hebrew");

        String input = "בדיקה עברית";
        String output = transliterator.transliterate(input);
        String result = "bdykh 'bryth";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateArabic() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("arabic");

        String pangram = "نص حكيم له سر قاطع وذو شأن عظيم مكتوب على ثوب أخضر ومغلف بجلد أزرق";
        String pangramExpected = "n9 7kym lh sr qa63 wthw sh2n 36'ym mktwb 3la thwb 259'r wm3'lf bjld 2zrq";
        String pangramActual = transliterator.transliterate(pangram);
        assertEquals("Arabic pangram transliteration failed", pangramExpected, pangramActual);

        String taMarbutah = "ﺓ";
        String taMarbutahExpected = "";
        String taMarbutahActual = transliterator.transliterate(taMarbutah);
        assertEquals("ta marbutah transliteration failed", taMarbutahExpected, taMarbutahActual);

        String hamza = "ءأؤإئآ";
        String hamzaExpected = "222222";
        String hamzaActual = transliterator.transliterate(hamza);
        assertEquals("hamza transliteration failed", hamzaExpected, hamzaActual);

        String easternArabicNumeralsArabic = "٠١٢٣٤٥٦٧٨٩";
        String easternArabicNumeralsExpected = "0123456789";
        assertEquals("Eastern Arabic numerals (Arabic) failed", easternArabicNumeralsExpected,
                transliterator.transliterate(easternArabicNumeralsArabic));
    }

    public void testStringTransliteratePersian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("persian");

        String farsi = "گچپژ";
        String farsiExpected = "gchpzh";
        String farsiActual = transliterator.transliterate(farsi);
        assertEquals("Farsi transiteration failed", farsiExpected, farsiActual);

        String easternArabicNumeralsFarsi = "۰۱۲۳۴۵۶۷۸۹";
        String easternArabicNumeralsExpected = "0123456789";

        assertEquals("Eastern Arabic numerals (Farsi) failed", easternArabicNumeralsExpected,
                transliterator.transliterate(easternArabicNumeralsFarsi));
    }

    @Test
    public void testStringTransliterateBengali() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("bengali");

        // input with cyrillic and diacritic letters
        String[] inputs = {"অনিরুদ্ধ", "বিজ্ঞানযাত্রা চলছে চলবে।", "আমি সব দেখেশুনে ক্ষেপে গিয়ে করি বাঙলায় চিৎকার!",
                "আমার জাভা কোড is so bad! কী আর বলবো!"};
        String[] outputs = {"oniruddho", "biggaanJaatraa cholchhe cholbe.",
                "aami sob dekheshune kkhepe giye kori baanglaay chitkaar!",
                "aamaar jaabhaa koD is so bad! kii aar bolbo!"};

        String result;

        for (int i = 0; i < inputs.length; i++) {
            result = transliterator.transliterate(inputs[i]);
            assertEquals("Transliteration failed", outputs[i], result);
        }
    }

    @Test
    public void testStringTransliterateKorean() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("korean");

        // A familiar phrase with no special provisions.
        String hello = "안녕하세요";
        String helloExpected = "annyeonghaseyo";
        String helloActual = transliterator.transliterate(hello);
        assertEquals("Korean hello transliteration failed", helloExpected, helloActual);

        // Korean pangram. Includes some ASCII punctuation which should not be changed by
        // transliteration.
        //
        // Translation: "Chocolate!? What I wanted was some rice puffs and clothes." "Child, why are
        // you complaining again?"
        String pangram = "\"웬 초콜릿? 제가 원했던 건 뻥튀기 쬐끔과 의류예요.\" \"얘야, 왜 또 불평?\"";
        String pangramExpected = "\"wen chokollit? jega wonhaetdeon geon ppeongtwigi jjoekkeumgwa uiryuyeyo.\" \"yaeya, wae tto bulpyeong?\"";
        String pangramActual = transliterator.transliterate(pangram);
        assertEquals("Korean pangram transliteration failed", pangramExpected, pangramActual);

        // Several words excercising special provisions, from Wikipedia.
        String special = "좋고, 놓다, 잡혀, 낳지";
        String specialExpected = "joko, nota, japhyeo, nachi";
        String specialActual = transliterator.transliterate(special);
        assertEquals("Korean special provisions transliteration failed", specialExpected, specialActual);

        // Isolated jamo.
        String isolatedJamo = "ㅋㅋㅋ";
        String isolatedJamoExpected = "kkk";
        String isolatedJamoActual = transliterator.transliterate(isolatedJamo);
        assertEquals("Korean isolated jamo transliteration failed", isolatedJamoExpected, isolatedJamoActual);

        // Korean transliteration shouldn't touch non-Hangul composites.
        String german = "schön";
        String germanExpected = german;
        String germanActual = transliterator.transliterate(german);
        assertEquals("Korean transliteration modified a non-Hangul composite", germanExpected, germanActual);
    }

    @Test
    public void testStringTransliterateLatvian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("latvian");

        String input = "ā č ē ģ ī ķ ļ ņ š ū ž Ā Č Ē Ģ Ī Ķ Ļ Ņ Š Ū Ž";
        String output = transliterator.transliterate(input);
        String expected = "a c e g i k l n s u z A C E G I K L N S U Z";
        assertEquals("latvian translation failed", expected, output);

        input = "aāa cčc eēe gģg iīi kķk lļl nņn sšs uūu zžz AĀA CČC EĒE GĢG IĪI KĶK LĻL NŅN SŠS UŪU ZŽZ";
        output = transliterator.transliterate(input);
        expected = "aaa ccc eee ggg iii kkk lll nnn sss uuu zzz AAA CCC EEE GGG III KKK LLL NNN SSS UUU ZZZ";
        assertEquals("latvian translation failed", expected, output);
    }

    @Test
    public void testStringTransliterateLithuanian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("lithuanian");

        String input = "ą č ę ė į š ų ū ž";
        String output = transliterator.transliterate(input);
        String expected = "a c e e i s u u z";
        assertEquals("lithuanian translation failed", expected, output);

        input = "aąa cčc eęe eėe iįi sšs uųu uūu zžz";
        output = transliterator.transliterate(input);
        expected = "aaa ccc eee eee iii sss uuu uuu zzz";
        assertEquals("lithuanian translation failed", expected, output);
    }

    @Test
    public void testStringTransliterateGeorgian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("georgian");

        String input = "ა ბ ტ თ ჟ ყ წ ჭ ჰ";
        String output = transliterator.transliterate(input);
        String expected = "a b t T J y w W h";
        assertEquals("georgian transliteration failed", expected, output);
    }

    @Test
    public void testStringTransliterateHungarian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("hungarian");

        String input = "á é í ó ö ő ü ű";
        String output = transliterator.transliterate(input);
        String expected = "a e i o o o u u";
        assertEquals("hungarian transliteration failed", expected, output);
    }

    @Test
    public void testStringTransliterateCommonSymbols() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("common_symbols");

        String input = "© ® ™ ° « – » “ ” 〜 ² ³ ₅";
        String output = transliterator.transliterate(input);
        String expected = "(c) (r) (tm) * < - > \" \" ~ 2 3 5";
        assertEquals("common symbols translation failed", expected, output);

        input = "a©a b®b c™c d°d e«e f–f g»g h“h i”i j〜j k²k l³l m₅m";
        output = transliterator.transliterate(input);
        expected = "a(c)a b(r)b c(tm)c d*d e<e f-f g>g h\"h i\"i j~j k2k l3l m5m";
        assertEquals("common symbols translation failed", expected, output);
    }

    @Test
    public void testStringTransliterateCroatian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("croatian");

        String input = "č ć đ š ž";
        String output = transliterator.transliterate(input);
        String expected = "c c d s z";
        assertEquals("croatian transliteration failed", expected, output);
    }

    @Test
    public void testStringTransliteratePolish() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("polish");

        String input = "ą ć ę ł ń ó ś ź ż Ą Ć Ę Ł Ń Ó Ś Ź Ż";
        String output = transliterator.transliterate(input);
        String expected = "a c e l n o s z z A C E L N O S Z Z";
        assertEquals("polish transliteration failed", expected, output);
    }

    @Test
    public void testFlattenToAscii() throws Exception {
        final FlattenToAsciiTransliterator transliterator = new FlattenToAsciiTransliterator();
        String input = "ä ș ț ă ﬁne";
        String output = transliterator.transliterate(input);
        String expected = "a s t a fine";
        assertEquals("flatten to ascii transliteration failed", expected, output);
    }

    @Test
    public void testMultitransliterator() throws Exception {
        final MultiTransliterator multiTransliterator = new MultiTransliterator(Arrays.asList(
                new CzechTransliterator(),
                new ExtendedAsciiTransliterator(),
                new FlattenToAsciiTransliterator()
        ));
        assertEquals("Zlutoucky kun upel \"dabelske\" \"ody\"", multiTransliterator.transliterate("Žluťoučký kůň úpěl »ďábelské« „ódy“"));
        assertEquals("300 Kc", multiTransliterator.transliterate("300\u00A0Kč"));
    }

    @Test
    public void testTransliterateOption() throws Exception {
        enableTransliteration(false);
        assertNull("Transliteration option fail! Expected 'Off' by default, but result is 'On'",
                getTransliteration());

        enableTransliteration(true);
        assertNotNull("Transliteration option fail! Expected 'On', but result is 'Off'", getTransliteration());
    }

    private void enableTransliteration(boolean enable) {
        SharedPreferences devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(dummyGBDevice.getAddress());
        SharedPreferences.Editor editor = devicePrefs.edit();
        if (enable) {
            editor.putString(PREF_TRANSLITERATION_LANGUAGES, "extended_ascii,scandinavian,german,russian,hebrew,greek,ukranian,arabic,persian,lithuanian,polish,estonian,icelandic,czech,turkish,bengali,korean,georgian,croatian");
        } else {
            editor.remove(PREF_TRANSLITERATION_LANGUAGES);
        }
        editor.apply();
    }

    private Transliterator getTransliteration() {
        return LanguageUtils.getTransliterator(dummyGBDevice);
    }
}
