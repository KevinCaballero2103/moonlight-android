package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.limelight.R;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.utils.UiHelper;

/** An editable region, not a mouse button: the root owns its routing semantics. */
public final class KeyBoardPassthroughRegion extends keyBoardVirtualControllerElement
        implements TouchRoutingLayout.PassthroughArea {
    private final Paint regionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    public KeyBoardPassthroughRegion(KeyBoardController controller, Context context, String id) {
        super(controller, context, id);
        shapeType = 1;
    }

    @Override
    public boolean isPassthroughEnabled() {
        return enabled && isNomal() && !virtualController.isInputSuppressed();
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean containsPassthroughPoint(float x, float y) {
        return TouchControlShape.contains(shapeType, getWidth(), getHeight(), x, y);
    }

    private void drawRegion(Canvas canvas, float inset) {
        if (shapeType == 0) {
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f,
                    Math.max(0, Math.min(getWidth(), getHeight()) / 2f - inset), regionPaint);
        } else {
            bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRect(bounds, regionPaint);
        }
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        // Explicit per-control opacity is applied by the shared drawing layer.
        // Editing must remain visible even if the play opacity is zero.
        int alpha = isEditing() ? 255 : getDrawingOpacity() * 255 / 100;
        regionPaint.setColor(Color.argb(alpha / 4, 0, 210, 230));
        regionPaint.setStyle(Paint.Style.FILL);
        drawRegion(canvas, 0);
        float stroke = UiHelper.dpToPx(getContext(), 2);
        regionPaint.setColor(Color.argb(alpha, 0, 210, 230));
        regionPaint.setStyle(Paint.Style.STROKE);
        regionPaint.setStrokeWidth(stroke);
        drawRegion(canvas, stroke / 2);
        regionPaint.setStyle(Paint.Style.FILL);
        regionPaint.setTextAlign(Paint.Align.CENTER);
        regionPaint.setTextSize(UiHelper.dpToPx(getContext(), 12));
        canvas.drawText(getResources().getString(R.string.control_touch_passthrough),
                getWidth() / 2f, getHeight() / 2f
                        - (regionPaint.ascent() + regionPaint.descent()) / 2f, regionPaint);
    }
}
