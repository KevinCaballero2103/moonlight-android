/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.ui.TouchRoutingLayout;
import com.limelight.Game;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.gamemenu.GameKeyboardUpdateFragment;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.utils.UiHelper;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class keyBoardVirtualControllerElement extends View implements TouchRoutingLayout.Control {
    protected static boolean _PRINT_DEBUG_INFORMATION = false;

    protected KeyBoardController virtualController;
    protected final String elementId;

    private final Paint paint = new Paint();
    private Integer opacityOverride;
    private int globalOpacity;
    private boolean inputCancelled;
    private int touchPointerId = -1;

    public void setOpacityOverride(Integer opacity) {
        opacityOverride = opacity == null ? null : Math.max(0, Math.min(100, opacity));
        setOpacity(globalOpacity);
    }

    // Explicit opacity is applied once to the complete drawing, including icons,
    // pressed highlights and switch-mode borders. Keep legacy rendering unchanged.
    protected int getDrawingOpacity() {
        return opacityOverride == null ? globalOpacity : 100;
    }

    private int normalColor = 0xF0888888;
    protected int textColor = 0xFFFFFFFF;
    protected int pressedColor = 0x805C5CAD;
    protected int strokeColor = 0xB3ADADAD;
    protected int strokeSwicthModeColor = 0xB3FFC107;

    private int configMoveColor = 0xF0FF0000;
    private int configResizeColor = 0xF0FF00FF;
    private int configSelectedColor = Color.parseColor("#FFABABFF");

    private int configDisabledColor = 0xF0AAAAAA;

    protected int startSize_x;
    protected int startSize_y;

    float position_pressed_x = 0;
    float position_pressed_y = 0;

    public boolean enabled = true;
    protected enum Mode {
        Normal,
        Resize,
        Move
    }

    protected Mode currentMode = Mode.Normal;

    protected keyBoardVirtualControllerElement(KeyBoardController controller, Context context, String elementId) {
        super(context);

        this.virtualController = controller;
        this.elementId = elementId;
        this.normalColor= PreferenceConfiguration.readPreferences(context).virtualkeyViewNormalColor;
    }

    protected void moveElement(int pressed_x, int pressed_y, int x, int y) {
        int newPos_x = (int) getX() + x - pressed_x;
        int newPos_y = (int) getY() + y - pressed_y;

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

        layoutParams.leftMargin = newPos_x > 0 ? newPos_x : 0;
        layoutParams.topMargin = newPos_y > 0 ? newPos_y : 0;
        layoutParams.rightMargin = 0;
        layoutParams.bottomMargin = 0;

        requestLayout();
    }

    protected void resizeElement(int pressed_x, int pressed_y, int width, int height) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

        int newHeight = height + (startSize_y - pressed_y);
        int newWidth = width + (startSize_x - pressed_x);

        layoutParams.height = Math.max(1, newHeight);
        layoutParams.width = Math.max(1, newWidth);

        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean applyOverride = opacityOverride != null && isNomal();
        if (applyOverride && opacityOverride == 0) {
            // Only skip painting. Visibility and touch dispatch must stay enabled.
            return;
        }
        int layer = -1;
        if (applyOverride && opacityOverride < 100) {
            layer = canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(),
                    opacityOverride * 255 / 100);
        }
        onElementDraw(canvas);
        if (layer != -1) {
            canvas.restoreToCount(layer);
        }

        if (currentMode != Mode.Normal) {
            paint.setColor(configSelectedColor);
            paint.setStrokeWidth(getDefaultStrokeWidth());
            paint.setStyle(Paint.Style.STROKE);

            canvas.drawRect(paint.getStrokeWidth(), paint.getStrokeWidth(),
                    getWidth()-paint.getStrokeWidth(), getHeight()-paint.getStrokeWidth(),
                    paint);
        }

        super.onDraw(canvas);
    }


    protected void actionEnableMove() {
        currentMode = Mode.Move;
    }

    protected void actionEnableResize() {
        currentMode = Mode.Resize;
    }

    public void cancelInput() {
        inputCancelled = true;
        touchPointerId = -1;
        setPressed(false);
        invalidate();
    }

    protected void actionCancel() {
        currentMode = Mode.Normal;
        invalidate();
    }

    protected int getDefaultColor() {
        if(virtualController.getControllerMode() == KeyBoardController.ControllerMode.MoveButtons
                &&virtualController.getCurrentIndex().equals(getTag())){
            return configSelectedColor;
        }else{
            return normalColor;
        }
//        if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.MoveButtons)
//            return configMoveColor;
//        else if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.ResizeButtons)
//            return configResizeColor;
//        else if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.DisableEnableButtons)
//            return enabled ? configSelectedColor: configDisabledColor;
//        else
//            return normalColor;
    }

    protected int getDefaultStrokeWidth() {
//        DisplayMetrics screen = getResources().getDisplayMetrics();
//        return (int)(screen.heightPixels*0.004f);
        return UiHelper.dpToPx(getContext(),1);
    }

    @Override
    public boolean isEditing() {
        KeyBoardController.ControllerMode mode = virtualController.getControllerMode();
        return mode != KeyBoardController.ControllerMode.Active
                && mode != KeyBoardController.ControllerMode.NONE;
    }

    protected boolean isNomal(){
        return virtualController.getControllerMode() == KeyBoardController.ControllerMode.Active;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isNomal()) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (!containsTouchPoint(event.getX(), event.getY())) return false;
                if (touchPointerId != -1) cancelInput();
                inputCancelled = false;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                cancelInput();
                return true;
            }
            if (inputCancelled) return true;
            // Attack + camera already owns its pointer and consumes MOVE history.
            if (handlesSecondaryTouchEvents()) return onElementTouchEvent(event);
            return dispatchOwnedTouch(event);
        }

        // Keep the legacy secondary-touch guard for layout editing only.
        if (event.getActionIndex() != 0) return true;

        if(onItem!=null){
            onItem.click((TagInfo) this.getTag());
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                position_pressed_x = event.getX();
                position_pressed_y = event.getY();
                startSize_x = getWidth();
                startSize_y = getHeight();

                if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.MoveButtons)
                    actionEnableMove();
                else if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.ResizeButtons)
                    actionEnableResize();
                else if (virtualController.getControllerMode() == KeyBoardController.ControllerMode.DisableEnableButtons)
                    actionDisableEnableButton();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                switch (currentMode) {
                    case Move: {
                        moveElement(
                                (int) position_pressed_x,
                                (int) position_pressed_y,
                                (int) event.getX(),
                                (int) event.getY());
                        break;
                    }
                    case Resize: {
                        resizeElement(
                                (int) position_pressed_x,
                                (int) position_pressed_y,
                                (int) event.getX(),
                                (int) event.getY());
                        break;
                    }
                    case Normal: {
                        break;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                actionCancel();
                return true;
            }
            default: {
            }
        }
        return true;
    }

    protected final boolean hasOwnedTouchPointer() {
        return touchPointerId != -1;
    }

    private boolean dispatchOwnedTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            touchPointerId = event.getPointerId(event.getActionIndex());
            boolean handled = onElementTouchEvent(event);
            if (!handled) touchPointerId = -1;
            return handled;
        }
        if (touchPointerId == -1) return true;
        if (action == MotionEvent.ACTION_POINTER_DOWN) return true;
        int index = event.findPointerIndex(touchPointerId);
        if (index == -1) {
            cancelInput();
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP) {
            if (event.getPointerId(event.getActionIndex()) != touchPointerId) return true;
            action = MotionEvent.ACTION_UP;
        } else if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) {
            return true;
        }
        MotionEvent owned = OwnedPointerMotion.obtain(event, index, action);
        if (action == MotionEvent.ACTION_UP) touchPointerId = -1;
        try {
            return onElementTouchEvent(owned);
        } finally {
            if (owned != event) owned.recycle();
        }
    }

    // Only new gestures use the shape; captured drags keep their original target.
    // Editing continues to use the full bounding rectangle.
    protected boolean containsTouchPoint(float x, float y) {
        return true;
    }

    abstract protected void onElementDraw(Canvas canvas);

    // Opt-in for controls with their own pointerId handling; other active controls
    // get a normalized owner-only gesture from dispatchOwnedTouch().
    protected boolean handlesSecondaryTouchEvents() {
        return false;
    }

    abstract public boolean onElementTouchEvent(MotionEvent event);

    protected static final void _DBG(String text) {
        if (_PRINT_DEBUG_INFORMATION) {
//            System.out.println(text);
        }
    }

    public void setColors(int normalColor, int pressedColor) {
        this.normalColor = normalColor;
        this.pressedColor = pressedColor;

        invalidate();
    }


    public void setOpacity(int opacity) {
        globalOpacity = Math.max(0, Math.min(100, opacity));
        int hexOpacity = getDrawingOpacity() * 255 / 100;
        // 计算 1.5 倍透明度并确保不超过 255
        int textHexOpacity = Math.min(255, (int)(hexOpacity * 1.5f));
        this.normalColor = (hexOpacity << 24) | (normalColor & 0x00FFFFFF);
        this.textColor=(textHexOpacity << 24) | (textColor & 0x00FFFFFF);
        this.strokeColor=(hexOpacity << 24) | (textColor & 0x00FFFFFF);
//        this.pressedColor = (hexOpacity << 24) | (pressedColor & 0x00FFFFFF);
        invalidate();
    }

    protected final float getPercent(float value, float percent) {
        return value / 100 * percent;
    }

    protected final int getCorrectWidth() {
        return getWidth() > getHeight() ? getHeight() : getWidth();
    }


    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = new JSONObject();

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

        configuration.put("LEFT", layoutParams.leftMargin);
        configuration.put("TOP", layoutParams.topMargin);
        configuration.put("WIDTH", layoutParams.width);
        configuration.put("HEIGHT", layoutParams.height);
        configuration.put("ENABLED", enabled);
        return configuration;
    }

    public void loadConfiguration(JSONObject configuration) throws JSONException {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

        layoutParams.leftMargin = configuration.getInt("LEFT");
        layoutParams.topMargin = configuration.getInt("TOP");
        layoutParams.width = configuration.getInt("WIDTH");
        layoutParams.height = configuration.getInt("HEIGHT");

        enabled = configuration.getBoolean("ENABLED");

        setVisibility(enabled ? VISIBLE: GONE);
        requestLayout();
    }

    protected  void actionDisableEnableButton(){
        enabled = !enabled;
    }

    protected int shapeType;

    private int controlLevel = 2;
    @Override public int getControlLevel() { return controlLevel; }
    public void setControlLevel(int level) {
        controlLevel = Math.max(1, Math.min(99, level));
        // Z orders both drawing and Android's native pointer hit testing.
        setTranslationZ(controlLevel);
    }
    @Override public boolean containsControlPoint(float x, float y) { return containsTouchPoint(x, y); }

    public void setShapeType(int shapeType) {
        this.shapeType = shapeType;
    }

    private onItem onItem;

    public interface onItem{
        void click(TagInfo tag);
    }

    public void setOnClick(onItem onItem) {
        this.onItem = onItem;
    }

}
