package com.limelight.ui.gamemenu.bean;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameMenuQuickBeanShapeTest {
    private final Gson gson = new Gson();

    @Test public void everyOldTouchpadAndRegionStaysRectangular() {
        for (int type : new int[]{2, 6}) {
            for (String oldShape : new String[]{"", ",\"shapeType\":0", ",\"shapeType\":1"}) {
                GameMenuQuickBean bean = gson.fromJson("{\"btnType\":" + type + oldShape + "}",
                        GameMenuQuickBean.class);
                assertEquals(1, bean.getShapeType());
                assertEquals(1, gson.fromJson(gson.toJson(bean), GameMenuQuickBean.class).getShapeType());
            }
        }
    }

    @Test public void chosenCircleRoundTripsWithOpacityAndCustomDimensions() {
        for (int type : new int[]{2, 6}) {
            GameMenuQuickBean bean = gson.fromJson("{\"btnType\":" + type
                    + ",\"code\":14,\"width\":17,\"height\":23,\"opacity\":0}", GameMenuQuickBean.class);
            bean.setShapeType(0);
            GameMenuQuickBean loaded = gson.fromJson(gson.toJson(bean), GameMenuQuickBean.class);
            assertEquals(0, loaded.getShapeType());
            assertEquals(17, loaded.getWidth());
            assertEquals(23, loaded.getHeight());
            assertEquals(Integer.valueOf(0), loaded.getOpacity());
            loaded.setShapeType(1);
            assertEquals(1, gson.fromJson(gson.toJson(loaded), GameMenuQuickBean.class).getShapeType());
        }
    }

    @Test public void normalButtonShapesKeepTheirOriginalMeaning() {
        assertEquals(0, gson.fromJson("{\"btnType\":4}", GameMenuQuickBean.class).getShapeType());
        assertEquals(0, gson.fromJson("{\"btnType\":4,\"shapeType\":0}", GameMenuQuickBean.class).getShapeType());
        assertEquals(1, gson.fromJson("{\"btnType\":4,\"shapeType\":1}", GameMenuQuickBean.class).getShapeType());
    }
}
