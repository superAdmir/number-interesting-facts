# Release checklist — Number interesting facts (`com.nip.numberinterestingfacts`)

Nothing in this checklist has been uploaded or published. Every item marked **Owner** needs Play
Console / AdMob access or a decision that cannot be verified from the repository.

## 1. Identity and signing
- [x] `applicationId` / `namespace` = `com.nip.numberinterestingfacts` (unchanged; matches the Play listing).
- [x] Launcher component `.RandomActivity` unchanged **from 1.5** — **not verified against 1.7** (source missing).
- [x] Play upload certificate (owner, Play Console): SHA-256
      `36:E8:C8:8B:E3:D2:04:1F:E2:DE:6C:9C:9D:62:DB:9C:38:FA:1E:7F:83:72:7A:D6:16:C4:57:70:7E:5A:51:42`.
      Not the key that signed the committed 1.5 bundle (`84:8C:25:25:…:AB:E8`).
- [ ] **Owner:** confirm which app-signing certificate is current (`AA:4B:BD:80:…` or `77:DB:35:A1:…`).
- [x] Old upload keystore lost (owner). NEW upload key created locally 2026-09-26:
      `/Users/AdmirSatara/number-interesting-facts-upload-key/` (see RESTORE.txt there).
      NEW certificate SHA-256 `7A:EC:75:B0:9E:C5:02:0A:0D:56:F8:8B:B3:C1:A7:53:FF:F0:F8:BF:74:77:1D:C7:12:E6:C6:3A:E8:42:32:56`
      — **not registered with Play yet**.
- [ ] **Owner: back up the whole folder** `/Users/AdmirSatara/number-interesting-facts-upload-key/`
      to an independent, encrypted location (the `backup-2026-09-26/` subfolder is on the same disk).
- [ ] **Owner: submit the upload-key reset** (admin permission needed):
      Play Console → select the app → Protected with Play → Play Store protection → **Manage Play app
      signing** → Upload key certificate → **Request upload key reset** → reason: lost key → upload
      `/Users/AdmirSatara/number-interesting-facts-upload-key/nif-upload-certificate.pem` → submit.
      Do **not** use "upgrade app signing key" — that changes the app-signing key.
- [ ] Wait for Google's confirmation (email / App integrity page shows the new upload certificate
      SHA-256 `7A:EC:75:B0:…:32:56` and when it takes effect).
- [ ] Then set `RESET_STATUS=confirmed` in `tools/signing/upload-key-status.properties`, rebuild, and
      run `tools/signing/verify-upload-key.sh` and `tools/signing/verify-signed-bundle.sh` (both exit 0).
- [x] Signed 18 / 2.0 bundle built with the new key — "signed with new upload key — Play reset pending".

## 2. Version
- [x] Owner-confirmed: production 17 / "1.7", highest uploaded 17 (earlier 16 / 1.6, 15 / 1.5).
- [x] `app/build.gradle`: versionCode **18**, versionName **"2.0"** (verified in the built APK and AAB).

## 3. Upgrade and data retention
- [ ] **Blocked: production 1.7 not available.** Download from Play Console → App bundle explorer →
      version 17 → Downloads: the **original app bundle** and the **signed universal APK**. Needed to
      compare storage (preferences, databases, files), backup rules, permissions, components and features.
- [ ] Then: debug-signed 1.7 → 2.0 upgrade test with representative data on a separate test
      device/emulator (not the owner's phone app).
- [x] Only covered so far: 1.5 (no app data) → 2.0-era build, debug-signed, in place. **Not evidence for 1.7.**
- [ ] Internal-testing track: install 1.7 from Play, then update to 2.0 from Play, without uninstalling —
      the only real Play-signed upgrade test.

## 4. Device support (compare in Play Console → Release → App bundle explorer)
- [x] Configuration comparison **against 1.5 only**: minSdk 26, permissions, `uses-feature` (faketouch
      only), supported screens and native code (none) match the 1.5 bundle. **1.7 not compared.**
      All library minSdk values ≤ 26. This is not a Play device-catalog check.
- [x] targetSdk 34 → **36** (required for updates since 2026-08-31).
- [ ] **Owner:** after uploading to internal testing, compare the "Supported devices" count and the
      excluded-devices list with the current production release.

## 5. AdMob production configuration and consent
- [x] Release IDs (owner-provided, configured only in `app/build.gradle`):
      app `ca-app-pub-6402675413704299~8756323389`;
      Random banner `…/3830574382`, Year banner `…/2325921024`, Date banner `…/6579757155`,
      Math banner `…/1395982738`, interstitial `…/7346043914`.
- [x] Debug builds use Google sample IDs only (verified in the built APKs and merged manifests).
- [ ] **Owner (AdMob):** confirm each unit exists, is the right format (4 banners + 1 interstitial),
      and belongs to this app. Note 1.5 used the Year banner ID for interstitials; that is fixed.
- [ ] **Owner (AdMob → Privacy & messaging):** publish a **GDPR (EEA/UK/CH) message** and, if you
      serve US users, a **US state regulations message** for this app. Without a published message
      UMP shows no form and `canRequestAds()` is true, i.e. ads run without a consent prompt.
- [ ] Understand the behaviour: `canRequestAds()` is true for NOT_REQUIRED and OBTAINED; OBTAINED
      includes "Do not consent", after which the app still requests ads and Google serves only what
      the TCF signals allow (e.g. limited ads). This is intended UMP behaviour, not a bug.
- [ ] **Owner:** check `app-ads.txt` on the developer website listed in Play (AdMob → Apps → app-ads.txt).
- [ ] Do **not** launch the release build on a device for testing: it requests live ads.

## 6. Policy declarations (Play Console → App content)
- [ ] **Privacy policy (release-blocking):** the app now requests content from Wikipedia
      (`en.wikipedia.org`, Wikimedia Foundation), which receives the device's IP address and the
      app's User-Agent. Add this and the ads/consent handling to the policy (in-app
      `TermsAndCondition.txt` and the URL in Play). The in-app text is unchanged from 1.5 on purpose:
      legal wording is the owner's decision.
- [ ] **Data safety:** "Data is encrypted in transit" is now true for the app's own traffic (HTTPS
      only, `usesCleartextTraffic=false`). Re-check data types for the Ads SDK (device/advertising ID,
      app interactions, diagnostics, approximate location) and whether Wikipedia requests need
      declaring: https://developers.google.com/admob/android/privacy/play-data-disclosure
- [ ] **Content licence:** keep the in-app attribution ("Source: Wikipedia, “Title” · CC BY-SA 4.0")
      and the Open-source licenses screen; if you add Wikipedia text to store listings or screenshots,
      attribute it too.
- [ ] **Ads declaration:** "Contains ads" = Yes (unchanged).
- [ ] **Advertising ID declaration:** Yes, used for advertising (`AD_ID` permission merged from the SDK, as in 1.5).
- [ ] **Content rating / target audience:** unchanged content type (educational number facts); keep
      current answers. If the target audience includes children, ad settings must be revisited.

## 7. Release build verification
```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew clean :app:testDebugUnitTest :app:lintRelease :app:bundleRelease
```
- [x] `bundleRelease` builds (R8 + resource shrinking) → `app/build/outputs/bundle/release/app-release.aab`
      — **unsigned, not upload-ready**.
- [x] R8 behaviour exercised with the separate **QA build type** (release config + sample ad IDs +
      debug key, `app/build/outputs/apk/qa/app-qa.apk`): found and fixed a start-up crash
      (WorkManager/Room constructor removed by R8; keep rule in `proguard-rules.pro`). Never upload
      the QA APK; it says nothing about Play signing or upgrading a Play-installed copy.
- [ ] After signing, keep the rule `-keep class * extends androidx.room.RoomDatabase { <init>(); }`
      in `proguard-rules.pro` unless the Mobile Ads SDK moves to a WorkManager/Room with correct rules.
- [ ] Sign with the upload key, then verify signature (section 1).
- [ ] Optionally inspect with bundletool / App bundle explorer.

## 8. Future upload steps (owner, manual)
1. Complete sections 1–6.
2. Upload the signed bundle to **Internal testing** first; install from Play on a device that has the
   live version; confirm upgrade, consent form (EEA test device), Wikipedia facts in all four
   categories, banners, and one interstitial after ≥ 3 minutes and 4 category switches.
3. Paste release notes from `RELEASE_NOTES.md`.
4. Promote to production with a staged rollout (e.g. 20 %), watch Android vitals.
5. Store icon: upload `docs/store-assets/play-icon-512.png` (512×512, opaque, no rounded corners).
   Feature graphic (1024×500) still to be made.
6. Screenshots: pending — see `MODERNIZATION_REPORT.md` § 11 (ready-to-run capture; Play needs
   aspect ratio ≤ 2:1, native 1080×2340 captures must be cropped/padded in the final graphics).
