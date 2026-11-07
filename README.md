# Blend Keyboard

An Android keyboard that combines the best parts of the iPhone keyboard, Samsung
Keyboard and Unexpected Keyboard.

## Features (v0.1.0)

- iPhone-style layout and look, light and dark.
- Space-bar trackpad for moving the cursor and selecting text.
- Delete key with swipe modes: words back/forward, letters forward, line back.
- iPhone-style shift, auto-capitalization, double-space period.

See [CHANGELOG.md](CHANGELOG.md) for details.

## Build and install

Requires JDK 17 and the Android SDK (platform 34).

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open **Blend Keyboard** on the phone, enable it, and switch to it.

## Checks

```bash
./gradlew ktlintCheck testDebugUnitTest lintDebug
```

Use `./gradlew ktlintFormat` to auto-format.

## Code layout

- `model/` – key layouts and touch geometry (pure Kotlin, unit-tested).
- `logic/` – shift, text-deletion rules and gesture trackers (pure Kotlin, unit-tested).
- `ui/KeyboardView.kt` – drawing and touch handling.
- `BlendImeService.kt` – the input method service that talks to apps.
