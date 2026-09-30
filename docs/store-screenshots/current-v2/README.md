# Number interesting facts — current UI screenshots, v2

Authentic captures of the current app UI (after the navigation-bar, keyboard and category-chip
fixes) for preparing Google Play graphics. Unedited PNGs: no cropping, scaling, frames or captions.
Supersedes the earlier `nif-current-screenshots.zip`.

| File | Screen | Theme | Content shown | Size |
|---|---|---|---|---|
| 01-random-42-dark.png | Main — Random (Number trivia) | Dark | 42: Wikipedia “42 (number)” | 1080×2340 |
| 02-math-1729-dark.png | Main — Math (Math trivia) | Dark | 1729: Wikipedia “1729 (number)” | 1080×2340 |
| 03-year-1969-dark.png | Main — Year (Year in history) | Dark | 1969: Wikipedia “1969” | 1080×2340 |
| 04-year-1969-light.png | Main — Year (Year in history) | Light | 1969 (Apollo 10): Wikipedia “1969” | 1080×2340 |
| 05-date-march14-dark.png | Main — Date (On this day) | Dark | March 14: Wikipedia “March 14” | 1080×2340 |
| 06-settings-light.png | Settings | Light | — | 1080×2340 |
| 07-settings-dark.png | Settings | Dark | — | 1080×2340 |

Capture configuration (QA/debug only; production behaviour unchanged):
- Device: Samsung SM-S711B, Android 16, 1080×2340 px, 450 dpi, 3-button navigation, system font size.
- Build: debug build of version 1.5 with `-PscreenshotMode=true`, which disables ads in that debug
  build only (no ads, test ads or empty ad slots). No consent dialog in this build.
- Method: instrumented test `ScreenshotCaptureTest`, captured 2026-09-25 with PixelCopy of the app's
  own window — only what the app renders. System overlays (status bar, navigation buttons,
  Samsung Edge panel handle, notifications) are not part of the app window and do not appear.
  The plain band at the bottom is the space the app reserves for the 3-button navigation bar;
  the top area is where the status bar sits. No system setting was changed.
- Content: live English Wikipedia excerpts (CC BY-SA 4.0), attributed on screen; the boxed
  "Calculated on your device" line is the app's labelled calculated fact.

Note for store use: Google Play phone screenshots need an aspect ratio of at most 2:1; these are
2.17:1, so final graphics must crop or pad. Wikipedia text used in store graphics must keep its
attribution (CC BY-SA 4.0).
