package nodomain.freeyourgadget.gadgetbridge.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.Assert;

import nodomain.freeyourgadget.gadgetbridge.util.language.impl.EmojiTransliterator;

public class EmojiTransliteratorTest extends TestCase {

    @Test
    public void testSimpleEmoji() {
        Assert.assertEquals("<3", new EmojiTransliterator().transliterate("❤\uFE0F"));
        Assert.assertEquals(":grinning:", new EmojiTransliterator().transliterate("\uD83D\uDE00"));
    }

    @Test
    public void testCompoundEmoji() {
        Assert.assertEquals(":factory_worker_tone4:", new EmojiTransliterator().transliterate("\uD83E\uDDD1\uD83C\uDFFE\u200D\uD83C\uDFED"));
    }
}
