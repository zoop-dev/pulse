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
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.Locale;

public class SimpleSleepDetailsHROverlay implements SleepDetailsOverlay {

        public static int NO_DATA = Integer.MIN_VALUE;

        private final int[] data;

        private final int average;

        private final int yMin;
        private final int yMax;
        private final int yDelta;

        private final Path linePath = new Path();
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint averagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hudBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect labelRect = new Rect();

        private final Paint scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SimpleSleepDetailsHROverlay(int yMin, int yMax, int[] data, int average, int mainColor, int hubBackgroundColor, int averageColor) {
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(4);
            linePaint.setColor(mainColor);

            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(mainColor);

            hudPaint.setStyle(Paint.Style.STROKE);
            hudPaint.setStrokeWidth(2);
            hudPaint.setColor(mainColor);

            hudBackgroundPaint.setStyle(Paint.Style.FILL);
            hudBackgroundPaint.setColor(hubBackgroundColor);

            labelPaint.setColor(Color.GRAY);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTextSize(25);

            scalePaint.setColor(Color.GRAY);
            scalePaint.setTextAlign(Paint.Align.RIGHT);
            scalePaint.setTextSize(25);

            averagePaint.setColor(averageColor);
            averagePaint.setStrokeWidth(2);
            averagePaint.setStrokeCap(Paint.Cap.ROUND);
            averagePaint.setStyle(Paint.Style.STROKE);
            averagePaint.setPathEffect(new DashPathEffect(new float[]{15f, 5f}, 0f));

            this.data = data;
            this.yMax = yMax;
            this.yMin = yMin;

            this.average = average;

            this.yDelta = this.yMax - this.yMin;
        }

        private float getAdjustedValue(float val) {
            if(val < yMin) {
                return 0;
            }
            if(val > yMax) {
                return yDelta;
            }
            return val - yMin;
        }

        @Override
        public void drawOverlayScale(Canvas canvas, int lineSpacing, int horizontalLineCount, int chartTopStart, int x) {

            int yLabelsDelta = yDelta / horizontalLineCount;
            int yStart = yMax;
            for (int i = 0; i <= horizontalLineCount; i++) {
                final float y = (i * lineSpacing);
                final String currentValue = String.format(Locale.ROOT, "%d", yStart);
                canvas.drawText(currentValue, x, chartTopStart + y, scalePaint);
                yStart -= yLabelsDelta;
            }
        }

        @Override
        public void drawOverlay(Canvas canvas, int left, int top, int height, int width) {
            if (data == null || data.length == 0)
                return;

            float unitHeight = ((float) height / yDelta);
            float unitWidth = ((float) width / data.length);
            linePath.reset();
            boolean first = true;
            float prevX = 0;
            float prevY = 0;
            for (int i = 0; i < data.length; i++) {
                if(data[i] == NO_DATA) {
                    if(!first) {
                        canvas.drawPath(linePath, linePaint);
                        linePath.reset();
                    }
                    first = true;
                }
                if (data[i] < 0) {
                    continue;
                }
                float curX = left + i * unitWidth;
                float curY = top + (height - (getAdjustedValue(data[i]) * unitHeight));
                if (first) {
                    linePath.moveTo(curX, curY);
                    first = false;
                } else {
                    float conX1, conY1, conX2, conY2;
                    conX1 = (prevX + curX) / 2;
                    conY1 = prevY;
                    conX2 = (prevX + curX) / 2;
                    conY2 = curY;
                    linePath.cubicTo(
                            conX1, conY1, conX2, conY2,
                            curX, curY
                    );
                }
                prevX = curX;
                prevY = curY;
            }
            canvas.drawPath(linePath, linePaint);
            if(average > 0) {
                float avgY = top + (height - (getAdjustedValue(average) * unitHeight));
                canvas.drawLine(left, avgY, left + width, avgY, averagePaint);
            }
        }

        @Override
        public void drawOverlaySelector(Canvas canvas, int left, int top, int height, int width, float selectorPos) {
            final float unitWidth = ((float) width / data.length);

            int idx = (int) ((selectorPos - left) / unitWidth);
            if (idx > 0 && idx < data.length && data[idx] > 0) {
                float unitHeight = ((float) height / yDelta);

                final int verticalPadding = 12;
                final int horizontalPadding = 16;
                final float cornerRadius = 10F;
                final float lineLength = 15F;

                final float dataValue = data[idx];

                final float centerX = left + idx * unitWidth;
                final float centerY = top + height - (getAdjustedValue(dataValue) * unitHeight);

                final String currentValue = String.format(Locale.ROOT, "%d", (int) dataValue);

                canvas.drawCircle(centerX, centerY, 7, dotPaint);

                labelPaint.getTextBounds(currentValue, 0, currentValue.length(), labelRect);

                float bgLeft = centerX - (labelRect.width() / 2f) - horizontalPadding;
                float bgBottom = centerY - lineLength - verticalPadding;
                float bgTop = bgBottom - labelRect.height() - 2 * verticalPadding;
                float bgRight = centerX + (labelRect.width() / 2f) + horizontalPadding;

                if (bgLeft < left + verticalPadding) {
                    bgLeft = left + verticalPadding;
                    bgRight = bgLeft + labelRect.width() + 2 * horizontalPadding;
                }
                if (bgRight > left + width - verticalPadding) {
                    bgRight = left + width - verticalPadding;
                    bgLeft = bgRight - (labelRect.width() + 2 * horizontalPadding);
                }

                float adjustedCenterX = (bgLeft + bgRight) / 2f;

                canvas.drawLine(centerX, centerY, adjustedCenterX, bgBottom, dotPaint);
                canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, cornerRadius, cornerRadius, hudBackgroundPaint);
                canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, cornerRadius, cornerRadius, hudPaint);
                canvas.drawText(currentValue, adjustedCenterX, bgBottom - verticalPadding, labelPaint);
            }
        }
}
