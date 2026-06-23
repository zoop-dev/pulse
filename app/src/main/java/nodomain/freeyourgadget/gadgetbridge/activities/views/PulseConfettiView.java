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

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

/** Pulse: a short, self-removing confetti burst used to celebrate hitting a goal.
 *  Add it over a screen's content with {@link #celebrate(ViewGroup)}. */
public class PulseConfettiView extends View {

    private static final int[] COLORS = {
            0xFF2BB8FF, 0xFF2BD8FF, 0xFF7A5CFF, 0xFF4AD6A0, 0xFFFF6B6B, 0xFFFF9A4A
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private float[] x, y, vx, vy, rot, vrot, size;
    private int[] colors;
    private int count;
    private float progress; // 0..1
    private ValueAnimator animator;

    public PulseConfettiView(final Context context) {
        super(context);
        setWillNotDraw(false);
    }

    /** Convenience: overlay a confetti view on the given root and play once. */
    public static void celebrate(final ViewGroup root) {
        if (root == null) return;
        final PulseConfettiView view = new PulseConfettiView(root.getContext());
        root.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.post(view::start);
    }

    private void start() {
        final int w = getWidth();
        final int h = getHeight();
        if (w == 0 || h == 0) {
            removeSelf();
            return;
        }
        count = 90;
        x = new float[count]; y = new float[count];
        vx = new float[count]; vy = new float[count];
        rot = new float[count]; vrot = new float[count];
        size = new float[count]; colors = new int[count];

        final float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < count; i++) {
            // burst from the upper-middle, spraying outward + down
            x[i] = w * (0.35f + rnd.nextFloat() * 0.3f);
            y[i] = h * (0.28f + rnd.nextFloat() * 0.08f);
            vx[i] = (rnd.nextFloat() - 0.5f) * w * 1.6f;
            vy[i] = (-0.4f - rnd.nextFloat() * 0.6f) * h;
            rot[i] = rnd.nextFloat() * 360f;
            vrot[i] = (rnd.nextFloat() - 0.5f) * 720f;
            size[i] = (4f + rnd.nextFloat() * 6f) * density;
            colors[i] = COLORS[rnd.nextInt(COLORS.length)];
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1700);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final android.animation.Animator animation) {
                removeSelf();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (x == null) return;
        final float t = progress;          // 0..1
        final float gravity = 1.4f;        // pull-down factor over the run
        final int alpha = (int) (255 * (1f - Math.max(0f, (t - 0.7f) / 0.3f))); // fade last 30%
        for (int i = 0; i < count; i++) {
            final float px = x[i] + vx[i] * t;
            final float py = y[i] + vy[i] * t + gravity * t * t * getHeight() * 0.5f;
            canvas.save();
            canvas.rotate(rot[i] + vrot[i] * t, px, py);
            paint.setColor((colors[i] & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24));
            canvas.drawRect(px - size[i] / 2, py - size[i] / 2, px + size[i] / 2, py + size[i] / 2, paint);
            canvas.restore();
        }
    }

    private void removeSelf() {
        final ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) parent.removeView(this);
    }
}
