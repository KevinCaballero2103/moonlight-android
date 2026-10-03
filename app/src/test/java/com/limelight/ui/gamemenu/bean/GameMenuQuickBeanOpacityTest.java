package com.limelight.ui.gamemenu.bean;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.*;

public class GameMenuQuickBeanOpacityTest {
    private final Gson gson = new Gson();

    @Test
    public void oldLayoutInheritsGlobalOpacityWithoutAddingAField() {
        GameMenuQuickBean bean = gson.fromJson(
                "{\"name\":\"Camera\",\"btnType\":2,\"code\":13,\"width\":400,\"height\":200}",
                GameMenuQuickBean.class);
        assertNull(bean.getOpacity());
        JsonObject saved = JsonParser.parseString(gson.toJson(bean)).getAsJsonObject();
        assertFalse(saved.has("opacity"));
        assertEquals(13, saved.get("code").getAsInt());
        assertEquals(400, saved.get("width").getAsInt());
    }

    @Test
    public void everyCustomControlTypePreservesZeroIntermediateAndFullOpacity() {
        for (int type = 1; type <= 5; type++) {
            for (int opacity : new int[] {0, 10, 20, 100}) {
                GameMenuQuickBean bean = new GameMenuQuickBean();
                bean.setBtnType(type);
                bean.setOpacity(opacity);
                GameMenuQuickBean imported = gson.fromJson(gson.toJson(bean), GameMenuQuickBean.class);
                assertEquals(Integer.valueOf(opacity), imported.getOpacity());
                assertEquals(type, imported.getBtnType());
            }
        }
    }

    @Test
    public void mixedLayoutKeepsIndependentOverridesAfterExportAndImport() {
        GameMenuQuickBean[] layout = gson.fromJson(
                "[{\"btnType\":2,\"opacity\":0},{\"btnType\":3,\"opacity\":10},"
                        + "{\"btnType\":4,\"opacity\":20},{\"btnType\":1}]",
                GameMenuQuickBean[].class);
        GameMenuQuickBean[] imported = gson.fromJson(gson.toJson(layout), GameMenuQuickBean[].class);
        assertEquals(Integer.valueOf(0), imported[0].getOpacity());
        assertEquals(Integer.valueOf(10), imported[1].getOpacity());
        assertEquals(Integer.valueOf(20), imported[2].getOpacity());
        assertNull(imported[3].getOpacity());
    }

    @Test
    public void resettingToGlobalRemovesOverrideFromSavedLayout() {
        GameMenuQuickBean bean = new GameMenuQuickBean();
        bean.setOpacity(0);
        bean.setOpacity(null);
        assertNull(bean.getOpacity());
        assertFalse(JsonParser.parseString(gson.toJson(bean)).getAsJsonObject().has("opacity"));
    }

    @Test
    public void explicitNullAlsoInheritsGlobalOpacity() {
        assertNull(gson.fromJson("{\"opacity\":null}", GameMenuQuickBean.class).getOpacity());
    }

    @Test
    public void outOfRangeImportedValuesAreClampedEvenWhenGsonBypassesSetter() {
        assertEquals(Integer.valueOf(0),
                gson.fromJson("{\"opacity\":-1}", GameMenuQuickBean.class).getOpacity());
        assertEquals(Integer.valueOf(100),
                gson.fromJson("{\"opacity\":101}", GameMenuQuickBean.class).getOpacity());
        GameMenuQuickBean bean = new GameMenuQuickBean();
        bean.setOpacity(-99);
        assertEquals(Integer.valueOf(0), bean.getOpacity());
        bean.setOpacity(999);
        assertEquals(Integer.valueOf(100), bean.getOpacity());
    }
}
