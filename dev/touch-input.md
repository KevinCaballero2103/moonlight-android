# Touch input improvements: first stage

## Starting point

The fork's `feature/per-control-opacity` branch is one commit ahead of
`master` (`48c780da`). Its source blobs match local opacity commit `dac5a030`.
The fork commit is `97dab97a`; four Java files have different executable bits,
but identical source contents. This work starts from the local opacity branch
on `feature/touch-input-improvements`. The touch-only test APK change is separate.

## Existing input path

`GameMenuQuickBean` stores controls in layout JSON. The loader constructs views
and listeners. Digital buttons call `onClick()` on touch down and `onRelease()`
on touch up; locked buttons keep their input down after touch up. Combination
buttons pass their comma-separated Android keycodes to
`KeyBoardController.sendAssembleKey()`. That method currently sends all downs
immediately in stored order, then sends reverse-order ups when released.
It does not generate a complete tap on down alone.

`Game.keyboardEvent()` translates Android keycodes, updates modifier flags via
`handleSpecialKeys()`, and sends keyboard packets through `NvConnection`.
The AXIX prefix (`29,52,37,52,...`) selects local UI actions on release and must
continue to bypass host keyboard input.

## First-stage design

- Parse and validate existing combination strings without changing layout JSON.
- Deduplicate keys, preserving their order within modifier/non-modifier groups.
- Press Alt/Ctrl/Shift/Meta first, spacing successive transitions by 40 ms using
  the existing main-thread Handler. Release in reverse order.
- Keep keys held while the button is held. A short tap still receives a full
  keydown interval before release. Queue repeated taps of the same button.
- Track input ownership per control/session. Ending a combination must not
  release an Alt key still owned by a separate locked button or another chord.
- Cancel scheduled work and release owned inputs when a gesture is cancelled,
  the overlay is hidden/rebuilt, focus is lost, or the stream stops.
- Keep AXIX actions and single-key buttons on their existing immediate paths.

The locked-Alt experiment demonstrates that the host/game accept the shortcut.
It does not prove that 40 ms is the required timing; Android/Genshin testing is
still needed. This stage does not change touch splitting, sensitivity, or layout
geometry. Attack + camera and passthrough are subsequent stages.
