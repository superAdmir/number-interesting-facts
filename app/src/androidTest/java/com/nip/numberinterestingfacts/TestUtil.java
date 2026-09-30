package com.nip.numberinterestingfacts;

import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import java.util.concurrent.atomic.AtomicBoolean;

/** Polling helpers: Wikipedia requests run on Volley threads that Espresso does not track. */
final class TestUtil {
    static final long NETWORK_TIMEOUT_MS = 30_000;

    private TestUtil() {
    }

    /** Waits until the fact card has finished loading (progress hidden, text visible). */
    static void waitForFact(ActivityScenario<RandomActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + NETWORK_TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            AtomicBoolean done = new AtomicBoolean();
            scenario.onActivity(a -> done.set(
                    a.findViewById(R.id.factProgress).getVisibility() == View.GONE
                            && a.findViewById(R.id.factText).getVisibility() == View.VISIBLE));
            if (done.get()) return;
            SystemClock.sleep(200);
        }
        throw new AssertionError("fact did not finish loading within " + NETWORK_TIMEOUT_MS + " ms");
    }

    static String text(ActivityScenario<RandomActivity> scenario, int id) {
        String[] out = new String[1];
        scenario.onActivity(a -> out[0] = ((android.widget.TextView) a.findViewById(id)).getText().toString());
        return out[0];
    }

    static int visibility(ActivityScenario<RandomActivity> scenario, int id) {
        int[] out = new int[1];
        scenario.onActivity(a -> out[0] = a.findViewById(id).getVisibility());
        return out[0];
    }
}
