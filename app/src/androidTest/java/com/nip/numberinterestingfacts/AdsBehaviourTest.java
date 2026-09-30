package com.nip.numberinterestingfacts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.nip.numberinterestingfacts.TestUtil.visibility;
import static com.nip.numberinterestingfacts.TestUtil.waitForFact;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.nip.numberinterestingfacts.ads.AdsManager;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

/**
 * Ad behaviour with Google's sample ad units only (debug build). Run a single test with
 * {@code -e class com.nip.numberinterestingfacts.AdsBehaviourTest#<name>}.
 */
@RunWith(AndroidJUnit4.class)
public class AdsBehaviourTest {
    private static final String TAG = "AdsBehaviourTest";

    @After
    public void reset() {
        DebugOverrides.setAdUnitId(null);
    }

    private static AdsManager ads() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        return ((NumberFactsApp) app).getAdsManager();
    }

    private static void waitUntil(String what, long timeoutMs, java.util.function.BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (SystemClock.uptimeMillis() > deadline) throw new AssertionError("timed out waiting for " + what);
            SystemClock.sleep(250);
        }
    }

    private static Activity resumedActivity() {
        Activity[] out = new Activity[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            out[0] = resumed.isEmpty() ? null : resumed.iterator().next();
        });
        return out[0];
    }

    /**
     * UMP semantics on this device: canRequestAds() reflects having a decision (NOT_REQUIRED or
     * OBTAINED), not consent being granted; the app additionally waits for this session's check.
     * Uses only the app's AdsManager so it also works on the shrunk QA build.
     */
    @Test
    public void canRequestAdsFollowsUmpConsentStatus() {
        final int notRequired = 1, required = 2, obtained = 3; // UMP ConsentStatus constants (SDK 4.0.0)
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            waitUntil("consent info", 30_000, () -> ads().consentStatus() != 0);
            int status = ads().consentStatus();
            if (status == required) {
                // The consent form is on screen: nothing may initialise or load until it is answered.
                SystemClock.sleep(5_000);
                Log.i(TAG, "consentStatus=" + status + " (REQUIRED) umpCanRequestAds=" + ads().umpCanRequestAds()
                        + " appCanRequestAds=" + ads().canRequestAds() + " sdkReady=" + ads().isSdkReady()
                        + " bannerVisible=" + (visibility(s, R.id.adContainer) == View.VISIBLE));
                assertFalse("SDK must not initialise before consent", ads().isSdkReady());
                assertFalse(ads().canRequestAds());
                return;
            }
            waitUntil("this session's consent check", 30_000, () -> ads().isConsentCheckedThisSession());
            status = ads().consentStatus();
            boolean can = ads().umpCanRequestAds();
            Log.i(TAG, "consentStatus=" + status + " umpCanRequestAds=" + can
                    + " appCanRequestAds=" + ads().canRequestAds()
                    + " privacyOptionsRequired=" + ads().privacyOptionsRequiredForTesting()
                    + " sdkReady=" + ads().isSdkReady());
            assertEquals(status == obtained || status == notRequired, can);
            assertEquals(can, ads().canRequestAds());
            if (can) {
                waitUntil("SDK init", 30_000, () -> ads().isSdkReady());
                // Observe (not assume) whether an ad actually serves with the stored choice.
                SystemClock.sleep(15_000);
                Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
                Log.i(TAG, "banner slot visible after 15 s: "
                        + (visibility(s, R.id.adContainer) == View.VISIBLE) + " | TCF purpose consents="
                        + ctx.getSharedPreferences(ctx.getPackageName() + "_preferences", Context.MODE_PRIVATE)
                        .getString("IABTCF_PurposeConsents", "(none)"));
            } else {
                assertFalse("SDK must not initialise before consent", ads().isSdkReady());
            }
        }
    }

    /** A request that fails (invalid unit under Google's sample publisher) leaves the slot hidden. */
    @Test
    public void bannerLoadFailureKeepsTheSlotHiddenAndTheAppUsable() {
        DebugOverrides.setAdUnitId("ca-app-pub-3940256099942544/0000000000");
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            waitUntil("SDK init", 30_000, () -> ads().isSdkReady());
            SystemClock.sleep(15_000); // give the failing request time to complete
            assertEquals(View.GONE, visibility(s, R.id.adContainer));
            onView(withId(R.id.newFactButton)).perform(scrollTo(), click());
            waitForFact(s);
            assertEquals(View.GONE, visibility(s, R.id.adContainer));
        }
    }

    /**
     * Real production pacing (no shortcuts): never within the first 3 minutes of the session;
     * afterwards only once at least 4 category switches have happened since the last ad (or since
     * launch). The sample interstitial must render and dismiss back to the app. Takes ~3.5 min.
     */
    @Test
    public void interstitialFollowsPacingRendersAndDismisses() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            waitUntil("SDK init", 60_000, () -> ads().isSdkReady());
            assertTrue("run this test in its own process (am instrument -e class ...#this)",
                    SystemClock.elapsedRealtime() - processStart() < 2 * 60 * 1000L);
            if (!ads().isInterstitialLoaded()) {
                // A failed preload is retried lazily at the next category switch, by design.
                onView(withId(R.id.chipMath)).perform(scrollTo(), click());
                waitForFact(s);
            }
            waitUntil("interstitial preload", 90_000, () -> ads().isInterstitialLoaded());

            // Within the first 3 minutes, even 4+ switches never show an ad.
            int[] chips = {R.id.chipYear, R.id.chipDate, R.id.chipMath, R.id.chipTrivia};
            for (int chip : chips) {
                onView(withId(chip)).perform(scrollTo(), click());
                waitForFact(s);
                assertTrue("no ad before 3 minutes, but resumed=" + describe(resumedActivity()),
                        resumedActivity() instanceof RandomActivity);
            }

            long sessionAgeNeeded = 3 * 60 * 1000L + 5_000;
            long waitMs = sessionAgeNeeded - (SystemClock.elapsedRealtime() - processStart());
            if (waitMs > 0) SystemClock.sleep(waitMs);

            // 3 minutes passed and 4 switches already counted: the next switch shows the ad.
            onView(withId(chips[0])).perform(scrollTo(), click());
            waitUntil("interstitial shown", 15_000, () -> {
                Activity a = resumedActivity();
                return a != null && a.getClass().getName().equals("com.google.android.gms.ads.AdActivity");
            });
            Log.i(TAG, "interstitial shown: " + resumedActivity().getClass().getName());

            // Dismiss like a user (Back); the app must come back with its content.
            // Video test creatives only allow closing after their countdown, so allow up to 2 minutes.
            long shownAt = SystemClock.uptimeMillis();
            long deadline = shownAt + 120_000;
            int backPresses = 0;
            while (!(resumedActivity() instanceof RandomActivity)) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw new AssertionError("interstitial did not dismiss after " + backPresses + " Back presses");
                }
                backPresses++;
                try {
                    Espresso.pressBackUnconditionally();
                } catch (RuntimeException ignored) {
                    // AdActivity may ignore Back for its first seconds.
                }
                SystemClock.sleep(1_000);
            }
            Log.i(TAG, "interstitial dismissed after " + (SystemClock.uptimeMillis() - shownAt) / 1000 + " s and "
                    + backPresses + " Back presses, back in " + resumedActivity().getClass().getSimpleName());
            waitForFact(s);
            // Right after an ad: 4 more switches are not enough, because 3 minutes have not passed.
            for (int chip : chips) {
                onView(withId(chip)).perform(scrollTo(), click());
                waitForFact(s);
                assertTrue("no back-to-back interstitial, but resumed=" + describe(resumedActivity()),
                        resumedActivity() instanceof RandomActivity);
            }
            waitUntil("next interstitial preloaded", 60_000, () -> ads().isInterstitialLoaded());
        }
    }

    private static String describe(Activity a) {
        return a == null ? "none (app paused: another window is in front)" : a.getClass().getName();
    }

    private static long processStart() {
        return android.os.Process.getStartElapsedRealtime();
    }
}
