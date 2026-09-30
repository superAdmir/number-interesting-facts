package com.nip.numberinterestingfacts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.nip.numberinterestingfacts.TestUtil.text;
import static com.nip.numberinterestingfacts.TestUtil.waitForFact;
import static org.junit.Assert.assertEquals;

import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Core UI flows that do not depend on specific Wikipedia content. Debug = test ads only. */
@RunWith(AndroidJUnit4.class)
public class MainScreenTest {

    @Test
    public void eachCategoryShowsItsOwnHeading() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            waitForFact(s);
            onView(withId(R.id.factEyebrow)).check(matches(withText(R.string.eyebrow_trivia)));
            onView(withId(R.id.chipYear)).perform(scrollTo(), click());
            waitForFact(s);
            onView(withId(R.id.factEyebrow)).check(matches(withText(R.string.eyebrow_year)));
            onView(withId(R.id.lookupTitle)).check(matches(withText(R.string.lookup_title_year)));
            onView(withId(R.id.chipDate)).perform(scrollTo(), click());
            waitForFact(s);
            onView(withId(R.id.factEyebrow)).check(matches(withText(R.string.eyebrow_date)));
            onView(withId(R.id.chipMath)).perform(scrollTo(), click());
            waitForFact(s);
            onView(withId(R.id.factEyebrow)).check(matches(withText(R.string.eyebrow_math)));
        }
    }

    @Test
    public void invalidYearShowsAnError() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            onView(withId(R.id.chipYear)).perform(scrollTo(), click());
            onView(withId(R.id.lookupInput)).perform(scrollTo(), replaceText("0"), closeSoftKeyboard());
            onView(withId(R.id.lookupButton)).perform(scrollTo(), click());
            s.onActivity(a -> assertEquals(a.getString(R.string.error_enter_year),
                    String.valueOf(((TextInputLayout) a.findViewById(R.id.lookupInputLayout)).getError())));
        }
    }

    @Test
    public void resultSurvivesRecreation() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            onView(withId(R.id.chipYear)).perform(scrollTo(), click());
            waitForFact(s);
            String before = text(s, R.id.factText);
            String subject = text(s, R.id.factSubject);
            String supplement = text(s, R.id.supplementText);
            s.recreate();
            onView(withId(R.id.factEyebrow)).check(matches(withText(R.string.eyebrow_year)));
            assertEquals(before, text(s, R.id.factText));
            assertEquals(subject, text(s, R.id.factSubject));
            assertEquals(supplement, text(s, R.id.supplementText));
        }
    }

    @Test
    public void dateFieldIsNotTruncated() {
        try (ActivityScenario<RandomActivity> s = ActivityScenario.launch(RandomActivity.class)) {
            onView(withId(R.id.chipDate)).perform(scrollTo(), click());
            // Regression: the number field's 4-character limit used to cut "September 25" to "Sept".
            s.onActivity(a -> {
                TextView input = a.findViewById(R.id.lookupInput);
                input.setText("September 25");
                assertEquals("September 25", input.getText().toString());
            });
        }
    }
}
