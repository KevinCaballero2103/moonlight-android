# Attack + camera: design before implementation

Alt + number is confirmed working by the user in Genshin. The export change
was reverted at the user's request; this stage starts from the verified input
and per-control opacity implementation.

## Existing path

The editor lists touchpads as `btnType: 2` with numeric mouse-mode codes. Gson
saves these beans and import/export copies the layout JSON. The controller
loader constructs `KeyBoardTouchPadButton` and maps its callbacks to mouse
events using that View as the input owner. The existing left touchpad (code 11)
presses left mouse after a MOVE more than 100 ms after DOWN and also clicks on
short UP; merely holding without moving does not immediately press the mouse.
Legacy deltas use `1280 / controlWidth` and `720 / controlHeight`.

Views are children of the Game root FrameLayout. No explicit motion-event
splitting override was found in Game, its layout or this controller. Android
dispatch is retained in this stage. The common control base ignores secondary
action indexes, with a warning about unrelated extra pointers. Other controls
keep that guard. The new mode opts into pointer-aware events only while active.
It resolves its initial pointerId with findPointerIndex for each MOVE and
checks the actual lifted pointer on POINTER_UP. A second finger on the same
View cannot take over the gesture; lifting the original finger ends it.

Reference: https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi

## New mode

- Add touchpad code 14, named Attack + camera with Spanish/Chinese strings.
- Reuse the existing touchpad View, mouse callback loader, editor, geometry,
  opacity and JSON fields. Older control codes retain their behavior.
- DOWN presses left mouse immediately; MOVE sends relative mouse deltas;
  UP releases it. A short tap must produce exactly one down/up pair.
- No click timer, lock mode, delayed click, pointer handoff or double-tap macro.
- Retain the gesture when dragged outside the initial control rectangle.
- Use overlay dimensions, not control dimensions, for the new mode's reference
  scale, then apply the existing global touchpad X/Y sensitivity. Accumulate
  fractional motion so small movements are not discarded. Legacy sensitivity
  stays unchanged; per-control sensitivity is a later stage.
- CANCEL, missing pointer, hidden/rebuilt layout, focus loss and disconnect use
  the existing safe-release path. Releasing this control must not release a
  separate held/locked mouse button that has another owner.

The event-independent gesture helper will be tested for quick taps, holds,
movement signs/fractions, nonzero pointer IDs, other-pointer release, missing
pointer cancellation, repeated cancellation, new gestures and shared mouse
ownership. Gson checks will cover the new code and opacity round trip.
Android dispatch, overlap priority and Genshin charging/aiming require device
tests; this stage does not claim to solve all overlay multitouch or passthrough.
