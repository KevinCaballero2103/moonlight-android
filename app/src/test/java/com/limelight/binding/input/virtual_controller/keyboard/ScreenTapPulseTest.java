package com.limelight.binding.input.virtual_controller.keyboard;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
public class ScreenTapPulseTest {
    private static class Fixture implements TimedKeyCombination.Scheduler {
        final List<String> events = new ArrayList<>();
        Runnable pending;
        boolean reject;
        final ScreenTapPulse pulse = new ScreenTapPulse(this, (owner, x, y) -> {
            if (reject) return null;
            events.add("down:" + owner);
            return () -> events.add("up:" + owner);
        });
        public void postDelayed(Runnable task, long delay) { assertEquals(100, delay); pending = task; }
        public void remove(Runnable task) { if (pending == task) pending = null; }
        void finish() { Runnable task = pending; if (task != null) task.run(); }
    }
    @Test public void pulseEndsExactlyOnce() {
        Fixture f = new Fixture(); f.pulse.tap("one", 1, 2);
        assertEquals(List.of("down:one"), f.events);
        f.finish(); f.finish(); f.pulse.cancel();
        assertEquals(List.of("down:one", "up:one"), f.events);
    }
    @Test public void rapidTapsEndOldPulseWithoutQueuing() {
        Fixture f = new Fixture(); f.pulse.tap("one", 1, 2); f.pulse.tap("two", 3, 4);
        assertEquals(List.of("down:one", "up:one", "down:two"), f.events);
        f.finish(); assertEquals("up:two", f.events.get(3));
    }
    @Test public void cancellingAnotherOwnerDoesNotReleasePulse() {
        Fixture f = new Fixture(); Object owner = new Object();
        f.pulse.tap(owner, 1, 2); f.pulse.cancel(new Object());
        assertEquals(1, f.events.size()); f.pulse.cancel(owner);
        assertEquals(2, f.events.size()); assertNull(f.pending);
    }
    @Test public void rejectedStartDoesNotScheduleRelease() {
        Fixture f = new Fixture(); f.reject = true; f.pulse.tap("one", 1, 2);
        assertTrue(f.events.isEmpty()); assertNull(f.pending);
    }
}
