package nodomain.freeyourgadget.gadgetbridge.util.language.impl;

import net.fellbaum.jemoji.EmojiManager;
import java.util.Objects;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

public class EmojiTransliterator implements Transliterator {
    @Override
    public String transliterate(String txt) {
        return EmojiManager.replaceAllEmojis(txt, emoji -> Objects.requireNonNullElse(emoji.getAllAliases().get(0), ""));
    }
}
