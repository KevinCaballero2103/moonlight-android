package com.limelight.ui.gamemenu.bean;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameMenuQuickBeanPassthroughTest {
    @Test public void regionRoundTripsWithOldControlsAndInvisibleGeometry() {
        Gson gson = new Gson();
        GameMenuQuickBean[] layout = gson.fromJson(
                "[{\"name\":\"Zona de toque directo\",\"btnType\":6,\"opacity\":0,"
                        + "\"width\":13,\"height\":7,\"mLeft\":920,\"mTop\":430,\"shapeType\":1},"
                        + "{\"btnType\":2,\"code\":14,\"opacity\":35},"
                        + "{\"btnType\":2,\"code\":13},"
                        + "{\"btnType\":4,\"codes\":\"57,8\"}]", GameMenuQuickBean[].class);
        GameMenuQuickBean[] imported = gson.fromJson(gson.toJson(layout), GameMenuQuickBean[].class);
        assertEquals(GameMenuQuickBean.TYPE_TOUCH_PASSTHROUGH, imported[0].getBtnType());
        assertEquals(Integer.valueOf(0), imported[0].getOpacity());
        assertEquals(13, imported[0].getWidth());
        assertEquals(7, imported[0].getHeight());
        assertEquals(920, imported[0].getmLeft());
        assertEquals(430, imported[0].getmTop());
        assertEquals(1, imported[0].getShapeType());
        assertEquals(14, imported[1].getCode());
        assertEquals(Integer.valueOf(35), imported[1].getOpacity());
        assertEquals(13, imported[2].getCode());
        assertNull(imported[2].getOpacity());
        assertEquals("57,8", imported[3].getCodes());
    }
}
