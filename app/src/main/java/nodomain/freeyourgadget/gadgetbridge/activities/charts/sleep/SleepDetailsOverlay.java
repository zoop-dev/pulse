/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep;

import android.graphics.Canvas;

public interface SleepDetailsOverlay {

    void init(float scaleTextSize, float labelTextSize, int horizontalLineCount);

    void drawOverlayScale(Canvas canvas, int lineSpacing, int chartTopStart, int x);

    void drawOverlay(Canvas canvas, int left, int top, int height, int width);

    void drawOverlaySelector(Canvas canvas, int left, int top, int height, int width, float selectorPos);
}
