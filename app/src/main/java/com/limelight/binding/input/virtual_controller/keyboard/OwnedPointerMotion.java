package com.limelight.binding.input.virtual_controller.keyboard;

import android.view.MotionEvent;

/** Adapts a captured legacy control to one pointer without changing root dispatch. */
final class OwnedPointerMotion {
    private OwnedPointerMotion() {}

    // The caller recycles the result only if it differs from the original event.
    static MotionEvent obtain(MotionEvent event, int index, int action) {
        if (event.getPointerCount() == 1 && event.getAction() == action) return event;
        MotionEvent.PointerProperties[] props = {new MotionEvent.PointerProperties()};
        event.getPointerProperties(index, props[0]);
        MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
        int history = action == MotionEvent.ACTION_MOVE ? event.getHistorySize() : 0;
        long firstTime;
        if (history > 0) {
            event.getHistoricalPointerCoords(index, 0, coords[0]);
            firstTime = event.getHistoricalEventTime(0);
        } else {
            event.getPointerCoords(index, coords[0]);
            firstTime = event.getEventTime();
        }
        MotionEvent owned = MotionEvent.obtain(event.getDownTime(), firstTime, action, 1,
                props, coords, event.getMetaState(), event.getButtonState(),
                event.getXPrecision(), event.getYPrecision(), event.getDeviceId(),
                event.getEdgeFlags(), event.getSource(), event.getFlags());
        for (int sample = 1; sample < history; sample++) {
            event.getHistoricalPointerCoords(index, sample, coords[0]);
            owned.addBatch(event.getHistoricalEventTime(sample), coords, event.getMetaState());
        }
        if (history > 0) {
            event.getPointerCoords(index, coords[0]);
            owned.addBatch(event.getEventTime(), coords, event.getMetaState());
        }
        return owned;
    }
}
