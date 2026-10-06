package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/** Modifier-first held chords. All entry points and callbacks run on the same thread. */
final class TimedKeyCombination {
    static final long STEP_MS = 40;

    interface Scheduler {
        void postDelayed(Runnable task, long delayMs);
        void remove(Runnable task);
    }

    private final Scheduler scheduler;
    private final VirtualInputState input;
    private final Map<Object, ArrayDeque<Session>> sessions = new HashMap<>();

    TimedKeyCombination(Scheduler scheduler, VirtualInputState input) {
        this.scheduler = scheduler;
        this.input = input;
    }

    // Android keycodes, kept literal so this state machine can run without Android.
    static boolean isModifier(int code) {
        return code == 57 || code == 58 || code == 59 || code == 60
                || code == 113 || code == 114 || code == 117 || code == 118;
    }

    static int[] parse(String codes) {
        if (codes == null || codes.trim().isEmpty()) {
            return new int[0];
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        try {
            for (String value : codes.split(",", -1)) {
                int code = Integer.parseInt(value.trim());
                if (code <= 0 || code > Short.MAX_VALUE) {
                    return new int[0];
                }
                unique.add(code);
            }
        } catch (NumberFormatException e) {
            return new int[0];
        }
        ArrayList<Integer> ordered = new ArrayList<>(unique);
        Collections.sort(ordered, (a, b) -> Boolean.compare(!isModifier(a), !isModifier(b)));
        int[] keys = new int[ordered.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = ordered.get(i);
        }
        return keys;
    }

    void down(Object owner, String codes) {
        int[] keys = parse(codes);
        if (keys.length == 0) {
            return;
        }
        ArrayDeque<Session> queue = sessions.get(owner);
        if (queue == null) {
            queue = new ArrayDeque<>();
            sessions.put(owner, queue);
        } else if (!queue.getLast().releaseRequested) {
            // Repeated down for an already held control is not a second chord.
            return;
        }
        Session session = new Session(owner, keys);
        queue.add(session);
        if (queue.size() == 1) {
            session.pressNext();
        }
    }

    void up(Object owner) {
        ArrayDeque<Session> queue = sessions.get(owner);
        if (queue == null) {
            return;
        }
        Session session = queue.getLast();
        if (session.releaseRequested) {
            return;
        }
        session.releaseRequested = true;
        if (session.pressed == session.keys.length) {
            // A single key needs no chord sequencing. Queuing its UP makes a
            // rapid re-press wait behind earlier taps (e.g. sprint on Ctrl).
            if (session.keys.length == 1) session.releaseNext();
            else session.schedule(session::releaseNext);
        }
    }

    void cancel(Object owner) {
        ArrayDeque<Session> queue = sessions.remove(owner);
        if (queue == null) {
            return;
        }
        for (Session session : queue) {
            session.cancelled = true;
            if (session.pending != null) {
                scheduler.remove(session.pending);
            }
            input.release(session);
        }
    }

    void cancelAll() {
        for (Object owner : new ArrayList<>(sessions.keySet())) {
            cancel(owner);
        }
    }

    private final class Session {
        final Object owner;
        final int[] keys;
        int pressed;
        int released;
        boolean releaseRequested;
        boolean cancelled;
        Runnable pending;

        Session(Object owner, int[] keys) {
            this.owner = owner;
            this.keys = keys;
        }

        void schedule(Runnable next) {
            pending = () -> {
                pending = null;
                if (!cancelled) {
                    next.run();
                }
            };
            scheduler.postDelayed(pending, STEP_MS);
        }

        void pressNext() {
            input.keyboard(this, keys[pressed++], true);
            if (cancelled) {
                return;
            }
            if (pressed < keys.length) {
                schedule(this::pressNext);
            } else if (releaseRequested) {
                schedule(this::releaseNext);
            }
        }

        void releaseNext() {
            input.keyboard(this, keys[keys.length - 1 - released++], false);
            if (cancelled) {
                return;
            }
            if (released < keys.length) {
                schedule(this::releaseNext);
                return;
            }
            ArrayDeque<Session> queue = sessions.get(owner);
            queue.removeFirst();
            if (queue.isEmpty()) {
                sessions.remove(owner);
            } else {
                // Preserve a key-up interval before a rapid repeat of this button.
                queue.getFirst().schedule(queue.getFirst()::pressNext);
            }
        }
    }
}
