package com.nip.numberinterestingfacts.ads;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialPacingTest {
    private static final long START = 1_000_000L;
    private static final long INTERVAL = InterstitialPacing.MIN_INTERVAL_MS;

    @Test
    public void neverRightAfterLaunch() {
        InterstitialPacing pacing = new InterstitialPacing(START);
        for (int i = 0; i < 20; i++) assertFalse(pacing.recordSwitchAndCheck(START + 1000L * i));
    }

    @Test
    public void needsEnoughSwitchesAndTime() {
        InterstitialPacing pacing = new InterstitialPacing(START);
        long later = START + INTERVAL;
        for (int i = 1; i < InterstitialPacing.SWITCHES_BETWEEN_ADS; i++) {
            assertFalse(pacing.recordSwitchAndCheck(later));
        }
        assertTrue(pacing.recordSwitchAndCheck(later));
    }

    @Test
    public void resetsAfterShowing() {
        InterstitialPacing pacing = new InterstitialPacing(START);
        long t = START + INTERVAL;
        for (int i = 0; i < InterstitialPacing.SWITCHES_BETWEEN_ADS; i++) pacing.recordSwitchAndCheck(t);
        pacing.recordShown(t);
        // Enough switches but not enough time: still blocked.
        for (int i = 0; i < 10; i++) assertFalse(pacing.recordSwitchAndCheck(t + INTERVAL - 1));
        assertTrue(pacing.recordSwitchAndCheck(t + INTERVAL));
    }
}
