package com.limelight.ui.gamemenu.bean;

import com.google.gson.annotations.SerializedName;

/**
 * Description
 * Date: 2024-10-20
 * Time: 20:53
 */
public class GameMenuQuickBean {
    public static final int TYPE_TOUCH_PASSTHROUGH = 6;
    // Missing/null means inherit the global setting, including in older layouts.
    @SerializedName("opacity")
    private Integer opacity;

    public Integer getOpacity() {
        return opacity == null ? null : Math.max(0, Math.min(100, opacity));
    }

    public void setOpacity(Integer opacity) {
        this.opacity = opacity == null ? null : Math.max(0, Math.min(100, opacity));
    }

    private String name;
    private short[] datas;

    private int code;

    private String codes;

    private String desc;

    private String id;
    //类型 1鼠标 2触控板 3摇杆 4普通按钮 5十字键
    private int btnType;
    //宽
    private int width;
    //高
    private int height;
    //左边距
    private int mLeft;
    //上边距
    private int mTop;
    //缩放比例
    private int zoom=100;

    //缩放比例 宽度&高度 仅普通按钮
    private int zoomW=100;
    private int zoomH=100;

    //0-圆形 1方形
    private int shapeType;

    // Touchpads used to ignore shapeType, even when it was 0. A separate optional
    // field keeps every old touchpad/region rectangular until explicitly edited.
    private Integer touchShape;

    private Integer stackLevel;
    private Integer circleDiameter;
    private Integer rectangleWidth;
    private Integer rectangleHeight;

    public int getStackLevel() { return stackLevel == null ? 2 : Math.max(1, Math.min(99, stackLevel)); }
    public void setStackLevel(int level) { stackLevel = Math.max(1, Math.min(99, level)); }

    public boolean supportsShape() {
        return btnType == 1 || btnType == 2 || btnType == 4 || btnType == TYPE_TOUCH_PASSTHROUGH;
    }

    public boolean isCircular() { return supportsShape() && getShapeType() == 0; }

    public int getCircleDiameter() {
        return circleDiameter == null ? Math.max(1, Math.min(width, height)) : Math.max(1, circleDiameter);
    }

    /** Legacy circles already used the shorter side; square bounds preserve that visible circle. */
    public void normalizeCircularBounds() {
        if (!isCircular()) return;
        if (width != height && rectangleWidth == null) {
            rectangleWidth = width; rectangleHeight = height;
        }
        setCircleDiameter(getCircleDiameter());
    }

    public void setCircleDiameter(int diameter) {
        diameter = Math.max(1, diameter);
        resizeAroundCenter(diameter, diameter);
        circleDiameter = diameter;
    }

    /** Shape switching remembers each size and keeps the control's center in place. */
    public void setEditorShapeType(int shape) {
        if (!supportsShape() || shape == getShapeType()) return;
        if (shape == 0) {
            rectangleWidth = width; rectangleHeight = height;
            setShapeType(0);
            normalizeCircularBounds();
        } else {
            circleDiameter = Math.max(1, Math.min(width, height));
            setShapeType(1);
            resizeAroundCenter(rectangleWidth == null ? width : Math.max(1, rectangleWidth),
                    rectangleHeight == null ? height : Math.max(1, rectangleHeight));
        }
    }

    private void resizeAroundCenter(int w, int h) {
        mLeft += Math.round((width - w) / 2f);
        mTop += Math.round((height - h) / 2f);
        width = w; height = h;
    }

    //开关模式
    private boolean switchMode;

    //自由摇杆
    private boolean isFreeStick;

    //固定行程自由摇杆
    private boolean fixedStrokeFreeStick;

    //默认是否绘制
    private boolean isFreeeStickDrawNormal=true;

    //是否是手柄按键
    private boolean isGamePad;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GameMenuQuickBean() {
    }

    public GameMenuQuickBean(String name, short[] datas) {
        this.name = name;
        this.datas = datas;
    }

    public GameMenuQuickBean(String name, int code, String desc, int btnType, boolean switchMode) {
        this.name = name;
        this.code = code;
        this.desc = desc;
        this.btnType = btnType;
        this.switchMode = switchMode;
    }

    public GameMenuQuickBean(String name, String codes, String desc, int btnType, boolean switchMode) {
        this.name = name;
        this.codes = codes;
        this.desc = desc;
        this.btnType = btnType;
        this.switchMode = switchMode;
    }

    public void setBtnType(int btnType) {
        this.btnType = btnType;
    }

    public int getBtnType() {
        return btnType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short[] getDatas() {
        return datas;
    }

    public void setDatas(short[] datas) {
        this.datas = datas;
    }

    public String getCodes() {
        return codes;
    }

    public void setCodes(String codes) {
        this.codes = codes;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getmLeft() {
        return mLeft;
    }

    public void setmLeft(int mLeft) {
        this.mLeft = mLeft;
    }

    public int getmTop() {
        return mTop;
    }

    public void setmTop(int mTop) {
        this.mTop = mTop;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public int getShapeType() {
        if (btnType == 2 || btnType == TYPE_TOUCH_PASSTHROUGH) {
            return touchShape != null && touchShape == 0 ? 0 : 1;
        }
        return shapeType;
    }

    public GameMenuQuickBean setShapeType(int shapeType) {
        this.shapeType = shapeType;
        if (btnType == 2 || btnType == TYPE_TOUCH_PASSTHROUGH) {
            touchShape = shapeType == 0 ? 0 : 1;
        }
        return this;
    }

    public boolean isSwitchMode() {
        return switchMode;
    }

    public void setSwitchMode(boolean switchMode) {
        this.switchMode = switchMode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getZoomW() {
        return zoomW;
    }

    public void setZoomW(int zoomW) {
        this.zoomW = zoomW;
    }

    public int getZoomH() {
        return zoomH;
    }

    public void setZoomH(int zoomH) {
        this.zoomH = zoomH;
    }

    public boolean isFreeStick() {
        return isFreeStick;
    }

    public GameMenuQuickBean setFreeStick(boolean freeStick) {
        isFreeStick = freeStick;
        return this;
    }

    public boolean isGamePad() {
        return isGamePad;
    }

    public GameMenuQuickBean setGamePad(boolean gamePad) {
        isGamePad = gamePad;
        return this;
    }

    public boolean isFixedStrokeFreeStick() {
        return fixedStrokeFreeStick;
    }

    public GameMenuQuickBean setFixedStrokeFreeStick(boolean fixedStrokeFreeStick) {
        this.fixedStrokeFreeStick = fixedStrokeFreeStick;
        return this;
    }

    public boolean isFreeeStickDrawNormal() {
        return isFreeeStickDrawNormal;
    }

    public GameMenuQuickBean setFreeeStickDrawNormal(boolean freeeStickDrawNormal) {
        isFreeeStickDrawNormal = freeeStickDrawNormal;
        return this;
    }
}
