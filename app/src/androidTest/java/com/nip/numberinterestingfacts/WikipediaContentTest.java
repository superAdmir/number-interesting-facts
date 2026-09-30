package com.nip.numberinterestingfacts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.nip.numberinterestingfacts.TestUtil.text;
import static com.nip.numberinterestingfacts.TestUtil.visibility;
import static com.nip.numberinterestingfacts.TestUtil.waitForFact;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * Live lookups against en.wikipedia.org (needs internet). Verifies that the content shown
 * belongs to the selected subject and is attributed, and that failures degrade honestly.
 */
@RunWith(AndroidJUnit4.class)
public class WikipediaContentTest {
    private static final String TAG = "WikipediaContentTest";
    private static final Pattern DATED_EVENT = Pattern.compile("^\\d{1,4}( ?(BC|BCE|AD|CE))? ?[–—-] .+");

    @After
    public void resetOverrides() {
        DebugOverrides.setWikiHost(null);
    }

    private static void lookUp(int chip, String value) {
        onView(withId(chip)).perform(scrollTo(), click());
        onView(withId(R.id.lookupInput)).perform(scrollTo(), replaceText(value), closeSoftKeyboard());
        onView(withId(R.id.lookupButton)).perform(scrollTo(), click());
    }

    private static void pickDate(ActivityScenario<RandomActivity> s, int month, int day) {
        onView(withId(R.id.chipDate)).perform(scrollTo(), click());
        long utc = LocalDate.of(2024, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        s.onActivity(a -> a.onDatePicked(utc));
    }

    private static void log(ActivityScenario<RandomActivity> s, String label) {
        Log.i(TAG, label + " | subject=" + text(s, R.id.factSubject) + " | text=" + text(s, R.id.factText)
                + " | source=" + text(s, R.id.factSource) + " | calculated=" + text(s, R.id.supplementText));
    }

    @Test
    public void yearShowsAnEventFromThatYearsArticle() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipYear, "1969");
            waitForFact(s);
            log(s, "year 1969");
            assertEquals("1969", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“1969”"));
            assertEquals(View.GONE, visibility(s, R.id.retryButton));
            s.onActivity(a -> assertEquals(a.getString(R.string.supplement_label),
                    text(s, R.id.supplementLabel)));
        }
    }

    @Test
    public void dateShowsAnEventOnThatDay() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            pickDate(s, 3, 14);
            waitForFact(s);
            log(s, "date 03-14");
            assertEquals("March 14", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“March 14”"));
            assertTrue(text(s, R.id.factText), DATED_EVENT.matcher(text(s, R.id.factText)).matches());
        }
    }

    @Test
    public void february29HasItsOwnEvents() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            pickDate(s, 2, 29);
            waitForFact(s);
            log(s, "date 02-29");
            assertEquals("February 29", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“February 29”"));
            assertTrue(DATED_EVENT.matcher(text(s, R.id.factText)).matches());
        }
    }

    @Test
    public void numberTriviaMentionsTheNumber() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipTrivia, "42");
            waitForFact(s);
            log(s, "trivia 42");
            assertEquals("42", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“42 (number)”"));
            assertTrue(text(s, R.id.factText).matches("(?s).*(?<![\\d.,])42(?![\\d]).*"));
        }
    }

    @Test
    public void mathTriviaComesFromTheMathematicsSection() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipMath, "1729");
            waitForFact(s);
            log(s, "math 1729");
            assertEquals("1729", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“1729 (number)”"));
            assertTrue(text(s, R.id.factText).matches("(?s).*(?<![\\d.,])1,?729(?![\\d]).*"));
        }
    }

    @Test
    public void numberWithoutAnArticleShowsAnHonestEmptyState() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipTrivia, "4567");
            waitForFact(s);
            log(s, "trivia 4567");
            s.onActivity(a -> {
                assertEquals(a.getString(R.string.not_found_number, "4567"), text(s, R.id.factText));
                assertEquals(a.getString(R.string.supplement_label_fallback), text(s, R.id.supplementLabel));
                assertEquals(a.getString(R.string.source_none), text(s, R.id.factSource));
            });
            assertEquals(View.GONE, visibility(s, R.id.retryButton));
            assertEquals(View.VISIBLE, visibility(s, R.id.supplementBlock));
        }
    }

    @Test
    public void futureYearShowsAnHonestEmptyState() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipYear, "2150");
            waitForFact(s);
            log(s, "year 2150");
            s.onActivity(a -> assertEquals(a.getString(R.string.not_found_year, "2150"), text(s, R.id.factText)));
        }
    }

    @Test
    public void networkFailureOffersRetryAndALabelledFallback() {
        // Refused connection on the device itself: simulates "offline" without touching settings.
        DebugOverrides.setWikiHost("https://127.0.0.1:9");
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            lookUp(R.id.chipYear, "1969");
            waitForFact(s);
            log(s, "offline 1969");
            s.onActivity(a -> {
                assertEquals(a.getString(R.string.network_error), text(s, R.id.factText));
                assertEquals(a.getString(R.string.supplement_label_fallback), text(s, R.id.supplementLabel));
            });
            assertEquals("1969", text(s, R.id.factSubject));
            assertEquals(View.VISIBLE, visibility(s, R.id.retryButton));
            assertTrue(text(s, R.id.supplementText).contains("1969"));

            // Connection restored: Try again loads the same year from Wikipedia.
            DebugOverrides.setWikiHost(null);
            onView(withId(R.id.retryButton)).perform(scrollTo(), click());
            waitForFact(s);
            log(s, "retry 1969");
            assertEquals("1969", text(s, R.id.factSubject));
            assertTrue(text(s, R.id.factSource).contains("“1969”"));
            assertEquals(View.GONE, visibility(s, R.id.retryButton));
        }
    }
}
