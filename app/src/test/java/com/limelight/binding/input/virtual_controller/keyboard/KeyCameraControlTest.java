package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.google.gson.Gson;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34},
        shadows = KeyboardControlTouchTest.NoNativeMoonBridge.class)
public class KeyCameraControlTest {
    private Activity activity;
    private TouchRoutingLayout root;
    private RecordingController controller;
    private KeyBoardTouchPadButton aim;
    private final List<String> events = new ArrayList<>();
    private long time = 1000;

    // Use actual Views, configuration callbacks, input ownership and chord timing;
    // replace only the external Game/network sink.
    private class RecordingController extends KeyBoardController {
        final VirtualInputState state = new VirtualInputState(new VirtualInputState.Sink() {
            public void keyboard(int key, boolean down) { events.add("K" + key + (down ? "D" : "U")); }
            public void mouse(int key, boolean down) { events.add("M" + key + (down ? "D" : "U")); }
        });
        final Handler handler = new Handler(Looper.getMainLooper());
        final TimedKeyCombination combo = new TimedKeyCombination(new TimedKeyCombination.Scheduler() {
            public void postDelayed(Runnable task, long delay) { handler.postDelayed(task, delay); }
            public void remove(Runnable task) { handler.removeCallbacks(task); }
        }, state);
        RecordingController() {
            super(null, root, activity, PreferenceConfiguration.readPreferences(activity), false);
            currentMode = ControllerMode.Active;
        }
        @Override public void sendKeyEvent(Object owner, KeyEvent event) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (event.getSource() == 1) state.mouse(owner, event.getKeyCode(), down);
            else state.keyboard(owner, event.getKeyCode(), down);
        }
        @Override public void sendAssembleKey(Object owner, String keys, int action) {
            if (action == KeyEvent.ACTION_DOWN) combo.down(owner, keys);
            else combo.up(owner);
        }
        @Override public void cancelCombination(Object owner) { combo.cancel(owner); }
        @Override public void sendMouseMove(int x, int y) { events.add("move:" + x + "," + y); }
    }

    @Before public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        root = new TouchRoutingLayout(activity);
        controller = new RecordingController();
        aim = KeyBoardControllerConfigurationLoader.createKeyCameraButton("aim", "33", "E", controller, activity);
        add(aim, 600, 100, 100, 100);
    }
    private void add(keyBoardVirtualControllerElement view, int x, int y, int w, int h) {
        controller.addElement(view, x, y, w, h);
        root.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1000, 600);
    }
    private void event(int action, int index, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            props[i] = new MotionEvent.PointerProperties();
            props[i].id = ids[i];
            props[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = xy[2*i]; coords[i].y = xy[2*i+1]; coords[i].pressure = 1;
        }
        MotionEvent event = MotionEvent.obtain(1000, time += 10,
                action | index << MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                ids.length, props, coords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { assertTrue(root.dispatchTouchEvent(event)); } finally { event.recycle(); }
    }
    @Test public void tapEmitsOnlyOneImmediateEPressAndRelease() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 650, 150);
        assertEquals(List.of("K33D"), events);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 650, 150);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(List.of("K33D", "K33U"), events);
    }
    @Test public void holdMovesOutsideBoundsAndReleasesWithoutMouseClick() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 650, 150);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(List.of("K33D"), events);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 800, 250);
        assertTrue(events.get(1).startsWith("move:"));
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 800, 250);
        assertEquals("K33U", events.get(2));
        assertFalse(events.stream().anyMatch(s -> s.startsWith("M")));
    }
    @Test public void joystickCtrlAndAimHaveIndependentLifetimes() {
        KeyBoardAnalogStickButton stick = KeyBoardControllerConfigurationLoader.createKeyBoardAnalogStickButton(
                controller, "wasd", activity, new int[]{51,47,29,32}, new String[]{"W","S","A","D"});
        add(stick, 0, 200, 200, 200);
        KeyBoardDigitalButton ctrl = KeyBoardControllerConfigurationLoader.createDigitalButton(
                "ctrl", "113", 4, 1, "Ctrl", -1, false, controller, activity);
        add(ctrl, 300, 200, 80, 80);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 300);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 100, 230);
        assertTrue(events.contains("K51D"));
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7,19}, 100,230, 340,240);
        assertEquals("K113D", events.get(events.size()-1));
        event(MotionEvent.ACTION_POINTER_DOWN, 2, new int[]{7,19,23}, 100,230, 340,240, 650,150);
        assertEquals("K33D", events.get(events.size()-1));
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7,19,23}, 100,230, 340,240, 660,155);
        assertTrue(events.stream().anyMatch(s -> s.startsWith("move:")));
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7,19,23}, 100,230, 340,240, 660,155);
        assertEquals("K113U", events.get(events.size()-1));
        assertFalse(events.contains("K33U"));
        assertFalse(events.contains("K51U"));
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7,23}, 100,230, 660,155);
        assertEquals("K33U", events.get(events.size()-1));
        assertFalse(events.contains("K51U"));
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 100,230);
        assertEquals("K51U", events.get(events.size()-1));
    }
    @Test public void sameViewOwnerLiftCannotHandKeyToSecondFinger() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 650,150);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7,19}, 650,150, 660,160);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{7,19}, 650,150, 660,160);
        assertEquals(List.of("K33D", "K33U"), events);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 670,170);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 670,170);
        assertEquals(List.of("K33D", "K33U"), events);
    }
    @Test public void cancelPreservesEOnAnotherHeldButton() {
        Object other = new Object();
        controller.state.keyboard(other, 33, true);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 650,150);
        event(MotionEvent.ACTION_CANCEL, 0, new int[]{7}, 650,150);
        assertEquals(List.of("K33D"), events);
        controller.state.keyboard(other, 33, false);
        assertEquals(List.of("K33D", "K33U"), events);
    }
    @Test public void malformedImportedKeyCannotBecomeLeftMouse() {
        for (String codes : new String[]{null, "", "33,34", "garbage", "32767"}) {
            KeyBoardTouchPadButton invalid = KeyBoardControllerConfigurationLoader.createKeyCameraButton(
                    "invalid", codes, "?", controller, activity);
            add(invalid, 800,100,100,100);
            event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 850,150);
            event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 860,160);
            event(MotionEvent.ACTION_UP, 0, new int[]{7}, 860,160);
            root.removeView(invalid);
        }
        assertTrue(events.isEmpty());
    }
    @Test public void selectedKeyAndShapeSurviveJsonRoundTrip() {
        Gson gson = new Gson();
        GameMenuQuickBean bean = gson.fromJson("{\"btnType\":2,\"code\":15,\"codes\":\"34\","
                + "\"name\":\"F\",\"touchShape\":0,\"opacity\":0,\"width\":150,\"height\":150}", GameMenuQuickBean.class);
        bean = gson.fromJson(gson.toJson(bean), GameMenuQuickBean.class);
        KeyBoardTouchPadButton f = KeyBoardControllerConfigurationLoader.createKeyCameraButton(
                "f", bean.getCodes(), bean.getName(), controller, activity);
        f.setShapeType(bean.getShapeType());
        f.setOpacityOverride(bean.getOpacity());
        add(f, 800,100,bean.getWidth(),bean.getHeight());
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 875,175);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 875,175);
        assertEquals(List.of("K34D", "K34U"), events);
        assertEquals(0, bean.getShapeType());
        assertEquals(Integer.valueOf(0), bean.getOpacity());
    }
}
