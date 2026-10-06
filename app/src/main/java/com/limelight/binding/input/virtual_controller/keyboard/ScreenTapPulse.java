package com.limelight.binding.input.virtual_controller.keyboard;

/** A short confirmed click/contact, released on time or when its owner cancels. */
final class ScreenTapPulse {
    static final long HOLD_MS = 100;
    interface Sink { Runnable begin(Object owner, float x, float y); }
    private final TimedKeyCombination.Scheduler scheduler;
    private final Sink sink;
    private Object owner;
    private Runnable release;
    private final Runnable finish = this::cancel;

    ScreenTapPulse(TimedKeyCombination.Scheduler scheduler, Sink sink) {
        this.scheduler = scheduler; this.sink = sink;
    }
    void tap(Object owner, float x, float y) {
        // Rapid taps end the previous pulse; they never queue behind it.
        cancel();
        Runnable end = sink.begin(owner, x, y);
        if (end == null) return;
        this.owner = owner;
        release = end;
        scheduler.postDelayed(finish, HOLD_MS);
    }
    void cancel(Object owner) { if (this.owner == owner) cancel(); }
    void cancel() {
        scheduler.remove(finish);
        Runnable end = release;
        release = null; owner = null;
        if (end != null) end.run();
    }
}
