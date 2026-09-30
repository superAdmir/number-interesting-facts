> **Superseded (2026-09-25, session 5):** these captures predate layout fixes (Math chip no longer
> clipped, content no longer under the 3-button navigation bar) and include Samsung's Edge-panel
> handle, a system overlay. Do not use for store graphics; they will be replaced by a new capture.

# Number interesting facts — current UI screenshots (review captures)

Authentic captures of the current app UI for preparing Google Play graphics. Unedited PNGs at the
phone's native resolution: no cropping, scaling, frames or captions.

| File | Screen | Theme | Content shown | Size |
|---|---|---|---|---|
| 01-random-42-dark.png | Main — Random (Number trivia) | Dark | 42: Wikipedia “42 (number)” | 1080×2340 |
| 02-math-1729-dark.png | Main — Math (Math trivia) | Dark | 1729: Wikipedia “1729 (number)” | 1080×2340 |
| 03-year-1969-dark.png | Main — Year (Year in history) | Dark | 1969: Wikipedia “1969” | 1080×2340 |
| 04-year-1969-light.png | Main — Year (Year in history) | Light | 1969: Wikipedia “1969” | 1080×2340 |
| 05-date-march14-dark.png | Main — Date (On this day) | Dark | March 14: Wikipedia “March 14” | 1080×2340 |
| 06-settings-light.png | Settings | Light | — | 1080×2340 |
| 07-settings-dark.png | Settings | Dark | — | 1080×2340 |

Capture configuration (QA/debug only; production behaviour unchanged):
- Device: Samsung SM-S711B, Android 16, 1080×2340 px, 450 dpi, 3-button navigation, system font size.
- Build: debug build of version 1.5 with `-PscreenshotMode=true`, which disables ads in that debug
  build only (no ads or test ads visible; release ads unaffected). No consent dialog is shown in it.
- Method: instrumented test `ScreenshotCaptureTest` (UiAutomation screenshot), captured 2026-09-25.
  The status bar is hidden inside the app's own window during capture, so no notifications appear;
  no system setting was changed. The system navigation bar is visible at the bottom.
- Content: live English Wikipedia excerpts (CC BY-SA 4.0), attributed on screen; the boxed
  "Calculated on your device" line is the app's labelled calculated fact.

Note for store use: Google Play phone screenshots need an aspect ratio of at most 2:1; these are
2.17:1, so final graphics must crop or pad. Wikipedia text used in store graphics must keep its
attribution (CC BY-SA 4.0).
