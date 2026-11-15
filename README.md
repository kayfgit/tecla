# Tecla

An Android keyboard that combines the best parts of the iPhone keyboard, Samsung
Keyboard and Unexpected Keyboard.

## Features (v0.2.0)

- iPhone-style layout in a blackout (pure black) theme.
- Space-bar trackpad for moving the cursor and selecting text.
- Delete key with corner swipes: words back/forward, letters forward, line back.
- Layouts: QWERTY, QWERTZ, AZERTY, Dvorak, Colemak, Colemak-DH and Workman.
- Corner swipes (Unexpected Keyboard style): numbers, symbols and accent dead keys
  in the letters' corners, and an Fn key with Alt, keyboard switching, emoji and
  settings. A Ctrl key for shortcuts.
- Hold any letter, number or symbol to repeat it.
- Fn page with F1-F12, Esc, Tab, Home/End, Page Up/Down and arrows.
- Emoji picker with recents.

See [CHANGELOG.md](CHANGELOG.md) for details.

## Build and install

Requires JDK 17 and the Android SDK (platform 34).

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open **Tecla** on the phone, enable it, and switch to it.

## Checks

```bash
./gradlew ktlintCheck testDebugUnitTest lintDebug
```

Use `./gradlew ktlintFormat` to auto-format.

## Code layout

- `model/` – key layouts and touch geometry (pure Kotlin, unit-tested).
- `logic/` – shift, accents, text-deletion rules, emoji data and gesture trackers (pure Kotlin, unit-tested).
- `ui/KeyboardView.kt` – drawing and touch handling; `ui/EmojiPane.kt` – the emoji picker.
- `TeclaImeService.kt` – the input method service that talks to apps.

## Emoji data

`app/src/main/assets/emoji.txt` is generated from Unicode's emoji list:

```bash
python tools/generate_emoji.py path/to/emoji-test.txt
```

Download `emoji-test.txt` from <https://unicode.org/Public/emoji/latest/emoji-test.txt>.

## License

Copyright (C) 2026 Kayf

tecla is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version. See [LICENSE](LICENSE).
