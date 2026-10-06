package com.limelight.binding.input.virtual_controller.keyboard;
import org.junit.Test;
public class TapCameraGestureTest {
    @Test public void tap() { TapCameraGestureChecks.tapUsesOriginalPointAndNoDownEvent(); }
    @Test public void drag() { TapCameraGestureChecks.dragNeverClicksEvenWhenReturningToOrigin(); }
    @Test public void hold() { TapCameraGestureChecks.stationaryHoldDoesNotClick(); }
    @Test public void finalMove() { TapCameraGestureChecks.finalUpDisplacementBecomesMotion(); }
    @Test public void owner() { TapCameraGestureChecks.otherPointerCannotMoveOrReleaseOwner(); }
    @Test public void cancel() { TapCameraGestureChecks.cancelAndCancelledUpNeverClick(); }
    @Test public void fractions() { TapCameraGestureChecks.fractionalMotionAndZeroSensitivityStillClassifyDrag(); }
    @Test public void boundary() { TapCameraGestureChecks.boundaryAndNewGestureWork(); }
    @Test public void backwardsTime() { TapCameraGestureChecks.backwardsTimeCannotClick(); }
}
