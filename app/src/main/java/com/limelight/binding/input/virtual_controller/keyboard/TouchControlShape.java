package com.limelight.binding.input.virtual_controller.keyboard;

/** Shared geometry for the painted shape and new-gesture hit testing. */
final class TouchControlShape {
    private TouchControlShape() {}

    static boolean contains(int shape, float width, float height, float x, float y) {
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x >= width || y >= height) {
            return false;
        }
        if (shape != 0) return true;
        float radius = Math.min(width, height) / 2f;
        float dx = x - width / 2f;
        float dy = y - height / 2f;
        return dx * dx + dy * dy <= radius * radius;
    }
}
