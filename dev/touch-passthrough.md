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

## Device regression: v6 direct touches did nothing (2026-10-05)

The user's reference test succeeds with the controls hidden at the same Genshin
quick-burst icon, but fails inside a region over the camera pad. The initial
review incorrectly assumed that Game binds a finger-touch listener to
StreamView. It actually binds only generic motion/key listeners there, and
binds Game.onTouch only to backgroundTouchView. A promoted passive SurfaceView
rejects DOWN; the camera pad can then consume the touch.

The root now promotes backgroundTouchView for every passthrough gesture, exactly
as the existing normal input path does. Game retains its existing mapping from
background coordinates to the video, normal absolute/relative/native touch
mode, mouse-disable setting, input grab and editor checks. No new mouse macro or
forced absolute mode is added. Native pointer splitting/merging/capture remains.

The dispatch fixture now uses a real passive StreamView, rather than an
accepting fake. New integration tests wire the actual Game.onTouch listener to a
background View and use actual AbsoluteTouchContext/RelativeTouchContext with a
recording NvConnection. They check the click position and down/up pair, parity
with the hidden overlay, simultaneous camera hold, letterboxed mapping, relative
mode, ungrabbed input and safe cancellation. The test-only reproduction commit
is built against the old routing before the correction is applied.
