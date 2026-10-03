package com.limelight.binding.input.virtual_controller.keyboard;

import org.junit.Test;

public class TimedKeyCombinationTest {
    @Test public void shortTapOrdersAltBeforeNumber() { TimedKeyCombinationChecks.shortTapOrdersAltBeforeNumber(); }
    @Test public void heldChordStaysDownUntilRelease() { TimedKeyCombinationChecks.heldChordStaysDownUntilRelease(); }
    @Test public void allModifierVariantsAreRecognized() { TimedKeyCombinationChecks.allModifierVariantsAreRecognized(); }
    @Test public void multipleModifiersReleaseLast() { TimedKeyCombinationChecks.multipleModifiersReleaseLast(); }
    @Test public void lockedAltIsNotReleasedByChord() { TimedKeyCombinationChecks.lockedAltIsNotReleasedByChord(); }
    @Test public void overlappingChordsKeepSharedModifier() { TimedKeyCombinationChecks.overlappingChordsKeepSharedModifier(); }
    @Test public void rapidTapsAreSerialized() { TimedKeyCombinationChecks.rapidTapsAreSerialized(); }
    @Test public void cancellationBeforeNumberPreventsItsPress() { TimedKeyCombinationChecks.cancellationBeforeNumberPreventsItsPress(); }
    @Test public void cancellationReleasesNumberBeforeAltImmediately() { TimedKeyCombinationChecks.cancellationReleasesNumberBeforeAltImmediately(); }
    @Test public void cancellationDiscardsQueuedTapsAndAllowsReuse() { TimedKeyCombinationChecks.cancellationDiscardsQueuedTapsAndAllowsReuse(); }
    @Test public void malformedCodesDoNotPartiallyPressKeys() { TimedKeyCombinationChecks.malformedCodesDoNotPartiallyPressKeys(); }
    @Test public void whitespaceAndDuplicateKeysAreHandled() { TimedKeyCombinationChecks.whitespaceAndDuplicateKeysAreHandled(); }
    @Test public void nonModifierChordAndSingleKeyStillWork() { TimedKeyCombinationChecks.nonModifierChordAndSingleKeyStillWork(); }
    @Test public void duplicateDownAndUnmatchedUpAreHarmless() { TimedKeyCombinationChecks.duplicateDownAndUnmatchedUpAreHarmless(); }
    @Test public void sharedMouseButtonAndReleaseAllAreSafe() { TimedKeyCombinationChecks.sharedMouseButtonAndReleaseAllAreSafe(); }
}
