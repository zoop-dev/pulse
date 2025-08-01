package nodomain.freeyourgadget.gadgetbridge.util.language.impl;

import net.fellbaum.jemoji.EmojiManager;

import java.util.Comparator;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

public class EmojiTransliterator implements Transliterator {
    private static boolean isAllASCII(String input) {
        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 0x7F) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }

    @Override
    public String transliterate(String txt) {
        return EmojiManager.replaceAllEmojis(txt, emoji -> Objects.requireNonNullElse(
                emoji.getAllAliases()
                        .stream()
                        .filter(Objects::nonNull)
                        // This is required, because in another case emoji's will be returned
                        .filter(EmojiTransliterator::isAllASCII)
                        .min(Comparator.comparingInt(String::length))
                        .orElse(""), ""));
    }
}
