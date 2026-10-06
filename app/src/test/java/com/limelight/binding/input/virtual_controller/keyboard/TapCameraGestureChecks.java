package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.ArrayList;
import java.util.List;

public final class TapCameraGestureChecks {
    private static class Fixture {
        final List<String> events = new ArrayList<>();
        final TapCameraGesture gesture = new TapCameraGesture(new TapCameraGesture.Sink() {
            public void move(int x, int y) { events.add("move:" + x + "," + y); }
            public void tap(float x, float y) { events.add("tap:" + x + "," + y); }
        });
        Fixture() { gesture.begin(7, 100, 100, 1000, 8, 1, 1); }
        void expect(String... wanted) {
            if (!events.equals(List.of(wanted))) throw new AssertionError(events + " != " + List.of(wanted));
        }
    }
    static void tapUsesOriginalPointAndNoDownEvent() {
        Fixture f = new Fixture(); f.expect();
        f.gesture.move(7, 102, 101);
        f.gesture.release(7, 103, 102, 1100, false);
        f.expect("tap:100.0,100.0");
    }
    static void dragNeverClicksEvenWhenReturningToOrigin() {
        Fixture f = new Fixture();
        f.gesture.move(7, 120, 110); f.gesture.move(7, 100, 100);
        f.gesture.release(7, 100, 100, 1100, false);
        f.expect("move:20,10", "move:-20,-10");
    }
    static void stationaryHoldDoesNotClick() {
        Fixture f = new Fixture(); f.gesture.release(7, 100, 100, 1500, false); f.expect();
    }
    static void finalUpDisplacementBecomesMotion() {
        Fixture f = new Fixture(); f.gesture.release(7, 120, 100, 1100, false); f.expect("move:20,0");
    }
    static void otherPointerCannotMoveOrReleaseOwner() {
        Fixture f = new Fixture(); f.gesture.move(19, 300, 300);
        f.gesture.release(19, 300, 300, 1100, false); f.expect();
        f.gesture.release(7, 100, 100, 1100, false); f.expect("tap:100.0,100.0");
    }
    static void cancelAndCancelledUpNeverClick() {
        Fixture f = new Fixture(); f.gesture.cancel();
        f.gesture.release(7, 100, 100, 1100, false); f.expect();
        f.gesture.begin(7, 100, 100, 1200, 8, 1, 1);
        f.gesture.release(7, 200, 200, 1300, true); f.expect();
    }
    static void fractionalMotionAndZeroSensitivityStillClassifyDrag() {
        Fixture f = new Fixture(); f.gesture.begin(7, 0, 0, 1000, 1, 0.1, 0.1);
        f.gesture.move(7, 9, 0); f.gesture.move(7, 10, 0);
        f.gesture.release(7, 10, 0, 1100, false); f.expect("move:1,0");
        Fixture zero = new Fixture(); zero.gesture.begin(7, 0, 0, 1000, 8, 0, 0);
        zero.gesture.release(7, 20, 0, 1100, false); zero.expect();
    }
    static void boundaryAndNewGestureWork() {
        Fixture f = new Fixture(); f.gesture.release(7, 108, 100, 1250, false);
        f.expect("tap:100.0,100.0");
        f.gesture.begin(19, 200, 200, 1300, 8, 1, 1);
        f.gesture.release(19, 200, 200, 1551, false);
        f.expect("tap:100.0,100.0");
    }
    static void backwardsTimeCannotClick() {
        Fixture f = new Fixture(); f.gesture.release(7, 100, 100, 999, false); f.expect();
    }
    public static void main(String[] args) {
        tapUsesOriginalPointAndNoDownEvent(); dragNeverClicksEvenWhenReturningToOrigin();
        stationaryHoldDoesNotClick(); finalUpDisplacementBecomesMotion();
        otherPointerCannotMoveOrReleaseOwner(); cancelAndCancelledUpNeverClick();
        fractionalMotionAndZeroSensitivityStillClassifyDrag(); boundaryAndNewGestureWork();
        backwardsTimeCannotClick();
        System.out.println("9 camera + tap checks passed");
    }
}
