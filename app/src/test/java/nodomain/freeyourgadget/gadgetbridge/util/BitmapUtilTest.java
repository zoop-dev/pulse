/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.util;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class BitmapUtilTest extends TestBase {
    @Test
    public void adaptiveIconUsesForegroundAndFallbackDimensions() {
        final AdaptiveIconDrawable icon = new AdaptiveIconDrawable(
                new ColorDrawable(Color.RED),
                new ColorDrawable(Color.TRANSPARENT)
        );

        final Bitmap bitmap = BitmapUtil.toBitmap(icon);

        assertEquals(128, bitmap.getWidth());
        assertEquals(128, bitmap.getHeight());
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(64, 64));
    }
}
