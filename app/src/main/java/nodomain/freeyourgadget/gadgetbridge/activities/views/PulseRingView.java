/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.activities.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * Thick progress ring: grey track, neon-blue arc with a rounded cap and a dot marker.
 */
public class PulseRingView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private int accentColor;
    private int darkAccentColor;
    private int nubColor;
    private int overNubColor;

    private float progress = 0f; // 0..1 (may exceed 1, clamped for the arc)
    private float strokeWidth;

    public PulseRingView(Context context) {
        this(context, null);
    }

    public PulseRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        final float density = getResources().getDisplayMetrics().density;
        // thick band so the round nub (diameter = band width) sits flush inside it
        strokeWidth = 26f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(getResources().getColor(R.color.pulse_card_alt));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Pulse: the "overachiever" second lap, drawn once you pass the goal.
        overPaint.setStyle(Paint.Style.STROKE);
        overPaint.setStrokeWidth(strokeWidth);
        overPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(getResources().getColor(R.color.pulse_ring_steps));
    }

    private android.animation.ValueAnimator progressAnimator;

    public void setProgress(float factor) {
        this.progress = Math.max(0f, factor);
        invalidate();
    }

    /** spring the arc to its target with a little overshoot */
    public void setProgressAnimated(final float factor) {
        final float target = Math.max(0f, factor);
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        progressAnimator = android.animation.ValueAnimator.ofFloat(progress, target);
        progressAnimator.setDuration(820);
        progressAnimator.setInterpolator(new android.view.animation.OvershootInterpolator(1.4f));
        progressAnimator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // nub fits inside the band now, so padding is just half the stroke
        final float pad = strokeWidth / 2f + 2f;
        final float size = Math.min(w, h);
        final float left = (w - size) / 2f + pad;
        final float top = (h - size) / 2f + pad;
        arcRect.set(left, top, left + size - 2 * pad, top + size - 2 * pad);
        accentColor = nodomain.freeyourgadget.gadgetbridge.GBApplication.getAccentColor(getContext());
        darkAccentColor = darken(accentColor, 0.55f);
        nubColor = darken(accentColor, 0.6f);        // normal nub: a deep dot on the bright ring
        overNubColor = lighten(accentColor, 0.55f);  // overachiever nub: a bright pop on the deep lap
        progressPaint.setShader(new LinearGradient(
                arcRect.left, arcRect.top, arcRect.right, arcRect.bottom,
                accentColor,
                getResources().getColor(R.color.pulse_ring_steps),
                Shader.TileMode.CLAMP));
        overPaint.setColor(darkAccentColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Full track
        canvas.drawArc(arcRect, 0, 360, false, trackPaint);

        // Lap 1: fill to the goal in the accent colour
        final float sweep1 = Math.min(progress, 1f) * 360f;
        if (sweep1 > 0) {
            canvas.drawArc(arcRect, -90, sweep1, false, progressPaint);
        }

        // Lap 2: once past the goal, keep filling in a deeper shade
        final boolean over = progress > 1f;
        float leadSweep = sweep1;
        if (over) {
            final float sweep2 = Math.min(progress - 1f, 1f) * 360f;
            canvas.drawArc(arcRect, -90, sweep2, false, overPaint);
            leadSweep = sweep2;
        }

        // Rounded nub at the leading edge, flush inside the band
        final double angle = Math.toRadians(-90 + leadSweep);
        final float cx = arcRect.centerX() + (float) Math.cos(angle) * arcRect.width() / 2f;
        final float cy = arcRect.centerY() + (float) Math.sin(angle) * arcRect.height() / 2f;
        dotPaint.setColor(over ? overNubColor : nubColor);
        canvas.drawCircle(cx, cy, strokeWidth / 2f, dotPaint);
    }

    /** Darken a colour by mixing toward black (keep = fraction of the original brightness). */
    private static int darken(final int color, final float keep) {
        final int r = Math.round(android.graphics.Color.red(color) * keep);
        final int g = Math.round(android.graphics.Color.green(color) * keep);
        final int b = Math.round(android.graphics.Color.blue(color) * keep);
        return android.graphics.Color.rgb(r, g, b);
    }

    /** Lighten a colour by mixing toward white (amount = 0..1 toward white). */
    private static int lighten(final int color, final float amount) {
        final int r = Math.round(android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * amount);
        final int g = Math.round(android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * amount);
        final int b = Math.round(android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * amount);
        return android.graphics.Color.rgb(r, g, b);
    }
}
