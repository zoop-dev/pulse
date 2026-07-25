package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringUtilsTest extends TestBase {
    private static final String SEP = ":";
    private static final String E1 = "e1";
    private static final String E2 = "e2";
    private static final String E3 = "e3";

    @Test
    public void testJoinNull() {
        StringBuilder result = StringUtils.join(SEP, (String[]) null);
        assertEquals("", result.toString());
    }

    @Test
    public void testJoinNullElement() {
        StringBuilder result = StringUtils.join(SEP, (String) null);
        assertEquals("", result.toString());
    }

    @Test
    public void testJoinSingleElement() {
        StringBuilder result = StringUtils.join(SEP, E1);
        assertEquals(E1, result.toString());
    }

    @Test
    public void testJoinSingleAndNullElement() {
        StringBuilder result = StringUtils.join(SEP, E1, null);
        assertEquals(E1, result.toString());
    }

    @Test
    public void testJoinTwoElements() {
        StringBuilder result = StringUtils.join(SEP, E1, E2);
        assertEquals(E1 + SEP + E2, result.toString());
    }

    @Test
    public void testJoinTwoElementsAndNull() {
        StringBuilder result = StringUtils.join(SEP, E1, null, E2);
        assertEquals(E1 + SEP + E2, result.toString());
    }

    @Test
    public void testJoinThreeElements() {
        StringBuilder result = StringUtils.join(SEP, E1, E2, E3);
        assertEquals(E1 + SEP + E2 + SEP + E3, result.toString());
    }

    @Test
    public void testUntilNullTerminator() {
        assertNull(StringUtils.untilNullTerminator("Hello, World!".getBytes(StandardCharsets.UTF_8), 7));
        assertNull(StringUtils.untilNullTerminator("Hello, World!".getBytes(StandardCharsets.UTF_8), 99));
        assertEquals("Hello, World!", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 0));
        assertEquals("World!", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 7));
        assertEquals("", StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 13));
        assertNull(StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 14));
        assertNull(StringUtils.untilNullTerminator("Hello, World!\0Another String".getBytes(StandardCharsets.UTF_8), 99));
    }

    @Test
    public void testTruncateToBytesWithPrefixNoTruncateSimple() {
        assertEquals(
                "hello",
                StringUtils.truncateToBytes("hello", 10, "..")
        );
    }

    @Test
    public void testTruncateToBytesWithPrefixSimple() {
        assertEquals(
                "hello..",
                StringUtils.truncateToBytes("hello world!", 7, "..")
        );
    }

    @Test
    public void testTruncateToBytesWithPrefixUtf8() {
        String expected = "abcdeöf..";
        String truncated = StringUtils.truncateToBytes("abcdeöfghijkl", 10, "..");
        assertEquals(expected, truncated);
        assertEquals(10, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixNoTruncateUtf8() {
        String expected = "abcdeöfghijkl";
        String truncated = StringUtils.truncateToBytes(expected, 20, "..");
        assertEquals(expected, truncated);
        assertEquals(14, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixUtf8Edge() {
        String expected1 = "abcd..";
        String truncated1 = StringUtils.truncateToBytes("abcdeöfghijkl", 6, "..");
        assertEquals(expected1, truncated1);
        assertEquals(6, truncated1.getBytes(StandardCharsets.UTF_8).length);

        String expected2 = "abcde..";
        String truncated2 = StringUtils.truncateToBytes("abcdeöfghijkl", 7, "..");
        assertEquals(expected2, truncated2);
        assertEquals(7, truncated2.getBytes(StandardCharsets.UTF_8).length);

        String expected3 = "abcde..";
        String truncated3 = StringUtils.truncateToBytes("abcdeöfghijkl", 8, "..");
        assertEquals(expected3, truncated3);
        assertEquals(7, truncated3.getBytes(StandardCharsets.UTF_8).length);

        String expected4 = "abcdeö..";
        String truncated4 = StringUtils.truncateToBytes("abcdeöfghijkl", 9, "..");
        assertEquals(expected4, truncated4);
        assertEquals(9, truncated4.getBytes(StandardCharsets.UTF_8).length);

        String expected5 = "abcdeöf..";
        String truncated5 = StringUtils.truncateToBytes("abcdeöfghijkl", 10, "..");
        assertEquals(expected5, truncated5);
        assertEquals(10, truncated5.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixNull() {
        assertNull(StringUtils.truncateToBytes(null, 10, ".."));
    }

    @Test
    public void testTruncateToBytesWithPrefixEmpty() {
        assertEquals("", StringUtils.truncateToBytes("", 10, ".."));
    }

    @Test
    public void testTruncateToBytesWithPrefixMaxBytesZero() {
        assertEquals("", StringUtils.truncateToBytes("hello", 0, ".."));
    }

    @Test
    public void testTruncateToBytesWithPrefixMaxBytesTooSmallForEllipsis() {
        // No room for "..", so it falls back to a raw (non-ellipsized) truncation.
        String truncated = StringUtils.truncateToBytes("abc", 1, "..");
        assertEquals("a", truncated);
        assertEquals(1, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixExactByteBoundaryUtf8() {
        // "abcdeö" is exactly 7 bytes - it should fit as-is, with no ellipsis appended.
        String expected = "abcdeö";
        String truncated = StringUtils.truncateToBytes(expected, 7, "..");
        assertEquals(expected, truncated);
        assertEquals(7, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixThreeByteChar() {
        // '中' is a 3-byte UTF-8 character that doesn't fit once the ".." budget is reserved.
        String truncated = StringUtils.truncateToBytes("ab中cd", 5, "..");
        assertEquals("ab..", truncated);
        assertEquals(4, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixFirstCharDoesNotFit() {
        // Even the first character ('中', 3 bytes) doesn't fit in the reserved budget.
        String truncated = StringUtils.truncateToBytes("中abc", 3, "..");
        assertEquals("..", truncated);
        assertEquals(2, truncated.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixSurrogatePair() {
        // U+1F600 (😀) is a 4-byte UTF-8 character represented as a surrogate pair in
        // Java - it must never be split in half.
        String truncated1 = StringUtils.truncateToBytes("abc😀def", 8, "..");
        assertEquals("abc..", truncated1);
        assertEquals(5, truncated1.getBytes(StandardCharsets.UTF_8).length);

        String truncated2 = StringUtils.truncateToBytes("abc😀def", 9, "..");
        assertEquals("abc😀..", truncated2);
        assertEquals(9, truncated2.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void testTruncateToBytesWithPrefixNoTruncateSurrogatePair() {
        String expected = "abc😀def";
        String truncated = StringUtils.truncateToBytes(expected, 20, "..");
        assertEquals(expected, truncated);
    }

    @Test
    public void testTruncateToBytesNoTruncateSimple() {
        assertEquals("hello", new String(StringUtils.truncateToBytes("hello", 10), StandardCharsets.UTF_8));
    }

    @Test
    public void testTruncateToBytesSimple() {
        assertEquals("hello", new String(StringUtils.truncateToBytes("hello world!", 5), StandardCharsets.UTF_8));
    }

    @Test
    public void testTruncateToBytesSingleCharMaxBytesZero() {
        // A single character must not be returned when it doesn't fit the budget.
        assertEquals(0, StringUtils.truncateToBytes("é", 1).length);
    }

    @Test
    public void testTruncateToBytesMaxBytesZero() {
        assertEquals(0, StringUtils.truncateToBytes("hello", 0).length);
    }

    @Test
    public void testTruncateToBytesSurrogatePair() {
        // U+1F600 (😀) is a 4-byte UTF-8 character represented as a surrogate pair in
        // Java - it must never be split in half.
        byte[] truncated = StringUtils.truncateToBytes("abc😀def", 5);
        assertEquals("abc", new String(truncated, StandardCharsets.UTF_8));
        assertEquals(3, truncated.length);
    }
}
