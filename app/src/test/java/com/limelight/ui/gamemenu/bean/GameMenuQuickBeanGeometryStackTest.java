package com.limelight.ui.gamemenu.bean;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameMenuQuickBeanGeometryStackTest {
    private final Gson gson = new Gson();
    private GameMenuQuickBean rectangle(int type) {
        GameMenuQuickBean b = new GameMenuQuickBean("control", 16, "control", type, false);
        b.setShapeType(1); b.setWidth(400); b.setHeight(200); b.setmLeft(50); b.setmTop(100);
        return b;
    }
    @Test public void circleIsSquareAndCenteredForAllEditableTypes() {
        for (int type : new int[]{1,2,4,6}) {
            GameMenuQuickBean b=rectangle(type); b.setEditorShapeType(0);
            assertEquals(200,b.getWidth()); assertEquals(200,b.getHeight());
            assertEquals(150,b.getmLeft()); assertEquals(100,b.getmTop());
            b.setCircleDiameter(300);
            assertEquals(100,b.getmLeft()); assertEquals(50,b.getmTop());
            b.setEditorShapeType(1);
            assertEquals(400,b.getWidth()); assertEquals(200,b.getHeight());
            assertEquals(50,b.getmLeft()); assertEquals(100,b.getmTop());
        }
    }
    @Test public void bothSizesAndLayerSurviveExportAndImport() {
        GameMenuQuickBean b=rectangle(2); b.setEditorShapeType(0);b.setCircleDiameter(120);b.setStackLevel(1);b.setOpacity(0);
        b=gson.fromJson(gson.toJson(b),GameMenuQuickBean.class);b.normalizeCircularBounds();
        assertEquals(120,b.getWidth()); assertEquals(120,b.getHeight());assertEquals(1,b.getStackLevel());
        assertEquals(Integer.valueOf(0),b.getOpacity());
        b.setEditorShapeType(1);assertEquals(400,b.getWidth());assertEquals(200,b.getHeight());
        b.setWidth(500);b.setHeight(250);b.setEditorShapeType(0);
        assertEquals(120,b.getCircleDiameter());b.setEditorShapeType(1);
        assertEquals(500,b.getWidth());assertEquals(250,b.getHeight());
    }
    @Test public void importedOldCircleKeepsVisibleAreaAndRectangularBackup() {
        GameMenuQuickBean b=gson.fromJson("{\"btnType\":2,\"touchShape\":0,\"width\":400,\"height\":200,\"mLeft\":50,\"mTop\":100}",GameMenuQuickBean.class);
        b.normalizeCircularBounds();assertEquals(200,b.getWidth());assertEquals(150,b.getmLeft());
        b.normalizeCircularBounds();assertEquals(150,b.getmLeft());
        b.setEditorShapeType(1);assertEquals(400,b.getWidth());assertEquals(50,b.getmLeft());
    }
    @Test public void oddDiameterDoesNotAccumulateRoundingDriftAcrossReloads() {
        GameMenuQuickBean b=rectangle(2);
        for(int i=0;i<12;i++) {
            b.setEditorShapeType(0);b.setCircleDiameter(75);
            // Selecting the same control must not discard its fractional center.
            b.setmLeft(b.getmLeft());b.setmTop(b.getmTop());
            b=gson.fromJson(gson.toJson(b),GameMenuQuickBean.class);b.normalizeCircularBounds();
            b.setEditorShapeType(1);assertEquals(50,b.getmLeft());assertEquals(100,b.getmTop());
        }
        b.setEditorShapeType(0);b.setmLeft(b.getmLeft()+10);b.setmTop(b.getmTop()+20);
        float cx=b.getmLeft()+b.getWidth()/2f,cy=b.getmTop()+b.getHeight()/2f;
        b.setEditorShapeType(1);assertEquals(cx,b.getmLeft()+b.getWidth()/2f,0.5f);assertEquals(cy,b.getmTop()+b.getHeight()/2f,0.5f);
    }
    @Test public void oldRectanglesAndLayersStayUnchangedUntilEdited() {
        GameMenuQuickBean b=gson.fromJson("{\"btnType\":2,\"width\":417,\"height\":193}",GameMenuQuickBean.class);
        b.normalizeCircularBounds();assertEquals(417,b.getWidth());assertEquals(193,b.getHeight());assertEquals(2,b.getStackLevel());
        assertEquals(2,gson.fromJson(gson.toJson(b),GameMenuQuickBean.class).getStackLevel());
    }
    @Test public void invalidSizesAndLevelsAreBounded() {
        GameMenuQuickBean b=rectangle(6);b.setEditorShapeType(0);b.setCircleDiameter(0);
        assertEquals(1,b.getWidth());assertEquals(1,b.getHeight());b.setStackLevel(-1);assertEquals(1,b.getStackLevel());
        b.setStackLevel(1000);assertEquals(99,b.getStackLevel());
        assertEquals(1,gson.fromJson("{\"stackLevel\":-300}",GameMenuQuickBean.class).getStackLevel());
    }
    @Test public void sticksAndDpadsAreNotConvertedByShapeSettings() {
        for(int type:new int[]{3,5}) {GameMenuQuickBean b=rectangle(type);b.setEditorShapeType(0);b.normalizeCircularBounds();assertEquals(400,b.getWidth());assertEquals(200,b.getHeight());}
    }
}
