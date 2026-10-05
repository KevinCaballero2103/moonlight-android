package com.limelight.ui;

import android.app.Application;
import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34})
public class TouchRoutingLayoutTest {
    private TouchRoutingLayout root;
    private RecordingView stream;
    private RecordingView background;
    private ControlView camera;
    private ControlView joystick;
    private AreaView area;
    private long time;

    private static class Packet {
        final int action;
        final int actionIndex;
        final int[] ids;
        final float x;
        final float y;
        Packet(MotionEvent event) {
            action = event.getActionMasked();
            actionIndex = event.getActionIndex();
            ids = new int[event.getPointerCount()];
            for (int i = 0; i < ids.length; i++) ids[i] = event.getPointerId(i);
            x = event.getX();
            y = event.getY();
        }
    }

    private static class RecordingView extends View {
        final List<Packet> packets = new ArrayList<>();
        boolean accepts = true;
        RecordingView(Context context) { super(context); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            packets.add(new Packet(event));
            return accepts || ((TouchRoutingLayout) getParent()).isPassthroughTarget(this);
        }
        Packet last() { return packets.get(packets.size() - 1); }
    }

    private static class ControlView extends RecordingView implements TouchRoutingLayout.Control {
        boolean editing;
        ControlView(Context context) { super(context); }
        @Override public boolean isEditing() { return editing; }
    }

    private static class AreaView extends ControlView implements TouchRoutingLayout.PassthroughArea {
        boolean active = true;
        AreaView(Context context) { super(context); }
        @Override public boolean isPassthroughEnabled() { return active; }
        @Override public boolean onTouchEvent(MotionEvent event) {
            return editing && super.onTouchEvent(event);
        }
    }

    @Before public void setup() {
        Context context = RuntimeEnvironment.getApplication();
        root = new TouchRoutingLayout(context);
        background = new RecordingView(context);
        background.setId(R.id.backgroundTouchView);
        stream = new RecordingView(context);
        stream.setId(R.id.surfaceView);
        area = new AreaView(context);
        camera = new ControlView(context);
        joystick = new ControlView(context);
        add(background, 0, 0, 1000, 600);
        add(stream, 0, 0, 1000, 600);
        add(area, 700, 100, 100, 100);
        add(camera, 400, 0, 600, 600); // Above the region deliberately.
        add(joystick, 0, 200, 200, 300);
        layout();
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

    private void event(int action, int index, int[] ids, float... coordinates) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] points = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            points[i] = new MotionEvent.PointerCoords();
            points[i].x = coordinates[2 * i];
            points[i].y = coordinates[2 * i + 1];
            points[i].pressure = 1;
            points[i].size = 1;
        }
        MotionEvent event = MotionEvent.obtain(1, ++time,
                action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT), ids.length,
                properties, points, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            boolean handled = root.dispatchTouchEvent(event);
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                assertTrue(handled);
            }
        }
        finally { event.recycle(); }
    }

    private void down(int id, float x, float y) {
        event(MotionEvent.ACTION_DOWN, 0, new int[]{id}, x, y);
    }

    @Test public void cameraOutsideAreaIsUnchanged() {
        down(7, 500, 100);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 500, 100);
        assertEquals(2, camera.packets.size());
        assertEquals(MotionEvent.ACTION_UP, camera.last().action);
        assertTrue(stream.packets.isEmpty());
    }

    @Test public void regionBypassesCameraAndHigherButtonRegardlessOfCreationOrder() {
        ControlView attack = new ControlView(root.getContext());
        add(attack, 710, 110, 70, 70);
        layout();
        down(9, 720, 120);
        event(MotionEvent.ACTION_UP, 0, new int[]{9}, 720, 120);
        assertEquals(2, stream.packets.size());
        assertArrayEquals(new int[]{9}, stream.last().ids);
        assertTrue(camera.packets.isEmpty());
        assertTrue(attack.packets.isEmpty());
    }

    @Test public void joystickAndPassthroughHaveIndependentPointers() {
        down(7, 100, 300);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 100, 300, 720, 120);
        assertArrayEquals(new int[]{19}, stream.last().ids);
        assertEquals(MotionEvent.ACTION_DOWN, stream.last().action);
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 19}, 100, 300, 720, 120);
        assertEquals(MotionEvent.ACTION_UP, stream.last().action);
        assertEquals(MotionEvent.ACTION_MOVE, joystick.last().action);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 100, 300);
        assertEquals(MotionEvent.ACTION_UP, joystick.last().action);
    }

    @Test public void cameraAlreadyHoldingCannotStealNewFingerInRegion() {
        down(4, 500, 100);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{4, 12}, 500, 100, 720, 120);
        assertArrayEquals(new int[]{4}, camera.last().ids);
        assertArrayEquals(new int[]{12}, stream.last().ids);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{4, 12}, 500, 100, 720, 120);
        assertEquals(MotionEvent.ACTION_UP, camera.last().action);
        assertEquals(MotionEvent.ACTION_MOVE, stream.last().action);
        event(MotionEvent.ACTION_UP, 0, new int[]{12}, 720, 120);
        assertEquals(MotionEvent.ACTION_UP, stream.last().action);
    }

    @Test public void passthroughFirstThenJoystickStillSplits() {
        down(18, 720, 120);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{18, 2}, 720, 120, 100, 300);
        assertArrayEquals(new int[]{18}, stream.last().ids);
        assertArrayEquals(new int[]{2}, joystick.last().ids);
        assertEquals(MotionEvent.ACTION_DOWN, joystick.last().action);
    }

    @Test public void twoPassthroughFingersMergeIntoOneNativeStreamTarget() {
        down(17, 720, 120);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{17, 3}, 720, 120, 750, 150);
        assertEquals(MotionEvent.ACTION_POINTER_DOWN, stream.last().action);
        assertEquals(1, stream.last().actionIndex);
        assertArrayEquals(new int[]{17, 3}, stream.last().ids);
        event(MotionEvent.ACTION_POINTER_UP, 0, new int[]{17, 3}, 720, 120, 750, 150);
        assertEquals(MotionEvent.ACTION_POINTER_UP, stream.last().action);
        assertEquals(0, stream.last().actionIndex);
        event(MotionEvent.ACTION_UP, 0, new int[]{3}, 750, 150);
        assertArrayEquals(new int[]{3}, stream.last().ids);
        assertEquals(MotionEvent.ACTION_UP, stream.last().action);
    }

    @Test public void naturalStreamAndPassthroughFingersMerge() {
        down(5, 300, 100); // No virtual control at this point.
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{5, 11}, 300, 100, 720, 120);
        assertArrayEquals(new int[]{5, 11}, stream.last().ids);
        assertEquals(MotionEvent.ACTION_POINTER_DOWN, stream.last().action);
    }

    @Test public void threeDistinctTargetsRemainIndependent() {
        down(4, 100, 300);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{4, 9}, 100, 300, 500, 100);
        event(MotionEvent.ACTION_POINTER_DOWN, 2, new int[]{4, 9, 22}, 100, 300, 500, 100, 720, 120);
        assertArrayEquals(new int[]{4}, joystick.last().ids);
        assertArrayEquals(new int[]{9}, camera.last().ids);
        assertArrayEquals(new int[]{22}, stream.last().ids);
    }

    @Test public void fingerLeavingAreaKeepsStreamCapture() {
        down(8, 720, 120);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{8}, 500, 250);
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 500, 250);
        assertEquals(3, stream.packets.size());
        assertTrue(camera.packets.isEmpty());
    }

    @Test public void cameraFingerEnteringAreaKeepsCameraCapture() {
        down(8, 500, 250);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{8}, 720, 120);
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 720, 120);
        assertEquals(3, camera.packets.size());
        assertTrue(stream.packets.isEmpty());
    }

    @Test public void editingAnyControlDisablesPassthrough() {
        joystick.editing = true;
        down(8, 720, 120);
        assertEquals(1, camera.packets.size());
        assertTrue(stream.packets.isEmpty());
    }

    @Test public void regionIsSelectableDuringEditing() {
        area.editing = true;
        area.bringToFront();
        down(8, 720, 120);
        assertEquals(1, area.packets.size());
        assertTrue(stream.packets.isEmpty());
    }

    @Test public void transparentRegionStillRoutesTouch() {
        area.setAlpha(0);
        down(8, 720, 120);
        assertEquals(1, stream.packets.size());
        assertTrue(camera.packets.isEmpty());
    }

    @Test public void disabledHiddenAndDeletedRegionsDoNotRoute() {
        area.active = false;
        down(8, 720, 120);
        assertEquals(1, camera.packets.size());
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 720, 120);
        area.active = true;
        area.setVisibility(View.GONE);
        down(8, 720, 120);
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 720, 120);
        root.removeView(area);
        down(8, 720, 120);
        assertEquals(5, camera.packets.size());
        assertTrue(stream.packets.isEmpty());
    }

    @Test public void elevatedCameraIsBypassedWithoutChangingPersistentZOrOrder() {
        camera.setElevation(12);
        stream.setTranslationZ(2);
        int streamIndex = root.indexOfChild(stream);
        down(8, 720, 120);
        assertEquals(1, stream.packets.size());
        assertEquals(12f, camera.getZ(), 0);
        assertEquals(2f, stream.getTranslationZ(), 0);
        assertEquals(streamIndex, root.indexOfChild(stream));
        assertFalse(root.isPassthroughTarget(stream));
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 720, 120);
        down(8, 500, 100);
        assertEquals(1, camera.packets.size());
    }

    @Test public void streamCoordinatesUseItsNativeTransform() {
        stream.setTranslationX(100);
        stream.setTranslationY(50);
        down(8, 720, 120);
        assertEquals(620f, stream.last().x, 0);
        assertEquals(70f, stream.last().y, 0);
    }

    @Test public void translatedRegionIsHitAtNewLocation() {
        area.setTranslationX(-200);
        down(8, 520, 120);
        assertEquals(1, stream.packets.size());
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 520, 120);
        down(8, 720, 120);
        assertEquals(1, camera.packets.size());
    }

    @Test public void letterboxUsesExistingBackgroundTouchTarget() {
        stream.setTranslationY(250);
        down(8, 720, 120);
        assertEquals(1, background.packets.size());
        assertTrue(stream.packets.isEmpty());
        assertTrue(camera.packets.isEmpty());
    }

    @Test public void declinedStreamDownCannotFallThroughToCamera() {
        stream.accepts = false;
        down(8, 720, 120);
        event(MotionEvent.ACTION_UP, 0, new int[]{8}, 720, 120);
        assertEquals(2, stream.packets.size());
        assertTrue(camera.packets.isEmpty());
    }

    @Test public void layoutCancellationEndsAllTargetsAndIgnoresTrailingPackets() {
        down(7, 100, 300);
        event(MotionEvent.ACTION_POINTER_DOWN, 1, new int[]{7, 19}, 100, 300, 720, 120);
        root.cancelPassthroughTouches();
        assertEquals(MotionEvent.ACTION_CANCEL, stream.last().action);
        assertEquals(MotionEvent.ACTION_CANCEL, joystick.last().action);
        int streamCount = stream.packets.size();
        root.cancelPassthroughTouches();
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7, 19}, 100, 300, 720, 120);
        event(MotionEvent.ACTION_POINTER_UP, 1, new int[]{7, 19}, 100, 300, 720, 120);
        event(MotionEvent.ACTION_UP, 0, new int[]{7}, 100, 300);
        assertEquals(streamCount, stream.packets.size());
        down(12, 720, 120);
        assertEquals(MotionEvent.ACTION_DOWN, stream.last().action);
        assertArrayEquals(new int[]{12}, stream.last().ids);
    }

    @Test public void nativeCancelEndsCaptureAndFreshGestureWorks() {
        down(7, 720, 120);
        event(MotionEvent.ACTION_CANCEL, 0, new int[]{7}, 720, 120);
        assertEquals(MotionEvent.ACTION_CANCEL, stream.last().action);
        event(MotionEvent.ACTION_MOVE, 0, new int[]{7}, 730, 130);
        assertEquals(2, stream.packets.size());
        down(8, 720, 120);
        assertEquals(3, stream.packets.size());
    }
}
