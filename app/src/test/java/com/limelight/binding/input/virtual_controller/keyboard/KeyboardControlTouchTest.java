package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.Application;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34})
public class KeyboardControlTouchTest {
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
