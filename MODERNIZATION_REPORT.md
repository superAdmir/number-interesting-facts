# Modernization report — Number interesting facts

Last updated 2026-09-25 · Package `com.nip.numberinterestingfacts` · Nothing was uploaded, published,
pushed or committed.

Status words used below: **implemented** = in the code; **tested** = verified with the evidence
named; **ready for release** = no open blocker. The app is implemented and tested on one Android 16 device (debug and shrunk QA builds) but
**not ready for release** (section 9).

> **Baseline correction (2026-09-26).** Play Console shows production **1.7 (versionCode 17)**;
> 16/1.6 and 15/1.5 were uploaded before it. This audit and the upgrade test used the 1.5 source
> and bundle in the repository — **not** production. The 1.6/1.7 source and bundles were not found
> (section 13), so nothing below proves 1.7 → 2.0 upgrade or data safety. The next release is set to
> **versionCode 18 / versionName 2.0**.

## 1. Audit baseline (before this work)

| Area | Finding |
|---|---|
| Repo | `github.com/superAdmir/number-interesting-facts`, `main` @ `24e4741`, with an uncommitted, half-finished redesign in the working tree (see section 8). |
| Identity | applicationId = namespace = `com.nip.numberinterestingfacts`; repository HEAD is versionCode 15 / versionName 1.5 (production is 17 / 1.7, source not in the repository); minSdk 26; target 34 at HEAD. No signing config in Gradle; no keystore in the repo. |
| Committed bundle | `app/release/app-release.aab` (committed 2024-09-27): 15 / 1.5, target 34, signed with the certificate `CN=Admir Satara, O=superadmir`, SHA-256 `84:8C:25:25:77:34:06:B1:93:FC:A2:D6:4B:A1:D7:6B:9C:13:A7:9F:B0:4F:8C:16:1E:59:29:35:31:51:AB:E8`. This is the certificate the bundle was signed with before upload; whether Play re-signs installs with a different app-signing key is unknown (section 7). |
| Features | Six Activities. Random (launcher), Year, Date (date picker) and Math were four near-identical copies: random fact on open, lookup, banner, exit icon → interstitial → `System.exit`. Menu showed the privacy policy; Exit asked for confirmation. |
| Fact source | Numbers API over cleartext HTTP: `http://numbersapi.com/{n\|random\|M/D}/{trivia\|year\|date\|math}?json`, JSON field `text`. Trivia/Year/Date were historical or cultural facts, Math were mathematical trivia. |
| Storage | None of the app's own: no SharedPreferences, database, DataStore, files, accounts or purchases. |
| Ads | Test IDs in source; production IDs only in the committed bundle. 1.5 requested interstitials with `…/2325921024`, which the owner identifies as the Year **banner** unit. Interstitial on app exit (disallowed by AdMob policy). `MobileAds.initialize` in every screen; no consent SDK. |
| Tests | Template stubs; the instrumented test asserted a wrong package name and could never pass. |
| Play listing | Education, Everyone, 50+ downloads, "Contains ads", updated Oct 1 2024. Data safety says "Data is encrypted in transit", which the cleartext traffic contradicted. |

### Numbers API investigation (evidence, no conclusions about ownership)
Probed 2026-09-25 15:02–15:03 UTC from this machine (curl 8.x, certificate verification on):

| Time (UTC) | Request | Result |
|---|---|---|
| 15:02:42 | `GET http://numbersapi.com/42/trivia?json` | HTTP 404, `Server: nginx/1.24.0 (Ubuntu)`, HTML body "404 Not Found", IP 159.65.220.83 |
| 15:02:43 | `GET http://numbersapi.com/random/year?json` | HTTP 404, same server/body |
| 15:02:43 | `GET http://numbersapi.com/3/14/date?json` | HTTP 404, same |
| 15:02:43 | `GET http://numbersapi.com/28/math?json` | HTTP 404, same |
| 15:02:44 | `GET https://numbersapi.com/42/trivia?json` | TLS failure, curl error 60: "no alternative certificate subject name matches target host name 'numbersapi.com'" |
| 15:02:44 | `openssl s_client -servername numbersapi.com` | Chain verifies (Let's Encrypt YE2, valid 2026-08-20 → 2026-11-18), but subject/SAN are `amanahtrader.uk`, `www.amanahtrader.uk` only → hostname check fails |

What this shows: the API paths the app used are not served at that address today, and no valid
HTTPS endpoint exists for the hostname. It does not show who controls the domain or why. Either
way, 1.5 cannot display facts, and restoring its cleartext requests is not acceptable.

## 2. Content source: restoring trivia and history

### Options compared (researched 2026-09-25)

| | A. Maintained HTTPS service: **Wikipedia via the MediaWiki Action API** | B. Bundled dataset | C. Hybrid |
|---|---|---|---|
| Coverage | Year: "Events" of year articles (checked 1–2026; AD 1 has no Events section; 2027+ none). Date: "Events" of all 366 date articles incl. February 29 (42 events for March 14, 43 for February 29). Numbers: individual articles for 0–~200 and many others (1729, 9999), range articles for 201–999 ("400 (number)" § 437); many 4-digit numbers have no article (e.g. 4567). | Found no dataset with verified redistribution rights. The Numbers API's own data/licence could not be found (the `divad12/numbersapi` repository no longer exists; GitHub search found only client apps). Wikidata is CC0 but gives structured statements, not readable trivia. A Wikipedia snapshot would be CC BY-SA but goes stale and grows the APK. | Wikipedia for content + on-device calculations as a labelled supplement/fallback. |
| Licensing | Text CC BY-SA 4.0; commercial use allowed; attribution by link to the article; indicate changes; licence link. https://en.wikipedia.org/wiki/Wikipedia:Reusing_Wikipedia_content | — | Same as A. |
| Reliability | Wikimedia production infrastructure. The REST "feed" (on-this-day) endpoints are being restructured from July 2026 (https://wikitech.wikimedia.org/wiki/API_Portal/Deprecation); the Action API used here is not on that schedule. | Offline, but frozen. | A's reliability; offline still gives a labelled result. |
| Maintenance | Parsing depends on article structure ("Events" section; `N (number)` titles). Covered by fixture tests; content changes do not need app updates. | Needs periodic regeneration and licence review. | As A. |
| Cost | Free, no key. Must send a descriptive User-Agent (https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_User-Agent_Policy) and make requests serially at human pace (https://www.mediawiki.org/wiki/API:Etiquette). | Free, larger APK. | Free. |

**Chosen: C (hybrid).** Wikipedia is the only source; calculated facts never replace it.

### Implementation
- `facts/wiki/WikiApi`: titles and URLs — year `AD {y}` (resolves to "1969" etc.; the resolved title
  must be exactly the year article, so far-future years that redirect to "3rd millennium" count as
  not found); date `March 14`; number `N (number)` with redirects (range articles use the number's
  own section via the redirect fragment).
- `facts/wiki/WikiClient`: HTTPS only (cleartext disabled app-wide; platform TLS validation, no
  custom trust), descriptive User-Agent with the developer's public support e-mail, 8 s timeout +
  1 retry, parsing on Volley's network thread, in-memory LRU cache (48 responses) per session.
- `facts/wiki/WikiParser` (jsoup 1.23.2): verbatim list items/paragraphs; removes citation markers,
  edit links, tables, figures; skips items containing math markup; keeps nested-list dates
  ("July 20 – …"); date events must start with a year; number excerpts must mention the number as a
  whole token (`1729` or `1,729`, not `17290`); long paragraphs cut at sentence boundaries (≤ 450 chars).
- Category behaviour, matching the original: Random → trivia from the number's non-math sections;
  Math → the mathematics section; Year → an event of that year; Date → an event on that day.
  Random picks: numbers 0–999, years 1000–(current − 1), any day incl. February 29; a random number
  without content is re-drawn up to 3 times.
- UI: headings "Number trivia / Year in history / On this day / Math trivia"; attribution line
  "Source: Wikipedia, “Title” · CC BY-SA 4.0 · Excerpt, citations removed", tappable (opens the
  article). A separate, bordered box shows the calculated fact labelled "Calculated on your device ·
  not a historical fact" (or "Calculated instead · not from Wikipedia" when there is no Wikipedia
  content). Honest states with a next action: not found ("Wikipedia lists no events for 2150. Try
  another year, or tap New random fact."), network error ("Couldn’t reach Wikipedia…" + **Try again**).
  Share/copy include the source and licence; calculated-only shares are marked "(calculated)".
- Removed: the Numbers API client and its build flag.
- Wrong-subject guards (session 3), based on a probe of how all `N (number)` titles for 0–999 and
  31 larger numbers resolve (2026-09-25): 357 own articles, 390 number sections of range articles,
  280 decade/range sections (e.g. 611 → "600 (number)#610s", 1984 → "1000 (number)#1900 to 1999"),
  4 missing. Rules: a number page must be titled `N (number)`/a range article, or exactly `N`
  without an "Events" section (so a year article can never supply number trivia); disambiguation
  pages (`prop=properties`) are rejected for numbers, years and dates; in a range article the
  number's own subsection is used when it exists ("611" inside "610s"), otherwise facts must *start*
  with the number; year and date pages must resolve to exactly the requested year/date. Unit-tested.
- Attribution (session 3): "Source: Wikipedia, “Title” · CC BY-SA 4.0 · Excerpt, citations removed"
  with two working links — the article (section anchor) and the licence
  (https://creativecommons.org/licenses/by-sa/4.0/). Both checked with HTTP 200 on 2026-09-25
  (1969#Events, March_14#Events, February_29#Events, 42_(number)#Popular_culture,
  1729_(number)#In_mathematics, 400_(number)#437, AD_5#Events, licence). The Open-source licenses
  screen explains the source, the changes made and the warranty disclaimer, with tappable links.
  Governing guidance: CC BY-SA 4.0 §3(a) (identify creators, licence notice + link, disclaimer
  notice, link to the material, indicate modifications; "in any reasonable manner", e.g. by link)
  https://creativecommons.org/licenses/by-sa/4.0/legalcode.en and Wikipedia's reuse guide (a link to
  the article satisfies author attribution) https://en.wikipedia.org/wiki/Wikipedia:Reusing_Wikipedia_content

### Limitations
- English only; needs internet for trivia/history (offline shows the labelled calculated fact + retry).
- Coverage gaps: numbers without articles (many above 999), AD 1, future years; shown as not found.
- Wikipedia content changes over time and can contain errors or recently edited text; each fact links
  its source. Some year articles list future/"scheduled" items only in future-year articles, which the
  app treats as not found (resolved title must match).
- Licence note: excerpts stay CC BY-SA 4.0 and are attributed as above. That the rest of the app is
  not an adaptation requiring CC BY-SA is the common reading of Wikimedia's reuse guidance, but it is
  a legal judgement for the owner, not something verified here.
- Privacy: the app now contacts `en.wikipedia.org` (IP address and User-Agent reach Wikimedia).
  Privacy policy and Data safety review needed (checklist § 6).

## 3. Calculated facts: validation and limits
Implemented in `facts/LocalFactGenerator`, now only a labelled supplement/fallback.
- Documented input limits: numbers 0–9999, years 1–9999 (UI validates the same ranges; input field
  accepts digits only, max 4); any month/day incl. February 29. Out-of-range → `IllegalArgumentException`.
- Evidence (unit tests, all passing): zero/one special cases; negatives, 10000, `Integer.MIN/MAX_VALUE`,
  year 0 rejected; Roman numerals only 1–3999 (0 and 4000 get none); overflow-free at 9999
  (σ(9999) = 15912), 9973 = 1229th prime, 8192 = 2¹³, 5040 = 7!; leap years 1600/2000/2024 vs
  1700/1800/1900/2100; years before 1583 are phrased "By Gregorian calendar rules, …" and get no
  weekday facts; next February 29 from 2097-03-01 is 2104 (2100 skipped); December 31 no longer says
  "0 days left"; every number 0–9999, year 1–9999 and all 366 days produce facts.
- Time zones: "this year" comes from the device clock and zone (test: 2026-12-31T23:30Z is 2027 in
  Tokyo, 2026 in UTC). The date picker's UTC-midnight selection is converted in UTC, verified for
  Honolulu, New York, UTC, Sarajevo, Kolkata and Kiritimati (March 14 and February 29 unchanged).
- UI thread: calculations run on a single background executor. Measured (JVM, this Mac): all numbers
  0–9999 in ~0.8 s total, worst single number ~2.2 ms.
- Calendar model: proleptic Gregorian via `java.time`; not a historical calendar before 1583.

## 4. Other implemented changes (from the first pass, unchanged)
- **Design "Graph paper"**, from the launcher icon: teal/mint Material 3, full light and dark
  schemes, grid-textured fact card, 640 dp content column on large screens, edge-to-edge insets,
  predictive back, state kept across rotation/process death, 48 dp targets, content descriptions,
  accessibility headings, live region on the fact.
- **Typography:** Space Grotesk Medium (numbers, titles) + Manrope Regular/SemiBold (text, labels),
  SIL OFL 1.1, bundled static instances; full type scale; sp sizes; checked at 200 % font size.
- **Navigation:** one main screen (launcher `.RandomActivity` unchanged) with Random/Year/Date/Math
  chips; lookup card with validation; Settings (theme, privacy options, policy, licences, version);
  Exit dialog without ads; share and copy.
- **Build:** AGP 9.4.1, Gradle 9.7.1 (checksum pinned), JDK 17, compile/target 36, Groovy assignment
  syntax, R8 + resource shrinking.

## 5. Ads and consent
| Feature | Release unit (owner-provided, not verified in AdMob) | Debug unit |
|---|---|---|
| App ID | `ca-app-pub-6402675413704299~8756323389` | `ca-app-pub-3940256099942544~3347511713` |
| Random banner | `…/3830574382` | `ca-app-pub-3940256099942544/6300978111` |
| Year banner | `…/2325921024` | same sample |
| Date banner | `…/6579757155` | same sample |
| Math banner | `…/1395982738` | same sample |
| Interstitial | `…/7346043914` | `ca-app-pub-3940256099942544/1033173712` |

Configuration: only `app/build.gradle` (`admobRelease` / `admobDebug`), consumed by the manifest
placeholder and generated string resources. Verified in the built APKs (`aapt2 dump resources`) and
merged manifests. A byte scan of the release DEX found no sample IDs and no test hooks.

Behaviour: UMP `requestConsentInfoUpdate` → `loadAndShowConsentFormIfRequired` once per process
(also after process death); Mobile Ads initialised once, on a background thread, only after that
check has finished **and** UMP `canRequestAds()` is true; every banner load and resume re-checks
`canRequestAds()` (a banner is removed if consent no longer allows ads); banner per category, hidden
until loaded, separated from controls, paused/resumed/destroyed with the Activity; interstitial only
at a category switch: not in the first 3 minutes, then at most once per 3 minutes and only after 4
switches since the last ad (or launch); failures retried lazily at the next switch, never in a loop.

Fixed in session 3 (found on the phone): the app used to initialise the SDK right after calling
`requestConsentInfoUpdate`, as Google's sample does. `canRequestAds()` then still answered from the
**previous** session's cached state, so with a region change (non-EEA → EEA) the SDK initialised
although UMP then reported REQUIRED. Now nothing starts until this session's update has completed;
if the update fails (offline), UMP's own `canRequestAds()` decides.

Consent semantics (documented and observed): `canRequestAds()` is true when the consent status is
`NOT_REQUIRED` (1) or `OBTAINED` (3) (UMP API reference; constants read from the UMP 4.0.0 SDK).
`OBTAINED` means a choice was recorded, **including "Do not consent"**, so the app still requests ads
after a refusal; Google then decides from the TCF signals what may serve, e.g. limited ads without
personal data (https://support.google.com/admob/answer/10105530). Declining consent is therefore not
the same as blocking all ad requests. Observed: EEA test flow stored `IABTCF_PurposeConsents` =
`11111111111` after "Consent" and `00000000000` after "Do not consent" via Privacy options; with the
real (non-EEA) geography the device reported `consentStatus=1 canRequestAds=true
privacyOptions=NOT_REQUIRED sdkReady=true`.

Observed on SM-S711B with sample units (session 3):
| Situation | UMP status | canRequestAds | SDK | Ad |
|---|---|---|---|---|
| EEA (debug geography), form showing | 2 REQUIRED | false | not initialised, 0 Ads SDK log lines | none |
| EEA, after "Do not consent" | 3 OBTAINED (`IABTCF_PurposeConsents=00000000000`) | true | initialised | sample banner served |
| Real region (non-EEA) | 1 NOT_REQUIRED | true | initialised | sample banner served |
Sample units always fill; with production units Google decides from the TCF signals what, if
anything, may serve after a refusal (e.g. limited ads). A refusal therefore neither always allows
nor always blocks ad serving; it always allows *requests*, per UMP's semantics.

## 6. Tests and results

| Check | Result |
|---|---|
| Unit tests (`:app:testDebugUnitTest`) | **46/46 pass**: LocalFactGenerator (11) + edge cases (11), WikiParser against captured Wikipedia fixtures and wrong-subject guards (17), date-picker time zones (1), lookup validation (2), interstitial pacing (3), banner mapping (1). |
| Lint debug + release | 0 errors; warnings: newer Gradle/core/compileSdk 37 (intentional), launcher-icon assets (unchanged on purpose), unused launcher resources. |
| Builds | `assembleDebug`, `assembleRelease`, `bundleRelease` pass (release bundle **unsigned**). |
| Instrumented, SM-S711B Android 16, via `am instrument` (no uninstall) | **13/13 pass**: headings per category, invalid-year error, state across recreation, date field not truncated, package name; live Wikipedia: year 1969, March 14, February 29, trivia 42, math 1729 each show content from the matching article; 4567 and 2150 show the honest empty state; offline (refused connection via debug hook) shows error + retry + labelled fallback, and Try again then loads 1969 from Wikipedia. |
| Displayed content (logcat evidence) | e.g. 1969 → "July 8 – Vietnam War: The first U.S. troop withdrawals are made."; March 14 → "1074 – Battle of Mogyoród: …"; February 29 → "1972 – South Korea withdraws 11,000 of its 48,000 troops…"; 1729 (math) → "1729 is the dimension of the Fourier transform…"; each with source “1969”, “March 14”, “February 29”, “1729 (number)”. |
| Ads, same device, sample units only | Pass: consent/`canRequestAds` invariant (all three situations above); banner load failure (invalid unit under Google's sample publisher → "Publisher data not found", code 3) keeps the slot hidden and the app usable; **interstitial with production pacing** (debug build): no ad in the first 3 minutes despite 4 switches, shown (`AdActivity`) on the first eligible switch, dismissed with Back, returned to `RandomActivity`, no second ad within the next 4 switches, next ad preloaded. Pacing constants unchanged (3 min, 4 switches — `InterstitialPacing`, unit-tested). |
| Shrunk **QA build** (release R8/shrinking + sample IDs + debug key), same device | First run **crashed at start-up** in the R8 build: WorkManager 2.7.0 / Room 2.2.5 (transitive from the Mobile Ads SDK) could not create `WorkDatabase` because R8 full mode removed the generated constructor (Room 2.2.5's rule keeps only the class). Fixed in `proguard-rules.pro` (`-keep class * extends androidx.room.RoomDatabase { <init>(); }`) — **this also fixes the production release build**. After the fix: app starts (0 crashes), 12/12 UI + Wikipedia checks pass, consent check passes (NOT_REQUIRED, banner served), interstitial displays and dismisses (video creative, closable after 31 s) back to `RandomActivity`. |
| Earlier session (still valid) | EEA consent form shown before any ad SDK activity; Privacy options form opens; test banner renders; manual QA of both themes, rotation, landscape, 200 % font; in-place upgrade 1.5 → new build (debug-signed). |
| Emulator | Not available: the installed emulator (31.1.4) cannot boot the API 35 image; the API 30 QA AVD was deleted outside this session twice and its emulator was stopped, so it was not recreated again. Android < 16 remains untested. |
| Not tested | The production-signed release build on a device (live ads — the QA build stands in for its R8 behaviour, not for signing or Play upgrade); Play-installed upgrade; Android < 16. |

## 7. Release claims, qualified
- **Versions** — sources checked 2026-09-25; dates are artifact publication dates (HTTP Last-Modified on Google Maven / Maven Central / services.gradle.org):

| Component | Version | Published | Compatibility evidence |
|---|---|---|---|
| Android Gradle Plugin | 9.4.1 | 2026-09-18 | Gradle ≥ 9.6.0, JDK 17, compileSdk ≤ 37 — https://developer.android.com/build/releases/gradle-plugin |
| Gradle | 9.7.1 | 2026-08-19 | Runs on JVM 17–27; tested with AGP 9.0–9.5.0-alpha02 — https://docs.gradle.org/current/userguide/compatibility.html (9.8.0 of 2026-09-24 skipped as one day old) |
| JDK | Temurin 17.0.13 | — | AGP 9.4 minimum/default |
| compile/target SDK | 36 | — | Play: API 36 required for updates since 2026-08-31 — https://developer.android.com/google/play/requirements/target-sdk |
| play-services-ads | 25.5.0 | 2026-09-17 | minSdk 24, compileSdk ≥ 35 — https://developers.google.com/admob/android/rel-notes , /quick-start |
| UMP | 4.0.0 | 2025-10-30 | minSdk 23 — https://developers.google.com/admob/android/privacy/release-notes |
| appcompat | 1.8.0 | 2026-08-12 | AAR minCompileSdk 34, minSdk 23 — https://developer.android.com/jetpack/androidx/releases/appcompat |
| activity | 1.13.0 | 2026-03-11 | AAR minCompileSdk 36, minSdk 23 — https://developer.android.com/jetpack/androidx/releases/activity |
| core | 1.18.0 | 2026-03-11 | 1.19.0 needs compileSdk 36.1 (release notes) and 1.19.1's AAR declares minCompileSdk 37 → stay on 1.18.0 — https://developer.android.com/jetpack/androidx/releases/core |
| material | 1.14.0 | 2026-05-13 | AAR minSdk 23, no minCompileSdk constraint — https://github.com/material-components/material-components-android/releases |
| constraintlayout | 2.2.2 | 2026-07-29 | AAR minCompileSdk 34, minSdk 21 |
| volley | 1.2.1 | 2021-08-24 | unchanged from 1.5 |
| jsoup | 1.23.2 | 2026-08-26 | MIT — https://jsoup.org/license |
All library minSdk values are ≤ 26 (the app's minSdk).
- **Device support:** the *configuration* is unchanged versus the 1.5 bundle (same minSdk 26,
  permissions, `uses-feature` faketouch only, supported screens, no native code — compared with
  `aapt2 dump badging`). Target 34 → 36 changes platform behaviour, not eligibility. This is not a
  Play device-catalog verification; compare supported-device counts in Play Console after upload.
- **Signing:** the committed bundle's certificate is what was used to sign it for upload. If Play App
  Signing is enabled, Play-installed copies are signed with Google-held app-signing key, possibly
  different. An APK signed with the upload key can then **not** update a Play-installed app. The
  owner must check both certificates in Play Console (checklist § 1).
- **Upgrade:** only a debug-signed in-place upgrade (1.5 source → new) was tested. Production-upgrade
  compatibility is unverified until an internal-testing install over the live app.

### QA build type (session 3)
`app/build.gradle` → `buildTypes.qa`: `initWith release` (same code, `minifyEnabled`,
`shrinkResources`, `proguard-android-optimize.txt` + `proguard-rules.pro`), Google sample AdMob IDs,
debug keystore, versionName `1.5-qa`, outputs under `app/build/outputs/apk/qa/`. Only difference in
R8 input: `app/proguard-qa-test.pro`, which keeps what the instrumented tests reach into (R ids, a
few test accessors, and `androidx.tracing`/`kotlin` for the androidx.test runner), so the QA APK is
larger (4.74 MB vs 4.22 MB unsigned release APK). Build: `./gradlew :app:assembleQa`; tests:
`./gradlew :app:assembleQaAndroidTest -PtestBuildType=qa`, then `adb install -r` both and
`am instrument`. It is **not** evidence of Play signing or of upgrading a Play-installed copy.

## 8. Backup git ref
The working tree contained someone's unfinished, uncommitted redesign when work began (a
Material 3 dark theme, a new Random layout, bottom-navigation icons and menu, `RandomActivity`
changes; some files staged). The modernization rewrote the same files, so, to honour "do not discard
existing uncommitted work", it was saved first without touching the tree:
`git stash create` → `git update-ref refs/backup/wip-before-modernization acd4eba`.
It is still recoverable: `git show --stat refs/backup/wip-before-modernization` lists it, and
`git stash apply refs/backup/wip-before-modernization` (on a clean tree or new branch) restores it.
The ref is local only (never pushed).

## 9. Remaining blockers and risks
1. **Owner:** highest uploaded versionCode → then bump versionCode/versionName (release-blocking).
2. **Owner:** sign with the upload key; confirm upload vs app-signing certificates (release-blocking).
3. **Owner:** privacy policy and Data safety must mention Wikipedia requests and ads; "encrypted in
   transit" is now true for the app's own traffic (release-blocking for policy accuracy).
4. **Owner:** AdMob Privacy & messaging GDPR (and, if relevant, US states) message published; ad units confirmed.
5. **Test gap:** Android < 16 untested (no usable emulator); production-signed build never run.
6. **Content risk:** Wikipedia layout changes can reduce coverage (fixture tests will catch parser
   regressions, not live changes); consider a periodic live smoke test (`WikipediaContentTest`).
7. **Store screenshots:** v2 review captures done (section 11); they show the app UI only (not the
   launcher icon). Final store graphics need cropping to ≤ 2:1 and the new Play icon (section 10).

## 10. Launcher icon (new, session 8 — 2026-09-26)
Original vector artwork generated by `tools/icon/generate_icon.py` (one geometry definition emits
both the Android vector XML and the PNGs): a mint magnifying glass with a bold number sign (#) in
the lens, on a dark-teal background with a faint graph-paper grid — the app's palette and motif. No
text, logos or emoji; heavy shapes that stay legible at 24 px.
- Adaptive icon: `res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` →
  `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`,
  `drawable/ic_launcher_monochrome.xml` (themed icons, Android 13+).
- Legacy PNGs: `res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` and `ic_launcher_round.png`
  (48–192 px). minSdk is 26, so every supported Android version uses the adaptive icon; the PNGs
  are fallbacks only.
- Manifest: added `android:roundIcon="@mipmap/ic_launcher_round"`; `android:icon` unchanged.
- One icon for all build types: removed the duplicated old icons in `src/debug/res` and
  `src/release/res`, the unused templates in `src/main/res`, the old `ic_launcher-playstore.png`
  copies and the QA build's `res.srcDirs` override (old files remain in git history).
- Google Play: `docs/store-assets/play-icon-512.png` — 512×512, 32-bit RGBA, fully opaque, no baked
  rounded corners (Play applies its own mask), 32 KB.
- Safe area: artwork reaches radius 28.5 of the 33-unit safe circle (checked by the generator); the
  # stays 2.4 units inside the lens.
- Previews: `docs/icon/icon-preview.png` (circle / rounded square / square masks, 108-unit canvas
  with visible and safe areas, 192–24 px on light and dark, themed monochrome) and
  `docs/icon/icon-android-render.png` (the real resources drawn by Android on SM-S711B, API 36, by
  `LauncherIconRenderTest`: layers, circle, rounded square, the device's own Samsung mask,
  monochrome layer). Android's rendering matches the generator's preview.
- Verified: build (debug, release, QA, bundle), lint (MonochromeLauncherIcon, ObsoleteSdkInt and
  unused-launcher warnings resolved), all variants resolve the app icon to the adaptive XML.
- Not changed: applicationId, launcher activity, signing, versions, user data.

## 11. Store screenshots
**Current (session 7, 2026-09-25): v2 captured, inspected and approved (7/7).**
- Folder: `docs/store-screenshots/current-v2/` (01-random-42-dark, 02-math-1729-dark,
  03-year-1969-dark, 04-year-1969-light, 05-date-march14-dark, 06-settings-light, 07-settings-dark;
  PNG 1080×2340, unedited, README.md).
- ZIP: `docs/store-screenshots/nif-current-screenshots-v2.zip` (7 PNGs + README only).
- Method: PixelCopy of the app window (no system overlays, no Edge handle, no notifications), ads-off
  debug capture build (installed APK verified byte-identical to the current build, not reinstalled),
  guard before and after the run; only this app's activities were started.
- Inspection: all four category chips fully visible; content ends above the reserved 3-button
  navigation area; Wikipedia attribution with licence link visible on every main screen; no ads,
  empty ad slots, dialogs, keyboard, loading or error states.
- The session-4 folder `current/` and `nif-current-screenshots.zip` are superseded.

**Status (session 6): recapture still pending** — real-input checks passed, but the guard aborted the
capture because Viber's call screen came to the foreground; not retried. `nif-current-screenshots-v2.zip`
not created yet.
**Status (session 5): SUPERSEDED.** The 7 images below were approved in session 4
but predate the session-5 layout fixes (section 12): they show the clipped Math chip, content drawn
under the 3-button navigation bar and Samsung's Edge-panel handle. Do not use them for store
graphics. Recapture with the updated `ScreenshotCaptureTest` (PixelCopy of the app window only).

Session 4 record: **captured and approved (7/7), 2026-09-25.** Review captures of the current UI,
not final store assets (those depend on the new icon and on cropping to Play's ≤ 2:1 ratio).
- Folder: `docs/store-screenshots/current/` — 01-random-42-dark, 02-math-1729-dark,
  03-year-1969-dark, 04-year-1969-light, 05-date-march14-dark, 06-settings-light, 07-settings-dark
  (PNG, 1080×2340, unedited, with README.md).
- ZIP: `docs/store-screenshots/nif-current-screenshots.zip` (7 PNGs + README only).
- Configuration: debug build with `-PscreenshotMode=true` (ads off in that build only), SM-S711B
  Android 16, `ScreenshotCaptureTest`; status bar hidden inside the app window; no system setting
  changed. Each image was inspected: correct subject and Wikipedia attribution visible, no clipping of
  content, no dialogs, keyboard, ads, loading or error states, no notifications or private data.
- Content shown (live Wikipedia): 42 — "In 1994, Adams created the 42 Puzzle…"; 1729 — "…the first
  nontrivial taxicab number, a Carmichael number…"; 1969 dark — "October 15 – DZKB-TV Channel 9…";
  1969 light — "May 14 – Colonel Muammar Gaddafi visits Mecca, Saudi Arabia."; March 14 — "1964 – Jack
  Ruby is convicted…". The 1969 picks are genuine but not the most iconic events (Apollo 11 entries
  were too long to keep the attribution on screen); re-run the test if different events are wanted.
- Guard: `guard.sh`-style pre-checks abort (exit 1) on an unreachable device, lock screen, active call
  or another foreground app; verified with negative tests before use. Two early runs were stopped by
  the test itself: its injected tap on "Show fact" landed on the 3-button navigation bar's Home
  button (edge-to-edge content behind the bar). The capture test now drives the app only through its
  own views (no injected touches). Users are unaffected (content scrolls clear of the bar).
- Old `docs/store-screenshots/phone-01-random-dark-1080x1920.png` shows the superseded design.

## 12. Navigation bar, keyboard and category chips (session 5)
**Cause (real layout defect, not only a test coordinate).** Screens used edge-to-edge with the
navigation-bar inset applied as *padding* of the scroll view (`clipToPadding=false`), so content
was drawn beneath the navigation bar. With 3-button navigation that area is tappable system UI:
- At rest, the lookup card (input, "Show fact") lay under the bar; a real tap there was delivered to
  the `NavigationBar0` window (logged: touch delivered to `1f87349` at offset (0,−2205)), and SystemUI
  injected Home — the two interrupted capture runs and one test run.
- Scroll-into-view requests (focus changes, keyboard, TalkBack, Espresso `scrollTo`) treat the
  padding as visible: measured before the fix, "Show fact" landed at y 2205–2340, fully behind the
  135 px bar (overlap 135 px).
- With the keyboard open, only the text line was revealed; helper/error text sat 61 px under the
  keyboard and "Show fact" was hidden.
- Category chips were in a horizontal scroller; with the bundled font the fourth chip ("Math") ran
  past the gutter and was cut off at normal font size.

**Fix.**
- `ui/EdgeToEdgeInsets`: uses `WindowInsetsCompat.Type.tappableElement()`. Tappable bars (3-button
  navigation, including side bars in landscape) are reserved outside the content (root padding), so
  nothing is drawn beneath system buttons; a non-tappable gesture handle keeps the edge-to-edge look
  (content scrolls behind it, scroll view pads for it). No device-specific values.
- `ui/InsetAwareScrollView` (main, Settings, document screens): scroll-into-view requests exclude
  the scroll view's inset padding, so focused/requested views stop above the bars.
- `RandomActivity.revealLookupAboveKeyboard`: when the keyboard opens for the lookup field, the whole
  lookup card (field, helper/error, "Show fact") is scrolled above the keyboard.
- Category chips: wrapping `ChipGroup` (no horizontal scroller), text-only chips (decorative icons
  removed): one line at normal size, more lines at large font sizes, never clipped.

**Verification (SM-S711B, Android 16, 3-button navigation, debug build with ads off = the layout a
user sees whenever no banner is loaded).**
| Check | Before | After |
|---|---|---|
| Scroll viewport end vs. tappable bar top (y 2205) | content to 2340 | ends at 2205 |
| Controls visible under the tappable bar at rest | lookup card | none (all 9 controls checked) |
| "Show fact" after scroll-into-view | y 2205–2340 (behind bar) | y 2070–2205 |
| Lookup input after scroll-into-view | behind bar | y 2044–2205 |
| Chips at normal size | Math cut at gutter | 4 chips on one line, x 45–886 of 990 px row, none clipped |
| Chips at font scale 2.0 (layout inflated off-screen with fontScale 2.0 — device setting unchanged) | — | 2 rows, no label ellipsized |
| Real input (injected taps, soft-keyboard typing) | tap reached NavigationBar → Home | taps on chip and field reached the app; **0** real presses on the navigation bar (only ACTION_OUTSIDE notifications) |
| Keyboard open (keyboard top y 1422): field + helper/error | helper 61 px under keyboard | field + error "Please enter a number from 0 to 9999" at y 969–1208, above keyboard (session 6, real typing + keyboard search action) |
| Keyboard open: "Show fact" | hidden by keyboard | y 1231–1366, above keyboard |
| Back dismisses keyboard | — | PASS (keyboard closed, app still in front) |
| Real tap on "Show fact" with 28 typed | — | PASS: subject 28, "28 also appears in the Padovan sequence.", source Wikipedia “28 (number)” |
| Swipe to end, then real tap | — | "Show fact" at y 1946–2081 (bar top 2205); tap PASS |
| Presses delivered to the navigation bar during real input | Home triggered | **0** (7 touches delivered to the app; only ACTION_OUTSIDE notifications reach the bar) |
| Gesture navigation | — | **untested** (no device/emulator with gesture nav; phone setting not changed) |

The screenshot helper drives views programmatically and is not evidence of touch accessibility;
the evidence above comes from `NavBarInsetsTest` (measurements + real input).

**Right-edge handle** in the session-4 screenshots: Samsung Edge panel, a system overlay window
(`com.sec.android.app.launcher/…edgepanel.app.CocktailBarService`), not part of the app. The capture
helper now uses PixelCopy of the app's own window, which contains only what the app renders (no
Edge handle, status bar, navigation bar or notifications) — no setting changed, nothing painted.

## 13. Production baseline 1.7 and release signing (session 9 — 2026-09-26)
**Version.** `app/build.gradle`: versionCode **18**, versionName **"2.0"** (owner-confirmed Play
Console: production 17 / 1.7, highest uploaded 17; earlier 16 / 1.6, 15 / 1.5). QA build: `2.0-qa`.

**Search for 1.6 / 1.7 (read-only).**
| Place | Result |
|---|---|
| Local git: branches, tags, stash, reflog, all commits | Only 1.4 (`a64644a`) and 1.5 (`24e4741`); no tags; the only other refs are this work's WIP backup |
| GitHub `superAdmir/number-interesting-facts` (ls-remote, API) | Single branch `main` @ `24e4741`, last push 2024-09-27; no tags, releases or other branches |
| Committed bundle `app/release/app-release.aab` | 15 / 1.5 |
| This Mac (Spotlight + home-folder search for bundles/APKs of this package, excluding caches) | Only this project's bundles; other bundles found belong to different apps (`com.calzero.app`, `com.habitly`) |
| The phone | Only this work's debug build (15 / 1.5 at the time, installer none) — not the Play copy |
| Play listing (public) | "Updated on Oct 1, 2024" — suggests 1.6/1.7 were uploaded between 2024-09-27 and 2024-10-01 (inference only) |

**Conclusion:** 1.7's identity details, storage (preferences, databases, files), backup rules,
permissions, components (including the launcher activity name) and features are **unknown**. It must
not be assumed to store no data because 1.5 did not. No 1.7 → 2.0 upgrade test was possible.

**Needed from Play Console** (Test and release → App bundle explorer → select version **17 (1.7)**
→ Downloads; repeat for 16 if convenient):
1. **Original app bundle** (the AAB you uploaded) — to compare manifest, permissions, components,
   backup rules, and code/resources for SharedPreferences names, databases and files.
2. **Signed, universal APK** (Play-signed) — to see what users actually run, and to read the current
   app-signing certificate.
3. Test and release → App integrity → App signing: which app-signing certificate is **current** (the
   Digital Asset Links snippet lists `AA:4B:BD:80:…:CA:77` and `77:DB:35:A1:…:63:7C` without saying
   which is current).
With (1) or (2): a debug-signed 1.7 → 2.0 upgrade test on a separate test device/emulator with
representative data becomes possible (1.7 re-signed with the debug key; never on the owner's phone
app). It still is not a Play-signed upgrade; that requires the internal-testing track.

**Signing.**
- Play upload certificate (owner-provided): SHA-256
  `36:E8:C8:8B:E3:D2:04:1F:E2:DE:6C:9C:9D:62:DB:9C:38:FA:1E:7F:83:72:7A:D6:16:C4:57:70:7E:5A:51:42`.
- The committed 1.5 bundle was signed with `84:8C:25:25:…:51:AB:E8` — **a different key**: the
  upload key changed after 1.5 (e.g. an upload-key reset) or 1.6/1.7 were signed differently.
- No upload keystore or credentials exist in this project, its Gradle properties, or Android Studio's
  saved signed-bundle settings for it (no other folders searched). **Signing is blocked** until the
  owner provides the keystore locally.
- Prepared (inert until configured): `app/build.gradle` `signingConfigs.upload`, reading
  `storeFile`/`keyAlias` from the git-ignored `signing.local.properties` and passwords only from the
  environment (`NIF_UPLOAD_STORE_PASSWORD`, optional `NIF_UPLOAD_KEY_PASSWORD`); root `.gitignore`
  ignores `signing.local.properties`, `*.jks`, `*.keystore`. `tools/signing/verify-upload-key.sh`
  compares the alias certificate with the upload SHA-256 (password via `keytool -storepass:env`);
  `tools/signing/verify-signed-bundle.sh` checks signature, signer, package, production AdMob ID and
  absence of debug hooks. Tested: missing configuration stops the key check; the 1.5 bundle is
  rejected as SIGNER MISMATCH; without configuration the release stays unsigned.

**Unsigned 2.0 bundle (built and verified; not upload-ready).**
`app/build/outputs/bundle/release/app-release.aab` — `bundletool 1.18.3 validate` OK; versionCode 18,
versionName 2.0, package `com.nip.numberinterestingfacts`; production AdMob app ID and 5 production
units; DEX contains no `DebugOverrides`, no sample publisher ID, no test host; `jarsigner`: not signed.

## 14. New upload key and upload-key reset preparation (session 10 — 2026-09-26)
The owner no longer has the registered upload keystore. A **new upload key** was created locally
(owner-authorised); the reset is **not** submitted and Play settings are unchanged. The app-signing
key is untouched.
- Official requirements checked 2026-09-26: RSA ≥ 2048 bits (Play Help "Use Play App Signing",
  https://support.google.com/googleplay/android-developer/answer/9842756); validity ≥ 25 years and
  ending after 2033-10-22; PEM export `keytool -export -rfc` (https://developer.android.com/studio/publish/app-signing).
- Key: `/Users/AdmirSatara/number-interesting-facts-upload-key/nif-upload-keystore.p12` (PKCS12,
  alias `nif-upload`, RSA 4096, SHA256withRSA, valid 2026-09-26 → 2054-02-11). Generated with a
  40-character random password written straight to a 600 file; passed to keytool only via
  `-storepass:file` (never on a command line, in logs or chat). Folder 700, secrets 600.
- **NEW upload certificate (not yet registered with Google Play):**
  `/Users/AdmirSatara/number-interesting-facts-upload-key/nif-upload-certificate.pem`
  SHA-256 `7A:EC:75:B0:9E:C5:02:0A:0D:56:F8:8B:B3:C1:A7:53:FF:F0:F8:BF:74:77:1D:C7:12:E6:C6:3A:E8:42:32:56`
  SHA-1 `73:6C:0E:98:3C:FB:1D:1A:1B:7E:8B:26:D9:B1:88:4F:35:D5:A9:CF`
- **Currently registered upload certificate (until Google confirms):**
  `36:E8:C8:8B:E3:D2:04:1F:E2:DE:6C:9C:9D:62:DB:9C:38:FA:1E:7F:83:72:7A:D6:16:C4:57:70:7E:5A:51:42`.
  Recorded with `RESET_STATUS=pending` in `tools/signing/upload-key-status.properties` (public data).
- Local signing: git-ignored `signing.local.properties` (storeFile, keyAlias, storePasswordFile —
  paths only); Gradle reads the password from the file or `NIF_UPLOAD_*` env vars.
- Scripts: `verify-upload-key.sh` / `verify-signed-bundle.sh` classify a certificate as registered
  (exit 0), NEW with reset pending (exit 10, never reported as a Play match), or unknown (exit 1).
  Verified: local key → NEW/pending; old 1.5 bundle → UNKNOWN.
- Signed artifact: `app/build/outputs/bundle/release/app-release.aab` and the labelled copy
  `nif-2.0-vc18-SIGNED-NEW-UPLOAD-KEY-PLAY-RESET-PENDING.aab` (identical), SHA-256
  `75abad97207994a829b016de5ddbfdcd00ab347b67f85919fe3e4819608916ed`. jarsigner: jar verified
  (expected warnings: self-signed certificate, no timestamp); signer = NEW key, reset pending;
  bundletool 1.18.3 validate OK; 18 / 2.0; package `com.nip.numberinterestingfacts`; production AdMob
  app ID; no debug hooks or sample IDs. **Not upload-ready until Google confirms the reset**, and a
  correctly signed bundle is not proof of a working Play-installed upgrade.
- Recovery: `RESTORE.txt`, `CREDENTIALS.txt` and a same-disk copy `backup-2026-09-26/` (verified
  identical with cmp) in the key folder. Same-disk copies do not replace an independent backup.
- Git: no keystore, password, credentials or `signing.local.properties` tracked, staged or addable;
  the password value occurs in no repository file. `.gitignore` covers `signing.local.properties`,
  `*.jks`, `*.keystore`, `*.p12`.
