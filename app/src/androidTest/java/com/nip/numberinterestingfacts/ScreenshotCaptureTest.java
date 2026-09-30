package com.nip.numberinterestingfacts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.nip.numberinterestingfacts.TestUtil.text;
import static com.nip.numberinterestingfacts.TestUtil.waitForFact;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.nip.numberinterestingfacts.settings.ThemePreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Review captures of the real UI. Run only on the screenshot-mode debug build
 * ({@code -PscreenshotMode=true}: no ads). App-scoped: drives only this app's own views (no
 * injected touches), captures only this app's window (PixelCopy), changes no system setting,
 * and refuses to capture unless this app is resumed and focused. Files: {@code files/screenshots/}.
 */
@RunWith(AndroidJUnit4.class)
public class ScreenshotCaptureTest {
    private static final String TAG = "ScreenshotCapture";
    private String originalTheme;

    @Before
    public void rememberTheme() {
        originalTheme = ThemePreferences.get(context());
    }

    @After
    public void restoreTheme() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ThemePreferences.set(context(), originalTheme));
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static void setTheme(String theme) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ThemePreferences.set(context(), theme));
        SystemClock.sleep(500);
    }

    // ---- guards ----

    private static Activity resumed() {
        Activity[] out = new Activity[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> r = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            out[0] = r.isEmpty() ? null : r.iterator().next();
        });
        return out[0];
    }

    /** Stops the run if this app is not the focused foreground app. */
    private static void assertForeground(Class<?> expected) {
        Activity a = resumed();
        boolean[] focus = new boolean[1];
        if (a != null) InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> focus[0] = a.hasWindowFocus());
        if (a == null || !expected.isInstance(a) || !focus[0]) {
            throw new AssertionError("STOP: this app is not in the foreground (resumed="
                    + (a == null ? "none" : a.getClass().getName()) + ", focus=" + focus[0] + ")");
        }
    }

    private static boolean imeVisible(Activity a) {
        if (a == null) throw new AssertionError("STOP: this app is not in the foreground (resumed=none)");
        boolean[] out = new boolean[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());
            out[0] = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        });
        return out[0];
    }

    private static boolean fullyOnScreen(Activity a, int id) {
        if (a == null) throw new AssertionError("STOP: this app is not in the foreground (resumed=none)");
        boolean[] out = new boolean[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View v = a.findViewById(id);
            Rect r = new Rect();
            out[0] = v.getVisibility() == View.VISIBLE && v.getGlobalVisibleRect(r) && r.height() == v.getHeight();
        });
        return out[0];
    }

    /**
     * Captures only this app's own window with PixelCopy: exactly what the app renders, without
     * system overlays (status bar, navigation bar, Samsung Edge panel handle, notifications).
     * No setting is changed and nothing is drawn over the result.
     */
    private static void capture(String name, Class<?> expected) throws IOException {
        assertForeground(expected);
        Activity a = resumed();
        SystemClock.sleep(1200); // let layout, fonts and ripples settle
        assertForeground(expected);
        if (imeVisible(a)) throw new AssertionError("keyboard still visible");
        View decor = a.getWindow().getDecorView();
        Bitmap shot = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        int[] result = {-1};
        android.os.HandlerThread thread = new android.os.HandlerThread("pixelcopy");
        thread.start();
        android.view.PixelCopy.request(a.getWindow(), shot, r -> {
            result[0] = r;
            done.countDown();
        }, new android.os.Handler(thread.getLooper()));
        try {
            done.await(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            thread.quitSafely();
        }
        if (result[0] != android.view.PixelCopy.SUCCESS) throw new IOException("PixelCopy failed: " + result[0]);
        assertForeground(expected); // still ours after the capture
        File dir = new File(context().getFilesDir(), "screenshots");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("mkdir " + dir);
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            shot.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        Log.i(TAG, "captured " + name + " " + shot.getWidth() + "x" + shot.getHeight() + " (app window only)");
    }

    // ---- content selection: real Wikipedia content, retried until it reads well ----

    private interface Lookup {
        void run();
    }

    private static void selectFact(ActivityScenario<RandomActivity> s, Lookup lookup, Pattern prefer, int maxLen,
                                   String label) {
        String fallback = null;
        for (int i = 0; i < 25; i++) {
            assertForeground(RandomActivity.class);
            lookup.run();
            waitForFact(s);
            s.onActivity(a -> ((NestedScrollView) a.findViewById(R.id.scroll)).scrollTo(0, 0));
            SystemClock.sleep(300);
            String t = text(s, R.id.factText);
            boolean isWiki = text(s, R.id.factSource).startsWith("Source: Wikipedia");
            boolean fits = isWiki && t.length() <= maxLen && fullyOnScreen(resumed(), R.id.factSource);
            if (fits && prefer.matcher(t).find()) {
                Log.i(TAG, label + " chosen (preferred): " + t);
                return;
            }
            if (fits && fallback == null && i >= 12) {
                Log.i(TAG, label + " chosen (readable): " + t);
                return;
            }
        }
        throw new AssertionError("no readable Wikipedia fact for " + label + " after 25 attempts");
    }

    // No injected touches anywhere in this test: everything is driven through the app's own views,
    // so nothing can land on system UI (e.g. a 3-button navigation bar drawn over the content).
    private static ActivityScenario<RandomActivity> current;

    private static Lookup number(int chip, String value) {
        return () -> current.onActivity(a -> {
            a.findViewById(chip).performClick();
            android.widget.TextView input = a.findViewById(R.id.lookupInput);
            input.setText(value);
            a.findViewById(R.id.lookupButton).performClick();
        });
    }

    private static Lookup date(ActivityScenario<RandomActivity> s, int month, int day) {
        return () -> s.onActivity(a -> {
            a.findViewById(R.id.chipDate).performClick();
            long utc = LocalDate.of(2024, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            a.onDatePicked(utc);
        });
    }

    private void mainShot(String theme, String name, Lookup lookup, ActivityScenario<RandomActivity> s,
                          Pattern prefer, int maxLen) throws IOException {
        selectFact(s, lookup, prefer, maxLen, name);
        closeKeyboardIfOpen(s);
        capture(name, RandomActivity.class);
    }

    private static void closeKeyboardIfOpen(ActivityScenario<RandomActivity> s) {
        for (int i = 0; i < 10 && imeVisible(resumed()); i++) {
            s.onActivity(a -> {
                View focus = a.getCurrentFocus();
                android.view.inputmethod.InputMethodManager imm =
                        a.getSystemService(android.view.inputmethod.InputMethodManager.class);
                if (focus != null && imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            });
            SystemClock.sleep(300);
        }
    }

    @Test
    public void captureReviewScreenshots() throws IOException {
        setTheme(ThemePreferences.THEME_DARK);
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            current = s;
            waitForFact(s);
            mainShot("dark", "01-random-42-dark", number(R.id.chipTrivia, "42"), s,
                    Pattern.compile("Hitchhiker|Ultimate Question"), 330);
            mainShot("dark", "02-math-1729-dark", number(R.id.chipMath, "1729"), s,
                    Pattern.compile("Ramanujan|taxicab|two cubes|sum of"), 300);
            mainShot("dark", "03-year-1969-dark", number(R.id.chipYear, "1969"), s,
                    Pattern.compile("Apollo 11|Moon"), 300);
            mainShot("dark", "05-date-march14-dark", date(s, 3, 14), s,
                    Pattern.compile("Einstein|Pi|Mogyoród|Kennedy"), 240);
        }
        setTheme(ThemePreferences.THEME_LIGHT);
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            current = s;
            waitForFact(s);
            mainShot("light", "04-year-1969-light", number(R.id.chipYear, "1969"), s,
                    Pattern.compile("Apollo|Moon|Woodstock|Concorde"), 300);
        }
        try (ActivityScenario<SettingsActivity> s = ActivityScenario.launch(SettingsActivity.class)) {
            SystemClock.sleep(800);
            capture("06-settings-light", SettingsActivity.class);
        }
        setTheme(ThemePreferences.THEME_DARK);
        try (ActivityScenario<SettingsActivity> s = ActivityScenario.launch(SettingsActivity.class)) {
            SystemClock.sleep(800);
            capture("07-settings-dark", SettingsActivity.class);
        }
    }
}
