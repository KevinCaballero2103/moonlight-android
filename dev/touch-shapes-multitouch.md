# Shapes and pointer ownership: review before implementation (v8)

v7 passthrough was confirmed working by the user on Android/Genshin.
The feature and test branches are clean; no PRs will be created.

## Existing behavior

- The root TouchRoutingLayout explicitly enables native event splitting.
  Different child Views therefore receive independently captured gestures.
  Joystick + camera + attack already work by this route; region gestures go
  to backgroundTouchView, preserving Game.onTouch and its configured mode.
- The secondary-touch actionIndex guard does not filter MOVE (actionIndex 0).
  Legacy camera/buttons/fixed joystick/cruceta then read index 0. When two
  pointers share a View and the original finger lifts, its POINTER_UP is not
  handled as UP, and the surviving finger may take over on the next MOVE.
- The free joystick has its own touchID but the shared actionIndex guard may
  discard its owner's POINTER_UP. Attack + camera already opts into complete
  events and correctly tracks its owner; that path will remain unchanged.
- Digital buttons have legacy drag-across-button behavior. It must not release
  another button already held by its own finger (or a locked button).
- shapeType is persisted, but touchpads ignore it and the shape checkbox is
  hidden for them/regions. Stored shapeType=0 on an old touchpad still means
  rectangular visually, so interpreting it as circular now would break layouts.

## Design

- Keep native ViewGroup splitting, child order, Game touch contexts and the
  editor's secondary-touch guard. Normalize only a legacy control's own input
  callback to its initiating pointerId, preserving coordinates, times,
  properties and MOVE history with public MotionEvent APIs. Owner lift becomes
  UP immediately; additional pointers cannot take over until a fresh DOWN.
  CANCEL, missing owner or lifecycle cleanup release safely. Attack + camera
  continues using its existing owner/history path.
- Add optional touchShape for touchpad/region types: absent = rectangle,
  0 = circle, 1 = rectangle. Existing shapeType semantics for ordinary buttons
  are unchanged. Use the existing checkbox and independent width/height sliders
  for touchpads/regions, without changing their dimensions on shape selection.
- A circular control is a centered circle with diameter min(width,height),
  both for painting and new-gesture hit testing. Editor selection keeps the
  full bounds. Gestures starting inside remain captured outside the circle.
  Circular passthrough corners remain usable by the camera beneath.

## Validation planned

Robolectric tests on Android 28/34 with actual controls and native dispatch:
three different controls, same-control owner lift, pointer order changes,
secondary lift, cancellation, lock/drag interference, circular hit testing,
region routing with the actual Game listener, and old/new JSON round trips.
Existing timing, Attack + camera, opacity and passthrough tests remain enabled.
Physical stream validation follows the APK delivery.

## Implemented changes

- touchShape uses the existing editor checkbox (checked = rectangular,
  unchecked = circular), localized in Spanish/Chinese/English. It is also
  exposed for the existing mouse buttons. Old ordinary button shapes are kept.
- Programmatic binding of zoom sliders no longer recalculates imported pixel
  dimensions; selecting a control or changing its shape preserves its geometry.
- The base active control captures its initiating pointerId and presents its
  existing input callback with an owner-only MotionEvent. It retains MOVE
  history, metadata and times; the ordinary split single-pointer path reuses
  the original event. Owner POINTER_UP is normalized to UP, never to a new tap.
  Editing still retains its guard. Attack + camera keeps its proven gesture.
- Slide-across-button handling ignores targets independently held by a finger
  or locked down, so another button's release cannot release their input.
- Android's same-View native capture still groups extra pointers inside the
  View's bounding box. These controls intentionally consume and ignore extra
  fingers rather than reassign them mid-gesture. Separate control Views keep
  independent captures. This is ownership stability, not pinch/two-finger
  camera gestures. The normal stream touch modes are unchanged.

References:
- https://developer.android.com/reference/android/view/ViewGroup#setMotionEventSplittingEnabled(boolean)
- https://developer.android.com/reference/android/view/MotionEvent (pointer IDs, indexes and batching)
