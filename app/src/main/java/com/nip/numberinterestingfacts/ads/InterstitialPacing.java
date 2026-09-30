package com.nip.numberinterestingfacts.ads;

/**
 * Pure pacing rules for interstitials, kept separate so they can be unit tested:
 * never within the first {@link #MIN_INTERVAL_MS} of the session, at most once per
 * {@link #MIN_INTERVAL_MS}, and only after {@link #SWITCHES_BETWEEN_ADS} category switches.
 */
final class InterstitialPacing {
    static final long MIN_INTERVAL_MS = 3 * 60 * 1000L;
    static final int SWITCHES_BETWEEN_ADS = 4;

    private long lastShownAt;
    private int switchesSinceLastAd;

    InterstitialPacing(long sessionStart) {
        // Treat session start as the last show so nothing appears right after launch.
        lastShownAt = sessionStart;
    }

    /** Records a category switch and returns whether an interstitial may be shown now. */
    boolean recordSwitchAndCheck(long now) {
        switchesSinceLastAd++;
        return switchesSinceLastAd >= SWITCHES_BETWEEN_ADS && now - lastShownAt >= MIN_INTERVAL_MS;
    }

    void recordShown(long now) {
        lastShownAt = now;
        switchesSinceLastAd = 0;
    }
}
