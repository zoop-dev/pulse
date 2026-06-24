package nodomain.freeyourgadget.gadgetbridge.activities.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.viewpager2.widget.ViewPager2;

/**
 * Lets a horizontally-scrollable child (here, the Today carousel ViewPager2) live inside the
 * outer tab ViewPager2 without the two fighting over swipes. Ported from Google's official
 * ViewPager2 nested-scrolling sample: the host claims horizontal drags while the child can still
 * scroll that way, and hands them back to the parent at the edges.
 */
public class NestedScrollableHost extends FrameLayout {
    private int touchSlop = 0;
    private float initialX = 0f;
    private float initialY = 0f;

    public NestedScrollableHost(final Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public NestedScrollableHost(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private ViewPager2 parentViewPager() {
        ViewParent v = getParent();
        while (v != null && !(v instanceof ViewPager2) && v instanceof View) {
            v = v.getParent();
        }
        return v instanceof ViewPager2 ? (ViewPager2) v : null;
    }

    private View child() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    private boolean canChildScroll(final int orientation, final float delta) {
        final int direction = -(int) Math.signum(delta);
        final View child = child();
        if (child == null) return false;
        if (orientation == 0) {
            return child.canScrollHorizontally(direction);
        }
        return child.canScrollVertically(direction);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent e) {
        handleInterceptTouchEvent(e);
        return super.onInterceptTouchEvent(e);
    }

    private void handleInterceptTouchEvent(final MotionEvent e) {
        final ViewPager2 vp = parentViewPager();
        if (vp == null) return;
        final int orientation = vp.getOrientation();

        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f)) {
            return;
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            final float dx = e.getX() - initialX;
            final float dy = e.getY() - initialY;
            final boolean isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL;
            final float scaledDx = Math.abs(dx) * (isVpHorizontal ? 0.5f : 1f);
            final float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : 0.5f);

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Swipe is perpendicular to our pager — let the parent handle it.
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    // Swipe is along our pager — keep it if the child can still scroll that way.
                    getParent().requestDisallowInterceptTouchEvent(canChildScroll(orientation, isVpHorizontal ? dx : dy));
                }
            }
        }
    }
}
