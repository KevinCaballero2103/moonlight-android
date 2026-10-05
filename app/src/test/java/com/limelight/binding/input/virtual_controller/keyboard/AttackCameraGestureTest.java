package com.limelight.binding.input.virtual_controller.keyboard;

import org.junit.Test;

public class AttackCameraGestureTest {
    @Test public void downImmediatelyHoldsWithoutMovement() { AttackCameraGestureChecks.downImmediatelyHoldsWithoutMovement(); }
    @Test public void shortTapProducesExactlyOneClick() { AttackCameraGestureChecks.shortTapProducesExactlyOneClick(); }
    @Test public void dragMovesBothAxesWhileButtonIsHeld() { AttackCameraGestureChecks.dragMovesBothAxesWhileButtonIsHeld(); }
    @Test public void smallMovementsAccumulateInsteadOfBeingDiscarded() { AttackCameraGestureChecks.smallMovementsAccumulateInsteadOfBeingDiscarded(); }
    @Test public void fractionalDirectionReversalDoesNotInventMotion() { AttackCameraGestureChecks.fractionalDirectionReversalDoesNotInventMotion(); }
    @Test public void independentAxisSensitivityIsApplied() { AttackCameraGestureChecks.independentAxisSensitivityIsApplied(); }
    @Test public void zeroSensitivityStillAllowsAttack() { AttackCameraGestureChecks.zeroSensitivityStillAllowsAttack(); }
    @Test public void otherPointerCannotMoveOrReleaseOwner() { AttackCameraGestureChecks.otherPointerCannotMoveOrReleaseOwner(); }
    @Test public void liftingOwnerDoesNotHandOffToRemainingFinger() { AttackCameraGestureChecks.liftingOwnerDoesNotHandOffToRemainingFinger(); }
    @Test public void cancelReleasesOnceAndRejectsLateMovement() { AttackCameraGestureChecks.cancelReleasesOnceAndRejectsLateMovement(); }
    @Test public void newGestureGetsFreshAnchorAndFractionalState() { AttackCameraGestureChecks.newGestureGetsFreshAnchorAndFractionalState(); }
    @Test public void sharedMouseButtonIsNotReleasedByThisControl() { AttackCameraGestureChecks.sharedMouseButtonIsNotReleasedByThisControl(); }
    @Test public void draggingOutsideHitRectangleKeepsAiming() { AttackCameraGestureChecks.draggingOutsideHitRectangleKeepsAiming(); }
    @Test public void secondBeginCannotReplaceAnActiveGesture() { AttackCameraGestureChecks.secondBeginCannotReplaceAnActiveGesture(); }
}
