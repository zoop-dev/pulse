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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Pulse: a pull-to-sync layout where the whole content follows your finger down and a thick
 * progress bar peeks in at the top, then fills up while syncing. No spinner.
 */
public class PulseRefreshLayout extends SwipeRefreshLayout {
    private float downY;
    private boolean dragging;
    private ProgressBar syncBar;
    private ObjectAnimator fillAnim;

    public PulseRefreshLayout(final Context context) {
        super(context);
    }

    public PulseRefreshLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSyncBar(final ProgressBar bar) {
        this.syncBar = bar;
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    /** The content child (everything except SwipeRefreshLayout's own progress circle). */
    private View content() {
        for (int i = 0; i < getChildCount(); i++) {
            final View c = getChildAt(i);
            if (!c.getClass().getSimpleName().contains("CircleImageView")) {
                return c;
            }
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downY = ev.getY();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if (!canChildScrollUp() && !isRefreshing()) {
                    final float dy = ev.getY() - downY;
                    if (dy > 0) {
                        dragging = true;
                        final float max = 130f * density();
                        final float t = Math.min(dy * 0.5f, max);
                        final View c = content();
                        if (c != null) {
                            c.setTranslationY(t);
                        }
                        if (syncBar != null) {
                            final float p = Math.min(1f, dy / (max * 2f));
                            syncBar.setVisibility(VISIBLE);
                            syncBar.setAlpha(p);
                            syncBar.setScaleY(0.4f + 0.6f * p);
                            syncBar.setProgress((int) (p * 18));
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    if (!isRefreshing()) {
                        resetVisual();
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    /** Called when a sync actually starts: rest the content down and fill the bar. */
    public void onSyncStarted() {
        final View c = content();
        if (c != null) {
            c.animate().translationY(44f * density()).setDuration(160).start();
        }
        if (syncBar != null) {
            syncBar.setVisibility(VISIBLE);
            syncBar.setAlpha(1f);
            syncBar.setScaleY(1f);
            if (fillAnim != null) {
                fillAnim.cancel();
            }
            fillAnim = ObjectAnimator.ofInt(syncBar, "progress", Math.max(syncBar.getProgress(), 10), 90);
            fillAnim.setDuration(4500);
            fillAnim.setInterpolator(new DecelerateInterpolator());
            fillAnim.start();
        }
    }

    /** Called when the sync finishes: snap the bar to full, then clear everything. */
    public void onSyncFinished() {
        if (fillAnim != null) {
            fillAnim.cancel();
            fillAnim = null;
        }
        if (syncBar != null) {
            final ObjectAnimator done = ObjectAnimator.ofInt(syncBar, "progress", syncBar.getProgress(), 100);
            done.setDuration(220);
            done.start();
            syncBar.animate().alpha(0f).setStartDelay(220).setDuration(180)
                    .withEndAction(() -> {
                        syncBar.setVisibility(GONE);
                        syncBar.setProgress(0);
                        syncBar.setAlpha(1f);
                    }).start();
        }
        final View c = content();
        if (c != null) {
            c.animate().translationY(0).setDuration(240).start();
        }
    }

    private void resetVisual() {
        final View c = content();
        if (c != null) {
            c.animate().translationY(0).setDuration(220).start();
        }
        if (syncBar != null) {
            syncBar.animate().alpha(0f).setDuration(160)
                    .withEndAction(() -> {
                        syncBar.setVisibility(GONE);
                        syncBar.setProgress(0);
                        syncBar.setAlpha(1f);
                    }).start();
        }
    }
}
