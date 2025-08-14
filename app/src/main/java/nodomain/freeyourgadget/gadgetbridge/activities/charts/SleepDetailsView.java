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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class SleepDetailsView extends View {

    public static class SleepDetail {
        public int type;
        public int durationMinutes;
        public long startTimestamp;

        public SleepDetail(int type, int durationMinutes, long startTimestamp) {
            this.type = type;
            this.durationMinutes = durationMinutes;
            this.startTimestamp = startTimestamp;
        }
    }


    public static class DataConfig {
        public int type;
        public int color;

        public DataConfig(int type, int color) {
            this.type = type;
            this.color = color;
        }
    }

    private List<SleepDetail> sleepDetails = new ArrayList<>();
    private int sleepMinutesCount = 0;

    private long startTimestamp;
    private long endTimestamp;

    private DataConfig[] config;

    private float heightUnit;

    private float unitWidth;

    private int horizontalLineCount;

    private float barHeight;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint connectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timeInfoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cornerPath = new Path();
    private final Path rectPath = new Path();
    private final RectF rectF = new RectF();
    private final Rect timeInfoTextRect = new Rect();
    private final Rect infoTextRect = new Rect();

    private final int infoTextSize = 25;

    private final float cornerRadius = 10.0F;

    private float selectorPos = -1.0F;

    private float moveEventLastX = 0.0F;

    private int chartLeftStart;
    private int chartWidth;
    private int chartTopStart;
    private int chartHeight;

    public SleepDetailsView(Context context) {
        super(context);
        init();
    }

    public SleepDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SleepDetailsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        config = null;
        horizontalLineCount = 8;
        heightUnit = (float) (1.0 / 8);

        gridPaint.setColor(Color.GRAY);
        gridPaint.setStrokeWidth(1.0F);
        gridPaint.setStrokeCap(Paint.Cap.ROUND);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{2f, 10f}, 0f));

        fillPaint.setStyle(Paint.Style.FILL);
        cornerPaint.setStyle(Paint.Style.FILL);
        connectionPaint.setStyle(Paint.Style.FILL);

        timeInfoTextPaint.setColor(Color.GRAY);
        timeInfoTextPaint.setTextAlign(Paint.Align.CENTER);
        timeInfoTextPaint.setTextSize(25);

        infoTextPaint.setColor(Color.GRAY);
        infoTextPaint.setTextAlign(Paint.Align.LEFT);
        infoTextPaint.setTextSize(infoTextSize);

        selectorPaint.setColor(Color.GRAY);
        selectorPaint.setStrokeWidth(1.0F);
        selectorPaint.setStrokeCap(Paint.Cap.ROUND);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setPathEffect(new DashPathEffect(new float[]{2f, 10f}, 0f));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private int getIndexByType(int type) {
        if (config == null)
            return -1;
        for (int i = 0; i < config.length; i++) {
            if (config[i].type == type) {
                return i;
            }
        }
        return -1;
    }

    private void drawGrid(Canvas canvas) {
        final int lineSpacing = (chartHeight) / horizontalLineCount;
        for (int i = 0; i <= horizontalLineCount; i++) {
            final float y = (i * lineSpacing);
            canvas.drawLine(chartLeftStart, chartTopStart + y, chartLeftStart + chartWidth, chartTopStart + y, gridPaint);
        }

        canvas.drawLine(chartLeftStart, chartTopStart, chartLeftStart, chartTopStart + chartHeight, gridPaint);
        canvas.drawLine(chartLeftStart + chartWidth, chartTopStart, chartLeftStart + chartWidth, chartTopStart + chartHeight, gridPaint);
    }

    private int getPositionFromType(int type) {
        if (config == null) {
            return 7;
        }
        int idx = getIndexByType(type);
        if (idx == -1) {
            return config.length * 2 - 1;
        }
        return idx * 2;
    }

    private int getColorFromType(int type) {
        int idx = getIndexByType(type);
        if (idx == -1) {
            return Color.GRAY;
        }
        return config[idx].color;
    }

    private enum Corner {
        TOP_LEFT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT
    }

    private void drawCorner(Canvas canvas, float tipX, float tipY, float left, float barWidth, int color, Corner corner) {
        final float halfBarHeight = barHeight / 2f;
        final float curveEdgeHeight = halfBarHeight + 3f;
        final float halfWidth = barWidth / 2f;

        final boolean isLeft = (corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT);
        final boolean isTop = (corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT);
        final float horizontalOffset = isLeft ? -0.5f : 0.5f;
        final float verticalOffset = isTop ? -curveEdgeHeight : curveEdgeHeight;

        final float secondX = tipX + horizontalOffset;
        final float thirdX = tipX + (horizontalOffset < 0 ? halfWidth : -halfWidth);

        cornerPath.reset();
        cornerPath.moveTo(tipX, tipY);
        cornerPath.lineTo(secondX, tipY + verticalOffset);
        cornerPath.lineTo(thirdX, tipY + verticalOffset);
        cornerPath.lineTo(thirdX, tipY);
        cornerPath.close();

        final float top = isTop ? tipY - barHeight : tipY + halfBarHeight;
        final float bottom = top + halfBarHeight;
        rectPath.reset();
        rectF.set(left, top, left + barWidth, bottom);
        rectPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);

        cornerPaint.setColor(color);

        canvas.save();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(rectPath);
        } else {
            canvas.clipPath(rectPath, Region.Op.DIFFERENCE);
        }

        canvas.drawPath(cornerPath, fillPaint);
        canvas.restore();
    }

    private void drawEdges(Canvas canvas, List<SleepDetail> details, int currentIndex, float left, float top) {
        if (currentIndex < 0 || currentIndex >= details.size()) return;

        SleepDetail currentData = details.get(currentIndex);
        final int currentType = currentData.type;
        final int currentIdx = getIndexByType(currentType);
        if (currentIdx == -1) return;

        final float posY = top + (barHeight / 2f);
        final int color = getColorFromType(currentType);

        final float currentUnitWidth = currentData.durationMinutes * unitWidth;
        if (currentIndex > 0) {
            final int prevIdx = getIndexByType(details.get(currentIndex - 1).type);
            if (prevIdx != -1) {
                Corner corner = (currentIdx > prevIdx) ? Corner.TOP_LEFT : Corner.BOTTOM_LEFT;
                drawCorner(canvas, left, posY, left, currentUnitWidth, color, corner);
            }
        }

        if (currentIndex < details.size() - 1) {
            final int nextIdx = getIndexByType(details.get(currentIndex + 1).type);
            if (nextIdx != -1) {
                final float rightX = left + currentUnitWidth;
                Corner corner = (currentIdx > nextIdx) ? Corner.TOP_RIGHT : Corner.BOTTOM_RIGHT;
                drawCorner(canvas, rightX, posY, left, currentUnitWidth, color, corner);
            }
        }
    }

    private void drawConnections(Canvas canvas, List<SleepDetail> details, int currentIndex, float left) {
        if (currentIndex <= 0 || currentIndex >= details.size()) {
            return;
        }

        int prevType = details.get(currentIndex - 1).type;
        int curType = details.get(currentIndex).type;

        int prevIdx = getIndexByType(prevType);
        int curIdx = getIndexByType(curType);
        if (prevIdx == -1 || curIdx == -1) {
            return;
        }

        float prevY = chartTopStart + getPositionFromType(prevType) * barHeight;
        float curY = chartTopStart + getPositionFromType(curType) * barHeight;

        float topY = Math.min(prevY, curY) + barHeight;
        float bottomY = Math.max(prevY, curY);

        int startColor = (curIdx < prevIdx) ? getColorFromType(curType) : getColorFromType(prevType);
        int endColor = (curIdx < prevIdx) ? getColorFromType(prevType) : getColorFromType(curType);

        Shader shader = new LinearGradient(left, topY, left, bottomY, startColor, endColor, Shader.TileMode.MIRROR);
        connectionPaint.setShader(shader);
        canvas.drawLine(left, topY, left, bottomY, connectionPaint);
        connectionPaint.setShader(null);
    }

    private void drawDetails(Canvas canvas) {
        if (sleepDetails.isEmpty()) return;

        float x = chartLeftStart;
        for (int i = 0, n = sleepDetails.size(); i < n; i++) {
            SleepDetail sd = sleepDetails.get(i);
            float top = chartTopStart + getPositionFromType(sd.type) * barHeight;

            fillPaint.setColor(getColorFromType(sd.type));

            final float currentUnitWidth = sd.durationMinutes * unitWidth;

            rectF.set(x, top, x + currentUnitWidth, top + barHeight);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint);

            drawEdges(canvas, sleepDetails, i, x, top);
            drawConnections(canvas, sleepDetails, i, x);

            x += currentUnitWidth;
        }
    }

    public static String formatDurationHoursMinutes(long duration) {
        Date date = new Date(duration);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private String timeStringFormat(long seconds) {
        return DateTimeUtils.formatDurationHoursMinutes(seconds, TimeUnit.SECONDS);
    }

    private void drawSelectedInfo(Canvas canvas) {
        float x = chartLeftStart;
        SleepDetail curDetail = null;

        for (int i = 0; i < sleepDetails.size(); i++) {
            final SleepDetail detail = sleepDetails.get(i);
            final float curXEnd = x + detail.durationMinutes * unitWidth;

            boolean selected = (i < sleepDetails.size() - 1)
                    ? (selectorPos >= x && selectorPos < curXEnd)
                    : (selectorPos >= x && selectorPos <= curXEnd);
            if (selected) {
                curDetail = detail;
                break;
            }
            x = curXEnd;
        }

        if (curDetail == null) {
            return;
        }

        final long endTimestamp = curDetail.startTimestamp + ((curDetail.durationMinutes) * 60000L);
        final String startStr = SleepDetailsView.formatDurationHoursMinutes(curDetail.startTimestamp);
        final String endStr = SleepDetailsView.formatDurationHoursMinutes(endTimestamp);
        final String info = String.format("%s - %s (%s)", startStr, endStr, timeStringFormat(curDetail.durationMinutes * 60L));

        infoTextPaint.getTextBounds(info, 0, info.length(), infoTextRect);

        fillPaint.setColor(getColorFromType(curDetail.type));

        final int iconSize = infoTextSize;

        final float infoIconX = chartLeftStart + ((float) chartWidth /2) - (float) infoTextRect.width() / 2 - iconSize - 5;

        rectF.set(infoIconX, infoTextRect.height() - iconSize, infoIconX + iconSize, infoTextRect.height());
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint);

        final float infoTextX = chartLeftStart + ((float) chartWidth /2) - (float) infoTextRect.width() / 2;
        canvas.drawText(info, infoTextX, infoTextRect.height(), infoTextPaint);

    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();

        final String startDate = formatDurationHoursMinutes(this.startTimestamp);
        timeInfoTextPaint.getTextBounds(startDate, 0, startDate.length(), timeInfoTextRect);

        final int timeInfoHeight = timeInfoTextRect.height() * 2;

        chartLeftStart = timeInfoTextRect.width() / 2;
        chartWidth = width - chartLeftStart - (timeInfoTextRect.width() / 2);
        chartTopStart = infoTextSize * 2;
        chartHeight = height - chartTopStart - timeInfoHeight;

        barHeight = (chartHeight) * heightUnit;

        unitWidth = sleepMinutesCount > 0 ? ((float) chartWidth / sleepMinutesCount) : 0;

        drawGrid(canvas);
        drawDetails(canvas);

        //draw start/end Time Info
        final String endDate = formatDurationHoursMinutes(this.endTimestamp);
        float timeInfoY = chartTopStart + chartHeight + timeInfoHeight;
        canvas.drawText(startDate, chartLeftStart, timeInfoY, timeInfoTextPaint);
        canvas.drawText(endDate, width - (float) timeInfoTextRect.width() / 2, timeInfoY, timeInfoTextPaint);

        //draw selector
        if (selectorPos > chartLeftStart && selectorPos < chartLeftStart + chartWidth) {
            canvas.drawLine(selectorPos, chartTopStart, selectorPos, chartTopStart + chartHeight, selectorPaint);
            if (selectorPos > (chartLeftStart + timeInfoTextRect.width()) && selectorPos < (chartLeftStart + chartWidth - timeInfoTextRect.width())) {
                final String selectedDate = formatDurationHoursMinutes(this.startTimestamp + (long) ((selectorPos - chartLeftStart) / unitWidth) * 60000L);
                canvas.drawText(selectedDate, selectorPos, timeInfoY, timeInfoTextPaint);
            }
            drawSelectedInfo(canvas);
        }
    }

    public void setConfig(DataConfig[] config) {
        this.config = config;
        horizontalLineCount = config.length * 2;
        heightUnit = (float) (1.0 / (config.length * 2));
    }

    public void setData(List<SleepDetail> sleepDetails) {
        this.sleepDetails = sleepDetails;
        this.sleepMinutesCount = 0;
        for (SleepDetail sd : sleepDetails) {
            this.sleepMinutesCount += sd.durationMinutes;
        }
        if (!sleepDetails.isEmpty()) {
            this.startTimestamp = sleepDetails.get(0).startTimestamp;
            SleepDetail last = sleepDetails.get(sleepDetails.size() - 1);
            this.endTimestamp = last.startTimestamp + ((last.durationMinutes) * 60000L);
        }
        selectorPos = -1.0F;
        invalidate();
    }


    @Override
    public boolean performClick() {
        super.performClick();
        // TODO: added only to suppress warning. I don't know what to do with this method here.
        return true;
    }

    private boolean isStart = false;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                moveEventLastX = ev.getX();
                isStart = true;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float x = ev.getX();
                final float dx = x - moveEventLastX;
                if(dx != 0) {
                    if(selectorPos < 0) {
                        selectorPos = (dx < 0)?chartLeftStart + chartWidth:0;
                    }
                    selectorPos = Math.max(0, Math.min(chartLeftStart + chartWidth, selectorPos + dx));
                    if (isStart && Math.abs(dx) > 5.0) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        isStart = false;
                    }
                    moveEventLastX = x;
                    invalidate();
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                moveEventLastX = ev.getX();
                performClick();
                getParent().requestDisallowInterceptTouchEvent(false);
                isStart = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                getParent().requestDisallowInterceptTouchEvent(false);
                isStart = false;
                break;
            }
            default:
                break;
        }
        return true;
    }

}
