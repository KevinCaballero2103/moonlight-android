#!/usr/bin/env sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir=$(mktemp -d /tmp/axixi-virtual-input.XXXXXX)
main_dir="$project_dir/app/src/main/java/com/limelight/binding/input/virtual_controller/keyboard"
test_dir="$project_dir/app/src/test/java/com/limelight/binding/input/virtual_controller/keyboard"

# Uses the JDK compiler module directly so a javac launcher is not required.
java com.sun.tools.javac.Main --release 11 -d "$classes_dir" \
    "$main_dir/VirtualInputState.java" "$main_dir/TimedKeyCombination.java" \
    "$test_dir/TimedKeyCombinationChecks.java"
java -cp "$classes_dir" com.limelight.binding.input.virtual_controller.keyboard.TimedKeyCombinationChecks
