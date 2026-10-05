package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dependency-free gesture and shared-input regression checks, also run by JUnit. */
public final class AttackCameraGestureChecks {
    private static final class Fixture {
        final List<String> events = new ArrayList<>();
        final Object owner = new Object();
        final VirtualInputState inputs = new VirtualInputState(new VirtualInputState.Sink() {
            public void keyboard(int key, boolean down) { throw new AssertionError("Keyboard event"); }
            public void mouse(int button, boolean down) { events.add(down ? "DOWN" : "UP"); }
        });
        final AttackCameraGesture gesture = new AttackCameraGesture(new AttackCameraGesture.Sink() {
            public void button(boolean down) { inputs.mouse(owner, 1, down); }
            public void move(int x, int y) { events.add("MOVE:" + x + "," + y); }
        });

        void expect(String... expected) {
            if (!events.equals(Arrays.asList(expected))) {
                throw new AssertionError("Expected " + Arrays.toString(expected) + ", got " + events);
            }
        }
    }

    static void downImmediatelyHoldsWithoutMovement() {
        Fixture f = new Fixture();
        f.gesture.begin(7, 30, 40, 1, 1);
        f.expect("DOWN");
        if (f.gesture.getPointerId() != 7) throw new AssertionError("Lost held pointer");
        f.gesture.release(7);
        f.expect("DOWN", "UP");
    }

    static void shortTapProducesExactlyOneClick() {
        Fixture f = new Fixture();
        f.gesture.begin(0, 0, 0, 1, 1);
        f.gesture.release(0);
        f.gesture.release(0);
        f.gesture.cancel();
        f.expect("DOWN", "UP");
    }

    static void dragMovesBothAxesWhileButtonIsHeld() {
        Fixture f = new Fixture();
        f.gesture.begin(9, 20, 30, 1, 1);
        f.gesture.move(9, 25, 23);
        f.gesture.move(9, 15, 33);
        f.gesture.release(9);
        f.expect("DOWN", "MOVE:5,-7", "MOVE:-10,10", "UP");
    }

    static void smallMovementsAccumulateInsteadOfBeingDiscarded() {
        Fixture f = new Fixture();
        f.gesture.begin(2, 0, 0, 0.5, 0.5);
        f.gesture.move(2, 0.5f, -0.5f);
        f.gesture.move(2, 1, -1);
        f.gesture.move(2, 1.5f, -1.5f);
        f.expect("DOWN");
        f.gesture.move(2, 2, -2);
        f.expect("DOWN", "MOVE:1,-1");
    }

    static void fractionalDirectionReversalDoesNotInventMotion() {
        Fixture f = new Fixture();
        f.gesture.begin(2, 0, 0, 1, 1);
        f.gesture.move(2, 0.75f, -0.75f);
        f.gesture.move(2, 0, 0);
        f.gesture.move(2, -1, 1);
        f.expect("DOWN", "MOVE:-1,1");
    }

    static void independentAxisSensitivityIsApplied() {
        Fixture f = new Fixture();
        f.gesture.begin(0, 0, 0, 2.5, 1.5);
        f.gesture.move(0, 4, -4);
        f.expect("DOWN", "MOVE:10,-6");
    }

    static void zeroSensitivityStillAllowsAttack() {
        Fixture f = new Fixture();
        f.gesture.begin(0, 0, 0, 0, 0);
        f.gesture.move(0, 100, 100);
        f.gesture.release(0);
        f.expect("DOWN", "UP");
    }

    static void otherPointerCannotMoveOrReleaseOwner() {
        Fixture f = new Fixture();
        f.gesture.begin(27, 10, 10, 1, 1);
        f.gesture.move(4, 500, 500);
        f.gesture.release(4);
        f.gesture.move(27, 12, 13);
        f.expect("DOWN", "MOVE:2,3");
        f.gesture.release(27);
        f.expect("DOWN", "MOVE:2,3", "UP");
    }

    static void liftingOwnerDoesNotHandOffToRemainingFinger() {
        Fixture f = new Fixture();
        f.gesture.begin(27, 10, 10, 1, 1);
        f.gesture.release(27);
        f.gesture.move(4, 500, 500);
        f.gesture.release(4);
        f.expect("DOWN", "UP");
    }

    static void cancelReleasesOnceAndRejectsLateMovement() {
        Fixture f = new Fixture();
        f.gesture.begin(1, 10, 10, 1, 1);
        f.gesture.cancel();
        f.gesture.cancel();
        f.gesture.move(1, 200, 200);
        f.gesture.release(1);
        f.expect("DOWN", "UP");
        if (f.gesture.getPointerId() != -1) throw new AssertionError("Retained pointer");
    }

    static void newGestureGetsFreshAnchorAndFractionalState() {
        Fixture f = new Fixture();
        f.gesture.begin(1, 0, 0, 1, 1);
        f.gesture.move(1, 0.75f, 0.75f);
        f.gesture.cancel();
        f.gesture.begin(6, 100, 100, 1, 1);
        f.gesture.move(6, 100.5f, 100.5f);
        f.expect("DOWN", "UP", "DOWN");
        f.gesture.move(6, 101, 101);
        f.gesture.release(6);
        f.expect("DOWN", "UP", "DOWN", "MOVE:1,1", "UP");
    }

    static void sharedMouseButtonIsNotReleasedByThisControl() {
        Fixture f = new Fixture();
        Object lockedButton = new Object();
        f.inputs.mouse(lockedButton, 1, true);
        f.gesture.begin(1, 0, 0, 1, 1);
        f.gesture.cancel();
        f.expect("DOWN");
        f.inputs.mouse(lockedButton, 1, false);
        f.expect("DOWN", "UP");
    }

    static void draggingOutsideHitRectangleKeepsAiming() {
        Fixture f = new Fixture();
        f.gesture.begin(5, 30, 30, 1, 1);
        f.gesture.move(5, -100, 500);
        f.gesture.release(5);
        f.expect("DOWN", "MOVE:-130,470", "UP");
    }

    static void secondBeginCannotReplaceAnActiveGesture() {
        Fixture f = new Fixture();
        f.gesture.begin(5, 10, 10, 1, 1);
        if (f.gesture.begin(6, 500, 500, 1, 1)) throw new AssertionError("Pointer takeover");
        f.gesture.move(5, 11, 11);
        f.gesture.release(5);
        f.expect("DOWN", "MOVE:1,1", "UP");
    }

    public static void main(String[] args) {
        downImmediatelyHoldsWithoutMovement();
        shortTapProducesExactlyOneClick();
        dragMovesBothAxesWhileButtonIsHeld();
        smallMovementsAccumulateInsteadOfBeingDiscarded();
        fractionalDirectionReversalDoesNotInventMotion();
        independentAxisSensitivityIsApplied();
        zeroSensitivityStillAllowsAttack();
        otherPointerCannotMoveOrReleaseOwner();
        liftingOwnerDoesNotHandOffToRemainingFinger();
        cancelReleasesOnceAndRejectsLateMovement();
        newGestureGetsFreshAnchorAndFractionalState();
        sharedMouseButtonIsNotReleasedByThisControl();
        draggingOutsideHitRectangleKeepsAiming();
        secondBeginCannotReplaceAnActiveGesture();
        System.out.println("14 attack + camera checks passed");
    }
}
