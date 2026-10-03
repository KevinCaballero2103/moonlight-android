package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Main-thread ownership of custom keyboard/mouse inputs, independent of Android Views. */
final class VirtualInputState {
    interface Sink {
        void keyboard(int keyCode, boolean down);
        void mouse(int button, boolean down);
    }

    private static final int MOUSE_FLAG = 0x10000;
    private final Sink sink;
    private final Map<Object, Set<Integer>> owners = new HashMap<>();
    private final Map<Integer, Integer> counts = new HashMap<>();

    VirtualInputState(Sink sink) {
        this.sink = sink;
    }

    void keyboard(Object owner, int keyCode, boolean down) {
        set(owner, keyCode, down);
    }

    void mouse(Object owner, int button, boolean down) {
        set(owner, MOUSE_FLAG | button, down);
    }

    private void set(Object owner, int input, boolean down) {
        Set<Integer> held = owners.get(owner);
        if (down) {
            if (held == null) {
                held = new LinkedHashSet<>();
                owners.put(owner, held);
            }
            if (!held.add(input)) {
                return;
            }
            Integer previous = counts.get(input);
            int count = previous == null ? 0 : previous;
            counts.put(input, count + 1);
            if (count == 0) {
                send(input, true);
            }
        } else if (held != null && held.remove(input)) {
            if (held.isEmpty()) {
                owners.remove(owner);
            }
            int count = counts.get(input) - 1;
            if (count == 0) {
                counts.remove(input);
                send(input, false);
            } else {
                counts.put(input, count);
            }
        }
    }

    void release(Object owner) {
        Set<Integer> held = owners.get(owner);
        if (held == null) {
            return;
        }
        ArrayList<Integer> reverse = new ArrayList<>(held);
        for (int i = reverse.size() - 1; i >= 0; i--) {
            set(owner, reverse.get(i), false);
        }
    }

    void releaseAll() {
        ArrayList<Integer> held = new ArrayList<>(counts.keySet());
        // All ordinary keys/buttons come up before any remaining modifiers.
        Collections.sort(held, (a, b) -> Boolean.compare(TimedKeyCombination.isModifier(a),
                TimedKeyCombination.isModifier(b)));
        owners.clear();
        counts.clear();
        for (int input : held) {
            send(input, false);
        }
    }

    private void send(int input, boolean down) {
        if ((input & MOUSE_FLAG) != 0) {
            sink.mouse(input & ~MOUSE_FLAG, down);
        } else {
            sink.keyboard(input, down);
        }
    }
}
