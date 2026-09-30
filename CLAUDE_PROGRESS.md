# CLAUDE_PROGRESS — modernization handoff

Last updated: 2026-09-25 (session 2). Full evidence: `MODERNIZATION_REPORT.md`. Do not repeat the audit.

## Current state (session 3)
- Tested on SM-S711B / Android 16: interstitial with production pacing (debug) PASS; EEA consent
  REQUIRED → no SDK init, "Do not consent" → OBTAINED + canRequestAds true + sample banner served,
  non-EEA NOT_REQUIRED; shrunk QA build: start-up crash found and fixed (Room/WorkManager keep rule
  in proguard-rules.pro), then 12/12 UI+Wikipedia, consent and interstitial PASS.
- Fixed this session: stale-cache consent gating (AdsManager waits for this session's UMP check),
  consent check after process death, wrong-subject guards for Wikipedia titles, licence link in
  attribution, "Source:" spacing, banner removal when canRequestAds() turns false.
- Unit 46/46, lint 0 errors, release bundle builds unsigned; versionCode/versionName still 15 / 1.5.
- Screenshots (session 4): **7/7 captured, inspected and approved** →
  `docs/store-screenshots/current/*.png` + `docs/store-screenshots/nif-current-screenshots.zip`
  (report § 11). Capture test no longer injects touches (its tap had hit the nav-bar Home button).

## Session 5 (nav bar / keyboard / chips) — see report § 12
- Implemented: tappableElement insets (no content under 3-button bar), InsetAwareScrollView,
  keyboard reveal of the lookup card, wrapping text-only category chips, PixelCopy app-window capture.
- Verified on device: measurements PASS (nothing under the tappable bar; chips fit; 2.0 font wraps).
- Session 6: `NavBarInsetsTest#realTapsTypingAndKeyboardDismissal` PASS on device (keyboard reveal
  incl. error text, Back dismissal, real Show fact tap → 28, swipe + tap; 0 presses on nav bar).
- Session 7: screenshots v2 DONE — 7/7 captured (PixelCopy app window), inspected, approved →
  `docs/store-screenshots/current-v2/` + `docs/store-screenshots/nif-current-screenshots-v2.zip`.
  Gesture navigation remains untested. Current screenshots/ZIP are
  **superseded** (pre-fix layout, Edge handle).
- Gesture navigation: untested.

## Session 8 (launcher icon) — see report § 10
- New adaptive + monochrome + legacy icon from `tools/icon/generate_icon.py`
  (run: `python3 tools/icon/generate_icon.py <repo> docs/icon`, needs Pillow). Play icon:
  `docs/store-assets/play-icon-512.png`. Previews in `docs/icon/`. Old per-variant icons removed.
- Verified: build + lint; on-device Android render (`LauncherIconRenderTest`, guarded). The phone has
  the current ads-off debug build installed, so its launcher shows the new icon for that app.

## Session 9 (production baseline + signing) — see report § 13
- Version set: 18 / "2.0" (production is 17 / 1.7). 1.6/1.7 source/bundles NOT found (git, GitHub, this Mac,
  phone) → 1.7 → 2.0 upgrade/data safety unverified. Need from Play Console: version 17 original app bundle +
  signed universal APK; which app-signing cert is current.
- Upload cert (Play): 36:E8:C8:8B:…:51:42 ≠ committed 1.5 signer 84:8C:25:25:…:AB:E8. No upload keystore
  locally → signing blocked. Inert signing wiring added (signing.local.properties + env passwords,
  .gitignore, tools/signing/verify-upload-key.sh, tools/signing/verify-signed-bundle.sh).
- Unsigned 2.0 AAB validated with bundletool 1.18.3 (jar in scratchpad): 18 / 2.0, production IDs, no hooks.

## Session 10 (new upload key) — see report § 14
- NEW upload key: /Users/AdmirSatara/number-interesting-facts-upload-key/ (keystore .p12, PEM, password file,
  CREDENTIALS.txt, RESTORE.txt, backup-2026-09-26/). NEW SHA-256 7A:EC:75:B0:…:32:56 — reset PENDING;
  registered still 36:E8:C8:8B:…:51:42 (tools/signing/upload-key-status.properties).
- signing.local.properties (git-ignored) points to it; signed 18/2.0 AAB built + verified (exit 10 = pending).
- Owner next: back up the key folder independently; submit "Request upload key reset" with the PEM.

## Pending (in order)
1. DONE (session 4): screenshots. To redo: guard first (device reachable, unlocked, mCallState=0,
   foreground = launcher or this app — abort otherwise), then `ScreenshotCaptureTest` (report § 11).
2. Owner input: highest uploaded versionCode; privacy-policy wording (Wikipedia + ads); AdMob messages.
3. Optional: Android < 16 test.

## Device / environment notes
- Phone: SM-S711B over wireless adb (`adb-RZCX10M8W3L-M3EQcw._adb-tls-connect._tcp.`), connection drops often.
  Installed by this work: debug `com.nip.numberinterestingfacts` and `com.nip.numberinterestingfacts.test`
  (remove with `adb uninstall …` only if the owner asks). Session 1 set the phone's auto-rotate off
  (`accelerometer_rotation 0`) without recording the old value — owner informed.
- QA build: `./gradlew :app:assembleQa :app:assembleQaAndroidTest -PtestBuildType=qa`. The phone now
  has the **screenshot-mode debug** build (ads off) and its test APK installed; that app's own theme
  preference was left on Dark by interrupted runs (in-app setting only).
- Never use `connectedDebugAndroidTest` on the phone (it uninstalls the app); use `adb install -r` +
  `am instrument`. Never run the release build on a device (live ads). Never `pm clear`.
- Emulator 31.1.4 cannot boot the API 35 image; the API 30 AVD `nif_qa_api30` was deleted outside this
  session twice — do not recreate without asking.

## Changed files (vs HEAD 24e4741), session 2 additions marked *
- Build: `build.gradle`, `app/build.gradle` (AGP 9.4.1, target 36, deps incl. *jsoup 1.23.2, AdMob IDs,
  QA switches `-PumpDebugGeography`, `-PumpTestDeviceId`, `-PscreenshotMode`), wrapper, `gradle.properties`.
- Java: `RandomActivity`, `NumberFactsApp`, `SettingsActivity`, `DocumentActivity`, *`DebugOverrides`;
  `facts/` (FactCategory, FactQuery, *Fact, *FactResult, *FactRepository, *LocalFactGenerator);
  *`facts/wiki/` (WikiApi, WikiClient, WikiParser); `ads/` (AdsManager, InterstitialPacing);
  `settings/ThemePreferences`; `ui/` (EdgeToEdgeInsets, GridPaperDrawable). Deleted: 5 old Activities,
  *NumbersApiContract.
- Resources: layouts (random*, settings, document), menu, values (colors, themes, type, *strings, dimens),
  values-night/colors, values-w600dp, font/, drawables (*bg_supplement, *ic_refresh_24, ic_*),
  *assets/licenses/open_source_licenses.txt.
- Tests: unit — LocalFactGeneratorTest, *LocalFactGeneratorEdgeCaseTest, *WikiParserTest (+ *fixtures in
  `app/src/test/resources/wiki/`, CC BY-SA), *DatePickerConversionTest, LookupInputTest, InterstitialPacingTest,
  BannerMappingTest; instrumented — *MainScreenTest, *WikipediaContentTest, *AdsBehaviourTest, *TestUtil,
  ExampleInstrumentedTest.
- Docs: `MODERNIZATION_REPORT.md`, `RELEASE_CHECKLIST.md`, `RELEASE_NOTES.md`, this file,
  `docs/store-screenshots/` (1 outdated review capture).

## Backup of the pre-existing WIP
`refs/backup/wip-before-modernization` → `acd4eba` (verified present 2026-09-25; 17 app files).
Restore: `git stash apply refs/backup/wip-before-modernization` on a clean tree or new branch.

## Verification commands
```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:bundleRelease
export ANDROID_SERIAL=<device>
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.nip.numberinterestingfacts.MainScreenTest,com.nip.numberinterestingfacts.WikipediaContentTest com.nip.numberinterestingfacts.test/androidx.test.runner.AndroidJUnitRunner
adb logcat -d -s WikipediaContentTest:I     # what was displayed per lookup
```
