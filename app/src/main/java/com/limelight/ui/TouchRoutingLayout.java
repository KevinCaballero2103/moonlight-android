package com.limelight.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.R;

/** Keeps Android's native pointer splitting/capture while punching holes in OSC controls. */
public class TouchRoutingLayout extends FrameLayout {
    public interface Control {
        boolean isEditing();
    }

    public interface PassthroughArea extends Control {
        boolean isPassthroughEnabled();
    }

    private final Matrix inverse = new Matrix();
    private final float[] point = new float[2];
    private View promotedTarget;
    private int promotedIndex = -1;
    private MotionEvent lastPassthroughEvent;
    private boolean hasPassthroughGesture;
    private boolean ignoreUntilNextDown;

    public TouchRoutingLayout(Context context) {
        this(context, null);
    }

    public TouchRoutingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
        setMotionEventSplittingEnabled(true);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int position) {
        if (promotedIndex < 0 || promotedIndex >= childCount) {
            return position;
        }
        // Used only during the new pointer's hit test, never during drawing.
        if (position == childCount - 1) {
            return promotedIndex;
        }
        return position >= promotedIndex ? position + 1 : position;
    }

    /** Let the normal stream listener accept a reserved DOWN even if input is suppressed. */
    public boolean isPassthroughTarget(View view) {
        return view == promotedTarget;
    }

    private boolean contains(View child, float x, float y) {
        if (child == null || child.getVisibility() != VISIBLE) {
            return false;
        }
        point[0] = x + getScrollX() - child.getLeft();
        point[1] = y + getScrollY() - child.getTop();
        if (!child.getMatrix().isIdentity()) {
            if (!child.getMatrix().invert(inverse)) {
                return false;
            }
            inverse.mapPoints(point);
        }
        return point[0] >= 0 && point[1] >= 0
                && point[0] < child.getWidth() && point[1] < child.getHeight();
    }

    private View passthroughTarget(float x, float y) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE && child instanceof Control
                    && ((Control) child).isEditing()) {
                return null;
            }
        }
        boolean inArea = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof PassthroughArea
                    && ((PassthroughArea) child).isPassthroughEnabled()
                    && contains(child, x, y)) {
                inArea = true;
                break;
            }
        }
        if (!inArea) {
            return null;
        }
        View stream = findViewById(R.id.surfaceView);
        View target = contains(stream, x, y) ? stream : findViewById(R.id.backgroundTouchView);
        return target != null && target.getParent() == this && contains(target, x, y)
                ? target : null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            ignoreUntilNextDown = false;
            clearLastEvent();
            hasPassthroughGesture = false;
        } else if (ignoreUntilNextDown) {
            return true;
        }
        View target = null;
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex();
            target = passthroughTarget(event.getX(index), event.getY(index));
        }
        if (target != null) {
            hasPassthroughGesture = true;
        }
        if (hasPassthroughGesture) {
            clearLastEvent();
            lastPassthroughEvent = MotionEvent.obtainNoHistory(event);
        }

        float previousTranslationZ = 0;
        if (target != null) {
            promotedTarget = target;
            promotedIndex = indexOfChild(target);
            previousTranslationZ = target.getTranslationZ();
            float maximumZ = target.getZ();
            for (int i = 0; i < getChildCount(); i++) {
                maximumZ = Math.max(maximumZ, getChildAt(i).getZ());
            }
            // Native hit testing sorts by Z before child order. Restore before any drawing.
            if (maximumZ > target.getZ()) {
                target.setTranslationZ(maximumZ - target.getElevation());
            }
        }
        try {
            return super.dispatchTouchEvent(event);
        } finally {
            if (target != null) {
                target.setTranslationZ(previousTranslationZ);
            }
            promotedTarget = null;
            promotedIndex = -1;
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                hasPassthroughGesture = false;
                clearLastEvent();
                if (action == MotionEvent.ACTION_CANCEL) {
                    ignoreUntilNextDown = true;
                }
            }
        }
    }

    /** Layout/focus changes end native captures as well as app-owned keys. */
    public void cancelPassthroughTouches() {
        if (!hasPassthroughGesture || lastPassthroughEvent == null) {
            return;
        }
        MotionEvent cancel = MotionEvent.obtainNoHistory(lastPassthroughEvent);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        hasPassthroughGesture = false;
        ignoreUntilNextDown = true;
        clearLastEvent();
        try {
            super.dispatchTouchEvent(cancel);
        } finally {
            cancel.recycle();
        }
    }

    private void clearLastEvent() {
        if (lastPassthroughEvent != null) {
            lastPassthroughEvent.recycle();
            lastPassthroughEvent = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelPassthroughTouches();
        clearLastEvent();
        super.onDetachedFromWindow();
    }
}
