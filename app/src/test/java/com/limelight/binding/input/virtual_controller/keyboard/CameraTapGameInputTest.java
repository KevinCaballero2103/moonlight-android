package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.google.gson.Gson;
import com.limelight.Game;
import com.limelight.binding.input.touch.AbsoluteTouchContext;
import com.limelight.binding.input.touch.TouchContext;
import com.limelight.ui.StreamView;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.video.VideoZoomController;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Actual OSC dispatch, controller ownership, Game coordinate mapping, and only a fake network. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34},
        shadows = KeyboardControlTouchTest.NoNativeMoonBridge.class)
public class CameraTapGameInputTest {
    private Game game, previousGame;
    private TouchRoutingLayout root;
    private StreamView stream;
    private KeyBoardController controller;
    private KeyBoardTouchPadButton pad;
    private RecordingConnection connection;
    private long time = 1000;
    private static class RecordingConnection extends NvConnection {
        final List<String> events = new ArrayList<>();
        int touchResult = MoonBridge.LI_ERR_UNSUPPORTED;
        RecordingConnection(Context c) { super(c, null, 0, "test", null, null, null); }
        @Override public void sendMousePosition(short x, short y, short w, short h) {
            events.add("position:" + x + "," + y + "/" + w + "," + h);
        }
        @Override public void sendMouseButtonDown(byte b) { events.add("down:" + b); }
        @Override public void sendMouseButtonUp(byte b) { events.add("up:" + b); }
        @Override public void sendMouseMove(short x, short y) { events.add("move:" + x + "," + y); }
        @Override public int sendTouchEvent(byte type, int id, float x, float y,
                float pressure, float major, float minor, short rotation) {
            events.add("touch:" + type + ":" + id + ":" + x + "," + y);
            return touchResult;
        }
    }
    @Before public void setup() {
        previousGame = Game.instance;
        game = Robolectric.buildActivity(Game.class).get();
        Game.instance = game;
        game.connected = true;
        game.prefConfig = PreferenceConfiguration.readPreferences(game);
        game.prefConfig.enableMultiTouchScreen = false;
        game.prefConfig.touchscreenTrackpad = false;
        root = new TouchRoutingLayout(game);
        stream = new StreamView(game);
        add(stream, 0, 0, 1000, 600);
        connection = new RecordingConnection(game);
        ReflectionHelpers.setField(game, "conn", connection);
        ReflectionHelpers.setField(game, "streamView", stream);
        ReflectionHelpers.setField(game, "rootView", root);
        controller = new KeyBoardController(null, root, game, game.prefConfig, false);
        controller.currentMode = KeyBoardController.ControllerMode.Active;
        pad = KeyBoardControllerConfigurationLoader.createDigitalTouchButton(
                "pad", KeyBoardTouchPadButton.CODE_CAMERA_TAP, 1, 1, "Camera + tap", -1, controller, game);
        controller.addElement(pad, 400, 0, 600, 600);
        layout();
    }
    @After public void cleanup() {
        controller.releaseAllVirtualInputs();
        Game.instance = previousGame;
    }
    private void add(View view, int x, int y, int w, int h) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h);
        p.leftMargin=x; p.topMargin=y; root.addView(view,p);
    }
    private void layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));
        root.layout(0,0,1000,600);
    }
    private void event(int action, int index, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i=0;i<ids.length;i++) {
            props[i]=new MotionEvent.PointerProperties(); props[i].id=ids[i];
            props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords(); coords[i].x=xy[i*2]; coords[i].y=xy[i*2+1];
            coords[i].pressure=1;
        }
        MotionEvent e=MotionEvent.obtain(1000,time+=10,action|index<<MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                ids.length,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try { assertTrue(root.dispatchTouchEvent(e)); } finally { e.recycle(); }
    }
    private void tap(float x,float y) {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},x,y);
        event(MotionEvent.ACTION_UP,0,new int[]{7},x,y);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
    }
    @Test public void shortTapPositionsThenClicksAndReleases() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        assertTrue(connection.events.isEmpty());
        event(MotionEvent.ACTION_UP,0,new int[]{7},720,120);
        assertEquals(List.of("position:720,120/1000,600","down:1"),connection.events);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        assertEquals("up:1",connection.events.get(2));
    }
    @Test public void dragOutAndBackMovesCameraWithoutClick() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_MOVE,0,new int[]{7},800,120);
        event(MotionEvent.ACTION_MOVE,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_UP,0,new int[]{7},720,120);
        assertEquals(2,connection.events.size());
        assertTrue(connection.events.stream().allMatch(e->e.startsWith("move:")));
    }
    @Test public void longHoldCancelAndCancelledPointerUpCannotClick() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        time+=500;
        event(MotionEvent.ACTION_UP,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_CANCEL,0,new int[]{7},720,120);
        assertTrue(connection.events.isEmpty());
    }
    @Test public void samePadOwnerLiftClicksOnceThenIgnoresRemainingFinger() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,19},720,120,750,140);
        event(MotionEvent.ACTION_POINTER_UP,0,new int[]{7,19},720,120,750,140);
        event(MotionEvent.ACTION_MOVE,0,new int[]{19},850,200);
        event(MotionEvent.ACTION_UP,0,new int[]{19},850,200);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        assertEquals(List.of("position:720,120/1000,600","down:1","up:1"),connection.events);
    }
    @Test public void secondaryFingerLiftCannotClickOwner() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,19},720,120,750,140);
        event(MotionEvent.ACTION_POINTER_UP,1,new int[]{7,19},720,120,750,140);
        assertTrue(connection.events.isEmpty());
        event(MotionEvent.ACTION_UP,0,new int[]{7},720,120);
        assertEquals("down:1",connection.events.get(1));
    }
    @Test public void letterboxAndViewTransformMapToVideoCoordinates() {
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)stream.getLayoutParams();
        p.width=800;p.height=500;p.leftMargin=100;p.topMargin=50;stream.setLayoutParams(p);layout();
        tap(720,120);
        assertEquals("position:620,70/800,500",connection.events.get(0));
        connection.events.clear();
        pad.setPivotX(0);pad.setScaleX(1.5f);pad.setTranslationX(20);
        // Direct API receives the source View's local coordinates.
        controller.sendScreenTap(pad,30,120);
        assertEquals("position:365,70/800,500",connection.events.get(0));
    }
    @Test public void zoomMappingAndBlackBarsAreHandled() {
        VideoZoomController zoom=new VideoZoomController(root,stream,null);
        zoom.scaleBy(2,500,300);
        ReflectionHelpers.setField(game,"videoZoomController",zoom);
        tap(700,300);
        assertEquals("position:600,300/1000,600",connection.events.get(0));
        controller.releaseAllVirtualInputs();connection.events.clear();
        ReflectionHelpers.setField(game,"videoZoomController",null);
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)stream.getLayoutParams();
        p.width=800;p.leftMargin=100;stream.setScaleX(1);stream.setScaleY(1);stream.setTranslationX(0);stream.setTranslationY(0);
        stream.setLayoutParams(p);layout();
        controller.currentMode=KeyBoardController.ControllerMode.Active;
        controller.sendScreenTap(pad,550,100); // screen x=950, outside video
        assertTrue(connection.events.isEmpty());
    }
    @Test public void nativeTouchesUseIsolatedIdAndUnsupportedHostsFallBack() {
        game.prefConfig.enableMultiTouchScreen=true;connection.touchResult=0;
        tap(720,120);
        assertEquals(2,connection.events.size());
        assertTrue(connection.events.get(0).startsWith("touch:"+MoonBridge.LI_TOUCH_EVENT_DOWN+":65536:"));
        assertTrue(connection.events.get(1).startsWith("touch:"+MoonBridge.LI_TOUCH_EVENT_UP+":65536:"));
        connection.events.clear();connection.touchResult=MoonBridge.LI_ERR_UNSUPPORTED;
        tap(750,140);
        assertEquals(List.of("position:750,140/1000,600","down:1","up:1"),connection.events.subList(1,4));
    }
    @Test public void separateDirectTouchContextIsNotCancelledByTap() {
        TouchContext[] contexts=ReflectionHelpers.getField(game,"touchContextMap");
        AbsoluteTouchContext context=new AbsoluteTouchContext(connection,0,stream);
        contexts[0]=context;
        context.touchDownEvent(100,100,1000,true);
        tap(720,120);
        assertFalse(context.isCancelled());
        context.cancelTouch();
    }
    @Test public void heldLeftButtonIsNotReleasedOrMovedByTap() {
        KeyBoardDigitalButton left=KeyBoardControllerConfigurationLoader.createDigitalButton(
                "left",1,1,1,"Left",-1,false,controller,game);
        controller.addElement(left,0,0,100,100);layout();
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},50,50);
        event(MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,19},50,50,720,120);
        event(MotionEvent.ACTION_POINTER_UP,1,new int[]{7,19},50,50,720,120);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        assertEquals(List.of("down:1"),connection.events);
        event(MotionEvent.ACTION_UP,0,new int[]{7},50,50);
        assertEquals(List.of("down:1","up:1"),connection.events);
    }
    @Test public void hidingAfterTapReleasesImmediatelyAndOldTimerCannotReleaseNewButton() {
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},720,120);
        event(MotionEvent.ACTION_UP,0,new int[]{7},720,120);
        controller.hide();
        assertEquals("up:1",connection.events.get(2));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(3,connection.events.size());
    }
    @Test public void suppressedDisconnectedOrUngrabbedInputCannotClick() {
        controller.setInputSuppressed(true);tap(720,120);assertTrue(connection.events.isEmpty());
        controller.setInputSuppressed(false);game.connected=false;tap(720,120);assertTrue(connection.events.isEmpty());
        game.connected=true;ReflectionHelpers.setField(game,"grabbedInput",false);
        tap(720,120);assertTrue(connection.events.isEmpty());
    }
    @Test public void jsonKeepsModeShapeOpacityAndImportedOldModes() {
        Gson gson=new Gson();GameMenuQuickBean[] beans=gson.fromJson(
                "[{\"btnType\":2,\"code\":16,\"touchShape\":0,\"opacity\":0},"
                +"{\"btnType\":2,\"code\":13},{\"btnType\":2,\"code\":14},{\"btnType\":2,\"code\":15,\"codes\":\"33\"}]",
                GameMenuQuickBean[].class);
        beans=gson.fromJson(gson.toJson(beans),GameMenuQuickBean[].class);
        assertEquals(16,beans[0].getCode());assertEquals(0,beans[0].getShapeType());assertEquals(Integer.valueOf(0),beans[0].getOpacity());
        assertEquals(13,beans[1].getCode());assertEquals(14,beans[2].getCode());assertEquals("33",beans[3].getCodes());
        pad.setShapeType(0);pad.setOpacityOverride(0);tap(700,300);
        assertEquals("position:700,300/1000,600",connection.events.get(0));
    }
}
