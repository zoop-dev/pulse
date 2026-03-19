package com.android.nQuant;

import android.graphics.Color;

public class BitmapUtilities {
    static final char BYTE_MAX = -Byte.MIN_VALUE + Byte.MAX_VALUE;

    static int getColorIndex(final int c, boolean hasSemiTransparency, boolean hasTransparency) {
        if (hasSemiTransparency)
            return (Color.alpha(c) & 0xF0) << 8 | (Color.red(c) & 0xF0) << 4 | (Color.green(c) & 0xF0) | (Color.blue(c) >> 4);
        if (hasTransparency)
            return (Color.alpha(c) & 0x80) << 8 | (Color.red(c) & 0xF8) << 7 | (Color.green(c) & 0xF8) << 2 | (Color.blue(c) >> 3);
        return (Color.red(c) & 0xF8) << 8 | (Color.green(c) & 0xFC) << 3 | (Color.blue(c) >> 3);
    }

    static double sqr(double value) {
        return value * value;
    }
}
