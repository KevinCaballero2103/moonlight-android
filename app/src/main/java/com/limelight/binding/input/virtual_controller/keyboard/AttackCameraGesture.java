package com.limelight.binding.input.virtual_controller.keyboard;

/** One pointer owns a held mouse button and relative aiming until release/cancel. */
final class AttackCameraGesture {
    interface Sink {
        void button(boolean down);
        void move(int x, int y);
    }

    private final Sink sink;
    private int pointerId = -1;
    private float lastX, lastY;
    private double scaleX, scaleY;
    private double remainderX, remainderY;

    AttackCameraGesture(Sink sink) {
        this.sink = sink;
    }

    int getPointerId() {
        return pointerId;
    }

    boolean begin(int pointer, float x, float y, double xScale, double yScale) {
        if (pointerId != -1 || pointer < 0) return false;
        pointerId = pointer;
        lastX = x;
        lastY = y;
        scaleX = xScale;
        scaleY = yScale;
        remainderX = remainderY = 0;
        sink.button(true);
        return true;
    }

    void move(int pointer, float x, float y) {
        if (pointerId == -1 || pointer != pointerId) return;
        remainderX += (x - lastX) * scaleX;
        remainderY += (y - lastY) * scaleY;
        lastX = x;
        lastY = y;
        int deltaX = (int) remainderX;
        int deltaY = (int) remainderY;
        remainderX -= deltaX;
        remainderY -= deltaY;
        if (deltaX != 0 || deltaY != 0) sink.move(deltaX, deltaY);
    }

    void release(int pointer) {
        if (pointerId != -1 && pointer == pointerId) cancel();
    }

    void cancel() {
        if (pointerId == -1) return;
        pointerId = -1;
        remainderX = remainderY = 0;
        sink.button(false);
    }
}
