package com.nip.numberinterestingfacts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.nip.numberinterestingfacts.TestUtil.text;
import static com.nip.numberinterestingfacts.TestUtil.waitForFact;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

/**
 * Checks that interactive content can be brought fully above the navigation bar and tapped.
 * {@link #scrolledIntoViewStaysAboveNavigationBar} measures only (no touches).
 * {@link #realTapsTypingAndKeyboardDismissal} uses real injected taps, typing and Back.
 */
@RunWith(AndroidJUnit4.class)
public class NavBarInsetsTest {
    private static final String TAG = "NavBarInsetsTest";

    private static final class Bars {
        int screenHeight, screenWidth, navTop, navBottomInset, tappableBottom, imeBottom;
    }

    private static Bars bars(Activity a) {
        Bars b = new Bars();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View decor = a.getWindow().getDecorView();
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
            b.screenHeight = decor.getHeight();
            b.screenWidth = decor.getWidth();
            b.navBottomInset = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // Tappable system UI (3-button navigation); 0 for a gesture handle.
            b.tappableBottom = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom;
            b.imeBottom = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            b.navTop = b.screenHeight - Math.max(b.tappableBottom, b.imeBottom);
        });
        return b;
    }

    private static Rect screenRect(Activity a, int id) {
        Rect r = new Rect();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View v = a.findViewById(id);
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            r.set(loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
        });
        return r;
    }

    private static Activity resumed() {
        Activity[] out = new Activity[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> r = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            out[0] = r.isEmpty() ? null : r.iterator().next();
        });
        return out[0];
    }

    private static void assertStillInApp(String step) {
        Activity a = resumed();
        assertTrue("left the app after " + step + " (resumed=" + (a == null ? "none" : a.getClass().getName()) + ")",
                a instanceof RandomActivity);
    }

    private static final int[] CONTROLS = {R.id.chipTrivia, R.id.chipYear, R.id.chipDate, R.id.chipMath,
            R.id.newFactButton, R.id.copyButton, R.id.shareButton, R.id.lookupInput, R.id.lookupButton};

    /** Visible (clipped) part of a view on screen, or an empty rect when it is not visible. */
    private static Rect visibleRect(Activity a, int id) {
        Rect r = new Rect();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (!a.findViewById(id).getGlobalVisibleRect(r)) r.setEmpty();
        });
        return r;
    }

    private static void assertNothingUnderTappableBar(Activity a, Bars b, String when) {
        for (int id : CONTROLS) {
            Rect r = visibleRect(a, id);
            String name = a.getResources().getResourceEntryName(id);
            if (!r.isEmpty() && r.bottom > b.navTop) {
                throw new AssertionError(when + ": " + name + " visible at " + r.toShortString()
                        + " reaches into the tappable navigation bar (top " + b.navTop + ")");
            }
        }
    }

    /** Measurement only (no touches): at rest and after scroll-into-view requests. */
    @Test
    public void scrolledIntoViewStaysAboveNavigationBar() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            Activity a = resumed();
            Bars b = bars(a);
            s.onActivity(act -> act.findViewById(R.id.scroll).scrollTo(0, 0));
            SystemClock.sleep(300);
            Log.i(TAG, "insets: screen=" + b.screenWidth + "x" + b.screenHeight + " navBar=" + b.navBottomInset
                    + " tappable=" + b.tappableBottom + " -> content must end above y=" + b.navTop);
            Log.i(TAG, "resting layout: scroll=" + screenRect(a, R.id.scroll).toShortString()
                    + " factCard=" + screenRect(a, R.id.factCard).toShortString()
                    + " lookupButton visible=" + visibleRect(a, R.id.lookupButton).toShortString());
            assertNothingUnderTappableBar(a, b, "at rest");

            // Category chips: every chip fully visible and inside the screen at the current font size.
            for (int id : new int[]{R.id.chipTrivia, R.id.chipYear, R.id.chipDate, R.id.chipMath}) {
                Rect full = screenRect(a, id);
                Rect vis = visibleRect(a, id);
                Log.i(TAG, a.getResources().getResourceEntryName(id) + " " + full.toShortString());
                assertEquals("chip clipped: " + full.toShortString() + " visible " + vis.toShortString(),
                        full.width(), vis.width());
                assertTrue("chip outside the screen", full.left >= 0 && full.right <= b.screenWidth);
            }

            // The same request the system makes for focus, keyboard and accessibility navigation.
            for (int id : new int[]{R.id.lookupInput, R.id.lookupButton}) {
                onView(withId(id)).perform(scrollTo());
                SystemClock.sleep(300);
                Rect r = screenRect(a, id);
                Log.i(TAG, "after scrollTo " + a.getResources().getResourceEntryName(id) + ": " + r.toShortString()
                        + " navBarTop=" + b.navTop);
                assertTrue("still behind the tappable navigation bar", r.bottom <= b.navTop);
                assertNothingUnderTappableBar(a, b, "after scrollTo");
            }
        }
    }

    /**
     * Large text without touching the device setting: the main layout is inflated off-screen
     * with fontScale 2.0 at the real screen width, and no category label may be cut off.
     */
    @Test
    public void categoriesStayReadableAtDoubleFontSize() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            s.onActivity(a -> {
                android.content.res.Configuration c = new android.content.res.Configuration(a.getResources().getConfiguration());
                c.fontScale = 2.0f;
                android.content.Context big = a.createConfigurationContext(c);
                android.content.Context themed = new android.view.ContextThemeWrapper(big, R.style.Theme_NumberFacts);
                View layout = android.view.LayoutInflater.from(themed).inflate(R.layout.activity_random, null, false);
                int width = a.getWindow().getDecorView().getWidth();
                layout.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                layout.layout(0, 0, width, layout.getMeasuredHeight());
                View group = layout.findViewById(R.id.categoryGroup);
                int rows = 0, lastTop = -1;
                for (int id : new int[]{R.id.chipTrivia, R.id.chipYear, R.id.chipDate, R.id.chipMath}) {
                    com.google.android.material.chip.Chip chip = layout.findViewById(id);
                    if (chip.getTop() != lastTop) {
                        rows++;
                        lastTop = chip.getTop();
                    }
                    boolean ellipsized = chip.getLayout() != null && chip.getLayout().getEllipsisCount(0) > 0;
                    Log.i(TAG, "fontScale 2.0: " + chip.getText() + " [" + chip.getLeft() + "," + chip.getTop()
                            + "][" + chip.getRight() + "," + chip.getBottom() + "] ellipsized=" + ellipsized);
                    assertFalse("label cut off at 200% font: " + chip.getText(), ellipsized);
                    assertTrue("chip wider than its row", chip.getRight() <= group.getWidth());
                }
                Log.i(TAG, "fontScale 2.0: categories wrap into " + rows + " row(s) within width " + group.getWidth());
            });
        }
    }

    /** Real input: taps, typing on the soft keyboard, Back to dismiss it, swipe scrolling. */
    @Test
    public void realTapsTypingAndKeyboardDismissal() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            onView(withId(R.id.chipMath)).perform(scrollTo(), click());
            waitForFact(s);
            assertStillInApp("tapping Math");

            onView(withId(R.id.lookupInput)).perform(scrollTo(), click());   // real tap: keyboard opens
            SystemClock.sleep(800);
            // Empty search from the keyboard: the error must be readable above the keyboard.
            onView(withId(R.id.lookupInput)).perform(pressImeActionButton());
            SystemClock.sleep(600);
            Bars errIme = bars(resumed());
            Rect errField = screenRect(resumed(), R.id.lookupInputLayout);
            String error = String.valueOf(((com.google.android.material.textfield.TextInputLayout)
                    resumed().findViewById(R.id.lookupInputLayout)).getError());
            Log.i(TAG, "keyboard open + error: imeInset=" + errIme.imeBottom + " field+error=" + errField.toShortString()
                    + " keyboardTop=" + errIme.navTop + " error=\"" + error + "\"");
            assertTrue("keyboard closed unexpectedly", errIme.imeBottom > 0);
            assertEquals(resumed().getString(R.string.error_enter_number), error);
            assertTrue("error text hidden by the keyboard", errField.bottom <= errIme.navTop);

            onView(withId(R.id.lookupInput)).perform(typeText("28"));          // real key input
            SystemClock.sleep(600);
            Bars withIme = bars(resumed());
            Rect button = screenRect(resumed(), R.id.lookupButton);
            Log.i(TAG, "keyboard open: Show fact=" + button.toShortString());
            assertTrue("Show fact hidden by the keyboard", button.bottom <= withIme.navTop);
            Rect field = screenRect(resumed(), R.id.lookupInputLayout);
            Log.i(TAG, "keyboard open: imeInset=" + withIme.imeBottom + " field+helper=" + field.toShortString()
                    + " keyboardTop=" + withIme.navTop);
            assertTrue("keyboard did not open", withIme.imeBottom > 0);
            assertTrue("input field hidden by the keyboard", field.bottom <= withIme.navTop);

            pressBack();                                                      // real Back: dismiss keyboard
            SystemClock.sleep(800);
            assertFalse("keyboard still open after Back", bars(resumed()).imeBottom > 0);
            assertStillInApp("Back to dismiss the keyboard");

            onView(withId(R.id.lookupButton)).perform(scrollTo(), click());   // real tap on Show fact
            waitForFact(s);
            assertStillInApp("tapping Show fact");
            assertEquals("28", text(s, R.id.factSubject));
            Log.i(TAG, "Show fact tapped: subject=" + text(s, R.id.factSubject) + " text=" + text(s, R.id.factText)
                    + " source=" + text(s, R.id.factSource));

            // Finger-scroll to the very end, then tap Show fact again.
            onView(withId(R.id.scroll)).perform(swipeUp());
            onView(withId(R.id.scroll)).perform(swipeUp());
            SystemClock.sleep(600);
            button = screenRect(resumed(), R.id.lookupButton);
            Bars b = bars(resumed());
            Log.i(TAG, "after swipe to end: lookupButton=" + button.toShortString() + " navBarTop=" + b.navTop);
            assertTrue("Show fact behind the navigation bar at the end of the list", button.bottom <= b.navTop);
            onView(withId(R.id.lookupButton)).perform(click());
            waitForFact(s);
            assertStillInApp("tapping Show fact after swiping");
            Log.i(TAG, "real taps OK: subject=" + text(s, R.id.factSubject));
        }
    }
}
