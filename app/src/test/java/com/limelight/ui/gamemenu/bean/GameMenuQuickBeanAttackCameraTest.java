package com.limelight.ui.gamemenu.bean;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameMenuQuickBeanAttackCameraTest {
    @Test public void newModeRoundTripsAlongsideOldControlsWithOpacityAndGeometry() {
        Gson gson = new Gson();
        GameMenuQuickBean[] layout = gson.fromJson(
                "[{\"name\":\"攻击＋视角\",\"btnType\":2,\"code\":14,\"opacity\":0,"
                        + "\"width\":180,\"height\":100,\"mLeft\":920,\"mTop\":430,\"shapeType\":1},"
                        + "{\"name\":\"Camera\",\"btnType\":2,\"code\":13},"
                        + "{\"name\":\"Old left touchpad\",\"btnType\":2,\"code\":11},"
                        + "{\"name\":\"Alt+1\",\"btnType\":4,\"codes\":\"57,8\"}]",
                GameMenuQuickBean[].class);
        GameMenuQuickBean[] imported = gson.fromJson(gson.toJson(layout), GameMenuQuickBean[].class);
        GameMenuQuickBean aim = imported[0];
        assertEquals(2, aim.getBtnType());
        assertEquals(14, aim.getCode());
        assertEquals(Integer.valueOf(0), aim.getOpacity());
        assertEquals(180, aim.getWidth());
        assertEquals(100, aim.getHeight());
        assertEquals(920, aim.getmLeft());
        assertEquals(430, aim.getmTop());
        assertEquals(1, aim.getShapeType());
        assertFalse(aim.isSwitchMode());
        assertEquals(13, imported[1].getCode());
        assertNull(imported[1].getOpacity());
        assertEquals(11, imported[2].getCode());
        assertEquals("57,8", imported[3].getCodes());
    }
}
