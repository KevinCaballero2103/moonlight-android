package com.limelight.binding.input.virtual_controller.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Dependency-free checks; also called by the normal JUnit suite. */
public final class TimedKeyCombinationChecks {
    private static final class Task {
        long due;
        long order;
        Runnable runnable;
    }

    private static final class Clock implements TimedKeyCombination.Scheduler {
        long now;
        long nextOrder;
        final PriorityQueue<Task> tasks = new PriorityQueue<>(
                Comparator.comparingLong((Task t) -> t.due).thenComparingLong(t -> t.order));

        public void postDelayed(Runnable runnable, long delayMs) {
            Task task = new Task();
            task.due = now + delayMs;
            task.order = nextOrder++;
            task.runnable = runnable;
            tasks.add(task);
        }

        public void remove(Runnable runnable) {
            tasks.removeIf(t -> t.runnable == runnable);
        }

        void to(long time) {
            while (!tasks.isEmpty() && tasks.peek().due <= time) {
                Task task = tasks.remove();
                now = task.due;
                task.runnable.run();
            }
            now = time;
        }
    }

    private static final class Fixture {
        final Clock clock = new Clock();
        final List<String> events = new ArrayList<>();
        final VirtualInputState input = new VirtualInputState(new VirtualInputState.Sink() {
            public void keyboard(int key, boolean down) {
                events.add(clock.now + ":K" + key + (down ? "D" : "U"));
            }

            public void mouse(int key, boolean down) {
                events.add(clock.now + ":M" + key + (down ? "D" : "U"));
            }
        });
        final TimedKeyCombination combo = new TimedKeyCombination(clock, input);

        void expect(String... expected) {
            if (!events.equals(Arrays.asList(expected))) {
                throw new AssertionError("Expected " + Arrays.toString(expected) + ", got " + events);
            }
        }
    }

    static void shortTapOrdersAltBeforeNumber() {
        Fixture f = new Fixture();
        f.combo.down("button", "8,57");
        f.combo.up("button");
        f.clock.to(300);
        f.expect("0:K57D", "40:K8D", "80:K8U", "120:K57U");
    }

    static void heldChordStaysDownUntilRelease() {
        Fixture f = new Fixture();
        f.combo.down("button", "57,8");
        f.clock.to(200);
        f.expect("0:K57D", "40:K8D");
        f.combo.up("button");
        f.clock.to(300);
        f.expect("0:K57D", "40:K8D", "240:K8U", "280:K57U");
    }

    static void allModifierVariantsAreRecognized() {
        int[] modifiers = {57, 58, 59, 60, 113, 114, 117, 118};
        for (int modifier : modifiers) {
            Fixture f = new Fixture();
            f.combo.down("button", "8," + modifier);
            f.combo.up("button");
            f.clock.to(200);
            f.expect("0:K" + modifier + "D", "40:K8D", "80:K8U", "120:K" + modifier + "U");
        }
    }

    static void multipleModifiersReleaseLast() {
        Fixture f = new Fixture();
        f.combo.down("button", "8,113,57,59,117");
        f.combo.up("button");
        f.clock.to(500);
        f.expect("0:K113D", "40:K57D", "80:K59D", "120:K117D", "160:K8D",
                "200:K8U", "240:K117U", "280:K59U", "320:K57U", "360:K113U");
    }

    static void lockedAltIsNotReleasedByChord() {
        Fixture f = new Fixture();
        f.input.keyboard("lockedAlt", 57, true);
        f.combo.down("button", "8,57");
        f.combo.up("button");
        f.clock.to(200);
        f.expect("0:K57D", "40:K8D", "80:K8U");
        f.input.keyboard("lockedAlt", 57, false);
        f.expect("0:K57D", "40:K8D", "80:K8U", "200:K57U");
    }

    static void overlappingChordsKeepSharedModifier() {
        Fixture f = new Fixture();
        f.combo.down("one", "57,8");
        f.combo.down("two", "57,9");
        f.combo.up("one");
        f.clock.to(120);
        f.expect("0:K57D", "40:K8D", "40:K9D", "80:K8U");
        f.combo.up("two");
        f.clock.to(300);
        f.expect("0:K57D", "40:K8D", "40:K9D", "80:K8U", "160:K9U", "200:K57U");
    }

    static void rapidTapsAreSerialized() {
        Fixture f = new Fixture();
        f.combo.down("button", "57,8");
        f.combo.up("button");
        f.clock.to(10);
        f.combo.down("button", "8,57");
        f.combo.up("button");
        f.clock.to(400);
        f.expect("0:K57D", "40:K8D", "80:K8U", "120:K57U",
                "160:K57D", "200:K8D", "240:K8U", "280:K57U");
    }

    static void cancellationBeforeNumberPreventsItsPress() {
        Fixture f = new Fixture();
        f.combo.down("button", "57,8");
        f.clock.to(20);
        f.combo.cancel("button");
        f.clock.to(500);
        f.expect("0:K57D", "20:K57U");
    }

    static void cancellationReleasesNumberBeforeAltImmediately() {
        Fixture f = new Fixture();
        f.combo.down("button", "57,8");
        f.clock.to(60);
        f.combo.cancel("button");
        f.clock.to(500);
        f.expect("0:K57D", "40:K8D", "60:K8U", "60:K57U");
    }

    static void cancellationDiscardsQueuedTapsAndAllowsReuse() {
        Fixture f = new Fixture();
        f.combo.down("button", "57,8");
        f.combo.up("button");
        f.combo.down("button", "57,8");
        f.combo.up("button");
        f.combo.cancelAll();
        f.clock.to(500);
        f.expect("0:K57D", "0:K57U");
        f.combo.down("button", "57,8");
        f.combo.up("button");
        f.clock.to(1000);
        f.expect("0:K57D", "0:K57U", "500:K57D", "540:K8D", "580:K8U", "620:K57U");
    }

    static void malformedCodesDoNotPartiallyPressKeys() {
        Fixture f = new Fixture();
        for (String codes : new String[]{null, "", "57,", "57,garbage", "57,-1", "57,0", "57,32768"}) {
            f.combo.down("button", codes);
            f.combo.up("button");
        }
        f.clock.to(500);
        f.expect();
    }

    static void whitespaceAndDuplicateKeysAreHandled() {
        Fixture f = new Fixture();
        f.combo.down("button", " 8 , 57 ,8,57 ");
        f.combo.up("button");
        f.clock.to(200);
        f.expect("0:K57D", "40:K8D", "80:K8U", "120:K57U");
    }

    static void nonModifierChordAndSingleKeyStillWork() {
        Fixture f = new Fixture();
        f.combo.down("button", "29,30");
        f.combo.up("button");
        f.clock.to(200);
        f.expect("0:K29D", "40:K30D", "80:K30U", "120:K29U");
        f.input.keyboard("single", 31, true);
        f.input.keyboard("single", 31, false);
        f.expect("0:K29D", "40:K30D", "80:K30U", "120:K29U", "200:K31D", "200:K31U");
    }

    static void duplicateDownAndUnmatchedUpAreHarmless() {
        Fixture f = new Fixture();
        f.combo.up("button");
        f.combo.down("button", "57,8");
        f.combo.down("button", "57,8");
        f.combo.up("button");
        f.combo.up("button");
        f.clock.to(200);
        f.expect("0:K57D", "40:K8D", "80:K8U", "120:K57U");
    }

    static void sharedMouseButtonAndReleaseAllAreSafe() {
        Fixture f = new Fixture();
        f.input.mouse("one", 1, true);
        f.input.mouse("two", 1, true);
        f.input.mouse("one", 1, false);
        f.expect("0:M1D");
        f.input.keyboard("alt", 57, true);
        f.input.releaseAll();
        f.input.releaseAll();
        f.expect("0:M1D", "0:K57D", "0:M1U", "0:K57U");
    }

    public static void main(String[] args) {
        shortTapOrdersAltBeforeNumber();
        heldChordStaysDownUntilRelease();
        allModifierVariantsAreRecognized();
        multipleModifiersReleaseLast();
        lockedAltIsNotReleasedByChord();
        overlappingChordsKeepSharedModifier();
        rapidTapsAreSerialized();
        cancellationBeforeNumberPreventsItsPress();
        cancellationReleasesNumberBeforeAltImmediately();
        cancellationDiscardsQueuedTapsAndAllowsReuse();
        malformedCodesDoNotPartiallyPressKeys();
        whitespaceAndDuplicateKeysAreHandled();
        nonModifierChordAndSingleKeyStillWork();
        duplicateDownAndUnmatchedUpAreHarmless();
        sharedMouseButtonAndReleaseAllAreSafe();
        System.out.println("15 input checks passed");
    }
}
