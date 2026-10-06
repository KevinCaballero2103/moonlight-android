package com.limelight.binding.input.virtual_controller.keyboard;

/** One finger: a brief stationary tap or relative camera motion, never both. */
final class TapCameraGesture {
    static final long MAX_TAP_MS = 250;
    interface Sink {
        void move(int x, int y);
        void tap(float x, float y);
    }
    private final Sink sink;
    private int pointer = -1;
    private float downX, downY, lastX, lastY;
    private long downTime;
    private double slopSquared, scaleX, scaleY, remainderX, remainderY;
    private boolean dragging;

    TapCameraGesture(Sink sink) { this.sink = sink; }
    int getPointerId() { return pointer; }
    void begin(int id, float x, float y, long time, float slop, double sx, double sy) {
        cancel();
        if (id < 0) return;
        pointer = id;
        downX = lastX = x; downY = lastY = y;
        downTime = time;
        slopSquared = Math.max(1, slop) * Math.max(1, slop);
        scaleX = sx; scaleY = sy;
    }
    void move(int id, float x, float y) {
        if (pointer == -1 || pointer != id) return;
        if (!dragging) {
            double dx = x - downX, dy = y - downY;
            if (dx * dx + dy * dy <= slopSquared) return;
            dragging = true;
        }
        // Include the initial motion when crossing the threshold. Retain fractions
        // after that, and never reclassify an out-and-back drag as a click.
        remainderX += (x - lastX) * scaleX;
        remainderY += (y - lastY) * scaleY;
        lastX = x; lastY = y;
        int dx = (int) remainderX, dy = (int) remainderY;
        remainderX -= dx; remainderY -= dy;
        if (dx != 0 || dy != 0) sink.move(dx, dy);
    }
    void release(int id, float x, float y, long time, boolean cancelled) {
        if (pointer == -1 || pointer != id) return;
        // UP can contain the only/final displacement; it must not become a tap.
        if (!cancelled) move(id, x, y);
        boolean tap = !cancelled && !dragging && time >= downTime
                && time - downTime <= MAX_TAP_MS;
        float px = downX, py = downY;
        cancel();
        if (tap) sink.tap(px, py);
    }
    void cancel() {
        pointer = -1;
        dragging = false;
        remainderX = remainderY = 0;
    }
}
