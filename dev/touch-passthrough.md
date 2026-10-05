# Touch passthrough: input review and design before implementation

The stream and custom controls currently share a FrameLayout. Android's native
motion-event splitting assigns each new pointer to the topmost accepting child.
Existing controls retain their secondary-pointer guard; Attack + camera opts
into pointerId ownership. StreamView and the letterbox background already use
Game.onTouch, which implements the configured trackpad/absolute/native touch
mode. Game's ordinary touch contexts depend on the pointer indexes of that
native split stream, so manually forwarding events from separate region Views
would risk merging incorrectly or stealing UP events.

A transparent View returning false cannot punch a hole through a camera pad.
Instead the new root TouchRoutingLayout knows the rectangular passthrough areas.
For DOWN/POINTER_DOWN inside an active area it temporarily prioritizes the normal
StreamView (or its existing background listener in letterboxing) in Android's
child hit test. The normal framework dispatcher still splits/merges pointers,
transforms coordinates and preserves history. Rendering and persistent child
order are unchanged. The target is captured until UP/CANCEL even if a finger
crosses the region boundary. No synthetic per-finger forwarding is needed.

All custom controls disable passthrough routing while either editor is active.
Regions use a new bean type, existing geometry/opacity fields and Gson import/
export. They default to 0% opacity but draw a labeled cyan rectangle in editing;
nonzero opacity can show the region during play. They produce no virtual keys.
The safe-release path cancels an ongoing passthrough gesture before hiding or
recreating controls and suppresses remaining events until a fresh DOWN.

Framework dispatch tests will cover overlap, pointer splitting/merging, editing,
capture across borders, Z-order, letterboxing and cancellation. Existing Android
stream touch behavior and advanced multitouch within legacy controls remain as
before. Device/Genshin testing is required for the streamed effect.

References:
- https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r5/core/java/android/view/ViewGroup.java
- https://developer.android.com/reference/android/view/ViewGroup#setMotionEventSplittingEnabled(boolean)
