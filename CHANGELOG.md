# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Holding a letter, number or symbol keeps typing it until you let go: after
  400ms, about 30 a second. Holding on an accent corner repeats the accent
  mark itself (^^^^), and holding on a symbol corner repeats the symbol.
- Numbers and symbols in the letters' corners (Unexpected Keyboard layout):
  1–0 in the top-right corners of q–p, symbols around them, Esc under q and
  Tab above a.

### Fixed
- The keyboard no longer sinks over the navigation bar and its switch/hide
  buttons. Before, if the bar was hidden for a moment (a fullscreen app or a
  transition), the keys could stay low until you switched keyboards.
- Selecting text with the space bar (slide or long-press space, then tap a
  second finger) now works in text fields: the arrow keys sent while
  selecting are wrapped in a real Shift press.
- Shift always cycles one capital → caps lock → off, however fast you tap.
  Before, quick triple taps left caps lock on.

### Changed
- tecla is now licensed under the GNU GPL v3 or later (GPL-3.0-or-later), copyright Kayf.
- Targets Android 17 (API 37). Builds with Android Gradle Plugin 9.5.1
  (built-in Kotlin), Gradle 9.9.0 and ktlint Gradle plugin 14.2.0.
- On Android 15 and later the keyboard fills the area behind the navigation
  bar and its switch/hide buttons with the theme background, keeping the keys
  above them.
- Accent hints in key corners are larger, bold and brighter, drawn as the
  plain accent marks (` ´ ^ ~ ¸ ˇ ¯ ¨) instead of small dotted-circle glyphs.
- The return key is smaller (the space bar is wider) and shows a return
  arrow instead of the word "return". Go, search, send, done, next and
  previous still show their words.
- The space-bar cursor moves about 15% faster (13dp per character, 35dp per
  line).
- Holding delete follows the Samsung Keyboard (measured on a Galaxy A36): it
  starts after 400ms, deletes about 30 letters a second, then speeds up to
  about 75 a second. Swiping delete up repeats the same way.
- The bottom-left key is a plain Ctrl key (tap it, then press a key for
  Ctrl+C, Ctrl+V…).
- Shift no longer turns ß into "SS".

### Removed
- The 123 key and the numbers/symbols pages; their characters are in the
  letter corners now. Number, phone and date fields still open a numbers
  page on their own.
- Holding delete no longer switches to deleting whole words; swipe left on
  delete for words.
- £, ¥ and • (they were only on the symbols page).

## [0.2.0] - 2026-11-08

### Added
- Corner swipes (Unexpected Keyboard style): small symbols in a key's corner are
  used by swiping from the key toward that corner.
- Accent dead keys under a, s, d, f, g, h, l and above u: grave, acute,
  circumflex, tilde, cedilla, caron, macron and diaeresis. Pick one, then type
  the letter (´ then e = é).
- Fn key in place of the globe. Tap for a page with F1–F12, Esc, Tab, Home, End,
  Page Up/Down, Insert, arrows and forward delete (arrows and delete repeat when
  held). Hold to pick a keyboard. Corners: Alt, switch keyboard, emoji, settings.
- One-shot Ctrl (bottom-right of the 123 key) and Alt modifiers for shortcuts
  such as Ctrl+C, Ctrl+V and Ctrl+Z.
- Emoji picker with category tabs and recent emoji, built from Unicode's
  emoji-test.txt by `tools/generate_emoji.py`. Emoji the phone's font cannot
  draw are hidden.
- Blackout theme (pure black), now the default for the keyboard and the app.

### Changed
- Renamed from Blend Keyboard to Tecla (package `app.tecla.keyboard`).
- Delete swipes now delete once, then repeat with the same timing as holding
  delete while the finger stays out, instead of deleting more with distance.
- Delete key redrawn with Unexpected Keyboard style side icons for each swipe;
  shift and delete keys are wider.
- Space-bar cursor moves slower (15dp per character, 40dp per line).
- The setup screen's test box now has a visible border, cursor and selection
  highlight.

### Removed
- Double-tap space to type a period.
- Light and dark themes that followed the system (blackout replaces them for now).

## [0.1.0] - 2026-11-06

### Added
- First demo of Tecla, an Android keyboard (input method).
- iPhone-style layout and look: QWERTY letters, "123" and "#+=" pages, globe,
  space and return keys, light and dark themes that follow the system.
- Key-press balloons, haptic feedback, and fast two-thumb typing (a key is
  committed as soon as the next finger lands).
- Space-bar trackpad: slide or long-press the space bar to move the cursor like
  arrow keys, in all four directions; tap a second finger to select text.
- Multi-mode delete key: tap or hold to delete letters (switches to words when
  held), swipe left/right to delete words backwards/forwards, swipe up to delete
  letters forwards, swipe down to delete to the start of the line.
- iPhone-style shift (tap, double-tap for caps lock, hold to type capitals),
  auto-capitalization, and double-tap space for a period.
- Return key that shows the field's action (go, search, send, next, done).
- Setup screen with buttons to enable and switch to the keyboard, a test box,
  and a gesture guide.
- Tooling: ktlint, Android lint, unit tests, GitHub Actions CI.
