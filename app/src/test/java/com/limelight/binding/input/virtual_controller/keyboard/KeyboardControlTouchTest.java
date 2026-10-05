package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.Application;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.limelight.R;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34},
        shadows = KeyboardControlTouchTest.NoNativeMoonBridge.class)
public class KeyboardControlTouchTest {
    // Preferences reference MoonBridge's audio configurations. This fixture tests
    // real preferences, Views and input routing, not Android JNI/audio/networking.
    // Keep the production native loader intact; bypass it only in this sandbox.
    @Implements(value = MoonBridge.class, isInAndroidSdk = false)
    public static class NoNativeMoonBridge {
        @Implementation protected static void __staticInitializer__() {}
    }

    private Activity activity;
    private TouchRoutingLayout root;
    private KeyBoardController controller;
    private KeyBoardTouchPadButton camera;
    private KeyBoardTouchPadButton attack;
    private final List<String> cameraInput = new ArrayList<>();
    private final List<String> attackInput = new ArrayList<>();
    private long time;

    @Before public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        root = new TouchRoutingLayout(activity);
        controller = new KeyBoardController(null, root, activity,
                PreferenceConfiguration.readPreferences(activity), false);
        controller.currentMode = KeyBoardController.ControllerMode.Active;
        camera = pad(13, cameraInput);
        attack = pad(KeyBoardTouchPadButton.CODE_ATTACK_CAMERA, attackInput);
        add(camera, 400, 0, 600, 600);
        add(attack, 700, 100, 100, 100);
        layout();
    }

    private KeyBoardTouchPadButton pad(int code, List<String> input) {
        KeyBoardTouchPadButton view = new KeyBoardTouchPadButton(controller, "test", 0, activity);
        view.setCode(code);
        view.addDigitalButtonListener(new KeyBoardTouchPadButton.DigitalButtonListener() {
            @Override public void onClick() { input.add("down"); }
            @Override public void onLongClick() { input.add("long"); }
            @Override public void onRelease() { input.add("up"); }
            @Override public void onMove(int x, int y) { input.add("move:" + x + "," + y); }
        });
        return view;
    }

    private void add(View view, int x, int y, int width, int height) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.leftMargin = x;
        params.topMargin = y;
        root.addView(view, params);
        if (view instanceof keyBoardVirtualControllerElement) {
            controller.getElements().add((keyBoardVirtualControllerElement) view);
        }
    }

    private void layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1000, 600);
    }

    private MotionEvent motion(int action, int index, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            props[i] = new MotionEvent.PointerProperties();
            props[i].id = ids[i];
            props[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = xy[2 * i];
            coords[i].y = xy[2 * i + 1];
            coords[i].pressure = 1;
        }
        return MotionEvent.obtain(1000, time += 10, action | index << MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                ids.length, props, coords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    private void event(int action, int index, int[] ids, float... xy) {
        MotionEvent event = motion(action, index, ids, xy);
        try { assertTrue(root.dispatchTouchEvent(event)); }
        finally { event.recycle(); }
    }

    @Test public void rectangularAttackStillAcceptsCornersByDefault() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 702, 102);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 702, 102);
        assertEquals(List.of("down", "up"), attackInput);
        assertTrue(cameraInput.isEmpty());
    }

    @Test public void circularAttackCornerStartsCameraAndCenterKeepsDragCapture() {
        attack.setShapeType(0);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 702, 102);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 712, 102);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 712, 102);
        assertTrue(attackInput.isEmpty());
        assertTrue(cameraInput.stream().anyMatch(s -> s.startsWith("move:")));
        cameraInput.clear();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{19}, 750, 150);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 850, 250);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 850, 250);
        assertEquals("down", attackInput.get(0));
        assertEquals("up", attackInput.get(attackInput.size() - 1));
        assertTrue(attackInput.stream().anyMatch(s -> s.startsWith("move:")));
        assertTrue(cameraInput.isEmpty());
    }

    @Test public void circleFitsShorterSideOfNonSquareBounds() {
        KeyBoardPassthroughRegion region = new KeyBoardPassthroughRegion(controller, activity, "region");
        region.layout(0, 0, 200, 100);
        region.setShapeType(0);
        assertTrue(region.containsPassthroughPoint(100, 50));
        assertFalse(region.containsPassthroughPoint(20, 50));
        assertFalse(region.containsPassthroughPoint(150, 99));
        region.setShapeType(1);
        assertTrue(region.containsPassthroughPoint(20, 50));
    }

    @Test public void circularInvisibleRegionRoutesCenterButCornerStaysCamera() {
        attack.setVisibility(View.GONE);
        List<Integer> normalInput = new ArrayList<>();
        View background = new View(activity) {
            @Override public boolean onTouchEvent(MotionEvent event) {
                normalInput.add(event.getActionMasked());
                return true;
            }
        };
        background.setId(R.id.backgroundTouchView);
        root.addView(background, 0, new FrameLayout.LayoutParams(1000, 600));
        KeyBoardPassthroughRegion region = new KeyBoardPassthroughRegion(controller, activity, "region");
        region.setShapeType(0);
        region.setOpacityOverride(0);
        add(region, 700, 100, 100, 100);
        layout();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 702, 102);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 702, 102);
        assertTrue(normalInput.isEmpty());
        cameraInput.clear();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{19}, 750, 150);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 850, 250);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 850, 250);
        assertEquals(List.of(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), normalInput);
        assertTrue(cameraInput.isEmpty());
    }

    @Test public void editingCircularAttackStillAcceptsFullRectangleWithoutInput() {
        attack.setShapeType(0);
        controller.currentMode = KeyBoardController.ControllerMode.MoveButtons;
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 702, 102);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 702, 102);
        assertTrue(attackInput.isEmpty());
        assertTrue(cameraInput.isEmpty());
    }

    private KeyBoardDigitalButton digital(int x, int y, List<String> input) {
        KeyBoardDigitalButton button = new KeyBoardDigitalButton(controller, "key", 0, activity);
        button.addDigitalButtonListener(new KeyBoardDigitalButton.DigitalButtonListener() {
            @Override public void onClick() { input.add("down"); }
            @Override public void onLongClick() { input.add("long"); }
            @Override public void onRelease() { input.add("up"); }
        });
        add(button, x, y, 80, 80);
        layout();
        return button;
    }

    @Test public void cameraOwnerLiftStopsMovementEvenWhenAnotherFingerStaysInside() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 500, 200);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 500, 200, 520, 220);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 19}, 510, 200, 590, 280);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{7, 19}, 510, 200, 590, 280);
        int releasedCount = cameraInput.size();
        assertEquals("up", cameraInput.get(releasedCount - 1));
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 650, 300);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 650, 300);
        assertEquals(releasedCount, cameraInput.size());
        event(MotionEvent.ACTION_DOWN, 0, new int[]{23}, 500, 200);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{23}, 510, 200);
        assertTrue(cameraInput.size() > releasedCount);
        event(MotionEvent.ACTION_UP, 0, new int[]{23}, 510, 200);
    }

    @Test public void secondaryLiftDoesNotReleaseDigitalButtonOwner() {
        List<String> input = new ArrayList<>();
        digital(500, 200, input);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 520, 220, 540, 240);
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 19}, 520, 220, 540, 240);
        assertEquals(List.of("down"), input);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 520, 220);
        assertEquals(List.of("down", "up"), input);
    }

    @Test public void digitalOwnerLiftReleasesBeforeSecondaryFingerLifts() {
        List<String> input = new ArrayList<>();
        digital(500, 200, input);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 520, 220, 540, 240);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{7, 19}, 520, 220, 540, 240);
        assertEquals(List.of("down", "up"), input);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 540, 240);
        assertEquals(List.of("down", "up"), input);
    }

    @Test public void draggingAnotherButtonCannotReleaseAnIndependentlyHeldKey() {
        List<String> held = new ArrayList<>();
        List<String> dragged = new ArrayList<>();
        digital(500, 200, held);
        digital(600, 200, dragged);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 520, 220, 620, 220);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 19}, 520, 220, 530, 230);
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 19}, 520, 220, 530, 230);
        assertEquals(List.of("down"), held);
        assertEquals(List.of("down", "up"), dragged);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 520, 220);
        assertEquals(List.of("down", "up"), held);
    }

    @Test public void draggingAnotherButtonCannotReleaseLockedKey() {
        List<String> held = new ArrayList<>();
        KeyBoardDigitalButton locked = digital(500, 200, held);
        locked.setEnableSwitchDown(true);
        digital(600, 200, new ArrayList<>());
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_DOWN, 0, new int[]{19}, 620, 220);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 530, 230);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 530, 230);
        assertEquals(List.of("down"), held);
        controller.releaseAllVirtualInputs();
        assertEquals(List.of("down", "up"), held);
    }

    @Test public void fixedJoystickReleasesOwnerAndDoesNotAdoptRemainingFinger() {
        List<String> input = new ArrayList<>();
        KeyAnalogStick stick = fixedStick(input);
        add(stick, 0, 200, 200, 200);
        layout();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 300);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 140, 300);
        assertTrue(stick.isPressed());
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 140, 300, 100, 330);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{7, 19}, 140, 300, 100, 330);
        assertFalse(stick.isPressed());
        assertEquals("neutral", input.get(input.size() - 1));
        int count = input.size();
        event(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 160, 330);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 160, 330);
        assertEquals(count, input.size());
    }

    private KeyAnalogStick fixedStick(List<String> input) {
        KeyAnalogStick stick = new KeyAnalogStick(controller, activity, "stick");
        stick.addAnalogStickListener(new KeyAnalogStick.AnalogStickListener() {
            @Override public void onMovement(float x, float y) {
                input.add(x == 0 && y == 0 ? "neutral" : "movement");
            }
            @Override public void onClick() {}
            @Override public void onDoubleClick() {}
            @Override public void onRevoke() {}
        });
        return stick;
    }

    @Test public void freeJoystickReleasesOwnerEvenAtNonzeroPointerIndex() {
        keyAnalogStickFree stick = new keyAnalogStickFree(controller, activity, "free");
        List<String> input = new ArrayList<>();
        stick.addAnalogStickListener(new keyAnalogStickFree.AnalogStickListener() {
            @Override public void onMovement(float x, float y) { input.add(x == 0 && y == 0 ? "neutral" : "move"); }
            @Override public void onClick() {}
            @Override public void onDoubleClick() {}
            @Override public void onRevoke() {}
        });
        stick.layout(0, 0, 200, 200);
        MotionEvent down = motion(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 100);
        stick.onTouchEvent(down);
        down.recycle();
        MotionEvent up = motion(MotionEvent.ACTION_POINTER_UP, 1, new int[]{19, 7}, 80, 80, 100, 100);
        stick.onTouchEvent(up);
        up.recycle();
        assertFalse(stick.isPressed());
        assertEquals(List.of("neutral"), input);
    }

    @Test public void dpadUsesOwnerAfterPointerReorderingAndReleasesAtOwnerLift() {
        KeyboardDigitalPadButton pad = new KeyboardDigitalPadButton(controller, activity, "dpad");
        List<Integer> directions = new ArrayList<>();
        pad.addDigitalPadListener(directions::add);
        pad.layout(0, 0, 200, 200);
        MotionEvent down = motion(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 10);
        pad.onTouchEvent(down);
        down.recycle();
        MotionEvent move = motion(MotionEvent.ACTION_MOVE, 0, new int[]{19, 7}, 10, 100, 190, 100);
        pad.onTouchEvent(move);
        move.recycle();
        assertEquals(List.of(KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_UP,
                KeyboardDigitalPadButton.DIGITAL_PAD_DIRECTION_RIGHT), directions);
        MotionEvent up = motion(MotionEvent.ACTION_POINTER_UP, 1, new int[]{19, 7}, 10, 100, 190, 100);
        pad.onTouchEvent(up);
        up.recycle();
        assertEquals(Integer.valueOf(0), directions.get(directions.size() - 1));
    }

    @Test public void joystickCameraAndAttackKeepIndependentNativeCaptures() {
        List<String> stickInput = new ArrayList<>();
        KeyAnalogStick stick = fixedStick(stickInput);
        add(stick, 0, 200, 200, 200);
        layout();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 300);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 100, 300, 500, 200);
        event(MotionEvent.ACTION_POINTER_DOWN, 2, new int[]{7, 19, 23}, 100, 300, 500, 200, 750, 150);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 19, 23}, 140, 300, 510, 200, 760, 150);
        assertTrue(stick.isPressed());
        assertTrue(stickInput.contains("movement"));
        assertTrue(cameraInput.stream().anyMatch(s -> s.startsWith("move:")));
        assertEquals("down", attackInput.get(0));
        assertTrue(attack.isPressed());
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 19, 23}, 140, 300, 510, 200, 760, 150);
        assertTrue(stick.isPressed());
        assertTrue(attack.isPressed());
        int cameraCount = cameraInput.size();
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 23}, 145, 300, 770, 150);
        assertEquals(cameraCount, cameraInput.size());
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 23}, 145, 300, 770, 150);
        assertEquals("up", attackInput.get(attackInput.size() - 1));
        assertTrue(stick.isPressed());
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 145, 300);
        assertEquals("neutral", stickInput.get(stickInput.size() - 1));
    }

    @Test public void cancellingCameraDoesNotCreateTapOrDelayedLongClick() {
        KeyBoardTouchPadButton left = pad(11, cameraInput);
        add(left, 500, 200, 80, 80);
        layout();
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 520, 220);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 520, 220, 540, 240);
        controller.releaseAllVirtualInputs();
        int count = cameraInput.size();
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 19}, 530, 220, 550, 240);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{7, 19}, 530, 220, 550, 240);
        event(MotionEvent.ACTION_UP, 0, new int[]{19}, 550, 240);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(count, cameraInput.size());
        assertFalse(cameraInput.contains("down"));
        assertFalse(cameraInput.contains("long"));
    }

    private class RecordingElement extends keyBoardVirtualControllerElement {
        MotionEvent last;
        int count;
        RecordingElement() { super(controller, activity, "recording"); }
        @Override protected void onElementDraw(Canvas canvas) {}
        @Override public boolean onElementTouchEvent(MotionEvent event) {
            if (last != null) last.recycle();
            last = MotionEvent.obtain(event);
            count++;
            return true;
        }
    }

    @Test public void pointerOrderChangesKeepOwnerCoordinatesHistoryAndRelease() {
        RecordingElement element = new RecordingElement();
        MotionEvent down = motion(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 100, 120);
        element.onTouchEvent(down);
        down.recycle();
        MotionEvent move = motion(MotionEvent.ACTION_MOVE, 0, new int[]{19, 7}, 900, 910, 110, 130);
        MotionEvent.PointerCoords[] next = {new MotionEvent.PointerCoords(), new MotionEvent.PointerCoords()};
        next[0].x = 950; next[0].y = 960;
        next[1].x = 115; next[1].y = 135;
        move.addBatch(time += 10, next, 0);
        element.onTouchEvent(move);
        assertEquals(2, move.getPointerCount());
        assertEquals(1, element.last.getPointerCount());
        assertEquals(7, element.last.getPointerId(0));
        assertEquals(115, element.last.getX(), 0);
        assertEquals(135, element.last.getY(), 0);
        assertEquals(1, element.last.getHistorySize());
        assertEquals(110, element.last.getHistoricalX(0), 0);
        assertEquals(130, element.last.getHistoricalY(0), 0);
        assertEquals(move.getDownTime(), element.last.getDownTime());
        assertEquals(move.getEventTime(), element.last.getEventTime());
        move.recycle();
        MotionEvent up = motion(MotionEvent.ACTION_POINTER_UP, 1, new int[]{19, 7}, 950, 960, 115, 135);
        element.onTouchEvent(up);
        assertEquals(MotionEvent.ACTION_UP, element.last.getAction());
        assertEquals(7, element.last.getPointerId(0));
        up.recycle();
        element.last.recycle();
    }

    @Test public void missingOwnerCancelsInsteadOfSwitchingCoordinatesToAnotherFinger() {
        List<String> input = new ArrayList<>();
        KeyBoardDigitalButton button = digital(500, 200, input);
        MotionEvent down = motion(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 20, 20);
        button.onTouchEvent(down);
        down.recycle();
        MotionEvent missing = motion(MotionEvent.ACTION_MOVE, 0, new int[]{19}, 60, 60);
        button.onTouchEvent(missing);
        missing.recycle();
        assertEquals(List.of("down", "up"), input);
        assertFalse(button.isPressed());
    }

    @Test public void editorShowsShapeAndKeepsExactImportedSizeAndOpacity() throws Exception {
        for (int type : new int[]{2, 6}) {
            controller.currentMode = KeyBoardController.ControllerMode.MoveButtons;
            GameMenuQuickBean bean = new GameMenuQuickBean("test", 14, "test", type, false);
            bean.setWidth(17);
            bean.setHeight(23);
            bean.setOpacity(0);
            bean.setZoomW(9);
            bean.setZoomH(13);
            Field list = KeyBoardController.class.getDeclaredField("beanList");
            list.setAccessible(true);
            list.set(controller, new ArrayList<>(List.of(bean)));
            attack.setTag(new TagInfo(0, false));
            attack.setLayoutParams(new FrameLayout.LayoutParams(17, 23));
            Method select = KeyBoardController.class.getDeclaredMethod("updateItem", int.class);
            select.setAccessible(true);
            select.invoke(controller, 0);
            Field panelField = KeyBoardController.class.getDeclaredField("lv_left_view");
            panelField.setAccessible(true);
            View panel = (View) panelField.get(controller);
            CheckBox shape = panel.findViewById(R.id.cb_round);
            assertEquals(View.VISIBLE, shape.getVisibility());
            assertTrue(shape.isChecked());
            shape.setChecked(false);
            assertEquals(0, bean.getShapeType());
            assertEquals(View.VISIBLE, panel.findViewById(R.id.lv_zoom_wh).getVisibility());
            assertEquals(17, bean.getWidth());
            assertEquals(23, bean.getHeight());
            assertEquals(Integer.valueOf(0), bean.getOpacity());
            assertEquals(9, ((SeekBar) panel.findViewById(R.id.sb_zoom_w)).getProgress());
            assertEquals(13, ((SeekBar) panel.findViewById(R.id.sb_zoom_h)).getProgress());
        }
    }
}
