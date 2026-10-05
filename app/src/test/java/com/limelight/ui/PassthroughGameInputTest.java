package com.limelight.ui;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.Game;
import com.limelight.R;
import com.limelight.binding.input.touch.AbsoluteTouchContext;
import com.limelight.binding.input.touch.RelativeTouchContext;
import com.limelight.binding.input.touch.TouchContext;
import com.limelight.nvstream.NvConnection;
import com.limelight.preferences.PreferenceConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Real Game listener + real touch contexts, with only the network sink replaced. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34})
public class PassthroughGameInputTest {
    private Game game;
    private TouchRoutingLayout root;
    private StreamView stream;
    private View background;
    private Area area;
    private Camera camera;
    private RecordingConnection connection;
    private long time = 1000;

    private static final class RecordingConnection extends NvConnection {
        final List<String> events = new ArrayList<>();
        RecordingConnection(Context context) {
            super(context, null, 0, "test", null, null, null);
        }
        @Override public void sendMousePosition(short x, short y, short w, short h) {
            events.add("position:" + x + "," + y + "/" + w + "," + h);
        }
        @Override public void sendMouseButtonDown(byte button) { events.add("down:" + button); }
        @Override public void sendMouseButtonUp(byte button) { events.add("up:" + button); }
        @Override public void sendMouseMove(short x, short y) { events.add("move:" + x + "," + y); }
        @Override public void sendMouseHighResScroll(short amount) { events.add("scroll:" + amount); }
    }

    private static class Camera extends View implements TouchRoutingLayout.Control {
        final List<Integer> actions = new ArrayList<>();
        Camera(Context context) { super(context); }
        @Override public boolean isEditing() { return false; }
        @Override public boolean onTouchEvent(MotionEvent event) {
            actions.add(event.getActionMasked());
            return true;
        }
    }

    private static final class Area extends View implements TouchRoutingLayout.PassthroughArea {
        Area(Context context) { super(context); }
        @Override public boolean isEditing() { return false; }
        @Override public boolean isPassthroughEnabled() { return true; }
    }

    @Before public void setup() {
        Context context = RuntimeEnvironment.getApplication();
        // Don't create a real stream/network session. Exercise the production listener directly.
        game = Robolectric.buildActivity(Game.class).get();
        game.prefConfig = new PreferenceConfiguration();
        game.prefConfig.touchscreenTrackpad = false;
        game.prefConfig.enableMultiTouchScreen = false;
        root = new TouchRoutingLayout(context);
        background = new View(context);
        background.setId(R.id.backgroundTouchView);
        background.setOnTouchListener(game); // This is how Game.onCreate binds touch input.
        stream = new StreamView(context); // No touch listener, just like production.
        stream.setId(R.id.surfaceView);
        stream.setFocusable(true);
        stream.setFocusableInTouchMode(true);
        area = new Area(context);
        camera = new Camera(context);
        add(background, 0, 0, 1000, 600);
        add(stream, 0, 0, 1000, 600);
        add(area, 700, 100, 100, 100);
        add(camera, 400, 0, 600, 600);
        layout();
        connection = new RecordingConnection(context);
        ReflectionHelpers.setField(game, "conn", connection);
        ReflectionHelpers.setField(game, "streamView", stream);
        ReflectionHelpers.setField(game, "rootView", root);
        ReflectionHelpers.setField(game, "grabbedInput", true);
        resetAbsoluteContexts();
    }

    private void resetAbsoluteContexts() {
        TouchContext[] contexts = ReflectionHelpers.getField(game, "touchContextMap");
        for (int i = 0; i < contexts.length; i++) {
            contexts[i] = new AbsoluteTouchContext(connection, i, stream);
        }
    }

    private void add(View view, int x, int y, int w, int h) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(w, h);
        params.leftMargin = x;
        params.topMargin = y;
        root.addView(view, params);
    }

    private void layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1000, 600);
    }

    private void event(int action, int index, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = xy[2 * i];
            coords[i].y = xy[2 * i + 1];
            coords[i].pressure = 1;
        }
        MotionEvent event = MotionEvent.obtain(1000, time += 10,
                action | index << MotionEvent.ACTION_POINTER_INDEX_SHIFT,
                ids.length, properties, coords, 0, 0, 1, 1, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            boolean handled = root.dispatchTouchEvent(event);
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                assertTrue(handled);
            }
        }
        finally { event.recycle(); }
    }

    private void tap(float x, float y) {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, x, y);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, x, y);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
    }

    @Test public void regionOverCameraSendsOneAbsoluteLeftClickThroughGame() {
        tap(720, 120);
        assertEquals(Arrays.asList("position:720,120/1000,600", "down:1", "up:1"), connection.events);
        assertTrue(camera.actions.isEmpty());
    }

    @Test public void sameTouchWithControlsHiddenAndWithRegionHasIdenticalInput() {
        tap(720, 120);
        List<String> withRegion = new ArrayList<>(connection.events);
        assertFalse(withRegion.isEmpty());
        connection.events.clear();
        resetAbsoluteContexts();
        area.setVisibility(View.GONE);
        camera.setVisibility(View.GONE);
        tap(720, 120);
        assertEquals(withRegion, connection.events);
    }

    @Test public void cameraHeldAndSecondFingerInAreaStillProducesAbsoluteClick() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{4}, 500, 250);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{4, 19}, 500, 250, 720, 120);
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{4, 19}, 500, 250, 720, 120);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        assertEquals(Arrays.asList("position:720,120/1000,600", "down:1", "up:1"), connection.events);
        assertFalse(camera.actions.contains(MotionEvent.ACTION_UP));
        event(MotionEvent.ACTION_UP, 0, new int[]{4}, 500, 250);
        assertEquals(Integer.valueOf(MotionEvent.ACTION_UP), camera.actions.get(camera.actions.size() - 1));
    }

    @Test public void letterboxedVideoKeepsNormalBackgroundToStreamCoordinateMapping() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) stream.getLayoutParams();
        params.width = 800;
        params.height = 500;
        params.leftMargin = 100;
        params.topMargin = 50;
        stream.setLayoutParams(params);
        layout();
        tap(720, 120);
        assertEquals(Arrays.asList("position:620,70/800,500", "down:1", "up:1"), connection.events);
    }

    @Test public void configuredTrackpadStillUsesRelativeTouchContext() {
        game.prefConfig.touchscreenTrackpad = true;
        TouchContext[] contexts = ReflectionHelpers.getField(game, "touchContextMap");
        for (int i = 0; i < contexts.length; i++) {
            contexts[i] = new RelativeTouchContext(connection, i, 1280, 720, stream, game.prefConfig);
        }
        tap(720, 120);
        assertEquals(Arrays.asList("down:1", "up:1"), connection.events);
    }

    @Test public void ungrabbedInputDoesNotClickOrFallThroughToCamera() {
        ReflectionHelpers.setField(game, "grabbedInput", false);
        tap(720, 120);
        assertTrue(connection.events.isEmpty());
        assertTrue(camera.actions.isEmpty());
    }

    @Test public void cancelledHeldDirectTouchReleasesAndDoesNotCreateAnotherClick() {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{7}, 720, 120);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150));
        root.cancelPassthroughTouches();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700));
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 720, 120);
        assertEquals(Arrays.asList("position:720,120/1000,600", "down:1", "up:1"), connection.events);
    }
}
